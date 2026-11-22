package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64

/** A subtitle given inline as a `data:` URL (AY3): its bytes and the file extension mpv needs to detect it. */
internal class AnimeDataSubtitle(val bytes: ByteArray, val extension: String)

/** Only HTTP(S) URLs and app-written files are handed to mpv. */
internal suspend fun prepareAnimeSubtitleTracks(tracks: List<Track>, cacheDirectory: () -> File): List<Track> {
    if (tracks.none { it.url.startsWith("data:", ignoreCase = true) }) {
        return tracks.mapNotNull { it.asHttpSubtitle() }
    }
    return withContext(Dispatchers.IO) {
        tracks.mapNotNull { track ->
            ensureActive()
            if (!track.url.startsWith("data:", ignoreCase = true)) return@mapNotNull track.asHttpSubtitle()
            val inline = decodeAnimeDataSubtitle(track.url)
            if (inline == null) {
                AnimeLog.w("skipped an inline subtitle: the data: address is malformed")
                return@mapNotNull null
            }
            val file = writeAnimeDataSubtitle(cacheDirectory(), inline)
            if (file == null) {
                AnimeLog.w("skipped an inline subtitle: the subtitle cache could not be written")
            }
            file?.let { Track(it.path, track.lang) }
        }
    }
}

private fun Track.asHttpSubtitle(): Track? {
    val http = url.toHttpUrlOrNull()
    if (http == null) AnimeLog.w("skipped a subtitle track that is neither an http(s) nor a data: address")
    return http?.let { Track(it.toString(), lang) }
}

/** Decodes `data:[<mime>][;base64],<payload>`; null for anything else or a malformed URL. */
internal fun decodeAnimeDataSubtitle(url: String): AnimeDataSubtitle? {
    if (!url.startsWith("data:", ignoreCase = true)) return null
    val comma = url.indexOf(',').takeIf { it > 0 } ?: return null
    val header = url.substring(5, comma).lowercase()
    val payload = url.substring(comma + 1)
    val parts = header.split(';')
    val bytes = try {
        if ("base64" in parts) {
            Base64.getDecoder().decode(payload.replace("\\s".toRegex(), ""))
        } else {
            percentDecodeBytes(payload) ?: return null
        }
    } catch (_: IllegalArgumentException) {
        return null
    }
    val extension = when (parts.first()) {
        "text/vtt" -> "vtt"
        "text/x-ssa", "text/x-ass", "application/x-ass", "application/x-ssa" -> "ass"
        else -> "srt"
    }
    return AnimeDataSubtitle(bytes, extension)
}

/**
 * RFC 2397 percent escapes are bytes, not UTF-8 text (AY3-F): `%E9` stays the single byte E9 so a Latin-1 or
 * Shift-JIS subtitle reaches mpv unchanged. Other characters are taken as UTF-8; `+` is a literal plus.
 * Null for a broken escape.
 */
internal fun percentDecodeBytes(text: String): ByteArray? {
    val out = java.io.ByteArrayOutputStream(text.length)
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '%') {
            if (i + 2 >= text.length) return null
            val high = Character.digit(text[i + 1], 16)
            val low = Character.digit(text[i + 2], 16)
            if (high < 0 || low < 0) return null
            out.write(high * 16 + low)
            i += 3
        } else {
            val end = if (Character.isHighSurrogate(c) && i + 1 < text.length) i + 2 else i + 1
            out.write(text.substring(i, end).toByteArray(Charsets.UTF_8))
            i = end
        }
    }
    return out.toByteArray()
}

/**
 * Writes [subtitle] once to `<dir>/<sha1>.<ext>` (the same subtitle reuses its file) and returns the file, or
 * null when the cache can't be written (AY3-F): that one subtitle is skipped and playback carries on. The bytes
 * go to a temporary file first, so a failed write never leaves a short file under the final name.
 */
internal fun writeAnimeDataSubtitle(dir: File, subtitle: AnimeDataSubtitle): File? {
    val hash = MessageDigest.getInstance("SHA-1").digest(subtitle.bytes).joinToString("") { "%02x".format(it) }
    return try {
        if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) return null
        val file = File(dir, "$hash.${subtitle.extension}")
        if (!file.isFile || file.length() != subtitle.bytes.size.toLong()) {
            val partial = File.createTempFile("subtitle-", ".part", dir)
            try {
                partial.writeBytes(subtitle.bytes)
                Files.move(
                    partial.toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (e: IOException) {
                // Another preparation may already have published the file for these same bytes.
                if (!file.isFile || file.length() != subtitle.bytes.size.toLong()) throw e
            } finally {
                partial.delete()
            }
        }
        file
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
