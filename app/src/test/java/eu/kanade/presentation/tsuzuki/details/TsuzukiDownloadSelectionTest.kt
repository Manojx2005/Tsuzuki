package eu.kanade.presentation.tsuzuki.details

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.ChapterList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class TsuzukiDownloadSelectionTest {

    private fun item(
        id: Long,
        read: Boolean = false,
        bookmark: Boolean = false,
        state: Download.State = Download.State.NOT_DOWNLOADED,
        number: Double = -1.0,
        scanlator: String? = null,
        selected: Boolean = false,
    ) = ChapterList.Item(
        chapter = Chapter.create().copy(
            id = id,
            read = read,
            bookmark = bookmark,
            chapterNumber = number,
            scanlator = scanlator,
        ),
        downloadState = state,
        downloadProgress = 0,
        selected = selected,
    )

    @Test
    fun `next batch follows supplied reading order and skips read chapters`() {
        val chapters = listOf(item(9, read = true), item(7), item(2), item(8), item(1), item(6), item(3))

        assertEquals(listOf(7L), chaptersToSave(chapters, TsuzukiDownloadOption.NEXT_1).map { it.id })
        assertEquals(listOf(7L, 2L, 8L, 1L, 6L), chaptersToSave(chapters, TsuzukiDownloadOption.NEXT_5).map { it.id })
    }

    @Test
    fun `saved and active downloads are excluded while failed chapters can retry`() {
        val chapters = listOf(
            item(1, state = Download.State.DOWNLOADED),
            item(2, state = Download.State.QUEUE),
            item(3, state = Download.State.DOWNLOADING),
            item(4, state = Download.State.ERROR),
            item(5),
        )

        assertEquals(listOf(4L, 5L), chaptersToSave(chapters, TsuzukiDownloadOption.UNREAD).map { it.id })
        assertEquals(listOf(4L, 5L), chaptersToSave(chapters, TsuzukiDownloadOption.ALL).map { it.id })
    }

    @Test
    fun `unread and all differ when a read chapter has not been saved`() {
        val chapters = listOf(item(1, read = true), item(2), item(3, read = true))

        assertEquals(listOf(2L), chaptersToSave(chapters, TsuzukiDownloadOption.UNREAD).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), chaptersToSave(chapters, TsuzukiDownloadOption.ALL).map { it.id })
    }

    @Test
    fun `custom range is inclusive and preserves decimal and duplicate source numbers`() {
        val chapters = listOf(
            item(10, number = 12.0),
            item(20, number = 12.5, scanlator = "A"),
            item(30, number = 12.5, scanlator = "B"),
            item(40, number = 13.0),
        )

        assertEquals(listOf(20L, 30L), chaptersInNumberRange(chapters, 12.5, 12.5).map { it.id })
        assertEquals(listOf(20L, 30L, 40L), chaptersInNumberRange(chapters, 12.5, 13.0).map { it.id })
        assertTrue(chaptersInNumberRange(chapters, 13.0, 12.5).isEmpty())
        assertTrue(chaptersInNumberRange(chapters, Double.NaN, 13.0).isEmpty())
    }

    @Test
    fun `chapters without a source number use their one-based reading position`() {
        val chapters = listOf(
            item(10, number = 100.0),
            item(20, read = true),
            item(30, state = Download.State.QUEUE),
            item(40),
        )

        assertEquals(listOf(20L, 30L, 40L), chaptersInNumberRange(chapters, 2.0, 4.0).map { it.id })
        assertEquals(listOf(20L, 40L), chaptersToSave(chapters, TsuzukiDownloadOption.CUSTOM, 2.0..4.0).map { it.id })
        assertTrue(chaptersToSave(chapters, TsuzukiDownloadOption.CUSTOM, 5.0..9.0).isEmpty())
        assertTrue(chaptersToSave(chapters, TsuzukiDownloadOption.CUSTOM, null).isEmpty())
    }

    @Test
    fun `chapter entry accepts decimal keyboards and rejects nonnumeric or infinite values`() {
        assertEquals(12.5, parseDownloadChapterNumber("12.5"))
        assertEquals(12.5, parseDownloadChapterNumber("12,5"))
        assertEquals(12.5, parseDownloadChapterNumber("１２．５"))
        assertEquals(null, parseDownloadChapterNumber("12.5.5"))
        assertEquals(null, parseDownloadChapterNumber("1e2"))
        assertEquals(null, parseDownloadChapterNumber("Infinity"))
    }

    @Test
    fun `long press selects up to the nearest visible selection boundary`() {
        val chapters = listOf(item(1), item(2), item(3, selected = true), item(4), item(5, selected = true), item(6))
        assertEquals(0..2, chapterSelectionRange(chapters, 0))
        assertEquals(4..5, chapterSelectionRange(chapters, 5))
        assertEquals(3..3, chapterSelectionRange(chapters, 3))
        assertEquals(0..0, chapterSelectionRange(chapters.take(2), 0))
    }

    @Test
    fun `bookmarks preserve read chapters but exclude already saved bookmarks`() {
        val chapters = listOf(
            item(1),
            item(2, read = true, bookmark = true),
            item(3, bookmark = true, state = Download.State.DOWNLOADED),
            item(4, bookmark = true),
        )

        assertEquals(listOf(2L, 4L), chaptersToSave(chapters, TsuzukiDownloadOption.BOOKMARKED).map { it.id })
    }

    @Test
    fun `no option queues anything for an empty library`() {
        TsuzukiDownloadOption.entries.forEach { option ->
            assertTrue(chaptersToSave(emptyList(), option).isEmpty(), option.name)
        }
    }

    @Test
    fun `a chapter appearing twice only enters a batch once`() {
        val chapters = listOf(item(3), item(8), item(3), item(5))

        assertEquals(listOf(3L, 8L, 5L), chaptersToSave(chapters, TsuzukiDownloadOption.ALL).map { it.id })
    }
}
