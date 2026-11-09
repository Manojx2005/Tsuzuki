package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.anime.AnimeTrackingSheet
import eu.kanade.presentation.tsuzuki.anime.displayName
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
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackProgressJob
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.system.toShareIntent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
internal fun AnimeDetailsScreen(
    entry: RegisteredAnimeSource,
    anime: SAnime,
    library: AnimeLibraryStore,
    resumeUrl: String?,
    onBack: () -> Unit,
    launchPlayer: (Episode, Boolean) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val model = remember(entry, anime.url) { AnimeDetailsScreenModel(entry, anime, library, scope) }
    val details by model.state.collectAsState()
    val downloads = remember { Injekt.get<AnimeDownloads>() }
    val downloadQueue by downloads.state.collectAsState()
    val downloadStatuses = remember(downloadQueue.entries) {
        downloadQueue.entries.associate { it.episodeId to it.status }
    }
    val saved = details.saved
    val emptyEpisodes = remember { AnimeEpisodes(emptyList(), null) }
    val episodeState by remember(saved?.id) {
        saved?.let { library.observeEpisodes(it.id) } ?: flowOf(emptyEpisodes)
    }.collectAsState(emptyEpisodes)
    val display = details.display
    val filtered = remember(episodeState, details.episodeDisplay) {
        details.episodeDisplay.apply(episodeState.items)
    }
    val ordered = remember(episodeState.items) { episodeState.items.sortedWith(animeEpisodeNumberOrder) }
    val shelves by remember { library.observeShelves() }.collectAsState(emptyList())
    var shelfSelection by remember { mutableStateOf<Set<Long>?>(null) }
    var selection by rememberSaveable(anime.url) { mutableStateOf(longArrayOf()) }
    val selected = episodeState.items.filter { it.id in selection }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var showTracking by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }
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

    fun mutate(block: suspend () -> Unit) {
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
                saveError = true
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

    fun toggleSelection(id: Long) {
        selection = if (id in selection) selection.filterNot { it == id }.toLongArray() else selection + id
    }

    suspend fun markSeen(ids: Set<Long>, seen: Boolean) {
        val changed = library.setEpisodesSeen(ids, seen)
        if (!Injekt.get<BasePreferences>().incognitoMode.get()) {
            changed.filter { it.episodeNumber.isFinite() }
                .maxByOrNull(Episode::episodeNumber)
                ?.let { AnimeTrackProgressJob.enqueue(context, it) }
        }
    }

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
                    Box {
                        IconButton(onClick = { showSelectionMenu = true }, enabled = !saving) {
                            Icon(Icons.Outlined.MoreVert, stringResource(MR.strings.label_more))
                        }
                        DropdownMenu(expanded = showSelectionMenu, onDismissRequest = { showSelectionMenu = false }) {
                            val ids = selected.mapTo(mutableSetOf(), Episode::id)
                            val single = selected.singleOrNull()
                            AnimeEpisodeActions(
                                seen = selected.all(Episode::seen),
                                bookmarked = selected.all(Episode::bookmark),
                                onMarkSeen = { seen ->
                                    showSelectionMenu = false
                                    mutate {
                                        markSeen(ids, seen)
                                        selection = longArrayOf()
                                    }
                                },
                                onBookmark = { bookmarked ->
                                    showSelectionMenu = false
                                    mutate {
                                        library.setEpisodesBookmarked(ids, bookmarked)
                                        selection = longArrayOf()
                                    }
                                },
                                onMarkPrevious = single?.let { episode ->
                                    {
                                        showSelectionMenu = false
                                        markPrevious(episode)
                                    }
                                },
                            )
                            TsuzukiMenuItem(
                                text = stringResource(MR.strings.action_save),
                                onClick = {
                                    showSelectionMenu = false
                                    enqueue(ordered.filter { it.id in ids })
                                    selection = longArrayOf()
                                },
                            )
                            TsuzukiMenuItem(
                                text = stringResource(MR.strings.action_select_all),
                                onClick = {
                                    showSelectionMenu = false
                                    selection = filtered.map(Episode::id).toLongArray()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        PullRefresh(
            refreshing = details.refreshing,
            enabled = selected.isEmpty(),
            onRefresh = model::refresh,
            indicatorPadding = padding,
        ) {
            fun LazyListScope.overview(expanded: Boolean) {
                item(key = "anime-overview") {
                    AnimeDetailsHeader(entry.source, display, coverWidth = if (expanded) 156.dp else 104.dp)
                }
                item(key = "anime-actions") {
                    val eligible = filtered.filter { episode ->
                        val status = downloadStatuses[episode.id]
                        status == null || status == AnimeDownloadStatus.Failed
                    }
                    AnimeLibraryActions(
                        favorite = saved?.favorite == true,
                        enabled = !saving && selected.isEmpty(),
                        onFavorite = {
                            if (saved?.favorite == true) {
                                mutate { model.setFavorite(false, emptySet()) }
                            } else {
                                scope.launch {
                                    shelfSelection = saved?.let { library.shelfIds(it.id) } ?: setOf(0L)
                                }
                            }
                        },
                        onTrack = if (saved != null) ({ showTracking = true }) else null,
                        onSave = if (eligible.isNotEmpty()) {
                            {
                                selection = eligible.map(Episode::id).toLongArray()
                                showSelectionMenu = true
                            }
                        } else {
                            null
                        },
                        onExternal = externalUrl?.let { url ->
                            { context.startActivity(WebViewActivity.newIntent(context, url, title = display.title)) }
                        },
                    )
                }
                if (details.refreshFailed || saveError) {
                    item(key = "anime-detail-error") {
                        AnimeCatalogueStatus(stringResource(MR.strings.unknown_error), model::refresh)
                    }
                }
            }

            fun LazyListScope.episodes() {
                episodeState.resume?.let { next ->
                    item { AnimeResumeTile(next, selected.isEmpty()) { play(next) } }
                }
                if (episodeState.items.isNotEmpty()) {
                    item {
                        TsuzukiSectionLabel(
                            text = stringResource(
                                MR.strings.tsz_anime_episode_progress,
                                episodeState.items.count(Episode::seen),
                                episodeState.items.size,
                            ),
                        )
                    }
                }
                if (filtered.isEmpty() && !details.refreshing && !details.refreshFailed) {
                    item { AnimeCatalogueStatus(androidStringResource(R.string.tsz_anime_no_episodes)) }
                }
                items(filtered, key = Episode::id) { episode ->
                    AnimeEpisodeTile(
                        source = entry.source,
                        episode = episode,
                        selected = episode.id in selection,
                        enabled = !saving,
                        onClick = { if (selected.isEmpty()) play(episode) else toggleSelection(episode.id) },
                        onSelect = { toggleSelection(episode.id) },
                        onMarkSeen = { mutate { markSeen(setOf(episode.id), it) } },
                        onBookmark = { mutate { library.setEpisodesBookmarked(setOf(episode.id), it) } },
                        onMarkPrevious = { markPrevious(episode) },
                        downloadStatus = downloadStatuses[episode.id],
                        onDownload = { enqueue(listOf(episode)) },
                    )
                }
            }

            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth >= 840.dp) {
                    Row(
                        Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LazyColumn(
                            modifier = Modifier.weight(0.43f).fillMaxHeight(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            overview(expanded = true)
                        }
                        LazyColumn(
                            modifier = Modifier.weight(0.57f).fillMaxHeight(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            episodes()
                        }
                    }
                } else {
                    LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        overview(expanded = false)
                        episodes()
                    }
                }
            }
        }
    }
    shelfSelection?.let { initial ->
        AnimeShelfPicker(shelves, initial, onDismiss = { shelfSelection = null }) { selectedShelves ->
            shelfSelection = null
            mutate { model.setFavorite(true, selectedShelves) }
        }
    }
    if (showFilter) {
        AnimeEpisodeFilterDialog(details.episodeDisplay, onDismiss = { showFilter = false }) { displayOptions ->
            showFilter = false
            mutate { model.setEpisodeDisplay(displayOptions) }
        }
    }
}

@Composable
private fun AnimeShelfPicker(
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
internal fun AnimeCover(source: AnimeSource?, anime: SAnime, modifier: Modifier = Modifier) {
    MangaCover.Book(rememberAnimeImage(source, anime.thumbnail_url), modifier = modifier, generatedTitle = anime.title)
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
