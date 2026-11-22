package eu.kanade.presentation.tsuzuki.anime

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.updates.AnimeAiringStrip
import eu.kanade.presentation.tsuzuki.updates.rememberAnimeAiringToday
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.ContinueAnime
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.animeSeasonGrouping
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.tsuzuki.anime.groupAnimeSeasons
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimeCatalogueActivity
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.NovelsTab
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.res.stringResource as androidStringResource

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
        val filtered = remember(library, shelfId, search, groupSeasons, downloadedOnly, savedAnime) {
            val matches = library.filter {
                shelfId in it.shelfIds && it.anime.title.contains(search, ignoreCase = true) &&
                    (!downloadedOnly || it.anime.id in savedAnime)
            }
            if (groupSeasons && search.isEmpty()) groupAnimeSeasons(matches) else matches
        }
        val tokens = TsuzukiTheme.tokens
        val compact = LocalConfiguration.current.screenWidthDp < 600
        val bottomPad = if (compact) 108.dp else 24.dp
        Scaffold(
            modifier = Modifier.tsuzukiBackground(tokens),
            containerColor = Color.Transparent,
            topBar = {
                SearchToolbar(
                    searchQuery = query,
                    onChangeSearchQuery = { query = it },
                    titleContent = { AppBarTitle(stringResource(MR.strings.label_library)) },
                    actions = {
                        IconButton(onClick = { navigator.push(AnimeShelvesScreen()) }) {
                            Icon(Icons.Outlined.FilterList, stringResource(MR.strings.tsz_anime_shelves))
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                AnimeLibraryMedia()
                if (shelves.size > 1) {
                    TsuzukiPillRow(
                        labels = shelves.map { shelf ->
                            "${shelf.displayName()} ${library.count { shelf.id in it.shelfIds }}"
                        },
                        selected = activeShelf,
                        onClick = { selectedShelf = shelves[it].id },
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
                    columns = if (compact) GridCells.Fixed(3) else GridCells.Adaptive(126.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPad),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(filtered, key = { it.anime.id }) { item ->
                        AnimeLibraryCover(item.anime, onOpen = { openAnime(context, item.anime.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeLibraryMedia() {
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
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPad),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        next?.let { resume ->
            item(key = "continue_watching") {
                AnimeWatchHero(resume) { openAnime(context, resume.anime.id, resume.episode.id) }
            }
        }
        airing?.let { entry -> item(key = "airing_today") { AnimeAiringStrip(entry) } }
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

@Composable
private fun AnimeWatchHero(resume: ContinueAnime, onOpen: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val foreground = if (tokens.isDot) tokens.dotOn else tokens.onAccent
    val muted = if (tokens.isDot) tokens.mutedText else foreground.copy(alpha = 0.75f)
    val position = resume.episode.lastSecondSeen
    val duration = resume.episode.totalSeconds
    val artwork = resume.episode.previewUrl ?: resume.anime.backgroundUrl ?: resume.anime.thumbnailUrl
    TsuzukiTile(
        modifier = Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button, onClick = onOpen),
        padding = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().background(if (tokens.isDot) Color.Transparent else tokens.accent)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().heightIn(max = 252.dp).aspectRatio(2f)
                    .clip(RoundedCornerShape(16.dp)).background(tokens.background),
            ) {
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = tsuzukiCoverFilter(),
                )
                if (position in 1L until duration) {
                    val left = DateUtils.formatElapsedTime(duration - position)
                    Text(
                        tsuzukiMenuLabel(androidStringResource(R.string.tsz_anime_history_remaining, left)),
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            .clip(RoundedCornerShape(8.dp)).background(tokens.background.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.dotOn,
                    )
                }
                if (duration > 0) {
                    TsuzukiProgressBar(
                        progress = (position.toDouble() / duration).toFloat(),
                        modifier = Modifier.align(Alignment.BottomCenter),
                        fill = tokens.dotOn,
                        thickness = 4.dp,
                        spreadDots = true,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        tsuzukiMenuLabel(resume.episode.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                        maxLines = 1,
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
    }
}

@Composable
internal fun AnimeLibraryCover(anime: Anime, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Column(
        modifier.tsuzukiClickable(role = Role.Button, onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MangaCover.Book(anime.thumbnailUrl, modifier = Modifier.fillMaxWidth(), generatedTitle = anime.title)
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

@Composable
internal fun AnimeCategory.displayName(): String =
    if (isSystem) stringResource(MR.strings.label_default) else name
