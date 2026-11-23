package eu.kanade.presentation.tsuzuki.browse

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.ui.rememberAnimeImage
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchStats
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal fun showAnimeGlobalSearch(enabled: Boolean, sourceCount: Int): Boolean = enabled && sourceCount > 0

internal fun selectAnimeSearchSources(
    available: List<RegisteredAnimeSource>,
    pinnedIds: Set<String>,
    pinnedOnly: Boolean,
): List<RegisteredAnimeSource> = available
    .filter { !pinnedOnly || it.source.id.toString() in pinnedIds }
    .sortedWith(compareBy({ it.source.id.toString() !in pinnedIds }, { it.source.name.lowercase() }, { it.source.id }))

internal data class AnimeSearchSourceResult(
    val entry: RegisteredAnimeSource,
    val titles: List<SAnime>? = null,
    val elapsedMillis: Long? = null,
    val failed: Boolean = false,
) {
    val loading: Boolean get() = titles == null && !failed
}

internal class AnimeGlobalSearch {
    var selected by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var pinnedOnly by mutableStateOf(false)
    var hasResultsOnly by mutableStateOf(false)
    var revision by mutableIntStateOf(0)
        private set
    var results by mutableStateOf(emptyList<AnimeSearchSourceResult>())
        private set

    fun select(anime: Boolean, text: String?) {
        if (selected == anime) return
        selected = anime
        if (anime) submit(text.orEmpty())
    }

    fun submit(text: String) {
        query = text.trim()
        revision++
    }

    companion object {
        @Volatile private var animeFirst = false

        /** The next global search opens on the Anime pill (Browse › Anime toolbar, AP10). */
        fun openAnimeFirst() {
            animeFirst = true
        }

        internal fun consumeAnimeFirst(): Boolean = animeFirst.also { animeFirst = false }
    }

    suspend fun search(sources: List<RegisteredAnimeSource>, submitted: String) = coroutineScope {
        if (submitted.isBlank()) {
            results = emptyList()
            return@coroutineScope
        }
        results = sources.map { AnimeSearchSourceResult(it) }
        val permits = Semaphore(5)
        sources.forEachIndexed { index, entry ->
            launch {
                val item = permits.withPermit { searchAnimeSource(entry, submitted) }
                ensureActive()
                results = results.toMutableList().apply { this[index] = item }
            }
        }
    }
}

private suspend fun searchAnimeSource(entry: RegisteredAnimeSource, query: String): AnimeSearchSourceResult {
    val source = entry.source
    val started = System.nanoTime()
    return try {
        val titles = withContext(Dispatchers.IO) {
            source.getSearchAnime(1, query, source.getFilterList()).animes.distinctBy(SAnime::url)
        }
        AnimeSearchSourceResult(entry, titles, GlobalSearchStats.elapsedMillis(started, System.nanoTime()))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        source.logcat(LogPriority.WARN) { "Anime search failed for source ${source.id} (${e.javaClass.simpleName})" }
        AnimeSearchSourceResult(entry, failed = true)
    } catch (e: LinkageError) {
        source.logcat(LogPriority.WARN) { "Incompatible anime source ${source.id} (${e.javaClass.simpleName})" }
        AnimeSearchSourceResult(entry, failed = true)
    }
}

@Composable
internal fun rememberAnimeGlobalSearch(allowed: Boolean): AnimeGlobalSearch? {
    if (!allowed || !TsuzukiAnime.enabled || TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) return null
    val available by rememberAnimeBrowseSources()
    val (_, pins) = rememberAnimeSourcePins()
    val model = remember {
        AnimeGlobalSearch().also { if (AnimeGlobalSearch.consumeAnimeFirst()) it.select(true, null) }
    }
    val selected = model.selected
    val submitted = model.query
    val sources = remember(available, pins, model.pinnedOnly) {
        selectAnimeSearchSources(available, pins, model.pinnedOnly)
    }
    LaunchedEffect(selected, submitted, model.revision, sources) {
        if (selected) model.search(sources, submitted)
    }
    return model.takeIf { showAnimeGlobalSearch(TsuzukiAnime.enabled, available.size) }
}

@Composable
internal fun AnimeSearchMediaPills(model: AnimeGlobalSearch, currentQuery: String?, booksAreNovels: Boolean) {
    TsuzukiPillRow(
        labels = listOf(
            stringResource(if (booksAreNovels) TDMR.strings.label_novels else TDMR.strings.label_manga),
            stringResource(MR.strings.tsz_anime_media),
        ),
        selected = if (model.selected) 1 else 0,
        onClick = { model.select(it == 1, currentQuery) },
    )
}

