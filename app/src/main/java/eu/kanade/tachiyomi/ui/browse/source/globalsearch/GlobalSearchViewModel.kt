package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.isNovelSource

class GlobalSearchViewModel(
    initialQuery: String,
    initialExtensionFilter: String?,
    savedState: SavedStateHandle? = null,
) : SearchViewModel(State(searchQuery = initialQuery), savedState = savedState) {

    companion object {
        val INITIAL_QUERY_KEY = CreationExtras.Key<String>()
        val INITIAL_EXTENSION_FILTER_KEY = CreationExtras.Key<String?>()

        val Factory = viewModelFactory {
            initializer {
                GlobalSearchViewModel(
                    initialQuery = get(INITIAL_QUERY_KEY)!!,
                    initialExtensionFilter = get(INITIAL_EXTENSION_FILTER_KEY),
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }

    init {
        extensionFilter = initialExtensionFilter
        restoreSearch(initialQuery)
    }

    override fun getEnabledSources(): List<Source> {
        val filter = state.value.sourceFilter
        return super.getEnabledSources()
            .filterNot { it.isNovelSource() } // Exclude novel sources from manga global search
            .filter {
                when (filter) {
                    SourceFilter.All -> true
                    SourceFilter.PinnedOnly -> "${it.id}" in pinnedSources
                    is SourceFilter.Group -> "${it.id}" in sourceGroups.getOrElse(filter.name) { emptySet() }
                }
            }
    }
}
