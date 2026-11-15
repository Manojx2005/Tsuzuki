package eu.kanade.tachiyomi.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.entries.anime.model.Anime

class AnimeSeasonsTest {
    @Test
    fun groupsSeasonsWhenTheirParentIsPresent() {
        val parent = entry(1)
        val season = entry(2, 1)
        val other = entry(3)
        assertEquals(listOf(parent, other), groupAnimeSeasons(listOf(parent, season, other)))
    }

    @Test
    fun keepsOrphansAndChildrenOnShelvesWithoutTheirParents() {
        val parent = entry(1)
        val season = entry(2, 1)
        val orphan = entry(3, 9)
        val crossSource = entry(4, 1).let { it.copy(anime = it.anime.copy(source = 43)) }
        assertEquals(listOf(season, orphan), groupAnimeSeasons(listOf(season, orphan)))
        assertEquals(
            listOf(parent, orphan, crossSource),
            groupAnimeSeasons(listOf(parent, season, orphan, crossSource)),
        )
    }

    @Test
    fun keepsMalformedCyclesAndDeeperChildrenVisible() {
        val entries = listOf(entry(1, 2), entry(2, 1), entry(3, 3), entry(4), entry(5, 4), entry(6, 5))
        assertEquals(listOf(1L, 2L, 3L, 4L, 6L), groupAnimeSeasons(entries).map { it.anime.id })
    }

    @Test
    fun groupingDefaultsOnAndCanBeTurnedOff() {
        val store = InMemoryPreferenceStore()
        val grouping = animeSeasonGrouping(store)
        assertTrue(grouping.get())
        grouping.set(false)
        assertFalse(animeSeasonGrouping(store).get())
    }

    private fun entry(id: Long, parentId: Long? = null) = AnimeLibraryEntry(
        Anime(id = id, source = 42, url = "/$id", title = "Anime $id", parentId = parentId),
        setOf(0L),
    )
}
