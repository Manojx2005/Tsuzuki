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
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.util.system.NetworkState
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.networkStateFlow
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.selects.select
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimeDownloadJob(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (!TsuzukiAnime.enabled) return Result.success()
        val wifiOnly = inputData.getBoolean(WIFI_ONLY, true)
        if (!applicationContext.activeNetworkState().allowsAnimeDownloads(wifiOnly)) return Result.retry()
        setForegroundSafely()
        return coroutineScope {
            val lostNetwork = async {
                applicationContext.networkStateFlow()
                    .onStart { emit(applicationContext.activeNetworkState()) }
                    .first { !it.allowsAnimeDownloads(wifiOnly) }
            }
            val download = async { Injekt.get<AnimeDownloads>().drain(wifiOnly) }
            try {
                select {
                    lostNetwork.onAwait { Result.retry() }
                    download.onAwait { Result.success() }
                }
            } finally {
                lostNetwork.cancel()
                download.cancel()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = applicationContext.notificationBuilder(Notifications.CHANNEL_DOWNLOADER_PROGRESS) {
            setContentTitle(applicationContext.getString(R.string.tsz_anime_downloading))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOngoing(true)
        }.build()
        return ForegroundInfo(
            -204,
            notification,
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
