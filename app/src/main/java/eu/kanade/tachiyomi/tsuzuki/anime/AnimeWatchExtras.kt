package eu.kanade.tachiyomi.tsuzuki.anime

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.items.episode.model.Episode

/**
 * Aniyomi parity (owner 2026-11-28): "Download ahead" — while watching a downloaded episode whose next episode is
 * downloaded too, past [ANIME_DOWNLOAD_AHEAD_FRACTION] of it, queue the next N unseen episodes. 0 = off.
 */
internal fun animeDownloadAhead(preferences: PreferenceStore) =
    preferences.getInt("tsuzuki_anime_download_ahead", 0)

/** The choices Aniyomi offers (0 = off). */
internal val ANIME_DOWNLOAD_AHEAD_CHOICES = listOf(0, 2, 3, 5, 10)

/** Aniyomi checks at 35 % of the episode. */
internal const val ANIME_DOWNLOAD_AHEAD_FRACTION = 0.35

/**
 * Aniyomi parity: when an episode becomes seen, other unseen episodes with the same number (another release group's
 * copy, a re-upload) are marked seen too. Off by default, as in Aniyomi.
 */
internal fun animeMarkDuplicatesSeen(preferences: PreferenceStore) =
    preferences.getBoolean("tsuzuki_anime_mark_duplicates_seen", false)

/**
 * The episodes to queue ahead of [currentId] in [ordered] (playback order): only when the current and the next
 * episode are both [saved] (so streaming viewers never download unasked), then the next [amount] unseen episodes
 * after the next one that are not already in the queue ([inQueue]).
 */
internal fun animeEpisodesToDownloadAhead(
    ordered: List<Episode>,
    currentId: Long,
    saved: Set<Long>,
    inQueue: Set<Long>,
    amount: Int,
): List<Episode> {
    if (amount <= 0) return emptyList()
    val index = ordered.indexOfFirst { it.id == currentId }
    val next = ordered.getOrNull(index + 1) ?: return emptyList()
    if (index < 0 || currentId !in saved || next.id !in saved) return emptyList()
    return ordered.drop(index + 2).filter { !it.seen && it.id !in inQueue }.take(amount)
}

/** Unseen episodes that share [seen]'s recognised number (a negative number is "unknown" and never matches). */
internal fun animeDuplicateEpisodes(episodes: List<Episode>, seen: Episode): Set<Long> =
    if (seen.episodeNumber < 0) {
        emptySet()
    } else {
        episodes.filter { !it.seen && it.id != seen.id && it.episodeNumber == seen.episodeNumber }
            .mapTo(hashSetOf()) { it.id }
    }
