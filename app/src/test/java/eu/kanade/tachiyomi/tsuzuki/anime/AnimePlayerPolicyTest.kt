package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimeStreamQueue
import eu.kanade.tachiyomi.tsuzuki.anime.player.adjacentAnimeEpisodes
import eu.kanade.tachiyomi.tsuzuki.anime.player.rankAnimeVideos
import eu.kanade.tachiyomi.tsuzuki.anime.player.repeatAnimeCheckpoints
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tachiyomi.domain.items.episode.model.Episode

class AnimePlayerPolicyTest {
    @Test
    fun highestQualityIsDefaultAndPreferredQualityCapsTheFirstChoice() {
        val sd = Video("sd", "480p")
        val hd = Video("hd", resolution = 720)
        val full = Video("full", resolution = 1080)
        val unknown = Video("unknown")
        val videos = listOf(unknown, hd, sd, full)
        assertEquals(listOf(full, hd, sd, unknown), rankAnimeVideos(videos, 0))
        assertEquals(listOf(hd, sd, full, unknown), rankAnimeVideos(videos, 720))
        assertEquals(listOf(sd, hd, full, unknown), rankAnimeVideos(videos, 360))
    }

    @Test
    fun resolutionFailureAndPlaybackFailureMoveToTheNextCandidate() = runTest {
        val videos = listOf(
            Video("expired", resolution = 1080),
            Video("first", resolution = 720),
            Video("fallback", resolution = 480),
        )
        val queue = AnimeStreamQueue(videos, preferred = 0)
        val attempted = mutableListOf<String>()
        val resolve: suspend (Video) -> Video = {
            attempted += it.videoUrl
            check(it.videoUrl != "expired")
            it
        }
        assertEquals("first", queue.next(resolve)?.videoUrl)
        assertEquals("fallback", queue.next(resolve)?.videoUrl)
        assertNull(queue.next(resolve))
        assertEquals(listOf("expired", "first", "fallback"), attempted)
    }

    @Test
    fun previousAndNextFollowEpisodeNumberEvenWhenSourceOrderIsReversed() {
        val first = Episode(1, "/1", "First", id = 8, episodeNumber = 1.0, sourceOrder = 2)
        val middle = Episode(1, "/2", "Second", id = 9, episodeNumber = 2.0, sourceOrder = 1)
        val last = Episode(1, "/3", "Third", id = 10, episodeNumber = 3.0, sourceOrder = 0)
        val episodes = listOf(last, middle, first)
        assertEquals(first to last, adjacentAnimeEpisodes(episodes, middle.id))
        assertEquals(middle to null, adjacentAnimeEpisodes(episodes, last.id))
        assertEquals(null to null, adjacentAnimeEpisodes(episodes, 99))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun periodicProgressFlushesAtTenSecondIntervals() = runTest {
        var checkpoints = 0
        backgroundScope.launch { repeatAnimeCheckpoints { checkpoints++ } }
        runCurrent()
        advanceTimeBy(9_999)
        assertEquals(0, checkpoints)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, checkpoints)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(2, checkpoints)
    }
}
