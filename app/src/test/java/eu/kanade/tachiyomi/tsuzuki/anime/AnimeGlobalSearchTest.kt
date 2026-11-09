package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.presentation.tsuzuki.browse.AnimeGlobalSearch
import eu.kanade.presentation.tsuzuki.browse.selectAnimeSearchSources
import eu.kanade.presentation.tsuzuki.browse.showAnimeGlobalSearch
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class AnimeGlobalSearchTest {
    @Test
    fun animePillRequiresTheFeatureGateAndAnInstalledSource() {
        assertFalse(showAnimeGlobalSearch(false, 3))
        assertFalse(showAnimeGlobalSearch(true, 0))
        assertTrue(showAnimeGlobalSearch(true, 1))
    }

    @Test
    fun searchSelectsPinnedInstalledSourcesAndSortsDeterministically() {
        val sources = listOf(
            entry(1, "Zeta"),
            entry(2, "Beta"),
            entry(3, "Alpha"),
            entry(4, "Gamma"),
        )
        val pins = setOf("1", "2", "9")
        assertEquals(listOf(2L, 1L), selectAnimeSearchSources(sources, pins, pinnedOnly = true).map { it.source.id })
        assertEquals(
            listOf(2L, 1L, 3L, 4L),
            selectAnimeSearchSources(sources, pins, pinnedOnly = false).map { it.source.id },
        )
        assertTrue(selectAnimeSearchSources(sources, emptySet(), pinnedOnly = true).isEmpty())
    }

    @Test
    fun animeSearchLimitsConcurrencyAndKeepsOtherSourcesWhenOneFails() = runBlocking {
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val atLimit = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val title = SAnime.create().apply {
            url = "/result"
            this.title = "Search result"
        }
        val sources = (1L..7L).map { id ->
            entry(id, "Source $id").also { registered ->
                every { registered.source.getFilterList() } returns AnimeFilterList()
                coEvery { registered.source.getSearchAnime(1, "query", any()) } coAnswers {
                    val count = active.incrementAndGet()
                    peak.updateAndGet { maxOf(it, count) }
                    if (count == 5) atLimit.complete(Unit)
                    try {
                        release.await()
                        if (id == 2L) error("Fixture source failed")
                        AnimesPage(listOf(title, title), false)
                    } finally {
                        active.decrementAndGet()
                    }
                }
            }
        }
        val model = AnimeGlobalSearch()
        val request = async { model.search(sources, "query") }
        try {
            withTimeout(10_000) { atLimit.await() }
            assertEquals(5, peak.get())
        } finally {
            release.complete(Unit)
        }
        request.await()
        assertEquals(5, peak.get())
        assertTrue(model.results.single { it.entry.source.id == 2L }.failed)
        assertTrue(model.results.filterNot { it.failed }.all { it.titles == listOf(title) })
    }

    private fun entry(id: Long, name: String): RegisteredAnimeSource {
        val source = mockk<AnimeSource>()
        every { source.id } returns id
        every { source.name } returns name
        return RegisteredAnimeSource(source, "example.anime", "Fixture", 17.0)
    }
}
