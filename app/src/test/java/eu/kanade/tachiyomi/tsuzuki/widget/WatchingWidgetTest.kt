package eu.kanade.tachiyomi.tsuzuki.widget

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.anime.model.AnimeHistoryDisplay
import java.util.Date

class WatchingWidgetTest {
    @Test
    fun showsTheNewestInProgressEpisodePerAnime() {
        val entries = listOf(
            item(anime = 3, episode = 30, lastSeen = 10),
            item(anime = 1, episode = 10, lastSeen = 20),
            item(anime = 2, episode = 20, lastSeen = 25, duration = 0),
            item(anime = 1, episode = 11, lastSeen = 30),
            item(anime = 4, episode = 40, lastSeen = 40, seen = true),
            item(anime = 5, episode = 50, lastSeen = 50, position = 0),
            item(anime = 6, episode = 60, lastSeen = 60, position = 600),
        )
        assertEquals(listOf(11L, 20L, 30L), selectWatchingWidgetItems(entries, 4).map { it.episodeId })
        assertEquals(listOf(11L), selectWatchingWidgetItems(entries, 1).map { it.episodeId })
        assertTrue(selectWatchingWidgetItems(emptyList(), 4).isEmpty())
    }

    @Test
    fun fitsAtMostFourRowsAtTheCurrentWidgetHeight() {
        assertEquals(listOf(1, 1, 2, 3, 4, 4), listOf(96f, 161f, 162f, 238f, 314f, 500f).map(::watchingWidgetSlots))
        val entries = (1L..5L).map { item(anime = it, episode = it, lastSeen = it) }
        assertEquals(listOf(5L, 4L, 3L, 2L), selectWatchingWidgetItems(entries, 9).map { it.episodeId })
    }

    private fun item(
        anime: Long,
        episode: Long,
        lastSeen: Long,
        seen: Boolean = false,
        position: Long = 100,
        duration: Long = 600,
    ) = AnimeHistoryDisplay(
        animeId = anime,
        title = "Anime $anime",
        thumbnailUrl = null,
        episodeId = episode,
        episodeName = "Episode $episode",
        episodeNumber = episode.toDouble(),
        previewUrl = null,
        seen = seen,
        position = position,
        duration = duration,
        lastSeen = Date(lastSeen),
    )
}
