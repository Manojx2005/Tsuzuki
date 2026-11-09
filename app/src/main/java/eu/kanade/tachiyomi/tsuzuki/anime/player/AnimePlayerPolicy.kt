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
