package eu.kanade.tachiyomi.tsuzuki.reader

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class MangaSpreadTest {
    @Test
    fun firstPageAndUnpairedTailStaySingle() {
        val items = pages(6)
        val (first, second, third, fourth, fifth) = items
        val sixth = items.last()

        val grouped = groupMangaPages(listOf(first, second, third, fourth, fifth, sixth), false) { true }

        assertEquals(
            listOf(first, MangaSpread(second, third, false), MangaSpread(fourth, fifth, false), sixth),
            grouped,
        )
    }

    @Test
    fun wideAndUnknownPagesStaySingle() {
        val items = pages(6)
        val (first, second, third, fourth, fifth) = items
        val sixth = items.last()
        val portraits = setOf(first, second, fourth, fifth)

        val grouped = groupMangaPages(listOf(first, second, third, fourth, fifth, sixth), false, portraits::contains)

        assertEquals(listOf(first, second, third, MangaSpread(fourth, fifth, false), sixth), grouped)
    }

    @Test
    fun chaptersStaySeparateWhenNoTransitionPageIsShown() {
        val firstChapter = pages(2)
        val nextChapter = pages(3)

        val grouped = groupMangaPages(firstChapter + nextChapter, false) { true }

        assertEquals(firstChapter + listOf(nextChapter[0], MangaSpread(nextChapter[1], nextChapter[2], false)), grouped)
    }

    @Test
    fun rtlPlacesEarlierPageOnRightWithoutChangingReadingOrder() {
        val (first, second, third) = pages(3)
        val spread = groupMangaPages(listOf(first, second, third), true) { true }.last() as MangaSpread

        assertEquals(second, spread.first)
        assertEquals(third, spread.second)
        assertEquals(second, spread.right)
        assertEquals(third, spread.left)
    }

    private fun pages(count: Int): List<ReaderPage> {
        val chapter = ReaderChapter(Chapter.create())
        return List(count) { index -> ReaderPage(index).apply { this.chapter = chapter } }
    }
}
