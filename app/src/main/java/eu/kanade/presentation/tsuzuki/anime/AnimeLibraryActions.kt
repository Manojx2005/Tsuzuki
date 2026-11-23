package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.SwapCalls
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.selection.SelectionAction
import eu.kanade.presentation.tsuzuki.selection.TsuzukiSelectionBar
import eu.kanade.presentation.tsuzuki.selection.awaitTsuzukiUndo
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.animeEpisodeNumberOrder
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.tsuzuki.anime.excludedAnimeGroups
import eu.kanade.tachiyomi.tsuzuki.anime.filterAnimeGroups
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackProgressJob
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimeMigrationActivity
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimeShelfPicker
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.res.stringResource as androidStringResource
import tachiyomi.core.common.i18n.stringResource as contextStringResource

internal enum class AnimeLibraryDownload(val count: Int?, val bookAction: DownloadAction?) {
    NextOne(1, DownloadAction.NEXT_1_CHAPTER),
    NextFive(5, DownloadAction.NEXT_5_CHAPTERS),
    NextTen(10, DownloadAction.NEXT_10_CHAPTERS),
    Unwatched(null, DownloadAction.UNREAD_CHAPTERS),
    All(null, null),
}

internal fun episodesToDownload(
    episodes: List<Episode>,
    choice: AnimeLibraryDownload,
    unavailable: Set<Long>,
): List<Episode> {
    val eligible = episodes.asSequence()
        .filter { it.id !in unavailable && (choice == AnimeLibraryDownload.All || !it.seen) }
        .sortedWith(animeEpisodeNumberOrder)
    return (choice.count?.let(eligible::take) ?: eligible).toList()
}

internal data class BookLibrarySelection(
    val model: LibraryViewModel,
    val entries: List<Manga>,
    val downloadable: Boolean,
)

/** Library actions and the Undo prompt survive switches between Home tabs. */
internal class AnimeLibraryActionModel : ViewModel() {
    val snackbar = SnackbarHostState()
}

internal suspend fun changeLibraryFavorites(
    favorite: Boolean,
    setBooks: (suspend (Boolean) -> Boolean)?,
    setAnime: suspend (Boolean) -> Unit,
) = withNonCancellableContext {
    check(setBooks?.invoke(favorite) != false)
    try {
        setAnime(favorite)
    } catch (error: Exception) {
        try {
            check(setBooks?.invoke(!favorite) != false)
        } catch (rollbackError: Exception) {
            if (rollbackError !== error) error.addSuppressed(rollbackError)
        }
        throw error
    }
}

internal class AnimeLibraryActions {
    var picker by mutableStateOf<ShelfSelection?>(null)
    var deleteConfirmation: (() -> Unit)? by mutableStateOf(null)

    data class ShelfSelection(
        val shelves: List<AnimeCategory>,
        val initial: Set<Long>,
        val onSave: (Set<Long>) -> Unit,
    )
}

