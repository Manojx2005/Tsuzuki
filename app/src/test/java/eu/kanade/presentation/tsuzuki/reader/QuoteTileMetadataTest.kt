package eu.kanade.presentation.tsuzuki.reader

import eu.kanade.tachiyomi.ui.reader.quote.Quote
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class QuoteTileMetadataTest {
    @Test
    fun `chapter label displays a one-based paragraph when known`() {
        val quote = Quote(chapterName = "Chapter 12", content = "The quote", paragraphIndex = 0)

        quoteChapterLabel(quote) shouldBe "Chapter 12 · ¶1"
        quoteChapterLabel(quote.copy(paragraphIndex = null)) shouldBe "Chapter 12"
    }

    @Test
    fun `chapter label handles manually added quotes`() {
        val quote = Quote(chapterName = "", content = "The quote")

        quoteChapterLabel(quote) shouldBe ""
        quoteChapterLabel(quote.copy(paragraphIndex = 4)) shouldBe "¶5"
    }

    @Test
    fun `sharing keeps the quote verbatim and includes an available chapter`() {
        val quote = Quote(chapterName = "Chapter 12", content = "First line\nSecond line")

        quoteForSharing(quote) shouldBe "First line\nSecond line\n\nChapter 12"
        quoteForSharing(quote.copy(chapterName = " ")) shouldBe quote.content
    }

    @Test
    fun exportKeepsSavedOrderAndAvailableChapterNames() {
        val first = Quote(chapterName = "Chapter 12", content = "最初の引用")
        val second = Quote(chapterName = "", content = "Second quote")
        val text = buildString { writeQuoteExport(this, "My novel", listOf(first, second)) }

        text shouldBe "My novel\n\n最初の引用\n\nChapter 12\n\nSecond quote\n"
    }
}
