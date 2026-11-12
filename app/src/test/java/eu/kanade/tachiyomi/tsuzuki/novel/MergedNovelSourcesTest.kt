package eu.kanade.tachiyomi.tsuzuki.novel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.manga.model.Manga

class MergedNovelSourcesTest {
    @Test
    fun `combines numbered gaps and chooses an unlocked copy by priority`() {
        val primary = edition(1, chapter(11, 1, 1.0), chapter(12, 1, 2.5, locked = true))
        val fallback = edition(2, chapter(21, 2, 1.0, read = true), chapter(22, 2, 2.5), chapter(23, 2, 3.0))

        val combined = mergeNovelChapters(listOf(primary, fallback))

        assertEquals(listOf(11L, 22L, 23L), combined.map { it.preferred.id })
        assertTrue(combined.first().read)
        assertEquals(listOf(21L, 22L, 23L), mergeNovelChapters(listOf(fallback, primary)).map { it.preferred.id })
    }

    @Test
    fun `unnumbered and invalid chapters retain separate copies`() {
        val combined = mergeNovelChapters(
            listOf(
                edition(1, chapter(11, 1, -1.0), chapter(12, 1, Double.POSITIVE_INFINITY)),
                edition(2, chapter(21, 2, -1.0), chapter(22, 2, Double.NaN)),
            ),
        )
        assertEquals(listOf(11L, 12L, 21L, 22L), combined.map { it.preferred.id })
        assertTrue(combined.all { it.copies.size == 1 })
    }

    @Test
    fun `read state reaches matching editions without copying text positions`() {
        val original = chapter(11, 1, 2.5, read = true, progress = 99)
        val match = chapter(21, 2, 2.5, progress = 20)
        val editions = listOf(
            edition(1, original, chapter(12, 1, 2.5)),
            edition(2, match, chapter(22, 2, 2.0), chapter(23, 2, -1.0)),
        )
        assertEquals(
            listOf(ChapterUpdate(21, read = true)),
            linkedNovelReadUpdates(listOf(original), editions, read = true),
        )
        assertEquals(
            listOf(ChapterUpdate(21, read = false, lastPageRead = 0)),
            linkedNovelReadUpdates(listOf(original), editions, read = false),
        )
    }

    @Test
    fun `a newly fetched copy inherits only recognised read chapter numbers`() {
        val editions = listOf(edition(1, chapter(11, 1, 2.5, read = true), chapter(12, 1, -1.0, read = true)))
        val incoming = listOf(chapter(21, 2, 2.5), chapter(22, 2, 3.0), chapter(23, 2, -1.0))
        val updated = inheritNovelReadStatus(incoming, editions)

        assertEquals(listOf(true, false, false), updated.map(Chapter::read))
        assertEquals(listOf(0L, 0L, 0L), updated.map(Chapter::lastPageRead))
    }

    private fun edition(id: Long, vararg chapters: Chapter): NovelEdition =
        NovelEdition(Manga.create().copy(id = id, source = id, url = "/novel", isNovel = true), chapters.toList())

    private fun chapter(
        id: Long,
        book: Long,
        number: Double,
        read: Boolean = false,
        locked: Boolean = false,
        progress: Long = 0,
    ): Chapter = Chapter.create().copy(
        id = id,
        mangaId = book,
        chapterNumber = number,
        read = read,
        locked = locked,
        lastPageRead = progress,
    )
}
