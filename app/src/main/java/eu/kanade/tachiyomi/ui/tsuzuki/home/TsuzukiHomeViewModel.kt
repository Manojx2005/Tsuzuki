package eu.kanade.tachiyomi.ui.tsuzuki.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import eu.kanade.presentation.tsuzuki.novel.novelChapterHeading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import mihon.core.viewmodel.StateViewModel
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.model.asMangaCover
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar
import tachiyomi.domain.manga.model.MangaCover as CoverData

/** A book shown on Home: the "Continue" hero or a cover in the "Reading" grid. */
@Immutable
data class HomeBook(
    val mangaId: Long,
    /** Last chapter read, or null when the book was never opened (resume opens details instead). */
    val chapterId: Long?,
    val title: String,
    val cover: CoverData,
    val chapterLabel: String?,
    /** Chapters read ÷ total, or null when the book isn't in the library. */
    val progress: Float?,
    /** Chapters fetched since the book was last read (only for books with history). */
    val unread: Int,
    val isNovel: Boolean,
    /** Latest chapters for the tablet details pane (newest first, at most 5). */
    val recentChapters: List<HomeChapter> = emptyList(),
    /** Source name and chapter count, e.g. "109 ch · Royal Road". */
    val meta: String? = null,
)

/** One row in the details pane's chapter list. */
@Immutable
data class HomeChapter(
    val id: Long,
    val number: String,
    val name: String,
    val status: ChapterStatus,
)

enum class ChapterStatus { NEW, IN_PROGRESS, READ, UNREAD }

@Immutable
data class HomeState(
    val loading: Boolean = true,
    val books: List<HomeBook> = emptyList(),
    /** True when [books] come from reading history (resume works) rather than just the library. */
    val fromHistory: Boolean = false,
    val libraryCount: Int = 0,
    val newChapters: Int = 0,
    val todayMinutes: Int = 0,
) {
    val isEmpty get() = !loading && books.isEmpty()
    val hero get() = books.firstOrNull()
    val shelf get() = books.drop(1)
}

/** Chapter number as shown in the details pane: whole numbers without a decimal point. */
internal fun homeChapterNumber(number: Double): String =
    if (number >= 0 && number % 1.0 == 0.0) number.toLong().toString() else number.toString()

/** "Ch. 12" label for a chapter without a name; null when the number is unknown (negative). */
internal fun homeChapterLabel(number: Double): String? = when {
    number < 0 -> null
    number % 1.0 == 0.0 -> "Ch. ${number.toLong()}"
    else -> "Ch. $number"
}

/** Status pill for a chapter row: read, started, fetched since the book was last read, or plain unread. */
internal fun homeChapterStatus(read: Boolean, lastPageRead: Long, dateFetch: Long, lastReadAt: Long): ChapterStatus =
    when {
        read -> ChapterStatus.READ
        lastPageRead > 0 -> ChapterStatus.IN_PROGRESS
        dateFetch > lastReadAt -> ChapterStatus.NEW
        else -> ChapterStatus.UNREAD
    }

