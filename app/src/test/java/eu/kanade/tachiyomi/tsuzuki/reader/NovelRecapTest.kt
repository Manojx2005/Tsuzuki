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
    fun keepsHardWrappedPlainTextLinesInTheirParagraphs() {
        val wrapped = "The archivist lowered the ledger\nonto the table.\n\n" +
            "She heard the bells ring\nfrom the locked tower."
        assertEquals(listOf(first, second), novelRecapParagraphs(wrapped))
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

    @Test
    fun completedRenderedParagraphsKeepLiteralMarkupAndUseOnlyPassedProse() {
        val literal = "The sign read <North Gate> and nobody knew who had written it."
        assertEquals(
            listOf(second, third, literal),
            novelRecapCompletedParagraphs(listOf(first, second, third, literal)),
        )
        assertEquals(
            listOf(first, second),
            novelRecapCompletedParagraphs(listOf(first, second, "Author's notes", third)),
        )
        assertEquals(emptyList<String>(), novelRecapCompletedParagraphs(listOf(first, "Short.")))
    }

    @Test
    fun labelJoinsTheKnownPartsLikeTheBoard() {
        assertEquals("Previously · Ch. 4 · 2 days ago", novelRecapLabel("Previously", "Ch. 4", "2 days ago"))
        assertEquals("Previously · 2 days ago", novelRecapLabel("Previously", null, "2 days ago"))
        assertEquals("Previously · Ch. 4", novelRecapLabel("Previously", "  Ch. 4 ", null))
        assertEquals("Previously", novelRecapLabel("Previously", " ", ""))
    }

    @Test
    fun modeSeparatesFreshOpensFromResumesAndRespectsTheGap() {
        assertEquals(NovelRecapMode.None, novelRecapMode(due = false, entryProgress = 0.4f))
        assertEquals(NovelRecapMode.None, novelRecapMode(due = false, entryProgress = 0f))
        assertEquals(NovelRecapMode.CardAboveText, novelRecapMode(due = true, entryProgress = 0f))
        assertEquals(NovelRecapMode.ResumeOverlay, novelRecapMode(due = true, entryProgress = 0.01f))
        assertEquals(NovelRecapMode.ResumeOverlay, novelRecapMode(due = true, entryProgress = 0.98f))
        // Reopening a finished chapter shows its last screen: no overlay repeating what is visible.
        assertEquals(NovelRecapMode.None, novelRecapMode(due = true, entryProgress = 0.99f))
        assertEquals(NovelRecapMode.None, novelRecapMode(due = true, entryProgress = 1f))
    }

    @Test
    fun overlayGoesAwayAfterTwoPointsOfProgressOrAPagedTurn() {
        assertFalse(novelRecapScrolledAway(40, 40, 3, 3))
        assertFalse(novelRecapScrolledAway(40, 41, 3, 3))
        assertTrue(novelRecapScrolledAway(40, 42, 3, 3))
        assertTrue(novelRecapScrolledAway(40, 38, 3, 3))
        assertTrue(novelRecapScrolledAway(40, 40, 3, 4))
    }
}
