package eu.kanade.tachiyomi.tsuzuki.download

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.isNovelSource

internal fun activeMangaDownloads(
    queue: List<Download>,
    parallelSources: Int,
    pausedSources: Set<Long>,
): List<Download> = queue.asSequence()
    .filterNot { it.source.isNovelSource() || it.source.id in pausedSources }
    .filter { it.status.value <= Download.State.DOWNLOADING.value }
    .distinctBy { it.source }
    .take(parallelSources.coerceAtLeast(1))
    .toList()

internal fun pendingDownloadsForStart(queue: List<Download>, retryErrors: Boolean): List<Download> =
    queue.filter { it.status != Download.State.DOWNLOADED && (retryErrors || it.status != Download.State.ERROR) }

internal data class DownloadQueueControls(
    val pausedMangaSources: Set<Long> = emptySet(),
    val retryGeneration: Long = 0,
)
