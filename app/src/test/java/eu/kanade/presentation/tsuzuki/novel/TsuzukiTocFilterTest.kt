package eu.kanade.presentation.tsuzuki.novel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class TsuzukiTocFilterTest {

    private fun entry(id: Long, name: String, position: Int, bookmark: Boolean = false) = TsuzukiTocEntry(
        chapter = Chapter.create().copy(id = id, name = name, bookmark = bookmark),
        position = position,
        downloaded = false,
    )

    private val entries = listOf(
        entry(1, "Chapter 1: He Bravely Turned", 1),
        entry(2, "Rice Farming 101", 2, bookmark = true),
        entry(3, "v8c6: Herald", 347),
    )

    @Test
    fun `empty query keeps everything`() {
        assertEquals(listOf(1L, 2L, 3L), entries.filterToc("  ", marksOnly = false).map { it.chapter.id })
    }

    @Test
    fun `title match ignores case`() {
        assertEquals(listOf(3L), entries.filterToc("herald", marksOnly = false).map { it.chapter.id })
    }

    @Test
    fun `a number matches the reading position or a title containing it`() {
        assertEquals(listOf(3L), entries.filterToc("347", marksOnly = false).map { it.chapter.id })
        // "1" is position 1 and also appears in two titles.
        assertEquals(listOf(1L, 2L), entries.filterToc("1", marksOnly = false).map { it.chapter.id })
    }

    @Test
    fun `marks tab shows only bookmarked chapters`() {
        assertEquals(listOf(2L), entries.filterToc("", marksOnly = true).map { it.chapter.id })
    }
}
