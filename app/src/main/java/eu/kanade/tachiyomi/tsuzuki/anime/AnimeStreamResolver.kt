package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal inline fun <T> callAnimeSource(block: () -> T): T = try {
    block()
} catch (e: LinkageError) {
    AnimeLog.e("Incompatible anime source (extension linked against a class this app does not provide)", e)
    throw IllegalStateException("Incompatible anime source", e)
}

/** `video.headers ?: source.headers`: a video with its own headers keeps exactly those. */
internal fun Video.withDefaultHeaders(defaults: Headers?): Video =
    if (headers != null || defaults == null) this else copy(headers = defaults)

internal object AnimeStreamResolver {
    @Suppress("DEPRECATION")
    suspend fun episodes(source: AnimeSource, libVersion: Double, anime: SAnime): List<SEpisode> = callAnimeSource {
        if (libVersion >= 17) {
            source.getAnimeEpisodeUpdate(anime, emptyList(), fetchDetails = false, fetchEpisodes = true).episodes
        } else {
            source.getEpisodeList(anime)
        }
    }

    @Suppress("DEPRECATION")
    suspend fun videos(source: AnimeSource, libVersion: Double, episode: SEpisode): List<Video> = callAnimeSource {
        // Same as Aniyomi's loader: the source's own sortHosters()/sortVideos() decide the order it offers.
        val http = source as? AnimeHttpSource
        if (libVersion < 16) {
            val videos = source.getVideoList(episode)
            if (http == null) videos else with(http) { videos.sortVideos() }
        } else {
            val hosters = source.getHosterList(episode).let { list ->
                if (http == null) list else with(http) { list.sortHosters() }
            }
            hosters.flatMap { hoster ->
                val videos = hoster.videoList ?: source.getVideoList(hoster)
                if (http == null) videos else with(http) { videos.sortVideos() }
            }
        }
    }

    /** The source's own headers (User-Agent, Referer, ...) for a video that brings none, as Aniyomi's player does. */
    fun withSourceHeaders(source: AnimeSource, video: Video): Video =
        video.withDefaultHeaders((source as? AnimeHttpSource)?.headers)

    @Suppress("DEPRECATION")
    suspend fun resolve(source: AnimeSource, libVersion: Double, video: Video): Video = callAnimeSource {
        val result = when {
            source !is AnimeHttpSource -> video
            libVersion < 16 && video.videoUrl.toHttpUrlOrNull() == null -> video.copy(
                videoUrl = source.getVideoUrl(video),
            )
            libVersion >= 16 -> checkNotNull(source.resolveVideo(video)) { "The source could not resolve this video" }
            else -> video
        }
        require(result.videoUrl.toHttpUrlOrNull() != null) { "The video has no HTTP(S) stream URL" }
        result
    }
}
