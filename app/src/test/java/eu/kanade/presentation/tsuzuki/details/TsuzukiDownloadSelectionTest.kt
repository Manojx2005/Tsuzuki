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
    ) = ChapterList.Item(
        chapter = Chapter.create().copy(id = id, read = read, bookmark = bookmark),
        downloadState = state,
        downloadProgress = 0,
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
    fun `custom range uses inclusive list positions before excluding active downloads`() {
        val chapters = listOf(item(10), item(20, read = true), item(30, state = Download.State.QUEUE), item(40))

        assertEquals(listOf(20L, 40L), chaptersToSave(chapters, TsuzukiDownloadOption.CUSTOM, 1..3).map { it.id })
        assertEquals(
            listOf(10L, 20L, 40L),
            chaptersToSave(chapters, TsuzukiDownloadOption.CUSTOM, -5..99).map {
                it.id
            },
        )
        assertTrue(chaptersToSave(chapters, TsuzukiDownloadOption.CUSTOM, 4..9).isEmpty())
        assertTrue(chaptersToSave(chapters, TsuzukiDownloadOption.CUSTOM, IntRange.EMPTY).isEmpty())
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
