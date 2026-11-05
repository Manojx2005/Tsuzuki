package eu.kanade.tachiyomi.tsuzuki.tts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TtsStatus { STOPPED, PAUSED, PLAYING }

/** What novel TTS is reading right now. Paragraph progress is left out so the state only changes when it matters. */
data class TtsNowPlayingState(
    val status: TtsStatus = TtsStatus.STOPPED,
    val bookTitle: String = "",
    val chapterName: String = "",
    val mangaId: Long = -1L,
    val chapterId: Long = -1L,
) {
    val isActive: Boolean get() = status != TtsStatus.STOPPED
}

/**
 * Tsuzuki-owned, read-only view of `TtsPlaybackService` for the Listening widget. The service reports into it with
 * one-line hooks; nothing here drives playback.
 */
object TtsNowPlaying {

    private val mutableState = MutableStateFlow(TtsNowPlayingState())
    val state: StateFlow<TtsNowPlayingState> = mutableState.asStateFlow()

    /** Playback started, paused, resumed or moved to another chapter. */
    fun onSync(isPaused: Boolean, bookTitle: String, chapterName: String, mangaId: Long, chapterId: Long) {
        mutableState.value = TtsNowPlayingState(
            status = if (isPaused) TtsStatus.PAUSED else TtsStatus.PLAYING,
            bookTitle = bookTitle,
            chapterName = chapterName,
            mangaId = mangaId,
            chapterId = chapterId,
        )
    }

    /** The service paused or resumed from a notification, headset or widget action (before the reader syncs). */
    fun onPausedChanged(isPaused: Boolean) {
        mutableState.value = mutableState.value.let {
            if (!it.isActive) it else it.copy(status = if (isPaused) TtsStatus.PAUSED else TtsStatus.PLAYING)
        }
    }

    fun onStopped() {
        mutableState.value = TtsNowPlayingState()
    }
}
