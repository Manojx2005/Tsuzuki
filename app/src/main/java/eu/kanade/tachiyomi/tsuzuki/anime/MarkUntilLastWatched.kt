package eu.kanade.tachiyomi.tsuzuki.anime

import tachiyomi.domain.items.episode.model.Episode

/** Unwatched episodes before the furthest completed or started episode, in playback order. */
internal fun episodesBeforeLastWatched(ordered: List<Episode>): List<Episode> {
    val furthest = ordered.indexOfLast { it.seen || it.lastSecondSeen > 0L }
    return if (furthest <= 0) emptyList() else ordered.take(furthest).filterNot(Episode::seen)
}
