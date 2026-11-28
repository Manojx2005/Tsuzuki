package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import java.io.File

/*
 * A6.5: open an episode in another video app. The per-player extras and the result keys follow Aniyomi's
 * `ui/player/ExternalIntents.kt` (Apache-2.0, https://github.com/aniyomiorg/aniyomi); written for Tsuzuki.
 * Positions go out and come back in milliseconds, the unit mpv-android, MX Player and VLC use.
 */

/** The "Play with" preference: Tsuzuki's own player, the system chooser, or one app's package name. */
internal sealed interface AnimeExternalChoice {
    data object Tsuzuki : AnimeExternalChoice
    data object Ask : AnimeExternalChoice
    data class App(val packageName: String) : AnimeExternalChoice

    val stored: String
        get() = when (this) {
            Tsuzuki -> ""
            Ask -> ASK
            is App -> packageName
        }

    companion object {
        const val ASK = "*"

        fun from(stored: String): AnimeExternalChoice = when (val value = stored.trim()) {
            "" -> Tsuzuki
            ASK -> Ask
            else -> App(value)
        }
    }
}

/** Everything the intent needs, kept free of Android types so it can be tested on the JVM. */
internal data class AnimeExternalLaunch(
    /** null: let Android choose (the system chooser, or the only app that handles it). */
    val packageName: String?,
    /** The activity to start inside [packageName], when the player is a known one. */
    val className: String?,
    /** http(s) / content URL, or an absolute file path (shared through the FileProvider). */
    val url: String,
    val mimeType: String,
    val title: String,
    val positionMs: Long,
    /** Request headers as `name, value, name, value…` (the `headers` extra MX Player and mpv-android read). */
    val headers: List<String>,
    val subtitles: List<Subtitle>,
) {
    data class Subtitle(val url: String, val name: String)

    val isLocalFile: Boolean get() = url.startsWith("/") || url.startsWith("file:")

    /** VLC reads `position` as a long and wants `from_start` off; the others read an int. */
    val isVlc: Boolean get() = packageName?.contains("vlc") == true
}

/** What the player reported when it returned: position and duration in seconds, and whether it played to the end. */
internal data class AnimeExternalResult(val position: Double, val duration: Double, val completed: Boolean)

private val KNOWN_PLAYERS = mapOf(
    "is.xyz.mpv" to "is.xyz.mpv.MPVActivity",
    "com.mxtech.videoplayer.ad" to "com.mxtech.videoplayer.ad.ActivityScreen",
    "com.mxtech.videoplayer.pro" to "com.mxtech.videoplayer.pro.ActivityScreen",
    "org.videolan.vlc" to "org.videolan.vlc.gui.video.VideoPlayerActivity",
    "live.mehiz.mpvkt" to "live.mehiz.mpvkt.ui.player.PlayerActivity",
    "live.mehiz.mpvkt.preview" to "live.mehiz.mpvkt.ui.player.PlayerActivity",
    "com.brouken.player" to "com.brouken.player.PlayerActivity",
    "dev.anilbeesetti.nextplayer" to "dev.anilbeesetti.nextplayer.feature.player.PlayerActivity",
)

/** The MIME type for [url]: a precise one for the containers players care about, else any video. */
internal fun animeExternalMimeType(url: String): String {
    val path = url.substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.endsWith(".m3u8") -> "application/x-mpegURL"
        path.endsWith(".mkv") -> "video/x-matroska"
        path.endsWith(".webm") -> "video/webm"
        path.endsWith(".mp4") || path.endsWith(".m4v") -> "video/mp4"
        else -> "video/*"
    }
}

/**
 * The launch for [choice], or null when Tsuzuki plays it itself. A saved HLS playlist on disk can't be handed over:
 * its segments are separate files no other app can read through one shared URI.
 */
