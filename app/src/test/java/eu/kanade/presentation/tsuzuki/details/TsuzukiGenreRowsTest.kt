package eu.kanade.presentation.tsuzuki.details

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TsuzukiGenreRowsTest {

    @Test
    fun `all chips fit across two rows without an overflow chip`() {
        assertEquals(4, genresFittingTwoLines(listOf(40, 40, 40, 40), maxWidth = 100, gap = 8))
    }

    @Test
    fun `overflow chip displaces a tag that would make a third row`() {
        val widths = listOf(45, 45, 45, 45, 45)
        assertEquals(4, genresFittingTwoLines(widths, maxWidth = 100, gap = 8))
        assertEquals(3, genresFittingTwoLines(widths, maxWidth = 100, gap = 8, moreWidth = 32))
    }

    @Test
    fun `wide labels leave the second row to the overflow chip when needed`() {
        assertEquals(1, genresFittingTwoLines(listOf(95, 95, 95), maxWidth = 100, gap = 8, moreWidth = 32))
    }
}
