package eu.kanade.presentation.tsuzuki.settings

import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiSettingsPaneTest {

    @Test
    fun `Dot uppercases Latin text`() {
        applyDotCase(true, "Tablet layout") shouldBe "TABLET LAYOUT"
    }

    @Test
    fun `other themes keep the text as written`() {
        applyDotCase(false, "Tablet layout") shouldBe "Tablet layout"
    }

    @Test
    fun `null text stays null in both cases`() {
        applyDotCase(true, null) shouldBe null
        applyDotCase(false, null) shouldBe null
    }

    @Test
    fun `Japanese text is unchanged by uppercase`() {
        applyDotCase(true, "表紙の表示") shouldBe "表紙の表示"
    }

    @Test
    fun `value label shows when the subtitle equals the selected entry`() {
        showsValueLabel(TsuzukiStyle.DOT, "Auto", "Auto") shouldBe true
        showsValueLabel(TsuzukiStyle.COLOUR, "Auto", "Auto") shouldBe true
    }

    @Test
    fun `value label stays off for stock themes`() {
        showsValueLabel(TsuzukiStyle.NONE, "Auto", "Auto") shouldBe false
    }

    @Test
    fun `value label stays off for descriptive subtitles`() {
        showsValueLabel(TsuzukiStyle.DOT, "Pick how the tablet looks", "Auto") shouldBe false
    }

    @Test
    fun `value label stays off for blank or missing subtitles`() {
        showsValueLabel(TsuzukiStyle.DOT, null, null) shouldBe false
        showsValueLabel(TsuzukiStyle.DOT, "", "") shouldBe false
        showsValueLabel(TsuzukiStyle.DOT, "  ", "  ") shouldBe false
        showsValueLabel(TsuzukiStyle.DOT, "Auto", null) shouldBe false
    }
}
