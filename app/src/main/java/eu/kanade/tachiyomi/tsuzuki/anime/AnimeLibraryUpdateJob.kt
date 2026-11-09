package eu.kanade.tachiyomi.tsuzuki.anime

import android.content.Context
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.manga.interactor.FetchInterval
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

internal class AnimeLibraryUpdateJob(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = updateLock.withLock {
        if (!TsuzukiAnime.enabled) return Result.success()
        val preferences = Injekt.get<LibraryPreferences>()
        if (DEVICE_ONLY_ON_WIFI in preferences.autoUpdateDeviceRestrictions.get() &&
            !applicationContext.isConnectedToWifi()
        ) {
            return Result.retry()
        }
        val library = Injekt.get<AnimeLibraryStore>()
        val restrictions = preferences.autoUpdateMangaRestrictions.get()
        val skipDays = preferences.skipUpdateTime.get()
        val throttle = preferences.autoUpdateThrottle.get().toLong()
        val fetchDetails = preferences.autoUpdateMetadata.get()
        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val (_, windowEnd) = Injekt.get<FetchInterval>().getWindow(now.toLocalDateTime(zone).date, zone)
        val entries = library.getLibrary().filter {
            shouldUpdateAnime(it, restrictions, skipDays, now.toEpochMilliseconds(), windowEnd)
        }
        if (entries.isEmpty()) return Result.success()
        val sources = Injekt.get<AnimeSourceManager>().refresh().associateBy { it.source.id }
        var requested = false
        entries.forEach { show ->
            val source = sources[show.source] ?: return@forEach
            if (requested) delay(throttle)
            requested = true
            try {
                library.refresh(source, show.toSourceAnime(), fetchDetails = fetchDetails, favoriteOnly = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Could not update library anime " + show.id }
            }
        }
        Result.success()
    }

    companion object {
        private const val PERIODIC = "tsuzuki-anime-library-periodic"
        internal const val MANUAL = "tsuzuki-anime-library-manual"
        private val updateLock = Mutex()

        fun setupTask(context: Context, prefInterval: Int? = null, enabled: Boolean = TsuzukiAnime.enabled) {
            if (!enabled) return
            val preferences = Injekt.get<LibraryPreferences>()
            val interval = prefInterval ?: preferences.autoUpdateInterval.get()
            if (interval <= 0) {
                context.workManager.cancelUniqueWork(PERIODIC)
                return
            }
            val request = PeriodicWorkRequestBuilder<AnimeLibraryUpdateJob>(
                interval.toLong(),
                TimeUnit.HOURS,
                10,
                TimeUnit.MINUTES,
            )
                .setConstraints(libraryUpdateConstraints(preferences.autoUpdateDeviceRestrictions.get()))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                .build()
            context.workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            context.workManager.cancelUniqueWork(PERIODIC)
            context.workManager.cancelUniqueWork(MANUAL)
        }

        fun start(context: Context): Boolean {
            if (!TsuzukiAnime.enabled) return false
            val restrictions = Injekt.get<LibraryPreferences>().autoUpdateDeviceRestrictions.get()
            val request = OneTimeWorkRequestBuilder<AnimeLibraryUpdateJob>()
                .setConstraints(libraryUpdateConstraints(restrictions))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                .build()
            context.workManager.enqueueUniqueWork(MANUAL, ExistingWorkPolicy.KEEP, request)
            return true
        }
    }
}

internal fun shouldUpdateAnime(
    show: Anime,
    restrictions: Set<String>,
    skipDays: Int,
    now: Long,
    windowEnd: Long,
): Boolean {
    val lastUpdate = show.lastUpdate ?: 0L
    return when {
        !show.favorite || show.fetchType != FetchType.Episodes -> false
        show.updateStrategy == AnimeUpdateStrategy.ONLY_FETCH_ONCE && show.totalCount > 0 -> false
        MANGA_NON_COMPLETED in restrictions && show.status == SAnime.COMPLETED.toLong() -> false
        MANGA_HAS_UNREAD in restrictions && show.seenCount < show.totalCount -> false
        MANGA_NON_READ in restrictions && show.totalCount > 0 && show.seenCount == 0L -> false
        MANGA_OUTSIDE_RELEASE_PERIOD in restrictions && (show.nextUpdate ?: 0L) > windowEnd -> false
        skipDays > 0 && lastUpdate > 0 -> now - lastUpdate >= TimeUnit.DAYS.toMillis(skipDays.toLong())
        else -> true
    }
}

private fun libraryUpdateConstraints(restrictions: Set<String>): Constraints {
    val networkType = if (DEVICE_NETWORK_NOT_METERED in restrictions) {
        NetworkType.UNMETERED
    } else {
        NetworkType.CONNECTED
    }
    val networkRequest = NetworkRequest.Builder().apply {
        removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        if (DEVICE_ONLY_ON_WIFI in restrictions) {
            addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        }
        if (DEVICE_NETWORK_NOT_METERED in restrictions) {
            addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }
    }
        .build()
    return Constraints.Builder()
        // 'networkRequest' only applies to Android 9+, otherwise 'networkType' is used
        .setRequiredNetworkRequest(networkRequest, networkType)
        .setRequiresCharging(DEVICE_CHARGING in restrictions)
        .setRequiresBatteryNotLow(true)
        .build()
}
