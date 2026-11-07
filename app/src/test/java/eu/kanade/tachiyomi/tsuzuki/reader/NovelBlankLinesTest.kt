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

    @Test
    fun `lines holding only invisible characters count as blank`() {
        blankLineRanges("a\n\u200B\u00A0\nb").size shouldBe 1
    }

    @Test
    fun `empty html paragraphs and long br runs collapse`() {
        collapseBlankHtml("<p>a</p><p>&nbsp;</p><P class=x> <br/> </P><p>b</p>") shouldBe "<p>a</p><p>b</p>"
        collapseBlankHtml("a<br><br/><br /><br>b") shouldBe "a<br><br>b"
        collapseBlankHtml("<p>keep me</p><p><img src=x></p>") shouldBe "<p>keep me</p><p><img src=x></p>"
    }
}
