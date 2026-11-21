package eu.kanade.tachiyomi.tsuzuki.anime.player

import java.io.File
import java.io.IOException
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
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("can't create $dir")
        val file = File(dir, "$hash.${subtitle.extension}")
        if (!file.isFile || file.length() != subtitle.bytes.size.toLong()) {
            val partial = File(dir, "$hash.${subtitle.extension}.part")
            partial.writeBytes(subtitle.bytes)
            if (!partial.renameTo(file)) {
                file.delete()
                if (!partial.renameTo(file)) throw IOException("can't move $partial")
            }
        }
        file
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
