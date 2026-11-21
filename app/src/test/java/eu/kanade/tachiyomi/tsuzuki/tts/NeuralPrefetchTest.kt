package eu.kanade.tachiyomi.tsuzuki.tts

import kotlinx.coroutines.CompletableDeferred
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NeuralPrefetchTest {
    @Test
    fun `the matching continuation gets the prepared audio`() {
        val slot = NeuralPrefetchSlot<String>()
        val audio = CompletableDeferred<String>()
        slot.prepare("1.0|next", audio)
        assertSame(audio, slot.take("1.0|next"))
        assertFalse(audio.isCancelled)
        assertFalse(slot.isPending)
    }

    @Test
    fun `unrelated playback, a newer prefetch and stop cancel it`() {
        val slot = NeuralPrefetchSlot<String>()
        val unrelated = CompletableDeferred<String>()
        slot.prepare("1.0|next", unrelated)
        assertNull(slot.take("1.0|another chapter"))
        assertTrue(unrelated.isCancelled)

        val older = CompletableDeferred<String>()
        val newer = CompletableDeferred<String>()
        slot.prepare("1.0|a", older)
        slot.prepare("1.0|b", newer)
        assertTrue(older.isCancelled)

        slot.cancel() // explicit STOP or unbind
        assertTrue(newer.isCancelled)
        assertFalse(slot.isPending)
    }

    @Test
    fun `stop while the phone voice speaks an uncovered chunk still names the worker's request`() {
        val target = NeuralStopTarget()
        target.started(7)
        target.uncovered(7) // the reader's request ended; the worker prepares the continuation for request 7
        assertEquals(7, target.stop())
        assertNull(target.stop())
    }

    @Test
    fun `a continuation replaces the handed-off request, and a finished chapter needs no stop`() {
        val target = NeuralStopTarget()
        target.started(7)
        target.uncovered(7)
        target.started(8) // phone voice done: the continuation request
        assertEquals(8, target.stop())

        target.started(9)
        target.finished(9)
        assertNull(target.stop())
    }
}
