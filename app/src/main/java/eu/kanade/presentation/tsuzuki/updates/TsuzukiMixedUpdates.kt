package eu.kanade.presentation.tsuzuki.updates

import eu.kanade.presentation.tsuzuki.anime.MixedTimelineRow
import eu.kanade.presentation.tsuzuki.anime.mergeNewestFirst
import eu.kanade.presentation.updates.UpdatesUiModel
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Updates › All with anime (AY-INT item 2, the per-chapter list): new episodes folded in with new chapters by
 * time (chapter `dateFetch`, episode timestamp), day headers rebuilt over both. With no anime the chapter list
 * comes back unchanged.
 */
internal fun mergeAnimeIntoUpdates(
    chapters: List<UpdatesUiModel>,
    anime: List<AnimeUpdateEpisode>,
    timeZone: TimeZone,
): List<UpdatesUiModel> {
    if (anime.isEmpty()) return chapters
    val items = chapters.filterIsInstance<UpdatesUiModel.Item>().map { it.item }
    val rows = mergeNewestFirst(items, { it.update.dateFetch }, anime, { it.timestamp })
    val result = ArrayList<UpdatesUiModel>(rows.size + 8)
    var day: LocalDate? = null
    for (row in rows) {
        val rowDay = Instant.fromEpochMilliseconds(row.at).toLocalDateTime(timeZone).date
        if (rowDay != day) {
            result += UpdatesUiModel.Header(rowDay)
            day = rowDay
        }
        result += when (row) {
            is MixedTimelineRow.Book -> UpdatesUiModel.Item(row.item)
            is MixedTimelineRow.Anime -> UpdatesUiModel.Anime(row.item)
        }
    }
    return result
}

/** One anime's new episodes, shown as a group next to the book groups in grouped Updates › All. */
internal data class AnimeUpdateGroup(
    val animeId: Long,
    val title: String,
    val cover: String?,
    val count: Int,
    val latestName: String,
    val latest: Long,
)

/** Groups new episodes by anime, newest group first. */
internal fun animeUpdateGroups(episodes: List<AnimeUpdateEpisode>): List<AnimeUpdateGroup> =
    episodes.groupBy { it.animeId }.map { (animeId, list) ->
        val newest = list.maxBy { it.timestamp }
        AnimeUpdateGroup(animeId, newest.title, newest.previewUrl, list.size, newest.name, newest.timestamp)
    }.sortedByDescending { it.latest }

/** Book groups and anime groups by their latest update, newest first (books win ties). */
internal fun <B> mergeUpdateGroups(
    books: List<B>,
    bookLatest: (B) -> Long,
    anime: List<AnimeUpdateGroup>,
): List<MixedTimelineRow<B, AnimeUpdateGroup>> = mergeNewestFirst(books, bookLatest, anime, { it.latest })

/** Unwatched episodes that "Save all" would queue, excluding entries already in the download queue. */
internal fun episodesToSave(episodes: List<AnimeUpdateEpisode>, inQueue: Set<Long>): List<AnimeUpdateEpisode> =
    episodes.filter { !it.seen && it.episodeId !in inQueue }.distinctBy { it.episodeId }
