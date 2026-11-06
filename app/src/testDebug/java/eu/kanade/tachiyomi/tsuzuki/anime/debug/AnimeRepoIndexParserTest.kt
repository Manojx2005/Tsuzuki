package eu.kanade.tachiyomi.tsuzuki.anime.debug

import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtension
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AnimeRepoIndexParserTest {
    private val fixture = checkNotNull(javaClass.getResourceAsStream("/anime-repo-index.json"))
        .bufferedReader().use { it.readText() }

    @Test
    fun parsesIndexAndIgnoresExtraFields() {
        val entry = AnimeRepoIndexParser.parse(fixture).single()

        assertEquals("Fixture source", entry.name)
        assertEquals("test.fixture.anime", entry.packageName)
        assertEquals("fixture-v14.2.apk", entry.apk)
        assertEquals("en", entry.lang)
        assertEquals(2L, entry.versionCode)
        assertEquals("14.2", entry.versionName)
    }

    @Test
    fun rejectsEscapingApkPath() {
        assertThrows<IllegalArgumentException> {
            AnimeRepoIndexParser.parse(fixture.replace("fixture-v14.2.apk", "../fixture-v14.2.apk"))
        }
    }

    @Test
    fun mapsGithubRepoAndYuzonoAlias() {
        assertEquals(
            "https://raw.githubusercontent.com/Secozzi/aniyomi-extensions/repo/index.min.json",
            AnimeRepoIndexParser.indexUrl("github.com/Secozzi/aniyomi-extensions/").toString(),
        )
        assertEquals(
            "https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json",
            AnimeRepoIndexParser.indexUrl("https://github.com/yuzono/anime-extensions").toString(),
        )
    }

    @Test
    fun acceptsHttpsIndexAndRejectsUnsupportedUrls() {
        val url = "https://example.org/anime/index.min.json"
        assertEquals(url, AnimeRepoIndexParser.indexUrl(url).toString())
        assertNull(AnimeRepoIndexParser.indexUrl("http://example.org/index.min.json"))
        assertNull(AnimeRepoIndexParser.indexUrl("https://secret@example.org/index.min.json"))
        assertNull(AnimeRepoIndexParser.indexUrl("https://github.com/org/repo/tree/repo"))
    }

    @Test
    fun mapsLoaderResultsToExplicitActions() {
        val untrusted = AnimeExtension.Untrusted("Fixture", "test.fixture.anime", "14.2", 2, 14.0, "hash")
        val installed = AnimeExtension.Installed(
            name = untrusted.name,
            pkgName = untrusted.pkgName,
            versionName = untrusted.versionName,
            versionCode = untrusted.versionCode,
            libVersion = untrusted.libVersion,
            lang = "en",
            isNsfw = false,
            isTorrent = false,
            pkgFactory = null,
            sources = emptyList(),
            icon = null,
            isShared = false,
        )

        assertEquals(AnimeDebugInstallAction.Install, animeDebugInstallAction(null))
        assertEquals(AnimeDebugInstallAction.Retry, animeDebugInstallAction(AnimeLoadResult.Error))
        assertEquals(AnimeDebugInstallAction.Trust, animeDebugInstallAction(AnimeLoadResult.Untrusted(untrusted)))
        assertEquals(AnimeDebugInstallAction.Installed, animeDebugInstallAction(AnimeLoadResult.Success(installed)))
    }
}
