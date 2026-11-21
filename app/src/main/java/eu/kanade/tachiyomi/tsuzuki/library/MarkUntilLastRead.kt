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

/** Library selection action "Mark until last read". */
internal class MarkUntilLastRead(
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val getHistory: GetHistory = Injekt.get(),
    private val setReadStatus: SetReadStatus = Injekt.get(),
) {
    /** Marks the chapters and returns how many were marked for each book id. */
    suspend fun await(books: List<Manga>): Map<Long, Int> = books.associate { manga ->
        val chapters = getChaptersByMangaId.await(manga.id, applyScanlatorFilter = true)
            .sortedWith(getChapterSort(manga, sortDescending = false))
        val toMark = chaptersBeforeLastRead(chapters, getHistory.await(manga.id))
        if (toMark.isNotEmpty()) setReadStatus.await(true, *toMark.toTypedArray())
        manga.id to toMark.size
    }
}

/**
 * Runs the action for [selection]: offers undo first, then marks the chapters, updates the
 * library counts in place and says how many chapters changed.
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
    val marked = withNonCancellableContext {
        MarkUntilLastRead().await(selection).filterValues { it > 0 }.also { counts ->
            getLibraryManga.applyBatchChapterUpdates(
                counts.mapValues { (_, count) ->
                    val update: LibraryManga.() -> LibraryManga = {
                        copy(readCount = (readCount + count).coerceAtMost(totalChapters))
                    }
                    update
                },
            )
        }
    }
    val total = marked.values.sum()
    snackbarHostState.showSnackbar(
        if (total == 0) {
            application.stringResource(MR.strings.tsz_nothing_before_last_read)
        } else {
            application.pluralStringResource(MR.plurals.tsz_marked_until_last_read, total, total)
        },
    )
}
