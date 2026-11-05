package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.ui.graphics.Color
import eu.kanade.domain.ui.model.AppTheme
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiAccentTest {
    @Test
    fun `default swatch is the original gold`() {
        TsuzukiAccentSwatch.Default.argb shouldBe 0xFFE9B44C.toInt()
        ColourTokens.accent shouldBe Color(0xFFE9B44C)
    }

    @Test
    fun `stored colour maps to its swatch and unknown values fall back to gold`() {
        TsuzukiAccentSwatch.fromArgb(TsuzukiAccentSwatch.SKY.argb) shouldBe TsuzukiAccentSwatch.SKY
        TsuzukiAccentSwatch.fromArgb(0x12345678) shouldBe TsuzukiAccentSwatch.GOLD
    }

    @Test
    fun `swatches are distinct`() {
        TsuzukiAccentSwatch.entries.map { it.argb }.toSet().size shouldBe TsuzukiAccentSwatch.entries.size
    }

    @Test
    fun `colour tokens take the chosen accent and Dot stays red`() {
        val mint = TsuzukiAccentSwatch.MINT.color
        val colour = tsuzukiTokensFor(AppTheme.COLOUR, mint)
        colour.accent shouldBe mint
        colour.dotHere shouldBe mint
        colour.background shouldBe ColourTokens.background
        tsuzukiTokensFor(AppTheme.COLOUR, null) shouldBe ColourTokens
        tsuzukiTokensFor(AppTheme.COLOUR, ColourTokens.accent) shouldBe ColourTokens
        tsuzukiTokensFor(AppTheme.DOT, mint) shouldBe DotTokens
    }

    @Test
    fun `reduce motion is the system setting or the app setting`() {
        isMotionReduced(systemScaleIsZero = false, appPreference = false) shouldBe false
        isMotionReduced(systemScaleIsZero = true, appPreference = false) shouldBe true
        isMotionReduced(systemScaleIsZero = false, appPreference = true) shouldBe true
        isMotionReduced(systemScaleIsZero = true, appPreference = true) shouldBe true
    }
}
