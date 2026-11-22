package eu.kanade.presentation.tsuzuki.anime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TsuzukiMixedTimelineTest {

    private data class B(val name: String, val t: Long)
    private data class A(val name: String, val t: Long)

    private fun names(rows: List<MixedTimelineRow<B, A>>) = rows.map {
        when (it) {
            is MixedTimelineRow.Book -> it.item.name
            is MixedTimelineRow.Anime -> it.item.name
        }
    }

    private fun merge(books: List<B>, anime: List<A>) = mergeNewestFirst(books, { it.t }, anime, { it.t })

    @Test
    fun `interleaves books and anime newest first`() {
        val rows = merge(listOf(B("b1", 10), B("b2", 30)), listOf(A("a1", 20), A("a2", 40)))
        assertEquals(listOf("a2", "b2", "a1", "b1"), names(rows))
    }

    @Test
    fun `book wins a tie`() {
        assertEquals(listOf("b", "a"), names(merge(listOf(B("b", 5)), listOf(A("a", 5)))))
    }

    @Test
    fun `no anime leaves the book list in time order`() {
        val books = listOf(B("old", 1), B("new", 9), B("mid", 5))
        assertEquals(listOf("new", "mid", "old"), names(merge(books, emptyList())))
        assertEquals(emptyList<String>(), names(merge(emptyList(), emptyList())))
    }

    @Test
    fun `groups by local day and keeps order inside a day`() {
        val day = 86_400_000L
        val rows = merge(listOf(B("b", day + 100)), listOf(A("a", day + 200), A("old", 100)))
        val grouped = groupByDay(rows, TimeZone.UTC)
        assertEquals(listOf(LocalDate(1970, 1, 2), LocalDate(1970, 1, 1)), grouped.keys.toList())
        assertEquals(listOf("a", "b"), names(grouped.getValue(LocalDate(1970, 1, 2))))
    }

    @Test
    fun `day boundary follows the time zone`() {
        // 1970-01-01 23:30 UTC is already 1970-01-02 in Tokyo.
        val rows = merge(listOf(B("late", 23 * 3_600_000L + 1_800_000L)), emptyList())
        assertEquals(
            listOf(LocalDate(1970, 1, 2)),
            groupByDay(rows, TimeZone.of("Asia/Tokyo")).keys.toList(),
        )
    }
}
