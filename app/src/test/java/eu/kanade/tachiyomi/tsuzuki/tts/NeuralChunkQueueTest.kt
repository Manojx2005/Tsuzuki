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
        val completed = mutableListOf<Int>()
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
                onChunkDone = { completed.add(it) },
                onFailure = { _, error -> throw error },
            )
        }
        runCurrent()
        assertEquals(listOf("one", "two"), synthesized)
        assertEquals(listOf(4), started)
        assertTrue(completed.isEmpty())

        firstPlayed.complete(Unit)
        job.join()
        assertEquals(listOf("one", "two", "three"), synthesized)
        assertEquals(listOf(4, 5, 6), started)
        assertEquals(listOf(4, 5, 6), completed)
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

    @Test
    fun `the first chunk is split after its first sentence so the first audio is short`() = runTest {
        val synthesized = mutableListOf<String>()
        val started = mutableListOf<Int>()
        val completed = mutableListOf<Int>()
        val played = mutableListOf<String>()
        val firstChunk = "The morning began quietly. Nobody spoke until the bell rang!"
        playNeuralChunks(
            listOf(firstChunk, "Second chunk. Stays whole."),
            firstIndex = 3,
            synthesize = {
                synthesized.add(it)
                it
            },
            play = { played.add(it) },
            onStart = { started.add(it) },
            onDone = {},
            onChunkDone = { completed.add(it) },
            onFailure = { _, error -> throw error },
        )
        assertEquals(
            listOf("The morning began quietly.", "Nobody spoke until the bell rang!", "Second chunk. Stays whole."),
            synthesized,
        )
        assertEquals(synthesized, played)
        // One start and one completion per chunk, not per piece.
        assertEquals(listOf(3, 4), started)
        assertEquals(listOf(3, 4), completed)
        assertEquals(null, splitFirstSentence("No boundary here at all"))
        assertEquals(null, splitFirstSentence("Only one sentence."))
        assertEquals(null, splitFirstSentence("Mr. A"))
    }

    @Test
    fun `a chunk the pack cannot speak stops playback and is reported without a failure`() = runTest {
        val synthesized = mutableListOf<String>()
        val started = mutableListOf<Int>()
        val completed = mutableListOf<Int>()
        val uncovered = mutableListOf<Int>()
        playNeuralChunks(
            listOf("covered", "UNKNOWN name", "never reached"),
            firstIndex = 10,
            synthesize = {
                synthesized.add(it)
                it
            },
            covered = { it != "UNKNOWN name" },
            play = {},
            onStart = { started.add(it) },
            onDone = { error("The chapter did not finish") },
            onChunkDone = { completed.add(it) },
            onUncovered = { uncovered.add(it) },
            onFailure = { _, error -> throw error },
        )
        assertEquals(listOf("covered"), synthesized)
        assertEquals(listOf(10), started)
        assertEquals(listOf(10), completed)
        assertEquals(listOf(11), uncovered)
    }

    @Test
    fun `an uncovered first chunk plays nothing from that chunk`() = runTest {
        val uncovered = mutableListOf<Int>()
        playNeuralChunks<String>(
            listOf("An uncovered first sentence. And a second one."),
            firstIndex = 2,
            synthesize = { error("must not synthesize") },
            covered = { false },
            play = { error("must not play") },
            onStart = { error("must not start") },
            onDone = {},
            onUncovered = { uncovered.add(it) },
            onFailure = { _, error -> throw error },
        )
        assertEquals(listOf(2), uncovered)
    }
}
