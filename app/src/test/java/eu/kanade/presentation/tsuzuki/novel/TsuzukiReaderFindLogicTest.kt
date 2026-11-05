package eu.kanade.presentation.tsuzuki.novel

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiReaderFindLogicTest {
    @Test
    fun `counter shows one-based position of the active match`() {
        findCounterText(0, 7) shouldBe "1/7"
        findCounterText(2, 7) shouldBe "3/7"
        findCounterText(6, 7) shouldBe "7/7"
    }

    @Test
    fun `counter reads 0 of 0 without matches and clamps strays`() {
        findCounterText(0, 0) shouldBe "0/0"
        findCounterText(3, -1) shouldBe "0/0"
        findCounterText(9, 4) shouldBe "4/4"
        findCounterText(-2, 4) shouldBe "1/4"
    }
}
