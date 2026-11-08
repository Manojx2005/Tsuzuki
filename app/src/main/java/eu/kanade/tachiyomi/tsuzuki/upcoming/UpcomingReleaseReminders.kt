package eu.kanade.tachiyomi.tsuzuki.upcoming

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.NotificationWithIdAndTag
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mihon.domain.upcoming.interactor.GetUpcomingManga
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

internal class UpcomingReminderPreferences(private val store: PreferenceStore) {
    val enabled = store.getStringSet(Preference.appStateKey("tsz_upcoming_reminders"))

    fun setEnabled(id: Long, enable: Boolean) {
        val ids = enabled.get()
        enabled.set(if (enable) ids + id.toString() else ids - id.toString())
    }

    fun lastNotified(id: Long) = store.getLong(Preference.appStateKey("tsz_upcoming_notified_$id"), 0L)
}

internal object UpcomingReleaseReminders {
    fun observe(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val preferences = UpcomingReminderPreferences(Injekt.get())
            val getUpcoming = Injekt.get<GetUpcomingManga>()
            var scheduled = emptyMap<Long, Long>()
            preferences.enabled.changes()
                .flatMapLatest { ids ->
                    if (ids.isEmpty()) {
                        flowOf(emptyMap())
                    } else {
                        getUpcoming.subscribe(emptyList(), emptyList()).map { books ->
                            books.filter { it.id.toString() in ids }.associate { it.id to it.nextUpdate }
                        }
                    }
                }
                .distinctUntilChanged()
                .collect { forecasts ->
                    val now = Clock.System.now()
                    val zone = TimeZone.currentSystemDefault()
                    val desired = forecasts.filter { (id, time) ->
                        shouldScheduleUpcomingRelease(time, preferences.lastNotified(id).get(), now, zone)
                    }
                    if (desired.isEmpty() && scheduled.isEmpty()) return@collect
                    val workManager = WorkManager.getInstance(context)
                    scheduled.forEach { (id, time) ->
                        if (desired[id] != time) workManager.cancelUniqueWork(upcomingWorkName(id, time))
                    }
                    desired.forEach { (id, time) ->
                        if (scheduled[id] == time) return@forEach
                        val delay = (time - now.toEpochMilliseconds()).coerceAtLeast(0L)
                        val request = OneTimeWorkRequestBuilder<UpcomingReleaseReminderJob>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(workDataOf(BOOK_ID to id, FORECAST to time))
                            .build()
                        // The forecast is part of the unique name, so startup cannot reset an
                        // existing delay or replace a worker already delivering this reminder.
                        workManager.enqueueUniqueWork(upcomingWorkName(id, time), ExistingWorkPolicy.KEEP, request)
                    }
                    scheduled = desired
                }
        }
    }
}

internal fun shouldScheduleUpcomingRelease(
    forecast: Long,
    lastNotified: Long,
    now: Instant,
    zone: TimeZone,
): Boolean {
    if (forecast <= 0L) return false
    val date = Instant.fromEpochMilliseconds(forecast).toLocalDateTime(zone).date
    if (date < now.toLocalDateTime(zone).date) return false
    return lastNotified <= 0L || date != Instant.fromEpochMilliseconds(lastNotified).toLocalDateTime(zone).date
}

internal fun shouldDeliverUpcomingRelease(
    forecast: Long,
    scheduledForecast: Long,
    lastNotified: Long,
    now: Instant,
    zone: TimeZone,
): Boolean =
    forecast == scheduledForecast &&
        shouldScheduleUpcomingRelease(forecast, lastNotified, now, zone) &&
        Instant.fromEpochMilliseconds(forecast).toLocalDateTime(zone).date == now.toLocalDateTime(zone).date

internal fun canPostUpcomingRelease(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java)
    return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        manager?.getNotificationChannel(Notifications.CHANNEL_NEW_CHAPTERS)?.importance !=
        NotificationManager.IMPORTANCE_NONE
}

class UpcomingReleaseReminderJob(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = upcomingDeliveryLock.withLock {
        val id = inputData.getLong(BOOK_ID, -1L)
        val scheduledForecast = inputData.getLong(FORECAST, 0L)
        val preferences = UpcomingReminderPreferences(Injekt.get())
        if (id.toString() !in preferences.enabled.get()) return@withLock Result.success()

        val book = Injekt.get<GetUpcomingManga>().findEligible(id) ?: return@withLock Result.success()
        val lastNotified = preferences.lastNotified(id)
        if (id.toString() in preferences.enabled.get() &&
            canPostUpcomingRelease(applicationContext) &&
            shouldDeliverUpcomingRelease(
                book.nextUpdate,
                scheduledForecast,
                lastNotified.get(),
                Clock.System.now(),
                TimeZone.currentSystemDefault(),
            )
        ) {
            notifyBook(book)
            lastNotified.set(book.nextUpdate)
        }
        Result.success()
    }

    private fun notifyBook(book: Manga) {
        val context = applicationContext
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Constants.SHORTCUT_MANGA)
            .setData(Uri.fromParts("tsuzuki", "upcoming-${book.id}", null))
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Constants.MANGA_EXTRA, book.id)
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = context.notificationBuilder(Notifications.CHANNEL_NEW_CHAPTERS) {
            setSmallIcon(R.drawable.ic_tsuzuki_dots)
            setContentTitle(book.title)
            setContentText(context.stringResource(MR.strings.tsz_upcoming_reminder_message))
            setContentIntent(pending)
            setAutoCancel(true)
            setOnlyAlertOnce(true)
        }.build()
        context.notify(listOf(NotificationWithIdAndTag("upcoming-${book.id}", 0, notification)))
    }
}

private fun upcomingWorkName(id: Long, forecast: Long) = "tsz-upcoming-$id-$forecast"

private val upcomingDeliveryLock = Mutex()

private const val BOOK_ID = "book_id"
private const val FORECAST = "forecast"
