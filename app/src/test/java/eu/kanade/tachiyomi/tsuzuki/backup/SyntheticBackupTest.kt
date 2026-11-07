package eu.kanade.tachiyomi.tsuzuki.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.BackupDecoder
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SyntheticBackupTest {
    @Test
    fun syntheticLibraryDecodesWithChapterProgressShelvesAndHistory() {
        // python scripts/generate_synthetic_backup.py app/src/test/resources/synthetic-library-small.tachibk
        //     --books 4 --shelves 2 --chapters-per-book 5 --anchor-ms 1793923200000
        val fixture = checkNotNull(javaClass.getResourceAsStream("/synthetic-library-small.tachibk"))
            .use { it.readBytes() }
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver> {
            every { openInputStream(uri) } answers { fixture.inputStream() }
        }
        val context = mockk<Context> { every { contentResolver } returns resolver }

        val backup = BackupDecoder(context, ProtoBuf).decode(uri)
        val books = backup.backupManga
        val chapters = books.flatMap { it.chapters }
        val sourceIds = backup.backupSources.map { it.sourceId }.toSet()
        val shelfOrders = backup.backupCategories.map { it.order }.toSet()

        assertEquals(4, books.size)
        assertEquals(2, books.count { it.isNovel })
        assertEquals(2, sourceIds.size)
        assertEquals(setOf(0L, 1L), shelfOrders)
        assertEquals(20, chapters.size)
        assertEquals(20, chapters.map { it.url }.toSet().size)
        assertEquals(8, books.sumOf { it.history.size })
        assertTrue(backup.backupCategories.all { it.name.startsWith("[synthetic]") })
        assertTrue(backup.backupSources.all { it.name.startsWith("[synthetic]") })
        books.forEach { book ->
            assertTrue(book.title.startsWith("[synthetic]"))
            assertTrue(book.favorite)
            assertTrue(book.source in sourceIds)
            assertTrue(book.categories.single() in shelfOrders)
            assertEquals(listOf(5f, 4f, 3f, 2f, 1f), book.chapters.map { it.chapterNumber })
            val progress = book.chapters.single { it.lastPageRead > 0 }
            assertEquals(if (book.isNovel) 60L else 7L, progress.lastPageRead)
            val chapterUrls = book.chapters.map { it.url }.toSet()
            assertTrue(book.history.all { it.url in chapterUrls && it.lastRead > 0 && it.readDuration > 0 })
        }
    }
}