@Composable
internal fun AnimeSearchFilterPills(model: AnimeGlobalSearch) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TsuzukiPill(
            text = stringResource(MR.strings.pinned_sources),
            selected = model.pinnedOnly,
            onClick = { model.pinnedOnly = true },
        )
        TsuzukiPill(
            text = stringResource(MR.strings.all),
            selected = !model.pinnedOnly,
            onClick = { model.pinnedOnly = false },
        )
        TsuzukiPill(
            text = stringResource(MR.strings.has_results),
            selected = model.hasResultsOnly,
            onClick = { model.hasResultsOnly = !model.hasResultsOnly },
            role = Role.Checkbox,
        )
    }
}

@Composable
internal fun AnimeGlobalSearchResults(
    model: AnimeGlobalSearch,
    padding: PaddingValues,
    onSelect: ((RegisteredAnimeSource, SAnime) -> Unit)? = null,
) {
    val tokens = TsuzukiTheme.tokens
    val context = LocalContext.current
    // In place from the main Global search, like book results; the details Activity elsewhere.
    val openDetails = eu.kanade.presentation.tsuzuki.anime.rememberAnimeDetailsOpener()
    val scope = rememberCoroutineScope()
    val library = remember { Injekt.get<AnimeLibraryStore>() }
    val openError = stringResource(MR.strings.unknown_error)
    var opening by remember { mutableStateOf(false) }
    val results = model.results
    val finished = results.count { !it.loading }
    val visible = if (model.hasResultsOnly) results.filter { !it.titles.isNullOrEmpty() } else results

    LazyColumn(contentPadding = padding) {
        if (results.isNotEmpty() && finished < results.size) {
            item(key = "anime-search-progress") {
                TsuzukiProgressBar(
                    progress = finished.toFloat() / results.size,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    thickness = 5.dp,
                    spreadDots = true,
                )
            }
        }
        if (model.query.isNotBlank() && visible.isEmpty() && finished == results.size) {
            item(key = "anime-search-empty") {
                val label = if (results.isEmpty() && model.pinnedOnly) {
                    MR.strings.no_pinned_sources
                } else {
                    MR.strings.no_results_found
                }
                Text(
                    tsuzukiMenuLabel(stringResource(label)),
                    Modifier.padding(16.dp),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(visible, key = { it.entry.source.id }) { result ->
            val source = result.entry.source
            val subtitle = when {
                result.loading -> stringResource(MR.strings.loading)
                result.failed -> stringResource(MR.strings.unknown_error)
                else -> stringResource(
                    MR.strings.tsz_search_results_time,
                    result.titles.orEmpty().size,
                    GlobalSearchStats.formatSeconds(checkNotNull(result.elapsedMillis)),
                )
            }
            TsuzukiGlobalSearchResultItem(
                title = "${source.name} · ${stringResource(MR.strings.tsz_anime_media)}",
                subtitle = subtitle,
                onClick = if (onSelect ==
                    null
                ) {
                    ({ openAnimeCatalogue(context, source.id, query = model.query) })
                } else {
                    null
                },
            ) {
                if (result.loading) GlobalSearchLoadingResultItem()
                if (!result.titles.isNullOrEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(result.titles, key = SAnime::url) { title ->
                            Column(
                                Modifier.size(width = 96.dp, height = 156.dp)
                                    .tsuzukiClickable(role = Role.Button) {
                                        if (onSelect != null) {
                                            onSelect(result.entry, title)
                                            return@tsuzukiClickable
                                        }
                                        if (!opening) {
                                            opening = true
                                            scope.launch {
                                                try {
                                                    openDetails(library.cacheSearchResult(source.id, title))
                                                } catch (e: CancellationException) {
                                                    throw e
                                                } catch (e: Exception) {
                                                    logcat(LogPriority.WARN, e) { "Could not open the anime result" }
                                                    Toast.makeText(context, openError, Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    opening = false
                                                }
                                            }
                                        }
                                    },
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                AsyncImage(
                                    model = rememberAnimeImage(source, title.thumbnail_url),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().size(height = 132.dp, width = 96.dp)
                                        .clip(RoundedCornerShape(6.dp)).background(tokens.track),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = tsuzukiCoverFilter(),
                                )
                                Text(
                                    tsuzukiMenuLabel(title.title),
                                    color = tokens.dotOn,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
