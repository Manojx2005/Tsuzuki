package eu.kanade.tachiyomi.ui.library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LibraryRangeSelectionTest {

    private val category = listOf(10L, 11L, 12L, 13L, 14L)

    @Test
    fun `selects forward range inclusive`() {
        assertEquals(listOf(11L, 12L, 13L), libraryRangeSelection(category, 11L, 13L))
    }

    @Test
    fun `selects backward range inclusive`() {
        assertEquals(listOf(11L, 12L, 13L), libraryRangeSelection(category, 13L, 11L))
    }

    @Test
    fun `same item selects just that item`() {
        assertEquals(listOf(12L), libraryRangeSelection(category, 12L, 12L))
    }

    @Test
    fun `last selection from another category falls back to the single item`() {
        // The last selected manga lives in a different category, so it is not in this category's items.
        assertEquals(listOf(12L), libraryRangeSelection(category, 99L, 12L))
    }

    @Test
    fun `no last selection falls back to the single item`() {
        assertEquals(listOf(12L), libraryRangeSelection(category, null, 12L))
    }

    @Test
    fun `current item missing from category falls back to the single item`() {
        assertEquals(listOf(99L), libraryRangeSelection(category, 11L, 99L))
    }
}
