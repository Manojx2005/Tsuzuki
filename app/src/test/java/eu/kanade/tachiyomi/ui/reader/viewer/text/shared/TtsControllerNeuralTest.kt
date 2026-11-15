package eu.kanade.tachiyomi.ui.reader.viewer.text.shared

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import eu.kanade.tachiyomi.tsuzuki.tts.FakeNeuralPlayer
import eu.kanade.tachiyomi.tsuzuki.tts.ListeningSession
import eu.kanade.tachiyomi.tsuzuki.tts.NeuralTtsProtocol
import eu.kanade.tachiyomi.tsuzuki.tts.TtsChunker
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import java.util.Locale

class TtsControllerNeuralTest {
    private class Harness(scope: TestScope) {
        val events = mutableListOf<String>()
        val spoken = mutableListOf<Triple<String, Int, String>>()
        val player = FakeNeuralPlayer()
        var neuralOn = true
        lateinit var listener: UtteranceProgressListener

        private fun <T> pref(value: T) = mockk<Preference<T>> { every { get() } returns value }

        private val englishVoice = mockk<Voice> {
            every { name } returns "en-voice"
            every { locale } returns Locale.ENGLISH
        }

        val engine = mockk<TextToSpeech> {
            every { isSpeaking } returns false
            every { stop() } returns TextToSpeech.SUCCESS
            every { voice } returns englishVoice
            every { voices } returns setOf(englishVoice)
            every { setVoice(englishVoice) } returns TextToSpeech.SUCCESS
            every { setSpeechRate(any()) } returns TextToSpeech.SUCCESS
            every { setPitch(any()) } returns TextToSpeech.SUCCESS
            every { speak(any<CharSequence>(), any(), any(), any()) } answers {
                spoken += Triple(firstArg<CharSequence>().toString(), secondArg<Int>(), arg<String>(3))
                TextToSpeech.SUCCESS
            }
            every { setOnUtteranceProgressListener(any()) } answers {
                listener = firstArg()
                TextToSpeech.SUCCESS
            }
        }
        val listening = mockk<ListeningSession>(relaxed = true) {
            every { startChunk(any(), any()) } answers { events += "stats start ${secondArg<Int>()}" }
            every { endChunk(any(), any(), any()) } answers {
                events += "stats end ${secondArg<Int>()}"
                true
            }
        }
        val callbacks = mockk<TtsController.Callbacks>(relaxed = true) {
            every { runOnUiThread(any()) } answers { firstArg<() -> Unit>().invoke() }
            every { onHighlightChunk(any(), any(), any(), any()) } answers {
                events += "highlight ${firstArg<Int>()}"
            }
        }
        val preferences = mockk<ReaderPreferences> {
            every { novelTtsEnableHighlight } returns pref(true)
            every { novelTtsAutoNextChapter } returns pref(false)
            every { novelTtsPitch } returns pref(1f)
            every { novelTtsSpeed } returns pref(1f)
            every { novelTtsVoice } returns pref("en-voice")
        }
        val controller = TtsController(
            mockk<Context>(),
            preferences,
            scope,
            listening,
            callbacks,
            { 0L },
            neuralGate = { neuralOn },
            newNeuralPlayer = { player },
        )

        init {
            fun field(name: String) = TtsController::class.java.getDeclaredField(name).apply { isAccessible = true }
            field("tts").set(controller, engine)
            field("ttsInitialized").setBoolean(controller, true)
            TtsController::class.java.getDeclaredMethod("setupListener").apply { isAccessible = true }
                .invoke(controller)
        }

        fun queueOf(utterance: String) = utterance.removePrefix("tts_utterance_").substringBefore('_')
        fun phoneSpeaks(utterance: String) {
            listener.onStart(utterance)
            listener.onDone(utterance)
        }
    }

