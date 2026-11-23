package eu.kanade.presentation.tsuzuki.anime

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.library.components.DownloadsBadge
import eu.kanade.presentation.library.components.LanguageBadge
import eu.kanade.presentation.library.components.LibrarySelectionToolbar
import eu.kanade.presentation.library.components.UnreadBadge
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackEnabled
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.selection.TsuzukiSelectedCover
import eu.kanade.presentation.tsuzuki.selection.TsuzukiUndoSnackbarHost
import eu.kanade.presentation.tsuzuki.updates.AnimeAiringStrip
import eu.kanade.presentation.tsuzuki.updates.rememberAnimeAiringToday
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryDisplay
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryEntry
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryOptionPreferences
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryOptions
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.tsuzuki.anime.ContinueAnime
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.animeSeasonGrouping
import eu.kanade.tachiyomi.tsuzuki.anime.applyAnimeLibraryOptions
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.tsuzuki.anime.groupAnimeSeasons
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimeCatalogueActivity
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.NovelsTab
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal const val SAVED_ANIME_ID = "saved_anime_id"
internal const val RESUME_ANIME_EPISODE_ID = "resume_anime_episode_id"

internal fun openAnime(context: Context, animeId: Long, resumeId: Long? = null) {
    if (!TsuzukiAnime.enabled) return
    val intent = Intent(context, AnimeCatalogueActivity::class.java).putExtra(SAVED_ANIME_ID, animeId)
    if (resumeId != null) intent.putExtra(RESUME_ANIME_EPISODE_ID, resumeId)
    context.startActivity(intent)
}

