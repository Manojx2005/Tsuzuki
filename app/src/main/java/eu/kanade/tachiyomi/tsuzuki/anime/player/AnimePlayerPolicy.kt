package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.animeEpisodeNumberOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import tachiyomi.domain.items.episode.model.Episode

private val qualityLabel = Regex("""\b(\d{3,4})p\b""", RegexOption.IGNORE_CASE)

internal val Video.animeResolution: Int?
    get() = resolution?.takeIf { it > 0 } ?: qualityLabel.find(videoTitle)?.groupValues?.get(1)?.toIntOrNull()

internal fun rankAnimeVideos(videos: List<Video>, preferred: Int): List<Video> = videos.sortedWith(
    compareBy<Video> { video ->
        val quality = video.animeResolution
        when {
            quality == null -> 2
            preferred <= 0 || quality <= preferred -> 0
            else -> 1
        }
    }.thenBy { video ->
        val quality = video.animeResolution ?: 0
        if (preferred > 0 && quality > preferred) quality else -quality
    }.thenByDescending { it.bitrate ?: 0 },
)

internal class AnimeStreamQueue(videos: List<Video>, preferred: Int) {
    private val candidates = rankAnimeVideos(videos, preferred).iterator()

    suspend fun next(resolve: suspend (Video) -> Video): Video? {
        while (candidates.hasNext()) {
            try {
                return resolve(candidates.next())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The source may offer an expired embed alongside other hosts.
            }
        }
        return null
    }
}

internal fun adjacentAnimeEpisodes(episodes: List<Episode>, id: Long): Pair<Episode?, Episode?> {
    val ordered = episodes.sortedWith(animeEpisodeNumberOrder)
    val current = ordered.indexOfFirst { it.id == id }
    if (current < 0) return null to null
    return ordered.getOrNull(current - 1) to ordered.getOrNull(current + 1)
}

internal suspend fun repeatAnimeCheckpoints(save: () -> Unit): Nothing {
    while (true) {
        delay(10_000)
        save()
    }
}

/** A saved position this close to the end (seconds) is treated as "finished", not "resume here". */
internal const val ANIME_RESUME_END_MARGIN_SECONDS = 10.0
internal const val ANIME_RESUME_END_FRACTION = 0.9

/** True when starting playback at [start] would begin at (or within the closing seconds of) a file of [duration]. */
internal fun animeStartIsAtEnd(start: Double, duration: Double): Boolean =
    start.isFinite() && duration.isFinite() && start > 0 && duration > 0 &&
        (start >= duration - ANIME_RESUME_END_MARGIN_SECONDS || start >= duration * ANIME_RESUME_END_FRACTION)

/**
 * Where to start an episode. A seen episode starts over; a saved position at the end of the known duration
 * ([knownDuration] is 0 when it was never stored) starts over too instead of opening on the last frame.
 */
internal fun animeResumeStart(saved: Double, seen: Boolean, knownDuration: Double): Double = when {
    !saved.isFinite() || saved <= 0 || seen -> 0.0
    animeStartIsAtEnd(saved, knownDuration) -> 0.0
    else -> saved
}

internal enum class AnimePlayAfterEnd { SEEK_TO_START, RELOAD }

/** Play on a finished file: seek back when the duration is known, otherwise load the stream again from 0. */
internal fun animePlayAfterEnd(duration: Double): AnimePlayAfterEnd =
    if (duration.isFinite() && duration > 0) AnimePlayAfterEnd.SEEK_TO_START else AnimePlayAfterEnd.RELOAD
