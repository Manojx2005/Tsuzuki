package eu.kanade.presentation.tsuzuki.anime

import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WatchActivityTilesTest {
    private fun episode(anime: Long, id: Long) = AnimeUpdateEpisode(anime, "Anime $anime", id, "Ep $id", null, id)

    @Test
    fun newEpisodesCountShowsOnce() {
        assertEquals(0 to 0, newEpisodeSummary(emptyList()))
        assertEquals(
            5 to 3,
            newEpisodeSummary(listOf(episode(1, 1), episode(1, 2), episode(2, 3), episode(3, 4), episode(3, 5))),
        )
    }
}
