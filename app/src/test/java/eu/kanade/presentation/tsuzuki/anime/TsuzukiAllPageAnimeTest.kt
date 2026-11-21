package eu.kanade.presentation.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
}
