package eu.kanade.presentation.tsuzuki.updates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.MangaBottomActionMenu
import eu.kanade.presentation.tsuzuki.selection.LocalSelectionEpisodes
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.ui.updates.UpdatesFilter
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/*
 * AP9-ALL-SELECT: in Updates › All the new-episode rows join the book selection. Books stay in the upstream
 * UpdatesViewModel; the episode ids live here, and the shared action bar sends each kind to its own helpers.
 */

/** Saved episode ids plus the "an anime action is running" flag (selection is frozen meanwhile). */
@Stable
internal class AnimeAllSelectionState(initial: Set<Long> = emptySet()) {
    var ids by mutableStateOf(initial)
    var applying by mutableStateOf(false)

    companion object {
        val Saver = Saver<AnimeAllSelectionState, LongArray>(
            save = { it.ids.toLongArray() },
            restore = { AnimeAllSelectionState(it.toSet()) },
        )
    }
}

@Composable
internal fun rememberAnimeAllSelectionState(): AnimeAllSelectionState =
    rememberSaveable(saver = AnimeAllSelectionState.Saver) { AnimeAllSelectionState() }

/** The new episodes that are rows of the list now: only the per-chapter list of All shows them. */
internal fun selectableAnimeInAll(
    filter: UpdatesFilter,
    groupByBook: Boolean,
    episodes: List<AnimeUpdateEpisode>,
): List<AnimeUpdateEpisode> = if (filter == UpdatesFilter.ALL && !groupByBook) episodes else emptyList()

/** Selected rows among the listed episodes, so ids of episodes that left the list never come back. */
internal fun selectedAnimeInAll(listed: List<AnimeUpdateEpisode>, ids: Set<Long>): List<AnimeUpdateEpisode> =
    if (ids.isEmpty()) emptyList() else listed.filter { it.episodeId in ids }

internal fun toggledAnimeSelection(ids: Set<Long>, episodeId: Long): Set<Long> =
    if (episodeId in ids) ids - episodeId else ids + episodeId

/** One composition's view of the All selection: the anime half plus whether books are selected too. */
@Stable
internal class AnimeAllSelection(
    private val state: AnimeAllSelectionState,
    val listed: List<AnimeUpdateEpisode>,
    booksSelecting: Boolean,
) {
    val selected: List<AnimeUpdateEpisode> = selectedAnimeInAll(listed, state.ids)
    val selectedIds: Set<Long> = selected.mapTo(linkedSetOf()) { it.episodeId }
    val active: Boolean get() = selected.isNotEmpty()

    /** True while either kind is selected: taps then toggle rows instead of opening them. */
    val selecting: Boolean = booksSelecting || selected.isNotEmpty()
    val applying: Boolean get() = state.applying

    fun isSelected(entry: AnimeUpdateEpisode) = entry.episodeId in selectedIds
    fun toggle(entry: AnimeUpdateEpisode) = set(toggledAnimeSelection(selectedIds, entry.episodeId))
    fun selectAll() = set(animeUpdatesSelectAll(listed))
    fun invert() = set(animeUpdatesInvert(listed, selectedIds))
    fun clear() = set(emptySet())

    private fun set(ids: Set<Long>) {
        if (!state.applying) state.ids = ids
    }

    /** Runs [block] on the selected episodes; clears them on success, keeps them and reports on failure. */
    fun runOnSelected(
        scope: kotlinx.coroutines.CoroutineScope,
        onFailure: () -> Unit,
        block: suspend (List<AnimeUpdateEpisode>) -> Unit,
    ) {
        if (state.applying || selected.isEmpty()) return
        val items = selected
        state.applying = true
        scope.launch {
            try {
                withIOContext { block(items) }
                state.ids = state.ids - items.mapTo(hashSetOf()) { it.episodeId }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AnimeLog.w("Could not update the selected new episodes in Updates", e)
                onFailure()
            } finally {
                state.applying = false
            }
        }
    }
}

/** Builds this composition's All selection and drops saved ids once no episode rows are listed. */
@Composable
internal fun animeAllSelection(
    state: AnimeAllSelectionState,
    filter: UpdatesFilter,
    groupByBook: Boolean,
    episodes: List<AnimeUpdateEpisode>,
    booksSelecting: Boolean,
): AnimeAllSelection {
    val listed = selectableAnimeInAll(filter, groupByBook, episodes)
    LaunchedEffect(listed.isEmpty()) {
        if (listed.isEmpty() && !state.applying) state.ids = emptySet()
    }
    return AnimeAllSelection(state, listed, booksSelecting)
}

/** True while books or listed episodes are selected in Updates. */
internal fun updatesSelecting(booksSelecting: Boolean, anime: AnimeUpdatesSlots?): Boolean =
    booksSelecting || anime?.allSelection?.active == true

/** Book count plus episode count, for the action-mode counter. */
internal fun updatesSelectedCount(books: Int, anime: AnimeUpdatesSlots?): Int =
    books + (anime?.allSelection?.selected?.size ?: 0)

/** Which bar actions a mixed selection offers: the book rules (upstream) and episode rules, over both kinds. */
internal fun mixedUpdateSelectionActions(
    books: List<UpdatesItem>,
    anime: List<AnimeUpdateEpisode>,
    status: Map<Long, AnimeDownloadStatus>,
): AnimeUpdateSelectionActions {
    val book = bookUpdateSelectionActions(books)
    val episode = animeUpdateSelectionActions(anime, status)
    return AnimeUpdateSelectionActions(
        bookmark = book.bookmark || episode.bookmark,
        // "All bookmarked" over the union: an empty half doesn't count, an unbookmarked row in either does.
        removeBookmark = (books.isNotEmpty() || anime.isNotEmpty()) &&
            (books.isEmpty() || book.removeBookmark) &&
            (anime.isEmpty() || episode.removeBookmark),
        markSeen = book.markSeen || episode.markSeen,
        markUnseen = book.markUnseen || episode.markUnseen,
        download = book.download || episode.download,
        delete = book.delete || episode.delete,
    )
}

