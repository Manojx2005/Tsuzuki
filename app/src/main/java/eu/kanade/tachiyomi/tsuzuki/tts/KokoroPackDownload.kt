package eu.kanade.tachiyomi.tsuzuki.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Downloads the voice-pack zip into [target], resuming a partial file with an HTTP Range request. Pause = cancel
 * the coroutine and keep [target]; cancel = also delete it. The finished zip is installed by [KokoroPackImporter],
 * which checks every file against the manifest before anything is replaced, so a bad download never installs.
 *
 * [open] performs the request for bytes from `offset` (0 = whole file) and returns the status, the body length
 * (-1 if unknown) and the body.
 */
internal class KokoroPackDownload(
    private val target: File,
    private val open: suspend (offset: Long) -> Opened,
) {
    class Opened(val code: Int, val length: Long, val body: InputStream)

    /** The pack has not been published at the download address yet (HTTP 404). */
    class NotPublishedException : IOException("The voice pack is not published yet")

    suspend fun run(onProgress: (done: Long, total: Long) -> Unit): File = withContext(Dispatchers.IO) {
        val existing = if (target.isFile) target.length() else 0L
        val opened = open(existing)
        opened.body.use { body ->
            val (append, total) = when (opened.code) {
                206 -> true to if (opened.length >= 0) existing + opened.length else -1L
                200 -> false to opened.length
                404 -> throw NotPublishedException()
                // 416: the partial file is already complete (or the server changed); start over next time.
                416 -> {
                    target.delete()
                    throw IOException("The server refused to resume; try again")
                }
                else -> throw IOException("Voice pack download HTTP ${opened.code}")
            }
            var done = if (append) existing else 0L
            target.parentFile?.mkdirs()
            FileOutputStream(target, append).use { out ->
                val buffer = ByteArray(64 * 1024)
                var lastReport = -1L
                onProgress(done, total)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = body.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    done += read
                    // About every 256 KB, so the UI is not flooded.
                    if (done - lastReport >= 256 * 1024) {
                        lastReport = done
                        onProgress(done, total)
                    }
                }
            }
            if (total >= 0 && done != total) throw IOException("The download ended early ($done of $total bytes)")
            onProgress(done, if (total >= 0) total else done)
        }
        target
    }

    companion object {
        /** Space the device needs while installing: the zip plus the unpacked pack, with a little margin. */
        fun spaceNeeded(zipBytes: Long, installedBytes: Long): Long =
            zipBytes + installedBytes + 16L * 1024 * 1024
    }
}
