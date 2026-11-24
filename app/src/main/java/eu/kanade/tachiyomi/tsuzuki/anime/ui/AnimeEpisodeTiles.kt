package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.TabTitle
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.theme.tsuzuki.DotCoverFilter
import eu.kanade.presentation.theme.tsuzuki.TsuzukiAdaptiveSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeEpisodeDisplay
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeEpisodeSort
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import eu.kanade.tachiyomi.tsuzuki.anime.isExcludedAnimeGroup
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TriStateItem
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

@Composable
internal fun AnimeResumeTile(episode: Episode, enabled: Boolean, onPlay: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val resuming = episode.lastSecondSeen > 0
    val number = episode.displayNumber()
    val label = when {
        number == null -> mokoStringResource(if (resuming) MR.strings.tsz_resume else MR.strings.tsz_start)
        resuming -> stringResource(R.string.tsz_anime_resume_episode, number)
        else -> stringResource(R.string.tsz_anime_play_episode, number)
    }
    val contentColor = if (tokens.isDot) tokens.dotOn else tokens.onAccent
    val shape = RoundedCornerShape(tokens.tileRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(shape)
            .background(if (tokens.isDot) tokens.tile else tokens.accent)
            .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .tsuzukiClickable(enabled = enabled, role = Role.Button, onClick = onPlay)
            .heightIn(min = 72.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape)
                .background(if (tokens.isDot) tokens.accent else tokens.onAccent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (tokens.isDot) tokens.onAccent else tokens.accent,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tsuzukiMenuLabel(label), style = MaterialTheme.typography.titleMedium, color = contentColor)
            if (episode.totalSeconds > 0) {
                TsuzukiProgressBar(
                    progress = episode.playFraction,
                    dots = 18,
                    thickness = 5.dp,
                    fill = contentColor,
                    track = if (tokens.isDot) tokens.track else contentColor.copy(alpha = 0.18f),
                )
            }
        }
        if (resuming) {
            Text(
                DateUtils.formatElapsedTime(episode.lastSecondSeen),
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
internal fun AnimeEpisodeTile(
    source: AnimeSource,
    episode: Episode,
    coverUrl: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit,
    onMarkSeen: (Boolean) -> Unit,
    onBookmark: (Boolean) -> Unit,
    onMarkPrevious: () -> Unit,
    downloadStatus: AnimeDownloadStatus?,
    showNumberOnly: Boolean = false,
    onDownload: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    var menu by remember { mutableStateOf(false) }
    val meta = listOfNotNull(
        episode.scanlator?.takeIf(String::isNotBlank),
        episode.dateUpload.takeIf { it > 0 }?.let { DateFormat.getDateInstance().format(Date(it)) },
        stringResource(R.string.tsz_anime_filler).takeIf { episode.fillermark },
        stringResource(R.string.tsz_anime_watched).takeIf { episode.seen },
    )
    val number = episode.displayNumber()
    val title = when {
        number == null -> episode.name
        showNumberOnly -> stringResource(R.string.tsz_anime_history_episode, number)
        else -> stringResource(R.string.tsz_anime_episode_title, number, episode.name)
    }
    TsuzukiTile(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .combinedClickable(enabled = enabled, role = Role.Button, onClick = onClick, onLongClick = onSelect),
        padding = 10.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 54.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (selected) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = tokens.accent)
            }
            eu.kanade.tachiyomi.tsuzuki.anime.preview.AnimeEpisodeThumbnail(
                sourceId = source.id,
                episode = episode,
                coverUrl = coverUrl,
                modifier = Modifier.size(104.dp, 58.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    tsuzukiMenuLabel(title),
                    color = if (episode.seen) tokens.mutedText else tokens.dotOn,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (meta.isNotEmpty()) {
                    Text(
                        tsuzukiMenuLabel(meta.joinToString(" · ")),
                        color = tokens.mutedText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (episode.totalSeconds > 0 && episode.lastSecondSeen > 0 && !episode.seen) {
                    TsuzukiProgressBar(progress = episode.playFraction, dots = 18, thickness = 4.dp)
                }
            }
            if (episode.bookmark) {
                Icon(Icons.Outlined.Bookmark, stringResource(R.string.tsz_anime_bookmark), tint = tokens.accent)
            }
            val (downloadIcon, downloadLabel) = when (downloadStatus) {
                null -> Icons.Outlined.Download to MR.strings.manga_download
                AnimeDownloadStatus.Saved -> Icons.Outlined.Check to MR.strings.label_downloaded
                AnimeDownloadStatus.Queued -> Icons.Outlined.Schedule to MR.strings.tsz_downloads_queued
                AnimeDownloadStatus.Failed -> Icons.Outlined.Refresh to MR.strings.action_retry
                AnimeDownloadStatus.Removing -> Icons.Outlined.Schedule to MR.strings.action_delete
            }
            IconButton(
                onClick = onDownload,
                enabled = enabled && (downloadStatus == null || downloadStatus == AnimeDownloadStatus.Failed),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = tokens.dotOn,
                    disabledContentColor = tokens.mutedText,
                ),
            ) {
                Icon(downloadIcon, mokoStringResource(downloadLabel))
            }
            Box {
                IconButton(onClick = { menu = true }, enabled = enabled) {
                    Icon(Icons.Outlined.MoreVert, mokoStringResource(MR.strings.label_more), tint = tokens.dotOn)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    AnimeEpisodeActions(
                        seen = episode.seen,
                        bookmarked = episode.bookmark,
                        onMarkSeen = {
                            menu = false
                            onMarkSeen(it)
                        },
                        onBookmark = {
                            menu = false
                            onBookmark(it)
                        },
                        onMarkPrevious = {
                            menu = false
                            onMarkPrevious()
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun AnimeEpisodeActions(
    seen: Boolean,
    bookmarked: Boolean,
    onMarkSeen: (Boolean) -> Unit,
    onBookmark: (Boolean) -> Unit,
    onMarkPrevious: (() -> Unit)?,
) {
    TsuzukiMenuItem(
        text = stringResource(if (seen) R.string.tsz_anime_mark_unseen else R.string.tsz_anime_mark_seen),
        onClick = { onMarkSeen(!seen) },
    )
    TsuzukiMenuItem(
        text = stringResource(if (bookmarked) R.string.tsz_anime_unbookmark else R.string.tsz_anime_bookmark),
        onClick = { onBookmark(!bookmarked) },
    )
    if (onMarkPrevious != null) {
        TsuzukiMenuItem(text = stringResource(R.string.tsz_anime_mark_previous), onClick = onMarkPrevious)
    }
}

/**
 * Episode list settings use the chapter dialog's Filter, Sort and Display tabs. Changes apply immediately.
 */
@Composable
internal fun AnimeEpisodeFilterSheet(
    display: AnimeEpisodeDisplay,
    groups: List<String>,
    excludedGroups: Set<String>,
    onDismiss: () -> Unit,
    canMarkUntilLastWatched: Boolean,
    onMarkUntilLastWatched: () -> Unit,
    onSave: (AnimeEpisodeDisplay, Set<String>) -> Unit,
) {
    TabbedDialog(
        onDismissRequest = onDismiss,
        tabTitles = listOf(
            TabTitle.Text(mokoStringResource(MR.strings.action_filter)),
            TabTitle.Text(mokoStringResource(MR.strings.action_sort)),
            TabTitle.Text(mokoStringResource(MR.strings.action_display)),
        ),
    ) { page ->
        Column(Modifier.padding(vertical = TabbedDialogPaddings.Vertical).verticalScroll(rememberScrollState())) {
            if (page == 0) {
                // "Watched", the mirror of the stored unwatched filter (only watched = hide unwatched).
                TriStateItem(stringResource(R.string.tsz_anime_watched), display.unseen.mirrored()) {
                    onSave(display.copy(unseen = it.mirrored()), excludedGroups)
                }
                TriStateItem(mokoStringResource(MR.strings.label_downloaded), display.downloaded) {
                    onSave(display.copy(downloaded = it), excludedGroups)
                }
                TriStateItem(mokoStringResource(MR.strings.action_filter_bookmarked), display.bookmarked) {
                    onSave(display.copy(bookmarked = it), excludedGroups)
                }
                if (groups.isNotEmpty()) {
                    Text(
                        tsuzukiMenuLabel(stringResource(R.string.tsz_anime_exclude_groups)),
                        color = TsuzukiTheme.tokens.mutedText,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    groups.forEach { group ->
                        val excluded = group.isExcludedAnimeGroup(excludedGroups)
                        CheckboxItem(group, excluded) {
                            onSave(
                                display,
                                if (excluded) {
                                    excludedGroups.filterNot { it.equals(group, ignoreCase = true) }.toSet()
                                } else {
                                    excludedGroups + group
                                },
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onMarkUntilLastWatched,
                    enabled = canMarkUntilLastWatched,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Text(tsuzukiMenuLabel(stringResource(R.string.tsz_anime_mark_until_last_watched)))
                }
            } else if (page == 1) {
                listOf(
                    AnimeEpisodeSort.SOURCE to mokoStringResource(MR.strings.sort_by_source),
                    AnimeEpisodeSort.NUMBER to stringResource(R.string.tsz_anime_episode_number),
                    AnimeEpisodeSort.UPLOAD_DATE to mokoStringResource(MR.strings.tsz_chapter_filter_upload_date),
                ).forEach { (sort, label) ->
                    SortItem(
                        label = tsuzukiMenuLabel(label),
                        sortDescending = (!display.ascending).takeIf { display.sort == sort },
                    ) {
                        val next = if (display.sort == sort) {
                            display.copy(ascending = !display.ascending)
                        } else {
                            display.copy(sort = sort)
                        }
                        onSave(next, excludedGroups)
                    }
                }
            } else {
                RadioItem(
                    label = tsuzukiMenuLabel(mokoStringResource(MR.strings.show_title)),
                    selected = !display.showNumberOnly,
                ) {
                    onSave(display.copy(showNumberOnly = false), excludedGroups)
                }
                RadioItem(
                    label = tsuzukiMenuLabel(stringResource(R.string.tsz_anime_episode_number)),
                    selected = display.showNumberOnly,
                ) {
                    onSave(display.copy(showNumberOnly = true), excludedGroups)
                }
            }
        }
    }
}

@Composable
private fun AnimeEpisodeFilterOption(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Checkbox) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked, onCheckedChange = null)
        Text(tsuzukiMenuLabel(label), style = MaterialTheme.typography.bodyMedium)
    }
}

private val Episode.playFraction: Float
    get() = if (totalSeconds > 0) (lastSecondSeen.toFloat() / totalSeconds).coerceIn(0f, 1f) else 0f

private fun Episode.displayNumber(): String? = episodeNumber.takeIf { it.isFinite() && it >= 0 }?.let {
    NumberFormat.getNumberInstance().apply { isGroupingUsed = false }.format(it)
}

private fun TriState.mirrored(): TriState = when (this) {
    TriState.DISABLED -> TriState.DISABLED
    TriState.ENABLED_IS -> TriState.ENABLED_NOT
    TriState.ENABLED_NOT -> TriState.ENABLED_IS
}
