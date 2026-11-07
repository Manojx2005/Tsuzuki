package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.presentation.tsuzuki.anime.AnimeLibraryTab
import eu.kanade.presentation.tsuzuki.browse.BrowseMedia
import eu.kanade.presentation.tsuzuki.browse.browsePages
import eu.kanade.presentation.tsuzuki.home.showAnimeWatch
import eu.kanade.tachiyomi.ui.tsuzuki.navigation.isAnimeLibraryKey
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TsuzukiAnimeGateTest {
    @Test
    fun disabledHidesBrowseWatchAndLibraryDestinations() {
        val browse = browsePages(hideManga = false, hasAnime = true, animeEnabled = false)
        assertFalse(browse.any { it.media == BrowseMedia.ANIME })
        assertFalse(showAnimeWatch(watching = true, animeEnabled = false))
        assertFalse(isAnimeLibraryKey(AnimeLibraryTab.key, animeEnabled = false))
    }

    @Test
    fun enabledRetainsAnimeDestinations() {
        val browse = browsePages(hideManga = false, hasAnime = true, animeEnabled = true)
        assertTrue(browse.any { it.media == BrowseMedia.ANIME })
        assertTrue(showAnimeWatch(watching = true, animeEnabled = true))
        assertTrue(isAnimeLibraryKey(AnimeLibraryTab.key, animeEnabled = true))
    }
}
