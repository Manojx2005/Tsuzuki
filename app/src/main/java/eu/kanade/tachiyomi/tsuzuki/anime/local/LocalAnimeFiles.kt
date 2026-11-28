package eu.kanade.tachiyomi.tsuzuki.anime.local

import java.util.concurrent.ConcurrentHashMap

/** A6.8: file types the local anime source lists. */
internal val LOCAL_ANIME_VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm")
internal val LOCAL_ANIME_SUBTITLE_EXTENSIONS = setOf("srt", "ass", "ssa", "vtt")

internal fun String.fileExtension(): String = substringAfterLast('.', "").lowercase()

internal fun String.fileBaseName(): String = substringBeforeLast('.')

private val EPISODE_MARKER = Regex("""(?i)(?:^|[^a-z])e(?:p(?:isode)?)?\s*\.?\s*(\d{1,4}(?:\.\d+)?)""")
private val BRACKETED = Regex("""\[[^]]*]|\([^)]*\)""")
private val STANDALONE_NUMBER = Regex("""(?<![\w.])(\d{1,4}(?:\.\d+)?)(?![\w.])""")

/**
 * Episode number from a file name: "Show - 03.mkv" → 3, "Show S01E12 [1080p].mp4" → 12, "Movie.mkv" → null.
 * An explicit E/EP marker wins; otherwise the last standalone number outside brackets that is not a year.
 */
internal fun localEpisodeNumber(fileName: String): Double? {
    val name = fileName.fileBaseName().replace('_', ' ')
    EPISODE_MARKER.findAll(name).lastOrNull()?.let { return it.groupValues[1].toDouble() }
    // Bracketed tags ([1080p], (x264), [ABCD1234]) carry no episode number.
    val plain = name.replace(BRACKETED, " ")
    return STANDALONE_NUMBER.findAll(plain)
        .map { it.groupValues[1] }
        .lastOrNull { !(it.length == 4 && it.toDouble() in 1900.0..2100.0) }
        ?.toDouble()
}

/** Sidecar subtitles: the video's base name plus an optional language tag ("Show - 01.en.srt"); pairs of (file, tag). */
internal fun localSubtitlesFor(videoName: String, siblings: List<String>): List<Pair<String, String>> {
    val base = videoName.fileBaseName()
    return siblings.filter { it.fileExtension() in LOCAL_ANIME_SUBTITLE_EXTENSIONS }
        .mapNotNull { name ->
            val stem = name.fileBaseName()
            when {
                stem == base -> name to ""
                stem.startsWith("$base.") -> name to stem.removePrefix("$base.")
                else -> null
            }
        }
        .sortedBy { it.first }
}

/**
 * The content:// addresses the local anime source handed out in this process. Only these (never a URL an
 * extension supplies) may skip the http(s) rule, and the player opens them through its own file descriptor.
 */
internal object LocalAnimeVideos {
    private val issued = ConcurrentHashMap.newKeySet<String>()

    fun issue(uri: String): String = uri.also { issued += it }

    fun isIssued(uri: String): Boolean = uri.startsWith("content://") && uri in issued
}
