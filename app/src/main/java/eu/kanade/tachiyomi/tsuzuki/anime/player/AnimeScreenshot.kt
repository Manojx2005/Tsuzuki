package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.net.Uri
import kotlinx.coroutines.delay
import java.io.File
import java.io.RandomAccessFile

/** A6.7 (board `docs/design/boards/remaining-2026-11/Screenshot.dc.html`): the outcome shown in the snackbar. */
internal sealed interface AnimeScreenshotResult {
    data class Saved(val uri: Uri) : AnimeScreenshotResult
    data object Failed : AnimeScreenshotResult
}

/**
 * "Title - EP 12 - 00-14-32": anime title, episode number (or the episode's name when it has no number) and the
 * playback position. Characters a file name can't hold are replaced by the saver (`DiskUtil.buildValidFilename`).
 */
internal fun animeScreenshotName(
    title: String,
    episodeNumber: Double,
    episodeName: String,
    positionSeconds: Double,
): String {
    val episode = if (episodeNumber >= 0) {
        "EP " + if (episodeNumber % 1.0 == 0.0) episodeNumber.toLong().toString() else episodeNumber.toString()
    } else {
        episodeName.trim()
    }
    val total = positionSeconds.coerceAtLeast(0.0).toLong()
    val time = "%02d-%02d-%02d".format(total / 3600, total % 3600 / 60, total % 60)
    return listOf(title.trim(), episode, time).filter { it.isNotEmpty() }.joinToString(" - ")
}

/** mpv's async writer owns [file] until the PNG IEND chunk is written; wait up to 5 s for it. */
internal suspend fun awaitCompletePng(file: File): Boolean {
    repeat(100) {
        if (pngComplete(file)) return true
        delay(50)
    }
    return false
}

private fun pngComplete(file: File): Boolean {
    if (!file.isFile || file.length() < 12) return false
    return RandomAccessFile(file, "r").use {
        it.seek(it.length() - 12)
        val end = ByteArray(12)
        it.readFully(end)
        end.contentEquals(byteArrayOf(0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126))
    }
}