    @Test
    fun `an uncovered chunk is spoken by the phone under the same queue then neural resumes in order`() = runTest {
        val h = Harness(this)
        h.controller.speak("p0 text\np1 name\np2 text\np3 text", chapterId = 9L)
        assertEquals(listOf(0), h.player.starts)
        assertTrue(h.spoken.isEmpty())

        val neural = h.player.listener!!
        neural.onStart(0)
        neural.onChunkDone(0)
        neural.onUncovered(1)

        // The phone voice speaks exactly chunk 1, flushing, with the queue id the neural chunks used.
        assertEquals(1, h.spoken.size)
        val (text, mode, utterance) = h.spoken.single()
        assertEquals("p1 name", text)
        assertEquals(TextToSpeech.QUEUE_FLUSH, mode)
        val queue = h.queueOf(utterance)
        assertEquals("tts_utterance_${queue}_1", utterance)
        assertEquals(listOf(0), h.player.starts)

        h.phoneSpeaks(utterance)
        assertEquals(listOf(0, 2), h.player.starts)
        h.player.listener!!.onStart(2)
        h.player.listener!!.onChunkDone(2)
        h.player.listener!!.onStart(3)
        h.player.listener!!.onChunkDone(3)

        // Highlights and listening stats saw every chunk once, in reading order, across both voices.
        assertEquals(
            listOf(
                "stats start 0", "highlight 0", "stats end 0",
                "stats start 1", "highlight 1", "stats end 1",
                "stats start 2", "highlight 2", "stats end 2",
                "stats start 3", "highlight 3", "stats end 3",
            ),
            h.events,
        )
    }

    @Test
    fun `a failed worker is retried once then the phone voice speaks the rest under the same queue`() = runTest {
        val h = Harness(this)
        h.controller.speak("p0\np1\np2", chapterId = 9L)
        h.player.listener!!.onFailure(1)
        assertTrue(h.player.killed)
        assertEquals(listOf(0, 1), h.player.starts)
        assertTrue(h.spoken.isEmpty())

        h.player.listener!!.onFailure(1)
        assertEquals(listOf("p1", "p2"), h.spoken.map { it.first })
        assertEquals(listOf(TextToSpeech.QUEUE_FLUSH, TextToSpeech.QUEUE_ADD), h.spoken.map { it.second })
    }

    @Test
    fun `switching the voice on mid chapter re-chunks so no phone sized chunk reaches the worker`() = runTest {
        val h = Harness(this)
        h.neuralOn = false
        val sentence = "The quiet morning began slowly and the long road stretched ahead of them. "
        val text = sentence.repeat(40).trim()
        h.controller.speak(text, chapterId = 9L)
        assertEquals(1, h.controller.ttsChunks.size)
        assertTrue(h.player.starts.isEmpty())
        h.controller.pause()

        h.neuralOn = true
        h.controller.resume()
        assertTrue(h.controller.ttsChunks.size > 1)
        assertTrue(h.controller.ttsChunks.all { it.length <= NeuralTtsProtocol.CHUNK_LENGTH })
        assertEquals(listOf(0), h.player.starts)

        // And back off again: chunks return to phone size, position kept by character offset.
        h.controller.pause()
        h.neuralOn = false
        h.controller.resume()
        assertEquals(1, h.controller.ttsChunks.size)
    }

    @Test
    fun `re-chunking maps the position by character offset`() {
        val text = (1..20).joinToString("\n") { "Paragraph number $it is here." }
        val phone = TtsChunker.plan(text, forNeural = false, phoneMaxLength = 4000)
        val neural = TtsChunker.plan(text, forNeural = true, phoneMaxLength = 4000)
        assertEquals(neural.chunks.size, phone.chunks.size) // paragraphs are short: one chunk each
        val long = "word ".repeat(200).trim() + "\n" + "tail one." + "\n" + "tail two."
        val oldPlan = TtsChunker.plan(long, forNeural = false, phoneMaxLength = 4000)
        val newPlan = TtsChunker.plan(long, forNeural = true, phoneMaxLength = 4000)
        assertEquals(3, oldPlan.chunks.size)
        assertTrue(newPlan.chunks.size > 3)
        assertEquals("tail two.", newPlan.chunks[TtsChunker.remapIndex(oldPlan, 2, newPlan)])
        assertEquals(0, TtsChunker.remapIndex(oldPlan, 0, newPlan))
    }

    @Test
    fun `a stale phone completion from an older queue does not resume neural`() = runTest {
        val h = Harness(this)
        h.controller.speak("p0\np1\np2", chapterId = 9L)
        h.player.listener!!.onUncovered(0)
        val queue = h.queueOf(h.spoken.single().third)
        h.listener.onDone("tts_utterance_${queue.toLong() - 1}_0")
        assertEquals(listOf(0), h.player.starts)
        h.controller.pause()
        h.listener.onDone("tts_utterance_${queue}_0")
        assertEquals(listOf(0), h.player.starts)
    }
}
