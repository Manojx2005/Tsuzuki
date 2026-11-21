package eu.kanade.tachiyomi.tsuzuki.anime.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnimeSubtitleStyleTest {
    private fun options(style: AnimeSubtitleStyle) = subtitleStyleOptions(style).toMap()

    @Test
    fun `defaults map to readable mpv values`() {
        assertEquals(
            mapOf(
                "sub-font-size" to "55",
                "sub-bold" to "no",
                "sub-color" to "#FFFFFFFF",
                "sub-border-color" to "#FF000000",
                "sub-border-size" to "3",
                "sub-border-style" to "outline-and-shadow",
                "sub-back-color" to "#00000000",
                "sub-pos" to "95",
                "sub-ass-override" to "scale",
            ),
            options(AnimeSubtitleStyle()),
        )
    }

    @Test
    fun `every row maps to its option`() {
        val style = AnimeSubtitleStyle(
            fontSize = 30,
            bold = true,
            color = AnimeSubtitleColor.YELLOW,
            border = AnimeSubtitleBorder.GREY,
            borderSize = 5,
            background = true,
            position = 60,
            overrideAss = true,
        )
        val o = options(style)
        assertEquals("30", o["sub-font-size"])
        assertEquals("yes", o["sub-bold"])
        assertEquals("#FFFFFF00", o["sub-color"])
        assertEquals("#FF808080", o["sub-border-color"])
        assertEquals("5", o["sub-border-size"])
        assertEquals("background-box", o["sub-border-style"])
        assertEquals("#99000000", o["sub-back-color"])
        assertEquals("60", o["sub-pos"])
        assertEquals("force", o["sub-ass-override"])
    }

    @Test
    fun `no outline means outline width zero`() {
        val o = options(AnimeSubtitleStyle(border = AnimeSubtitleBorder.NONE, borderSize = 4))
        assertEquals("0", o["sub-border-size"])
    }

    @Test
    fun `out of range values are clamped`() {
        val o = options(AnimeSubtitleStyle(fontSize = 200, borderSize = -2, position = 10))
        assertEquals("80", o["sub-font-size"])
        assertEquals("0", o["sub-border-size"])
        assertEquals("50", o["sub-pos"])
    }

    @Test
    fun `only changed options are applied again`() {
        val before = AnimeSubtitleStyle()
        assertEquals(
            listOf("sub-color" to "#FF00FFFF"),
            changedSubtitleStyleOptions(before, before.copy(color = AnimeSubtitleColor.CYAN)),
        )
        assertEquals(9, changedSubtitleStyleOptions(null, before).size)
    }
}
