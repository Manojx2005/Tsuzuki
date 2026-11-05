package eu.kanade.tachiyomi.tsuzuki.tts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TtsNowPlayingTest {

    @BeforeEach
    fun reset() = TtsNowPlaying.onStopped()

    @Test
    fun `starts stopped`() {
        assertEquals(TtsStatus.STOPPED, TtsNowPlaying.state.value.status)
        assertFalse(TtsNowPlaying.state.value.isActive)
    }

    @Test
    fun `sync playing then paused then new chapter`() {
        TtsNowPlaying.onSync(false, "Book", "Ch. 1", 7, 70)
        assertEquals(TtsStatus.PLAYING, TtsNowPlaying.state.value.status)
        assertEquals("Book", TtsNowPlaying.state.value.bookTitle)
        TtsNowPlaying.onSync(true, "Book", "Ch. 1", 7, 70)
        assertEquals(TtsStatus.PAUSED, TtsNowPlaying.state.value.status)
        TtsNowPlaying.onSync(false, "Book", "Ch. 2", 7, 71)
        assertEquals("Ch. 2", TtsNowPlaying.state.value.chapterName)
        assertEquals(71L, TtsNowPlaying.state.value.chapterId)
    }

    @Test
    fun `paused change keeps the book and is ignored when stopped`() {
        TtsNowPlaying.onPausedChanged(true)
        assertEquals(TtsStatus.STOPPED, TtsNowPlaying.state.value.status)
        TtsNowPlaying.onSync(false, "Book", "Ch. 1", 7, 70)
        TtsNowPlaying.onPausedChanged(true)
        assertEquals(TtsStatus.PAUSED, TtsNowPlaying.state.value.status)
        assertEquals(7L, TtsNowPlaying.state.value.mangaId)
        TtsNowPlaying.onPausedChanged(false)
        assertTrue(TtsNowPlaying.state.value.status == TtsStatus.PLAYING)
    }

    @Test
    fun `stop clears everything`() {
        TtsNowPlaying.onSync(false, "Book", "Ch. 1", 7, 70)
        TtsNowPlaying.onStopped()
        assertEquals(TtsNowPlayingState(), TtsNowPlaying.state.value)
    }
}
