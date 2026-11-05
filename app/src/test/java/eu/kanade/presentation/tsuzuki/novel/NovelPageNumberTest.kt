package eu.kanade.presentation.tsuzuki.novel

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovelPageNumberTest {
    @Test
    fun `page indicator bounds transient indices to the available pages`() {
        novelPageNumber(-1, 18) shouldBe 1
        novelPageNumber(6, 18) shouldBe 7
        novelPageNumber(20, 18) shouldBe 18
        novelPageNumber(0, 1) shouldBe 1
    }
}
