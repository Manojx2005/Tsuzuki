package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtension
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository

class AnimeSourceManagerTest {
    @Test
    fun `refresh registers a trusted source and retains its stub after uninstall`() = runTest {
        val source = mockk<AnimeSource> {
            every { id } returns 42L
            every { name } returns "Fixture source"
            every { lang } returns "ja"
        }
        val repository = MemoryAnimeSources()
        var extensions = listOf(AnimeLoadResult.Error, extension(source))
        val manager = AnimeSourceManager(repository) { extensions }
        val metadata = StubAnimeSource(42L, "ja", "Fixture source")

        manager.refresh().map { it.source } shouldBe listOf(source)
        manager.get(42L)?.source shouldBe source
        manager.stub(42L) shouldBe metadata

        extensions = emptyList()
        manager.refresh() shouldBe emptyList()
        manager.get(42L) shouldBe null
        manager.stub(42L) shouldBe metadata
    }

    @Test
    fun `ensureLoaded loads extensions once across episode opens and again only after a change`() = runTest {
        val source = mockk<AnimeSource> {
            every { id } returns 7L
            every { name } returns "Counting source"
            every { lang } returns "ja"
        }
        var loads = 0
        val manager = AnimeSourceManager(MemoryAnimeSources()) {
            loads++
            listOf(extension(source))
        }

        manager.ensureLoaded()
        manager.ensureLoaded()
        manager.get(7L)?.source shouldBe source
        loads shouldBe 1

        AnimeSourceManager.extensionsChanged()
        manager.ensureLoaded()
        manager.ensureLoaded()
        loads shouldBe 2

        manager.refresh()
        loads shouldBe 3
    }

    @Test
    fun `built-in sources are merged, installed ids win, and nothing is added when there are none`() = runTest {
        fun source(id: Long, name: String) = mockk<AnimeSource> {
            every { this@mockk.id } returns id
            every { this@mockk.name } returns name
            every { lang } returns "all"
        }
        val installed = source(1L, "Installed")
        val clash = source(1L, "Built-in with an installed id")
        val fixture = source(2L, "Fixture")
        val builtIn = listOf(clash, fixture).map { RegisteredAnimeSource(it, "tsuzuki.fixture", "Fixture", 16.0) }
        val manager = AnimeSourceManager(MemoryAnimeSources(), { builtIn }) { listOf(extension(installed)) }

        manager.refresh().map { it.source } shouldBe listOf(installed, fixture)
        manager.get(2L)?.extensionPackage shouldBe "tsuzuki.fixture"
        AnimeSourceManager(MemoryAnimeSources()) { listOf(extension(installed)) }.refresh().map { it.source } shouldBe
            listOf(installed)
    }

    @Test
    fun `fixture sources stay out of non-debug builds and a missing class is harmless`() {
        debugBuiltInAnimeSources(debug = false) shouldBe emptyList()
        debugBuiltInAnimeSources(debug = true, className = "eu.kanade.tachiyomi.tsuzuki.NoSuchFixture") shouldBe
            emptyList()
    }

    @Test
    fun `load failures are described`() {
        animeLoadFailureMessage(listOf(AnimeLoadResult.Error, AnimeLoadResult.Error))
            ?.contains("2 anime extension(s)") shouldBe true
        animeLoadFailureMessage(emptyList()) shouldBe null
    }

    private fun extension(source: AnimeSource): AnimeLoadResult = AnimeLoadResult.Success(
        AnimeExtension.Installed(
            name = "Fixture extension",
            pkgName = "example.anime",
            versionName = "1.0",
            versionCode = 1,
            libVersion = 17.0,
            lang = "ja",
            isNsfw = false,
            isTorrent = false,
            pkgFactory = null,
            sources = listOf(source),
            icon = null,
            isShared = false,
        ),
    )

    private class MemoryAnimeSources : AnimeSourceRepository {
        private val stubs = mutableMapOf<Long, StubAnimeSource>()

        override suspend fun awaitById(id: Long): StubAnimeSource? = stubs[id]

        override suspend fun awaitAll(): List<StubAnimeSource> = stubs.values.toList()

        override suspend fun upsert(source: StubAnimeSource) {
            stubs[source.id] = source
        }
    }
}
