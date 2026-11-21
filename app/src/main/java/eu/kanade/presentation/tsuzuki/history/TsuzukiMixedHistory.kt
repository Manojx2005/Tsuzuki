package eu.kanade.presentation.tsuzuki.history

import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.presentation.tsuzuki.anime.MixedTimelineRow
import eu.kanade.presentation.tsuzuki.anime.mergeNewestFirst
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.history.anime.model.AnimeHistoryDisplay
import kotlin.time.Instant

/**
 * History › All with anime (AY-INT item 3): the paged book list with watched episodes folded in by time, and the
 * day headers rebuilt over both.
 *
 * The book list is paged, so while more pages may follow ([allBooksLoaded] false) an anime row older than the
 * oldest loaded book is held back; it appears once the pages reach it. Anime rows follow the same search as the
 * book list (title or episode name). With no anime the book list comes back unchanged.
 */
internal fun mergeAnimeIntoHistory(
    books: List<HistoryUiModel>,
    anime: List<AnimeHistoryDisplay>,
    searchQuery: String?,
    allBooksLoaded: Boolean,
    timeZone: TimeZone,
): List<HistoryUiModel> {
    val query = searchQuery.orEmpty().trim()
    val matching = anime.filter {
        query.isEmpty() || it.title.contains(query, ignoreCase = true) ||
            it.episodeName.contains(query, ignoreCase = true)
    }
    if (matching.isEmpty()) return books
    val bookItems = books.filterIsInstance<HistoryUiModel.Item>().map { it.item }
    val oldestBook = bookItems.mapNotNull { it.readAt?.time }.minOrNull()
    val shown = if (allBooksLoaded || oldestBook == null) {
        matching
    } else {
        matching.filter { it.lastSeen.time >= oldestBook }
    }
    val rows = mergeNewestFirst(bookItems, { it.readAt?.time ?: 0L }, shown, { it.lastSeen.time })
    val result = ArrayList<HistoryUiModel>(rows.size + 8)
    var day: LocalDate? = null
    for (row in rows) {
        val rowDay = Instant.fromEpochMilliseconds(row.at).toLocalDateTime(timeZone).date
        if (rowDay != day) {
            result += HistoryUiModel.Header(rowDay)
            day = rowDay
        }
        result += when (row) {
            is MixedTimelineRow.Book -> HistoryUiModel.Item(row.item)
            is MixedTimelineRow.Anime -> HistoryUiModel.Anime(row.item)
        }
    }
    return result
}
