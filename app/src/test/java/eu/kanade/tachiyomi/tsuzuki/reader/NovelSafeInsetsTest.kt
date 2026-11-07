package eu.kanade.tachiyomi.tsuzuki.reader

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovelSafeInsetsTest {
    @Test
    fun `the taller of notch and status bar wins at the top`() {
        // 3x density: 110 px notch, 90 px status bar -> 37 dp
        novelSafeInsetsDp(110, 90, 0, 63, 3f) shouldBe NovelSafeInsets(37, 21)
        novelSafeInsetsDp(0, 72, 0, 0, 3f) shouldBe NovelSafeInsets(24, 0)
    }

    @Test
    fun `no insets or a broken density give nothing`() {
        novelSafeInsetsDp(0, 0, 0, 0, 2.625f) shouldBe NovelSafeInsets.NONE
        novelSafeInsetsDp(100, 100, 100, 100, 0f) shouldBe NovelSafeInsets.NONE
    }
}
