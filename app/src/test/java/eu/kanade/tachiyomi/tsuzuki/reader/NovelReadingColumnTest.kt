package eu.kanade.tachiyomi.tsuzuki.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NovelReadingColumnTest {
    @Test
    fun phoneWidthClassKeepsExistingLayout() {
        assertFalse(usesNovelReadingColumn(384))
        assertFalse(usesNovelReadingColumn(599))
        assertTrue(usesNovelReadingColumn(600))
    }

    @Test
    fun dockedPaneUsesItsOwnAvailableWidth() {
        assertEquals(NovelColumnInsets(0, 0), novelColumnInsets(550, 16, 16, 600))
        assertEquals(NovelColumnInsets(0, 0), novelColumnInsets(632, 16, 16, 600))
        assertEquals(NovelColumnInsets(0, 1), novelColumnInsets(633, 16, 16, 600))
    }

    @Test
    fun tabletSplitsExcessPaneWidthWithoutRoundingUpTextWidth() {
        assertEquals(NovelColumnInsets(60, 61), novelColumnInsets(753, 16, 16, 600))
        assertEquals(NovelColumnInsets(121, 121), novelColumnInsets(1506, 32, 32, 1200))
    }

    @Test
    fun asymmetricUserMarginsStayOnTheirRespectiveSides() {
        val inset = novelColumnInsets(1200, 16, 80, 600)
        assertEquals(NovelColumnInsets(252, 252), inset)
        assertEquals(600, 1200 - (16 + inset.left) - (80 + inset.right))
    }
}
