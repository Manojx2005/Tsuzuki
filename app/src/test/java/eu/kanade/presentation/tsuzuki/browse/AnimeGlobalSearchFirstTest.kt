package eu.kanade.presentation.tsuzuki.browse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AnimeGlobalSearchFirstTest {

    @Test
    fun `a request from anime details carries its query once`() {
        AnimeGlobalSearch.openAnimeFirst("One Piece")
        assertEquals("One Piece", AnimeGlobalSearch.consumeAnimeFirst())
        assertNull(AnimeGlobalSearch.consumeAnimeFirst())
    }

    @Test
    fun `the Browse toolbar request opens the Anime pill with no query`() {
        AnimeGlobalSearch.openAnimeFirst()
        assertEquals("", AnimeGlobalSearch.consumeAnimeFirst())
    }

    @Test
    fun `nothing is requested by default`() {
        AnimeGlobalSearch.consumeAnimeFirst()
        assertNull(AnimeGlobalSearch.consumeAnimeFirst())
    }
}
