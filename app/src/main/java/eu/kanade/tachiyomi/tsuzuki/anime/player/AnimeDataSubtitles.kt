package eu.kanade.tachiyomi.tsuzuki.anime.player

import java.io.File
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Base64

/** A subtitle given inline as a `data:` URL (AY3): its bytes and the file extension mpv needs to detect it. */
internal class AnimeDataSubtitle(val bytes: ByteArray, val extension: String)

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
            URLDecoder.decode(payload.replace("+", "%2B"), Charsets.UTF_8).toByteArray(Charsets.UTF_8)
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

/** Writes [subtitle] once to `<dir>/<sha1>.<ext>` (the same subtitle reuses its file) and returns the file. */
internal fun writeAnimeDataSubtitle(dir: File, subtitle: AnimeDataSubtitle): File {
    val hash = MessageDigest.getInstance("SHA-1").digest(subtitle.bytes).joinToString("") { "%02x".format(it) }
    val file = File(dir.apply { mkdirs() }, "$hash.${subtitle.extension}")
    if (!file.exists() || file.length() != subtitle.bytes.size.toLong()) file.writeBytes(subtitle.bytes)
    return file
}
