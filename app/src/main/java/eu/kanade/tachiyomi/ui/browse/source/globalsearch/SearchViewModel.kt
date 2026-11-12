package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.isNovelSource
import eu.kanade.tachiyomi.source.tsuzuki.withSourceHealth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.core.viewmodel.StateViewModel
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.preference.toggle
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalNovelSource
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.Executors

abstract class SearchViewModel(
    initialState: State = State(),
    sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
    private val savedState: SavedStateHandle? = null,
) : StateViewModel<SearchViewModel.State>(initialState) {

    private val coroutineDispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    private var searchJob: Job? = null

    private val enabledLanguages = sourcePreferences.enabledLanguages.get()
    private val disabledSources = sourcePreferences.disabledSources.get()
    protected val pinnedSources = sourcePreferences.pinnedSources.get()

    // Group name -> member source ids
    protected val sourceGroups: Map<String, Set<String>> =
        sourcePreferences.groupPinnedSources.get()
            .groupBy({ it.substringBeforeLast("|") }, { it.substringAfterLast("|") })
            .mapValues { (_, ids) -> ids.toSet() }

    private var lastQuery: String? = null
    private var lastSourceFilter: SourceFilter? = null

    protected var extensionFilter: String? = null

    open val sortComparator = { map: Map<Source, SearchItemResult> ->
        compareBy<Source>(
            { (map[it] as? SearchItemResult.Success)?.isEmpty ?: true },
            { "${it.id}" !in pinnedSources },
            { "${it.name.lowercase()} (${it.lang})" },
        )
    }

    init {
        mutableState.update { it.copy(pinGroups = sourceGroups.keys.sorted()) }
        viewModelScope.launch {
            preferences.globalSearchFilterState.changes().collectLatest { state ->
                mutableState.update { it.copy(onlyShowHasResults = state) }
            }
        }
    }

    @Composable
    fun getManga(initialManga: Manga): androidx.compose.runtime.State<Manga> {
        return produceState(initialValue = initialManga) {
            getManga.subscribe(initialManga.url, initialManga.source)
                .filterNotNull()
                .collectLatest { manga ->
                    value = manga
                }
        }
    }

    open fun getEnabledSources(): List<Source> {
        return sourceManager.getAll()
            .filter {
                (it.lang in enabledLanguages || it.isLocal() || it.id == LocalNovelSource.ID) &&
                    "${it.id}" !in disabledSources
            }
            .sortedWith(
                compareBy(
                    { "${it.id}" !in pinnedSources },
                    { "${it.name.lowercase()} (${it.lang})" },
                ),
            )
    }

    private fun getSelectedSources(): List<Source> {
        val enabledSources = getEnabledSources()

        val filter = extensionFilter
        if (filter.isNullOrEmpty()) {
            return enabledSources
        }

        return extensionManager.installedExtensionsFlow.value
            .filter { it.pkgName == filter }
            .flatMap { it.sources }
            .filter { it in enabledSources }
    }

    protected fun restoreSearch(initialQuery: String) {
        val filter = when (savedState?.get<String>("source_filter")) {
            "all" -> SourceFilter.All
            "pinned" -> SourceFilter.PinnedOnly
            "group" -> savedState.get<String>("source_group")?.let { SourceFilter.Group(it) } ?: SourceFilter.PinnedOnly
            else -> if (extensionFilter != null) SourceFilter.All else state.value.sourceFilter
        }
        val draft = if (savedState?.contains("draft_query") == true) {
            savedState.get<String>("draft_query")
        } else {
            initialQuery
        }
        mutableState.update { it.copy(searchQuery = draft, sourceFilter = filter) }
        search(savedState?.get<String>("submitted_query") ?: initialQuery)
    }

    fun updateSearchQuery(query: String?) {
        savedState?.set("draft_query", query)
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun setSourceFilter(filter: SourceFilter) {
        savedState?.set(
            "source_filter",
            when (filter) {
                SourceFilter.All -> "all"
                SourceFilter.PinnedOnly -> "pinned"
                is SourceFilter.Group -> "group"
            },
        )
        savedState?.set("source_group", (filter as? SourceFilter.Group)?.name)
        mutableState.update { it.copy(sourceFilter = filter) }
        search()
    }

    fun toggleFilterResults() {
        preferences.globalSearchFilterState.toggle()
    }

    fun search() = search(state.value.searchQuery)

    private fun search(query: String?) {
        if (query.isNullOrBlank()) return
        val sourceFilter = state.value.sourceFilter
        savedState?.set("submitted_query", query)

        val sameQuery = this.lastQuery == query
        if (sameQuery && this.lastSourceFilter == sourceFilter) return

        this.lastQuery = query
        this.lastSourceFilter = sourceFilter

        searchJob?.cancel()

        val sources = getSelectedSources()

        // Reuse previous results if possible
        if (sameQuery) {
            val existingResults = state.value.items
            updateItems(
                sources
                    .associateWith { existingResults[it] ?: SearchItemResult.Loading },
            )
        } else {
            updateItems(
                sources
                    .associateWith { SearchItemResult.Loading },
            )
        }

        val startNanos = System.nanoTime()
        val anyLoading = state.value.items.values.any { it is SearchItemResult.Loading }
        mutableState.update { it.copy(elapsedMillis = null) }

        searchJob = viewModelScope.launchIO {
            sources.map { source ->
                async {
                    if (state.value.items[source] !is SearchItemResult.Loading) {
                        return@async
                    }

                    try {
                        val page = withContext(coroutineDispatcher) {
                            source.withSourceHealth { source.getSearchManga(1, query, source.getFilterList()) }
                        }

                        val titles = page.mangas
                            .map { it.toDomainManga(source.id, source.isNovelSource()) }
                            .distinctBy { it.url }
                            .let { networkToLocalManga(it) }

                        if (isActive) {
                            updateItem(source, SearchItemResult.Success(titles))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (isActive) {
                            updateItem(source, SearchItemResult.Error(e))
                        }
                    } catch (e: LinkageError) {
                        // Outdated/incompatible extensions throw LinkageError subtypes
                        // (NoClassDefFoundError, NoSuchMethodError, etc.) rather than Exception.
                        // Catching only Exception let one broken source crash the whole global
                        // search instead of failing just that source.
                        if (isActive) {
                            updateItem(source, SearchItemResult.Error(e))
                        }
                    }
                }
            }
                .awaitAll()
            if (anyLoading) {
                val elapsed = GlobalSearchStats.elapsedMillis(startNanos, System.nanoTime())
                mutableState.update { it.copy(elapsedMillis = elapsed) }
            }
        }
    }

    private fun updateItems(items: Map<Source, SearchItemResult>) {
        mutableState.update {
            it.copy(
                items = items
                    .toSortedMap(sortComparator(items)),
            )
        }
    }

    private fun updateItem(source: Source, result: SearchItemResult) {
        if (source !in getSelectedSources()) return
        mutableState.update { current ->
            val items = current.items + (source to result)
            current.copy(items = items.toSortedMap(sortComparator(items)))
        }
    }

    fun setMigrateDialog(currentId: Long, target: Manga) {
        viewModelScope.launchIO {
            val current = getManga.await(currentId) ?: return@launchIO
            mutableState.update { it.copy(dialog = Dialog.Migrate(target, current)) }
        }
    }

    fun clearDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    @Immutable
    data class State(
        val from: Manga? = null,
        val searchQuery: String? = null,
        val sourceFilter: SourceFilter = SourceFilter.PinnedOnly,
        val pinGroups: List<String> = emptyList(),
        val onlyShowHasResults: Boolean = false,
        val items: Map<Source, SearchItemResult> = mapOf(),
        val dialog: Dialog? = null,
        /** Wall time of the last finished search (monotonic clock), null while one is running. */
        val elapsedMillis: Long? = null,
    ) {
        val resultCount: Int = GlobalSearchStats.resultCount(items.values)
        val progress: Int = items.count { it.value !is SearchItemResult.Loading }
        val total: Int = items.size
        val filteredItems = items.filter { (_, result) -> result.isVisible(onlyShowHasResults) }
    }

    sealed interface Dialog {
        data class Migrate(val target: Manga, val current: Manga) : Dialog
    }
}

sealed interface SourceFilter {
    data object All : SourceFilter
    data object PinnedOnly : SourceFilter
    data class Group(val name: String) : SourceFilter
}

sealed interface SearchItemResult {
    data object Loading : SearchItemResult

    data class Error(
        val throwable: Throwable,
    ) : SearchItemResult

    data class Success(
        val result: List<Manga>,
    ) : SearchItemResult {
        val isEmpty: Boolean
            get() = result.isEmpty()
    }

    fun isVisible(onlyShowHasResults: Boolean): Boolean {
        return !onlyShowHasResults || (this is Success && !this.isEmpty)
    }
}

/** Pure helpers for the global search "N results · 0.6 s" line. */
object GlobalSearchStats {
    fun resultCount(results: Collection<SearchItemResult>): Int =
        results.sumOf { (it as? SearchItemResult.Success)?.result?.size ?: 0 }

    /** Elapsed milliseconds between two [System.nanoTime] readings; never negative. */
    fun elapsedMillis(startNanos: Long, endNanos: Long): Long =
        ((endNanos - startNanos) / 1_000_000L).coerceAtLeast(0L)

    /** Seconds with one decimal using [locale]'s decimal separator. */
    fun formatSeconds(millis: Long, locale: java.util.Locale = java.util.Locale.getDefault()): String =
        String.format(locale, "%.1f", millis / 1000.0)
}
