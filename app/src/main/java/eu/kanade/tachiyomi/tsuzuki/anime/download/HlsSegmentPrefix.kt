package eu.kanade.tachiyomi.tsuzuki.anime.download

import java.io.File
import java.io.RandomAccessFile

/**
 * AP3 (owner 2026-11-22: downloaded episodes don't play). Many anime hosts hide HLS segments behind a small image:
 * the file starts with a PNG/JPEG/GIF and the MPEG-TS stream follows. Online, the source's mpv options let ffmpeg
 * skip it; a downloaded copy has no such options, so mpv fails with "error reading packet". Saved segments keep
 * only the transport stream.
 */
internal object HlsSegmentPrefix {
    private const val SYNC = 0x47.toByte()
    private const val PACKET = 188
    private const val SCAN = 256 * 1024

    private fun isImage(head: ByteArray): Boolean =
        head.size >= 4 && (
            (head[0] == 0x89.toByte() && head[1] == 'P'.code.toByte() && head[2] == 'N'.code.toByte()) ||
                (head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte()) ||
                (head[0] == 'G'.code.toByte() && head[1] == 'I'.code.toByte() && head[2] == 'F'.code.toByte())
            )

    /** Where the transport stream starts in a segment that begins with an image, or null (nothing to strip). */
    fun payloadOffset(head: ByteArray): Int? {
        if (head.isEmpty() || head[0] == SYNC || !isImage(head)) return null
        for (i in 1 until head.size - 2 * PACKET) {
            if (head[i] == SYNC && head[i + PACKET] == SYNC && head[i + 2 * PACKET] == SYNC) return i
        }
        return null
    }

    /** Rewrites [file] without its image prefix; true when something was removed. */
    fun strip(file: File): Boolean {
        val head = file.inputStream().use { input ->
            val buffer = ByteArray(SCAN)
            var filled = 0
            while (filled < SCAN) {
                val count = input.read(buffer, filled, SCAN - filled)
                if (count == -1) break
                filled += count
            }
            buffer.copyOf(filled)
        }
        val offset = payloadOffset(head) ?: return false
        val cleaned = File(file.parentFile, "${file.name}.clean")
        RandomAccessFile(file, "r").use { source ->
            source.seek(offset.toLong())
            cleaned.outputStream().buffered().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = source.read(buffer)
                    if (count == -1) break
                    output.write(buffer, 0, count)
                }
            }
        }
        if (!cleaned.renameTo(file)) {
            file.delete()
            check(cleaned.renameTo(file)) { "Could not replace $file" }
        }
        return true
    }

    /**
     * Cleans an episode saved before AP3: every segment the playlist names. A marker keeps this to one pass; the
     * check reads only each file's first bytes.
     */
    fun repairFolder(folder: File, playlist: File) {
        val marker = File(folder, ".segments-clean")
        if (marker.isFile) return
        playlist.readLines()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.endsWith(".key.ts") }
            .map { File(folder, it) }
            .filter(File::isFile)
            .forEach(::strip)
        marker.writeText("1")
    }
}
