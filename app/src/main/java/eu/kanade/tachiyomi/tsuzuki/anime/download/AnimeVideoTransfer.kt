package eu.kanade.tachiyomi.tsuzuki.anime.download

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.coroutineContext

internal data class AnimeDownloadProgress(
    val completed: Long,
    val total: Long? = null,
    val fraction: Float? = total?.takeIf { it > 0 }?.let { (completed.toDouble() / it).toFloat().coerceIn(0f, 1f) },
)

internal class AnimeVideoTransfer(
    client: OkHttpClient,
    video: Video,
    private val preferredHeight: Int,
) {
    private val client = client.newBuilder().cache(null).followRedirects(false).build()
    private val original = video.videoUrl.toHttpUrlOrNull()
        ?.takeIf { it.username.isEmpty() && it.password.isEmpty() }
        ?: throw UnsupportedAnimeDownload()
    private val headers = video.headers ?: Headers.Builder().build()

    suspend fun download(folder: File, onProgress: (AnimeDownloadProgress) -> Unit) {
        if (!folder.isDirectory && !folder.mkdirs()) throw IOException("Cannot create episode directory")
        val partial = File(folder, "video.part")
        val validator = File(folder, "video.validator")
        val offset = partial.length()
        val range = if (offset > 0 && validator.isFile) {
            validator.readLines()
                .takeIf { it.size == 3 && it[0] == original.fingerprint() }
                ?.let { ResumeRange(offset, it[1], it[2]) }
        } else {
            null
        }
        val received = get(original, range) { response ->
            if (range != null && response.code == 416) {
                null
            } else {
                Received(receive(response, folder, range, onProgress))
            }
        } ?: get(original) { response -> Received(receive(response, folder, null, onProgress)) }
        val playlist = received.playlist
        if (playlist != null) saveHls(playlist, folder, onProgress)
    }

    private suspend fun receive(
        response: Response,
        folder: File,
        resume: ResumeRange?,
        onProgress: (AnimeDownloadProgress) -> Unit,
    ): OfflineHls.Parsed? {
        if (!response.isSuccessful) throw IOException("Video request failed: ${response.code}")
        val body = response.body
        val mime = body.contentType()
        val playlist = response.request.url.encodedPath.endsWith(".m3u8", ignoreCase = true) ||
            mime?.subtype?.contains("mpegurl", ignoreCase = true) == true ||
            (response.code == 200 && body.source().rangeEquals(0, "#EXTM3U".encodeUtf8()))
        if (playlist) {
            if (response.code != 200) throw UnsupportedAnimeDownload()
            return response.parsePlaylist()
        }

        if (mime != null && mime.type != "video" && mime.subtype !in binaryMedia) {
            throw UnsupportedAnimeDownload()
        }
        val partial = File(folder, "video.part")
        val validator = File(folder, "video.validator")
        val receivedTag = response.header("ETag")?.takeIf { it.startsWith('"') && it.endsWith('"') }
        val append = response.code == 206
        val offset = resume?.offset ?: 0
        val size = if (append) {
            val range = response.header("Content-Range")?.let(contentRange::matchEntire)
            val start = range?.groupValues?.get(1)?.toLongOrNull()
            val end = range?.groupValues?.get(2)?.toLongOrNull()
            val total = range?.groupValues?.get(3)?.toLongOrNull()
            if (resume == null || resume.target != response.request.url.fingerprint() ||
                receivedTag != resume.etag || start != offset || total == null ||
                end != total - 1 || total <= offset
            ) {
                validator.delete()
                throw IOException("Video returned a different byte range")
            }
            total
        } else {
            if (response.code != 200) throw UnsupportedAnimeDownload()
            // Open/truncate before writing a new validator. A restart can only pair it with new bytes.
            partial.outputStream().close()
            if (receivedTag == null) {
                validator.delete()
            } else {
                validator.writeText("${original.fingerprint()}\n${response.request.url.fingerprint()}\n$receivedTag")
            }
            body.contentLength().takeIf { it >= 0 }
        }
        val existing = if (append) offset else 0
        val bytes = existing + response.copyTo(partial, append) {
            onProgress(AnimeDownloadProgress(existing + it, size))
        }
        if (bytes == 0L || (size != null && bytes != size)) throw IOException("Incomplete video")
        coroutineContext.ensureActive()
        publish(partial, File(folder, VIDEO))
        folder.listFiles()?.forEach { if (it.name != VIDEO) it.delete() }
        return null
    }

    private suspend fun saveHls(
        initial: OfflineHls.Parsed,
        folder: File,
        onProgress: (AnimeDownloadProgress) -> Unit,
    ) {
        val hls = when (initial) {
            is OfflineHls.Parsed.Media -> initial.hls
            is OfflineHls.Parsed.Variant -> get(initial.url) { response ->
                (response.parsePlaylist() as? OfflineHls.Parsed.Media)?.hls
                    ?: throw UnsupportedAnimeDownload()
            }
        }
        val temporary = File(folder, "asset.part")
        var completed = 0L
        var received = 0
        for ((name, url) in hls.resources) {
            coroutineContext.ensureActive()
            val asset = File(folder, name)
            if (!asset.isFile) {
                get(url) { response ->
                    if (response.code != 200) throw IOException("HLS asset request failed: ${response.code}")
                    if (response.body.contentType()?.subtype in setOf("html", "json")) throw UnsupportedAnimeDownload()
                    val expected = response.body.contentLength()
                    val bytes = response.copyTo(temporary)
                    if (bytes <= 0 || (expected >= 0 && bytes != expected)) throw IOException("Incomplete HLS asset")
                    if (name.endsWith(".key.ts") && bytes != 16L) throw UnsupportedAnimeDownload()
                }
                publish(temporary, asset)
            }
            completed += asset.length()
            received++
            onProgress(AnimeDownloadProgress(completed, fraction = received.toFloat() / hls.resources.size))
        }

        coroutineContext.ensureActive()
        File(folder, "playlist.part").let {
            it.writeText(hls.playlist)
            publish(it, File(folder, PLAYLIST))
        }
        folder.listFiles()?.forEach { file ->
            if (file.name != PLAYLIST && file.name !in hls.resources) file.delete()
        }
    }

    private suspend fun Response.copyTo(
        file: File,
        append: Boolean = false,
        onBytes: ((Long) -> Unit)? = null,
    ): Long {
        val input = body.byteStream()
        val buffer = ByteArray(64 * 1024)
        var bytes = 0L
        var lastUpdate = 0L
        FileOutputStream(file, append).buffered().use { output ->
            while (true) {
                coroutineContext.ensureActive()
                val count = input.read(buffer)
                if (count == -1) break
                output.write(buffer, 0, count)
                bytes += count
                if (onBytes != null) {
                    val now = System.nanoTime()
                    if (now - lastUpdate >= 250_000_000) {
                        onBytes(bytes)
                        lastUpdate = now
                    }
                }
            }
        }
        return bytes
    }

    private fun Response.parsePlaylist(): OfflineHls.Parsed {
        if (code != 200) throw IOException("Playlist request failed: $code")
        val data = body.source()
        if (data.request(MAX_PLAYLIST + 1)) throw UnsupportedAnimeDownload()
        return OfflineHls.parse(data.readUtf8(), request.url, preferredHeight)
    }

    private suspend fun <T> get(
        start: HttpUrl,
        range: ResumeRange? = null,
        consume: suspend (Response) -> T,
    ): T {
        var url = start
        repeat(6) {
            val sameOrigin = original.scheme == url.scheme && original.host == url.host && original.port == url.port
            val request = Request.Builder().url(url).apply {
                for ((name, value) in headers) {
                    val header = name.lowercase()
                    if (header !in controlledHeaders && (sameOrigin || header in publicHeaders)) {
                        addHeader(name, value)
                    }
                }
                header("Accept-Encoding", "identity")
                if (range != null && range.target == url.fingerprint()) {
                    header("Range", "bytes=${range.offset}-")
                    header("If-Range", range.etag)
                }
            }.build()
            val call = client.newCall(request)
            val redirect = coroutineScope {
                val cancelRead = launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        call.cancel()
                    }
                }
                try {
                    call.await().use { response ->
                        if (response.isRedirect) {
                            response.header("Location")?.let(url::resolve)
                                ?.takeIf { it.username.isEmpty() && it.password.isEmpty() }
                                ?: throw IOException("Invalid video redirect")
                        } else {
                            return@coroutineScope Fetch.Complete(consume(response))
                        }
                    }.let { Fetch.Follow(it) }
                } finally {
                    cancelRead.cancel()
                }
            }
            when (redirect) {
                is Fetch.Complete -> return redirect.value
                is Fetch.Follow -> url = redirect.url
            }
        }
        throw IOException("Too many video redirects")
    }

    private fun publish(partial: File, final: File) {
        if (!partial.renameTo(final)) throw IOException("Cannot publish completed video data")
    }

    private fun HttpUrl.fingerprint(): String = toString().encodeUtf8().sha256().hex()

    private data class ResumeRange(val offset: Long, val target: String, val etag: String)

    private data class Received(val playlist: OfflineHls.Parsed?)

    private sealed interface Fetch<out T> {
        data class Complete<T>(val value: T) : Fetch<T>
        data class Follow(val url: HttpUrl) : Fetch<Nothing>
    }

    companion object {
        const val VIDEO = "video.media"
        const val PLAYLIST = "index.m3u8"
        private const val MAX_PLAYLIST = 2L * 1024 * 1024
        private val contentRange = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)")
        private val binaryMedia = setOf("octet-stream", "mp4", "ogg", "x-matroska", "webm")
        private val publicHeaders = setOf("accept", "user-agent")
        private val controlledHeaders = setOf(
            "host",
            "range",
            "if-range",
            "accept-encoding",
            "connection",
            "content-length",
            "transfer-encoding",
        )
    }
}
