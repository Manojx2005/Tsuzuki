package eu.kanade.tachiyomi.tsuzuki.anime.ui

import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimePictureInPictureTest {
    private val playing = AnimePlayerState(playing = true, loading = false, duration = 90.0)

    @Test
    fun playingVideoCanEnterAndOffersPauseBetweenTenSecondSeeks() {
        val controls = playing.pipControls(enabled = true, inPip = false)

        assertTrue(controls.canEnter)
        assertEquals(listOf(AnimePipAction.REWIND, AnimePipAction.PAUSE, AnimePipAction.FORWARD), controls.actions)
    }

    @Test
    fun restoredPausedVideoOffersPlayAndDoesNotEnterAutomatically() {
        val paused = playing.copy(playing = false, position = 42.0)
        val controls = paused.pipControls(enabled = true, inPip = true)

        assertFalse(controls.canEnter)
        assertEquals(listOf(AnimePipAction.REWIND, AnimePipAction.PLAY, AnimePipAction.FORWARD), controls.actions)
    }

    @Test
    fun disablingPreferenceStopsEntryAndKeepsControlsInAnAlreadyOpenWindow() {
        assertEquals(AnimePipControls(false, emptyList()), playing.pipControls(enabled = false, inPip = false))

        val pinned = playing.pipControls(enabled = false, inPip = true)
        assertFalse(pinned.canEnter)
        assertEquals(AnimePipAction.PAUSE, pinned.actions[1])
    }

    @Test
    fun loadingOrFailedVideoCannotEnterAndUnseekableVideoOnlyOffersPlaybackControl() {
        assertEquals(AnimePipControls(false, emptyList()), playing.copy(loading = true).pipControls(true, false))
        assertEquals(AnimePipControls(false, emptyList()), playing.copy(failed = true).pipControls(true, false))
        assertEquals(listOf(AnimePipAction.PAUSE), playing.copy(duration = 0.0).pipControls(true, true).actions)
    }
}
