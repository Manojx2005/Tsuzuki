package eu.kanade.presentation.tsuzuki.anime

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.graphics.Color
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
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
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
        var selectedShelf by rememberSaveable { mutableLongStateOf(0L) }
        var menu by remember { mutableStateOf(false) }
        val activeShelf = shelves.indexOfFirst { it.id == selectedShelf }.coerceAtLeast(0)
        val filtered = library.filter { (shelves.getOrNull(activeShelf)?.id ?: 0L) in it.shelfIds }
        val tokens = TsuzukiTheme.tokens
        val bottomPad = if (LocalConfiguration.current.screenWidthDp < 600) 108.dp else 24.dp
        Scaffold(
            modifier = Modifier.tsuzukiBackground(tokens),
            containerColor = Color.Transparent,
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.label_library),
                    actions = {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Outlined.MoreVert, stringResource(MR.strings.action_menu))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            TsuzukiMenuItem(stringResource(MR.strings.tsz_anime_shelves), onClick = {
                                menu = false
                                navigator.push(AnimeShelvesScreen())
                            })
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
                if (library.isEmpty()) {
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_library_empty)),
                        color = tokens.mutedText,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(106.dp),
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
        add(stringResource(TDMR.strings.label_novels))
        if (!hideManga) add(stringResource(TDMR.strings.label_manga))
        add(stringResource(MR.strings.tsz_anime_media))
    }
    TsuzukiPillRow(labels, selected = labels.lastIndex, onClick = { selected ->
        if (selected != labels.lastIndex) {
            if (joined) preferences.filterNovel().set(if (selected == 0) TriState.ENABLED_IS else TriState.ENABLED_NOT)
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
            item {
                TsuzukiTile(
                    modifier = Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button) {
                        openAnime(context, resume.anime.id, resume.episode.id)
                    },
                    padding = 16.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MangaCover.Book(
                            resume.anime.thumbnailUrl,
                            modifier = Modifier.width(76.dp),
                            generatedTitle = resume.anime.title,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_continue_watching)),
                                color = tokens.mutedText,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                tsuzukiMenuLabel(resume.anime.title),
                                color = tokens.dotOn,
                                style = MaterialTheme.typography.displaySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(tsuzukiMenuLabel(resume.episode.name), color = tokens.mutedText, maxLines = 1)
                            if (resume.episode.totalSeconds > 0) {
                                val progress = resume.episode.lastSecondSeen.toFloat() / resume.episode.totalSeconds
                                TsuzukiProgressBar(progress)
                            }
                        }
                        Icon(Icons.Outlined.PlayArrow, null, tint = tokens.accent)
                    }
                }
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
                    Text(stringResource(MR.strings.tsz_anime_library_empty), color = tokens.mutedText)
                    TextButton(onClick = onBrowse) { Text(stringResource(MR.strings.browse)) }
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
private fun AnimeLibraryCover(anime: Anime, modifier: Modifier = Modifier, onOpen: () -> Unit) {
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
