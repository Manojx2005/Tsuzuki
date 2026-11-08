package eu.kanade.presentation.tsuzuki.reader

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.tachiyomi.data.download.ChapterContentReader
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.tsuzuki.NovelEstimateMinutes
import eu.kanade.tachiyomi.tsuzuki.NovelEstimateWords
import eu.kanade.tachiyomi.tsuzuki.NovelReadingEstimates
import eu.kanade.tachiyomi.tsuzuki.remainingNovelWords
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal
import tachiyomi.source.local.isLocalNovel
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Both details and reader use the same local-only estimates. Progress changes only redo the math. */
@Composable
internal fun TsuzukiReadingEstimateLabel(
    manga: Manga,
    chapterId: Long,
    progress: Int,
    knownChapters: List<Chapter>? = null,
) {
    val incognito = remember { Injekt.get<BasePreferences>().incognitoMode }
    val incognitoChanges = remember(incognito) { incognito.changes() }
    val incognitoMode by incognitoChanges.collectAsState(initial = incognito.get())
    if (incognitoMode) return
    val store = remember { Injekt.get<NovelReadingEstimates>() }
    val rateChanges = remember { store.rateChanges() }
    val rateChange by rateChanges.collectAsState(initial = "")
    val rate = remember(rateChange) { store.speed() } ?: return
    val snapshot by key(manga.id, chapterId) {
        estimateWords(manga, chapterId, knownChapters, store)
    }
    val minutes = snapshot?.minutes(progress, rate) ?: return
    TsuzukiEstimateText(minutes)
}

@Composable
private fun estimateWords(
    manga: Manga,
    chapterId: Long,
    knownChapters: List<Chapter>?,
    store: NovelReadingEstimates,
): State<NovelEstimateWords?> {
    val context = LocalContext.current
    return produceState<NovelEstimateWords?>(null, manga, knownChapters) {
        val chapterChanges = knownChapters?.let { flowOf(it) } ?: flow {
            emitAll(Injekt.get<ChapterRepository>().getChapterByMangaIdAsFlow(manga.id, applyScanlatorFilter = true))
        }
        // The active chapter uses the live slider value. Progress saves also change the
        // database timestamp and version, which do not affect its estimate.
        val relevantChapters = chapterChanges
            .map { chapters ->
                chapters.map {
                    if (it.id == chapterId) it.copy(lastPageRead = 0, lastModifiedAt = 0, version = 0) else it
                }
            }
            .distinctUntilChanged()
        val downloadChanges = Injekt.get<DownloadManager>().statusFlow()
            .filter { it.mangaId == manga.id && it.status == Download.State.DOWNLOADED }
            .map { Unit }
            .onStart { emit(Unit) }
        combine(relevantChapters, downloadChanges, store.wordCountChanges) { chapters, _, _ -> chapters }
            // Counting can write cache entries. Finish each scan and coalesce its refreshes.
            .conflate()
            .collect { chapters ->
                value = calculateEstimateWords(context, manga, chapterId, chapters, store)
            }
    }
}

private suspend fun calculateEstimateWords(
    context: android.content.Context,
    manga: Manga,
    chapterId: Long,
    chapters: List<Chapter>,
    store: NovelReadingEstimates,
): NovelEstimateWords = withContext(Dispatchers.IO) {
    val selected = chapters.find { it.id == chapterId }
    val unread = chapters.filter { !it.read && it.id != chapterId }
    val isLocal = manga.isLocal() || manga.isLocalNovel()
    val missing = (unread + listOfNotNull(selected)).filter { store.wordCount(it.id) == null }
    val downloaded = if (isLocal || missing.isEmpty()) {
        emptySet()
    } else {
        Injekt.get<DownloadManager>().getDownloadedChapterIds(missing, manga)
    }
    val source by lazy { Injekt.get<SourceManager>().getOrStub(manga.source) }
    val reader by lazy { ChapterContentReader(context, Injekt.get<DownloadProvider>()) }

    fun available(chapter: Chapter) = store.wordCount(chapter.id) != null ||
        isLocal || chapter.id in downloaded

    suspend fun countChapter(chapter: Chapter): Int? {
        ensureActive()
        store.wordCount(chapter.id)?.let { return it }
        if (!available(chapter)) return null
        val content = try {
            if (isLocal) {
                source.fetchPageText(Page(0, chapter.url))
            } else {
                reader.readDownloadedContent(manga, chapter, source)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN) {
                "Could not read chapter ${chapter.id} for the time estimate (${e::class.simpleName})"
            }
            null
        } ?: return null
        return store.cacheWords(chapter.id, content)
    }

    val chapterWords = selected?.let { countChapter(it) } ?: store.wordCount(chapterId)
    val otherWords = if (selected != null && unread.all(::available)) {
        var total = 0.0
        for (chapter in unread) {
            val words = countChapter(chapter) ?: return@withContext NovelEstimateWords(
                chapterWords,
                null,
                !selected.read,
            )
            total += remainingNovelWords(words, chapter.lastPageRead.coerceIn(0L, 100L).toInt())
        }
        total
    } else {
        null
    }
    NovelEstimateWords(chapterWords, otherWords, selected?.read == false)
}

@Composable
private fun TsuzukiEstimateText(minutes: NovelEstimateMinutes) {
    val tokens = TsuzukiTheme.tokens
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT) }
    val labels = listOfNotNull(
        minutes.chapter?.let { stringResource(MR.strings.tsz_time_left_chapter, formatter.formatMinutes(it)) },
        minutes.book?.let { stringResource(MR.strings.tsz_time_left_book, formatter.formatMinutes(it)) },
    )
    if (labels.isEmpty()) return
    val label = labels.joinToString(" · ")
    Text(
        text = if (tokens.isDot) label.uppercase(locale) else label,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
        color = tokens.mutedText,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun MeasureFormat.formatMinutes(minutes: Int): String = when {
    minutes < 60 -> formatMeasures(Measure(minutes, MeasureUnit.MINUTE))
    minutes % 60 == 0 -> formatMeasures(Measure(minutes / 60, MeasureUnit.HOUR))
    else -> formatMeasures(Measure(minutes / 60, MeasureUnit.HOUR), Measure(minutes % 60, MeasureUnit.MINUTE))
}
