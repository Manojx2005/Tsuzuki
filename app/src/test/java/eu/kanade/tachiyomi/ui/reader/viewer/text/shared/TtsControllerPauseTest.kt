package eu.kanade.tachiyomi.ui.reader.viewer.text.shared

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import eu.kanade.tachiyomi.tsuzuki.tts.ListeningSession
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TtsControllerPauseTest {
    @Test
    fun pausesDuringSilentGapAndInvalidatesPendingChapterHandoff() = runTest {
        val engine = mockk<TextToSpeech> {
            every { isSpeaking } returns false
            every { stop() } returns TextToSpeech.SUCCESS
        }
        val listening = mockk<ListeningSession>(relaxed = true)
        val controller = TtsController(
            mockk<Context>(),
            mockk<ReaderPreferences>(),
            backgroundScope,
            listening,
            mockk<TtsController.Callbacks>(),
            { 0L },
        )
        fun field(name: String) = TtsController::class.java.getDeclaredField(name).apply { isAccessible = true }
        field("tts").set(controller, engine)
        field("ttsInitialized").setBoolean(controller, true)
        field("playbackQueue").set(controller, 42L)
        field("ttsChunks").set(controller, listOf("one", "two", "three"))
        controller.ttsCurrentChunkIndex = 2
        controller.pause()
        assertTrue(controller.isPaused())
        assertEquals(2, controller.ttsResumeChunkIndex)
        assertNull(field("playbackQueue").get(controller))
        controller.pause()
        verify(exactly = 1) { engine.stop() }
        verify(exactly = 1) { listening.endQueue(42L) }
    }

    @Test
    fun pauseAfterChapterEndsWithNothingFollowingDoesNotReplayLastChunk() = runTest {
        val listener = slot<UtteranceProgressListener>()
        val engine = mockk<TextToSpeech> {
            every { isSpeaking } returns false
            every { stop() } returns TextToSpeech.SUCCESS
            every { setOnUtteranceProgressListener(capture(listener)) } returns TextToSpeech.SUCCESS
        }
        val listening = mockk<ListeningSession>(relaxed = true) {
            every { endChunk(any(), any(), any()) } returns true
        }
        val callbacks = mockk<TtsController.Callbacks>(relaxed = true) {
            every { runOnUiThread(any()) } answers { firstArg<() -> Unit>().invoke() }
        }
        val controller = TtsController(
            mockk<Context>(),
            mockk<ReaderPreferences>(),
            backgroundScope,
            listening,
            callbacks,
            { 0L },
        )
        fun field(name: String) = TtsController::class.java.getDeclaredField(name).apply { isAccessible = true }
        field("tts").set(controller, engine)
        field("ttsInitialized").setBoolean(controller, true)
        field("playbackQueue").set(controller, 42L)
        field("ttsChunks").set(controller, listOf("one", "two", "three"))
        TtsController::class.java.getDeclaredMethod("setupListener").apply { isAccessible = true }.invoke(controller)
        controller.ttsCurrentChunkIndex = 2

        // Auto-next off and no sleep timer: the chapter simply ends.
        listener.captured.onDone("tts_utterance_42_2")
        controller.pause()

        assertFalse(controller.isPaused())
        assertNull(field("playbackQueue").get(controller))
        verify(exactly = 0) { engine.stop() }
        verify(exactly = 1) { listening.endQueue(42L) }
        verify(exactly = 1) { callbacks.onClearHighlights() }
    }
}
