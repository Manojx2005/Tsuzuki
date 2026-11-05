package eu.kanade.presentation.tsuzuki.updates

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import eu.kanade.presentation.updates.UpdatesUiModel
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesFilter
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesNovelGroup
import eu.kanade.tachiyomi.ui.updates.UpdatesViewModel
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Board "TabletUpdates": on tablets in the Tsuzuki themes the Updates list sits on the left and a preview of the
 * selected update on the right. Uses the same width test as the rest of the tablet UI (`isTabletUi()`, smallest
 * width >= 720 dp), so tablet portrait gets it too. Phones and stock themes keep the stock list.
 */
@Composable
fun tsuzukiUpdatesTwoPaneEnabled(): Boolean =
    TsuzukiTheme.tokens.style != TsuzukiStyle.NONE && isTabletUi()

/** The row the preview shows: the one with [key], else the first. */
internal fun <T> resolveUpdatesSelection(rows: List<T>, key: Long?, keyOf: (T) -> Long): T? =
    rows.firstOrNull { keyOf(it) == key } ?: rows.firstOrNull()

/** The other new chapters of the same book as [selected], in list order. */
internal fun otherNewChapters(items: List<UpdatesItem>, selected: UpdatesItem): List<UpdatesItem> =
    items.filter { it.update.mangaId == selected.update.mangaId && it.update.chapterId != selected.update.chapterId }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TsuzukiUpdatesTwoPane(
    state: UpdatesViewModel.State,
    contentPadding: PaddingValues,
    lastUpdated: Long,
    showFilterChips: Boolean,
    showAllFilter: Boolean,
    showMangaFilter: Boolean,
    onFilterSelected: (UpdatesFilter) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onOpenChapter: (UpdatesItem) -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onClickNovelGroup: (Long) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = TsuzukiTheme.tokens
    // Item rows are keyed by chapter id, grouped rows by manga id.
    var selectedKey by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingSave by remember { mutableStateOf<List<UpdatesItem>?>(null) }
    val groups = remember(state.items, state.groupByNovel) {
        if (state.groupByNovel) state.getNovelGroups() else emptyList()
    }
    val selectedItem = if (state.groupByNovel) {
        null
    } else {
        resolveUpdatesSelection(state.items, selectedKey) { it.update.chapterId }
    }
    val selectedGroup = if (state.groupByNovel) {
        resolveUpdatesSelection(groups, selectedKey) { it.mangaId }
    } else {
        null
    }

    Row(
        modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(start = 8.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val listState = rememberLazyListState()
        LaunchedEffect(listState) {
            snapshotFlow {
                val info = listState.layoutInfo
                (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
            }.collect { (last, total) -> if (total > 0 && last >= total - 3) onLoadMore() }
        }
        LazyColumn(
            modifier = Modifier.weight(0.46f).fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            if (showFilterChips) {
                item(key = "filters") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (showAllFilter) {
                            TsuzukiPill(
                                text = stringResource(MR.strings.all),
                                selected = state.filter == UpdatesFilter.ALL,
                                onClick = { onFilterSelected(UpdatesFilter.ALL) },
                            )
                        }
                        if (showMangaFilter) {
                            TsuzukiPill(
                                text = stringResource(TDMR.strings.label_manga),
                                selected = state.filter == UpdatesFilter.MANGA,
                                onClick = { onFilterSelected(UpdatesFilter.MANGA) },
                            )
                        }
                        TsuzukiPill(
                            text = stringResource(TDMR.strings.label_novels),
                            selected = state.filter == UpdatesFilter.NOVELS,
                            onClick = { onFilterSelected(UpdatesFilter.NOVELS) },
                        )
                    }
                }
            }
            if (state.items.isNotEmpty()) {
                item(key = "tiles") {
                    val unread = state.items.filter { !it.update.read }
                    val toSave = unread.filter { it.downloadStateProvider() == Download.State.NOT_DOWNLOADED }
                    TsuzukiUpdatesTiles(
                        newCount = unread.size,
                        toSave = toSave.size,
                        onSaveAll = { onDownloadChapter(toSave, ChapterDownloadAction.START) },
                    )
                }
            }
            if (state.items.isEmpty()) {
                item(key = "empty") { EmptyUpdates(Modifier.fillMaxWidth().padding(vertical = 64.dp)) }
            } else if (state.groupByNovel) {
                items(groups, key = { "group-${it.mangaId}" }) { group ->
                    UpdateRow(
                        title = group.mangaTitle,
                        subtitle = listOfNotNull(
                            stringResource(MR.strings.tsz_updates_new_count, group.chapterCount),
                            group.chapters.maxByOrNull { it.update.dateFetch }?.update?.chapterName,
                        ).joinToString(" · "),
                        cover = group.coverUrl,
                        read = !group.hasUnreadChapters,
                        saved = group.chapters.all { it.downloadStateProvider() == Download.State.DOWNLOADED },
                        highlighted = group.mangaId == selectedGroup?.mangaId,
                        selectedForAction = false,
                        onClick = { selectedKey = group.mangaId },
                        onLongClick = null,
                    )
                }
            } else {
                items(
                    state.getUiModel(),
                    key = {
                        when (it) {
                            is UpdatesUiModel.Header -> "header-${it.hashCode()}"
                            is UpdatesUiModel.Item -> "update-${it.item.update.chapterId}-${it.item.update.dateFetch}"
                        }
                    },
                ) { model ->
                    when (model) {
                        is UpdatesUiModel.Header -> TsuzukiSectionLabel(
                            text = relativeDateText(model.date),
                            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 6.dp),
                        )
                        is UpdatesUiModel.Item -> {
                            val item = model.item
                            UpdateRow(
                                title = item.update.mangaTitle,
                                subtitle = item.update.chapterName,
                                cover = item.update.coverData,
                                read = item.update.read,
                                saved = item.downloadStateProvider() == Download.State.DOWNLOADED,
                                highlighted = !state.selectionMode &&
                                    item.update.chapterId == selectedItem?.update?.chapterId,
                                selectedForAction = item.selected,
                                onClick = {
                                    if (state.selectionMode) {
                                        onUpdateSelected(item, !item.selected, false)
                                    } else {
                                        selectedKey = item.update.chapterId
                                    }
                                },
                                onLongClick = { onUpdateSelected(item, !item.selected, true) },
                            )
                        }
                    }
                }
                item(key = "last-updated") {
                    Text(
                        stringResource(MR.strings.updates_last_update_info, relativeTimeSpanString(lastUpdated)),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val shape = RoundedCornerShape(t.tileRadius)
        Box(
            Modifier
                .weight(0.54f)
                .fillMaxSize()
                .clip(shape)
                .background(t.tile)
                .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier),
        ) {
            when {
                selectedItem != null -> ChapterPreview(
                    item = selectedItem,
                    others = otherNewChapters(state.items, selectedItem),
                    onSelectOther = { selectedKey = it.update.chapterId },
                    onRead = { onOpenChapter(selectedItem) },
                    onMarkRead = { onMultiMarkAsReadClicked(listOf(selectedItem), !selectedItem.update.read) },
                    onDownload = { onDownloadChapter(listOf(selectedItem), ChapterDownloadAction.START) },
                    onOpenBook = { onClickCover(selectedItem) },
                )
                selectedGroup != null -> GroupPreview(
                    group = selectedGroup,
                    onOpenChapter = onOpenChapter,
                    onOpenBook = { onClickNovelGroup(selectedGroup.mangaId) },
                    onMarkAllRead = { onMultiMarkAsReadClicked(selectedGroup.chapters, true) },
                    onSaveAll = {
                        val toSave = selectedGroup.chapters.filter {
                            it.downloadStateProvider() == Download.State.NOT_DOWNLOADED
                        }
                        // One tap shouldn't silently queue hundreds of chapters.
                        if (toSave.size > LARGE_DOWNLOAD_CONFIRM) {
                            pendingSave = toSave
                        } else {
                            onDownloadChapter(toSave, ChapterDownloadAction.START)
                        }
                    },
                )
                else -> EmptyUpdates(Modifier.fillMaxSize())
            }
        }
    }
    pendingSave?.let { chapters ->
        AlertDialog(
            onDismissRequest = { pendingSave = null },
            text = { Text(stringResource(MR.strings.tsz_updates_confirm_download, chapters.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingSave = null
                        onDownloadChapter(chapters, ChapterDownloadAction.START)
                    },
                ) { Text(stringResource(MR.strings.action_download)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingSave = null }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}

private const val LARGE_DOWNLOAD_CONFIRM = 25

@Composable
private fun EmptyUpdates(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            stringResource(MR.strings.information_no_recent),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpdateRow(
    title: String,
    subtitle: String,
    cover: Any?,
    read: Boolean,
    saved: Boolean,
    highlighted: Boolean,
    selectedForAction: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val shape = RoundedCornerShape(22.dp)
    val press = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier
            .tsuzukiPress(press)
            .padding(vertical = 3.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(
                when {
                    highlighted -> Modifier.border(2.dp, t.accent, shape)
                    t.tileBorder != null -> Modifier.border(1.dp, t.tileBorder, shape)
                    else -> Modifier
                },
            )
            .selectedBackground(selectedForAction)
            .combinedClickable(
                interactionSource = press,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        it()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
            )
            .padding(10.dp)
            .alpha(if (read && !saved) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Book(data = cover, modifier = Modifier.width(44.dp).height(62.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                up(title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                up(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            !read -> StatusPill(up(stringResource(MR.strings.tsz_updates_new)), filled = true)
            saved -> StatusPill(up(stringResource(MR.strings.tsz_updates_saved)), filled = false)
            else -> Text(
                up(stringResource(MR.strings.tsz_read)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, filled: Boolean) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(t.accent)
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, shape)
                },
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (filled) t.onAccent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterPreview(
    item: UpdatesItem,
    others: List<UpdatesItem>,
    onSelectOther: (UpdatesItem) -> Unit,
    onRead: () -> Unit,
    onMarkRead: () -> Unit,
    onDownload: () -> Unit,
    onOpenBook: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val update = item.update
    val sourceName = remember(update.sourceId) { Injekt.get<SourceManager>().getOrStub(update.sourceId).name }
    val downloadState = item.downloadStateProvider()
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                MangaCover.Book(
                    data = update.coverData,
                    modifier = Modifier.width(96.dp),
                    onClick = onOpenBook,
                )
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    PreviewLabel(up(stringResource(MR.strings.tsz_updates_preview, update.mangaTitle)))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        up(update.chapterName),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        up(
                            listOfNotNull(
                                relativeTimeSpanString(update.dateFetch),
                                sourceName,
                                update.scanlator?.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (others.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                PreviewLabel(up(stringResource(MR.strings.tsz_updates_more_new, others.size)))
                Spacer(Modifier.height(4.dp))
                others.forEach { other -> OtherChapterRow(other, onClick = { onSelectOther(other) }) }
            }
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionPill(
                text = up(stringResource(MR.strings.tsz_updates_read_chapter)),
                primary = true,
                icon = true,
                onClick = onRead,
            )
            ActionPill(
                text = up(
                    stringResource(
                        if (update.read) MR.strings.action_mark_as_unread else MR.strings.action_mark_as_read,
                    ),
                ),
                onClick = onMarkRead,
            )
            when (downloadState) {
                Download.State.DOWNLOADED -> ActionPill(
                    text = up(stringResource(MR.strings.tsz_updates_saved)),
                    onClick = {},
                    enabled = false,
                )
                Download.State.NOT_DOWNLOADED, Download.State.ERROR -> ActionPill(
                    text = up(stringResource(MR.strings.action_download)),
                    onClick = onDownload,
                )
                else -> Unit
            }
            ActionPill(text = up(stringResource(MR.strings.tsz_updates_open_book)), onClick = onOpenBook)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupPreview(
    group: UpdatesNovelGroup,
    onOpenChapter: (UpdatesItem) -> Unit,
    onOpenBook: () -> Unit,
    onMarkAllRead: () -> Unit,
    onSaveAll: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val toSave = group.chapters.any { it.downloadStateProvider() == Download.State.NOT_DOWNLOADED }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                MangaCover.Book(data = group.coverUrl, modifier = Modifier.width(96.dp), onClick = onOpenBook)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    PreviewLabel(up(stringResource(MR.strings.tsz_updates_preview, group.mangaTitle)))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        up(group.mangaTitle),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        up(stringResource(MR.strings.tsz_updates_new_count, group.chapterCount)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            PreviewLabel(up(stringResource(MR.strings.tsz_chapters)))
            Spacer(Modifier.height(4.dp))
            group.chapters.forEach { OtherChapterRow(it, onClick = { onOpenChapter(it) }) }
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionPill(
                text = up(stringResource(MR.strings.tsz_updates_open_book)),
                primary = true,
                icon = true,
                onClick = onOpenBook,
            )
            if (group.hasUnreadChapters) {
                ActionPill(text = up(stringResource(MR.strings.action_mark_as_read)), onClick = onMarkAllRead)
            }
            if (toSave) {
                ActionPill(text = up(stringResource(MR.strings.action_download)), onClick = onSaveAll)
            }
        }
    }
}

@Composable
private fun PreviewLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        color = TsuzukiTheme.tokens.accent,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun OtherChapterRow(item: UpdatesItem, onClick: () -> Unit) {
    val t = TsuzukiTheme.tokens
    val update = item.update
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (t.isDot) update.chapterName.uppercase() else update.chapterName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (update.read) 0.55f else 1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!update.read) {
            Box(Modifier.padding(start = 8.dp).size(8.dp).clip(RoundedCornerShape(50)).background(t.accent))
        }
    }
}

@Composable
private fun ActionPill(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    icon: Boolean = false,
    enabled: Boolean = true,
) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(50)
    val press = remember { MutableInteractionSource() }
    val ink = if (primary) t.onAccent else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .tsuzukiPress(press)
            .clip(shape)
            .then(
                when {
                    primary -> Modifier.background(t.accent)
                    t.tileBorder != null -> Modifier.border(1.dp, t.tileBorder, shape)
                    else -> Modifier.background(t.track)
                },
            )
            .combinedClickable(
                enabled = enabled,
                interactionSource = press,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.6f)
            .height(48.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon) Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ink, maxLines = 1)
    }
}
