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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import eu.kanade.presentation.theme.tsuzuki.DotCoverFilter
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
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.i18n.MR
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
    selected: Boolean,
    playing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit,
    onMarkSeen: (Boolean) -> Unit,
    onBookmark: (Boolean) -> Unit,
    onMarkPrevious: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    var menu by remember { mutableStateOf(false) }
    val meta = listOfNotNull(
        episode.scanlator?.takeIf(String::isNotBlank),
        episode.dateUpload.takeIf { it > 0 }?.let { DateFormat.getDateInstance().format(Date(it)) },
        stringResource(R.string.tsz_anime_filler).takeIf { episode.fillermark },
        stringResource(R.string.tsz_anime_watched).takeIf { episode.seen },
    )
    val title = episode.displayNumber()?.let { stringResource(R.string.tsz_anime_episode_title, it, episode.name) }
        ?: episode.name
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
            episode.previewUrl?.takeIf(String::isNotBlank)?.let { preview ->
                AsyncImage(
                    model = rememberAnimeImage(source, preview),
                    contentDescription = null,
                    modifier = Modifier.size(104.dp, 58.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                    colorFilter = DotCoverFilter.takeIf { tokens.isDot },
                )
            }
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
            if (playing) {
                CircularProgressIndicator(Modifier.size(24.dp), color = tokens.accent)
            } else {
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

@Composable
internal fun AnimeEpisodeFilterDialog(
    display: AnimeEpisodeDisplay,
    onDismiss: () -> Unit,
    onSave: (AnimeEpisodeDisplay) -> Unit,
) {
    var draft by remember(display) { mutableStateOf(display) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tsuzukiMenuLabel(mokoStringResource(MR.strings.action_filter))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TsuzukiPillRow(
                    labels = listOf(
                        stringResource(R.string.tsz_anime_episode_number),
                        mokoStringResource(MR.strings.tsz_chapter_filter_upload_date),
                    ),
                    selected = if (draft.sortByDate) 1 else 0,
                    onClick = { draft = draft.copy(sortByDate = it == 1) },
                )
                TsuzukiPillRow(
                    labels = listOf(
                        mokoStringResource(MR.strings.action_asc),
                        mokoStringResource(MR.strings.action_desc),
                    ),
                    selected = if (draft.ascending) 0 else 1,
                    onClick = { draft = draft.copy(ascending = it == 0) },
                )
                AnimeEpisodeFilterOption(
                    stringResource(R.string.tsz_anime_unseen_only),
                    draft.unseenOnly,
                ) { draft = draft.copy(unseenOnly = !draft.unseenOnly) }
                AnimeEpisodeFilterOption(
                    stringResource(R.string.tsz_anime_bookmarked_only),
                    draft.bookmarkedOnly,
                ) { draft = draft.copy(bookmarkedOnly = !draft.bookmarkedOnly) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) { Text(mokoStringResource(MR.strings.action_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(mokoStringResource(MR.strings.action_cancel)) }
        },
    )
}

@Composable
private fun AnimeEpisodeFilterOption(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).toggleable(checked, role = Role.Checkbox) { onClick() },
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
