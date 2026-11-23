package eu.kanade.presentation.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.items.episode.model.Episode

class AnimeLibrarySelectionTest {
    @Test
    fun rangeAndInvertFollowTheVisibleShelfOrder() {
        val visible = listOf(105L, 3L, 88L, 12L, 9L)
        val selection = AnimeLibrarySelection()

        selection.range(12L, visible)
        selection.range(3L, visible)
        assertEquals(setOf(3L, 88L, 12L), selection.ids)

        selection.invert(visible)
        assertEquals(setOf(105L, 9L), selection.ids)
        selection.selectAll(visible)
        assertEquals(visible.toSet(), selection.ids)
        selection.clear()
        assertEquals(emptySet<Long>(), selection.ids)
    }

    @Test
    fun downloadNextSkipsWatchedAndExistingDownloadsBeforeTakingLimit() {
        val episodes = listOf(
            episode(4),
            episode(3),
            episode(2),
            episode(1).copy(seen = true),
        )
        assertEquals(listOf(3L), episodesToDownload(episodes, AnimeLibraryDownload.NextOne, setOf(2)).map(Episode::id))
        assertEquals(
            listOf(2L, 4L),
            episodesToDownload(episodes, AnimeLibraryDownload.Unwatched, setOf(3)).map(Episode::id),
        )
        assertEquals(
            listOf(1L, 2L, 4L),
            episodesToDownload(episodes, AnimeLibraryDownload.All, setOf(3)).map(Episode::id),
        )
    }

    private fun episode(number: Long) =
        Episode(
            animeId = 1,
            url = number.toString(),
            name = number.toString(),
            id = number,
            episodeNumber = number.toDouble(),
        )
}
