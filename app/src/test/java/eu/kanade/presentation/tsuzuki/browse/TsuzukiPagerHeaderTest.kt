package eu.kanade.presentation.tsuzuki.browse

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiPagerHeaderTest {

    @Test
    fun `neighbouring pages animate`() {
        pagerJumps(0, 1) shouldBe false
        pagerJumps(3, 2) shouldBe false
        pagerJumps(1, 1) shouldBe false
    }

    @Test
    fun `skipping a page jumps instead of sliding through it`() {
        // Novel sources (0) -> Novel extensions (2) would pass Manga sources (1).
        pagerJumps(0, 2) shouldBe true
        pagerJumps(3, 0) shouldBe true
    }
}
