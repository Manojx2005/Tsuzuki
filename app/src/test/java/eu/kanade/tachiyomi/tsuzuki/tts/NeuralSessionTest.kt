package eu.kanade.tachiyomi.tsuzuki.tts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FakeNeuralPlayer : NeuralPlayer {
    var listener: NeuralListener? = null
    val starts = mutableListOf<Int>()
    var stops = 0
    var closed = false
    var killed = false
    override var isPlaying = false

    override fun play(chunks: List<String>, startIndex: Int, speed: Float, listener: NeuralListener) {
        this.listener = listener
        starts += startIndex
        isPlaying = true
    }

    override fun stop() {
        stops++
        isPlaying = false
    }

    override fun close(kill: Boolean) {
        closed = true
        killed = kill
        isPlaying = false
    }
}

class NeuralSessionTest {
    private val chunks = listOf("zero", "one", "two", "three")
    private val phone = mutableListOf<Triple<Long, Int, Int>>()
    private val events = mutableListOf<String>()
    private val players = mutableListOf<FakeNeuralPlayer>()
    private var playersAvailable = 10

    private val session = NeuralSession(
        newPlayer = { if (playersAvailable-- > 0) FakeNeuralPlayer().also { players += it } else null },
        speakPhone = { queue, from, through -> phone += Triple(queue, from, through) },
        onStart = { queue, index -> events += "start $queue/$index" },
        onChunkDone = { queue, index -> events += "done $queue/$index" },
    )

    @Test
    fun `an uncovered chunk is spoken alone by the phone then neural resumes at the next chunk`() {
        assertTrue(session.start(7, chunks, 0, 1f))
        val first = players.single()
        first.listener!!.onStart(0)
        first.listener!!.onChunkDone(0)
        first.listener!!.onUncovered(1)

        assertEquals(listOf(Triple(7L, 1, 1)), phone)
        assertEquals(listOf(0), first.starts)

        // The phone voice finishing a different chunk, or another queue, must not resume neural.
        session.phoneChunkDone(7, 0)
        session.phoneChunkDone(8, 1)
        assertEquals(listOf(0), first.starts)

        session.phoneChunkDone(7, 1)
        assertSame(first, players.single())
        assertEquals(listOf(0, 2), first.starts)
        first.listener!!.onStart(2)
        first.listener!!.onChunkDone(2)
        first.listener!!.onUncovered(3)
        // Uncovered final chunk: the phone voice finishes the chapter, nothing resumes.
        assertEquals(Triple(7L, 3, 3), phone.last())
        session.phoneChunkDone(7, 3)
        assertEquals(listOf(0, 2), first.starts)
        assertEquals(listOf("start 7/0", "done 7/0", "start 7/2", "done 7/2"), events)
    }

    @Test
    fun `stopping cancels the pending resume`() {
        session.start(1, chunks, 0, 1f)
        players.single().listener!!.onUncovered(1)
        session.stop()
        session.phoneChunkDone(1, 1)
        assertEquals(listOf(0), players.single().starts)
    }

    @Test
    fun `a failure is retried once on a fresh killed worker then the chapter uses the phone voice`() {
        session.start(3, chunks, 0, 1f)
        val first = players.single()
        first.listener!!.onFailure(2)
        assertTrue(first.closed)
        assertTrue(first.killed)
        assertEquals(2, players.size)
        val second = players[1]
        assertNotSame(first, second)
        assertEquals(listOf(2), second.starts)
        assertTrue(phone.isEmpty())

        second.listener!!.onFailure(2)
        assertEquals(listOf(Triple(3L, 2, 3)), phone)
        assertEquals(2, players.size)

        // Still failed for this chapter: new requests go straight to the phone voice.
        assertFalse(session.start(3, chunks, 0, 1f))
        // The next chapter gets another chance and its own single retry.
        session.beginChapter()
        assertTrue(session.start(4, chunks, 0, 1f))
    }

    @Test
    fun `an unavailable worker falls back immediately`() {
        playersAvailable = 0
        assertFalse(session.start(1, chunks, 0, 1f))
    }

    @Test
    fun `chunks longer than the worker limit never reach it`() {
        val long = listOf("short", "x".repeat(NeuralTtsProtocol.CHUNK_LENGTH + 1), "tail")
        assertFalse(session.start(1, long, 0, 1f))
        assertTrue(players.isEmpty())
        // Starting after the long chunk is fine.
        assertTrue(session.start(1, long, 2, 1f))
        assertEquals(listOf(2), players.single().starts)
    }

    @Test
    fun `watchdog allowance scales with text length and speed and is generous only while cold`() {
        val warm1x = neuralWatchdogMs(180, 1f, cold = false)
        assertTrue(warm1x > neuralWatchdogMs(20, 1f, cold = false))
        assertTrue(neuralWatchdogMs(180, 0.5f, cold = false) > warm1x)
        assertTrue(neuralWatchdogMs(180, 2f, cold = false) < warm1x)
        // 360 characters (this chunk playing, the next synthesizing) at half speed is far beyond the old 30 s.
        assertTrue(neuralWatchdogMs(360, 0.5f, cold = false) > 100_000L)
        assertEquals(NEURAL_COLD_START_MS, neuralWatchdogMs(180, 1f, cold = true) - warm1x)
        assertTrue(neuralWatchdogMs(0, 2f, cold = true) >= NEURAL_COLD_START_MS)
    }
}
