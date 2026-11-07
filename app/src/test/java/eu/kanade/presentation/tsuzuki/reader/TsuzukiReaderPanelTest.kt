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

    @Test
    fun tabletopDockOnlyUsesHalfOpenedHorizontalFolds() {
        assertNull(readerFold(halfOpened = false, horizontal = true, top = 500, bottom = 520))
        assertNull(readerFold(halfOpened = true, horizontal = false, top = 500, bottom = 520))
        assertNull(readerFold(halfOpened = true, horizontal = true, top = 0, bottom = 20))
        assertEquals(ReaderFold(500, 520), readerFold(halfOpened = true, horizontal = true, top = 500, bottom = 520))
    }

    @Test
    fun tabletopPaneLeavesTheHingeClear() {
        assertEquals(FoldedReaderPane(500, 480), foldedReaderPane(ReaderFold(500, 520), 1000))
        assertEquals(FoldedReaderPane(500, 500), foldedReaderPane(ReaderFold(500, 500), 1000))
        assertNull(foldedReaderPane(ReaderFold(500, 520), 520))
    }

    @Test
    fun listeningProgressUsesTheRealParagraphCountAndClampsOffsets() {
        assertNull(listeningParagraphPosition(0, 0))
        assertEquals(ListeningParagraphPosition(0, 4), listeningParagraphPosition(-1, 4))
        assertEquals(0.5f, listeningParagraphPosition(1, 4)?.progress)
        assertEquals(1f, listeningParagraphPosition(99, 4)?.progress)
    }
}
