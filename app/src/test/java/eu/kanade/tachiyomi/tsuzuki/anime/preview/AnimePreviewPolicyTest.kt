package eu.kanade.tachiyomi.tsuzuki.anime.preview

import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class AnimePreviewPolicyTest {
    @Test
    fun sourceOrderDeduplicatesAndOffNeverReturnsNetworkUrls() {
        assertEquals(listOf("preview", "cover"), previewUrls("preview", "cover", true))
        assertEquals(listOf("cover"), previewUrls(" ", "cover", true))
        assertEquals(listOf("same"), previewUrls("same", "same", true))
        assertTrue(previewUrls("preview", "cover", false).isEmpty())
    }

    @Test
    fun wifiPolicyAndSavedModeSurvivePlayerRecreation() {
        assertTrue(previewNetworkAllowed(EpisodePreviewMode.ON, false))
        assertFalse(previewNetworkAllowed(EpisodePreviewMode.WIFI_ONLY, false))
        assertTrue(previewNetworkAllowed(EpisodePreviewMode.WIFI_ONLY, true))
        assertFalse(previewNetworkAllowed(EpisodePreviewMode.OFF, true))
        val store = InMemoryPreferenceStore()
        assertEquals(EpisodePreviewMode.ON, AnimePlayerPreferences(store).episodePreviews.get())
        AnimePlayerPreferences(store).episodePreviews.set(EpisodePreviewMode.OFF)
        assertEquals(EpisodePreviewMode.OFF, AnimePlayerPreferences(store).episodePreviews.get())
    }

    @Test
    fun rejectBlackFadeButKeepVisibleFrameAtThreshold() {
        assertFalse(frameIsVisible(IntArray(100) { 0xff070707.toInt() }))
        assertFalse(frameIsVisible(intArrayOf()))
        assertTrue(frameIsVisible(IntArray(100) { 0xff080808.toInt() }))
        assertTrue(frameIsVisible(intArrayOf(0xffff0000.toInt())))
    }

    @Test
    fun capEvictsOldestUntilUnderLimitWithDeterministicTieBreak() {
        val files = listOf(PreviewFile("b", 40, 1), PreviewFile("c", 70, 2), PreviewFile("a", 10, 1))
        assertEquals(listOf("a", "b"), previewFilesToEvict(files, 70))
        assertTrue(previewFilesToEvict(files, 120).isEmpty())
        assertEquals(listOf("a", "b", "c"), previewFilesToEvict(files, 0))
    }

    @Test
    fun frameAndNetworkPolicyInvalidateCacheAndUnknownDurationHasNoChip() {
        val model = AnimeEpisodePreview(1, 2, "preview", "cover", 10, 3, true)
        assertNotEquals(model.cacheKey, model.copy(frameVersion = 11).cacheKey)
        assertNotEquals(model.cacheKey, model.copy(networkAllowed = false).cacheKey)
        assertNotEquals(model.cacheKey, model.copy(sourceId = 4).cacheKey)
        // Captured frames stay per episode; shared artwork (no frame) is one cache entry for all its episodes.
        assertNotEquals(model.cacheKey, model.copy(episodeId = 99).cacheKey)
        val shared = model.copy(frameVersion = 0)
        assertEquals(shared.cacheKey, shared.copy(episodeId = 99, animeId = 7).cacheKey)
        assertNotEquals(shared.cacheKey, shared.copy(previewUrl = "other").cacheKey)
        assertNotEquals(shared.cacheKey, shared.copy(coverUrl = "other").cacheKey)
        assertNotEquals(shared.cacheKey, shared.copy(networkAllowed = false).cacheKey)
        assertNotEquals(shared.cacheKey, model.cacheKey)
        assertNull(previewRemaining(5, 0))
        assertNull(previewRemaining(5, -1))
        assertEquals(15L, previewRemaining(5, 20))
        assertEquals(0L, previewRemaining(25, 20))
    }

    @Test
    fun cacheKeysDistinguishUrlBoundariesAndMissingUrls() {
        for (frameVersion in listOf(0L, 10L)) {
            val model = AnimeEpisodePreview(
                animeId = 1,
                episodeId = 2,
                previewUrl = "https://example.org/preview;https://example.org/other",
                coverUrl = "https://example.org/cover",
                frameVersion = frameVersion,
                sourceId = 3,
                networkAllowed = true,
            )
            val differentUrls = model.copy(
                previewUrl = "https://example.org/preview",
                coverUrl = "https://example.org/other;https://example.org/cover",
            )
            assertNotEquals(model.cacheKey, differentUrls.cacheKey)
            assertNotEquals(model.copy(previewUrl = null).cacheKey, model.copy(previewUrl = "null").cacheKey)
            assertNotEquals(model.copy(coverUrl = null).cacheKey, model.copy(coverUrl = "").cacheKey)
        }
    }

    @Test
    fun captureThrottleDoesNotLoseFinalCloseOrAllowIncognito() {
        val gate = PreviewCaptureGate()
        assertFalse(gate.acquire(1, 4.9, 0, false, false))
        assertFalse(gate.acquire(1, 10.0, 0, false, true))
        assertTrue(gate.acquire(1, 10.0, 0, false, false))
        assertFalse(gate.acquire(1, 20.0, 29_999, false, false))
        assertTrue(gate.acquire(1, 40.0, 30_000, false, false))
        assertTrue(gate.acquire(2, 10.0, 30_001, false, false))
        assertTrue(gate.acquire(1, 40.0, 30_002, true, false))
        assertFalse(gate.acquire(1, 40.0, 30_003, true, false))
        assertFalse(gate.acquire(1, Double.NaN, 60_000, false, false))
    }
}
