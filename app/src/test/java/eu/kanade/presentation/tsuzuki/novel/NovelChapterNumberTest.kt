package eu.kanade.presentation.tsuzuki.novel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NovelChapterNumberTest {
    @Test
    fun `tablet sidebar separates the source number and title without duplicating either`() {
        assertEquals(NovelChapterHeading(129.0, "The Gate & The Key"), novelChapterHeading("129: The Gate & The Key"))
        assertEquals(
            NovelChapterHeading(12.5, "Volume 4 · Return"),
            novelChapterHeading("Volume 4 — Chapter 12.5: Return"),
        )
        assertEquals(NovelChapterHeading(3.0, "再会"), novelChapterHeading("第3話　再会"))
        assertEquals(NovelChapterHeading(1.0, ""), novelChapterHeading("Chapter 1"))
    }

    @Test
    fun `numbered source title takes precedence over the stored index`() {
        val title = "2: Story Branching"

        assertEquals(title, novelReaderChapterLabel(title, 1.0, true, true, "Ch."))
        assertEquals("Chapter 2", novelReaderChapterLabel(title, 1.0, true, false, "Chapter"))
        assertEquals(title, novelReaderChapterLabel(title, 1.0, false, true, "Ch."))
    }

    @Test
    fun `explicit decimal and volume chapter numbers are read from the heading`() {
        assertEquals(2.5, novelTitleChapterNumber("2.5: Interlude"))
        assertEquals(12.5, novelTitleChapterNumber("Volume 4 — Chapter 12,5: Return"))
        assertEquals(3.0, novelTitleChapterNumber("第3話　再会"))
        assertEquals(0.0, novelTitleChapterNumber("Chapter 0: Prologue"))
    }

    @Test
    fun `incidental numbers do not replace the stored number`() {
        val title = "The Last 5 Minutes"

        assertNull(novelTitleChapterNumber(title))
        assertNull(novelTitleChapterNumber("20,000 Leagues Under the Sea"))
        assertNull(novelTitleChapterNumber("Vol. 3: Interlude"))
        assertEquals("Ch. 6: $title", novelReaderChapterLabel(title, 6.0, true, true, "Ch."))
        assertEquals(title, novelReaderChapterLabel(title, -1.0, true, true, "Ch."))
    }
}
