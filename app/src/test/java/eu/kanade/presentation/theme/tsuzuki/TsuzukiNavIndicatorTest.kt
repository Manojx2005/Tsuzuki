package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiNavIndicatorTest {

    @Test
    fun `label contrast changes only after the moving indicator crosses the midpoint`() {
        navHighlightedIndex(1.49f, count = 5) shouldBe 1
        navHighlightedIndex(1.51f, count = 5) shouldBe 2
        navHighlightedIndex(-1.49f, count = 5, reverse = true) shouldBe 1
        navHighlightedIndex(-1.51f, count = 5, reverse = true) shouldBe 2
    }

    @Test
    fun `rail contrast follows the scaled cell stride`() {
        val extent = railCellHeight(40f) + 10f
        navHighlightedIndex(extent * 1.49f, count = 5, cellExtent = extent) shouldBe 1
        navHighlightedIndex(extent * 1.51f, count = 5, cellExtent = extent) shouldBe 2
        navHighlightedIndex(0f, count = 0) shouldBe -1
        navHighlightedIndex(0f, count = 5, cellExtent = 0f) shouldBe -1
    }

    @Test
    fun `phone indicator follows equal cells from the start edge`() {
        (0..4).map { navIndicatorOffset(it, count = 5, cellExtent = 64f) } shouldBe
            listOf(0f, 64f, 128f, 192f, 256f)
    }

    @Test
    fun `right to left indicator moves toward the physical left`() {
        navIndicatorOffset(3, count = 5, cellExtent = 64f, reverse = true) shouldBe -192f
    }

    @Test
    fun `rail indicator includes the gap between fixed cells`() {
        navIndicatorOffset(4, count = 5, cellExtent = 70f) shouldBe 280f
    }

    @Test
    fun `rail keeps normal cells at sixty dp and grows for scaled text`() {
        railCellHeight(labelHeightDp = 16f) shouldBe 60f
        railCellHeight(labelHeightDp = 40f) shouldBe 76f
        navIndicatorOffset(2, count = 5, cellExtent = railCellHeight(40f) + 10f) shouldBe 172f
    }

    @Test
    fun `empty and missing selections have no offset`() {
        navIndicatorOffset(-1, count = 5, cellExtent = 64f) shouldBe 0f
        navIndicatorOffset(0, count = 0, cellExtent = 64f) shouldBe 0f
        navIndicatorOffset(5, count = 5, cellExtent = 64f) shouldBe 0f
    }

    @Test
    fun `reduced motion snaps instead of sliding`() {
        (navIndicatorSpec(reduced = true) is SnapSpec<*>) shouldBe true
        (navIndicatorSpec(reduced = false) is SpringSpec<*>) shouldBe true
    }
}
