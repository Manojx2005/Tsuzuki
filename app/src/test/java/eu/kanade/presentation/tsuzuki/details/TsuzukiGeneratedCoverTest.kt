package eu.kanade.presentation.tsuzuki.details

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiGeneratedCoverTest {
    @Test
    fun `uses a stable palette and motif for a title`() {
        val design = generatedCoverDesign("A")
        design.palette shouldBe CoverPalette(Color(0xFF0E2A3A), Color(0xFF5FC7E8), Color(0xFFE8F6FB))
        design.motif shouldBe 0
        generatedCoverDesign(" A ") shouldBe design
    }

    @Test
    fun `negative hashes still select a palette and motif`() {
        val title = "polygenelubricants"
        title.hashCode() shouldBe Int.MIN_VALUE
        val design = generatedCoverDesign(title)
        design.palette shouldBe CoverPalette(Color(0xFF123B36), Color(0xFF9FD8B5), Color(0xFFEFF7F1))
        design.motif shouldBe 2
    }
}
