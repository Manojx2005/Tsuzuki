package eu.kanade.tachiyomi.tsuzuki.anime.download

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.NetworkState
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.roundToInt

internal data class AnimeDownloadNotificationProgress(
    val entry: AnimeDownloadEntry,
    val position: Int,
    val total: Int,
    val fraction: Float?,
)

internal data class AnimeDownloadNotificationResult(val saved: Int, val failed: List<AnimeDownloadEntry>)

internal class AnimeDownloadNotificationBatch(queue: AnimeDownloadQueue) {
    private val episodeIds = linkedSetOf<Long>()

    init {
        observe(queue)
    }

    fun observe(queue: AnimeDownloadQueue) {
        queue.entries.filter { it.status == AnimeDownloadStatus.Queued }.forEach { episodeIds.add(it.episodeId) }
    }

    fun progress(
        queue: AnimeDownloadQueue,
        progress: Pair<Long, AnimeDownloadProgress>?,
    ): AnimeDownloadNotificationProgress? {
        observe(queue)
        if (queue.paused) return null
        val entry = queue.entries.firstOrNull { it.status == AnimeDownloadStatus.Queued } ?: return null
        val fraction = progress?.second?.fraction?.takeIf { progress.first == entry.episodeId }
        val remaining = queue.entries.count { it.status == AnimeDownloadStatus.Queued }
        return AnimeDownloadNotificationProgress(entry, episodeIds.size - remaining + 1, episodeIds.size, fraction)
    }

    fun result(queue: AnimeDownloadQueue): AnimeDownloadNotificationResult {
        val completed = queue.entries.filter { it.episodeId in episodeIds }
        return AnimeDownloadNotificationResult(
            saved = completed.count { it.status == AnimeDownloadStatus.Saved },
            failed = completed.filter { it.status == AnimeDownloadStatus.Failed },
        )
    }
}

internal enum class AnimeDownloadDeferred { PAUSED, WIFI }

internal fun AnimeDownloadQueue.deferredDownloadNotification(network: NetworkState): AnimeDownloadDeferred? = when {
    !hasQueuedEpisodes -> null
    paused -> AnimeDownloadDeferred.PAUSED
    wifiOnly && !network.isWifi -> AnimeDownloadDeferred.WIFI
    else -> null
}

internal class AnimeDownloadNotifier(private val context: Context) {
    private val hiddenContent
        get() = Injekt.get<SecurityPreferences>().hideNotificationContent.get()

    fun ongoing(progress: AnimeDownloadNotificationProgress?): Notification =
        context.notificationBuilder(Notifications.CHANNEL_DOWNLOADER_PROGRESS) {
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOnlyAlertOnce(true)
            setOngoing(true)
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
            setContentTitle(progress?.entry?.let(::title) ?: context.getString(R.string.tsz_anime_downloading))
            if (progress != null) {
                setContentText(
                    context.getString(R.string.tsz_anime_download_position, progress.position, progress.total),
                )
            }
            val fraction = progress?.fraction
            setProgress(1000, fraction?.let { (it * 1000).roundToInt() } ?: 0, fraction == null)
            addAction(
                R.drawable.ic_pause_24dp,
                context.stringResource(MR.strings.action_pause),
                action(ACTION_PAUSE),
            )
            addCancelAction()
        }.build()

    fun showProgress(progress: AnimeDownloadNotificationProgress) = context.notify(ACTIVE_ID, ongoing(progress))

    fun showDeferred(status: AnimeDownloadDeferred?) {
        when (status) {
            null -> context.cancelNotification(DEFERRED_ID)
            else -> context.notify(DEFERRED_ID, Notifications.CHANNEL_DOWNLOADER_PROGRESS) {
                setSmallIcon(R.drawable.ic_pause_24dp)
                setOnlyAlertOnce(true)
                setOngoing(true)
                setContentTitle(
                    if (status == AnimeDownloadDeferred.PAUSED) {
                        context.stringResource(MR.strings.download_notifier_download_paused)
                    } else {
                        context.getString(R.string.tsz_anime_download_waiting_wifi)
                    },
                )
                setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
                if (status == AnimeDownloadDeferred.PAUSED) {
                    addAction(
                        R.drawable.ic_play_arrow_24dp,
                        context.stringResource(MR.strings.action_resume),
                        action(ACTION_RESUME),
                    )
                } else {
                    addAction(
                        R.drawable.ic_pause_24dp,
                        context.stringResource(MR.strings.action_pause),
                        action(ACTION_PAUSE),
                    )
                }
                addCancelAction()
            }
        }
    }

    fun showError(entry: AnimeDownloadEntry) {
        context.notify(ERROR_ID, Notifications.CHANNEL_DOWNLOADER_ERROR) {
            setSmallIcon(R.drawable.ic_warning_white_24dp)
            setContentTitle(context.stringResource(MR.strings.download_notifier_title_error))
            setContentText(title(entry))
            setOnlyAlertOnce(true)
            setAutoCancel(true)
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
            addAction(0, context.stringResource(MR.strings.action_retry), action(ACTION_RETRY, entry.episodeId))
        }
    }

    fun showComplete(result: AnimeDownloadNotificationResult) {
        if (result.saved == 0) return
        context.notify(COMPLETE_ID, Notifications.CHANNEL_DOWNLOADER_PROGRESS) {
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setContentTitle(context.getString(R.string.tsz_anime_download_complete))
            setContentText(context.getString(R.string.tsz_anime_download_counts, result.saved, result.failed.size))
            setAutoCancel(true)
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
        }
    }

    private fun title(entry: AnimeDownloadEntry): String = if (hiddenContent) {
        context.getString(R.string.tsz_anime_downloading)
    } else {
        "${entry.animeTitle} · ${entry.episodeTitle}"
    }

    private fun NotificationCompat.Builder.addCancelAction() {
        addAction(R.drawable.ic_close_24dp, context.stringResource(MR.strings.action_cancel_all), action(ACTION_CANCEL))
    }

    private fun action(name: String, episodeId: Long = -1): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, NotificationReceiver::class.java).setAction(name).putExtra(EXTRA_EPISODE_ID, episodeId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTIVE_ID = -204
        private const val DEFERRED_ID = -205
        private const val ERROR_ID = -206
        private const val COMPLETE_ID = -207
        private const val ACTION_PAUSE = "tsuzuki.anime.download.PAUSE"
        private const val ACTION_RESUME = "tsuzuki.anime.download.RESUME"
        private const val ACTION_CANCEL = "tsuzuki.anime.download.CANCEL"
        private const val ACTION_RETRY = "tsuzuki.anime.download.RETRY"
        private const val EXTRA_EPISODE_ID = "episode_id"

        fun handleAction(context: Context, intent: Intent): Boolean {
            when (intent.action) {
                ACTION_PAUSE -> Injekt.get<AnimeDownloads>().setPaused(true)
                ACTION_RESUME -> Injekt.get<AnimeDownloads>().setPaused(false)
                ACTION_CANCEL -> Injekt.get<AnimeDownloads>().cancelQueued()
                ACTION_RETRY -> {
                    val episodeId = intent.getLongExtra(EXTRA_EPISODE_ID, -1)
                    if (episodeId != -1L) {
                        context.cancelNotification(ERROR_ID)
                        Injekt.get<AnimeDownloads>().retry(episodeId)
                    }
                }
                else -> return false
            }
            return true
        }
    }
}
