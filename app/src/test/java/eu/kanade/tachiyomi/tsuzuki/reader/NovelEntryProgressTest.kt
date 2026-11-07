package eu.kanade.tachiyomi.tsuzuki.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NovelEntryProgressTest {
    @Test
    fun `previous leaves room to scroll through a completed chapter`() {
        assertEquals(0f, novelEntryProgress(100, true, true, enteredViaPrevious = true))
    }

    @Test
    fun `opening a completed chapter directly respects the restore preference`() {
        assertEquals(1f, novelEntryProgress(100, true, true, enteredViaPrevious = false))
        assertEquals(0f, novelEntryProgress(100, true, false, enteredViaPrevious = false))
    }

    @Test
    fun `previous restores a valid partial position`() {
        assertEquals(0.42f, novelEntryProgress(42, false, false, enteredViaPrevious = true))
        assertEquals(0.42f, novelEntryProgress(42, true, true, enteredViaPrevious = true))
        assertEquals(0f, novelEntryProgress(101, false, true, enteredViaPrevious = true))
    }
}
