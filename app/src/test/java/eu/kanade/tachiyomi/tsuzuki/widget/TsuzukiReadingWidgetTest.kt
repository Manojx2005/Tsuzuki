package eu.kanade.tachiyomi.tsuzuki.widget

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiReadingWidgetTest {
    @Test
    fun `widget hides progress when there are no chapters`() {
        readingWidgetProgress(0, 0) shouldBe null
    }

    @Test
    fun `widget uses the real read count and clamps inconsistent data`() {
        readingWidgetProgress(3, 4) shouldBe 0.75f
        readingWidgetProgress(-1, 4) shouldBe 0f
        readingWidgetProgress(5, 4) shouldBe 1f
    }
}
