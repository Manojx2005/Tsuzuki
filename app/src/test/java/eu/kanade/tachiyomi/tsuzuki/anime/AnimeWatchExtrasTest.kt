package eu.kanade.tachiyomi.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.items.episode.model.Episode

class AnimeWatchExtrasTest {
    private fun ep(id: Long, number: Double, seen: Boolean = false) =
        Episode(animeId = 1, url = "/$id", name = "Ep $number", id = id, seen = seen, episodeNumber = number)

    private val ordered = (1L..8L).map { ep(it, it.toDouble(), seen = it == 4L) }

    @Test
    fun downloadsAheadOnlyWhenCurrentAndNextAreSaved() {
        val result = animeEpisodesToDownloadAhead(ordered, currentId = 2, saved = setOf(2, 3), inQueue = setOf(2, 3, 6), amount = 3)
        // After the next episode (3): 4 is seen, 6 is already queued → 5, 7, 8.
        assertEquals(listOf(5L, 7L, 8L), result.map { it.id })
        assertEquals(emptyList<Long>(), animeEpisodesToDownloadAhead(ordered, 2, setOf(2), emptySet(), 3).map { it.id })
        assertEquals(emptyList<Long>(), animeEpisodesToDownloadAhead(ordered, 2, setOf(2, 3), emptySet(), 0).map { it.id })
        assertEquals(emptyList<Long>(), animeEpisodesToDownloadAhead(ordered, 8, setOf(8), emptySet(), 3).map { it.id })
    }

    @Test
    fun duplicatesShareTheRecognisedNumber() {
        val episodes = listOf(ep(1, 5.0), ep(2, 5.0), ep(3, 5.0, seen = true), ep(4, 6.0), ep(5, -1.0), ep(6, -1.0))
        assertEquals(setOf(2L), animeDuplicateEpisodes(episodes, episodes[0]))
        assertEquals(emptySet<Long>(), animeDuplicateEpisodes(episodes, episodes[4]))
    }
}
