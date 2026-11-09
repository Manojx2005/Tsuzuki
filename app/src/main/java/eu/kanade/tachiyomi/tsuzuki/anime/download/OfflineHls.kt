package eu.kanade.tachiyomi.tsuzuki.anime.download

import okhttp3.HttpUrl
import okio.ByteString.Companion.encodeUtf8
import java.io.IOException

internal class UnsupportedAnimeDownload : IOException("Unsupported offline video format")

internal data class OfflineHls(
    val playlist: String,
    val resources: Map<String, HttpUrl>,
) {
    companion object {
        private val attribute = Regex("""(?:^|,)([A-Z0-9-]+)=("[^"]*"|[^,]+)""")

        fun parse(text: String, base: HttpUrl, preferredHeight: Int): Parsed {
            val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
            if (lines.firstOrNull() != "#EXTM3U") throw UnsupportedAnimeDownload()
            return if (lines.any { it.startsWith("#EXT-X-STREAM-INF:") }) {
                Parsed.Variant(selectVariant(lines, base, preferredHeight))
            } else {
                Parsed.Media(localize(lines, base))
            }
        }

        private fun selectVariant(lines: List<String>, base: HttpUrl, preferredHeight: Int): HttpUrl {
            val externalGroups = lines.filter { it.startsWith("#EXT-X-MEDIA:") }
                .map(::attributes)
                .filter { "URI" in it && it["TYPE"] in setOf("AUDIO", "VIDEO") }
                .map { it["TYPE"] to it["GROUP-ID"] }
                .toSet()
            val variants = lines.zipWithNext().mapNotNull { (tag, uri) ->
                if (!tag.startsWith("#EXT-X-STREAM-INF:") || uri.startsWith("#")) return@mapNotNull null
                val fields = attributes(tag)
                if (externalGroups.any { (type, group) -> fields[type] == group }) return@mapNotNull null
                val height = fields["RESOLUTION"]?.substringAfter('x')?.toIntOrNull()
                Variant(url(base, uri), height, fields["BANDWIDTH"]?.toLongOrNull() ?: 0)
            }
            return variants.minWithOrNull(
                compareBy<Variant> {
                    when {
                        it.height == null -> 2
                        preferredHeight <= 0 || it.height <= preferredHeight -> 0
                        else -> 1
                    }
                }.thenBy {
                    val height = it.height ?: 0
                    if (preferredHeight > 0 && height > preferredHeight) height else -height
                }.thenByDescending { it.bandwidth },
            )?.url ?: throw UnsupportedAnimeDownload()
        }

        private fun localize(lines: List<String>, base: HttpUrl): OfflineHls {
            if ("#EXT-X-ENDLIST" !in lines || lines.none { it.startsWith("#EXTINF:") }) {
                throw UnsupportedAnimeDownload()
            }
            val resources = linkedMapOf<String, HttpUrl>()
            val mediaSuffix = if (lines.any { it.startsWith("#EXT-X-MAP:") }) "m4s" else "ts"
            var segments = 0

            fun localUri(value: String, suffix: String): String {
                val uri = url(base, value)
                val name = uri.toString().encodeUtf8().sha256().hex() + "." + suffix
                resources[name] = uri
                if (resources.size > 20_000) throw UnsupportedAnimeDownload()
                return name
            }

            val playlist = buildString {
                for (line in lines) {
                    val tag = line.substringBefore(':')
                    val local = when {
                        !line.startsWith("#") -> {
                            segments++
                            localUri(line, mediaSuffix)
                        }
                        tag == "#EXT-X-KEY" -> {
                            val fields = attributes(line)
                            when (fields["METHOD"]) {
                                "NONE" -> "#EXT-X-KEY:METHOD=NONE"
                                "AES-128" -> {
                                    if (fields["KEYFORMAT"]?.let { it != "identity" } == true) {
                                        throw UnsupportedAnimeDownload()
                                    }
                                    // FFmpeg checks the suffix of local key URLs as well as segments.
                                    val key = localUri(fields["URI"] ?: throw UnsupportedAnimeDownload(), "key.ts")
                                    val iv = fields["IV"]?.takeIf { it.matches(Regex("0[xX][a-fA-F0-9]{1,32}")) }
                                    "#EXT-X-KEY:METHOD=AES-128,URI=\"$key\"" + (iv?.let { ",IV=$it" } ?: "")
                                }
                                else -> throw UnsupportedAnimeDownload()
                            }
                        }
                        tag == "#EXT-X-MAP" -> {
                            val fields = attributes(line)
                            val init = localUri(fields["URI"] ?: throw UnsupportedAnimeDownload(), "mp4")
                            val range = fields["BYTERANGE"]?.takeIf { it.matches(Regex("[0-9]+(@[0-9]+)?")) }
                            "#EXT-X-MAP:URI=\"$init\"" + (range?.let { ",BYTERANGE=\"$it\"" } ?: "")
                        }
                        tag in mediaTags -> line
                        tag in unsupportedTags -> throw UnsupportedAnimeDownload()
                        else -> continue
                    }
                    appendLine(local)
                }
            }
            if (segments == 0) throw UnsupportedAnimeDownload()
            return OfflineHls(playlist, resources)
        }

        private fun attributes(line: String): Map<String, String> = attribute
            .findAll(line.substringAfter(':'))
            .associate { it.groupValues[1] to it.groupValues[2].removeSurrounding("\"") }

        private fun url(base: HttpUrl, value: String): HttpUrl {
            if ("{\$" in value) throw UnsupportedAnimeDownload()
            return base.resolve(value)?.takeIf { it.username.isEmpty() && it.password.isEmpty() }
                ?: throw UnsupportedAnimeDownload()
        }

        private val mediaTags = setOf(
            "#EXTM3U", "#EXT-X-VERSION", "#EXT-X-TARGETDURATION", "#EXT-X-MEDIA-SEQUENCE",
            "#EXT-X-DISCONTINUITY-SEQUENCE", "#EXT-X-DISCONTINUITY", "#EXT-X-BYTERANGE",
            "#EXTINF", "#EXT-X-ENDLIST", "#EXT-X-PLAYLIST-TYPE", "#EXT-X-INDEPENDENT-SEGMENTS",
        )

        private val unsupportedTags = setOf("#EXT-X-GAP", "#EXT-X-SKIP", "#EXT-X-I-FRAMES-ONLY")

        private data class Variant(val url: HttpUrl, val height: Int?, val bandwidth: Long)
    }

    sealed interface Parsed {
        data class Variant(val url: HttpUrl) : Parsed
        data class Media(val hls: OfflineHls) : Parsed
    }
}
