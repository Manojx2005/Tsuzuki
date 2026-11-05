package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.palette.graphics.Palette
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class BookPaletteTest {

    private fun swatch(argb: Int) = mockk<Palette.Swatch> {
        every { rgb } returns argb
        every { population } returns 100
    }

    private fun palette(
        darkVibrant: Int? = null,
        darkMuted: Int? = null,
        vibrant: Int? = null,
        muted: Int? = null,
        dominant: Int? = null,
    ) = mockk<Palette> {
        every { darkVibrantSwatch } returns darkVibrant?.let(::swatch)
        every { darkMutedSwatch } returns darkMuted?.let(::swatch)
        every { vibrantSwatch } returns vibrant?.let(::swatch)
        every { lightVibrantSwatch } returns null
        every { lightMutedSwatch } returns null
        every { mutedSwatch } returns muted?.let(::swatch)
        every { dominantSwatch } returns dominant?.let(::swatch)
    }

    private val navy = Color(0xFF1C2B4A)
    private val gold = Color(0xFFE9B44C)

    @Test
    fun `darkenForText leaves an already dark colour untouched`() {
        darkenForText(navy) shouldBe navy
    }

    @Test
    fun `darkenForText darkens a bright colour until white text is readable`() {
        val result = darkenForText(Color(0xFFFFE0A0))
        result.luminance() shouldBeLessThanOrEqual 0.08f
        result.alpha shouldBe 1f
    }

    @Test
    fun `darkenForText respects a custom luminance limit`() {
        darkenForText(Color(0xFF808080), maxLuminance = 0.02f).luminance() shouldBeLessThanOrEqual 0.02f
    }

    @Test
    fun `liftToDesignDepth keeps colours that are already deep enough`() {
        liftToDesignDepth(navy) shouldBe navy
    }

    @Test
    fun `liftToDesignDepth brightens a near black colour and keeps its dominant channel`() {
        val dark = Color(0xFF05020A)
        val lifted = liftToDesignDepth(dark)
        (lifted.luminance() > dark.luminance()) shouldBe true
        (lifted.blue >= lifted.red && lifted.blue >= lifted.green) shouldBe true
    }

    @Test
    fun `liftToDesignDepth leaves pure black alone`() {
        liftToDesignDepth(Color.Black) shouldBe Color.Black
    }

    @Test
    fun `pickBookPalette falls back to navy and gold without swatches`() {
        val result = pickBookPalette(palette())
        result.background shouldBe navy
        result.accent shouldBe gold
    }

    @Test
    fun `pickBookPalette keeps the gold accent for a greyscale cover`() {
        val result = pickBookPalette(palette(darkMuted = 0xFF202020.toInt(), vibrant = 0xFFCCCCCC.toInt()))
        result.accent shouldBe gold
    }

    @Test
    fun `pickBookPalette takes a vibrant accent that stands out from the background`() {
        val vibrant = 0xFFFF8040.toInt()
        val result = pickBookPalette(palette(darkVibrant = 0xFF1A2A5A.toInt(), vibrant = vibrant))
        result.accent shouldBe Color(vibrant)
        result.background.luminance() shouldBeLessThanOrEqual 0.08f
    }

    @Test
    fun `pickBookPalette rejects an accent too close to the background`() {
        val result = pickBookPalette(palette(darkVibrant = 0xFF1A2A5A.toInt(), vibrant = 0xFF1E2E60.toInt()))
        result.accent shouldBe gold
    }
}
