package eu.kanade.presentation.tsuzuki.details

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class MarkPreviousReadTest {

    private fun ch(id: Long, read: Boolean = false, lastPageRead: Long = 0) =
        Chapter.create().copy(id = id, read = read, lastPageRead = lastPageRead)

    @Test
    fun `nothing read means nothing to mark`() {
        assertEquals(emptyList<Long>(), chaptersBeforeFurthestProgress(listOf(ch(1), ch(2), ch(3))).map { it.id })
    }

    @Test
    fun `starting mid-way marks the skipped chapters`() {
        val chapters = listOf(ch(1), ch(2), ch(3), ch(4, lastPageRead = 5), ch(5))
        assertEquals(listOf(1L, 2L, 3L), chaptersBeforeFurthestProgress(chapters).map { it.id })
    }

    @Test
    fun `already read chapters are left alone and the anchor itself is never included`() {
        val chapters = listOf(ch(1, read = true), ch(2), ch(3, read = true), ch(4))
        assertEquals(listOf(2L), chaptersBeforeFurthestProgress(chapters).map { it.id })
    }

    @Test
    fun `progress on the first chapter only has nothing before it`() {
        assertEquals(
            emptyList<Long>(),
            chaptersBeforeFurthestProgress(listOf(ch(1, lastPageRead = 3), ch(2))).map {
                it.id
            },
        )
    }
}
