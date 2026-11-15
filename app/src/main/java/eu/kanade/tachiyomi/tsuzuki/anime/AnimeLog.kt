package eu.kanade.tachiyomi.tsuzuki.anime

import android.util.Log
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.error.redactErrorText
import eu.kanade.tachiyomi.tsuzuki.error.safeErrorHost

/**
 * One logcat tag for everything the anime player and its sources do: `adb logcat -s TsuzukiAnime`.
 * Messages are redacted (URLs are reduced to their host; tokens, cookies and query strings are dropped), so a
 * captured log is safe to attach to a bug report. Written with [Log] directly so it is visible in every build type.
 */
internal object AnimeLog {
    const val TAG = "TsuzukiAnime"

    fun d(message: String) = emit(Log.DEBUG, message)

    fun i(message: String) = emit(Log.INFO, message)

    fun w(message: String, error: Throwable? = null) =
        emit(Log.WARN, if (error == null) message else "$message\n${describeAnimeError(error)}")

    fun e(message: String, error: Throwable? = null) =
        emit(Log.ERROR, if (error == null) message else "$message\n${describeAnimeError(error)}")

    /** Logging must never break playback or a plain JVM unit test, where [Log] is not available. */
    private fun emit(priority: Int, message: String) {
        try {
            Log.println(priority, TAG, message)
        } catch (_: RuntimeException) {
            // Not running on Android.
        }
    }
}

/**
 * The exception chain (class and redacted message, outermost first) and the first stack frames of the root cause,
 * so a `NoSuchMethodError` from an extension shows which class it linked against.
 */
internal fun describeAnimeError(error: Throwable, frames: Int = 8): String {
    val chain = generateSequence(error) { it.cause?.takeIf { cause -> cause !== it } }.take(6).toList()
    val headline = chain.joinToString(" <- ") { "${it.javaClass.name}: ${redactErrorText(it.message.orEmpty())}" }
    val trace = chain.last().stackTrace.take(frames).joinToString("\n") { "    at $it" }
    return if (trace.isEmpty()) headline else "$headline\n$trace"
}

/** Severity decision for an mpv log line: warn and above always, verbose only in debug builds, otherwise dropped. */
internal fun mpvLogPriority(level: Int, debugBuild: Boolean): Int? = when {
    level <= 0 -> null
    level <= MPV_LOG_ERROR -> Log.ERROR
    level <= MPV_LOG_WARN -> Log.WARN
    debugBuild && level <= MPV_LOG_VERBOSE -> Log.DEBUG
    else -> null
}

internal fun formatMpvLog(prefix: String, text: String): String = "mpv/$prefix: ${redactErrorText(text)}"

private const val MPV_LOG_ERROR = 20
private const val MPV_LOG_WARN = 30
private const val MPV_LOG_VERBOSE = 50

/** Host (never path or query), resolution and header names, for the "chosen video" log line. */
internal fun describeAnimeVideo(video: Video): String {
    val host = safeErrorHost(video.videoUrl) ?: video.videoUrl.substringBefore(':').ifBlank { "none" } + ":"
    val headers = video.headers?.names()?.sorted()?.joinToString(",").orEmpty().ifEmpty { "-" }
    return "host=$host title='${redactErrorText(video.videoTitle).take(40)}' resolution=${video.resolution ?: "?"} " +
        "headers=[$headers] tracks=sub:${video.subtitleTracks.size}/audio:${video.audioTracks.size}"
}

/** The source answered but no stream could be started from what it returned. */
internal class AnimeNoPlayableVideoException : IllegalStateException("The source returned no playable video")

/** mpv ended the file with an error; [detail] is mpv's own error string. */
internal class AnimeStreamException(detail: String?) :
    IllegalStateException(
        "The video stream could not be played" + detail?.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty(),
    )
