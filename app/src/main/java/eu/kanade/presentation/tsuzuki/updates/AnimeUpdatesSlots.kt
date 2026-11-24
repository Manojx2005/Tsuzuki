package eu.kanade.presentation.tsuzuki.updates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.manga.components.MangaBottomActionMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.tsuzuki.anime.MixedTimelineRow
import eu.kanade.presentation.tsuzuki.selection.LocalSelectionEpisodes
import eu.kanade.presentation.tsuzuki.selection.TsuzukiUndoSnackbarHost
import eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle
import eu.kanade.presentation.updates.UpdatesUiModel
import eu.kanade.presentation.updates.updatesNovelGroups
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.ui.updates.UpdatesFilter
import eu.kanade.tachiyomi.ui.updates.UpdatesViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Clock

internal class AnimeUpdatesSlots(
    val mediaPills: (@Composable (twoPane: Boolean) -> Unit)?,
    val airingStrip: (@Composable () -> Unit)?,
    val screen: (@Composable (SnackbarHostState) -> Unit)?,
    /** New episodes of favourite anime (last 3 months), for Updates › All (AY-INT item 2). */
    val newEpisodes: List<AnimeUpdateEpisode> = emptyList(),
    /** Unwatched episodes not in the download queue yet; what "Save all" adds in All. */
    val episodesToSave: List<AnimeUpdateEpisode> = emptyList(),
    val saveEpisodes: () -> Unit = {},
    /** AP9-ALL-SELECT: the new-episode rows selected in Updates › All, next to the book selection. */
    val allSelection: AnimeAllSelection? = null,
)

/** True when Updates › All will list new episodes (per-chapter list only; grouped mode has no anime rows yet). */
@Suppress("UNUSED_PARAMETER") // Both list modes show anime now; kept so the call sites read the same.
internal fun hasAnimeInAll(anime: AnimeUpdatesSlots?, filter: UpdatesFilter, groupByBook: Boolean): Boolean =
    filter == UpdatesFilter.ALL && !anime?.newEpisodes.isNullOrEmpty()

/** Anime groups for grouped Updates › All, or none on the other filters. */
internal fun animeGroupsForAll(anime: AnimeUpdatesSlots?, filter: UpdatesFilter): List<AnimeUpdateGroup> =
    if (anime == null || filter != UpdatesFilter.ALL) emptyList() else animeUpdateGroups(anime.newEpisodes)

/** "2 new episodes · Episode 3". */
@Composable
internal fun animeGroupSubtitle(group: AnimeUpdateGroup): String = listOf(
    androidx.compose.ui.res.pluralStringResource(
        eu.kanade.tachiyomi.R.plurals.tsz_anime_new_episodes,
        group.count,
        group.count,
    ),
    group.latestName,
).joinToString(" · ")

/** Grouped Updates on the phone: book groups, plus anime groups in All, newest first. */
internal fun androidx.compose.foundation.lazy.LazyListScope.tsuzukiUpdateGroups(
    groups: List<eu.kanade.tachiyomi.ui.updates.UpdatesNovelGroup>,
    anime: AnimeUpdatesSlots?,
    filter: UpdatesFilter,
    onClickGroup: (Long) -> Unit,
) {
    val animeGroups = animeGroupsForAll(anime, filter)
    if (animeGroups.isEmpty()) {
        updatesNovelGroups(groups, onClickGroup)
        return
    }
    val rows = mergeUpdateGroups(groups, { it.latestChapterDate }, animeGroups)
    items(
        rows,
        key = {
            when (it) {
                is MixedTimelineRow.Book -> "novel-group-${it.item.mangaId}"
                is MixedTimelineRow.Anime -> "anime-group-${it.item.animeId}"
            }
        },
    ) { row ->
        when (row) {
            is MixedTimelineRow.Book ->
                eu.kanade.presentation.tsuzuki.novel.TsuzukiUpdatesGroupTile(row.item, {
                    onClickGroup(row.item.mangaId)
                })
            is MixedTimelineRow.Anime -> TsuzukiAnimeUpdateGroupEntry(row.item)
        }
    }
}

/** The chapter list for Updates, with new episodes folded in when All is selected and anime is on. */
internal fun withAnimeUpdates(
    chapters: List<UpdatesUiModel>,
    anime: AnimeUpdatesSlots?,
    filter: UpdatesFilter,
): List<UpdatesUiModel> = if (anime == null || filter != UpdatesFilter.ALL) {
    chapters
} else {
    mergeAnimeIntoUpdates(chapters, anime.newEpisodes, TimeZone.currentSystemDefault())
}

