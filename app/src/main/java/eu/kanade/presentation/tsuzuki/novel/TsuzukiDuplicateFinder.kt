package eu.kanade.presentation.tsuzuki.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.library.DeleteLibraryMangaDialog
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.ui.library.duplicate.DuplicateDetectionViewModel
import eu.kanade.tachiyomi.ui.library.duplicate.DuplicateDetectionViewModel.CategoryIncludeMode
import eu.kanade.tachiyomi.ui.library.duplicate.DuplicateDetectionViewModel.ContentType
import eu.kanade.tachiyomi.ui.library.duplicate.DuplicateDetectionViewModel.SortMode
import eu.kanade.tachiyomi.ui.library.duplicate.categorySelectionFor
import eu.kanade.tachiyomi.util.source.getMangaUrlOrNull
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.interactor.BlankTitleFilter
import tachiyomi.domain.manga.interactor.DuplicateMatchMode
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.core.common.i18n.stringResource as ctxStringResource

/**
 * Find duplicates in the Tsuzuki themes (board 08-tools "DuplicateFinder"). Same view model and the same actions as
 * the stock screen; only the presentation changes:
 *  - header with back circle, display title, "N groups · M books" (or "N selected") and a filter toggle;
 *  - match mode as the segmented control; "List all" is a pill in the filters;
 *  - the stock floating selection menu becomes a row of quick-select pills (same view model calls);
 *  - groups as tiles with covers, per-copy details and a rounded checkbox; select group / dismiss / collapse kept;
 *  - the selection bar at the bottom carries copy links, move to category and delete.
 * All states are kept: idle, scanning (with precheck progress), empty, no match for the filters, results, truncation
 * notices. Left out: the board's sample numbers and its filter icon opening a sheet (it toggles the filter pills).
 */
