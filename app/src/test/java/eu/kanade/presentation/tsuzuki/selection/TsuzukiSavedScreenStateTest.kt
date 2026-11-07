package eu.kanade.presentation.tsuzuki.selection

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

class TsuzukiSavedScreenStateTest {
    @Test
    fun `selection snapshot retains order and full long identifiers`() {
        val ids = linkedSetOf(7L, Long.MAX_VALUE, 9L)
        assertArrayEquals(longArrayOf(7, Long.MAX_VALUE, 9), ids.savedSelection())
    }

    @Test
    fun `oversized selection restores empty without keeping a misleading partial selection`() {
        val ids = (1L..MAX_SAVED_SELECTION + 1).toList()
        assertArrayEquals(longArrayOf(), ids.savedSelection())
    }
}
