package eu.kanade.tachiyomi.tsuzuki.anime.track

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PreferenceStore
import java.util.concurrent.TimeUnit

/** AniList's next episode for one anime (A6.6): episode number and the airing time in epoch milliseconds. */
@Serializable
internal data class AnimeNextAiring(val episode: Int, val airingAt: Long)

/**
 * A6.6 (board `docs/design/boards/remaining-2026-11/Airing.dc.html`): the next airing episode AniList reports for
 * anime bound to AniList, read once per anime per library update. Kept in one preference (no schema change); a
 * time that has passed is never shown (it waits for the next refresh).
 */
internal class AnimeAiringSchedule(preferences: PreferenceStore) {
    private val stored = preferences.getString("tsuzuki_anime_next_airing", "")
    private val json = Json { ignoreUnknownKeys = true }

    fun observe(): Flow<Map<Long, AnimeNextAiring>> = stored.changes().map(::decode)

    fun get(): Map<Long, AnimeNextAiring> = decode(stored.get())

    @Synchronized
    fun put(animeId: Long, next: AnimeNextAiring?) {
        val current = get()
        val updated = if (next == null) current - animeId else current + (animeId to next)
        if (updated != current) stored.set(json.encodeToString(updated.mapKeys { it.key.toString() }))
    }

    private fun decode(raw: String): Map<Long, AnimeNextAiring> =
        if (raw.isBlank()) {
            emptyMap()
        } else {
            runCatching { json.decodeFromString<Map<String, AnimeNextAiring>>(raw) }.getOrDefault(emptyMap())
                .mapNotNull { (key, value) -> key.toLongOrNull()?.let { it to value } }.toMap()
        }
}

/** The AniList time when it is still ahead, else null (stale times are never shown as "−3 h"). */
internal fun AnimeNextAiring?.upcoming(now: Long): AnimeNextAiring? = this?.takeIf { it.airingAt > now }

/** Time left until [airingAt], split for the compact label: under an hour minutes, under a day hours, else days+hours. */
internal sealed interface AiringCountdown {
    data class Minutes(val minutes: Int) : AiringCountdown
    data class Hours(val hours: Int) : AiringCountdown
    data class Days(val days: Int, val hours: Int) : AiringCountdown
}

internal fun airingCountdown(airingAt: Long, now: Long): AiringCountdown {
    val left = (airingAt - now).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(left).toInt()
    return when {
        minutes < 60 -> AiringCountdown.Minutes(minutes.coerceAtLeast(1))
        minutes < 24 * 60 -> AiringCountdown.Hours(minutes / 60)
        else -> AiringCountdown.Days(minutes / (24 * 60), minutes % (24 * 60) / 60)
    }
}
