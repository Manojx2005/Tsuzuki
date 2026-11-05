package eu.kanade.presentation.tsuzuki.details

import eu.kanade.tachiyomi.ui.reader.quote.Quote
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class TsuzukiNotebookTest {
    private fun chapter(id: Long, name: String, sourceOrder: Long) = Chapter(
        id = id, mangaId = 1, read = false, bookmark = false, lastPageRead = 0, dateFetch = 0,
        sourceOrder = sourceOrder, url = "u$id", name = name, dateUpload = 0, chapterNumber = id.toDouble(),
        scanlator = null, lastModifiedAt = 0, version = 0, memo = JsonObject(emptyMap()),
    )

    // Source order: the highest value is the oldest chapter.
    private val chapters = listOf(chapter(1, "Chapter 1", 2), chapter(2, "Chapter 2", 1), chapter(3, "Chapter 3", 0))

    private fun quote(id: String, chapter: String, ts: Long, para: Int? = null) =
        Quote(id = id, chapterName = chapter, content = "text $id", timestamp = ts, paragraphIndex = para)

    @Test
    fun `chapter is matched by name then ignoring case and spaces`() {
        resolveQuoteChapterId("Chapter 2", chapters) shouldBe 2L
        resolveQuoteChapterId("  chapter 3 ", chapters) shouldBe 3L
        resolveQuoteChapterId("Missing", chapters) shouldBe null
        resolveQuoteChapterId("", chapters) shouldBe null
    }

    @Test
    fun `chapter sort follows reading order with unknown chapters last`() {
        val quotes = listOf(
            quote("a", "Chapter 3", 10),
            quote("b", "Gone chapter", 20),
            quote("c", "Chapter 1", 30, para = 5),
            quote("d", "Chapter 1", 40, para = 2),
            quote("e", "Chapter 1", 5),
        )
        val groups = groupNotebookQuotes(quotes, chapters, NotebookSort.CHAPTER)

        groups.map { it.chapterName } shouldBe listOf("Chapter 1", "Chapter 3", "Gone chapter")
        groups[0].quotes.map { it.id } shouldBe listOf("d", "c", "e")
        groups[0].chapterId shouldBe 1L
        groups[2].chapterId shouldBe null
    }

    @Test
    fun `newest sort orders groups and quotes by most recent first`() {
        val quotes = listOf(
            quote("a", "Chapter 1", 10),
            quote("b", "Chapter 2", 50),
            quote("c", "Chapter 1", 60),
        )
        val groups = groupNotebookQuotes(quotes, chapters, NotebookSort.NEWEST)

        groups.map { it.chapterName } shouldBe listOf("Chapter 1", "Chapter 2")
        groups[0].quotes.map { it.id } shouldBe listOf("c", "a")
    }

    @Test
    fun `share text lists every quote with its chapter`() {
        val groups = groupNotebookQuotes(
            listOf(quote("a", "Chapter 1", 1), quote("b", "", 2)),
            chapters,
            NotebookSort.CHAPTER,
        )
        notebookShareText("Book", groups) shouldBe "Book\n\ntext a\n— Chapter 1\n\ntext b"
    }

    @Test
    fun `empty input gives no groups`() {
        groupNotebookQuotes(emptyList(), chapters, NotebookSort.CHAPTER) shouldBe emptyList()
    }
}
