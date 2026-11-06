package eu.kanade.tachiyomi.tsuzuki.reader

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.NovelPageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat

/** Reading progress (percent of the chapter) after which the next chapter's text is fetched in the background. */
internal const val NEXT_CHAPTER_WARMUP_PERCENT = 75
private const val WARMUP_TIMEOUT_MS = 30_000L

internal fun shouldWarmNextChapter(percent: Int): Boolean = percent >= NEXT_CHAPTER_WARMUP_PERCENT

/**
 * Card Q13: with infinite scroll off, › Next used to fetch the chapter text only after the tap, leaving a blank
 * page for about a second on network sources. Once the reader passes [NEXT_CHAPTER_WARMUP_PERCENT] of a chapter,
 * this prepares the next chapter (page list) and loads its text, once per chapter, without changing what is shown.
 * The Next tap then finds the chapter loaded (`ChapterLoader` skips it) and the text present (`awaitPageText`
 * returns at once). Nothing is written to disk.
 */
internal class NovelNextChapterWarmup {
    @Volatile private var warmedFrom: Long? = null
    private var job: Job? = null

    fun onProgress(
        percent: Int,
        currentChapterId: Long?,
        scope: CoroutineScope,
        prepareNext: suspend () -> ReaderChapter?,
    ) {
        if (currentChapterId == null || !shouldWarmNextChapter(percent) || warmedFrom == currentChapterId) return
        warmedFrom = currentChapterId
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            try {
                val next = prepareNext() ?: return@launch
                val page = next.pages?.firstOrNull() ?: return@launch
                val loader = next.pageLoader ?: return@launch
                NovelPageLoader.awaitPageText("NextChapterWarmup", page, loader, WARMUP_TIMEOUT_MS, this)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logcat(LogPriority.WARN) { "Next chapter warm-up failed (${e.message}); the Next tap will load it" }
                warmedFrom = null
            }
        }
    }
}
