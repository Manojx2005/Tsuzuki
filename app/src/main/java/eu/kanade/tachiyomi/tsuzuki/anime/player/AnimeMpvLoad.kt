package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog

/** mpv options an extension may set for its own stream; everything else (files, scripts, logging, TLS...) is dropped. */
private val ALLOWED_SOURCE_MPV_OPTIONS = setOf(
    "demuxer-lavf-o",
    "demuxer-lavf-format",
    "demuxer-lavf-probesize",
    "demuxer-lavf-analyzeduration",
    "demuxer-max-bytes",
    "demuxer-max-back-bytes",
    "demuxer-readahead-secs",
    "cache",
    "cache-secs",
    "force-seekable",
    "hls-bitrate",
    "referrer",
    "user-agent",
    "network-timeout",
)

private val OPTION_NAME = Regex("^[a-z0-9-]+$")

/** The stream URL is not something the player may open (not http(s) and not one of the app's own offline files). */
internal class AnimeUnsupportedVideoUrlException(scheme: String) :
    IllegalArgumentException("The video uses an unsupported address type ($scheme)")

/**
 * Only http(s) streams, or the app's own offline files (a bare absolute path such as `/storage/.../episode.mp4`, as
 * produced by `AnimeDownloads.offlineVideo`), may reach mpv. This blocks `file:`, `edl:`, `fd:`, `lavf:` and friends.
 */
internal fun checkAnimeVideoUrl(url: String) {
    val scheme = url.substringBefore(':', "").lowercase()
    val hasScheme = url.contains(':') && scheme.isNotEmpty() && scheme.all { it.isLetterOrDigit() || it in "+-." }
    if (hasScheme) {
        if (scheme != "http" && scheme != "https") throw AnimeUnsupportedVideoUrlException(scheme)
    } else if (!url.startsWith("/")) {
        throw AnimeUnsupportedVideoUrlException("none")
    }
}

/** Per-file options, like Aniyomi's PlayerActivity.setVideo; never leak a source's options into the next file. */
internal fun animeMpvLoadCommand(video: Video, resumePosition: Double): Array<String> {
    checkAnimeVideoUrl(video.videoUrl)
    val start = resumePosition.takeIf { it.isFinite() && it > 0 } ?: 0.0
    val accepted = video.mpvArgs.filter { (name, _) -> OPTION_NAME.matches(name) && name in ALLOWED_SOURCE_MPV_OPTIONS }
    video.mpvArgs.map { it.first }.filter { name -> accepted.none { it.first == name } }.distinct().forEach { name ->
        AnimeLog.w("dropped source mpv option '${name.take(40)}'")
    }
    val options = (accepted + ("start" to start.toString())).joinToString(",") { (name, value) ->
        // The JNI wrapper uses GetStringUTFChars (modified UTF-8, including six-byte surrogate pairs).
        // Length quoting keeps commas, quotes and nested option lists inside this value.
        val bytes = value.sumOf {
            if (it.code in 1..127) {
                1
            } else if (it.code <= 2047) {
                2
            } else {
                3
            }
        }
        "$name=%$bytes%$value"
    }
    return arrayOf("loadfile", video.videoUrl, "replace", "0", options)
}
