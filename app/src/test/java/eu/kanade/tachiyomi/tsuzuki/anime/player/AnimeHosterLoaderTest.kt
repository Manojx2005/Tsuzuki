package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.classOverridesHosterMethods
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimeHosterLoaderTest {
    open class LibraryBase

    @Suppress("unused")
    class WithHosterList : LibraryBase() {
        fun getHosterList(): List<Hoster> = emptyList()
    }

    @Suppress("unused")
    class WithParseOnly : LibraryBase() {
        fun hosterListParse(): List<Hoster> = emptyList()
    }

    @Suppress("unused")
    class EpisodeVideosOnly : LibraryBase() {
        fun getVideoList(): List<Video> = emptyList()
    }

    @Test
    fun `hoster methods are detected only when the extension overrides one`() {
        assertTrue(classOverridesHosterMethods(WithHosterList::class.java, LibraryBase::class.java))
        assertTrue(classOverridesHosterMethods(WithParseOnly::class.java, LibraryBase::class.java))
        assertFalse(classOverridesHosterMethods(EpisodeVideosOnly::class.java, LibraryBase::class.java))
    }

    private fun video(name: String, preferred: Boolean = false, resolution: Int? = null) =
        Video(videoUrl = "https://h/$name", videoTitle = name, resolution = resolution, preferred = preferred)

    @Test
    fun `a hoster that throws becomes an error and the others still load`() = runTest {
        val good = Hoster(hosterName = "good")
        val bad = Hoster(hosterName = "bad")
        val loader = AnimeHosterLoader(listOf(bad, good), {
            if (it ===
                bad
            ) {
                error("dead mirror")
            } else {
                listOf(video("ok"))
            }
        })
        loader.loadAll()
        assertTrue(loader.states.value[0].state is AnimeHosterState.Error)
        assertEquals(listOf("ok"), loader.videos().map { it.videoTitle })
    }

    @Test
    fun `a hanging hoster is cut at the time limit while the others play`() = runTest {
        val slow = Hoster(hosterName = "slow")
        val fast = Hoster(hosterName = "fast")
        val loader = AnimeHosterLoader(
            listOf(slow, fast),
            { if (it === slow) awaitCancellation() else listOf(video("fast")) },
            timeoutMs = 1_000,
        )
        val job = launch { loader.loadAll() }
        runCurrent()
        assertTrue(loader.states.value[0].state is AnimeHosterState.Loading)
        assertEquals(listOf("fast"), loader.videos().map { it.videoTitle })
        advanceTimeBy(1_001)
        job.join()
        assertTrue(loader.states.value[0].state is AnimeHosterState.Error)
    }

    /** A hoster that answers [videos] after [delayMs] (virtual time), or throws when [videos] is null. */
    private class Fake(val delayMs: Long, val videos: List<Video>?)

    private fun loaderOf(vararg hosters: Pair<String, Fake>, timeoutMs: Long = ANIME_HOSTER_TIMEOUT_MS) =
        AnimeHosterLoader(
            hosters.map { Hoster(hosterName = it.first) },
            { hoster ->
                val fake = hosters.first { it.first == hoster.hosterName }.second
                delay(fake.delayMs)
                fake.videos ?: error("dead mirror ${hoster.hosterName}")
            },
            timeoutMs,
        )

    @Test
    fun `a fast hoster starts after the grace while a slow one keeps loading`() = runTest {
        val loader = loaderOf(
            "slow" to Fake(8_000, listOf(video("slow", resolution = 1080))),
            "fast" to Fake(100, listOf(video("fast", resolution = 720))),
        )
        loader.start(backgroundScope)
        assertNull(loader.awaitPlayable(quality = 0))
        assertEquals(100 + ANIME_HOSTER_GRACE_MS, testScheduler.currentTime)
        assertEquals(listOf("fast"), loader.videos().map { it.videoTitle })
        assertTrue(loader.states.value[0].state is AnimeHosterState.Loading)
        assertFalse(loader.settled())
        // The slow hoster still arrives, for the quality sheet and the fallback.
        loader.awaitSettled()
        assertEquals(8_000L, testScheduler.currentTime)
        assertEquals(listOf("slow", "fast"), loader.videos().map { it.videoTitle })
    }

    @Test
    fun `a preferred video that arrives inside the grace still wins`() = runTest {
        val preferred = video("preferred", preferred = true)
        val loader = loaderOf(
            "fast" to Fake(100, listOf(video("fast"))),
            "preferred" to Fake(500, listOf(preferred)),
            "slow" to Fake(10_000, listOf(video("slow"))),
        )
        loader.start(backgroundScope)
        val resolved = loader.awaitPlayable(quality = 0, resolvePreferred = { it.copy(initialized = true) })
        assertEquals(preferred, resolved?.first)
        assertEquals(true, resolved?.second?.initialized)
        assertEquals(500L, testScheduler.currentTime)
        // Starting early no longer cancels the others.
        assertTrue(loader.states.value[2].state is AnimeHosterState.Loading)
    }

    @Test
    fun `a preferred video starts at once`() = runTest {
        val preferred = video("preferred", preferred = true)
        val loader = loaderOf("p" to Fake(0, listOf(preferred)), "slow" to Fake(10_000, listOf(video("slow"))))
        loader.start(backgroundScope)
        assertEquals(preferred, loader.awaitPlayable(quality = 0, resolvePreferred = { it })?.first)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun `a slow preferred video after the grace does not hold playback`() = runTest {
        val loader = loaderOf(
            "slow preferred" to Fake(8_000, listOf(video("preferred", preferred = true))),
            "fast" to Fake(100, listOf(video("fast"))),
        )
        loader.start(backgroundScope)
        assertNull(loader.awaitPlayable(quality = 0, resolvePreferred = { it }))
        assertEquals(100 + ANIME_HOSTER_GRACE_MS, testScheduler.currentTime)
    }

    @Test
    fun `a chosen quality waits for a hoster that has it`() = runTest {
        val loader = loaderOf(
            "fast 480" to Fake(100, listOf(video("480", resolution = 480))),
            "late 720" to Fake(2_000, listOf(video("720", resolution = 720))),
            "dead" to Fake(15_000, null),
        )
        loader.start(backgroundScope)
        assertNull(loader.awaitPlayable(quality = 720))
        assertEquals(2_000L, testScheduler.currentTime)
        assertEquals(listOf("480", "720"), loader.videos().map { it.videoTitle })
    }

    @Test
    fun `a chosen quality nobody has plays the closest after the longer grace`() = runTest {
        val loader = loaderOf(
            "fast 480" to Fake(100, listOf(video("480", resolution = 480))),
            "slow 1080" to Fake(10_000, listOf(video("1080", resolution = 1080))),
        )
        loader.start(backgroundScope)
        assertNull(loader.awaitPlayable(quality = 720))
        assertEquals(100 + ANIME_HOSTER_CLOSEST_GRACE_MS, testScheduler.currentTime)
    }

    @Test
    fun `when every hoster fails it returns once the last one has answered`() = runTest {
        val loader = loaderOf("a" to Fake(100, null), "b" to Fake(3_000, null))
        loader.start(backgroundScope)
        assertNull(loader.awaitPlayable(quality = 0))
        assertEquals(3_000L, testScheduler.currentTime)
        assertTrue(loader.settled())
        assertTrue(loader.videos().isEmpty())
    }

    @Test
    fun `a single hoster plays as soon as it answers, without a grace`() = runTest {
        val loader = loaderOf("only" to Fake(1_500, listOf(video("a"), video("b"))))
        loader.start(backgroundScope)
        assertNull(loader.awaitPlayable(quality = 720))
        assertEquals(1_500L, testScheduler.currentTime)
        assertEquals(listOf("a", "b"), loader.videos().map { it.videoTitle })
    }

    @Test
    fun `a hanging hoster beside a dead one still ends at the time limit`() = runTest {
        val loader = AnimeHosterLoader(
            listOf(Hoster(hosterName = "hang"), Hoster(hosterName = "dead")),
            { if (it.hosterName == "hang") awaitCancellation() else error("dead") },
            timeoutMs = 20_000,
        )
        loader.start(backgroundScope)
        assertNull(loader.awaitPlayable(quality = 0))
        assertEquals(20_000L, testScheduler.currentTime)
    }

    @Test
    fun `the fallback queue picks up a hoster that answered after playback started`() = runTest {
        val loader = loaderOf(
            "fast" to Fake(100, listOf(video("fast"))),
            "slow" to Fake(5_000, listOf(video("slow"))),
        )
        loader.start(backgroundScope)
        loader.awaitPlayable(quality = 0)
        val queue = AnimeStreamQueue(loader.videos(), 0, loader = loader)
        val attempted = mutableListOf<String>()
        val resolve: suspend (Video) -> Video = {
            attempted += it.videoTitle
            check(it.videoTitle != "fast") { "expired" }
            it
        }
        // "fast" fails; the queue waits for "slow" instead of giving up.
        assertEquals("slow", queue.next(resolve)?.videoTitle)
        assertEquals(5_000L, testScheduler.currentTime)
        assertNull(queue.next(resolve))
        assertEquals(listOf("fast", "slow"), attempted)
    }

    @Test
    fun `a queue without a loader stays on the videos it was given`() = runTest {
        val first = video("first")
        val queue = AnimeStreamQueue(listOf(first, video("other")), 0, first to first.copy(initialized = true))
        assertEquals(true, queue.next { it }?.initialized)
        assertEquals("other", queue.next { it }?.videoTitle)
        assertNull(queue.next { it })
    }

    @Test
    fun `cancelling the hoster scope does not leave the fallback waiting`() = runTest {
        val loader = loaderOf("fast" to Fake(100, listOf(video("fast"))), "hang" to Fake(60_000, listOf(video("x"))))
        val hosters = Job()
        loader.start(CoroutineScope(coroutineContext + hosters))
        loader.awaitPlayable(quality = 0)
        hosters.cancel()
        loader.awaitSettled()
        assertTrue(loader.settled())
    }

    @Test
    fun `a lazy hoster is not asked until it is picked`() = runTest {
        val lazy = Hoster(hosterName = "lazy", lazy = true)
        val normal = Hoster(hosterName = "normal")
        val asked = mutableListOf<String>()
        val loader = AnimeHosterLoader(listOf(lazy, normal), {
            asked += it.hosterName
            listOf(video(it.hosterName))
        })
        loader.loadAll()
        assertEquals(listOf("normal"), asked)
        assertEquals(AnimeHosterState.Idle, loader.states.value[0].state)
        loader.loadHoster(0)
        assertEquals(listOf("normal", "lazy"), asked)
        assertEquals(listOf("lazy", "normal"), loader.videos().map { it.videoTitle })
    }

    @Test
    fun `a hoster that brought its own videos needs no request`() = runTest {
        val own = Hoster(hosterName = "own", videoList = listOf(video("inline")))
        val loader = AnimeHosterLoader(listOf(own), { error("must not be called") })
        loader.loadAll()
        assertEquals(listOf("inline"), loader.videos().map { it.videoTitle })
    }

    @Test
    fun `on Auto the source order stands with its preferred video first`() {
        val a = video("a", resolution = 1080)
        val b = video("b", resolution = 480, preferred = true)
        val c = video("c", resolution = 720)
        assertEquals(listOf("b", "a", "c"), animePlaybackOrder(listOf(a, b, c), 0).map { it.videoTitle })
        // No preferred video: exactly the source's order, not highest resolution first.
        val d = video("d", resolution = 480)
        val e = video("e", resolution = 1080)
        assertEquals(listOf("d", "e"), animePlaybackOrder(listOf(d, e), 0).map { it.videoTitle })
    }

    @Test
    fun `a quality the user picked re-sorts, keeping source order between equals`() {
        val a = video("a", resolution = 1080)
        val b = video("b", resolution = 720)
        val c = video("c", resolution = 720)
        assertEquals(listOf("b", "c", "a"), animePlaybackOrder(listOf(a, b, c), 720).map { it.videoTitle })
    }

    @Test
    fun `videos without an address go last`() {
        val blank = Video(videoUrl = "", videoTitle = "blank")
        val ok = video("ok")
        assertEquals(listOf("ok", "blank"), animePlaybackOrder(listOf(blank, ok), 0).map { it.videoTitle })
    }
}
