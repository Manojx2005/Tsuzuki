package eu.kanade.tachiyomi.tsuzuki.sourcehealth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import java.time.Instant
import java.time.ZoneId

internal const val SOURCE_HEALTH_DAYS = 7
private const val SLOW_RESPONSE_MILLIS = 1_500L

internal enum class SourceHealthStatus { WORKING, SLOW, DOWN, UNKNOWN }

@Serializable
internal enum class SourceHealthError { HTTP, CLOUDFLARE, TIMEOUT, NETWORK }

@Serializable
internal data class SourceHealthFailure(val at: Long, val kind: SourceHealthError, val httpCode: Int? = null)

internal data class SourceHealthSample(val at: Long, val elapsedMillis: Long, val failure: SourceHealthFailure? = null)

@Serializable
internal data class SourceHealthDay(
    val day: Long,
    val latestAt: Long = 0,
    val lastRequestFailed: Boolean = false,
    val successfulRequests: Long = 0,
    val totalMillis: Long = 0,
) {
    val status: SourceHealthStatus
        get() = when {
            lastRequestFailed -> SourceHealthStatus.DOWN
            successfulRequests == 0L -> SourceHealthStatus.UNKNOWN
            totalMillis / successfulRequests >= SLOW_RESPONSE_MILLIS -> SourceHealthStatus.SLOW
            else -> SourceHealthStatus.WORKING
        }

    fun add(sample: SourceHealthSample): SourceHealthDay {
        val success = sample.failure == null
        val latest = sample.at >= latestAt
        return copy(
            latestAt = maxOf(sample.at, latestAt),
            lastRequestFailed = if (latest) !success else lastRequestFailed,
            successfulRequests = successfulRequests + if (success) 1 else 0,
            totalMillis = totalMillis + if (success) sample.elapsedMillis else 0,
        )
    }
}

@Serializable
internal data class SourceHealth(
    val lastSuccessAt: Long = 0,
    val lastError: SourceHealthFailure? = null,
    val days: List<SourceHealthDay> = emptyList(),
) {
    fun recentDays(today: Long): List<SourceHealthDay?> =
        (today - SOURCE_HEALTH_DAYS + 1..today).map { day -> days.firstOrNull { it.day == day } }

    fun status(today: Long): SourceHealthStatus {
        val observed = recentDays(today).filterNotNull()
        if (observed.lastOrNull()?.lastRequestFailed == true) return SourceHealthStatus.DOWN
        val average = averageMillis(today) ?: return SourceHealthStatus.UNKNOWN
        return if (average >= SLOW_RESPONSE_MILLIS) SourceHealthStatus.SLOW else SourceHealthStatus.WORKING
    }

    fun averageMillis(today: Long): Long? {
        val observed = recentDays(today).filterNotNull()
        val successful = observed.sumOf { it.successfulRequests }
        return if (successful > 0) observed.sumOf { it.totalMillis } / successful else null
    }

    fun add(sample: SourceHealthSample, day: Long): SourceHealth {
        val previous = days.firstOrNull { it.day == day } ?: SourceHealthDay(day)
        val mostRecentDay = maxOf(day, days.maxOfOrNull { it.day } ?: day)
        val recent = days.filter { it.day != day } + previous.add(sample)
        return copy(
            lastSuccessAt = if (sample.failure == null) maxOf(lastSuccessAt, sample.at) else lastSuccessAt,
            lastError = sample.failure?.takeIf { it.at >= (lastError?.at ?: 0) } ?: lastError,
            days = recent.filter { it.day > mostRecentDay - SOURCE_HEALTH_DAYS },
        )
    }
}

internal class SourceHealthStore(private val preferences: PreferenceStore) {
    private fun recordFor(sourceId: Long) =
        preferences.getString(Preference.appStateKey("tsz_source_health_$sourceId"), "")

    fun get(sourceId: Long): SourceHealth = decode(recordFor(sourceId).get())

    fun observe(sourceId: Long): Flow<SourceHealth> =
        recordFor(sourceId).changes().map(::decode).flowOn(Dispatchers.IO)

    @Synchronized
    fun record(sourceId: Long, sample: SourceHealthSample, zone: ZoneId = ZoneId.systemDefault()) {
        val day = Instant.ofEpochMilli(sample.at).atZone(zone).toLocalDate().toEpochDay()
        val preference = recordFor(sourceId)
        preference.set(Json.encodeToString(decode(preference.get()).add(sample, day)))
    }

    private fun decode(raw: String): SourceHealth {
        if (raw.isEmpty()) return SourceHealth()
        return try {
            Json.decodeFromString(raw)
        } catch (_: SerializationException) {
            SourceHealth()
        }
    }
}
