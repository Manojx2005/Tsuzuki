package eu.kanade.tachiyomi.tsuzuki.anime

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

internal data class AnimeEpisodeDisplay(
    val sortByDate: Boolean = false,
    val ascending: Boolean = false,
    val unseenOnly: Boolean = false,
    val bookmarkedOnly: Boolean = false,
) {
    fun apply(episodes: List<Episode>): List<Episode> {
        val order = if (sortByDate) {
            compareBy(Episode::dateUpload).then(animeEpisodeNumberOrder)
        } else {
            animeEpisodeNumberOrder
        }
        return episodes.filter { (!unseenOnly || !it.seen) && (!bookmarkedOnly || it.bookmark) }
            .sortedWith(if (ascending) order else order.reversed())
    }

    // Keep Aniyomi's low flag bits intact for backup compatibility.
    fun toFlags(previous: Long): Long = (previous and MASK.inv()) or
        (if (sortByDate) DATE else 0L) or (if (ascending) ASCENDING else 0L) or
        (if (unseenOnly) UNSEEN else 0L) or (if (bookmarkedOnly) BOOKMARKED else 0L)

    companion object {
        private const val DATE = 1L shl 32
        private const val ASCENDING = 1L shl 33
        private const val UNSEEN = 1L shl 34
        private const val BOOKMARKED = 1L shl 35
        private const val MASK = DATE or ASCENDING or UNSEEN or BOOKMARKED

        fun fromFlags(flags: Long) = AnimeEpisodeDisplay(
            sortByDate = flags and DATE != 0L,
            ascending = flags and ASCENDING != 0L,
            unseenOnly = flags and UNSEEN != 0L,
            bookmarkedOnly = flags and BOOKMARKED != 0L,
        )
    }
}
