package eu.kanade.presentation.tsuzuki.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TsuzukiReaderPanelTest {
    @Test
    fun `docks only on tablet ui with a wide window`() {
        assertTrue(shouldDockReaderPanel(tabletUi = true, windowWidthDp = 1205))
        assertTrue(shouldDockReaderPanel(tabletUi = true, windowWidthDp = 900))
        assertFalse(shouldDockReaderPanel(tabletUi = true, windowWidthDp = 753))
        assertFalse(shouldDockReaderPanel(tabletUi = false, windowWidthDp = 1205))
    }

    @Test
    fun `panel width is clamped`() {
        assertEquals(336, readerPanelWidthDp(1200))
        assertEquals(320, readerPanelWidthDp(900))
        assertEquals(420, readerPanelWidthDp(2000))
    }

    @Test
    fun `blank notes show nothing`() {
        assertNull(panelNoteText("  \n "))
        assertEquals("a note", panelNoteText("  a note\n"))
    }
}