internal fun animeExternalLaunch(
    choice: AnimeExternalChoice,
    url: String,
    title: String,
    positionSeconds: Double,
    headers: Map<String, String>,
    subtitles: List<AnimeExternalLaunch.Subtitle>,
): AnimeExternalLaunch? {
    val packageName = when (choice) {
        AnimeExternalChoice.Tsuzuki -> return null
        AnimeExternalChoice.Ask -> null
        is AnimeExternalChoice.App -> choice.packageName
    }
    val localFile = url.startsWith("/") || url.startsWith("file:")
    if (localFile && url.substringBefore('?').lowercase().endsWith(".m3u8")) return null
    return AnimeExternalLaunch(
        packageName = packageName,
        className = packageName?.let(KNOWN_PLAYERS::get),
        url = url,
        mimeType = animeExternalMimeType(url),
        title = title,
        positionMs = (positionSeconds.takeIf { it.isFinite() && it > 0 } ?: 0.0).times(1000).toLong(),
        headers = headers.flatMap { (name, value) -> listOf(name, value) },
        // Other apps can only fetch subtitles from the network.
        subtitles = subtitles.filter { it.url.startsWith("http://") || it.url.startsWith("https://") },
    )
}

/**
 * Read a player's result. [extra] looks up one extra of the returned intent. MX Player sends `end_by`, `position`
 * and `duration`; mpv-android `position` and `duration`; VLC `extra_position` and `extra_duration`. Null when the
 * player said nothing usable (it was closed before it knew the length, or it returns no result at all).
 */
internal fun parseAnimeExternalResult(extra: (String) -> Any?): AnimeExternalResult? {
    fun millis(key: String): Long? = when (val value = extra(key)) {
        is Long -> value
        is Int -> value.toLong()
        else -> null
    }
    val endBy = extra("end_by") as? String
    val (position, duration) = if (millis("extra_position") != null) {
        millis("extra_position") to millis("extra_duration")
    } else {
        millis("position") to millis("duration")
    }
    val completed = endBy == "playback_completion"
    val durationSeconds = (duration ?: 0L).coerceAtLeast(0L) / 1000.0
    if (completed) {
        return durationSeconds.takeIf { it > 0 }?.let { AnimeExternalResult(it, it, completed = true) }
    }
    val positionSeconds = (position ?: return null).coerceAtLeast(0L) / 1000.0
    if (durationSeconds <= 0 || positionSeconds <= 0) return null
    return AnimeExternalResult(positionSeconds.coerceAtMost(durationSeconds), durationSeconds, completed = false)
}

/** Build the intent; files on disk go out as a FileProvider URI with read permission. */
internal fun AnimeExternalLaunch.toIntent(context: Context): Intent {
    val uri = if (isLocalFile) {
        val file = File(Uri.parse(url).path ?: url)
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } else {
        Uri.parse(url)
    }
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (packageName != null) {
            val installed = context.isInstalled(packageName)
            when {
                // Android 13+ VLC rejects an explicit component from other apps; a package is enough.
                isVlc || className == null || !installed -> if (installed) setPackage(packageName)
                else -> component = ComponentName(packageName, className)
            }
        }
        putExtra("title", title)
        putExtra("return_result", true)
        putExtra("secure_uri", true)
        if (isVlc) {
            putExtra("position", positionMs)
            putExtra("from_start", false)
        } else {
            putExtra("position", positionMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        }
        if (headers.isNotEmpty()) {
            putExtra("headers", headers.toTypedArray())
            putExtra(
                "android.media.intent.extra.HTTP_HEADERS",
                Bundle().apply { headers.chunked(2).forEach { (name, value) -> putString(name, value) } },
            )
        }
        if (subtitles.isNotEmpty()) {
            val uris = subtitles.map { Uri.parse(it.url) }
            putExtra("subs", uris.toTypedArray())
            putExtra("subs.name", subtitles.map { it.name }.toTypedArray())
            putExtra("subs.enable", arrayOf(uris.first()))
            if (isVlc) putExtra("subtitles_location", subtitles.first().url)
        }
    }
}

/** Installed apps that open a network video, for the "Play with" list: package name to label, by label. */
internal fun installedAnimeVideoPlayers(context: Context): List<Pair<String, String>> {
    val probe = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("https://example.com/video.mp4"), "video/*")
    val manager = context.packageManager
    return manager.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
        .map { it.activityInfo }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .map { it.packageName to it.loadLabel(manager).toString() }
        .sortedBy { it.second.lowercase() }
}

/** The label of an installed app, or null once it was uninstalled. */
internal fun animeVideoPlayerLabel(context: Context, packageName: String): String? = try {
    val info = context.packageManager.getApplicationInfo(packageName, 0)
    context.packageManager.getApplicationLabel(info).toString()
} catch (_: PackageManager.NameNotFoundException) {
    null
}

private fun Context.isInstalled(packageName: String): Boolean = animeVideoPlayerLabel(this, packageName) != null
