package eu.kanade.tachiyomi.tsuzuki.anime.download

import eu.kanade.tachiyomi.util.system.NetworkState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AnimeDownloadNotificationTest {
    private fun entry(id: Long) = AnimeDownloadEntry(1L, 1L, id, "Fixture", "Anime fixture", "Episode $id")

    @Test
    fun progressTracksCurrentEpisodeAcrossAQueue() {
        val first = entry(1)
        val second = entry(2)
        val alreadySaved = entry(3).copy(status = AnimeDownloadStatus.Saved)
        val initial = AnimeDownloadQueue(entries = listOf(alreadySaved, first, second))
        val batch = AnimeDownloadNotificationBatch(initial)

        assertEquals(
            AnimeDownloadNotificationProgress(first, 1, 2, 0.5f),
            batch.progress(initial, first.episodeId to AnimeDownloadProgress(1000, 2000)),
        )
        val afterFirst = initial.change(first.episodeId) { it.copy(status = AnimeDownloadStatus.Saved) }
        assertEquals(
            AnimeDownloadNotificationProgress(second, 2, 2, null),
            batch.progress(afterFirst, first.episodeId to AnimeDownloadProgress(2000, 2000)),
        )
        assertEquals(
            AnimeDownloadNotificationProgress(second, 2, 2, 0.25f),
            batch.progress(afterFirst, second.episodeId to AnimeDownloadProgress(1, 4)),
        )
        val failed = second.copy(status = AnimeDownloadStatus.Failed)
        assertEquals(
            AnimeDownloadNotificationResult(1, listOf(failed)),
            batch.result(afterFirst.change(second.episodeId) { failed }),
        )
    }

    @Test
    fun wifiWaitingAndPauseAreReportedOnlyForPendingEpisodes() {
        val mobile = NetworkState(isConnected = true, isValidated = true, isWifi = false)
        val wifi = mobile.copy(isWifi = true)
        val queue = AnimeDownloadQueue(entries = listOf(entry(1)))

        assertEquals(AnimeDownloadDeferred.WIFI, queue.deferredDownloadNotification(mobile))
        assertEquals(AnimeDownloadDeferred.PAUSED, queue.copy(paused = true).deferredDownloadNotification(mobile))
        assertNull(queue.copy(wifiOnly = false).deferredDownloadNotification(mobile))
        assertNull(queue.deferredDownloadNotification(wifi))
        assertNull(queue.copy(entries = emptyList(), paused = true).deferredDownloadNotification(mobile))
    }
}
