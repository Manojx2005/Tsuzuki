package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.view.KeyEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.items.episode.model.Episode

class AnimeMediaSessionTest {
    private val ready = AnimePlayerState(loading = false, playing = true, position = 12.5, duration = 60.0)

    @Test
    fun `each player state maps to one session state`() {
        assertEquals(AnimeSessionPlayback.BUFFERING, animePlaybackState(AnimePlayerState()).playback)
        assertEquals(AnimeSessionPlayback.PLAYING, animePlaybackState(ready).playback)
        assertEquals(AnimeSessionPlayback.PAUSED, animePlaybackState(ready.copy(playing = false)).playback)
        assertEquals(AnimeSessionPlayback.BUFFERING, animePlaybackState(ready.copy(buffering = true)).playback)
        assertEquals(AnimeSessionPlayback.STOPPED, animePlaybackState(ready.copy(ended = true)).playback)
        assertEquals(AnimeSessionPlayback.ERROR, animePlaybackState(ready.copy(failed = true)).playback)
    }

    @Test
    fun `position, duration and actions follow the state`() {
        val session = animePlaybackState(ready)
        assertEquals(12_500L, session.positionMs)
        assertEquals(60_000L, session.durationMs)
        assertTrue(session.canSeek)
        assertFalse(session.canNext)
        assertFalse(session.canPrevious)
        val next = Episode(1, "/2", "Second", id = 9, episodeNumber = 2.0)
        assertTrue(animePlaybackState(ready.copy(next = next)).canNext)
        assertFalse(animePlaybackState(AnimePlayerState()).canSeek)
    }

    @Test
    fun `the media title joins anime and episode`() {
        assertEquals("Frieren — Episode 3", animeMediaTitle("Frieren", "Episode 3"))
        assertEquals("Frieren", animeMediaTitle("Frieren", null))
        assertEquals("Episode 3", animeMediaTitle(" ", "Episode 3"))
    }

    @Test
    fun `keys map to player actions`() {
        assertEquals(AnimeKeyAction.TOGGLE, animeKeyAction(KeyEvent.KEYCODE_SPACE))
        assertEquals(AnimeKeyAction.TOGGLE, animeKeyAction(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertEquals(AnimeKeyAction.PLAY, animeKeyAction(KeyEvent.KEYCODE_MEDIA_PLAY))
        assertEquals(AnimeKeyAction.PAUSE, animeKeyAction(KeyEvent.KEYCODE_MEDIA_PAUSE))
        assertEquals(AnimeKeyAction.BACK_10, animeKeyAction(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(AnimeKeyAction.FORWARD_10, animeKeyAction(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(AnimeKeyAction.NEXT, animeKeyAction(KeyEvent.KEYCODE_MEDIA_NEXT))
        assertEquals(AnimeKeyAction.PREVIOUS, animeKeyAction(KeyEvent.KEYCODE_MEDIA_PREVIOUS))
        assertNull(animeKeyAction(KeyEvent.KEYCODE_VOLUME_UP))
        assertNull(animeKeyAction(KeyEvent.KEYCODE_BACK))
    }

    @Test
    fun `a held toggle key acts once per press`() {
        // Held Space or a repeated media play/pause: down events with repeatCount 0, 1, 2, ...
        var playing = true
        repeat(2) {
            // two physical presses
            (0..6).forEach { repeatCount ->
                if (AnimeKeyAction.TOGGLE.runsOnDown(repeatCount)) playing = !playing
            }
        }
        assertTrue(playing) // paused by the first press, resumed by the second, never flipped by repeats
        for (action in listOf(
            AnimeKeyAction.PLAY,
            AnimeKeyAction.PAUSE,
            AnimeKeyAction.NEXT,
            AnimeKeyAction.PREVIOUS,
        )) {
            assertTrue(action.runsOnDown(0))
            assertFalse(action.runsOnDown(1))
        }
    }

    @Test
    fun `held seek keys keep seeking`() {
        val seeks = (0..4).count { AnimeKeyAction.FORWARD_10.runsOnDown(it) }
        assertEquals(5, seeks)
        assertTrue(AnimeKeyAction.BACK_10.runsOnDown(3))
    }
}