@Composable
internal fun AnimeLibraryActionDialogs(actions: AnimeLibraryActions) {
    actions.picker?.let { picker ->
        AnimeShelfPicker(picker.shelves, picker.initial, onDismiss = { actions.picker = null }) {
            actions.picker = null
            picker.onSave(it)
        }
    }
    actions.deleteConfirmation?.let { delete ->
        AlertDialog(
            onDismissRequest = { actions.deleteConfirmation = null },
            title = { Text(stringResource(MR.strings.delete_downloaded)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.deleteConfirmation = null
                    delete()
                }) { Text(stringResource(MR.strings.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { actions.deleteConfirmation = null }) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
        )
    }
}

@Composable
internal fun AnimeLibraryActionBar(
    selected: List<Anime>,
    actions: AnimeLibraryActions,
    actionModel: AnimeLibraryActionModel,
    snackbar: SnackbarHostState,
    clear: () -> Unit,
    books: BookLibrarySelection? = null,
) {
    val context = LocalContext.current
    val application = context.applicationContext
    val dialogScope = rememberCoroutineScope()
    val library = remember { Injekt.get<AnimeLibraryStore>() }
    val downloads = remember { Injekt.get<AnimeDownloads>() }
    val ids = selected.mapTo(linkedSetOf(), Anime::id)
    val markLabel = if (books == null) R.string.tsz_anime_mark_seen else R.string.tsz_library_mark_finished
    val unmarkLabel = if (books == null) R.string.tsz_anime_mark_unseen else R.string.tsz_library_mark_unfinished

    fun launchAction(scope: CoroutineScope = actionModel.viewModelScope, action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AnimeLog.w("Anime library selection action failed", error)
                snackbar.showSnackbar(application.contextStringResource(MR.strings.internal_error))
            }
        }
    }

    fun mark(seen: Boolean) {
        clear()
        launchAction {
            val pending = if (seen) markLabel else unmarkLabel
            if (snackbar.awaitTsuzukiUndo(application, application.getString(pending))) return@launchAction
            val episodeIds = buildSet {
                for (entry in selected) {
                    library.getEpisodes(entry.id).filterAnimeGroups(entry.memo.excludedAnimeGroups())
                        .mapTo(this, Episode::id)
                }
            }
            val changed = withNonCancellableContext {
                books?.model?.applyReadSelection(books.entries, seen)
                library.setEpisodesSeen(episodeIds, seen)
            }
            if (seen && !Injekt.get<BasePreferences>().incognitoMode.get()) {
                changed.filter { it.episodeNumber.isFinite() }.groupBy(Episode::animeId).values.forEach { episodes ->
                    episodes.maxByOrNull(Episode::episodeNumber)?.let { AnimeTrackProgressJob.enqueue(application, it) }
                }
            }
        }
    }

    fun download(choice: AnimeLibraryDownload) {
        choice.bookAction?.let { books?.model?.performDownloadAction(it) }
        clear()
        launchAction {
            val manager = Injekt.get<AnimeSourceManager>()
            val sources = manager.ensureLoaded().associateBy { it.source.id }
            for (entry in selected) {
                val source = sources[entry.source]?.source ?: continue
                val unavailable = downloads.state.value.entries.asSequence()
                    .filter { it.animeId == entry.id && it.status != AnimeDownloadStatus.Failed }
                    .mapTo(hashSetOf()) { it.episodeId }
                val available = library.getEpisodes(entry.id).filterAnimeGroups(entry.memo.excludedAnimeGroups())
                val episodes = episodesToDownload(available, choice, unavailable)
                downloads.enqueue(entry.source, source.name, entry.title, episodes)
            }
            selected.firstOrNull { it.source !in sources }?.let {
                val name = manager.stub(it.source)?.name ?: it.source.toString()
                snackbar.showSnackbar(application.contextStringResource(MR.strings.source_not_installed, name))
            }
        }
    }

    val primary = listOfNotNull(
        if (books == null) {
            SelectionAction(
                title = stringResource(MR.strings.action_move_category),
                icon = Icons.AutoMirrored.Outlined.Label,
                shortTitle = stringResource(MR.strings.tsz_q9_shelf),
                onClick = {
                    launchAction(dialogScope) {
                        val shared = selected.map<Anime, Set<Long>> { library.shelfIds(it.id) }.reduce { left, right ->
                            left intersect
                                right
                        }
                        actions.picker = AnimeLibraryActions.ShelfSelection(library.getShelves(), shared) { shelves ->
                            clear()
                            launchAction { library.moveLibraryAnime(ids, shelves) }
                        }
                    }
                },
            )
        } else {
            null
        },
        SelectionAction(
            androidStringResource(markLabel),
            Icons.Outlined.DoneAll,
            { mark(true) },
            shortTitle = androidStringResource(
                if (books == null) R.string.tsz_anime_mark_watched_short else R.string.tsz_library_mark_finished,
            ),
        ),
        if (books?.downloadable != false) {
            SelectionAction(
                title = stringResource(MR.strings.action_save),
                icon = Icons.Outlined.Download,
                menu = { expanded, dismiss ->
                    DropdownMenu(expanded = expanded, onDismissRequest = dismiss) {
                        AnimeLibraryDownload.entries.filter {
                            books == null || it.bookAction != null
                        }.forEach { choice ->
                            val title = when (choice) {
                                AnimeLibraryDownload.Unwatched -> androidStringResource(
                                    if (books == null) {
                                        R.string.tsz_anime_unwatched
                                    } else {
                                        R.string.tsz_selection_download_incomplete
                                    },
                                )
                                AnimeLibraryDownload.All -> stringResource(MR.strings.tsz_download_all)
                                else -> androidStringResource(
                                    R.string.tsz_selection_download_next,
                                    requireNotNull(choice.count),
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(tsuzukiMenuLabel(title)) },
                                onClick = {
                                    dismiss()
                                    download(choice)
                                },
                            )
                        }
                    }
                },
            )
        } else {
            null
        },
        SelectionAction(stringResource(MR.strings.action_remove), Icons.Outlined.Delete, onClick = {
            clear()
            launchAction {
                val setBooks: (suspend (Boolean) -> Boolean)? = if (books == null) null else { favorite ->
                    books.model.setLibraryFavorites(books.entries, favorite)
                }
                changeLibraryFavorites(false, setBooks) { library.setLibraryFavorites(ids, it) }
                val message = application.contextStringResource(MR.strings.manga_removed_library)
                if (snackbar.awaitTsuzukiUndo(application, message)) {
                    changeLibraryFavorites(true, setBooks) { library.setLibraryFavorites(ids, it) }
                }
            }
        }),
    )
    val additional = listOfNotNull(
        SelectionAction(androidStringResource(unmarkLabel), Icons.Outlined.RemoveDone, { mark(false) }),
        if (books?.downloadable != false) {
            SelectionAction(stringResource(MR.strings.delete_downloaded), Icons.Outlined.Delete, onClick = {
                actions.deleteConfirmation = {
                    clear()
                    books?.model?.removeMangas(books.entries, deleteFromLibrary = false, deleteChapters = true)
                    val saved = downloads.state.value.entries.filter {
                        it.animeId in ids && it.status == AnimeDownloadStatus.Saved
                    }
                    saved.forEach { downloads.remove(it.episodeId) }
                }
            })
        } else {
            null
        },
        if (books == null) {
            SelectionAction(stringResource(MR.strings.migrate), Icons.Outlined.SwapCalls, onClick = {
                clear()
                context.startActivity(AnimeMigrationActivity.fromSelection(context, ids))
            })
        } else {
            null
        },
    )
    TsuzukiSelectionBar(primary, additionalActions = additional)
}
