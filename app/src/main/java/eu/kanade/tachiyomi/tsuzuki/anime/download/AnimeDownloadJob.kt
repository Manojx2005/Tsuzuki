package eu.kanade.tachiyomi.tsuzuki.anime.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.util.system.NetworkState
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.networkStateFlow
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimeDownloadJob(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val notifier = AnimeDownloadNotifier(context)

    @OptIn(FlowPreview::class)
    override suspend fun doWork(): Result {
        if (!TsuzukiAnime.enabled) return Result.success()
        val wifiOnly = inputData.getBoolean(WIFI_ONLY, true)
        if (!applicationContext.activeNetworkState().allowsAnimeDownloads(wifiOnly)) return Result.retry()
        val downloads = Injekt.get<AnimeDownloads>()
        val queue = downloads.readyQueue()
        if (queue.paused || !queue.hasQueuedEpisodes) return Result.success()
        val batch = AnimeDownloadNotificationBatch(queue)
        setForegroundSafely()
        return coroutineScope {
            var shownFailure: Long? = null
            val notifications = launch {
                combine(downloads.state, downloads.progress) { currentQueue, progress ->
                    batch.progress(currentQueue, progress) to batch.result(currentQueue).failed.lastOrNull()
                }.distinctUntilChanged().sample(500).collect { (progress, error) ->
                    progress?.let(notifier::showProgress)
                    if (error != null && error.episodeId != shownFailure) {
                        shownFailure = error.episodeId
                        notifier.showError(error)
                    }
                }
            }
            val lostNetwork = async {
                applicationContext.networkStateFlow()
                    .onStart { emit(applicationContext.activeNetworkState()) }
                    .first { !it.allowsAnimeDownloads(wifiOnly) }
            }
            val download = async { downloads.drain(wifiOnly) }
            try {
                val completed = select {
                    lostNetwork.onAwait { false }
                    download.onAwait { true }
                }
                notifications.cancelAndJoin()
                val result = batch.result(downloads.state.value)
                result.failed.lastOrNull()?.takeUnless { it.episodeId == shownFailure }?.let(notifier::showError)
                if (completed) notifier.showComplete(result)
                if (completed) Result.success() else Result.retry()
            } finally {
                notifications.cancel()
                lostNetwork.cancel()
                download.cancel()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val queue = Injekt.get<AnimeDownloads>().readyQueue()
        val progress = AnimeDownloadNotificationBatch(queue).progress(queue, null)
        return ForegroundInfo(
            AnimeDownloadNotifier.ACTIVE_ID,
            notifier.ongoing(progress),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
    }

    companion object {
        private const val NAME = "tsuzuki-anime-download"
        private const val WIFI_ONLY = "wifi_only"

        fun schedule(context: Context, wifiOnly: Boolean, policy: ExistingWorkPolicy) {
            if (!TsuzukiAnime.enabled) return
            val constraints = Constraints.Builder().apply {
                if (wifiOnly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val wifi = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                    setRequiredNetworkRequest(wifi, NetworkType.CONNECTED)
                } else {
                    setRequiredNetworkType(NetworkType.CONNECTED)
                }
            }.build()
            val request = OneTimeWorkRequestBuilder<AnimeDownloadJob>()
                .setConstraints(constraints)
                .setInputData(workDataOf(WIFI_ONLY to wifiOnly))
                .build()
            context.workManager.enqueueUniqueWork(NAME, policy, request)
        }

        fun cancel(context: Context) {
            context.workManager.cancelUniqueWork(NAME)
        }
    }
}

internal fun NetworkState.allowsAnimeDownloads(wifiOnly: Boolean): Boolean = isOnline && (!wifiOnly || isWifi)
