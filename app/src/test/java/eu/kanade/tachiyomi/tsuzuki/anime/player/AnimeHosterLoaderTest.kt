package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.classOverridesHosterMethods
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun `a preferred stream starts immediately and cancels unfinished hosters`() = runTest {
        val slow = Hoster(hosterName = "slow")
        val fast = Hoster(hosterName = "fast")
        val preferred = video("preferred", preferred = true)
        var cancelled = false
        val loader = AnimeHosterLoader(listOf(slow, fast), {
            if (it === slow) {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            } else {
                listOf(preferred)
            }
        })
        val resolved = loader.loadAll { it.copy(initialized = true) }
        assertEquals(preferred, resolved?.first)
        assertEquals(true, resolved?.second?.initialized)
        assertTrue(cancelled)
        assertEquals(0L, testScheduler.currentTime)
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
