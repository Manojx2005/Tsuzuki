package eu.kanade.tachiyomi.ui.reader.viewer.text.shared

import android.content.Context
import android.speech.tts.TextToSpeech
import eu.kanade.tachiyomi.tsuzuki.tts.ListeningSession
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
}
