package eu.kanade.tachiyomi.tsuzuki.download

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.isNovelSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

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

internal data class DownloadQueueSchedule(
    val pausedMangaSources: Set<Long> = emptySet(),
    val revision: Long = 0,
)

internal class DownloadQueueControls(
    private val queue: StateFlow<List<Download>>,
    private val pausedNovels: StateFlow<Set<Long>>,
    private val startQueuedDownloads: () -> Unit,
) {
    private val schedule = MutableStateFlow(DownloadQueueSchedule())
    val changes = schedule.asStateFlow()
    val pausedMangaSourceIds = schedule.map { it.pausedMangaSources }.distinctUntilChanged()

    fun mangaQueue(sourceId: Long) = combine(queue, schedule) { downloads, state ->
        if (sourceId in state.pausedMangaSources) emptyList() else downloads
    }

    fun setMangaSourcePaused(sourceId: Long, paused: Boolean) {
        schedule.update { state ->
            val sources = if (paused) state.pausedMangaSources + sourceId else state.pausedMangaSources - sourceId
            state.copy(pausedMangaSources = sources)
        }
        if (!paused) startQueuedDownloads()
    }

    fun clearMangaPauses() {
        schedule.update { it.copy(pausedMangaSources = emptySet()) }
    }

    fun retryFailedDownloads(chapterIds: Set<Long>) {
        val failed = queue.value.filter { it.chapterId in chapterIds && it.status == Download.State.ERROR }
        if (failed.isEmpty()) return
        failed.forEach {
            it.clearError()
            it.status = Download.State.QUEUE
        }
        wakeScheduler()
        val pausedMangaSources = schedule.value.pausedMangaSources
        if (failed.any { download ->
                if (download.source.isNovelSource()) {
                    download.mangaId !in pausedNovels.value
                } else {
                    download.source.id !in pausedMangaSources
                }
            }
        ) {
            startQueuedDownloads()
        }
    }

    // A rapid retry or resume can make an attempt runnable before its previous job has exited.
    fun attemptFinished(download: Download) {
        if (download.status == Download.State.QUEUE) wakeScheduler()
    }

    private fun wakeScheduler() {
        schedule.update { it.copy(revision = it.revision + 1) }
    }
}
