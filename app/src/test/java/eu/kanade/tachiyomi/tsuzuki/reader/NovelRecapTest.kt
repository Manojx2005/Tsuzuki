package eu.kanade.tachiyomi.tsuzuki.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class NovelRecapTest {
    private val first = "The archivist lowered the ledger onto the table."
    private val second = "She heard the bells ring from the locked tower."
    private val third = "A light came on in the window above the gate."
    private val fourth = "The stranger stepped out into the rain without a word."

    @Test
    fun extractsLastThreeParagraphsInReadingOrder() {
        assertEquals(
            listOf(second, third, fourth),
            novelRecapParagraphs("<p>$first</p><div>$second</div><p>$third</p><p>$fourth</p>"),
        )
    }

    @Test
    fun ignoresShortParagraphsAndTrailingAuthorNotes() {
        val content = """
            <p>$first</p>
            <p>...</p>
            <p>$second</p>
            <p>&nbsp;</p>
            <h2>A note from the author</h2>
            <p>Thanks for reading, and please follow the next update.</p>
        """.trimIndent()
        assertEquals(listOf(first, second), novelRecapParagraphs(content))
    }

    @Test
    fun handlesPlainTextAndLineBreakMarkup() {
        assertEquals(listOf(first, second), novelRecapParagraphs("$first\n\n$second\n—"))
        assertEquals(listOf(first, second), novelRecapParagraphs("$first<br><br>$second"))
    }

    @Test
    fun skipsNonProseMarkupAndPreservesDecodedText() {
        val prose = "The label read <North Gate> and the letters were fading."
        val html = "<p>The label read &lt;North Gate&gt; and the letters were fading.</p><p>$second</p>" +
            "<aside>Navigation and publisher notices should never appear in the recap.</aside>" +
            "<script>alert('Never insert source markup into the recap');</script>"
        assertEquals(listOf(prose, second), novelRecapParagraphs(html))
        assertEquals(emptyList<String>(), novelRecapParagraphs("<p>$first</p><p>End.</p>"))
    }

    @Test
    fun showsOnlyAfterAnInclusiveTwentyFourHourGap() {
        val lastRead = 1_700_000_000_000L
        val day = TimeUnit.HOURS.toMillis(24)
        assertFalse(shouldShowNovelRecap(null, lastRead + day))
        assertFalse(shouldShowNovelRecap(0L, lastRead + day))
        assertFalse(shouldShowNovelRecap(lastRead, lastRead - 1))
        assertFalse(shouldShowNovelRecap(lastRead, lastRead + day - 1))
        assertTrue(shouldShowNovelRecap(lastRead, lastRead + day))
        assertTrue(shouldShowNovelRecap(lastRead, lastRead + 2 * day))
    }
}
