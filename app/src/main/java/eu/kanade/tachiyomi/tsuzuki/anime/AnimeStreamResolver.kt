package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Hoster.Companion.toHosterList
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

private val hosterMethodCache = java.util.concurrent.ConcurrentHashMap<Class<*>, Boolean>()

/**
 * True when the source's class (or a parent below [AnimeHttpSource]) overrides one of the hoster methods, as
 * Aniyomi's `EpisodeLoader.checkHasHosters` does. A lib-16 extension that only implements the episode video list
 * inherits `hosterListParse`, which throws, so it must be asked for `getVideoList(episode)` instead.
 */
internal fun animeSourceHasHosters(source: AnimeSource): Boolean = hosterMethodCache.getOrPut(source.javaClass) {
    classOverridesHosterMethods(source.javaClass, AnimeHttpSource::class.java)
}

/** Whether [type] or a parent below [stopAt] declares a hoster method (the library's own defaults don't count). */
internal fun classOverridesHosterMethods(type: Class<*>, stopAt: Class<*>): Boolean {
    var current: Class<*>? = type
    while (current != null && current != stopAt && current != Any::class.java) {
        if (current.declaredMethods.any { it.name in HOSTER_METHODS }) return true
        current = current.superclass
    }
    return false
}

private val HOSTER_METHODS = setOf("getHosterList", "hosterListRequest", "hosterListParse")

internal object AnimeStreamResolver {
    /**
     * The episode's hosters in the source's own order (AY1). Lib 14, or a source without hoster methods, gets one
     * pseudo hoster ([Hoster.NO_HOSTER_LIST]) holding its `sortVideos()`-ed episode video list.
     */
    @Suppress("DEPRECATION")
    suspend fun hosters(source: AnimeSource, libVersion: Double, episode: SEpisode): List<Hoster> = callAnimeSource {
        val http = source as? AnimeHttpSource
        if (libVersion < 16 || !animeSourceHasHosters(source)) {
            val videos = source.getVideoList(episode)
            val sorted = if (http == null) videos else with(http) { videos.sortVideos() }
            sorted.toHosterList()
        } else {
            val hosters = source.getHosterList(episode)
            if (http == null) {
                hosters
            } else {
                with(http) {
                    hosters.sortHosters().map { hoster ->
                        val videos = hoster.videoList ?: return@map hoster
                        hoster.copy(videoList = videos.sortVideos())
                    }
                }
            }
        }
    }

    /** One hoster's videos in the source's order. */
    suspend fun hosterVideos(source: AnimeSource, hoster: Hoster): List<Video> = callAnimeSource {
        val videos = source.getVideoList(hoster)
        val http = source as? AnimeHttpSource
        if (http == null) videos else with(http) { videos.sortVideos() }
    }

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
    fun withSourceHeaders(source: AnimeSource, video: Video): Video = callAnimeSource {
        video.withDefaultHeaders((source as? AnimeHttpSource)?.headers)
    }

    @Suppress("DEPRECATION")
    suspend fun resolve(source: AnimeSource, libVersion: Double, video: Video): Video = callAnimeSource {
        val result = when {
            source !is AnimeHttpSource -> video
            // An initialized video must not be sent back into the extension.
            video.initialized -> video
            libVersion < 16 && video.videoUrl.toHttpUrlOrNull() == null -> video.copy(
                videoUrl = source.getVideoUrl(video),
            )
            libVersion >= 16 -> checkNotNull(source.resolveVideo(video)) { "The source could not resolve this video" }
            else -> video
        }
        val url = requireNotNull(result.videoUrl.toHttpUrlOrNull()) { "The video has no HTTP(S) stream URL" }
        val address = if (' ' in result.videoUrl) url.toString() else result.videoUrl
        if (result.initialized &&
            address == result.videoUrl
        ) {
            result
        } else {
            result.copy(videoUrl = address, initialized = true)
        }
    }
}
