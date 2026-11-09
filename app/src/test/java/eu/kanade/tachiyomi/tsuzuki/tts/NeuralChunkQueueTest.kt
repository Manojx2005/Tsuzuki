package eu.kanade.tachiyomi.tsuzuki.tts

import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.TtsTextUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NeuralChunkQueueTest {
    @Test
    fun `synthesizes one chunk ahead while audio plays`() = runTest {
        val synthesized = mutableListOf<String>()
        val started = mutableListOf<Int>()
        val firstPlayed = CompletableDeferred<Unit>()
        val job = launch {
            playNeuralChunks(
                listOf("one", "two", "three"),
                firstIndex = 4,
                synthesize = {
                    synthesized.add(it)
                    it
                },
                play = { if (it == "one") firstPlayed.await() },
                onStart = { started.add(it) },
                onDone = {},
                onFailure = { _, error -> throw error },
            )
        }
        runCurrent()
        assertEquals(listOf("one", "two"), synthesized)
        assertEquals(listOf(4), started)

        firstPlayed.complete(Unit)
        job.join()
        assertEquals(listOf("one", "two", "three"), synthesized)
        assertEquals(listOf(4, 5, 6), started)
    }

    @Test
    fun `cancelled playback drops the queued audio and restart seeks to a new chunk`() = runTest {
        val started = mutableListOf<Int>()
        var completions = 0
        val old = launch {
            playNeuralChunks(
                listOf("old", "prefetched"),
                firstIndex = 0,
                synthesize = { it },
                play = { awaitCancellation() },
                onStart = { started.add(it) },
                onDone = { completions++ },
                onFailure = { _, error -> throw error },
            )
        }
        runCurrent()
        old.cancelAndJoin()
        playNeuralChunks(
            listOf("seek target"),
            firstIndex = 8,
            synthesize = { it },
            play = {},
            onStart = { started.add(it) },
            onDone = { completions++ },
            onFailure = { _, error -> throw error },
        )
        assertEquals(listOf(0, 8), started)
        assertEquals(1, completions)
    }

    @Test
    fun `load and prefetch failures identify the chunk for Android fallback`() = runTest {
        val played = mutableListOf<String>()
        val failures = mutableListOf<Int>()
        for (chunks in listOf(listOf("missing"), listOf("available", "missing"))) {
            playNeuralChunks(
                chunks,
                firstIndex = 4,
                synthesize = {
                    check(it != "missing")
                    it
                },
                play = { played.add(it) },
                onStart = {},
                onDone = { error("Unexpected completion") },
                onFailure = { index, _ -> failures.add(index) },
            )
        }
        assertEquals(listOf("available"), played)
        assertEquals(listOf(4, 5), failures)
    }

    @Test
    fun `neural chunks keep the length limit without splitting a surrogate pair`() {
        val input = "a".repeat(179) + "😀" + "b".repeat(30) + " end"
        val chunks = TtsTextUtils.splitTextForTts(input, NeuralTtsProtocol.CHUNK_LENGTH, allowLongWords = false)
        assertEquals(179, chunks.first().length)
        assertTrue(chunks[1].startsWith("😀"))
        assertTrue(chunks.all { it.length <= NeuralTtsProtocol.CHUNK_LENGTH })
        assertTrue(TtsTextUtils.splitTextForTts(input, NeuralTtsProtocol.CHUNK_LENGTH).first().length > 180)
    }
}
