package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object AnimeStreamResolver {
    @Suppress("DEPRECATION")
    suspend fun episodes(source: AnimeSource, libVersion: Double, anime: SAnime): List<SEpisode> =
        if (libVersion >= 17) {
            source.getAnimeEpisodeUpdate(anime, emptyList(), fetchDetails = false, fetchEpisodes = true).episodes
        } else {
            source.getEpisodeList(anime)
        }

    @Suppress("DEPRECATION")
    suspend fun videos(source: AnimeSource, libVersion: Double, episode: SEpisode): List<Video> {
        if (libVersion < 16) return source.getVideoList(episode)
        return source.getHosterList(episode).flatMap { it.videoList ?: source.getVideoList(it) }
    }

    @Suppress("DEPRECATION")
    suspend fun resolve(source: AnimeSource, libVersion: Double, video: Video): Video {
        val result = when {
            source !is AnimeHttpSource -> video
            libVersion < 16 && video.videoUrl.toHttpUrlOrNull() == null -> video.copy(
                videoUrl = source.getVideoUrl(video),
            )
            libVersion >= 16 -> checkNotNull(source.resolveVideo(video)) { "The source could not resolve this video" }
            else -> video
        }
        require(result.videoUrl.toHttpUrlOrNull() != null) { "The video has no HTTP(S) stream URL" }
        return result
    }
}
