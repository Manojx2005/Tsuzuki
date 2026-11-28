package eu.kanade.tachiyomi.tsuzuki.anime.track

import eu.kanade.presentation.tsuzuki.updates.airingDay
import eu.kanade.presentation.tsuzuki.updates.mergeAiringToday
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

class AnimeAiringScheduleTest {
    private val minute = 60_000L
    private val hour = 60 * minute

    @Test
    fun countdownUsesMinutesHoursThenDaysAndHours() {
        assertEquals(AiringCountdown.Minutes(42), airingCountdown(42 * minute, 0))
        assertEquals(AiringCountdown.Minutes(1), airingCountdown(10_000, 0))
        assertEquals(AiringCountdown.Hours(5), airingCountdown(5 * hour + 20 * minute, 0))
        assertEquals(AiringCountdown.Days(2, 4), airingCountdown(52 * hour + 10 * minute, 0))
    }

    @Test
    fun aPastAiringTimeIsNeverShown() {
        assertNull(AnimeNextAiring(13, 1_000).upcoming(now = 2_000))
        assertEquals(13, AnimeNextAiring(13, 3_000).upcoming(now = 2_000)?.episode)
        assertNull(null.upcoming(now = 0))
    }

    @Test
    fun todayUsesTheDeviceTimeZone() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        // 2026-11-28 23:30 in Tokyo is still "today" there although it is the 28th 14:30 UTC.
        val now = Instant.parse("2026-11-28T14:30:00Z")
        val day = airingDay(now, tokyo)
        assertEquals(Instant.parse("2026-11-27T15:00:00Z").toEpochMilli(), day.first)
        assertEquals(Instant.parse("2026-11-28T15:00:00Z").toEpochMilli() - 1, day.last)
    }

    @Test
    fun aniListTimeReplacesTheSourceEstimateForTheSameAnime() {
        val day = 0L until 24 * hour
        val source = listOf(
            AnimeUpdateEpisode(1, "A", 10, "Ep 12", null, 9 * hour),
            AnimeUpdateEpisode(2, "B", 20, "Ep 4", null, 20 * hour),
        )
        val aniList = mapOf(
            1L to AnimeNextAiring(13, 21 * hour),
            3L to AnimeNextAiring(1, 8 * hour), // not in the library: left out
            2L to AnimeNextAiring(5, 30 * hour), // tomorrow: B keeps the source entry
        )
        val merged = mergeAiringToday(source, aniList, mapOf(1L to "A", 2L to "B"), day) { "Ep $it" }
        assertEquals(listOf(2L to "Ep 4", 1L to "Ep 13"), merged.map { it.animeId to it.name })
        assertEquals(21 * hour, merged.last().timestamp)
    }
}
