package eu.kanade.tachiyomi.tsuzuki.tts

import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.TtsController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TtsSleepTimerTest {
    @Test
    fun replacementCancelsTheOldTimerAndTheNewTimerStopsPlayback() = runTest {
        val callbacks = mockk<TtsController.Callbacks>(relaxed = true)
        every { callbacks.runOnUiThread(any()) } answers { firstArg<() -> Unit>()() }
        val listening = mockk<ListeningSession>(relaxed = true)
        val controller = TtsController(mockk(), mockk(), this, listening, callbacks)

        controller.setSleepTimer(TtsSleepTimer.Fifteen)
        advanceTimeBy(10 * 60_000L)
        controller.setSleepTimer(TtsSleepTimer.Thirty)
        advanceTimeBy(6 * 60_000L)
        verify(exactly = 0) { callbacks.onClearHighlights() }

        advanceTimeBy(24 * 60_000L)
        runCurrent()
        assertEquals(TtsSleepTimer.Off, controller.sleepTimer)
        verify(exactly = 1) { callbacks.onClearHighlights() }
        verify(exactly = 1) { listening.endRun() }
    }

    @Test
    fun automaticDocumentHandoffPreservesTheRunButStopAndCloseEndIt() = runTest {
        val callbacks = mockk<TtsController.Callbacks>(relaxed = true)
        every { callbacks.runOnUiThread(any()) } answers { firstArg<() -> Unit>()() }
        val listening = mockk<ListeningSession>(relaxed = true)
        val controller = TtsController(mockk(), mockk(), this, listening, callbacks)

        controller.stop(preserveListeningRun = true)
        verify(exactly = 0) { listening.endRun() }
        controller.stop()
        verify(exactly = 1) { listening.endRun() }
        controller.destroy()
        verify(exactly = 2) { listening.endRun() }
    }
}