@Composable
internal fun rememberAnimeUpdatesSlots(
    state: UpdatesViewModel.State,
    hideManga: Boolean,
    onFilterSelected: (UpdatesFilter) -> Unit,
): AnimeUpdatesSlots? {
    if (!TsuzukiAnime.enabled || !isTsuzukiStyle()) return null
    val store = remember { Injekt.get<AnimeLibraryStore>() }
    val hasAnime by remember(store) { store.observeHasFavorites() }.collectAsState(null)
    var animeFilter by rememberSaveable { mutableStateOf(false) }
    val animeSelected = hasAnime == true && animeFilter
    BackHandler(enabled = animeSelected) { animeFilter = false }
    LaunchedEffect(hasAnime) {
        if (hasAnime == false) animeFilter = false
    }
    if (hasAnime != true) return null

    val airing = rememberAnimeAiringToday(store)
    val since = remember {
        Clock.System.now().minus(3, DateTimeUnit.MONTH, TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
    val newEpisodes by remember(store, since) { store.observeNewEpisodes(since) }.collectAsState(emptyList())
    val downloads = remember { Injekt.get<AnimeDownloads>() }
    val queue by downloads.state.collectAsState()
    val toSave = remember(newEpisodes, queue) {
        episodesToSave(newEpisodes, queue.entries.mapTo(hashSetOf()) { it.episodeId })
    }
    val scope = rememberCoroutineScope()
    val allSelection = animeAllSelection(
        rememberAnimeAllSelectionState(),
        state.filter,
        state.groupByNovel,
        if (animeSelected) emptyList() else newEpisodes,
        state.selectionMode,
    )
    val pills: @Composable (Boolean) -> Unit = { twoPane ->
        AnimeUpdatesPills(
            filter = state.filter,
            hideManga = hideManga,
            animeSelected = animeSelected,
            twoPane = twoPane,
            onBookFilterSelected = {
                animeFilter = false
                onFilterSelected(it)
            },
            onAnimeSelected = { animeFilter = true },
        )
    }
    return AnimeUpdatesSlots(
        mediaPills = pills.takeUnless { allSelection.selecting },
        airingStrip = airing?.let { entry -> { AnimeAiringStrip(entry, Modifier.padding(horizontal = 16.dp)) } },
        screen = if (animeSelected) {
            { snackbar -> AnimeUpdatesScreen(store, newEpisodes, toSave, snackbar, pills, airing) }
        } else {
            null
        },
        newEpisodes = newEpisodes,
        episodesToSave = toSave,
        saveEpisodes = { scope.launchIO { queueAnimeUpdates(store, downloads, toSave, unwatchedOnly = true) } },
        allSelection = allSelection,
    )
}

/** "New" and "to save" counts plus Save all for the Updates tiles, with episodes added in All. */
internal fun updatesTileCounts(
    unread: Int,
    chaptersToSave: Int,
    anime: AnimeUpdatesSlots?,
    filter: UpdatesFilter,
): Pair<Int, Int> = if (anime == null || filter != UpdatesFilter.ALL) {
    unread to chaptersToSave
} else {
    (unread + anime.newEpisodes.count { !it.seen }) to (chaptersToSave + anime.episodesToSave.size)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimeUpdatesPills(
    filter: UpdatesFilter,
    hideManga: Boolean,
    animeSelected: Boolean,
    twoPane: Boolean,
    onBookFilterSelected: (UpdatesFilter) -> Unit,
    onAnimeSelected: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (twoPane) 4.dp else 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!hideManga) {
            TsuzukiPill(
                text = stringResource(MR.strings.all),
                selected = !animeSelected && filter == UpdatesFilter.ALL,
                onClick = { onBookFilterSelected(UpdatesFilter.ALL) },
            )
            TsuzukiPill(
                text = stringResource(TDMR.strings.label_manga),
                selected = !animeSelected && filter == UpdatesFilter.MANGA,
                onClick = { onBookFilterSelected(UpdatesFilter.MANGA) },
            )
        }
        TsuzukiPill(
            text = stringResource(TDMR.strings.label_novels),
            selected = !animeSelected && filter == UpdatesFilter.NOVELS,
            onClick = { onBookFilterSelected(UpdatesFilter.NOVELS) },
        )
        TsuzukiPill(
            text = stringResource(MR.strings.tsz_anime_media),
            selected = animeSelected,
            onClick = onAnimeSelected,
        )
    }
}

@Composable
private fun AnimeUpdatesScreen(
    store: AnimeLibraryStore,
    entries: List<AnimeUpdateEpisode>,
    toSave: List<AnimeUpdateEpisode>,
    snackbar: SnackbarHostState,
    pills: @Composable (twoPane: Boolean) -> Unit,
    airing: AnimeUpdateEpisode?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloads = remember { Injekt.get<AnimeDownloads>() }
    val queue by downloads.state.collectAsState()
    val status = remember(queue) { queue.entries.associate { it.episodeId to it.status } }
    var selectedIds by rememberSaveable { mutableStateOf(longArrayOf()) }
    var applyingSelection by remember { mutableStateOf(false) }
    val selected = remember(entries, selectedIds) {
        val ids = selectedIds.toSet()
        entries.filter { it.episodeId in ids }
    }
    val selectedSet = remember(selected) { selected.ids() }
    fun select(ids: Set<Long>) {
        if (!applyingSelection) selectedIds = ids.toLongArray()
    }
    BackHandler(enabled = selected.isNotEmpty()) { select(emptySet()) }
    // As in book Updates: the phone's bottom nav makes room for the selection bar.
    val selecting = selected.isNotEmpty()
    LaunchedEffect(selecting) { eu.kanade.tachiyomi.ui.home.HomeScreen.showBottomNav(!selecting) }
    val failureMessage = stringResource(MR.strings.internal_error)
    fun runOnSelection(block: suspend (List<AnimeUpdateEpisode>) -> Unit) {
        if (applyingSelection) return
        val items = selected
        applyingSelection = true
        scope.launch {
            try {
                withIOContext { block(items) }
                selectedIds = longArrayOf()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AnimeLog.w("Could not update selected anime episodes", e)
                scope.launch { snackbar.showSnackbar(failureMessage) }
            } finally {
                applyingSelection = false
            }
        }
    }
    val actions = animeUpdateSelectionActions(selected, status)
    val track = !Injekt.get<BasePreferences>().incognitoMode.get()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_recent_updates),
                scrollBehavior = scrollBehavior,
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_update_library),
                                icon = Icons.Outlined.Refresh,
                                onClick = { AnimeLibraryUpdateJob.start(context) },
                            ),
                        ),
                    )
                },
                actionModeCounter = selected.size,
                onCancelActionMode = { select(emptySet()) },
                actionModeActions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_all),
                                icon = Icons.Outlined.SelectAll,
                                onClick = { select(animeUpdatesSelectAll(entries)) },
                                enabled = !applyingSelection,
                            ),
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_inverse),
                                icon = Icons.Outlined.FlipToBack,
                                onClick = { select(animeUpdatesInvert(entries, selectedSet)) },
                                enabled = !applyingSelection,
                            ),
                        ),
                    )
                },
            )
        },
        bottomBar = {
            CompositionLocalProvider(LocalSelectionEpisodes provides true) {
                MangaBottomActionMenu(
                    visible = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onBookmarkClicked = {
                        runOnSelection { items -> store.setEpisodesBookmarked(items.ids(), true) }
                    }.takeIf { actions.bookmark },
                    onRemoveBookmarkClicked = {
                        runOnSelection { items -> store.setEpisodesBookmarked(items.ids(), false) }
                    }.takeIf { actions.removeBookmark },
                    onMarkAsReadClicked = {
                        runOnSelection { items -> markAnimeUpdatesSeen(context, store, items.ids(), true, track) }
                    }.takeIf { actions.markSeen },
                    onMarkAsUnreadClicked = {
                        runOnSelection { items -> markAnimeUpdatesSeen(context, store, items.ids(), false, track) }
                    }.takeIf { actions.markUnseen },
                    onDownloadClicked = {
                        runOnSelection { items -> downloadAnimeSelection(store, downloads, items, status) }
                    }.takeIf { actions.download },
                    onDeleteClicked = {
                        runOnSelection { items -> deleteAnimeSelection(downloads, items, status) }
                    }.takeIf { actions.delete },
                )
            }
        },
        snackbarHost = { TsuzukiUndoSnackbarHost(snackbar) },
    ) { padding ->
        TsuzukiAnimeUpdates(
            entries = entries,
            padding = padding,
            pills = pills,
            airing = airing,
            selected = selectedSet,
            downloadStatus = status,
            toSave = toSave.size,
            onSaveAll = { scope.launchIO { queueAnimeUpdates(store, downloads, toSave, unwatchedOnly = true) } },
            onToggleSelection = { entry ->
                select(
                    if (entry.episodeId in
                        selectedSet
                    ) {
                        selectedSet - entry.episodeId
                    } else {
                        selectedSet + entry.episodeId
                    },
                )
            },
            onDownload = { entry ->
                if (status[entry.episodeId] == AnimeDownloadStatus.Failed) {
                    downloads.retry(entry.episodeId)
                } else {
                    scope.launchIO { queueAnimeUpdates(store, downloads, listOf(entry)) }
                }
            },
            onMarkSeen = { entry, seen ->
                scope.launch {
                    try {
                        withIOContext { markAnimeUpdatesSeen(context, store, setOf(entry.episodeId), seen, track) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AnimeLog.w("Could not update the previewed anime episode", e)
                        snackbar.showSnackbar(failureMessage)
                    }
                }
            },
        )
    }
}

private fun List<AnimeUpdateEpisode>.ids(): Set<Long> = mapTo(hashSetOf()) { it.episodeId }
