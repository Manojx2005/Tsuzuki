package eu.kanade.presentation.theme.tsuzuki

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiNavLabelTest {
    @Test
    fun `labels without spaces shrink to fit instead of wrapping`() {
        navLabelIsSingleWord("ライブラリ") shouldBe true
        navLabelIsSingleWord("LIBRARY") shouldBe true
        navLabelIsSingleWord("Bibliothek") shouldBe true
    }

    @Test
    fun `labels with spaces may still wrap between words`() {
        navLabelIsSingleWord("Mises à jour") shouldBe false
        navLabelIsSingleWord("Más opciones") shouldBe false
    }

    @Test
    fun `blank labels are not treated as single words`() {
        navLabelIsSingleWord("") shouldBe false
        navLabelIsSingleWord(" ") shouldBe false
    }
}
