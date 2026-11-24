package eu.kanade.presentation.tsuzuki.updates

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

/** The other new episodes of the same anime as [selected], in list order. */
internal fun otherNewEpisodes(
    entries: List<AnimeUpdateEpisode>,
    selected: AnimeUpdateEpisode,
): List<AnimeUpdateEpisode> =
    entries.filter { it.animeId == selected.animeId && it.episodeId != selected.episodeId }

/**
 * AP9 (board "TabletUpdates", the book `ChapterPreview`): the selected anime update on the right of the tablet
 * Updates list — cover, episode, when it arrived, the anime's other new episodes, and Play / Mark watched /
 * Download / Open anime.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnimeEpisodePreview(
    entry: AnimeUpdateEpisode,
    others: List<AnimeUpdateEpisode>,
    downloadStatus: AnimeDownloadStatus?,
    onSelectOther: (AnimeUpdateEpisode) -> Unit,
    onPlay: () -> Unit,
    onMarkSeen: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onOpenAnime: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .width(96.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.track)
                        .combinedClickable(role = Role.Button, onClick = onOpenAnime),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = entry.coverUrl ?: entry.previewUrl
                    if (image == null) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = tokens.mutedText)
                    } else {
                        AsyncImage(
                            model = image,
                            contentDescription = entry.title,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                            colorFilter = tsuzukiCoverFilter(),
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    PreviewLabel(tsuzukiMenuLabel(mokoStringResource(MR.strings.tsz_updates_preview, entry.title)))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tsuzukiMenuLabel(entry.name),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tsuzukiMenuLabel(relativeTimeSpanString(entry.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (others.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                PreviewLabel(tsuzukiMenuLabel(mokoStringResource(MR.strings.tsz_updates_more_new, others.size)))
                Spacer(Modifier.height(4.dp))
                others.forEach { other -> OtherEpisodeRow(other, onClick = { onSelectOther(other) }) }
            }
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionPill(
                text = tsuzukiMenuLabel(
                    mokoStringResource(
                        if (entry.lastSecondSeen > 0 && !entry.seen) MR.strings.action_resume else MR.strings.tsz_watch,
                    ),
                ),
                primary = true,
                icon = true,
                onClick = onPlay,
            )
            ActionPill(
                text = tsuzukiMenuLabel(
                    stringResource(if (entry.seen) R.string.tsz_anime_mark_unseen else R.string.tsz_anime_mark_seen),
                ),
                onClick = { onMarkSeen(!entry.seen) },
            )
            when (downloadStatus) {
                AnimeDownloadStatus.Saved -> ActionPill(
                    text = tsuzukiMenuLabel(mokoStringResource(MR.strings.tsz_updates_saved)),
                    onClick = {},
                    enabled = false,
                )
                null, AnimeDownloadStatus.Failed -> ActionPill(
                    text = tsuzukiMenuLabel(mokoStringResource(MR.strings.action_download)),
                    onClick = onDownload,
                )
                else -> Unit
            }
            ActionPill(text = tsuzukiMenuLabel(stringResource(R.string.tsz_anime_open_anime)), onClick = onOpenAnime)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OtherEpisodeRow(entry: AnimeUpdateEpisode, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tsuzukiMenuLabel(entry.name),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (entry.seen) 0.55f else 1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!entry.seen) {
            Box(Modifier.padding(start = 8.dp).size(8.dp).clip(RoundedCornerShape(50)).background(tokens.accent))
        }
    }
}
