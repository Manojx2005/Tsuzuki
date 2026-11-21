package eu.kanade.tachiyomi.tsuzuki.anime.preview

internal enum class EpisodePreviewMode { ON, WIFI_ONLY, OFF }

internal data class AnimeEpisodePreview(
    val animeId: Long,
    val episodeId: Long,
    val previewUrl: String?,
    val coverUrl: String?,
    val frameVersion: Long,
    val sourceId: Long,
    val networkAllowed: Boolean,
) {
    /**
     * A captured frame belongs to one episode. Without one, the artwork is the source's preview or the show's
     * cover, often shared by hundreds of episodes, so it is keyed without the episode and decoded once.
     */
    val cacheKey: String
        get() = if (frameVersion > 0) {
            listOf("anime-preview", animeId, episodeId, frameVersion, sourceId, previewUrl, coverUrl, networkAllowed)
        } else {
            listOf("anime-artwork", sourceId, previewUrl, coverUrl, networkAllowed)
        }.joinToString(";")
}

internal fun previewNetworkAllowed(mode: EpisodePreviewMode, wifi: Boolean): Boolean = when (mode) {
    EpisodePreviewMode.ON -> true
    EpisodePreviewMode.WIFI_ONLY -> wifi
    EpisodePreviewMode.OFF -> false
}

internal fun previewUrls(preview: String?, cover: String?, allowed: Boolean): List<String> =
    if (allowed) listOfNotNull(preview?.takeIf(String::isNotBlank), cover?.takeIf(String::isNotBlank)).distinct()
    else emptyList()

internal fun previewRemaining(position: Long, duration: Long): Long? =
    if (duration > 0) (duration - position.coerceAtLeast(0)).coerceAtLeast(0) else null

internal fun frameIsVisible(argb: IntArray): Boolean = argb.isNotEmpty() && argb.sumOf { pixel ->
    val red = (pixel ushr 16) and 255
    val green = (pixel ushr 8) and 255
    val blue = pixel and 255
    (red * 54L + green * 183L + blue * 19L) / 256
}.toDouble() / argb.size >= 8

internal data class PreviewFile(val id: String, val size: Long, val modified: Long)

internal fun previewFilesToEvict(files: List<PreviewFile>, limit: Long): List<String> {
    var total = files.sumOf { it.size }
    return files.sortedWith(compareBy(PreviewFile::modified, PreviewFile::id)).mapNotNull {
        if (total <= limit) null else it.id.also { _ -> total -= it.size }
    }
}

/** Per playback instance. Closing is allowed one last capture even inside the pause throttle window. */
internal class PreviewCaptureGate {
    private val lastCapture = mutableMapOf<Long, Long>()
    private var closed = false

    fun acquire(episodeId: Long, position: Double, now: Long, closing: Boolean, incognito: Boolean): Boolean {
        if (closed || incognito || !position.isFinite() || position < 5) return false
        if (closing) closed = true
        val previous = lastCapture[episodeId]
        if (!closing && previous != null && now - previous < 30_000) return false
        lastCapture[episodeId] = now
        return true
    }
}