/** The upstream Updates bar rules for books (UpdatesScreen's UpdatesBottomBar). */
internal fun bookUpdateSelectionActions(books: List<UpdatesItem>): AnimeUpdateSelectionActions =
    AnimeUpdateSelectionActions(
        bookmark = books.any { !it.update.bookmark },
        removeBookmark = books.isNotEmpty() && books.all { it.update.bookmark },
        markSeen = books.any { !it.update.read },
        markUnseen = books.any { it.update.read || it.update.lastPageRead > 0L },
        download = books.any { it.downloadStateProvider() != Download.State.DOWNLOADED },
        delete = books.any { it.downloadStateProvider() == Download.State.DOWNLOADED },
    )

/** Queues the selected episodes that aren't saved yet; retries failed ones. Throws if some can't be queued. */
internal suspend fun downloadAnimeSelection(
    store: AnimeLibraryStore,
    downloads: AnimeDownloads,
    items: List<AnimeUpdateEpisode>,
    status: Map<Long, AnimeDownloadStatus>,
) {
    items.filter { status[it.episodeId] == AnimeDownloadStatus.Failed }
        .forEach { downloads.retryAndAwait(it.episodeId) }
    val toQueue = items.filter { status[it.episodeId] == null }
    check(queueAnimeUpdates(store, downloads, toQueue)) { "Some selected anime episodes could not be queued" }
}

/** Removes the saved files of the selected episodes. */
internal suspend fun deleteAnimeSelection(
    downloads: AnimeDownloads,
    items: List<AnimeUpdateEpisode>,
    status: Map<Long, AnimeDownloadStatus>,
) {
    items.filter { status[it.episodeId] == AnimeDownloadStatus.Saved }
        .forEach { downloads.removeAndAwait(it.episodeId) }
}

/**
 * The Updates selection bar while anime is on: book actions go to the UpdatesViewModel callbacks, episode
 * actions to the anime store and downloads. A half the action doesn't apply to is just deselected.
 */
@Composable
internal fun TsuzukiMixedUpdatesBottomBar(
    books: List<UpdatesItem>,
    anime: AnimeAllSelection,
    snackbar: SnackbarHostState,
    onClearBooks: () -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { Injekt.get<AnimeLibraryStore>() }
    val downloads = remember { Injekt.get<AnimeDownloads>() }
    val queue by downloads.state.collectAsState()
    val status = remember(queue) { queue.entries.associate { it.episodeId to it.status } }
    val failureMessage = stringResource(MR.strings.internal_error)
    val track = !remember { Injekt.get<BasePreferences>() }.incognitoMode.get()

    val episodes = anime.selected
    val bookActions = bookUpdateSelectionActions(books)
    val animeActions = animeUpdateSelectionActions(episodes, status)
    val actions = mixedUpdateSelectionActions(books, episodes, status)

    fun act(
        forBooks: Boolean,
        forAnime: Boolean,
        bookAction: (List<UpdatesItem>) -> Unit,
        animeAction: suspend (List<AnimeUpdateEpisode>) -> Unit,
    ) {
        if (books.isNotEmpty()) {
            if (forBooks) bookAction(books) else onClearBooks()
        }
        if (episodes.isNotEmpty()) {
            if (forAnime) {
                anime.runOnSelected(scope, { scope.launch { snackbar.showSnackbar(failureMessage) } }, animeAction)
            } else {
                anime.clear()
            }
        }
    }

    // Episode wording (watched, not read) only when the selection is all episodes.
    CompositionLocalProvider(LocalSelectionEpisodes provides (books.isEmpty() && episodes.isNotEmpty())) {
        MangaBottomActionMenu(
            visible = books.isNotEmpty() || episodes.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onBookmarkClicked = {
                act(bookActions.bookmark, animeActions.bookmark, { onMultiBookmarkClicked(it, true) }) {
                    store.setEpisodesBookmarked(it.ids(), true)
                }
            }.takeIf { actions.bookmark },
            onRemoveBookmarkClicked = {
                act(bookActions.removeBookmark, animeActions.removeBookmark, { onMultiBookmarkClicked(it, false) }) {
                    store.setEpisodesBookmarked(it.ids(), false)
                }
            }.takeIf { actions.removeBookmark },
            onMarkAsReadClicked = {
                act(bookActions.markSeen, animeActions.markSeen, { onMultiMarkAsReadClicked(it, true) }) {
                    markAnimeUpdatesSeen(context, store, it.ids(), true, track)
                }
            }.takeIf { actions.markSeen },
            onMarkAsUnreadClicked = {
                act(bookActions.markUnseen, animeActions.markUnseen, { onMultiMarkAsReadClicked(it, false) }) {
                    markAnimeUpdatesSeen(context, store, it.ids(), false, track)
                }
            }.takeIf { actions.markUnseen },
            onDownloadClicked = {
                act(
                    bookActions.download,
                    animeActions.download,
                    { onDownloadChapter(it, ChapterDownloadAction.START) },
                ) { downloadAnimeSelection(store, downloads, it, status) }
            }.takeIf { actions.download },
            onDeleteClicked = {
                act(bookActions.delete, animeActions.delete, onMultiDeleteClicked) {
                    deleteAnimeSelection(downloads, it, status)
                }
            }.takeIf { actions.delete },
        )
    }
}

private fun List<AnimeUpdateEpisode>.ids(): Set<Long> = mapTo(hashSetOf()) { it.episodeId }
