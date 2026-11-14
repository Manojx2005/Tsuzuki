package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.library.components.CommonMangaItemDefaults
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.anime.RESUME_ANIME_EPISODE_ID
import eu.kanade.presentation.tsuzuki.anime.SAVED_ANIME_ID
import eu.kanade.presentation.tsuzuki.browse.ANIME_OPEN_LATEST
import eu.kanade.presentation.tsuzuki.browse.ANIME_SEARCH_QUERY
import eu.kanade.presentation.tsuzuki.browse.ANIME_SOURCE_ID
import eu.kanade.presentation.tsuzuki.components.errorHeading
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.callAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceAnime
import eu.kanade.tachiyomi.tsuzuki.error.errorCopyForUi
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeCatalogueActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!TsuzukiAnime.enabled || (!intent.hasExtra(ANIME_SOURCE_ID) && !intent.hasExtra(SAVED_ANIME_ID))) {
            finish()
            return
        }
        val sourceId = intent.getLongExtra(ANIME_SOURCE_ID, -1L)
        val savedId = intent.getLongExtra(SAVED_ANIME_ID, -1L)
        val resumeId = intent.getLongExtra(RESUME_ANIME_EPISODE_ID, -1L)
        val openLatest = intent.getBooleanExtra(ANIME_OPEN_LATEST, false)
        val initialQuery = intent.getStringExtra(ANIME_SEARCH_QUERY).orEmpty()
        val sources = AnimeSourceManager.create(applicationContext)
        val library = Injekt.get<AnimeLibraryStore>()
        setContent {
            TachiyomiTheme {
                val location by produceState<AnimeCatalogueLocation>(AnimeCatalogueLocation.Loading) {
                    value = try {
                        sources.refresh()
                        val saved = if (savedId >= 0) library.getAnime(savedId) else null
                        val actualSource = saved?.source ?: sourceId
                        val entry = sources.get(actualSource)
                        if (entry != null) {
                            val resume = if (resumeId >= 0) library.getEpisode(resumeId) else null
                            AnimeCatalogueLocation.Ready(
                                entry,
                                saved?.toSourceAnime(),
                                resume?.takeIf { it.animeId == savedId }?.url,
                            )
                        } else {
                            AnimeCatalogueLocation.Missing(sources.stub(actualSource)?.name, saved)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AnimeLog.w("Could not open anime catalogue", e)
                        AnimeCatalogueLocation.Missing(null)
                    }
                }
                when (val result = location) {
                    AnimeCatalogueLocation.Loading -> AnimeCatalogueNotice(null, null, ::finish)
                    is AnimeCatalogueLocation.Missing -> AnimeCatalogueNotice(
                        title = result.anime?.title ?: result.name,
                        message = stringResource(
                            MR.strings.source_not_installed,
                            result.name ?: (result.anime?.source ?: sourceId).toString(),
                        ),
                        onBack = ::finish,
                        cached = result.anime?.toSourceAnime(),
                    )
                    is AnimeCatalogueLocation.Ready -> {
                        val catalogueState = rememberSaveableStateHolder()
                        var appliedFilters by remember { mutableStateOf(AnimeFilterList()) }
                        var selected by remember { mutableStateOf(result.saved) }
                        val anime = selected
                        if (anime == null) {
                            catalogueState.SaveableStateProvider(sourceId) {
                                AnimeCatalogueScreen(
                                    source = result.entry.source,
                                    openLatest = openLatest,
                                    initialQuery = initialQuery,
                                    appliedFilters = appliedFilters,
                                    onApplyFilters = { appliedFilters = it },
                                    onBack = ::finish,
                                    onOpen = { selected = it },
                                )
                            }
                        } else {
                            val back = { if (result.saved != null) finish() else selected = null }
                            BackHandler(onBack = back)
                            AnimeDetailsScreen(
                                result.entry,
                                anime,
                                library,
                                result.resumeUrl,
                                back,
                            ) { episode, track ->
                                AnimePlayback.launchEpisode(this, episode.id, track)
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface AnimeCatalogueLocation {
    data object Loading : AnimeCatalogueLocation
    data class Ready(
        val entry: RegisteredAnimeSource,
        val saved: SAnime?,
        val resumeUrl: String?,
    ) : AnimeCatalogueLocation
    data class Missing(val name: String?, val anime: Anime? = null) : AnimeCatalogueLocation
}

private enum class AnimeCatalogueMode { POPULAR, LATEST, SEARCH }

@Composable
private fun AnimeCatalogueScreen(
    source: AnimeSource,
    openLatest: Boolean,
    initialQuery: String,
    appliedFilters: AnimeFilterList,
    onApplyFilters: (AnimeFilterList) -> Unit,
    onBack: () -> Unit,
    onOpen: (SAnime) -> Unit,
) {
    val modes = remember(source) {
        AnimeCatalogueMode.entries.filter { it != AnimeCatalogueMode.LATEST || source.supportsLatest }
    }
    var mode by rememberSaveable {
        mutableStateOf(
            when {
                initialQuery.isNotBlank() -> AnimeCatalogueMode.SEARCH
                openLatest && source.supportsLatest -> AnimeCatalogueMode.LATEST
                else -> AnimeCatalogueMode.POPULAR
            },
        )
    }
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var appliedQuery by rememberSaveable { mutableStateOf(initialQuery) }
    var editingFilters by remember { mutableStateOf<AnimeFilterList?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val submitSearch = {
        appliedQuery = query.trim()
        mode = AnimeCatalogueMode.SEARCH
        keyboard?.hide()
        Unit
    }
    val pagingFlow = remember(source, mode, appliedQuery, appliedFilters) {
        Pager(PagingConfig(pageSize = 24, enablePlaceholders = false)) {
            AnimeCataloguePagingSource(source, mode, appliedQuery, appliedFilters)
        }.flow
    }
    val shows = pagingFlow.collectAsLazyPagingItems()
    val tokens = TsuzukiTheme.tokens
    Scaffold(
        modifier = Modifier.tsuzukiBackground(tokens),
        containerColor = Color.Transparent,
        topBar = { AppBar(title = tsuzukiMenuLabel(source.name), navigateUp = onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TsuzukiSegments(
                labels = modes.map {
                    stringResource(
                        when (it) {
                            AnimeCatalogueMode.POPULAR -> MR.strings.popular
                            AnimeCatalogueMode.LATEST -> MR.strings.latest
                            AnimeCatalogueMode.SEARCH -> MR.strings.action_search
                        },
                    )
                },
                selected = modes.indexOf(mode),
                onClick = { mode = modes[it] },
            )
            if (mode == AnimeCatalogueMode.SEARCH) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(MR.strings.action_search)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                        trailingIcon = {
                            IconButton(onClick = submitSearch) {
                                Icon(Icons.Outlined.Search, stringResource(MR.strings.action_search))
                            }
                        },
                    )
                    IconButton(
                        onClick = {
                            editingFilters =
                                source.getFilterList().also { restoreAnimeFilterStates(appliedFilters, it) }
                        },
                    ) {
                        Icon(Icons.Outlined.FilterList, stringResource(MR.strings.action_filter))
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(112.dp),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
                horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
                modifier = Modifier.weight(1f),
            ) {
                items(shows.itemCount) { index ->
                    val anime = shows[index] ?: return@items
                    Column(
                        Modifier.tsuzukiClickable(role = Role.Button) { onOpen(anime) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AnimeCover(source, anime, Modifier.fillMaxWidth())
                        Text(
                            text = tsuzukiMenuLabel(anime.title),
                            modifier = Modifier.padding(4.dp),
                            color = tokens.dotOn,
                            style = MaterialTheme.typography.titleSmall,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                val refresh = shows.loadState.refresh
                val append = shows.loadState.append
                if (refresh is LoadState.Loading || append is LoadState.Loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) { AnimeCatalogueStatus(null) }
                } else if (refresh is LoadState.Error || append is LoadState.Error) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val context = LocalContext.current
                        val error = ((refresh as? LoadState.Error) ?: (append as? LoadState.Error))?.error
                        val message = remember(error) {
                            error?.let {
                                val copy = errorCopyForUi(it)
                                listOf(context.errorHeading(copy), copy.technical).filter(String::isNotBlank)
                                    .joinToString("\n")
                            }
                        }
                        AnimeCatalogueStatus(message ?: stringResource(MR.strings.unknown_error), shows::retry)
                    }
                } else if (shows.itemCount == 0) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AnimeCatalogueStatus(stringResource(MR.strings.no_results_found))
                    }
                }
            }
        }
    }
    editingFilters?.let { filters ->
        AnimeFilterDialog(
            filters = filters,
            onDismiss = { editingFilters = null },
            onReset = { editingFilters = source.getFilterList() },
            onApply = {
                onApplyFilters(filters)
                submitSearch()
                editingFilters = null
            },
        )
    }
}

private class AnimeCataloguePagingSource(
    private val source: AnimeSource,
    private val mode: AnimeCatalogueMode,
    private val query: String,
    private val filters: AnimeFilterList,
) : PagingSource<Int, SAnime>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SAnime> {
        val page = params.key ?: 1
        return try {
            val result = withContext(Dispatchers.IO) {
                callAnimeSource {
                    when (mode) {
                        AnimeCatalogueMode.POPULAR -> source.getPopularAnime(page)
                        AnimeCatalogueMode.LATEST -> source.getLatestUpdates(page)
                        AnimeCatalogueMode.SEARCH -> source.getSearchAnime(page, query, filters)
                    }
                }
            }
            LoadResult.Page(
                data = result.animes,
                prevKey = null,
                nextKey = (page + 1).takeIf { result.hasNextPage && result.animes.isNotEmpty() },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AnimeLog.w("catalogue ${source.name} $mode page=$page failed", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SAnime>): Int? = null
}

@Composable
private fun AnimeCatalogueNotice(
    title: String?,
    message: String?,
    onBack: () -> Unit,
    cached: SAnime? = null,
) {
    Scaffold(
        modifier = Modifier.tsuzukiBackground(TsuzukiTheme.tokens),
        containerColor = Color.Transparent,
        topBar = { AppBar(title = title?.let { tsuzukiMenuLabel(it) }, navigateUp = onBack) },
    ) { padding ->
        if (cached == null) {
            Column(Modifier.fillMaxSize().padding(padding)) { AnimeCatalogueStatus(message) }
        } else {
            LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { AnimeDetailsHeader(source = null, anime = cached) }
                item { AnimeCatalogueStatus(message) }
            }
        }
    }
}

@Composable
internal fun AnimeCatalogueStatus(message: String?, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (message == null) {
            CircularProgressIndicator()
        } else {
            Text(tsuzukiMenuLabel(message), color = TsuzukiTheme.tokens.dotOn)
        }
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_retry))) }
        }
    }
}
