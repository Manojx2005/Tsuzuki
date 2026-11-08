package eu.kanade.tachiyomi.tsuzuki

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jsoup.Jsoup
import tachiyomi.core.common.preference.PreferenceStore
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

internal class NovelReadingEstimates(private val preferences: PreferenceStore) {
    private val rates = preferences.getString("tsz_novel_reading_rates", "")
    private val countMutex = Mutex()
    private val countRevision = MutableStateFlow(0L)
    val wordCountChanges = countRevision.asStateFlow()

    private fun words(chapterId: Long) = preferences.getInt("tsz_novel_words_$chapterId", -1)

    fun wordCount(chapterId: Long): Int? = words(chapterId).get().takeIf { it > 0 }
    fun rateChanges() = rates.changes()
    fun speed(): Double? = medianReadingRate(decodeRates(rates.get()))

    suspend fun cacheWords(chapterId: Long, content: String): Int? = countMutex.withLock {
        wordCount(chapterId) ?: countChapterWords(content).takeIf { it > 0 }?.also {
            words(chapterId).set(it)
            countRevision.value++
        }
    }

    @Synchronized
    fun recordSession(durationMillis: Long, progress: Map<Long, Int>) {
        if (durationMillis < 30_000 || progress.isEmpty()) return
        val readWords = progress.entries.sumOf { (id, percentage) ->
            (wordCount(id) ?: return) * (percentage / 100.0)
        }
        val rate = (readWords * 60_000 / durationMillis).roundToInt()
        if (readWords < 30 || rate !in MIN_RATE..MAX_RATE) return
        rates.set((decodeRates(rates.get()) + rate).takeLast(9).joinToString(","))
    }

    private fun decodeRates(raw: String) = raw.split(',').mapNotNull {
        it.toIntOrNull()?.takeIf { rate -> rate in MIN_RATE..MAX_RATE }
    }

    private companion object {
        const val MIN_RATE = 40
        const val MAX_RATE = 1_200
    }
}

/** The same timer window used to record ReadingDays. Each chapter contributes only new ground. */
internal class NovelReadingSession {
    private data class Span(val start: Int, var furthest: Int)

    private val spans = mutableMapOf<Long, Span>()
    private var collecting = false

    @Synchronized
    fun start() {
        spans.clear()
        collecting = true
    }

    @Synchronized
    fun note(chapterId: Long, previous: Int, current: Int) {
        if (!collecting) return
        val span = spans.getOrPut(chapterId) {
            val start = previous.coerceIn(0, 100)
            Span(start, start)
        }
        span.furthest = maxOf(span.furthest, current.coerceIn(0, 100))
    }

    @Synchronized
    fun discard() {
        collecting = false
        spans.clear()
    }

    @Synchronized
    fun finish(): Map<Long, Int> {
        val progress = spans.mapValues { (_, span) -> span.furthest - span.start }.filterValues { it > 0 }
        discard()
        return progress
    }
}

internal fun countChapterWords(content: String): Int {
    val text = Jsoup.parse(content).body().text()
    val boundaries = BreakIterator.getWordInstance(Locale.JAPANESE).apply { setText(text) }
    var count = 0
    var start = boundaries.first()
    var end = boundaries.next()
    while (end != BreakIterator.DONE) {
        if ((start until end).any { text[it].isLetterOrDigit() }) count++
        start = end
        end = boundaries.next()
    }
    return count
}

internal fun medianReadingRate(samples: List<Int>): Double? {
    if (samples.size < 3) return null
    val sorted = samples.sorted()
    return (sorted[(sorted.lastIndex / 2)] + sorted[sorted.size / 2]) / 2.0
}

internal data class NovelEstimateMinutes(val chapter: Int? = null, val book: Int? = null)

internal fun remainingNovelWords(words: Int, progress: Int): Double =
    words.toLong() * (100 - progress.coerceIn(0, 100)) / 100.0

internal data class NovelEstimateWords(val chapter: Int?, val otherUnread: Double?, val includeChapterInBook: Boolean) {
    fun minutes(progress: Int, rate: Double): NovelEstimateMinutes {
        val chapterLeft = chapter?.let { remainingNovelWords(it, progress) }
        val bookLeft = when {
            !includeChapterInBook -> otherUnread
            chapterLeft != null -> otherUnread?.plus(chapterLeft)
            else -> null
        }
        return NovelEstimateMinutes(
            chapter = chapterLeft?.takeIf { it > 0 }?.let { ceil(it / rate).toInt() },
            book = bookLeft?.takeIf { it > 0 }?.let { ceil(it / rate).toInt() },
        )
    }
}
