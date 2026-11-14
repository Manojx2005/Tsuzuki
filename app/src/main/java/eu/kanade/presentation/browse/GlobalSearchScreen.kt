package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.browse.TsuzukiGlobalSearchSummary
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.nameWithTypeTag
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchViewModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
fun GlobalSearchScreen(
    state: SearchViewModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                hideSourceFilter = false,
                sourceFilter = state.sourceFilter,
                pinGroups = state.pinGroups,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchContent(
            items = state.filteredItems,
            operationKey = state.searchQuery,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
            header = if (TsuzukiTheme.tokens.style != TsuzukiStyle.NONE) {
                { TsuzukiGlobalSearchSummary(state.resultCount, state.elapsedMillis) }
            } else {
                null
            },
        )
    }
}

@Composable
internal fun GlobalSearchContent(
    items: Map<Source, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    fromSourceId: Long? = null,
    header: (@Composable () -> Unit)? = null,
    operationKey: Any? = null,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        header?.let { item(key = "tsz-summary") { it() } }
        items.forEach { (source, result) ->
            item(key = source.id) {
                val name = source.nameWithTypeTag()
                GlobalSearchResultItem(
                    title = fromSourceId?.let { "▶ $name".takeIf { source.id == fromSourceId } } ?: name,
                    subtitle = LocaleHelper.getLocalizedDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                    modifier = Modifier.animateItem(),
                ) {
                    when (result) {
                        SearchItemResult.Loading -> {
                            if (eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle()) {
                                eu.kanade.presentation.tsuzuki.components.TsuzukiLoadingStages(
                                    what = source.name,
                                    steps = listOf(
                                        tachiyomi.presentation.core.i18n.stringResource(
                                            tachiyomi.i18n.MR.strings.tsz_error_fetch,
                                        ),
                                    ),
                                    currentStep = 0,
                                    operationKey = operationKey to source.id,
                                    onRetry = {
                                        onClickSource(source)
                                    },
                                )
                            } else {
                                GlobalSearchLoadingResultItem()
                            }
                        }
                        is SearchItemResult.Success -> {
                            GlobalSearchCardRow(
                                titles = result.result,
                                getManga = getManga,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is SearchItemResult.Error -> {
                            if (eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle()) {
                                eu.kanade.presentation.tsuzuki.components.TsuzukiErrorState(
                                    error = result.throwable,
                                    whatFailed = source.name,
                                    onRetry = {
                                        onClickSource(source)
                                    },
                                    onExtensions = eu.kanade.presentation.tsuzuki.components.extensionsAction(),
                                )
                            } else {
                                GlobalSearchErrorResultItem(message = result.throwable.message)
                            }
                        }
                    }
                }
            }
        }
    }
}
