package eu.kanade.presentation.tsuzuki.history

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.anime.model.AnimeHistoryDisplay
import java.time.LocalDate
import java.util.Date

class AnimeHistoryStatsTest {

    private val today = LocalDate.of(2026, 11, 24)
    private val startOfToday = 1_000_000_000L

    private fun row(episodeId: Long, lastSeen: Long) = AnimeHistoryDisplay(
        animeId = 1,
        title = "",
        thumbnailUrl = null,
        episodeId = episodeId,
        episodeName = "",
        episodeNumber = episodeId.toDouble(),
        previewUrl = null,
        seen = false,
        position = 0,
        duration = 0,
        lastSeen = Date(lastSeen),
    )

    @Test
    fun `today counts watch time and the episodes last watched today`() {
        val stats = animeHistoryStats(
            rows = listOf(row(1, startOfToday + 10), row(2, startOfToday + 20), row(3, startOfToday - 1)),
            watchSeconds = mapOf(today to 25 * 60L + 59, today.minusDays(1) to 120L),
            today = today,
            startOfToday = startOfToday,
        )
        assertEquals(AnimeHistoryStats(todayMinutes = 25, episodesToday = 2, streak = 2), stats)
    }

    @Test
    fun `a day under a minute breaks the streak, and today may still be ahead`() {
        val days = mapOf(
            today.minusDays(1) to 600L,
            today.minusDays(2) to 600L,
            today.minusDays(3) to 30L,
            today.minusDays(4) to 600L,
        )
        assertEquals(2, animeHistoryStats(emptyList(), days, today, startOfToday).streak)
    }

    @Test
    fun `no watching is zero, not missing`() {
        assertEquals(AnimeHistoryStats(0, 0, 0), animeHistoryStats(emptyList(), emptyMap(), today, startOfToday))
    }
}
