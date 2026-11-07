package eu.kanade.tachiyomi.tsuzuki.anime.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.anime.displayName
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.res.stringResource as androidStringResource

private sealed interface AnimeDetailsLoad {
    data object Loading : AnimeDetailsLoad
    data object Error : AnimeDetailsLoad
    data class Ready(val anime: SAnime, val episodes: List<SEpisode>) : AnimeDetailsLoad
}

@Composable
internal fun AnimeDetailsScreen(
    entry: RegisteredAnimeSource,
    anime: SAnime,
    repository: AnimeDebugRepository,
    library: AnimeLibraryStore,
    resumeUrl: String?,
    onBack: () -> Unit,
    launchPlayer: (Video, Episode?) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    val details by produceState<AnimeDetailsLoad>(AnimeDetailsLoad.Loading, entry, anime, reload) {
        value = AnimeDetailsLoad.Loading
        value = try {
            withContext(Dispatchers.IO) { loadAnimeDetails(entry, anime, repository) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Could not load anime details" }
            AnimeDetailsLoad.Error
        }
    }
    var lastPlayedUrl by rememberSaveable(anime.url) { mutableStateOf<String?>(null) }
    var playingRequest by remember { mutableStateOf<String?>(null) }
    var playbackError by remember { mutableStateOf(false) }
    var saved by remember(anime.url) { mutableStateOf<Anime?>(null) }
    var shelfSelection by remember { mutableStateOf<Set<Long>?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    val shelves by remember { library.observeShelves() }.collectAsState(emptyList())
    val ready = details as? AnimeDetailsLoad.Ready
    val display = ready?.anime ?: anime
    LaunchedEffect(entry.source.id, anime.url) {
        saved = library.getAnime(entry.source.id, anime.url)
    }
    val saveFavorite: (Boolean, Set<Long>) -> Unit = { favorite, ids ->
        saving = true
        saveError = false
        scope.launch {
            try {
                if (favorite && ready != null) library.saveEpisodes(entry.source.id, display, ready.episodes)
                val id = library.setFavorite(entry.source.id, display, favorite, ids)
                saved = library.getAnime(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Could not change anime shelves" }
                saveError = true
            } finally {
                saving = false
            }
        }
    }
    val play: (SEpisode) -> Unit = { episode ->
        if (playingRequest == null) {
            playingRequest = episode.url
            playbackError = false
            scope.launch {
                try {
                    val (video, storedEpisode) = withContext(Dispatchers.IO) {
                        val video = resolveAnimeVideo(entry, episode, repository)
                        val storedEpisode = if (Injekt.get<BasePreferences>().incognitoMode.get()) {
                            null
                        } else {
                            val available = ready?.episodes ?: listOf(episode)
                            library.saveEpisodes(entry.source.id, display, available)[episode.url]
                        }
                        video to storedEpisode
                    }
                    launchPlayer(video, storedEpisode)
                    lastPlayedUrl = episode.url
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "Could not play anime episode" }
                    playbackError = true
                } finally {
                    playingRequest = null
                }
            }
        }
    }
    var resumed by rememberSaveable(anime.url) { mutableStateOf(false) }
    LaunchedEffect(ready, resumeUrl) {
        if (ready == null || resumeUrl == null || resumed) return@LaunchedEffect
        resumed = true
        ready.episodes.firstOrNull { it.url == resumeUrl }?.let(play)
    }

    Scaffold(
        modifier = Modifier.tsuzukiBackground(tokens),
        containerColor = Color.Transparent,
        topBar = { AppBar(title = null, navigateUp = onBack) },
    ) { padding ->
        LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnimeCover(entry.source, display, Modifier.width(104.dp))
                    Text(
                        text = tsuzukiMenuLabel(display.title),
                        modifier = Modifier.weight(1f),
                        color = tokens.dotOn,
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    val favorite = saved?.favorite == true
                    Button(enabled = !saving, onClick = {
                        if (favorite) {
                            saveFavorite(false, emptySet())
                        } else {
                            scope.launch {
                                shelfSelection = saved?.let { library.shelfIds(it.id) } ?: setOf(0L)
                            }
                        }
                    }) {
                        val label = if (favorite) MR.strings.remove_from_library else MR.strings.add_to_library
                        Text(tsuzukiMenuLabel(stringResource(label)))
                    }
                    if (favorite) {
                        TextButton(enabled = !saving, onClick = {
                            scope.launch { saved?.let { shelfSelection = library.shelfIds(it.id) } }
                        }) {
                            Text(tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_shelves)))
                        }
                    }
                }
            }
            if (saveError) {
                item { AnimeCatalogueStatus(stringResource(MR.strings.unknown_error)) }
            }
            display.description?.takeIf { it.isNotBlank() }?.let { description ->
                item {
                    TsuzukiTile(Modifier.fillMaxWidth().padding(horizontal = 12.dp), padding = 16.dp) {
                        Text(description, color = tokens.dotOn, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            when (val current = details) {
                AnimeDetailsLoad.Loading -> item { AnimeCatalogueStatus(null) }
                AnimeDetailsLoad.Error -> item {
                    AnimeCatalogueStatus(stringResource(MR.strings.unknown_error)) { reload++ }
                }
                is AnimeDetailsLoad.Ready -> {
                    val lastPlayed = current.episodes.indexOfFirst { it.url == lastPlayedUrl }
                    val next = current.episodes.getOrNull(lastPlayed + 1)
                    if (next != null) {
                        item {
                            Button(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                onClick = { play(next) },
                                enabled = playingRequest == null,
                            ) {
                                val label = if (lastPlayed < 0) {
                                    R.string.tsz_anime_play_first
                                } else {
                                    R.string.tsz_anime_play_next
                                }
                                Text(tsuzukiMenuLabel(androidStringResource(label)))
                            }
                        }
                    }
                    if (playbackError) {
                        item { AnimeCatalogueStatus(stringResource(MR.strings.tsz_anime_debug_playback_error)) }
                    }
                    item {
                        Text(
                            text = tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_debug_episodes)),
                            color = tokens.mutedText,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        )
                    }
                    if (current.episodes.isEmpty()) {
                        item { AnimeCatalogueStatus(androidStringResource(R.string.tsz_anime_no_episodes)) }
                    }
                    items(current.episodes, key = { it.url }) { episode ->
                        TsuzukiTile(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .tsuzukiClickable(
                                    enabled = playingRequest == null,
                                    role = Role.Button,
                                    onClick = { play(episode) },
                                ),
                            padding = 16.dp,
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = tsuzukiMenuLabel(episode.name),
                                        color = tokens.dotOn,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    episode.scanlator?.takeIf { it.isNotBlank() }?.let { group ->
                                        Text(
                                            tsuzukiMenuLabel(group),
                                            color = tokens.mutedText,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                                if (playingRequest == episode.url) {
                                    CircularProgressIndicator()
                                } else {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = tokens.accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    shelfSelection?.let { initial ->
        AnimeShelfPicker(shelves, initial, onDismiss = { shelfSelection = null }) { selected ->
            shelfSelection = null
            saveFavorite(true, selected)
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

@Suppress("DEPRECATION")
private suspend fun loadAnimeDetails(
    entry: RegisteredAnimeSource,
    anime: SAnime,
    repository: AnimeDebugRepository,
): AnimeDetailsLoad.Ready {
    val source = entry.source
    val update = if (entry.libVersion >= 17.0) {
        source.getAnimeEpisodeUpdate(anime, emptyList(), fetchDetails = true, fetchEpisodes = true)
    } else {
        eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate(
            source.getAnimeDetails(anime),
            repository.episodes(source, entry.libVersion, anime),
        )
    }
    val display = anime.copy().apply {
        title = try {
            update.anime.title.ifBlank { anime.title }
        } catch (_: UninitializedPropertyAccessException) {
            anime.title
        }
        thumbnail_url = update.anime.thumbnail_url ?: anime.thumbnail_url
        description = update.anime.description ?: anime.description
    }
    val episodes = update.episodes
        .distinctBy { it.url }
        .sortedBy { it.episode_number.takeIf { number -> number >= 0 } ?: Float.POSITIVE_INFINITY }
    return AnimeDetailsLoad.Ready(display, episodes)
}

private suspend fun resolveAnimeVideo(
    entry: RegisteredAnimeSource,
    episode: SEpisode,
    repository: AnimeDebugRepository,
): Video {
    val videos = repository.videos(entry.source, entry.libVersion, episode)
    for (video in videos) {
        try {
            return repository.resolve(entry.source, entry.libVersion, video)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            entry.logcat(LogPriority.WARN, e) { "Could not resolve anime stream" }
        }
    }
    error("No playable video")
}

@Composable
internal fun AnimeCover(source: AnimeSource, anime: SAnime, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val url = anime.thumbnail_url
    val request = remember(context, source, url) {
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
    MangaCover.Book(request, modifier = modifier, generatedTitle = anime.title)
}
