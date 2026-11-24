package eu.kanade.presentation.tsuzuki.updates

import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AnimeUpdatesPreviewTest {
    private fun episode(animeId: Long, id: Long) = AnimeUpdateEpisode(
        animeId = animeId,
        title = "Anime $animeId",
        episodeId = id,
        name = "Episode $id",
        previewUrl = null,
        timestamp = id,
    )

    @Test
    fun otherEpisodesAreTheSameAnimeInListOrder() {
        val selected = episode(1, 2)
        val entries = listOf(episode(1, 3), episode(2, 4), selected, episode(1, 1))
        assertEquals(listOf(3L, 1L), otherNewEpisodes(entries, selected).map { it.episodeId })
    }

    @Test
    fun previewFallsBackToTheFirstUpdate() {
        val entries = listOf(episode(1, 5), episode(1, 6))
        assertEquals(6L, resolveUpdatesSelection(entries, 6L) { it.episodeId }?.episodeId)
        // A previewed episode that left the list (marked watched elsewhere, removed) falls back to the top one.
        assertEquals(5L, resolveUpdatesSelection(entries, 99L) { it.episodeId }?.episodeId)
        assertNull(resolveUpdatesSelection(emptyList<AnimeUpdateEpisode>(), 6L) { it.episodeId })
    }
}
