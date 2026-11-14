package eu.kanade.presentation.tsuzuki.download

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile
import eu.kanade.presentation.tsuzuki.components.errorHeading
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.download.DownloadHeaderItem
import eu.kanade.tachiyomi.ui.download.DownloadItem
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import logcat.LogPriority
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.domain.manga.model.MangaCover as CoverData

@Composable
internal fun rememberMangaQueueStatuses(
    groups: List<DownloadHeaderItem>,
    observe: Boolean,
): Map<Long, Download.State> {
    if (!observe) return emptyMap()
    val downloads = remember(groups) { groups.flatMap { it.subItems.map(DownloadItem::download) } }
    if (downloads.isEmpty()) return emptyMap()
    val initial = remember(downloads) { downloads.associate { it.chapterId to it.status } }
    val statuses = remember(downloads) {
        combine(downloads.map { it.statusFlow }) { values ->
            downloads.indices.associate { downloads[it].chapterId to values[it] }
        }
    }
    return statuses.collectAsState(initial).value
}

@Composable
internal fun TsuzukiMangaDownloadQueue(
    groups: List<DownloadHeaderItem>,
    canReorder: Boolean,
    contentPadding: PaddingValues,
    onReorder: (List<Download>) -> Unit,
    onCancel: (List<Download>) -> Unit,
    coverFor: suspend (Long) -> CoverData?,
    pausedSources: Set<Long>,
    onPauseSource: (Long, Boolean) -> Unit,
    onRetry: (Download) -> Unit,
) {
    val bySource = remember(groups) { groups.associate { it.id to it.subItems.map(DownloadItem::download) } }
    val downloads = remember(bySource) { bySource.values.flatten() }
    val downloadsById = remember(downloads) { downloads.associateBy { it.chapterId } }
    val coverIds = remember(downloads) { downloads.mapTo(linkedSetOf()) { it.mangaId } }
    val covers = remember { mutableStateMapOf<Long, CoverData?>() }
    LaunchedEffect(coverIds) {
        covers.keys.toList().filterNot { it in coverIds }.forEach { covers.remove(it) }
        coverIds.forEach { mangaId ->
            if (!covers.containsKey(mangaId)) {
                covers[mangaId] = try {
                    coverFor(mangaId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "Failed to load the cover for queued manga $mangaId" }
                    null
                }
            }
        }
    }

    var draggedOrder by remember(groups, canReorder) { mutableStateOf<List<Download>?>(null) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val movingId = from.key as? Long ?: return@rememberReorderableLazyListState
        val targetId = to.key as? Long ?: return@rememberReorderableLazyListState
        val moving = downloadsById[movingId] ?: return@rememberReorderableLazyListState
        val target = downloadsById[targetId] ?: return@rememberReorderableLazyListState
        if (!canReorder || moving.source.id != target.source.id) return@rememberReorderableLazyListState
        val sourceId = moving.source.id
        val current = draggedOrder?.takeIf { it.firstOrNull()?.source?.id == sourceId } ?: bySource.getValue(sourceId)
        val oldIndex = current.indexOf(moving)
        val newIndex = current.indexOf(target)
        if (oldIndex >= 0 && newIndex >= 0 && oldIndex != newIndex) {
            draggedOrder = current.toMutableList().apply { add(newIndex, removeAt(oldIndex)) }
        }
    }
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = contentPadding.calculateBottomPadding() + 72.dp,
        ),
    ) {
        groups.forEach { group ->
            val rows = draggedOrder?.takeIf { it.firstOrNull()?.source?.id == group.id } ?: bySource.getValue(group.id)
            val paused = group.id in pausedSources
            item(key = "download-source-${group.id}", contentType = "source_header") {
                Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TsuzukiSectionLabel(group.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onPauseSource(group.id, !paused) }) {
                        Icon(
                            imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Outlined.Pause,
                            contentDescription = stringResource(
                                if (paused) MR.strings.action_resume else MR.strings.action_pause,
                            ),
                            tint = TsuzukiTheme.tokens.accent,
                        )
                    }
                }
            }
            itemsIndexed(rows, key = { _, download ->
                download.chapterId
            }, contentType = { _, _ -> "chapter" }) { index, download ->
                ReorderableItem(reorderState, key = download.chapterId) { isDragging ->
                    val series = remember(downloads, download.mangaId) {
                        downloads.filter {
                            it.mangaId ==
                                download.mangaId
                        }
                    }
                    val dragHandle = if (canReorder) {
                        Modifier.draggableHandle(
                            onDragStopped = { draggedOrder?.let(onReorder) },
                        )
                    } else {
                        null
                    }
                    MangaQueueRow(
                        download = download,
                        paused = paused,
                        onRetry = { onRetry(download) },
                        cover = covers[download.mangaId],
                        dragHandle = dragHandle,
                        isDragging = isDragging,
                        modifier = tsuzukiGroupedTile(first = index == 0, last = index == rows.lastIndex),
                        canReorder = canReorder,
                        canMoveUp = index > 0,
                        canMoveDown = index < rows.lastIndex,
                        onMove = { top, wholeSeries ->
                            val queue = if (wholeSeries) downloads else rows
                            val (selected, others) = queue.partition {
                                if (wholeSeries) it.mangaId == download.mangaId else it.chapterId == download.chapterId
                            }
                            onReorder(if (top) selected + others else others + selected)
                        },
                        onCancel = { wholeSeries -> onCancel(if (wholeSeries) series else listOf(download)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaQueueRow(
    download: Download,
    paused: Boolean,
    onRetry: () -> Unit,
    cover: CoverData?,
    dragHandle: Modifier?,
    isDragging: Boolean,
    modifier: Modifier,
    canReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (top: Boolean, wholeSeries: Boolean) -> Unit,
    onCancel: (wholeSeries: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val tokens = TsuzukiTheme.tokens
    val progress by download.progressFlow.collectAsState(initial = download.progress)
    val status by download.statusFlow.collectAsState()
    val error = download.error.takeIf { status == Download.State.ERROR && !it.isNullOrBlank() }
    val errorLabel = stringResource(MR.strings.download_notifier_title_error)
    val errorCopy = remember(error) {
        error?.let { eu.kanade.tachiyomi.tsuzuki.error.errorCopyForUi(IllegalStateException(it)) }
    }
    val statusLabel = when (status) {
        Download.State.DOWNLOADING -> stringResource(MR.strings.downloading_with_progress, progress)
        Download.State.ERROR -> errorCopy?.let(context::errorHeading) ?: errorLabel
        Download.State.DOWNLOADED -> stringResource(MR.strings.completed)
        else -> stringResource(if (paused) MR.strings.paused else MR.strings.tsz_downloads_queued)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isDragging) Modifier.background(tokens.tile) else Modifier)
            .padding(start = 8.dp, end = 2.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dragHandle != null) {
            Box(dragHandle.size(40.dp, 48.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DragHandle, stringResource(MR.strings.tsz_quotes_drag_to_reorder))
            }
        }
        MangaCover.Book(data = cover, modifier = Modifier.width(42.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = tsuzukiCase(download.mangaTitle).orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tsuzukiCase(download.chapterName).orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TsuzukiProgressBar(progress = progress / 100f, dots = 14, thickness = 4.dp)
            Text(
                text = tsuzukiCase(statusLabel).orEmpty(),
                color = if (status ==
                    Download.State.ERROR
                ) {
                    tokens.accent
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = if (error != null) {
                    Modifier.clickable {
                        context.copyToClipboard(errorLabel, eu.kanade.tachiyomi.tsuzuki.error.redactErrorText(error))
                    }
                } else {
                    Modifier
                },
            )
        }
        if (status == Download.State.ERROR) {
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(MR.strings.action_retry),
                    tint = tokens.accent,
                )
            }
        }
        MangaQueueMenu(canReorder, canMoveUp, canMoveDown, onMove, onCancel)
    }
}

@Composable
private fun MangaQueueMenu(
    canReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Boolean, Boolean) -> Unit,
    onCancel: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, stringResource(MR.strings.label_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (canReorder) {
                if (canMoveUp) {
                    TsuzukiMenuItem(stringResource(MR.strings.action_move_to_top), onClick = {
                        expanded = false
                        onMove(true, false)
                    })
                }
                TsuzukiMenuItem(stringResource(MR.strings.action_move_to_top_all_for_series), onClick = {
                    expanded = false
                    onMove(true, true)
                })
                if (canMoveDown) {
                    TsuzukiMenuItem(stringResource(MR.strings.action_move_to_bottom), onClick = {
                        expanded = false
                        onMove(false, false)
                    })
                }
                TsuzukiMenuItem(stringResource(MR.strings.action_move_to_bottom_all_for_series), onClick = {
                    expanded = false
                    onMove(false, true)
                })
            }
            TsuzukiMenuItem(stringResource(MR.strings.action_cancel), danger = true, onClick = {
                expanded = false
                onCancel(false)
            })
            TsuzukiMenuItem(stringResource(MR.strings.cancel_all_for_series), danger = true, onClick = {
                expanded = false
                onCancel(true)
            })
        }
    }
}
