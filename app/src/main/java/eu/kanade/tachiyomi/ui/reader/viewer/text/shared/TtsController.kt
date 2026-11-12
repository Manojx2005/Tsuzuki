package eu.kanade.tachiyomi.ui.reader.viewer.text.shared

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import eu.kanade.tachiyomi.tsuzuki.tts.TtsSleepTimer
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import java.util.Locale

class TtsController(
    private val context: Context,
    private val preferences: ReaderPreferences,
    private val scope: CoroutineScope,
    private val callbacks: Callbacks,
    private val elapsedMs: () -> Long = SystemClock::elapsedRealtime,
) : TextToSpeech.OnInitListener {

    interface Callbacks {
        fun onInitialized(pendingRequest: StartRequest?)
        fun getCurrentPage(): ReaderPage?
        fun onPlaybackStarted()
        fun onHighlightChunk(chunkIndex: Int, chunk: String, startOffset: Int, paragraphIndex: Int)
        fun onClearHighlights()
        fun onLastChunkDone()
        fun runOnUiThread(action: () -> Unit)
    }

    enum class StartRequest { NORMAL, VIEWPORT }

    private var tts: TextToSpeech? = null
    var ttsInitialized = false
        private set

    var isTtsAutoPlay = false
    var ttsPaused = false

    var ttsChunks: List<String> = emptyList()
        private set
    var ttsChunkParagraphIndexes: List<Int> = emptyList()
        private set
    var ttsChunkStartOffsets: List<Int> = emptyList()
        private set
    internal var listenParagraphs: List<String> = emptyList()
        private set

    @Volatile
    internal var sleepTimer: TtsSleepTimer = TtsSleepTimer.Off
        private set
    private var sleepJob: Job? = null

    @Volatile private var sleepDeadlineMs: Long? = null

    var ttsPlaybackChapterIndex: Int = 0
        private set
    var ttsPlaybackChapterId: Long? = null
        private set

    @Volatile var ttsCurrentChunkIndex = 0

    @Volatile var ttsResumeChunkIndex: Int = 0
    var ttsViewportParagraphIndex: Int = 0
    var hasViewportStartOverride: Boolean = false
    var pendingStartRequest: StartRequest? = null

    fun ensureInitialized() {
        if (tts == null) {
            try {
                tts = TextToSpeech(context, this)
                logcat(LogPriority.DEBUG) { "TTS: Initialization started" }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "TTS: Failed to create instance: ${e.message}" }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsInitialized = true
            applySettings()
            setupListener()
            val pending = pendingStartRequest
            pendingStartRequest = null
            callbacks.runOnUiThread {
                callbacks.onInitialized(pending)
            }
        } else {
            ttsInitialized = false
            logcat(LogPriority.ERROR) { "TTS initialization failed with status: $status" }
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                utteranceId?.removePrefix("tts_utterance_")?.toIntOrNull()?.let { chunkIndex ->
                    ttsCurrentChunkIndex = chunkIndex
                    if (preferences.novelTtsEnableHighlight.get()) {
                        val chunk = ttsChunks.getOrNull(chunkIndex) ?: return
                        val startOffset = ttsChunkStartOffsets.getOrElse(chunkIndex) { 0 }
                        val paragraphIndex = ttsChunkParagraphIndexes.getOrElse(chunkIndex) { chunkIndex }
                        callbacks.runOnUiThread {
                            callbacks.onHighlightChunk(chunkIndex, chunk, startOffset, paragraphIndex)
                        }
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                val finishedIndex = utteranceId?.removePrefix("tts_utterance_")?.toIntOrNull() ?: -1
                val isLastChunk = finishedIndex >= ttsChunks.size - 1
                if (isLastChunk && finishedIndex >= 0 && sleepTimer == TtsSleepTimer.ChapterEnd) {
                    val chapterId = ttsPlaybackChapterId
                    callbacks.runOnUiThread {
                        scope.launch {
                            delay(LAST_CHUNK_DONE_DELAY_MS)
                            if (sleepTimer == TtsSleepTimer.ChapterEnd && !isSpeaking() &&
                                ttsPlaybackChapterId == chapterId && finishedIndex == ttsChunks.lastIndex
                            ) {
                                stop()
                            }
                        }
                    }
                } else if (isLastChunk && isTtsAutoPlay && preferences.novelTtsAutoNextChapter.get()) {
                    callbacks.runOnUiThread {
                        scope.launch {
                            delay(LAST_CHUNK_DONE_DELAY_MS)
                            if (!isSpeaking()) {
                                if (sleepTimer == TtsSleepTimer.ChapterEnd) stop() else callbacks.onLastChunkDone()
                            }
                        }
                    }
                } else if (isLastChunk) {
                    callbacks.runOnUiThread {
                        callbacks.onClearHighlights()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                logcat(LogPriority.ERROR) { "TTS error on utterance: $utteranceId" }
            }
        })
    }

    /**
     * Shift the cached playback chapter index when chapters are removed from the front of the
     * loaded queue (TTS chapter trimming). [ttsPlaybackChapterId] is unaffected, so id-based
     * lookups stay correct regardless. Floors at 0.
     */
    fun shiftPlaybackChapterIndex(removedFromFront: Int) {
        ttsPlaybackChapterIndex = (ttsPlaybackChapterIndex - removedFromFront).coerceAtLeast(0)
    }

    fun speak(text: String, chapterIndex: Int = 0, chapterId: Long? = null) {
        if (!ttsInitialized || tts == null) {
            logcat(LogPriority.WARN) { "TTS not initialized, cannot speak" }
            return
        }
        ttsPlaybackChapterIndex = chapterIndex
        ttsPlaybackChapterId = chapterId
        applySettings()
        ttsPaused = false

        val maxLength = TextToSpeech.getMaxSpeechInputLength().takeIf { it > 0 } ?: 4000
        val paragraphs = TtsTextUtils.newlineParagraphBounds(text).map { (start, end) -> text.substring(start, end) }
        val chunkParagraphIndexes = mutableListOf<Int>()

        val chunks = if (paragraphs.size > 1) {
            paragraphs.flatMapIndexed { paragraphIndex, para ->
                val c = if (para.length <= maxLength) {
                    listOf(para)
                } else {
                    TtsTextUtils.splitTextForTts(para, maxLength)
                }
                repeat(c.size) { chunkParagraphIndexes.add(paragraphIndex) }
                c
            }
        } else if (text.length <= maxLength) {
            chunkParagraphIndexes.add(0)
            listOf(text)
        } else {
            val c = TtsTextUtils.splitTextForTts(text, maxLength)
            repeat(c.size) { chunkParagraphIndexes.add(0) }
            c
        }
        ttsChunks = chunks
        listenParagraphs = paragraphs.map { it.take(180) }
        ttsChunkParagraphIndexes = chunkParagraphIndexes

        // Compute char offset per chunk so Spannable-based highlight can locate the right occurrence.
        val offsets = mutableListOf<Int>()
        var searchFrom = 0
        for (chunk in ttsChunks) {
            val idx = text.indexOf(chunk, searchFrom)
            if (idx >= 0) {
                offsets.add(idx)
                searchFrom = idx + chunk.length
            } else {
                offsets.add(searchFrom)
            }
        }
        ttsChunkStartOffsets = offsets

        ttsCurrentChunkIndex = 0
        val startIndex = if (hasViewportStartOverride) {
            ttsChunkParagraphIndexes.indexOfFirst { it >= ttsViewportParagraphIndex }
                .takeIf { it >= 0 } ?: 0
        } else {
            0
        }
        hasViewportStartOverride = false

        speakChunksFrom(startIndex.coerceIn(0, (ttsChunks.size - 1).coerceAtLeast(0)))
    }

    fun speakChunksFrom(startIndex: Int) {
        if (ttsChunks.isEmpty() || startIndex >= ttsChunks.size) return
        val engine = tts ?: return
        callbacks.onPlaybackStarted()
        ttsChunks.drop(startIndex).forEachIndexed { i, chunk ->
            val actualIndex = startIndex + i
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(chunk, mode, null, "tts_utterance_$actualIndex")
        }
    }

    fun pause() {
        if (ttsInitialized && tts?.isSpeaking == true) {
            ttsPaused = true
            ttsResumeChunkIndex = ttsCurrentChunkIndex
            tts?.stop()
        }
    }

    fun resume() {
        if (ttsPaused && ttsChunks.isNotEmpty()) {
            ttsPaused = false
            speakChunksFrom(ttsResumeChunkIndex)
        }
    }

    internal fun remainingSleepSeconds(nowMs: Long = elapsedMs()): Long? =
        sleepDeadlineMs?.let { ((it - nowMs).coerceAtLeast(0) + 999) / 1_000 }

    internal fun setSleepTimer(option: TtsSleepTimer) {
        sleepJob?.cancel()
        sleepJob = null
        sleepTimer = option
        sleepDeadlineMs = option.minutes?.let { elapsedMs() + it * 60_000L }
        option.minutes?.let { minutes ->
            sleepJob = scope.launch {
                delay(minutes * 60_000L)
                sleepJob = null
                stop()
            }
        }
    }

    fun stop() {
        setSleepTimer(TtsSleepTimer.Off)
        isTtsAutoPlay = false
        ttsPaused = false
        pendingStartRequest = null
        ttsChunks = emptyList()
        listenParagraphs = emptyList()
        ttsChunkParagraphIndexes = emptyList()
        ttsChunkStartOffsets = emptyList()
        ttsCurrentChunkIndex = 0
        ttsResumeChunkIndex = 0
        hasViewportStartOverride = false
        if (ttsInitialized) tts?.stop()
        ttsPlaybackChapterIndex = 0
        ttsPlaybackChapterId = null
        callbacks.runOnUiThread { callbacks.onClearHighlights() }
    }

    fun stepParagraph(delta: Int, onEmpty: () -> Unit) {
        if (delta == 0) return
        if (ttsChunks.isEmpty()) {
            onEmpty()
            return
        }

        val target = TtsTextUtils.computeTtsStepTargetChunk(
            delta,
            ttsPaused,
            ttsResumeChunkIndex,
            ttsCurrentChunkIndex,
            ttsChunks,
            ttsChunkParagraphIndexes,
        )
        startFromChunk(target)
    }

    /**
     * Jumps directly to the first chunk of [paragraphIndex] (a media-notification seek, or any
     * future direct paragraph-jump caller) - as opposed to [stepParagraph]'s relative +/-1 move.
     * No-ops if nothing is loaded; clamps past the end to the last chunk.
     */
    fun seekToParagraph(paragraphIndex: Int) {
        if (ttsChunks.isEmpty()) return
        val target = ttsChunkParagraphIndexes.indexOfFirst { it >= paragraphIndex }
            .takeIf { it >= 0 } ?: (ttsChunks.size - 1)
        startFromChunk(target)
    }

    private fun startFromChunk(target: Int) {
        ttsResumeChunkIndex = target
        ttsCurrentChunkIndex = target
        ttsPaused = false
        tts?.stop()
        speakChunksFrom(target)
    }

    fun isSpeaking(): Boolean = ttsInitialized && tts?.isSpeaking == true
    fun isPaused(): Boolean = ttsPaused
    fun isStarting(): Boolean =
        pendingStartRequest != null || (!ttsInitialized && tts != null) || (ttsChunks.isEmpty() && isTtsAutoPlay)

    /**
     * Current (paragraphIndex, paragraphCount) - distinct from chunk index/count: a paragraph too
     * long for a single TTS utterance splits into multiple chunks (see [speak]), so paragraph
     * count is always <= chunk count. Used by the media notification for a "paragraph N of M"
     * display and as the seek-bar's position/duration basis.
     */
    fun getParagraphProgress(): Pair<Int, Int> {
        if (ttsChunks.isEmpty()) return 0 to 0
        val current = (if (ttsPaused) ttsResumeChunkIndex else ttsCurrentChunkIndex)
            .coerceIn(0, ttsChunks.size - 1)
        val paragraphIndex = ttsChunkParagraphIndexes.getOrElse(current) { current }
        val paragraphCount = (ttsChunkParagraphIndexes.maxOrNull() ?: current) + 1
        return paragraphIndex to paragraphCount
    }

    fun applySettings() {
        tts?.let { engine ->
            val voicePref = preferences.novelTtsVoice.get()
            if (voicePref.isNotEmpty()) {
                val selected = engine.voices?.find { it.name == voicePref }
                if (selected != null) {
                    engine.voice = selected
                } else {
                    try {
                        engine.language = Locale.forLanguageTag(voicePref)
                    } catch (e: Exception) {
                        engine.language = Locale.getDefault()
                    }
                }
            } else {
                engine.language = Locale.getDefault()
            }
            engine.setSpeechRate(preferences.novelTtsSpeed.get())
            engine.setPitch(preferences.novelTtsPitch.get())
        }
    }

    fun getAvailableVoices(): List<Pair<String, String>> =
        tts?.voices?.map { voice ->
            Pair(voice.name, "${voice.locale.displayLanguage} (${voice.name})")
        }?.sortedBy { it.second } ?: emptyList()

    internal fun getCurrentVoiceLabel(appLocale: Locale, offlineLabel: String, onlineLabel: String): String {
        val voice = tts?.voice ?: return preferences.novelTtsVoice.get()
        val connectionLabel = if (voice.isNetworkConnectionRequired) onlineLabel else offlineLabel
        return eu.kanade.tachiyomi.tsuzuki.tts.ttsVoiceLabel(voice.name, voice.locale, appLocale, connectionLabel)
    }

    fun destroy() {
        setSleepTimer(TtsSleepTimer.Off)
        tts?.stop()
        callbacks.runOnUiThread { callbacks.onClearHighlights() }
        tts?.shutdown()
        tts = null
        ttsInitialized = false
    }

    companion object {
        private const val LAST_CHUNK_DONE_DELAY_MS = 500L
    }
}
