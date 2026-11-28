package eu.kanade.tachiyomi.tsuzuki.anime.download

import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * AY7: what a saved episode keeps beside its video so offline playback matches streaming — the external subtitle
 * and audio files (downloaded next to the video, with the source's headers), their labels, the source's mpv
 * options and the chosen quality. Stored as `tracks.json`; `AnimeDownloads.offlineVideo` rebuilds the [Video].
 */
@Serializable
internal data class AnimeOfflineSidecar(
    val subtitles: List<AnimeOfflineTrack> = emptyList(),
    val audio: List<AnimeOfflineTrack> = emptyList(),
    val mpvArgs: List<List<String>> = emptyList(),
    val quality: String = "",
)

/** [file] is a name inside the episode folder (never a path), [lang] the source's label. */
@Serializable
internal data class AnimeOfflineTrack(val file: String, val lang: String)

internal object AnimeOfflineTracks {
    const val SIDECAR = "tracks.json"
    private const val MAX_SUBTITLE_BYTES = 16L * 1024 * 1024
    private const val MAX_AUDIO_BYTES = 2L * 1024 * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = true }
    private val safeExtension = Regex("^[a-z0-9]{1,5}$")

    /**
     * Download [video]'s external tracks into [folder] and write the sidecar. A track that can't be saved (not
     * http(s), a playlist, too large, a failed request) is left out with nothing half-written; the video stays.
     */
    suspend fun save(client: OkHttpClient, video: Video, folder: File) {
        val headers = video.headers ?: Headers.Builder().build()
        val subtitles = video.subtitleTracks.mapIndexedNotNull { index, track ->
            fetch(client, headers, track, folder, "subtitle-$index", MAX_SUBTITLE_BYTES)
        }
        val audio = video.audioTracks.mapIndexedNotNull { index, track ->
            fetch(client, headers, track, folder, "audio-$index", MAX_AUDIO_BYTES)
        }
        write(
            folder,
            AnimeOfflineSidecar(
                subtitles = subtitles,
                audio = audio,
                mpvArgs = video.mpvArgs.map { listOf(it.first, it.second) },
                quality = video.videoTitle,
            ),
        )
    }

    fun write(folder: File, sidecar: AnimeOfflineSidecar) {
        val target = File(folder, SIDECAR)
        val temporary = File(folder, "$SIDECAR.part")
        temporary.writeText(json.encodeToString(AnimeOfflineSidecar.serializer(), sidecar))
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw IOException("Could not save the episode's track list")
        }
    }

    fun read(folder: File): AnimeOfflineSidecar? = File(folder, SIDECAR).takeIf { it.isFile }?.let { file ->
        runCatching { json.decodeFromString(AnimeOfflineSidecar.serializer(), file.readText()) }.getOrNull()
    }

    /** The saved episode as a [Video]: tracks that still exist in [folder], and the source's mpv options. */
    fun offlineVideo(videoFile: File, folder: File): Video {
        val sidecar = read(folder) ?: return Video(videoUrl = videoFile.absolutePath)
        fun List<AnimeOfflineTrack>.present() = mapNotNull { track ->
            // Only plain names inside the folder; a sidecar can never point elsewhere.
            File(folder, track.file).takeIf { '/' !in track.file && '\\' !in track.file && it.isFile }
                ?.let { Track(it.absolutePath, track.lang) }
        }
        return Video(
            videoUrl = videoFile.absolutePath,
            videoTitle = sidecar.quality,
            subtitleTracks = sidecar.subtitles.present(),
            audioTracks = sidecar.audio.present(),
            mpvArgs = sidecar.mpvArgs.mapNotNull { if (it.size == 2) it[0] to it[1] else null },
        )
    }

    private suspend fun fetch(
        client: OkHttpClient,
        headers: Headers,
        track: Track,
        folder: File,
        stem: String,
        limit: Long,
    ): AnimeOfflineTrack? {
        val url = track.url.toHttpUrlOrNull() ?: return null
        val extension = url.pathSegments.lastOrNull().orEmpty().substringAfterLast('.', "").lowercase()
            .takeIf { safeExtension.matches(it) && it != "m3u8" } ?: if (stem.startsWith("subtitle")) "srt" else "m4a"
        if (url.encodedPath.endsWith(".m3u8", ignoreCase = true)) return null
        val name = "$stem.$extension"
        val target = File(folder, name)
        val partial = File(folder, "$name.part")
        return try {
            client.newCall(Request.Builder().url(url).headers(headers).build()).await().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body
                if (body.contentType()?.subtype?.contains("mpegurl", ignoreCase = true) == true) return null
                if (body.contentLength() > limit) return null
                var written = 0L
                partial.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            written += read
                            if (written > limit) throw IOException("Track too large")
                            output.write(buffer, 0, read)
                        }
                    }
                }
                if (written == 0L || !partial.renameTo(target)) {
                    partial.delete()
                    return null
                }
                AnimeOfflineTrack(name, track.lang)
            }
        } catch (e: CancellationException) {
            partial.delete()
            throw e
        } catch (_: IOException) {
            partial.delete()
            null
        }
    }
}
