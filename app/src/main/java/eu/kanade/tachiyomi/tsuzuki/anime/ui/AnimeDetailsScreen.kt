package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.components.MangaBottomActionMenu
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.anime.AnimeTrackingSheet
import eu.kanade.presentation.tsuzuki.anime.displayName
import eu.kanade.presentation.tsuzuki.selection.LocalSelectionEpisodes
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeEpisodes
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.animeEpisodeNumberOrder
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.tsuzuki.anime.episodesBeforeLastWatched
import eu.kanade.tachiyomi.tsuzuki.anime.excludedAnimeGroups
import eu.kanade.tachiyomi.tsuzuki.anime.filterAnimeGroups
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackProgressJob
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTracking
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.system.toShareIntent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.NumberFormat
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
internal fun AnimeDetailsScreen(
    entry: RegisteredAnimeSource,
    anime: SAnime,
    library: AnimeLibraryStore,
    resumeUrl: String?,
    onBack: () -> Unit,
    sharedCoverKey: String? = null,
    launchPlayer: (Episode, Boolean) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Seasons and duplicates open in place when these details are in place (AnimeDetailsRoute), else the Activity.
    val openDetails = eu.kanade.presentation.tsuzuki.anime.rememberAnimeDetailsOpener()
    val model = remember(entry, anime.url) { AnimeDetailsScreenModel(entry, anime, library, scope) }
    val migrate = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) scope.launch { model.reloadSaved() }
    }
    val details by model.state.collectAsState()
    val downloads = remember { Injekt.get<AnimeDownloads>() }
    val downloadQueue by downloads.state.collectAsState()
    val downloadStatuses = remember(downloadQueue.entries) {
        downloadQueue.entries.associate { it.episodeId to it.status }
    }
    val saved = details.saved
    val storedNotes by remember(library, saved?.id) {
        saved?.let { library.observeNotes(it.id) } ?: flowOf(null)
    }.collectAsState(null)
    val notes = storedNotes ?: saved?.notes.orEmpty()
    var editingNotes by rememberSaveable { mutableStateOf(false) }
    var notesSaveError by remember { mutableStateOf(false) }
    var showCover by rememberSaveable { mutableStateOf(false) }
    val excludedGroups = saved?.memo?.excludedAnimeGroups().orEmpty()
    val seasons by remember(saved?.id) {
        saved?.let { library.observeSeasons(it.id) } ?: flowOf(emptyList<Anime>())
    }.collectAsState(emptyList())
    val siblings by remember(saved?.parentId) {
        saved?.parentId?.let(library::observeSeasons) ?: flowOf(emptyList<Anime>())
    }.collectAsState(emptyList())
    val seasonChoices = seasons.ifEmpty { siblings.takeIf { it.size > 1 }.orEmpty() }
    val emptyEpisodes = remember { AnimeEpisodes(emptyList(), null) }
    // Null until the database has answered for this anime, so the "no episodes" line never shows while the episodes
    // are still loading (it flashed for a frame or two when an anime opened).
    val loadedEpisodes by remember(saved?.id) {
        saved?.let { library.observeEpisodes(it.id) } ?: flowOf(null)
    }.collectAsState(null)
    val episodeState = loadedEpisodes ?: emptyEpisodes
    val display = details.display
    val filtered = remember(episodeState, details.episodeDisplay, downloadStatuses) {
        details.episodeDisplay.apply(
            episodeState.items,
            downloadStatuses.filterValues { it == AnimeDownloadStatus.Saved }.keys,
        )
    }
    val ordered = remember(episodeState.items) { episodeState.items.sortedWith(animeEpisodeNumberOrder) }
    val shelves by remember { library.observeShelves() }.collectAsState(emptyList())
    var shelfSelection by remember { mutableStateOf<Set<Long>?>(null) }
    var duplicate by remember { mutableStateOf<AnimeDuplicateMatch?>(null) }
    var selection by rememberSaveable(anime.url) { mutableStateOf(longArrayOf()) }
    val selected = episodeState.items.filter { it.id in selection }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var showTracking by remember { mutableStateOf(false) }
    // The book action row's tracker count ("2 trackers", accent) on the Tracking tile.
    val tracking = remember { Injekt.get<AnimeTracking>() }
    val trackingCount by remember(saved?.id) {
        saved?.let { tracking.observe(it.id).map { tracks -> tracks.size } } ?: flowOf(0)
    }.collectAsState(0)
    var showFilter by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var externalUrl by remember(display.url) { mutableStateOf<String?>(null) }

    LaunchedEffect(model) { model.refresh() }
    LaunchedEffect(entry, display.url) {
        externalUrl = withContext(Dispatchers.IO) {
            runCatching {
                val source = entry.source as? AnimeHttpSource
                (source?.getAnimeUrl(display) ?: display.url).toHttpUrlOrNull()?.toString()
            }.getOrNull()
        }
    }

    fun mutate(onError: () -> Unit = { saveError = true }, block: suspend () -> Unit) {
        if (saving) return
        saving = true
        saveError = false
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Could not save anime details" }
                onError()
            } finally {
                saving = false
            }
        }
    }

    fun play(episode: Episode) {
        launchPlayer(episode, !Injekt.get<BasePreferences>().incognitoMode.get())
    }

    fun enqueue(episodes: List<Episode>) {
        downloads.enqueue(entry.source.id, entry.source.name, display.title, episodes)
    }

    fun toggleSelection(id: Long, fromLongPress: Boolean = false) {
        selection = toggleAnimeEpisodeSelection(filtered, selection, id, fromLongPress)
    }

    suspend fun syncWatchedProgress(changed: List<Episode>) {
        if (!Injekt.get<BasePreferences>().incognitoMode.get()) {
            changed.filter { it.episodeNumber.isFinite() }
                .maxByOrNull(Episode::episodeNumber)
                ?.let { AnimeTrackProgressJob.enqueue(context, it) }
        }
    }

    suspend fun markSeen(ids: Set<Long>, seen: Boolean) = syncWatchedProgress(library.setEpisodesSeen(ids, seen))

    fun markPrevious(episode: Episode) {
        val preceding = ordered.takeWhile { it.id != episode.id }.mapTo(mutableSetOf(), Episode::id)
        mutate {
            markSeen(preceding, true)
            selection = longArrayOf()
        }
    }

    var resumed by rememberSaveable(anime.url) { mutableStateOf(false) }
    LaunchedEffect(episodeState.items, resumeUrl) {
        if (resumeUrl == null || resumed) return@LaunchedEffect
        val requested = episodeState.items.firstOrNull { it.url == resumeUrl } ?: return@LaunchedEffect
        resumed = true
        play(requested)
    }
    BackHandler(enabled = selected.isNotEmpty()) { selection = longArrayOf() }
    if (showTracking && saved != null) {
        AnimeTrackingSheet(saved, onDismiss = { showTracking = false })
    }

    Scaffold(
        modifier = Modifier.tsuzukiBackground(tokens),
        containerColor = Color.Transparent,
        topBar = {
            AppBar(
                title = null,
                navigateUp = onBack,
                actions = {
                    IconButton(onClick = { showFilter = true }, enabled = saved != null && !saving) {
                        Icon(Icons.Outlined.FilterList, stringResource(MR.strings.action_filter))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, stringResource(MR.strings.label_more))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (saved?.favorite == true) {
                                TsuzukiMenuItem(
                                    text = stringResource(MR.strings.action_migrate),
                                    onClick = {
                                        showMenu = false
                                        migrate.launch(AnimeMigrationActivity.fromAnime(context, saved.id))
                                    },
                                )
                                TsuzukiMenuItem(
                                    text = stringResource(MR.strings.action_edit_notes),
                                    onClick = {
                                        showMenu = false
                                        editingNotes = true
                                    },
                                )
                                TsuzukiMenuItem(
                                    text = stringResource(MR.strings.tsz_anime_shelves),
                                    onClick = {
                                        showMenu = false
                                        scope.launch { shelfSelection = library.shelfIds(saved.id) }
                                    },
                                )
                            }
                            TsuzukiMenuItem(
                                text = stringResource(MR.strings.action_webview_refresh),
                                onClick = {
                                    showMenu = false
                                    model.refresh()
                                },
                            )
                            externalUrl?.let { url ->
                                TsuzukiMenuItem(
                                    text = stringResource(MR.strings.action_open_in_web_view),
                                    onClick = {
                                        showMenu = false
                                        context.startActivity(
                                            WebViewActivity.newIntent(context, url, title = display.title),
                                        )
                                    },
                                )
                                TsuzukiMenuItem(
                                    text = stringResource(MR.strings.action_share),
                                    onClick = {
                                        showMenu = false
                                        context.startActivity(url.toUri().toShareIntent(context, type = "text/plain"))
                                    },
                                )
                            }
                        }
                    }
                },
                actionModeCounter = selected.size,
                onCancelActionMode = { selection = longArrayOf() },
                actionModeActions = {
                    // AP6: as in the book chapter list, the top bar only selects; the actions are in the bottom bar.
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_all),
                                icon = Icons.Outlined.SelectAll,
                                onClick = { selection = filtered.map(Episode::id).toLongArray() },
                            ),
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_inverse),
                                icon = Icons.Outlined.FlipToBack,
                                onClick = {
                                    val chosen = selection.toSet()
                                    selection = filtered.map(Episode::id).filterNot { it in chosen }.toLongArray()
                                },
                            ),
                        ),
                    )
                },
            )
        },
        bottomBar = {
            // AP6: the shared selection bar of the book chapter list.
            val ids = selected.mapTo(mutableSetOf(), Episode::id)
            fun act(block: suspend () -> Unit) = mutate {
                block()
                selection = longArrayOf()
            }
            CompositionLocalProvider(LocalSelectionEpisodes provides true) {
                MangaBottomActionMenu(
                    visible = selected.isNotEmpty() && !saving,
                    modifier = Modifier.fillMaxWidth(),
                    onBookmarkClicked = { act { library.setEpisodesBookmarked(ids, true) } }
                        .takeIf { selected.any { !it.bookmark } },
                    onRemoveBookmarkClicked = { act { library.setEpisodesBookmarked(ids, false) } }
                        .takeIf { selected.isNotEmpty() && selected.all(Episode::bookmark) },
                    onMarkAsReadClicked = { act { markSeen(ids, true) } }
                        .takeIf { selected.any { !it.seen } },
                    onMarkAsUnreadClicked = { act { markSeen(ids, false) } }
                        .takeIf { selected.any { it.seen || it.lastSecondSeen > 0 } },
                    onMarkPreviousAsReadClicked = selected.singleOrNull()?.let { episode -> { markPrevious(episode) } },
                    onDownloadClicked = {
                        val chosen = ordered.filter { it.id in ids }
                        chosen.filter { downloadStatuses[it.id] == AnimeDownloadStatus.Failed }
                            .forEach { downloads.retry(it.id) }
                        enqueue(chosen.filter { downloadStatuses[it.id] == null })
                        selection = longArrayOf()
                    }.takeIf {
                        selected.any {
                            downloadStatuses[it.id] == null || downloadStatuses[it.id] == AnimeDownloadStatus.Failed
                        }
                    },
                    onDeleteClicked = {
                        selected.filter { downloadStatuses[it.id] == AnimeDownloadStatus.Saved }
                            .forEach { downloads.remove(it.id) }
                        selection = longArrayOf()
                    }.takeIf { selected.any { downloadStatuses[it.id] == AnimeDownloadStatus.Saved } },
                )
            }
        },
    ) { padding ->
        PullRefresh(
            refreshing = details.refreshing,
            enabled = selected.isEmpty(),
            onRefresh = model::refresh,
            indicatorPadding = padding,
        ) {
            fun LazyListScope.overview(expanded: Boolean) {
                // AP7: one item laid out like the book header (MangaScreen): info, action row, resume tile, then the
                // description with notes and genres, so the gaps between them are the book's, not the list spacing.
                item(key = "anime-overview") {
                    Column {
                        AnimeDetailsHeader(
                            entry.source,
                            display,
                            isTabletUi = expanded,
                            onCoverClick = { showCover = true }.takeIf { !display.thumbnail_url.isNullOrBlank() },
                            sharedCoverKey = sharedCoverKey,
                        )
                        if (seasonChoices.isNotEmpty()) {
                            val format = NumberFormat.getNumberInstance().apply { isGroupingUsed = false }
                            TsuzukiPillRow(
                                labels = seasonChoices.map { season ->
                                    season.seasonNumber.takeIf { it.isFinite() && it >= 0 }?.let {
                                        androidStringResource(R.string.tsz_anime_season_short, format.format(it))
                                    } ?: season.title
                                },
                                selected = seasonChoices.indexOfFirst { it.id == saved?.id },
                                onClick = { index ->
                                    val season = seasonChoices[index]
                                    if (season.id != saved?.id) openDetails(season.id)
                                },
                            )
                        }
                        val eligible = filtered.filter { episode ->
                            val status = downloadStatuses[episode.id]
                            status == null || status == AnimeDownloadStatus.Failed
                        }
                        AnimeLibraryActions(
                            trackingCount = trackingCount,
                            nextAiringDays = nextAiringDays(episodeState.items, System.currentTimeMillis()),
                            favorite = saved?.favorite == true,
                            enabled = !saving && selected.isEmpty(),
                            onFavorite = {
                                if (saved?.favorite == true) {
                                    mutate { model.setFavorite(false, emptySet()) }
                                } else {
                                    mutate {
                                        val id = saved?.id ?: library.cacheSearchResult(entry.source.id, display)
                                        val adding = checkNotNull(library.getAnime(id))
                                        val existing = library.findDuplicate(adding)
                                        if (existing == null) {
                                            shelfSelection = library.shelfIds(id)
                                        } else {
                                            duplicate = AnimeDuplicateMatch(existing, adding)
                                        }
                                    }
                                }
                            },
                            onTrack = if (saved != null) ({ showTracking = true }) else null,
                            // AP-LIB-F1: Save stays in the row (disabled) until the episodes load, no 3-to-4 jump.
                            onSave = if (animeSaveTile(saved != null, eligible.size) != AnimeSaveTile.Hidden) {
                                {
                                    selection = eligible.map(Episode::id).toLongArray()
                                }
                            } else {
                                null
                            },
                            saveEnabled = eligible.isNotEmpty(),
                            onExternal = externalUrl?.let { url ->
                                {
                                    context.startActivity(
                                        WebViewActivity.newIntent(context, url, title = display.title),
                                    )
                                }
                            },
                        )
                        episodeState.resume?.let { next ->
                            Box(Modifier.padding(vertical = 8.dp)) {
                                AnimeResumeTile(next, selected.isEmpty()) { play(next) }
                            }
                        }
                        AnimeDetailsDescription(
                            anime = display,
                            notes = notes,
                            defaultExpanded = expanded,
                            onEditNotes = { if (saved != null) editingNotes = true },
                        )
                    }
                }
                if (details.refreshFailed || saveError) {
                    item(key = "anime-detail-error") {
                        AnimeCatalogueStatus(stringResource(MR.strings.unknown_error), model::refresh)
                    }
                }
            }

            fun LazyListScope.episodes() {
                if (episodeState.items.isNotEmpty() || episodeState.groups.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TsuzukiSectionLabel(
                                text = stringResource(
                                    MR.strings.tsz_anime_episode_progress,
                                    episodeState.items.count(Episode::seen),
                                    episodeState.items.size,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            if (episodeState.groups.isNotEmpty()) {
                                TsuzukiPill(
                                    text = androidStringResource(R.string.tsz_anime_exclude_groups),
                                    selected = excludedGroups.isNotEmpty(),
                                    count = excludedGroups.size.takeIf { it > 0 },
                                    role = Role.Button,
                                    onClick = { if (!saving) showFilter = true },
                                )
                            }
                        }
                    }
                }
                if (loadedEpisodes != null && filtered.isEmpty() && seasons.isEmpty() && !details.refreshing &&
                    !details.refreshFailed
                ) {
                    item { AnimeCatalogueStatus(androidStringResource(R.string.tsz_anime_no_episodes)) }
                }
                items(filtered, key = Episode::id, contentType = { "anime_episode" }) { episode ->
                    val status = downloadStatuses[episode.id]
                    AnimeEpisodeSwipe(
                        episode = episode,
                        downloadStatus = status,
                        enabled = !saving && selected.isEmpty(),
                        onToggleSeen = { mutate { markSeen(setOf(episode.id), !episode.seen) } },
                        onToggleBookmark = {
                            mutate { library.setEpisodesBookmarked(setOf(episode.id), !episode.bookmark) }
                        },
                        onDownload = {
                            when (status) {
                                null -> enqueue(listOf(episode))
                                AnimeDownloadStatus.Failed -> downloads.retry(episode.id)
                                else -> downloads.remove(episode.id)
                            }
                        },
                    ) {
                        AnimeEpisodeTile(
                            source = entry.source,
                            episode = episode,
                            coverUrl = display.thumbnail_url,
                            showNumberOnly = details.episodeDisplay.showNumberOnly,
                            selected = episode.id in selection,
                            enabled = !saving,
                            onClick = { if (selected.isEmpty()) play(episode) else toggleSelection(episode.id) },
                            onSelect = { toggleSelection(episode.id, fromLongPress = true) },
                            onMarkSeen = { mutate { markSeen(setOf(episode.id), it) } },
                            onBookmark = { mutate { library.setEpisodesBookmarked(setOf(episode.id), it) } },
                            onMarkPrevious = { markPrevious(episode) },
                            downloadStatus = downloadStatuses[episode.id],
                            onDownload = { enqueue(listOf(episode)) },
                        )
                    }
                }
            }

            // AP7: the same tablet switch as book details (MangaScreen's isTabletUi), so both use two panes alike.
            Box(Modifier.fillMaxSize()) {
                if (isTabletUi()) {
                    // The book details split: start pane half the width, at most 450dp (MangaScreen's TwoPanelBox).
                    TwoPanelBox(
                        modifier = Modifier.padding(padding),
                        startContent = {
                            LazyColumn(
                                modifier = Modifier.fillMaxHeight(),
                                // The header brings its own 16dp top gap, as in the book pane.
                                contentPadding = PaddingValues(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                overview(expanded = true)
                            }
                        },
                        endContent = {
                            LazyColumn(
                                modifier = Modifier.fillMaxHeight(),
                                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                episodes()
                            }
                        },
                    )
                } else {
                    LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        overview(expanded = false)
                        episodes()
                    }
                }
            }
        }
    }
    duplicate?.let { match ->
        AnimeDuplicateDialog(
            existing = match.existing,
            adding = match.adding,
            addingSource = entry.source,
            onDismiss = { duplicate = null },
            onAdd = { mutate { shelfSelection = library.shelfIds(match.adding.id) } },
            onOpen = {
                duplicate = null
                openDetails(match.existing.id)
            },
            onMigrate = {
                migrate.launch(AnimeMigrationActivity.fromAnime(context, match.existing.id, match.adding.id))
            },
        )
    }
    shelfSelection?.let { initial ->
        AnimeShelfPicker(shelves, initial, onDismiss = { shelfSelection = null }) { selectedShelves ->
            shelfSelection = null
            mutate { model.setFavorite(true, selectedShelves) }
        }
    }
    display.thumbnail_url?.takeIf { showCover && it.isNotBlank() }?.let { url ->
        AnimeCoverDialog(entry.source, display.title, url) { showCover = false }
    }
    if (editingNotes && saved != null) {
        AnimeNotesDialog(
            initial = notes,
            saving = saving,
            saveError = notesSaveError,
            onDismiss = {
                notesSaveError = false
                editingNotes = false
            },
        ) { text ->
            notesSaveError = false
            mutate(onError = { notesSaveError = true }) {
                library.setNotes(saved.id, text)
                editingNotes = false
            }
        }
    }
    if (showFilter) {
        AnimeEpisodeFilterSheet(
            display = details.episodeDisplay,
            groups = episodeState.groups,
            excludedGroups = excludedGroups,
            onDismiss = { showFilter = false },
            // Same episodes the store marks: excluded subtitle groups are skipped.
            canMarkUntilLastWatched = saved != null && !saving &&
                episodesBeforeLastWatched(ordered.filterAnimeGroups(excludedGroups)).isNotEmpty(),
            onMarkUntilLastWatched = {
                if (saved != null) {
                    showFilter = false
                    mutate {
                        val changed = withContext(Dispatchers.IO) { library.markUntilLastWatched(saved.id) }
                        syncWatchedProgress(changed)
                    }
                }
            },
        ) { displayOptions, groups ->
            // Applies at once and stays open, as the book chapter dialog does.
            mutate { model.setEpisodeDisplay(displayOptions, groups) }
        }
    }
}

