package eu.kanade.tachiyomi.tsuzuki.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.isAppUpdateSkipped
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import eu.kanade.tachiyomi.util.system.updaterEnabled
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.model.Release
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub for a new Tsuzuki release once a day in the background and posts one notification per new version.
 * Tapping it opens the same "New update" screen the launch check shows. Builds without the updater do nothing.
 */
class TsuzukiAppUpdateJob(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!updaterEnabled) return Result.success()
        val result = try {
            AppUpdateChecker().checkForUpdate(forceCheck = true)
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Background app update check failed" }
            return Result.success()
        }
        if (result !is GetApplicationRelease.Result.NewUpdate) return Result.success()

        val release = result.release
        val skipped = Injekt.get<BasePreferences>().skippedAppUpdate.get()
        val lastNotified = lastNotifiedVersion()
        if (shouldNotify(release.version, skipped, lastNotified.get())) {
            notifyUpdate(applicationContext, release)
            lastNotified.set(release.version)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "TsuzukiAppUpdateCheck"
        private const val CHANNEL_ID = "tsz_app_update_channel"
        private const val NOTIFICATION_ID = -901
        private const val ACTION_SHOW_UPDATE = "io.github.manojx2005.tsuzuki.SHOW_APP_UPDATE"
        private const val EXTRA_VERSION = "tsz_update_version"
        private const val EXTRA_INFO = "tsz_update_info"
        private const val EXTRA_RELEASE_LINK = "tsz_update_release_link"
        private const val EXTRA_DOWNLOAD_LINK = "tsz_update_download_link"

        /** Schedules the daily check (kept across app starts); does nothing in builds without the updater. */
        fun setup(context: Context) {
            if (!updaterEnabled) return
            val request = PeriodicWorkRequestBuilder<TsuzukiAppUpdateJob>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** One notification per version, never for a version the user chose to skip. */
        internal fun shouldNotify(version: String, skippedVersion: String, lastNotifiedVersion: String): Boolean =
            version != lastNotifiedVersion && !isAppUpdateSkipped(version, skippedVersion)

        /** The update screen for a tapped update notification, or null for any other intent. */
        fun screenFrom(intent: Intent): NewUpdateScreen? {
            if (intent.action != ACTION_SHOW_UPDATE) return null
            return NewUpdateScreen(
                versionName = intent.getStringExtra(EXTRA_VERSION) ?: return null,
                changelogInfo = intent.getStringExtra(EXTRA_INFO).orEmpty(),
                releaseLink = intent.getStringExtra(EXTRA_RELEASE_LINK) ?: return null,
                downloadLink = intent.getStringExtra(EXTRA_DOWNLOAD_LINK) ?: return null,
            )
        }

        private fun lastNotifiedVersion() =
            Injekt.get<PreferenceStore>().getString("tsz_app_update_last_notified", "")

        private fun notifyUpdate(context: Context, release: Release) {
            NotificationManagerCompat.from(context).createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.stringResource(MR.strings.tsz_app_update_channel))
                    .build(),
            )
            val intent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_SHOW_UPDATE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_VERSION, release.version)
                putExtra(EXTRA_INFO, release.info)
                putExtra(EXTRA_RELEASE_LINK, release.releaseLink)
                putExtra(EXTRA_DOWNLOAD_LINK, release.downloadLink)
            }
            val pending = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            context.notify(
                NOTIFICATION_ID,
                context.notificationBuilder(CHANNEL_ID) {
                    setSmallIcon(R.drawable.ic_tsuzuki_dots)
                    setContentTitle(context.stringResource(MR.strings.tsz_app_update_available, release.version))
                    setContentText(context.stringResource(MR.strings.tsz_app_update_tap))
                    setContentIntent(pending)
                    setAutoCancel(true)
                }.build(),
            )
        }
    }
}
