package eu.kanade.tachiyomi.tsuzuki.anime.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.items.episode.model.Episode

class AnimeEpisodeSelectionTest {
    private val visible = listOf(65L, 4L, 39L, 11L, 83L, 21L).map { id ->
        Episode(animeId = 1, url = "/episode/$id", name = "Episode $id", id = id)
    }

    @Test
    fun longPressExtendsToNearestVisibleBoundaryOnEitherSide() {
        val selected = longArrayOf(39, 83)
        assertEquals(setOf(65L, 4L, 39L, 83L), toggle(selected, 65, fromLongPress = true))
        assertEquals(setOf(39L, 83L, 21L), toggle(selected, 21, fromLongPress = true))
        assertEquals(setOf(39L, 11L, 83L), toggle(selected, 11, fromLongPress = true))
    }

    @Test
    fun hiddenSelectionsDoNotAnchorRangesAndMissingRowsDoNotChangeSelection() {
        val selected = longArrayOf(100)
        assertEquals(setOf(100L, 83L), toggle(selected, 83, fromLongPress = true))
        assertEquals(setOf(100L), toggle(selected, 101, fromLongPress = true))
    }

    @Test
    fun tapAndLongPressOnSelectedRowsChangeOnlyOneRow() {
        val selected = longArrayOf(39, 83)
        assertEquals(setOf(39L, 83L, 65L), toggle(selected, 65, fromLongPress = false))
        assertEquals(setOf(83L), toggle(selected, 39, fromLongPress = true))
    }

    private fun toggle(selection: LongArray, id: Long, fromLongPress: Boolean) =
        toggleAnimeEpisodeSelection(visible, selection, id, fromLongPress).toSet()
}
