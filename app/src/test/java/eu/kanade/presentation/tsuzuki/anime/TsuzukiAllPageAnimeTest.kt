package eu.kanade.presentation.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.anime.model.Anime

class TsuzukiAllPageAnimeTest {
    private fun anime(id: Long, title: String) = Anime(source = 1, url = "/$id", title = title, id = id)

    @Test
    fun `all page lists every library anime alphabetically, once`() {
        val library = listOf(anime(3, "one piece"), anime(1, "Bleach"), anime(2, "Naruto"), anime(1, "Bleach"))
        assertEquals(
            listOf(1L, 2L, 3L),
            allPageAnime(library, downloadedOnly = false, savedAnimeIds = emptySet()).map {
                it.id
            },
        )
    }

    @Test
    fun `downloaded only keeps anime with a saved episode`() {
        val library = listOf(anime(1, "Bleach"), anime(2, "Naruto"))
        assertEquals(listOf(2L), allPageAnime(library, downloadedOnly = true, savedAnimeIds = setOf(2L)).map { it.id })
        assertEquals(emptyList<Anime>(), allPageAnime(library, downloadedOnly = true, savedAnimeIds = emptySet()))
    }

    @Test
    fun `library search matches anime titles, ignoring case`() {
        val library = listOf(anime(1, "Bleach"), anime(2, "Naruto Shippuden"), anime(3, "One Piece"))
        assertEquals(listOf(2L), allPageAnime(library, false, emptySet(), searchQuery = " naruto ").map { it.id })
        assertEquals(emptyList<Anime>(), allPageAnime(library, false, emptySet(), searchQuery = "Frieren"))
        assertEquals(listOf(1L, 2L, 3L), allPageAnime(library, false, emptySet(), searchQuery = "").map { it.id })
    }

    @Test
    fun `an anime-only library keeps one shelf page for the anime section`() {
        val default = Category(id = 0, name = "Default", order = 0, flags = 0)
        val reading = Category(id = 5, name = "Reading", order = 1, flags = 0)
        // Books elsewhere: the shelves are used as they are, empty first shelf included.
        assertEquals(listOf(reading), allPageShelves(listOf(reading), listOf(default, reading), hasAnime = true))
        // No books and no visible shelf: the default shelf comes back to hold the anime.
        assertEquals(listOf(default), allPageShelves(emptyList(), listOf(default, reading), hasAnime = true))
        assertEquals(0L, allPageShelves(emptyList(), emptyList(), hasAnime = true).single().id)
        // Anime off or none in the library: unchanged, so the empty-library screen still shows.
        assertEquals(emptyList<Category>(), allPageShelves(emptyList(), listOf(default), hasAnime = false))
    }
}
