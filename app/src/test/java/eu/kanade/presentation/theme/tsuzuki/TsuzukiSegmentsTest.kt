package eu.kanade.presentation.theme.tsuzuki

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiSegmentsTest {

    @Test
    fun `label keeps its contrast until the moving pill crosses the midpoint`() {
        highlightedSegment(0f, 3) shouldBe 0
        highlightedSegment(0.49f, 3) shouldBe 0
        highlightedSegment(0.5f, 3) shouldBe 1
        highlightedSegment(1f, 3) shouldBe 1
        highlightedSegment(1.49f, 3) shouldBe 1
        highlightedSegment(1.5f, 3) shouldBe 2
    }

    @Test
    fun `reverse travel crosses the same midpoint`() {
        highlightedSegment(2f, 3) shouldBe 2
        highlightedSegment(1.51f, 3) shouldBe 2
        highlightedSegment(1.49f, 3) shouldBe 1
        highlightedSegment(0.51f, 3) shouldBe 1
        highlightedSegment(0.49f, 3) shouldBe 0
    }

    @Test
    fun `highlight remains on an existing label when position overshoots`() {
        highlightedSegment(-0.7f, 3) shouldBe 0
        highlightedSegment(2.7f, 3) shouldBe 2
        highlightedSegment(5f, 1) shouldBe 0
    }

    @Test
    fun `an empty segment list has no highlighted label`() {
        highlightedSegment(0f, 0) shouldBe -1
    }
}