class TsuzukiHomeViewModel(
    getHistory: GetHistory = Injekt.get(),
    getLibraryManga: GetLibraryManga = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val sourceManager: tachiyomi.domain.source.service.SourceManager = Injekt.get(),
) : StateViewModel<HomeState>(HomeState()) {

    init {
        homeStates(
            history = getHistory.subscribe(query = "", limit = HISTORY_WINDOW),
            library = getLibraryManga.subscribe(),
            updates = libraryPreferences.newUpdatesCount.changes(),
            load = ::buildState,
        )
            .catch { mutableState.value = HomeState(loading = false) }
            .onEach { mutableState.value = it }
            .launchIn(viewModelScope)
    }

    /** The chapter to open for [book]: its last-read chapter, or the next one if that was finished. */
    suspend fun chapterToResume(book: HomeBook): Chapter? {
        val chapterId = book.chapterId ?: return null
        return withIOContext {
            getNextChapters.await(book.mangaId, chapterId, onlyUnread = false).firstOrNull()
        }
    }

    private suspend fun buildState(
        history: List<HistoryWithRelations>,
        library: List<LibraryManga>,
    ): HomeState {
        val byId = library.associateBy { it.id }

        val recent = history
            .filter { it.readAt != null }
            .sortedByDescending { it.readAt }
            .distinctBy { it.mangaId }
            .take(MAX_BOOKS)
            .map { h ->
                val lib = byId[h.mangaId]
                // One query per book (at most MAX_BOOKS): the chapter's real name, and how many
                // chapters arrived since this book was last read.
                val chapters = getChaptersByMangaId.await(h.mangaId)
                val current = chapters.firstOrNull { it.id == h.chapterId }
                val lastRead = h.readAt?.time ?: 0L
                val sourceName = lib?.manga?.source?.let { sourceManager.getOrStub(it).name }
                HomeBook(
                    mangaId = h.mangaId,
                    chapterId = h.chapterId,
                    title = h.title,
                    cover = h.coverData,
                    chapterLabel = current?.name?.takeIf { it.isNotBlank() } ?: homeChapterLabel(h.chapterNumber),
                    progress = lib?.progress(),
                    unread = chapters.count { !it.read && it.dateFetch > lastRead },
                    isNovel = h.isNovel,
                    recentChapters = chapters.toRecent(lastRead, h.isNovel),
                    meta = listOfNotNull(
                        "${chapters.size} ch".takeIf {
                            chapters.isNotEmpty()
                        },
                        sourceName,
                    ).joinToString(" · "),
                )
            }

        // Fill the rest of Home with library books that aren't in recent history.
        val seen = recent.mapTo(HashSet()) { it.mangaId }
        val others = if (recent.size == MAX_BOOKS) {
            emptyList()
        } else {
            library
                .filter { it.id !in seen }
                .sortedWith(compareByDescending<LibraryManga> { it.lastRead }.thenByDescending { it.manga.dateAdded })
                .take((MAX_BOOKS - recent.size).coerceAtLeast(0))
                .map { lib ->
                    val chapters = getChaptersByMangaId.await(lib.id)
                    HomeBook(
                        mangaId = lib.id,
                        chapterId = null,
                        title = lib.manga.title,
                        cover = lib.manga.asMangaCover(),
                        chapterLabel = null,
                        progress = lib.progress(),
                        unread = 0,
                        isNovel = lib.manga.isNovel,
                        recentChapters = chapters.toRecent(Long.MAX_VALUE, lib.manga.isNovel),
                        meta = listOfNotNull(
                            "${chapters.size} ch".takeIf { chapters.isNotEmpty() },
                            sourceManager.getOrStub(lib.manga.source).name,
                        ).joinToString(" · "),
                    )
                }
        }
        val books = recent + others

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayMs = history
            .filter { (it.readAt?.time ?: 0L) >= startOfToday }
            .sumOf { it.readDuration }

        return HomeState(
            loading = false,
            books = books,
            fromHistory = recent.isNotEmpty(),
            libraryCount = library.size,
            todayMinutes = (todayMs / 60_000L).toInt(),
        )
    }

    private fun List<Chapter>.toRecent(lastRead: Long, isNovel: Boolean): List<HomeChapter> =
        sortedBy { it.sourceOrder } // source order 0 = newest chapter
            .take(5)
            .map { c ->
                val heading = if (isNovel) novelChapterHeading(c.name) else null
                HomeChapter(
                    id = c.id,
                    number = homeChapterNumber(heading?.number ?: c.chapterNumber),
                    name = heading?.title ?: c.name,
                    status = homeChapterStatus(c.read, c.lastPageRead, c.dateFetch, lastRead),
                )
            }

    private fun LibraryManga.progress(): Float? =
        if (totalChapters > 0) readCount.toFloat() / totalChapters else null

    companion object {
        private const val HISTORY_WINDOW = 500L
        private const val MAX_BOOKS = 9
    }
}

/** Keep badge changes independent of the chapter queries used to build the stack. */
internal fun homeStates(
    history: Flow<List<HistoryWithRelations>>,
    library: Flow<List<LibraryManga>>,
    updates: Flow<Int>,
    load: suspend (List<HistoryWithRelations>, List<LibraryManga>) -> HomeState,
): Flow<HomeState> = combine(history, library, ::Pair)
    .mapLatest { (historyRows, libraryRows) -> withIOContext { load(historyRows, libraryRows) } }
    .combine(updates) { home, count -> home.copy(newChapters = count) }
