package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.presentation.tsuzuki.browse.isAnimeRepoLink
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeRepoIndexParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimeRepoLinkTest {
    @Test
    fun animeLinksKeepBookRepositoriesOnTheirOwnRoute() {
        assertTrue(isAnimeRepoLink("aniyomi", "add-repo"))
        assertTrue(isAnimeRepoLink("animetail", "add-repo"))
        assertTrue(isAnimeRepoLink("tachiyomi", "add-anime-repo"))
        assertTrue(isAnimeRepoLink("tsuzuki", "add-anime-repo"))
        assertFalse(isAnimeRepoLink("tachiyomi", "add-repo"))
        assertFalse(isAnimeRepoLink("aniyomi", "extension-store"))
        assertFalse(isAnimeRepoLink("https", "add-anime-repo"))
    }

    @Test
    fun normalizesYuzonoShorthandFoldersAndGithubPages() {
        val index = "https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json"
        listOf(
            "https://github.com/yuzono/anime-extensions",
            "https://github.com/yuzono/anime-repo/tree/repo",
            "https://github.com/yuzono/anime-repo/blob/repo/index.min.json",
            "https://github.com/yuzono/anime-repo/raw/repo/index.min.json",
            "https://raw.githubusercontent.com/yuzono/anime-repo/repo",
            "https://raw.githubusercontent.com/yuzono/anime-repo/repo/",
        ).forEach { input -> assertEquals(index, AnimeRepoIndexParser.indexUrl(input).toString(), input) }
    }

    @Test
    fun preservesExplicitIndexNamesAndRefPrefixes() {
        val secozzi = "https://raw.githubusercontent.com/Secozzi/aniyomi-extensions/refs/heads/repo"
        assertEquals("$secozzi/index.min.json", AnimeRepoIndexParser.indexUrl(secozzi).toString())
        assertEquals("$secozzi/index.json", AnimeRepoIndexParser.indexUrl("$secozzi/index.json").toString())
        val kohiDen = "https://kohiden.xyz/Kohi-den/extensions/raw/branch/main"
        assertEquals("$kohiDen/index.min.json", AnimeRepoIndexParser.indexUrl("$kohiDen/").toString())
        assertEquals("$kohiDen/index.min.json", AnimeRepoIndexParser.indexUrl("$kohiDen/index.min.json").toString())
        assertEquals(
            "https://repo.example/aniyomi/repo/index.min.json",
            AnimeRepoIndexParser.indexUrl("https://repo.example/aniyomi/repo").toString(),
        )
    }

    @Test
    fun rejectsInsecureAndUnsupportedLinks() {
        listOf(
            "http://raw.githubusercontent.com/yuzono/anime-repo/repo",
            "file:///data/index.min.json",
            "https://user:secret@example.org/repo",
            "https://example.org/",
            "https://example.org/repo/index.pb",
            "https://example.org/index.min.json#anchor",
            "https://github.com/yuzono/anime-repo/blob/repo/README.md",
        ).forEach { input -> assertNull(AnimeRepoIndexParser.indexUrl(input), input) }
    }

    @Test
    fun parsesCompactAndExpandedJsonIndexes() {
        val compact = """
            [{"name":"Fixture","pkg":"example.anime.fixture","apk":"aniyomi-en.fixture.apk",
              "lang":"en","code":13,"version":"16.13"}]
        """.trimIndent()
        val compactEntry = AnimeRepoIndexParser.parse(compact).single()
        val expanded = """
            {"extensionList":{"extensions":[
              {"name":"Fixture","packageName":"example.anime.fixture","versionCode":"13","versionName":"16.13",
               "resources":{"apkUrl":"https://cdn.example/apk/aniyomi-en.fixture.apk"},"sources":[{"language":"en"}]}
            ]}}
        """.trimIndent()
        val expandedEntry = AnimeRepoIndexParser.parse(expanded).single()
        assertEquals(compactEntry, expandedEntry.copy(downloadUrl = null))
        assertEquals("https://cdn.example/apk/aniyomi-en.fixture.apk", expandedEntry.downloadUrl)
        assertThrows(IllegalArgumentException::class.java) {
            AnimeRepoIndexParser.parse(expanded.replace("https:", "http:"))
        }
    }
}
