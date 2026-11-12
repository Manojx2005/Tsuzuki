package eu.kanade.tachiyomi.tsuzuki.reader

import android.content.Context
import android.util.AtomicFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.ChapterContentReader
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalNovelSource
import java.io.File
import java.io.FileNotFoundException
import java.util.Date

class NovelRecapSessionTest {
    @TempDir lateinit var cache: File

    @Test
    fun coldLocalRecapLoadsTextFromUrlOnlyPage() = runTest {
        withColdCache {
            val source = mockk<LocalNovelSource>()
            val chapter = chapter()
            val page = Page(0, url = "localnovels/book/chapter-1.txt")
            val first = "The archivist lowered the ledger onto the table."
            val second = "She heard the bells ring from the locked tower."
            coEvery { source.getPageList(chapter.chapter) } returns listOf(page)
            coEvery { source.fetchPageText(page) } returns "$first\n\n$second"
            val session = session()

            session.prepare(Manga.create().copy(id = 1), 3, chapter, source)

            assertEquals(listOf(first, second), session.forChapter(3)?.paragraphs)
            coVerify(exactly = 1) { source.fetchPageText(page) }
            session.dismiss(3)
            assertEquals(null, session.forChapter(3))
        }
    }

    @Test
    fun localFetchCancellationPropagates() {
        assertThrows(CancellationException::class.java) {
            runTest {
                withColdCache {
                    val source = mockk<LocalNovelSource>()
                    coEvery { source.getPageList(any()) } returns listOf(Page(0, url = "local"))
                    coEvery { source.fetchPageText(any()) } throws CancellationException("reader closed")
                    session().prepare(Manga.create().copy(id = 1), 3, chapter(), source)
                }
            }
        }
    }

    private fun session(): NovelRecapSession {
        val context = mockk<Context> { every { cacheDir } returns cache }
        return NovelRecapSession(context, mockk()) {
            listOf(History(1, 2, Date(1_000), 0))
        }
    }

    private fun chapter() = ReaderChapter(
        mockk<Chapter>(relaxed = true) {
            every { id } returns 2
            every { manga_id } returns 1
        },
    )

    private suspend fun withColdCache(block: suspend () -> Unit) {
        mockkConstructor(AtomicFile::class, ChapterContentReader::class)
        try {
            every { anyConstructed<AtomicFile>().readFully() } throws FileNotFoundException()
            coEvery { anyConstructed<ChapterContentReader>().readDownloadedContent(any(), any(), any()) } returns null
            block()
        } finally {
            unmockkConstructor(AtomicFile::class, ChapterContentReader::class)
        }
    }
}
