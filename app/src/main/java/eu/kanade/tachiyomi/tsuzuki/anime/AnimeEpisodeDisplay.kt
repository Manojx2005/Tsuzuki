package eu.kanade.tachiyomi.tsuzuki.anime

import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.items.episode.model.Episode

internal val animeEpisodeNumberOrder =
    compareBy<Episode> { it.episodeNumber.takeIf { number -> number >= 0 } ?: Double.POSITIVE_INFINITY }
        .thenBy(Episode::sourceOrder)
        .thenBy(Episode::id)

internal fun chooseAnimeEpisode(episodes: List<Episode>, recentInProgressId: Long?): Episode? {
    val unseen = episodes.filterNot(Episode::seen)
    val inProgress = unseen.filter { it.lastSecondSeen > 0 }
    return inProgress.firstOrNull { it.id == recentInProgressId }
        ?: inProgress.minWithOrNull(animeEpisodeNumberOrder)
        ?: unseen.minWithOrNull(animeEpisodeNumberOrder)
}

/**
 * The episode list's sort and filters, in the book chapter dialog's shape: three-state Unwatched, Downloaded and
 * Bookmarked filters (show only / hide / off). Stored in the anime's episode flags; the original "only" bits keep their
 * meaning, so settings saved before the tri-state filters (and backups) read the same.
 */
internal data class AnimeEpisodeDisplay(
    val sortByDate: Boolean = false,
    val ascending: Boolean = false,
    val unseen: TriState = TriState.DISABLED,
    val bookmarked: TriState = TriState.DISABLED,
    val downloaded: TriState = TriState.DISABLED,
) {
    val hasFilters: Boolean get() = listOf(unseen, bookmarked, downloaded).any { it != TriState.DISABLED }

    /** [saved] holds the ids of downloaded episodes, for the Downloaded filter. */
    fun apply(episodes: List<Episode>, saved: Set<Long> = emptySet()): List<Episode> {
        val order = if (sortByDate) {
            compareBy(Episode::dateUpload).then(animeEpisodeNumberOrder)
        } else {
            animeEpisodeNumberOrder
        }
        return episodes.filter {
            unseen.accepts(!it.seen) && bookmarked.accepts(it.bookmark) && downloaded.accepts(it.id in saved)
        }.sortedWith(if (ascending) order else order.reversed())
    }

    // Keep Aniyomi's low flag bits intact for backup compatibility.
    fun toFlags(previous: Long): Long = (previous and MASK.inv()) or
        (if (sortByDate) DATE else 0L) or (if (ascending) ASCENDING else 0L) or
        unseen.bits(UNSEEN, UNSEEN_NOT) or bookmarked.bits(BOOKMARKED, BOOKMARKED_NOT) or
        downloaded.bits(DOWNLOADED, DOWNLOADED_NOT)

    companion object {
        private const val DATE = 1L shl 32
        private const val ASCENDING = 1L shl 33
        private const val UNSEEN = 1L shl 34
        private const val BOOKMARKED = 1L shl 35
        private const val UNSEEN_NOT = 1L shl 36
        private const val BOOKMARKED_NOT = 1L shl 37
        private const val DOWNLOADED = 1L shl 38
        private const val DOWNLOADED_NOT = 1L shl 39
        private const val MASK = DATE or ASCENDING or UNSEEN or BOOKMARKED or UNSEEN_NOT or BOOKMARKED_NOT or
            DOWNLOADED or DOWNLOADED_NOT

        private fun TriState.bits(only: Long, not: Long): Long = when (this) {
            TriState.DISABLED -> 0L
            TriState.ENABLED_IS -> only
            TriState.ENABLED_NOT -> not
        }

        private fun triState(flags: Long, only: Long, not: Long): TriState = when {
            flags and only != 0L -> TriState.ENABLED_IS
            flags and not != 0L -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }

        fun fromFlags(flags: Long) = AnimeEpisodeDisplay(
            sortByDate = flags and DATE != 0L,
            ascending = flags and ASCENDING != 0L,
            unseen = triState(flags, UNSEEN, UNSEEN_NOT),
            bookmarked = triState(flags, BOOKMARKED, BOOKMARKED_NOT),
            downloaded = triState(flags, DOWNLOADED, DOWNLOADED_NOT),
        )
    }
}

private fun TriState.accepts(value: Boolean): Boolean = when (this) {
    TriState.DISABLED -> true
    TriState.ENABLED_IS -> value
    TriState.ENABLED_NOT -> !value
}
