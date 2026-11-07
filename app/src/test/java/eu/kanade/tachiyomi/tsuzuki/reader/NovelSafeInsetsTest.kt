package eu.kanade.tachiyomi.tsuzuki.reader

import androidx.core.graphics.Insets
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovelSafeInsetsTest {
    @Test
    fun `top novel status bar starts below the notch`() {
        val safeArea = Insets.of(0, 110, 0, 63)
        safeArea.withNovelStatusBar(heightPx = 40, atBottom = false) shouldBe Insets.of(0, 150, 0, 63)
    }

    @Test
    fun `bottom novel status bar preserves side cutouts`() {
        val safeArea = Insets.of(96, 60, 44, 24)
        safeArea.withNovelStatusBar(heightPx = 40, atBottom = true) shouldBe Insets.of(96, 60, 44, 64)
        safeArea.withNovelStatusBar(heightPx = 0, atBottom = true) shouldBe safeArea
    }
}