internal data object AnimeLibraryTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 8u, title = stringResource(MR.strings.tsz_anime_media))

    @Composable
    override fun Content() {
        if (!TsuzukiAnime.enabled) return
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val store = remember { Injekt.get<AnimeLibraryStore>() }
        val library by remember { store.observeLibrary() }.collectAsState(emptyList())
        val shelves by remember { store.observeShelves() }.collectAsState(emptyList())
        val groupSeasons by remember { animeSeasonGrouping(Injekt.get()) }.collectAsState()
        var selectedShelf by rememberSaveable { mutableLongStateOf(0L) }
        var query by rememberSaveable { mutableStateOf<String?>(null) }
        val activeShelf = shelves.indexOfFirst { it.id == selectedShelf }.coerceAtLeast(0)
        val shelfId = shelves.getOrNull(activeShelf)?.id ?: 0L
        val search = query.orEmpty().trim()
        // AY-INT item 5: More › Downloaded only also narrows the anime library to downloaded anime.
        val downloadedOnly by remember { Injekt.get<BasePreferences>().downloadedOnly }.collectAsState()
        val downloads by remember { Injekt.get<AnimeDownloads>().state }.collectAsState()
        val savedAnime = remember(downloads) { downloads.savedAnimeIds() }
        // AP8: Filter / Sort / Display options, in the book library's sheet.
        val optionPrefs = remember { AnimeLibraryOptionPreferences(Injekt.get()) }
        val options = AnimeLibraryOptions(
            sort = optionPrefs.sort.collectAsState().value,
            descending = optionPrefs.descending.collectAsState().value,
            downloaded = optionPrefs.downloaded.collectAsState().value,
            unwatched = optionPrefs.unwatched.collectAsState().value,
            started = optionPrefs.started.collectAsState().value,
            completed = optionPrefs.completed.collectAsState().value,
            tracked = optionPrefs.tracked.collectAsState().value,
            display = optionPrefs.display.collectAsState().value,
        )
        var showOptions by rememberSaveable { mutableStateOf(false) }
        val trackedIds by remember { Injekt.get<AnimeTrackRepository>().subscribeTrackedAnimeIds() }
            .collectAsState(emptySet())
        val libraryPrefs = remember { Injekt.get<LibraryPreferences>() }
        val badges = rememberAnimeLibraryBadges(downloads)
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val columnCount by (if (landscape) libraryPrefs.landscapeColumns else libraryPrefs.portraitColumns)
            .collectAsState()
        val filtered = remember(library, shelfId, search, groupSeasons, downloadedOnly, savedAnime, options, trackedIds) {
            val matches = library.filter {
                shelfId in it.shelfIds && it.anime.title.contains(search, ignoreCase = true) &&
                    (!downloadedOnly || it.anime.id in savedAnime)
            }
            val order = applyAnimeLibraryOptions(matches.map { it.anime }, options, savedAnime, trackedIds)
                .withIndex().associate { (index, anime) -> anime.id to index }
            val sorted = matches.filter { it.anime.id in order }.sortedBy { order.getValue(it.anime.id) }
            if (groupSeasons && search.isEmpty()) groupAnimeSeasons(sorted) else sorted
        }
        val selection = remember { AnimeLibrarySelection() }
        val actions = remember { AnimeLibraryActions() }
        val actionModel = viewModel<AnimeLibraryActionModel>()
        val haptic = LocalHapticFeedback.current
        val visibleIds = remember(filtered) { filtered.map { it.anime.id } }
        val selected = library.map(AnimeLibraryEntry::anime).filter { it.id in selection.ids }
        val selecting = selected.isNotEmpty()
        BackHandler(enabled = tsuzukiBackEnabled(selecting, HomeScreen), onBack = selection::clear)
        LaunchedEffect(selecting) { HomeScreen.showBottomNav(!selecting) }
        AnimeLibraryActionDialogs(actions)
        if (showOptions) {
            AnimeLibraryOptionsSheet(
                preferences = optionPrefs,
                onDismiss = { showOptions = false },
                onEditShelves = {
                    showOptions = false
                    navigator.push(AnimeShelvesScreen())
                },
            )
        }
        val tokens = TsuzukiTheme.tokens
        val compact = LocalConfiguration.current.screenWidthDp < 600
        val bottomPad = when {
            selecting -> 12.dp
            compact -> 108.dp
            else -> 24.dp
        }
        Scaffold(
            modifier = Modifier.tsuzukiBackground(tokens),
            containerColor = Color.Transparent,
            topBar = topBar@{
                if (selecting) {
                    LibrarySelectionToolbar(
                        selectedCount = selected.size,
                        onClickUnselectAll = selection::clear,
                        onClickSelectAll = { selection.selectAll(visibleIds) },
                        onClickInvertSelection = { selection.invert(visibleIds) },
                    )
                    return@topBar
                }
                SearchToolbar(
                    searchQuery = query,
                    onChangeSearchQuery = { query = it },
                    titleContent = { AppBarTitle(stringResource(MR.strings.label_library)) },
                    actions = {
                        IconButton(onClick = { showOptions = true }) {
                            Icon(
                                Icons.Outlined.FilterList,
                                stringResource(MR.strings.action_filter),
                                tint = if (options.hasFilters) tokens.accent else LocalContentColor.current,
                            )
                        }
                        // AP8: the book library toolbar's overflow: update this shelf, open a random title.
                        AppBarActions(
                            listOf(
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.action_update_category),
                                    onClick = { AnimeLibraryUpdateJob.start(context, shelfId) },
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.action_open_random_manga),
                                    onClick = {
                                        filtered.randomOrNull()?.let { openAnime(context, it.anime.id) }
                                    },
                                ),
                            ),
                        )
                    },
                )
            },
            bottomBar = {
                if (selecting) {
                    AnimeLibraryActionBar(selected, actions, actionModel, actionModel.snackbar, selection::clear)
                }
            },
            snackbarHost = { TsuzukiUndoSnackbarHost(actionModel.snackbar) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                AnimeLibraryMedia(onMediaChange = selection::clear)
                if (shelves.size > 1) {
                    TsuzukiPillRow(
                        labels = shelves.map { shelf ->
                            "${shelf.displayName()} ${library.count { shelf.id in it.shelfIds }}"
                        },
                        selected = activeShelf,
                        onClick = {
                            selection.clear()
                            selectedShelf = shelves[it].id
                        },
                    )
                }
                if (filtered.isEmpty()) {
                    val message = if (library.isEmpty()) {
                        MR.strings.tsz_anime_library_empty
                    } else {
                        MR.strings.no_results_found
                    }
                    Text(
                        tsuzukiMenuLabel(stringResource(message)),
                        color = tokens.mutedText,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                LazyVerticalGrid(
                    columns = when {
                        options.display == AnimeLibraryDisplay.LIST -> GridCells.Fixed(1)
                        columnCount > 0 -> GridCells.Fixed(columnCount)
                        compact -> GridCells.Fixed(3)
                        else -> GridCells.Adaptive(126.dp)
                    },
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPad),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(filtered, key = { it.anime.id }) { item ->
                        val id = item.anime.id
                        val onLongClick = {
                            selection.range(id, visibleIds)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        val onOpen = { if (selecting) selection.toggle(id) else openAnime(context, id) }
                        val itemBadges = badges.forAnime(item.anime)
                        if (options.display == AnimeLibraryDisplay.LIST) {
                            AnimeLibraryListRow(item.anime, id in selection.ids, selecting, itemBadges, onLongClick, onOpen)
                        } else {
                            AnimeLibraryCover(
                                item.anime,
                                selected = id in selection.ids,
                                selectionMode = selecting,
                                onLongClick = onLongClick,
                                badges = itemBadges,
                                display = options.display,
                                onOpen = onOpen,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeLibraryMedia(onMediaChange: () -> Unit) {
    val tabNavigator = LocalTabNavigator.current
    val preferences = remember { Injekt.get<LibraryPreferences>() }
    val joined by preferences.joinedLibrary.collectAsState()
    val hideManga by remember { Injekt.get<BasePreferences>().hideMangaUi }.collectAsState()
    val labels = buildList {
        if (joined) add(stringResource(MR.strings.tsz_library_all))
        if (!joined || !hideManga) add(stringResource(TDMR.strings.label_novels))
        if (!hideManga) add(stringResource(TDMR.strings.label_manga))
        add(stringResource(MR.strings.tsz_anime_media))
    }
    TsuzukiSegments(labels, selected = labels.lastIndex, onClick = { selected ->
        if (selected != labels.lastIndex) {
            onMediaChange()
            if (joined) {
                val filter = when (selected) {
                    0 -> TriState.DISABLED
                    1 -> TriState.ENABLED_IS
                    else -> TriState.ENABLED_NOT
                }
                preferences.filterNovel().set(filter)
            }
            tabNavigator.current = if (joined || selected == 0) NovelsTab else LibraryTab
        }
    })
}

@Composable
internal fun TsuzukiAnimeWatch(bottomPad: Dp, onBrowse: () -> Unit) {
    if (!TsuzukiAnime.enabled) return
    val context = LocalContext.current
    val tabNavigator = LocalTabNavigator.current
    val store = remember { Injekt.get<AnimeLibraryStore>() }
    val next by remember { store.observeContinueWatching() }.collectAsState(null)
    val library by remember { store.observeLibrary() }.collectAsState(emptyList())
    val airing = rememberAnimeAiringToday(store)
    val tokens = TsuzukiTheme.tokens
    BoxWithConstraints {
        // Tablets (600 dp and wider) put the preview beside the title so the rest of Watch stays above the fold.
        val wide = maxWidth >= 600.dp
        val side = if (wide) 28.dp else 16.dp
        LazyColumn(
            contentPadding = PaddingValues(start = side, end = side, top = 12.dp, bottom = bottomPad),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            next?.let { resume ->
                item(key = "continue_watching") {
                    AnimeWatchHero(resume, wide) { openAnime(context, resume.anime.id, resume.episode.id) }
                }
            }
            airing?.let { entry -> item(key = "airing_today") { AnimeAiringStrip(entry) } }
            if (library.isNotEmpty()) item(key = "watch_activity") { WatchActivityTiles(store) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.label_library)),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.dotOn,
                    )
                    TextButton(onClick = { tabNavigator.current = AnimeLibraryTab }) {
                        Text(tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_media)))
                    }
                }
            }
            if (library.isEmpty()) {
                item {
                    TsuzukiTile(Modifier.fillMaxWidth(), padding = 16.dp) {
                        Text(
                            tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_library_empty)),
                            color = tokens.mutedText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = onBrowse) { Text(tsuzukiMenuLabel(stringResource(MR.strings.browse))) }
                    }
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(library, key = { it.anime.id }) { item ->
                            AnimeLibraryCover(item.anime, Modifier.width(108.dp)) {
                                openAnime(context, item.anime.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeWatchHero(resume: ContinueAnime, wide: Boolean, onOpen: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val foreground = if (tokens.isDot) tokens.dotOn else tokens.onAccent
    val muted = if (tokens.isDot) tokens.mutedText else foreground.copy(alpha = 0.75f)
    val thumbnail = @Composable { modifier: Modifier ->
        eu.kanade.tachiyomi.tsuzuki.anime.preview.AnimeEpisodeThumbnail(
            sourceId = resume.anime.source,
            episode = resume.episode,
            coverUrl = resume.anime.thumbnailUrl,
            modifier = modifier.aspectRatio(16f / 9f),
            hero = true,
        )
    }
    val details = @Composable { modifier: Modifier ->
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_continue_watching)),
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                )
                Text(
                    tsuzukiMenuLabel(resume.anime.title),
                    style = MaterialTheme.typography.displaySmall,
                    color = foreground,
                    maxLines = if (wide) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tsuzukiMenuLabel(resume.episode.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                    maxLines = if (wide) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(
                    if (tokens.isDot) tokens.accent else tokens.onAccent,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = if (tokens.isDot) tokens.onAccent else tokens.accent,
                )
            }
        }
    }
    TsuzukiTile(
        modifier = Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button, onClick = onOpen),
        padding = 0.dp,
    ) {
        val background = Modifier.fillMaxWidth().background(if (tokens.isDot) Color.Transparent else tokens.accent)
        if (wide) {
            Row(
                background.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                thumbnail(Modifier.weight(0.55f))
                details(Modifier.weight(0.45f))
            }
        } else {
            Column(background.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                thumbnail(Modifier.fillMaxWidth())
                details(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun AnimeLibraryCover(
    anime: Anime,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    badges: AnimeCoverBadges = AnimeCoverBadges(),
    display: AnimeLibraryDisplay = AnimeLibraryDisplay.GRID,
    onOpen: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    Column(
        modifier
            .alpha(if (selectionMode && !selected) 0.55f else 1f)
            .then(if (selectionMode) Modifier.semantics { this.selected = selected } else Modifier)
            .tsuzukiClickable(role = Role.Button, onClick = onOpen, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            MangaCover.Book(anime.thumbnailUrl, modifier = Modifier.fillMaxWidth(), generatedTitle = anime.title)
            // AP8: the book covers' badges, following Library settings' download / unread badge switches.
            if (badges.unwatched > 0 || badges.downloaded > 0) {
                BadgeGroup(Modifier.padding(4.dp).align(Alignment.TopStart)) {
                    DownloadsBadge(badges.downloaded)
                    UnreadBadge(badges.unwatched)
                }
            }
            if (badges.language.isNotEmpty()) {
                BadgeGroup(Modifier.padding(4.dp).align(Alignment.TopEnd)) {
                    LanguageBadge(isLocal = false, sourceLanguage = badges.language)
                }
            }
            if (display == AnimeLibraryDisplay.COMPACT) {
                // AP8: compact grid, the title over the cover's lower edge as in the book library.
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f)),
                    ),
                )
                Text(
                    tsuzukiMenuLabel(anime.title),
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) TsuzukiSelectedCover()
        }
        if (display != AnimeLibraryDisplay.GRID) return@Column
        Text(
            tsuzukiMenuLabel(anime.title),
            style = MaterialTheme.typography.titleSmall,
            color = tokens.dotOn,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (anime.totalCount > 0) {
            Text(
                tsuzukiMenuLabel(
                    stringResource(MR.strings.tsz_anime_episode_progress, anime.seenCount, anime.totalCount),
                ),
                color = tokens.mutedText,
                style = MaterialTheme.typography.labelSmall,
            )
            TsuzukiProgressBar(anime.seenCount.toFloat() / anime.totalCount)
        }
    }
}

/** AP8: the library's List display, a book-library list row: cover, title, episode progress. */
@Composable
internal fun AnimeLibraryListRow(
    anime: Anime,
    selected: Boolean,
    selectionMode: Boolean,
    badges: AnimeCoverBadges,
    onLongClick: () -> Unit,
    onOpen: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    Row(
        Modifier.fillMaxWidth()
            .alpha(if (selectionMode && !selected) 0.55f else 1f)
            .then(if (selectionMode) Modifier.semantics { this.selected = selected } else Modifier)
            .tsuzukiClickable(role = Role.Button, onClick = onOpen, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(48.dp)) {
            MangaCover.Book(anime.thumbnailUrl, modifier = Modifier.fillMaxWidth(), generatedTitle = anime.title)
            if (selected) TsuzukiSelectedCover()
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                tsuzukiMenuLabel(anime.title),
                style = MaterialTheme.typography.titleSmall,
                color = tokens.dotOn,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (anime.totalCount > 0) {
                Text(
                    tsuzukiMenuLabel(
                        stringResource(MR.strings.tsz_anime_episode_progress, anime.seenCount, anime.totalCount),
                    ),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        BadgeGroup {
            DownloadsBadge(badges.downloaded)
            UnreadBadge(badges.unwatched)
            LanguageBadge(isLocal = false, sourceLanguage = badges.language)
        }
    }
}

@Composable
internal fun AnimeCategory.displayName(): String =
    if (isSystem) stringResource(MR.strings.label_default) else name
