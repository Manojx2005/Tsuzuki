package eu.kanade.tachiyomi.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entries.anime.model.Anime

class AnimeLibraryOptionsTest {
    private fun anime(id: Long, title: String, total: Long, seen: Long, added: Long = id) =
        Anime(source = 1, url = "/$id", title = title, id = id, totalCount = total, seenCount = seen, dateAdded = added)

    private val fresh = anime(1, "beta", total = 3, seen = 0)
    private val started = anime(2, "Alpha", total = 4, seen = 2)
    private val done = anime(3, "gamma", total = 2, seen = 2)
    private val empty = anime(4, "delta", total = 0, seen = 0)
    private val all = listOf(fresh, started, done, empty)

    private fun ids(options: AnimeLibraryOptions, saved: Set<Long> = emptySet()) =
        applyAnimeLibraryOptions(all, options, saved).map { it.id }

    @Test
    fun sortsByTitleIgnoringCaseAndCanReverse() {
        assertEquals(listOf(2L, 1L, 4L, 3L), ids(AnimeLibraryOptions()))
        assertEquals(listOf(3L, 4L, 1L, 2L), ids(AnimeLibraryOptions(descending = true)))
    }

    @Test
    fun sortsByUnwatchedThenTitle() {
        // Unwatched: fresh 3, started 2, done 0, empty 0 (ties fall back to title).
        assertEquals(listOf(4L, 3L, 2L, 1L), ids(AnimeLibraryOptions(sort = AnimeLibrarySort.UNWATCHED)))
    }

    @Test
    fun triStateFilters() {
        assertEquals(listOf(2L, 1L), ids(AnimeLibraryOptions(unwatched = TriState.ENABLED_IS)))
        assertEquals(listOf(2L), ids(AnimeLibraryOptions(started = TriState.ENABLED_IS)))
        assertEquals(listOf(3L), ids(AnimeLibraryOptions(completed = TriState.ENABLED_IS)))
        assertEquals(listOf(2L, 1L, 4L), ids(AnimeLibraryOptions(completed = TriState.ENABLED_NOT)))
        assertEquals(listOf(3L), ids(AnimeLibraryOptions(downloaded = TriState.ENABLED_IS), saved = setOf(3L)))
        val tracked =
            applyAnimeLibraryOptions(all, AnimeLibraryOptions(tracked = TriState.ENABLED_NOT), emptySet(), setOf(1L))
        assertEquals(listOf(2L, 4L, 3L), tracked.map { it.id })
    }

    @Test
    fun hasFiltersOnlyWhenOneIsSet() {
        assertFalse(AnimeLibraryOptions(sort = AnimeLibrarySort.DATE_ADDED).hasFilters)
        assertTrue(AnimeLibraryOptions(started = TriState.ENABLED_NOT).hasFilters)
    }
}
