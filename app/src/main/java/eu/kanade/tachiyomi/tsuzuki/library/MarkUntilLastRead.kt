package eu.kanade.tachiyomi.tsuzuki.library

import androidx.compose.material3.SnackbarHostState
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.presentation.tsuzuki.selection.awaitTsuzukiUndo
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The unread chapters that come before the furthest chapter the reader has reached.
 *
 * [chapters] must be in reading order (first chapter first). The anchor is whichever is later
 * in that order: the chapter read most recently (from history), or the last chapter that is
 * read or has saved progress. The anchor itself is left alone, so a chapter still in progress
 * keeps its place.
 */
internal fun chaptersBeforeLastRead(chapters: List<Chapter>, history: List<History>): List<Chapter> {
    val lastReadId = history.filter { it.readAt != null }.maxByOrNull { it.readAt!!.time }?.chapterId
    val historyIndex = lastReadId?.let { id -> chapters.indexOfFirst { it.id == id } } ?: -1
    val progressIndex = chapters.indexOfLast { it.read || it.lastPageRead > 0 }
    val anchor = maxOf(historyIndex, progressIndex)
    if (anchor <= 0) return emptyList()
    return chapters.subList(0, anchor).filterNot { it.read }
}

/** What one run of "Mark until last read" did. */
internal data class MarkUntilLastReadOutcome(
    /** Chapters marked for each book whose write succeeded (books with nothing to mark are left out). */
    val marked: Map<Long, Int>,
    /** Books whose chapter write failed; nothing is counted for them. */
    val failedBooks: Int,
)

/** Library selection action "Mark until last read". */
internal class MarkUntilLastRead(
    private val chaptersOf: suspend (Manga) -> List<Chapter> = Injekt.get<GetChaptersByMangaId>().let { get ->
        { manga -> get.await(manga.id, applyScanlatorFilter = true) }
    },
    private val historyOf: suspend (Long) -> List<History> = Injekt.get<GetHistory>().let { get ->
        { mangaId -> get.await(mangaId) }
    },
    private val markRead: suspend (List<Chapter>) -> SetReadStatus.Result = Injekt.get<SetReadStatus>().let { set ->
        { chapters -> set.await(true, *chapters.toTypedArray()) }
    },
) {
    /**
     * Marks the chapters book by book. Only successful writes are counted; a book whose write
     * fails is counted in [MarkUntilLastReadOutcome.failedBooks] and the others still go ahead.
     */
    suspend fun await(books: List<Manga>): MarkUntilLastReadOutcome {
        val marked = mutableMapOf<Long, Int>()
        var failed = 0
        for (manga in books) {
            val chapters = chaptersOf(manga).sortedWith(getChapterSort(manga, sortDescending = false))
            val toMark = chaptersBeforeLastRead(chapters, historyOf(manga.id))
            if (toMark.isEmpty()) continue
            when (markRead(toMark)) {
                SetReadStatus.Result.Success -> marked[manga.id] = toMark.size
                // Another writer already marked them; nothing changed here.
                SetReadStatus.Result.NoChapters -> Unit
                is SetReadStatus.Result.InternalError -> failed++
            }
        }
        return MarkUntilLastReadOutcome(marked, failed)
    }
}

/** The snackbar text for [outcome]. */
internal fun markUntilLastReadMessage(application: android.app.Application, outcome: MarkUntilLastReadOutcome): String {
    val total = outcome.marked.values.sum()
    return when {
        outcome.failedBooks > 0 && total > 0 -> application.stringResource(
            MR.strings.tsz_mark_until_last_read_partial,
            application.pluralStringResource(MR.plurals.tsz_marked_until_last_read, total, total),
            application.pluralStringResource(
                MR.plurals.tsz_mark_until_last_read_failed,
                outcome.failedBooks,
                outcome.failedBooks,
            ),
        )
        outcome.failedBooks > 0 -> application.pluralStringResource(
            MR.plurals.tsz_mark_until_last_read_failed,
            outcome.failedBooks,
            outcome.failedBooks,
        )
        total == 0 -> application.stringResource(MR.strings.tsz_nothing_before_last_read)
        else -> application.pluralStringResource(MR.plurals.tsz_marked_until_last_read, total, total)
    }
}

/**
 * Runs the action for [selection]: offers undo first, then marks the chapters, updates the
 * library counts in place for the books that were written and says what changed.
 */
internal suspend fun runMarkUntilLastRead(
    selection: List<Manga>,
    snackbarHostState: SnackbarHostState,
    getLibraryManga: GetLibraryManga,
) {
    val application = Injekt.get<android.app.Application>()
    if (snackbarHostState.awaitTsuzukiUndo(
            application,
            application.stringResource(MR.strings.tsz_pending_mark_until_last_read),
        )
    ) {
        return
    }
    val outcome = withNonCancellableContext {
        MarkUntilLastRead().await(selection).also { result ->
            getLibraryManga.applyBatchChapterUpdates(
                result.marked.mapValues { (_, count) ->
                    val update: LibraryManga.() -> LibraryManga = {
                        copy(readCount = (readCount + count).coerceAtMost(totalChapters))
                    }
                    update
                },
            )
        }
    }
    snackbarHostState.showSnackbar(markUntilLastReadMessage(application, outcome))
}
