package eu.kanade.presentation.theme.tsuzuki

import eu.kanade.presentation.tsuzuki.reader.novelActionSlotCount
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiNarrowLayoutTest {
    @Test
    fun `pill overflow keeps the selected shelf visible`() {
        visiblePillIndices(0, -1) shouldBe emptyList()
        visiblePillIndices(6, 5) shouldBe (0..5).toList()
        visiblePillIndices(24, 2) shouldBe (0..4).toList()
        visiblePillIndices(24, 23) shouldBe listOf(0, 1, 2, 3, 23)
    }

    @Test
    fun `reader action capacity drops as font scale grows`() {
        novelActionSlotCount(360f, 1f) shouldBe 5
        novelActionSlotCount(360f, 1.3f) shouldBe 4
        novelActionSlotCount(360f, 2f) shouldBe 2
    }
}
