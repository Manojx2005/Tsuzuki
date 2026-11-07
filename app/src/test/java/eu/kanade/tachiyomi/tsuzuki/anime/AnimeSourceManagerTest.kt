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
