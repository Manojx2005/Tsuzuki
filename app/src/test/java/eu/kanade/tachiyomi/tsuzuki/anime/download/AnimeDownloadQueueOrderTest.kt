package eu.kanade.tachiyomi.tsuzuki.anime.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnimeDownloadQueueOrderTest {
    private fun entry(id: Long, status: AnimeDownloadStatus = AnimeDownloadStatus.Queued) =
        AnimeDownloadEntry(1L, 1L, id, "Fixture", "Anime", "Episode $id", status = status)

    private fun AnimeDownloadQueue.ids() = entries.map { it.episodeId }

    @Test
    fun moveToTopAndBottomOnlyReordersQueuedEpisodes() {
        val queue = AnimeDownloadQueue(
            entries = listOf(entry(1, AnimeDownloadStatus.Saved), entry(2), entry(3), entry(4)),
        )
        assertEquals(listOf(1L, 4L, 2L, 3L), queue.move(4, top = true).ids())
        assertEquals(listOf(1L, 3L, 4L, 2L), queue.move(2, top = false).ids())
        // A saved episode is not part of the queue order.
        assertEquals(queue, queue.move(1, top = true))
    }

    @Test
    fun retryFailedRequeuesOnlyFailedEpisodes() {
        val queue = AnimeDownloadQueue(
            entries = listOf(entry(1, AnimeDownloadStatus.Failed), entry(2, AnimeDownloadStatus.Saved)),
        )
        assertEquals(
            listOf(AnimeDownloadStatus.Queued, AnimeDownloadStatus.Saved),
            queue.retryFailed().entries.map { it.status },
        )
    }

    @Test
    fun onlyEpisodesThatTurnWatchedAreDeleted() {
        // An anime's first look is its baseline: already-watched episodes are never deleted.
        assertEquals(emptySet<Long>(), newlyWatchedDownloads(emptyMap(), mapOf(7L to setOf(1L, 2L))))
        assertEquals(setOf(3L), newlyWatchedDownloads(mapOf(7L to setOf(1L, 2L)), mapOf(7L to setOf(1L, 2L, 3L))))
        // Unmarking is not a deletion, and a second anime joining starts with its own baseline.
        assertEquals(
            emptySet<Long>(),
            newlyWatchedDownloads(mapOf(7L to setOf(1L, 2L)), mapOf(7L to setOf(1L), 8L to setOf(9L))),
        )
    }
}
