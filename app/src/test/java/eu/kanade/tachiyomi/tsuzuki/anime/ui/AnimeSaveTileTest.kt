package eu.kanade.tachiyomi.tsuzuki.anime.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnimeSaveTileTest {
    @Test
    fun unsavedAnimeHasNoSaveTile() {
        assertEquals(AnimeSaveTile.Hidden, animeSaveTile(saved = false, saveableEpisodes = 0))
        assertEquals(AnimeSaveTile.Hidden, animeSaveTile(saved = false, saveableEpisodes = 3))
    }

    @Test
    fun savedAnimeKeepsTheTileDisabledUntilEpisodesCanBeSaved() {
        // First frame: episodes not loaded yet, the tile is already in the row.
        assertEquals(AnimeSaveTile.Disabled, animeSaveTile(saved = true, saveableEpisodes = 0))
        assertEquals(AnimeSaveTile.Enabled, animeSaveTile(saved = true, saveableEpisodes = 1))
    }
}
