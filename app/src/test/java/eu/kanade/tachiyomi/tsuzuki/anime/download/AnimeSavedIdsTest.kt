package eu.kanade.tachiyomi.tsuzuki.anime.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** "Downloaded only" keeps anime with at least one finished episode download (AY-INT item 5). */
class AnimeSavedIdsTest {

    private fun entry(animeId: Long, episodeId: Long, status: AnimeDownloadStatus) =
        AnimeDownloadEntry(1, animeId, episodeId, "Source", "Anime $animeId", "Episode", status)

    @Test
    fun `only anime with a saved episode count as downloaded`() {
        val queue = AnimeDownloadQueue(
            listOf(
                entry(10, 1, AnimeDownloadStatus.Saved),
                entry(10, 2, AnimeDownloadStatus.Queued),
                entry(20, 3, AnimeDownloadStatus.Queued),
                entry(30, 4, AnimeDownloadStatus.Failed),
                entry(40, 5, AnimeDownloadStatus.Removing),
                entry(50, 6, AnimeDownloadStatus.Saved),
            ),
        )
        assertEquals(setOf(10L, 50L), queue.savedAnimeIds())
    }

    @Test
    fun `an empty queue has no downloaded anime`() {
        assertEquals(emptySet<Long>(), AnimeDownloadQueue().savedAnimeIds())
    }
}