private data class AnimeDuplicateMatch(val existing: Anime, val adding: Anime)

@Composable
internal fun AnimeShelfPicker(
    shelves: List<AnimeCategory>,
    initial: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (Set<Long>) -> Unit,
) {
    var selection by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_shelves))) },
        text = {
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(shelves, key = { it.id }) { shelf ->
                    val selected = shelf.id in selection
                    Row(
                        modifier = Modifier.fillMaxWidth().toggleable(selected, role = Role.Checkbox) {
                            selection = when {
                                shelf.isSystem -> setOf(0L)
                                selected -> (selection - shelf.id).ifEmpty { setOf(0L) }
                                else -> (selection - 0L) + shelf.id
                            }
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Checkbox(selected, onCheckedChange = null)
                        Text(tsuzukiMenuLabel(shelf.displayName()))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selection) }) { Text(stringResource(MR.strings.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}

@Composable
internal fun AnimeCover(
    source: AnimeSource?,
    anime: SAnime,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String = "",
) {
    MangaCover.Book(
        rememberAnimeImage(source, anime.thumbnail_url),
        modifier = modifier,
        contentDescription = contentDescription,
        onClick = onClick,
        generatedTitle = anime.title,
    )
}

@Composable
internal fun rememberAnimeImage(source: AnimeSource?, url: String?): ImageRequest {
    val context = LocalContext.current
    return remember(context, source, url) {
        val httpSource = source as? AnimeHttpSource
        val urlHost = url?.toHttpUrlOrNull()?.host
        ImageRequest.Builder(context).data(url).apply {
            if (httpSource != null && urlHost != null && urlHost == httpSource.baseUrl.toHttpUrlOrNull()?.host) {
                val headers = NetworkHeaders.Builder().apply {
                    httpSource.headers.forEach { (name, value) -> add(name, value) }
                }.build()
                httpHeaders(headers)
            }
        }.build()
    }
}

/** Days until the earliest episode the source dates in the future (0 = within a day), or null when none is. */
internal fun nextAiringDays(episodes: List<Episode>, now: Long): Int? =
    episodes.asSequence().map { it.dateUpload }.filter { it > now }.minOrNull()
        ?.let { ((it - now) / java.util.concurrent.TimeUnit.DAYS.toMillis(1)).toInt() }
