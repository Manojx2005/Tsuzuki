package eu.kanade.tachiyomi.tsuzuki.library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.model.History
import java.util.Date

class MarkUntilLastReadTest {

    private fun chapter(id: Long, read: Boolean = false, page: Long = 0) =
        Chapter.create().copy(id = id, read = read, lastPageRead = page)

    private fun history(chapterId: Long, at: Long) =
        History(id = chapterId, chapterId = chapterId, readAt = Date(at), readDuration = 0)

    private fun ids(list: List<Chapter>) = list.map { it.id }

    @Test
    fun `marks unread chapters before the last read one and leaves it alone`() {
        val chapters = listOf(chapter(1), chapter(2, read = true), chapter(3), chapter(4, page = 5), chapter(5))
        assertEquals(listOf(1L, 3L), ids(chaptersBeforeLastRead(chapters, emptyList())))
    }

    @Test
    fun `uses the furthest of history and progress`() {
        val chapters = (1L..6L).map { chapter(it) }
        // Read chapter 5 first, then went back to chapter 2 most recently.
        val history = listOf(history(5, at = 100), history(2, at = 200))
        assertEquals(listOf(1L), ids(chaptersBeforeLastRead(chapters, history)))
        val progressed = chapters.map { if (it.id == 5L) it.copy(lastPageRead = 3) else it }
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(chaptersBeforeLastRead(progressed, history)))
    }

    @Test
    fun `nothing to mark without reading or at the first chapter`() {
        val chapters = (1L..3L).map { chapter(it) }
        assertEquals(emptyList<Long>(), ids(chaptersBeforeLastRead(chapters, emptyList())))
        assertEquals(emptyList<Long>(), ids(chaptersBeforeLastRead(chapters, listOf(history(1, at = 1)))))
    }

    @Test
    fun `history for a chapter no longer listed is ignored`() {
        val chapters = listOf(chapter(1), chapter(2), chapter(3, read = true))
        assertEquals(listOf(1L, 2L), ids(chaptersBeforeLastRead(chapters, listOf(history(99, at = 5)))))
    }
}
