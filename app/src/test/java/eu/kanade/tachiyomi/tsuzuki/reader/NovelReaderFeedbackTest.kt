package eu.kanade.tachiyomi.tsuzuki.reader

import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.theme.tsuzuki.ColourTokens
import eu.kanade.presentation.theme.tsuzuki.DotTokens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NovelReaderFeedbackTest {
    @Test
    fun readAlongClearsTheMeasuredBarAndGapAndClampsAtTheChapterStart() {
        assertEquals(284, novelHighlightScrollTarget(500, 200, 16))
        assertEquals(0, novelHighlightScrollTarget(100, 200, 16))
        assertEquals(484, novelHighlightScrollTarget(500, 0, 16))
    }

    @Test
    fun firstHighlightedLineCanClearTheBarEvenWhenScrollCannotGoNegative() {
        val bar = 200
        val gap = 16
        val inset = bar + gap
        for (line in listOf(0, 50, 500)) {
            val scroll = novelHighlightScrollTarget(line, bar, gap, inset)
            assertEquals(bar + gap, line + inset - scroll)
        }
    }

    @Test
    fun endCardSpaceExistsOnlyWhileTheVisibleBarsWouldCoverACard() {
        assertEquals(176, novelEndCardClearance(true, 160, 16))
        assertEquals(0, novelEndCardClearance(true, 0, 16))
        assertEquals(0, novelEndCardClearance(false, 160, 16))
    }

    @Test
    fun dotUsesRedWhiteSelectionAndNeutralReadAlongInsteadOfYellow() {
        val colors = novelFeedbackColors(DotTokens, 0xff000000.toInt())
        assertEquals(0xffff2d2d.toInt(), colors.selectionFill)
        assertEquals(0xffffffff.toInt(), colors.selectionInk)
        assertEquals(0xff2e2e2e.toInt(), colors.readAlongFill)
        assertEquals(0xffffffff.toInt(), colors.readAlongInk)
        val css = novelSelectionCss(colors)
        assertTrue(css.contains("::selection"))
        assertTrue(css.contains("background-color: #ff2d2dff !important"))
        assertTrue(css.contains("color: #ffffffff !important"))
    }

    @Test
    fun colourFollowsTheSelectedAccentAndKeepsThePageInkForReadAlong() {
        val tokens = ColourTokens.copy(accent = Color(0xff128bad), onAccent = Color.White)
        val colors = novelFeedbackColors(tokens, 0xff242424.toInt())
        assertTrue(novelSelectionCss(colors).contains("#128badff"))
        assertEquals("#128bad47", novelCssColor(colors.readAlongFill))
        assertEquals(0xff242424.toInt(), colors.readAlongInk)
    }
}
