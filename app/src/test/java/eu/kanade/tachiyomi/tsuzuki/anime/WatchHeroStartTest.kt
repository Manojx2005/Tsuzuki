package eu.kanade.tachiyomi.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tachiyomi.domain.items.episode.model.Episode

class WatchHeroStartTest {
    private fun episode(id: Long, number: Double, seen: Boolean = false, group: String? = null) =
        Episode(1, "/$id", "Ep $id", id = id, episodeNumber = number, seen = seen, scanlator = group)

    @Test
    fun `starts at the first unwatched episode in playback order`() {
        val episodes = listOf(episode(3, 3.0), episode(1, 1.0, seen = true), episode(2, 2.0))
        assertEquals(2L, firstUnwatchedEpisode(episodes, emptySet())?.id)
    }

    @Test
    fun `skips excluded subtitle groups`() {
        val episodes = listOf(episode(1, 1.0, group = "Bad"), episode(2, 2.0, group = "Good"))
        assertEquals(2L, firstUnwatchedEpisode(episodes, setOf("Bad"))?.id)
    }

    @Test
    fun `nothing to start when everything is watched`() {
        assertNull(firstUnwatchedEpisode(listOf(episode(1, 1.0, seen = true)), emptySet()))
    }
}
