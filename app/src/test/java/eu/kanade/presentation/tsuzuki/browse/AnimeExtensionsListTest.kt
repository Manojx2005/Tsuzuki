package eu.kanade.presentation.tsuzuki.browse

import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeListedExtension
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeRepoEntry
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeStoreAction
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeStoreRow
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnimeExtensionsListTest {
    private val index = "https://raw.githubusercontent.com/o/r/repo/index.min.json".toHttpUrl()

    private fun row(name: String, lang: String, pkg: String = "eu.kanade.tachiyomi.animeextension.$lang.${cleanAnimeExtensionName(name).lowercase()}") = AnimeStoreRow(
        pkgName = pkg,
        extension = null,
        listed = AnimeListedExtension(index, AnimeRepoEntry(name, pkg, "$pkg.apk", lang, 1, "14.1")),
        action = AnimeStoreAction.INSTALL,
        removable = false,
    )

    @Test
    fun `repo prefix is dropped`() {
        assertEquals("AnimeKai", cleanAnimeExtensionName("Aniyomi: AnimeKai"))
        assertEquals("Anime1.me", cleanAnimeExtensionName("aniyomi:Anime1.me"))
        assertEquals("Hanime", cleanAnimeExtensionName("Hanime"))
        assertEquals("Aniyomi:", cleanAnimeExtensionName("Aniyomi:"))
    }

    @Test
    fun `languages put all and multi first`() {
        val rows = listOf(row("A", "fr"), row("B", "all"), row("C", "en"), row("D", "en"))
        assertEquals(listOf("all", "en", "fr"), animeExtensionLanguages(rows))
    }

    @Test
    fun `filter by language and search ignores the prefix`() {
        val rows = listOf(row("Aniyomi: Zoro", "en"), row("Aniyomi: AnimeKai", "en"), row("Aniyomi: Anime-Sama", "fr"))
        assertEquals(listOf("AnimeKai", "Zoro"), filterAnimeStoreRows(rows, "en", null).map { cleanAnimeExtensionName(it.name) })
        assertEquals(listOf("Anime-Sama", "AnimeKai"), filterAnimeStoreRows(rows, null, "anime").map { cleanAnimeExtensionName(it.name) })
        assertEquals(emptyList<String>(), filterAnimeStoreRows(rows, null, "aniyomi").map { it.name })
    }
}