@Composable
fun TsuzukiDuplicateFinderScreen(
    screenModel: DuplicateDetectionViewModel,
    onBack: () -> Unit,
    onOpenSourcePriority: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onEditCategories: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val state by screenModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val hasSelection = state.selection.isNotEmpty()
    val hasGroups = state.filteredDuplicateGroups.isNotEmpty()

    Box(
        Modifier
            .fillMaxSize()
            .tsuzukiBackground(t),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (hasSelection) 120.dp else 32.dp),
        ) {
            item(key = "header") {
                val (groups, books) = duplicateTotals(state.filteredDuplicateGroups.values.map { it.size })
                val subtitle = when {
                    hasSelection -> stringResource(MR.strings.duplicate_n_selected, state.selection.size)
                    state.hasStartedAnalysis && !state.isLoading && hasGroups ->
                        stringResource(MR.strings.tsz_duplicates_subtitle, groups, books)
                    else -> null
                }
                Header(
                    subtitle = subtitle,
                    filtersOpen = filtersOpen,
                    onBack = onBack,
                    onToggleFilters = { filtersOpen = !filtersOpen },
                )
            }

            item(key = "match_mode") {
                TsuzukiSegments(
                    labels = listOf(
                        stringResource(MR.strings.duplicate_match_exact),
                        stringResource(MR.strings.duplicate_match_contains),
                        stringResource(MR.strings.duplicate_match_url),
                    ),
                    selected = duplicateSegmentIndex(state.matchMode),
                    onClick = { if (!state.listingMode) screenModel.setMatchMode(duplicateSegmentMode(it)) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .alpha(if (state.listingMode) 0.4f else 1f),
                )
            }

            if (hasGroups) {
                item(key = "quick_select") {
                    TsuzukiSectionLabel(stringResource(MR.strings.tsz_duplicates_quick_select))
                    QuickSelect(screenModel)
                }
            }

            if (state.listingMode && state.listingTruncated) {
                item(key = "listing_truncated") {
                    Notice(stringResource(MR.strings.duplicate_listing_truncated, state.listingTotalMatches))
                }
            }
            if (!state.listingMode && state.scanGroupsTruncated) {
                item(key = "scan_groups_truncated") {
                    Notice(
                        stringResource(
                            MR.strings.duplicate_scan_groups_truncated,
                            state.duplicateGroups.size,
                            state.scanTotalGroups,
                        ),
                    )
                }
            }

            if (state.hasStartedAnalysis && state.duplicateGroups.isNotEmpty()) {
                item(key = "search_field") {
                    SearchField(state.searchQuery, screenModel::setSearchQuery)
                }
            }

            if (filtersOpen) {
                item(key = "filters") { Filters(state, screenModel, onOpenSourcePriority) }
            }

            when {
                !state.hasStartedAnalysis -> item(key = "initial_state") {
                    Centered {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = t.accent,
                        )
                        Text(
                            t.case(stringResource(MR.strings.duplicate_initial_title)),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            stringResource(MR.strings.duplicate_initial_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        AccentButton(stringResource(MR.strings.duplicate_start_analysis), screenModel::loadDuplicates)
                    }
                }
                state.isLoading -> item(key = "loading_state") {
                    val progress = state.precheckProgress
                    if (progress != null) {
                        Centered {
                            TsuzukiProgressBar(
                                progress = progress.first.toFloat() / progress.second.coerceAtLeast(1),
                                modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
                            )
                            Text(
                                stringResource(
                                    TDMR.strings.duplicate_checking_progress,
                                    progress.first,
                                    progress.second,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LoadingScreen(modifier = Modifier.fillParentMaxWidth().height(280.dp))
                    }
                }
                !hasGroups -> item(key = "empty_state") {
                    Centered {
                        Text(
                            if (state.duplicateGroups.isEmpty()) {
                                stringResource(MR.strings.duplicate_no_duplicates)
                            } else {
                                stringResource(MR.strings.duplicate_no_matches_filter)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        AccentButton(stringResource(MR.strings.duplicate_reanalyze), screenModel::loadDuplicates)
                    }
                }
                else -> items(state.filteredDuplicateGroups.toList(), key = {
                    it.first
                }, contentType = { "duplicate_group" }) { (title, list) ->
                    val materialized = state.duplicateGroups[title]?.size ?: list.size
                    val scanFull = state.allGroupIds[title]?.size ?: materialized
                    GroupTile(
                        title = title,
                        list = list,
                        fullGroupCount = if (scanFull > materialized) scanFull else list.size,
                        canSelectHiddenTail = state.canIncludeHiddenTail,
                        selection = state.selection,
                        categories = state.mangaCategories,
                        showFullUrls = state.showFullUrls,
                        onToggle = screenModel::toggleSelection,
                        onSelectGroup = { screenModel.selectGroup(title) },
                        onDismiss = { screenModel.dismissGroup(title) },
                        onOpen = onOpenManga,
                    )
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        ) {
            SnackbarHost(snackbar)
            if (hasSelection) {
                SelectionBar(
                    count = state.selection.size,
                    onCopy = {
                        scope.launch {
                            val urls = screenModel.getSelectedUrls()
                            clipboard.setText(AnnotatedString(urls.joinToString("\n")))
                            snackbar.showSnackbar(
                                context.ctxStringResource(MR.strings.duplicate_urls_copied, urls.size),
                            )
                        }
                    },
                    onMove = screenModel::openMoveToCategoryDialog,
                    onRemove = screenModel::openDeleteDialog,
                )
            }
        }
    }

    if (state.showDeleteDialog) {
        DeleteLibraryMangaDialog(
            containsLocalManga = screenModel.selectionContainsLocalManga(),
            onDismissRequest = { screenModel.closeDeleteDialog() },
            onConfirm = {
                    removeFromLibrary,
                    deleteDownloads,
                    clearChaptersFromDb,
                    deleteTranslations,
                    clearCovers,
                    clearDescriptions,
                    clearTags,
                ->
                val count = state.selection.size
                scope.launch {
                    screenModel.deleteSelected(
                        removeFromLibrary = removeFromLibrary,
                        deleteDownloads = deleteDownloads,
                        clearChaptersFromDb = clearChaptersFromDb,
                        deleteTranslations = deleteTranslations,
                        clearCovers = clearCovers,
                        clearDescriptions = clearDescriptions,
                        clearTags = clearTags,
                    )
                    snackbar.showSnackbar(context.ctxStringResource(MR.strings.duplicate_deleted_count, count))
                }
            },
        )
    }

    if (state.showMoveToCategoryDialog) {
        ChangeCategoryDialog(
            initialSelection = remember(state.selection, state.mangaCategoryIdSets, state.categories) {
                categorySelectionFor(state)
            },
            onDismissRequest = { screenModel.closeMoveToCategoryDialog() },
            onEditCategories = {
                screenModel.closeMoveToCategoryDialog()
                onEditCategories()
            },
            onConfirm = { addCategories, removeCategories ->
                if (addCategories.isNotEmpty() || removeCategories.isNotEmpty()) {
                    val count = state.selection.size
                    scope.launch {
                        val success = screenModel.moveSelectedToCategories(addCategories, removeCategories)
                        val message = if (success) {
                            context.ctxStringResource(MR.strings.duplicate_moved_count, count)
                        } else {
                            context.ctxStringResource(MR.strings.duplicate_move_failed)
                        }
                        snackbar.showSnackbar(message)
                    }
                }
            },
        )
    }
}

@Composable
private fun Header(subtitle: String?, filtersOpen: Boolean, onBack: () -> Unit, onToggleFilters: () -> Unit) {
    val t = TsuzukiTheme.tokens
    Row(
        Modifier
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(
            Icons.AutoMirrored.Outlined.ArrowBack,
            stringResource(MR.strings.action_webview_back),
            onClick = onBack,
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                t.case(stringResource(MR.strings.duplicate_find_duplicates)),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, lineHeight = 36.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    t.case(subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RoundButton(
            Icons.Outlined.FilterList,
            stringResource(MR.strings.duplicate_filters_sort),
            active = filtersOpen,
            onClick = onToggleFilters,
        )
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit, active: Boolean = false) {
    val t = TsuzukiTheme.tokens
    val border = t.tileBorder
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) t.accent else t.tile)
            .then(if (!active && border != null) Modifier.border(1.dp, border, CircleShape) else Modifier)
            .tsuzukiClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (active) t.onAccent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Notice(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

@Composable
private fun AccentButton(text: String, onClick: () -> Unit) {
    val t = TsuzukiTheme.tokens
    Row(
        Modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(t.accent)
            .tsuzukiClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = t.onAccent, modifier = Modifier.size(20.dp))
        Text(
            t.case(text),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
            color = t.onAccent,
        )
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(24.dp)
    val border = t.tileBorder
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(shape)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier),
        placeholder = { Text(stringResource(MR.strings.action_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { onChange("") },
                    Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(MR.strings.tsz_a11y_clear))
                }
            }
        },
        singleLine = true,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = t.tile,
            unfocusedContainerColor = t.tile,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = t.accent,
        ),
    )
}

/** The stock floating selection menu, as pills; each calls the same view model function. */
@Composable
private fun QuickSelect(m: DuplicateDetectionViewModel) {
    val actions: List<Pair<String, () -> Unit>> = listOf(
        stringResource(MR.strings.duplicate_select_all) to m::selectAllDuplicates,
        stringResource(MR.strings.duplicate_select_all_except_first) to m::selectAllExceptFirst,
        stringResource(MR.strings.duplicate_select_lowest_ch) to m::selectLowestChapterCount,
        stringResource(MR.strings.duplicate_select_highest_ch) to m::selectHighestChapterCount,
        stringResource(MR.strings.duplicate_select_highest_dl) to m::selectHighestDownloadCount,
        stringResource(MR.strings.duplicate_select_lowest_dl) to m::selectLowestDownloadCount,
        stringResource(MR.strings.duplicate_select_highest_read) to m::selectHighestReadCount,
        stringResource(MR.strings.duplicate_select_lowest_read) to m::selectLowestReadCount,
        stringResource(MR.strings.duplicate_select_lowest_priority) to m::selectLowestSourcePriority,
        stringResource(MR.strings.duplicate_select_highest_priority) to m::selectHighestSourcePriority,
        stringResource(MR.strings.duplicate_select_pinned) to m::selectPinnedInGroups,
        stringResource(MR.strings.duplicate_select_non_pinned) to m::selectNonPinnedInGroups,
        stringResource(MR.strings.duplicate_invert_selection) to m::invertSelection,
        stringResource(MR.strings.duplicate_clear_selection) to m::clearSelection,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(actions, key = { it.first }, contentType = { "action_pill" }) { (label, action) ->
            TsuzukiPill(text = label, selected = false, onClick = action, role = Role.Button)
        }
    }
}

@Composable
private fun OptionPill(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    TsuzukiPill(
        text = text,
        selected = selected,
        onClick = { if (enabled) onClick() },
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
        role = Role.Button,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PillGroup(label: String?, content: @Composable () -> Unit) {
    if (label != null) TsuzukiSectionLabel(label)
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (label == null) 8.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Filters(
    state: DuplicateDetectionViewModel.State,
    m: DuplicateDetectionViewModel,
    onOpenSourcePriority: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    Column {
        PillGroup(stringResource(MR.strings.tsz_duplicates_options)) {
            OptionPill(stringResource(MR.strings.duplicate_listing_mode), state.listingMode) {
                m.setListingMode(!state.listingMode)
            }
            OptionPill(stringResource(TDMR.strings.duplicate_full_urls_short), state.showFullUrls) {
                m.toggleShowFullUrls()
            }
            OptionPill(stringResource(TDMR.strings.duplicate_flexible_group_matching), state.filterByGroupCategory) {
                m.setFilterByGroupCategory(!state.filterByGroupCategory)
            }
            OptionPill(stringResource(TDMR.strings.duplicate_apply_library_filters), state.applyLibraryFilters) {
                m.setApplyLibraryFilters(!state.applyLibraryFilters)
            }
        }
        PillGroup(stringResource(MR.strings.tsz_duplicates_type)) {
            OptionPill(stringResource(MR.strings.duplicate_type_all), state.contentType == ContentType.ALL) {
                m.setContentType(ContentType.ALL)
            }
            OptionPill(stringResource(MR.strings.duplicate_type_manga), state.contentType == ContentType.MANGA) {
                m.setContentType(ContentType.MANGA)
            }
            OptionPill(stringResource(MR.strings.duplicate_type_novel), state.contentType == ContentType.NOVEL) {
                m.setContentType(ContentType.NOVEL)
            }
        }
        val blankEnabled = !state.listingMode && state.matchMode != DuplicateMatchMode.CONTAINS
        PillGroup(stringResource(MR.strings.tsz_duplicates_blank)) {
            OptionPill(
                stringResource(MR.strings.duplicate_blank_exclude),
                state.blankTitleFilter == BlankTitleFilter.EXCLUDE,
                blankEnabled,
                { m.setBlankTitleFilter(BlankTitleFilter.EXCLUDE) },
            )
            OptionPill(
                stringResource(MR.strings.duplicate_blank_include),
                state.blankTitleFilter == BlankTitleFilter.INCLUDE,
                blankEnabled,
                { m.setBlankTitleFilter(BlankTitleFilter.INCLUDE) },
            )
        }
        PillGroup(stringResource(MR.strings.tsz_duplicates_sort)) {
            listOf(
                SortMode.NAME to stringResource(MR.strings.duplicate_sort_name),
                SortMode.LATEST_ADDED to stringResource(MR.strings.duplicate_sort_latest),
                SortMode.CHAPTER_COUNT_DESC to stringResource(MR.strings.duplicate_sort_ch_desc),
                SortMode.DOWNLOAD_COUNT_DESC to stringResource(MR.strings.duplicate_sort_dl_desc),
                SortMode.READ_COUNT_DESC to stringResource(MR.strings.duplicate_sort_read_desc),
                SortMode.PINNED_SOURCE to stringResource(MR.strings.tsz_duplicates_sort_pinned),
                SortMode.SOURCE_PRIORITY to stringResource(MR.strings.duplicate_sort_priority),
            ).forEach { (mode, label) ->
                OptionPill(label, state.sortMode == mode) { m.setSortMode(mode) }
            }
        }
        if (state.categories.isNotEmpty()) {
            val relevant = state.categories.filter { category ->
                when (state.contentType) {
                    ContentType.ALL -> true
                    ContentType.NOVEL ->
                        category.contentType == Category.CONTENT_TYPE_ALL ||
                            category.contentType == Category.CONTENT_TYPE_NOVEL
                    ContentType.MANGA ->
                        category.contentType == Category.CONTENT_TYPE_ALL ||
                            category.contentType == Category.CONTENT_TYPE_MANGA
                }
            }
            PillGroup(stringResource(MR.strings.tsz_duplicates_category)) {
                OptionPill(
                    stringResource(TDMR.strings.duplicate_category_include_or),
                    state.categoryIncludeMode == CategoryIncludeMode.ANY,
                    onClick = { m.setCategoryIncludeMode(CategoryIncludeMode.ANY) },
                )
                OptionPill(
                    stringResource(TDMR.strings.duplicate_category_include_and),
                    state.categoryIncludeMode == CategoryIncludeMode.ALL,
                    onClick = { m.setCategoryIncludeMode(CategoryIncludeMode.ALL) },
                )
                if (state.selectedCategoryFilters.isNotEmpty() || state.excludedCategoryFilters.isNotEmpty()) {
                    OptionPill(
                        stringResource(MR.strings.duplicate_category_clear),
                        true,
                        onClick = m::clearCategoryFilters,
                    )
                }
            }
            val defaultLabel = stringResource(MR.strings.label_default)
            PillGroup(null) {
                relevant.forEach { category ->
                    val included = category.id in state.selectedCategoryFilters
                    val excluded = category.id in state.excludedCategoryFilters
                    val name = category.name.ifBlank { defaultLabel }
                    OptionPill(
                        text = when {
                            included -> "✓ $name"
                            excluded -> "✕ $name"
                            else -> name
                        },
                        selected = included || excluded,
                        onClick = { m.toggleCategoryFilter(category.id) },
                    )
                }
            }
        }
        val shape = RoundedCornerShape(t.tileRadius)
        val border = t.tileBorder
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(t.tile)
                .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
                .tsuzukiClickable(role = Role.Button, onClick = onOpenSourcePriority)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                t.case(stringResource(MR.strings.duplicate_source_priority)),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = t.accent)
        }
    }
}

@Composable
private fun GroupTile(
    title: String,
    list: List<MangaWithChapterCount>,
    fullGroupCount: Int,
    canSelectHiddenTail: Boolean,
    selection: Set<Long>,
    categories: Map<Long, List<Category>>,
    showFullUrls: Boolean,
    onToggle: (Long) -> Unit,
    onSelectGroup: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val t = TsuzukiTheme.tokens
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    val allSelected = list.isNotEmpty() && list.all { it.manga.id in selection }
    val shape = RoundedCornerShape(t.tileRadius)
    val border = t.tileBorder
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .tsuzukiClickable(
                    onClickLabel = stringResource(
                        if (expanded) MR.strings.action_collapse else MR.strings.action_expand,
                    ),
                    role = Role.Button,
                    onClick = { expanded = !expanded },
                )
                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(
                    t.case(title.replaceFirstChar { it.uppercaseChar() }),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (fullGroupCount > list.size) {
                    Text(
                        if (canSelectHiddenTail) {
                            stringResource(MR.strings.duplicate_group_truncated_selectable, list.size, fullGroupCount)
                        } else {
                            stringResource(MR.strings.duplicate_group_truncated_filtered, list.size, fullGroupCount)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                t.case(stringResource(MR.strings.tsz_duplicates_copies, list.size)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            TileIcon(
                if (allSelected) Icons.Outlined.CheckBox else Icons.Outlined.SelectAll,
                if (allSelected) {
                    stringResource(TDMR.strings.duplicate_deselect_group)
                } else {
                    stringResource(MR.strings.duplicate_select_group)
                },
                tint = t.accent,
                onClick = onSelectGroup,
            )
            TileIcon(
                Icons.Filled.Close,
                stringResource(TDMR.strings.duplicate_dismiss_group),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onDismiss,
            )
        }
        if (expanded) {
            list.forEachIndexed { index, item ->
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (t.isDot) 0.14f else 0.08f),
                )
                CopyRow(
                    manga = item,
                    categories = categories[item.manga.id].orEmpty(),
                    selected = item.manga.id in selection,
                    original = index == 0,
                    showFullUrl = showFullUrls,
                    onToggle = { onToggle(item.manga.id) },
                    onOpen = { onOpen(item.manga.id) },
                )
            }
        }
    }
}

@Composable
private fun TileIcon(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).tsuzukiClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

@Composable
private fun CopyRow(
    manga: MangaWithChapterCount,
    categories: List<Category>,
    selected: Boolean,
    original: Boolean,
    showFullUrl: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val sourceManager: SourceManager = remember { Injekt.get() }
    val downloadManager: DownloadManager = remember { Injekt.get() }
    val source = remember(manga.manga.source) { sourceManager.getOrStub(manga.manga.source) }
    val sourceName = remember(source) { source.getNameForMangaInfo() }
    val downloaded = remember(manga.manga.id) { downloadManager.getDownloadCount(manga.manga) }
    val parts = buildList {
        add(stringResource(MR.strings.duplicate_n_chapters, manga.chapterCount))
        duplicateStats(downloaded, manga.readCount).forEach {
            add(
                when (it) {
                    DuplicateStat.DOWNLOADS -> stringResource(MR.strings.duplicate_n_downloads, downloaded)
                    DuplicateStat.READ -> stringResource(MR.strings.duplicate_n_read, manga.readCount.toInt())
                },
            )
        }
        add(sourceName)
        manga.manga.author?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .tsuzukiClickable(role = Role.Button, onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Book(
            data = manga.manga,
            modifier = Modifier.width(48.dp),
            shape = RoundedCornerShape(6.dp),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                t.case(manga.manga.title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (original) t.accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (original) {
                Text(
                    stringResource(MR.strings.duplicate_original),
                    style = MaterialTheme.typography.labelSmall,
                    color = t.accent,
                )
            }
            Text(
                parts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (categories.isNotEmpty()) {
                val defaultLabel = stringResource(MR.strings.label_default)
                Text(
                    stringResource(
                        MR.strings.duplicate_categories_label,
                        categories.joinToString(", ") { it.name.ifBlank { defaultLabel } },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showFullUrl) {
                val url = remember(manga.manga.url, source) {
                    source.getMangaUrlOrNull(manga.manga.toSManga()) ?: manga.manga.url
                }
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    color = t.accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val box = RoundedCornerShape(8.dp)
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Checkbox, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(box)
                    .then(
                        if (selected) {
                            Modifier.background(t.accent)
                        } else {
                            Modifier.border(2.dp, muted, box)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = t.onAccent)
            }
        }
    }
}

@Composable
private fun SelectionBar(count: Int, onCopy: () -> Unit, onMove: () -> Unit, onRemove: () -> Unit) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(32.dp)
    val border = t.tileBorder
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            t.case(stringResource(MR.strings.duplicate_n_selected, count)),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TileIcon(
            Icons.Filled.ContentCopy,
            stringResource(MR.strings.duplicate_copy_links),
            MaterialTheme.colorScheme.onSurface,
            onCopy,
        )
        BarButton(stringResource(MR.strings.tsz_duplicates_move), accent = false, onClick = onMove)
        BarButton(stringResource(MR.strings.tsz_duplicates_remove), accent = true, onClick = onRemove)
    }
}

@Composable
private fun BarButton(text: String, accent: Boolean, onClick: () -> Unit) {
    val t = TsuzukiTheme.tokens
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (accent) t.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .tsuzukiClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            t.case(text),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
            color = if (accent) t.onAccent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

private fun TsuzukiTokens.case(s: String) = if (isDot) s.uppercase() else s
