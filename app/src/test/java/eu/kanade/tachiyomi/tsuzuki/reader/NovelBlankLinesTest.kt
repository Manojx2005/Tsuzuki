package eu.kanade.tachiyomi.tsuzuki.reader

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovelBlankLinesTest {

    private fun strip(text: String): String {
        val sb = StringBuilder(text)
        blankLineRanges(text).asReversed().forEach { (s, e) -> sb.delete(s, e) }
        return sb.toString()
    }

    @Test
    fun `legacy paragraph breaks collapse to one line break`() {
        strip("First.\n\nSecond.\n\nThird.\n\n") shouldBe "First.\nSecond.\nThird.\n"
    }

    @Test
    fun `nbsp-only and whitespace lines are removed`() {
        strip("One.\n\n \u00A0\n\nTwo.") shouldBe "One.\nTwo."
    }

    @Test
    fun `inline image lines are kept`() {
        strip("Text.\n\n\uFFFC\n\nMore.") shouldBe "Text.\n\uFFFC\nMore."
    }

    @Test
    fun `text without blank lines is unchanged`() {
        strip("a\nb\nc") shouldBe "a\nb\nc"
        strip("") shouldBe ""
    }
}
