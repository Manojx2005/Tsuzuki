package eu.kanade.presentation.tsuzuki.anime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * One row of a list that shows books and anime together (History › All, Updates › All),
 * ordered by [at] (epoch milliseconds).
 */
internal sealed interface MixedTimelineRow<out B, out A> {
    val at: Long

    data class Book<B>(val item: B, override val at: Long) : MixedTimelineRow<B, Nothing>

    data class Anime<A>(val item: A, override val at: Long) : MixedTimelineRow<Nothing, A>
}

/**
 * Merges two lists newest first. Each input may arrive in any order. On equal times the book
 * row comes first, so a list with no anime is exactly the book list in time order.
 */
internal fun <B, A> mergeNewestFirst(
    books: List<B>,
    bookTime: (B) -> Long,
    anime: List<A>,
    animeTime: (A) -> Long,
): List<MixedTimelineRow<B, A>> {
    val rows = books.map { MixedTimelineRow.Book(it, bookTime(it)) } +
        anime.map { MixedTimelineRow.Anime(it, animeTime(it)) }
    // sortedWith is stable, and books were added first, so ties keep books ahead.
    return rows.sortedWith(compareByDescending { it.at })
}

/** Groups merged rows by local calendar day, keeping the newest-first order inside each day. */
internal fun <R : MixedTimelineRow<*, *>> groupByDay(rows: List<R>, timeZone: TimeZone): Map<LocalDate, List<R>> =
    rows.groupBy { Instant.fromEpochMilliseconds(it.at).toLocalDateTime(timeZone).date }
