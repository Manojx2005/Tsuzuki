package eu.kanade.presentation.tsuzuki.download

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadEntry
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadProgress
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
internal fun TsuzukiAnimeDownloadQueue(downloads: AnimeDownloads, contentPadding: PaddingValues) {
    val queue by downloads.state.collectAsState()
    val progress by downloads.progress.collectAsState()
    val groups = remember(queue.entries) { queue.entries.groupBy { it.sourceId to it.animeId } }
    val tokens = TsuzukiTheme.tokens
    val context = LocalContext.current
    var delete by remember { mutableStateOf<AnimeDownloadEntry?>(null) }
    val savedBytes = remember(queue.entries) {
        queue.entries.filter { it.status == AnimeDownloadStatus.Saved }.sumOf(AnimeDownloadEntry::bytes)
    }

    LazyColumn(
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
        ),
    ) {
        item {
            Row(
                Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TsuzukiTile(Modifier.weight(1.4f), padding = 14.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            tsuzukiMenuLabel(stringResource(MR.strings.tsz_downloads_queued)),
                            color = tokens.mutedText,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            queue.entries.count { it.status == AnimeDownloadStatus.Queued }.toString(),
                            color = tokens.dotOn,
                            style = MaterialTheme.typography.displaySmall,
                        )
                    }
                }
                TsuzukiTile(Modifier.weight(1f), padding = 14.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            tsuzukiMenuLabel(stringResource(MR.strings.label_downloaded)),
                            color = tokens.mutedText,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            tsuzukiMenuLabel(Formatter.formatShortFileSize(context, savedBytes)),
                            color = tokens.dotOn,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
            }
        }
        item {
            TsuzukiPill(
                text = stringResource(MR.strings.tsz_download_wifi),
                selected = queue.wifiOnly,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                onClick = { downloads.setWifiOnly(!queue.wifiOnly) },
                role = Role.Switch,
            )
        }
        if (groups.isEmpty()) {
            item {
                Text(
                    tsuzukiMenuLabel(stringResource(MR.strings.information_no_downloads)),
                    Modifier.padding(12.dp),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        groups.forEach { (group, entries) ->
            item(key = "group-${group.first}-${group.second}") {
                val first = entries.first()
                TsuzukiSectionLabel(
                    text = listOf(
                        first.animeTitle,
                        first.sourceName,
                        stringResource(MR.strings.tsz_anime_debug_episodes),
                    ).joinToString(" · "),
                )
            }
            itemsIndexed(entries, key = { _, entry -> entry.episodeId }) { index, entry ->
                val active = progress?.takeIf { it.first == entry.episodeId }?.second
                AnimeDownloadRow(
                    entry = entry,
                    active = active,
                    paused = queue.paused,
                    modifier = tsuzukiGroupedTile(first = index == 0, last = index == entries.lastIndex),
                    onRetry = { downloads.retry(entry.episodeId) },
                    onRemove = {
                        if (entry.status == AnimeDownloadStatus.Saved) {
                            delete = entry
                        } else {
                            downloads.remove(entry.episodeId)
                        }
                    },
                )
            }
        }
    }

    delete?.let { entry ->
        AlertDialog(
            onDismissRequest = { delete = null },
            title = { Text(tsuzukiMenuLabel(androidStringResource(R.string.tsz_anime_delete_download))) },
            text = { Text(tsuzukiMenuLabel(entry.episodeTitle)) },
            confirmButton = {
                TextButton(onClick = {
                    downloads.remove(entry.episodeId)
                    delete = null
                }) {
                    Text(tsuzukiMenuLabel(stringResource(MR.strings.action_delete)))
                }
            },
            dismissButton = {
                TextButton(onClick = { delete = null }) {
                    Text(tsuzukiMenuLabel(stringResource(MR.strings.action_cancel)))
                }
            },
        )
    }
}

@Composable
private fun AnimeDownloadRow(
    entry: AnimeDownloadEntry,
    active: AnimeDownloadProgress?,
    paused: Boolean,
    modifier: Modifier,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val context = LocalContext.current
    val label = when {
        active != null -> {
            val size = Formatter.formatShortFileSize(context, active.completed)
            active.total?.let { "$size / ${Formatter.formatShortFileSize(context, it)}" } ?: size
        }
        entry.status == AnimeDownloadStatus.Saved -> Formatter.formatShortFileSize(context, entry.bytes)
        entry.status == AnimeDownloadStatus.Failed -> stringResource(MR.strings.download_notifier_title_error)
        entry.status == AnimeDownloadStatus.Removing -> stringResource(MR.strings.action_delete)
        paused -> stringResource(MR.strings.paused)
        else -> stringResource(MR.strings.tsz_downloads_queued)
    }
    val fraction = if (entry.status == AnimeDownloadStatus.Saved) 1f else active?.fraction ?: 0f

    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                tsuzukiMenuLabel(entry.episodeTitle),
                color = tokens.dotOn,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TsuzukiProgressBar(progress = fraction, dots = 14, thickness = 4.dp)
            Text(tsuzukiMenuLabel(label), color = tokens.mutedText, style = MaterialTheme.typography.labelSmall)
        }
        if (entry.status == AnimeDownloadStatus.Failed) {
            IconButton(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, stringResource(MR.strings.action_retry), tint = tokens.dotOn)
            }
        }
        if (entry.status != AnimeDownloadStatus.Removing) {
            val saved = entry.status == AnimeDownloadStatus.Saved
            IconButton(onClick = onRemove) {
                Icon(
                    if (saved) Icons.Outlined.DeleteOutline else Icons.Outlined.Close,
                    stringResource(if (saved) MR.strings.action_delete else MR.strings.action_cancel),
                    tint = tokens.dotOn,
                )
            }
        }
    }
}
