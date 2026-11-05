package eu.kanade.presentation.tsuzuki.details

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.tachiyomi.ui.manga.search.ChapterSearchSnippet
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ChapterSearchSnippetTextTest {
    private val highlight = SpanStyle(fontWeight = FontWeight.Bold)

    @Test
    fun `keeps the source text and match position`() {
        val snippet = ChapterSearchSnippet("…told Ilse once…", 6, 10, 0.5f)

        val result = chapterSearchSnippetText(snippet, highlight)

        result.text shouldBe snippet.text + " (%.1f%%)".format(50f)
    }

    @Test
    fun `highlights only the supplied UTF-16 range`() {
        val snippet = ChapterSearchSnippet("📖 Ilse said Ilse", 13, 17, 0.8f)

        val result = chapterSearchSnippetText(snippet, highlight)
        val span = result.spanStyles.single()

        span.item shouldBe highlight
        span.start shouldBe 13
        span.end shouldBe 17
        result.text.substring(span.start, span.end) shouldBe "Ilse"
    }
}
