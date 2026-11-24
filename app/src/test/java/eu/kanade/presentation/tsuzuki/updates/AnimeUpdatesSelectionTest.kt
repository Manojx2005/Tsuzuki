package eu.kanade.presentation.tsuzuki.updates

import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimeUpdatesSelectionTest {
    private fun episode(id: Long, seen: Boolean = false, bookmark: Boolean = false, position: Long = 0) =
        AnimeUpdateEpisode(
            animeId = 1,
            title = "Anime",
            episodeId = id,
            name = "Episode $id",
            previewUrl = null,
            timestamp = id,
            seen = seen,
            bookmark = bookmark,
            lastSecondSeen = position,
        )

    @Test
    fun actionsFollowTheBookUpdatesRules() {
        val unseen = episode(1)
        val started = episode(2, position = 30)
        val watched = episode(3, seen = true, bookmark = true)

        val none = animeUpdateSelectionActions(emptyList(), emptyMap())
        assertFalse(none.removeBookmark)
        assertFalse(none.download)

        val fresh = animeUpdateSelectionActions(listOf(unseen), emptyMap())
        assertTrue(fresh.markSeen)
        assertFalse(fresh.markUnseen)
        assertTrue(fresh.bookmark)
        assertFalse(fresh.removeBookmark)
        assertTrue(fresh.download)
        assertFalse(fresh.delete)

        // A started episode can be reset to unwatched, like a partly read chapter.
        assertTrue(animeUpdateSelectionActions(listOf(started), emptyMap()).markUnseen)

        val bookmarked = animeUpdateSelectionActions(listOf(watched), mapOf(3L to AnimeDownloadStatus.Saved))
        assertFalse(bookmarked.markSeen)
        assertTrue(bookmarked.removeBookmark)
        assertFalse(bookmarked.bookmark)
        assertFalse(bookmarked.download)
        assertTrue(bookmarked.delete)
    }

    @Test
    fun queuedEpisodesAreNotDownloadedAgainButFailedOnesRetry() {
        val queued = animeUpdateSelectionActions(listOf(episode(1)), mapOf(1L to AnimeDownloadStatus.Queued))
        assertFalse(queued.download)
        assertFalse(queued.delete)
        val failed = animeUpdateSelectionActions(listOf(episode(1)), mapOf(1L to AnimeDownloadStatus.Failed))
        assertTrue(failed.download)
    }

    @Test
    fun selectAllAndInvert() {
        val entries = listOf(episode(1), episode(2), episode(3))
        assertEquals(setOf(1L, 2L, 3L), animeUpdatesSelectAll(entries))
        assertEquals(setOf(1L, 3L), animeUpdatesInvert(entries, setOf(2L)))
        assertEquals(emptySet<Long>(), animeUpdatesInvert(entries, setOf(1L, 2L, 3L)))
    }
}
