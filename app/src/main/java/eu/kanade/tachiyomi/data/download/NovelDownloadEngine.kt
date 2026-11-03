package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.translation.TranslationJob
import eu.kanade.tachiyomi.data.translation.TranslationService
import eu.kanade.tachiyomi.jsplugin.source.JsSource
import eu.kanade.tachiyomi.source.isNovelSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.chapter.ChapterImageEmbedder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.NovelDownloadPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.translation.service.TranslationPreferences
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Everything [Downloader] does differently for novel sources: paced text chapters, one chapter per novel at a time,
 * per-novel pause, chapter-count progress and auto-translation.
 *
 * Kept out of Downloader.kt so that file stays close to Mihon's and Mihon's download commits can be cherry-picked
 * (docs/UPSTREAM.md). Request pacing itself lives in the shared OkHttp client (PerHostDynamicRateLimitInterceptor).
 */
internal class NovelDownloadEngine(
    private val context: Context,
    private val preferences: NovelDownloadPreferences,
    private val translationPreferences: TranslationPreferences,
    private val translationService: TranslationService,
) {

    /**
     * Novel manga ids whose downloads are individually paused, independent of the global queue state.
     * Downloads of these groups are excluded from the active set while the queue runs.
     */
    private val _pausedMangaIds = MutableStateFlow<Set<Long>>(emptySet())
    val pausedMangaIds = _pausedMangaIds.asStateFlow()

    private val chapterTotals = ConcurrentHashMap<Long, Int>()

    fun pause(mangaId: Long) = _pausedMangaIds.update { it + mangaId }

    fun resume(mangaId: Long) = _pausedMangaIds.update { it - mangaId }

    fun parallelLimit(): Flow<Int> = preferences.parallelNovelDownloads().changes()

    /** Picks the novel downloads to run: one chapter per novel, up to [max] novels, skipping paused ones. */
    fun pickActive(queue: List<Download>, max: Int): List<Download> {
        val limit = max.coerceAtLeast(1)
        val used = HashSet<Long>(limit)
        return queue.asSequence()
            .filter { it.source.isNovelSource() }
            // Ignore completed downloads, leave them in the queue
            .filter { it.status.value <= Download.State.DOWNLOADING.value }
            .filter { it.mangaId !in _pausedMangaIds.value }
            .filter { used.add(it.mangaId) }
            .take(limit)
            .toList()
    }

    /**
     * Fetches and saves a novel page's text. Returns false when the page carries only an image,
     * which the caller downloads like a manga page.
     */
    suspend fun fetchPage(page: Page, download: Download, tmpDir: UniFile): Boolean {
        logcat { "NovelDownloadEngine.fetchPage: page=${page.number}, url=${page.url}, hasText=${page.text != null}" }

        if (page.text == null) {
            try {
                page.text = download.source.fetchPageText(page)
                logcat { "  -> Fetched text, length=${page.text?.length ?: 0}" }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e) { "  -> Error fetching novel page text" }
                page.status = Page.State.Error(e)
                return true
            }
        }

        if (page.text.isNullOrBlank()) {
            if (!page.imageUrl.isNullOrEmpty()) return false
            logcat(LogPriority.ERROR) { "  -> Novel page ${page.number} returned no text; failing download" }
            page.status = Page.State.Error(Exception("Chapter is empty - the source returned no text"))
            return true
        }

        val digitCount = (download.pages?.size ?: 0).toString().length.coerceAtLeast(3)
        val filename = "%0${digitCount}d.html".format(Locale.ENGLISH, page.number)

        // Embed images if enabled
        val htmlContent = if (preferences.downloadChapterImages().get()) {
            val baseUrl = page.url.takeIf { it.isNotBlank() }
                ?: (download.source as? HttpSource)?.baseUrl
                ?: (download.source as? JsSource)?.baseUrl
            ChapterImageEmbedder().processHtml(page.text!!, baseUrl, tmpDir)
        } else {
            page.text!!
        }

        val file = try {
            tmpDir.createFile(filename)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to create file $filename" }
            null
        }
        if (file == null) {
            page.status = Page.State.Error(Exception("Failed to create file $filename"))
            return true
        }

        file.openOutputStream().use { it.write(htmlContent.toByteArray()) }
        page.uri = file.uri
        page.progress = 100
        page.status = Page.State.Ready
        return true
    }

    /** "Chapter n of total" for this novel's current batch, for the progress notification. */
    fun progress(download: Download, queue: List<Download>): Pair<Int, Int> {
        val outstanding = queue.count { it.mangaId == download.mangaId && it.source.isNovelSource() }
        val total = chapterTotals.merge(download.mangaId, outstanding, ::maxOf)!!
        return (total - outstanding + 1).coerceIn(1, total) to total
    }

    /**
     * Called when a novel download job ends, on every path (success, error, or cancellation). Removes the novel's
     * tracked total once nothing of it is queued or downloading, so a stale total can't leak into a later batch.
     */
    fun clearTotalIfDone(mangaId: Long, queue: List<Download>) {
        val hasOutstanding = queue.any {
            it.mangaId == mangaId &&
                it.source.isNovelSource() &&
                it.status.value <= Download.State.DOWNLOADING.value
        }
        if (!hasOutstanding) chapterTotals.remove(mangaId)
    }

    fun archiveCompressionLevel(): Int = preferences.zipCompressionLevel().get()

    /** Queues the chapter for translation when auto-translate of downloads is on. */
    fun onDownloaded(manga: Manga?, chapter: Chapter?) {
        if (manga == null || chapter == null) return
        if (!translationPreferences.translationEnabled().get()) return
        if (!translationPreferences.autoTranslateDownloads().get()) return
        translationService.enqueue(manga = manga, chapter = chapter, priority = TranslationService.PRIORITY_LOW)
        TranslationJob.start(context)
    }
}
