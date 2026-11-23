package eu.kanade.presentation.tsuzuki.history

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.anime.model.AnimeHistoryDisplay
import tachiyomi.domain.items.episode.model.Episode
import java.time.Instant
import java.util.Date

class AnimeHistoryDisplayTest {
    @Test
    fun animePillRequiresEnabledAnimeAndVisibleHistory() {
        assertFalse(showAnimeHistory(hasHistory = true, animeEnabled = false))
        assertFalse(showAnimeHistory(hasHistory = false, animeEnabled = true))
        assertTrue(showAnimeHistory(hasHistory = true, animeEnabled = true))
    }

    @Test
    fun historyGroupsByLocalDayAndMatchesBothAnimeAndEpisodeNames() {
        val beforeMidnight = row(1, "Sparrow", "Night", "2026-11-07T14:59:00Z")
        val afterMidnight = row(2, "Heron", "Day", "2026-11-07T15:01:00Z")
        val latest = row(3, "Sparrow", "Dawn", "2026-11-07T16:00:00Z")
        val records = listOf(beforeMidnight, latest, afterMidnight)
        val tokyo = TimeZone.of("Asia/Tokyo")
        val groups = groupAnimeHistory(records, null, tokyo)

        assertEquals(listOf(LocalDate(2026, 11, 8), LocalDate(2026, 11, 7)), groups.keys.toList())
        assertEquals(listOf(latest, afterMidnight), groups.values.first())
        assertEquals(listOf(latest, beforeMidnight), groupAnimeHistory(records, "sparrow", tokyo).values.flatten())
        assertEquals(listOf(afterMidnight), groupAnimeHistory(records, "day", tokyo).values.flatten())
    }

    @Test
    fun lastOnlyKeepsTheNewestEpisodeOfEachAnime() {
        val older = row(1, "Sparrow", "One", "2026-11-07T10:00:00Z")
        val newer = row(2, "Sparrow", "Two", "2026-11-07T12:00:00Z")
        val other = row(3, "Heron", "One", "2026-11-07T11:00:00Z").copy(animeId = 9)
        val utc = TimeZone.UTC

        assertEquals(
            listOf(newer, other),
            groupAnimeHistory(listOf(older, newer, other), null, utc, lastOnly = true).values.flatten(),
        )
        assertEquals(3, groupAnimeHistory(listOf(older, newer, other), null, utc).values.flatten().size)
    }

    @Test
    fun partialHistoryResumesExactEpisodeAndFinishedHistoryAdvancesPastSeenEpisodes() {
        val row = row(1, "Sparrow", "Night", "2026-11-07T14:59:00Z")
        val first = Episode(8, "/1", "One", id = 1, episodeNumber = 1.0)
        val second = Episode(8, "/2", "Two", id = 2, episodeNumber = 2.0, seen = true)
        val third = Episode(8, "/3", "Three", id = 3, episodeNumber = 3.0)
        val episodes = listOf(third, first, second)

        assertEquals(1L, animeHistoryResumeTarget(row, episodes))
        assertEquals(3L, animeHistoryResumeTarget(row.copy(seen = true), episodes))
        assertEquals(1L, animeHistoryResumeTarget(row.copy(seen = true), listOf(first, second)))
    }

    private fun row(id: Long, title: String, episode: String, time: String) = AnimeHistoryDisplay(
        animeId = 8,
        title = title,
        thumbnailUrl = null,
        episodeId = id,
        episodeName = episode,
        episodeNumber = id.toDouble(),
        previewUrl = null,
        seen = false,
        position = 20,
        duration = 100,
        lastSeen = Date.from(Instant.parse(time)),
    )
}
