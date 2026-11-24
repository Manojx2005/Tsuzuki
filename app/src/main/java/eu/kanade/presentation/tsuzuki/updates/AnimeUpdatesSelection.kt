package eu.kanade.presentation.tsuzuki.updates

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackProgressJob
import kotlinx.coroutines.CancellationException
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Which bottom-bar actions apply to the selected new episodes (same rules as the book Updates bar). */
internal data class AnimeUpdateSelectionActions(
    val bookmark: Boolean,
    val removeBookmark: Boolean,
    val markSeen: Boolean,
    val markUnseen: Boolean,
    val download: Boolean,
    val delete: Boolean,
)

internal fun animeUpdateSelectionActions(
    selected: List<AnimeUpdateEpisode>,
    status: Map<Long, AnimeDownloadStatus>,
): AnimeUpdateSelectionActions = AnimeUpdateSelectionActions(
    bookmark = selected.any { !it.bookmark },
    removeBookmark = selected.isNotEmpty() && selected.all { it.bookmark },
    markSeen = selected.any { !it.seen },
    markUnseen = selected.any { it.seen || it.lastSecondSeen > 0 },
    download = selected.any { status[it.episodeId] == null || status[it.episodeId] == AnimeDownloadStatus.Failed },
    delete = selected.any { status[it.episodeId] == AnimeDownloadStatus.Saved },
)

/** Select all / invert, keeping only ids that are still listed. */
internal fun animeUpdatesSelectAll(entries: List<AnimeUpdateEpisode>): Set<Long> =
    entries.mapTo(linkedSetOf()) { it.episodeId }

internal fun animeUpdatesInvert(entries: List<AnimeUpdateEpisode>, selected: Set<Long>): Set<Long> =
    entries.mapNotNullTo(linkedSetOf()) { it.episodeId.takeUnless { id -> id in selected } }

/** Marks episodes watched or unwatched and, outside incognito, sends the newest watched episode to trackers. */
internal suspend fun markAnimeUpdatesSeen(
    context: Context,
    store: AnimeLibraryStore,
    ids: Set<Long>,
    seen: Boolean,
    track: Boolean,
) {
    val changed = store.setEpisodesSeen(ids, seen)
    if (!seen || !track) return
    changed.filter { it.episodeNumber.isFinite() }
        .groupBy { it.animeId }
        .values
        .mapNotNull { episodes -> episodes.maxByOrNull { it.episodeNumber } }
        .forEach { AnimeTrackProgressJob.enqueue(context, it) }
}

/** Queues the episodes for download, one call per anime with its source and title (as the details screen does). */
internal suspend fun queueAnimeUpdates(
    store: AnimeLibraryStore,
    downloads: AnimeDownloads,
    episodes: List<AnimeUpdateEpisode>,
    unwatchedOnly: Boolean = false,
): Boolean {
    if (episodes.isEmpty()) return true
    val sources = Injekt.get<AnimeSourceManager>()
    var savedAll = true
    for ((animeId, items) in episodes.groupBy { it.animeId }) {
        try {
            val anime = checkNotNull(store.getAnime(animeId))
            val ids = items.mapTo(hashSetOf()) { it.episodeId }
            val list = store.getEpisodes(animeId).filter { it.id in ids }
            val eligible = if (unwatchedOnly) list.filterNot { it.seen } else list
            if (eligible.isNotEmpty()) {
                val sourceName = sources.get(anime.source)?.source?.name ?: sources.stub(anime.source)?.name ?: ""
                downloads.enqueueAndAwait(anime.source, sourceName, anime.title, eligible)
            }
            if (list.size != ids.size) savedAll = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            savedAll = false
            eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog.w("Could not queue new episodes of anime $animeId", e)
        }
    }
    return savedAll
}

/**
 * A new episode drawn like a book update row (design board "Updates"): 56 dp tile, square cover, title,
 * unwatched dot and episode name with resume time, and a download indicator on the right.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AnimeUpdateTile(
    entry: AnimeUpdateEpisode,
    selected: Boolean,
    downloadStatus: AnimeDownloadStatus?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onDownload: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** The update shown in the tablet preview pane. */
    highlighted: Boolean = false,
) {
    val tokens = TsuzukiTheme.tokens
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)
    val muted = if (selected) tokens.dotOn else tokens.mutedText
    val main = if (entry.seen) muted else tokens.dotOn
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(shape)
            .background(if (selected) tokens.track else tokens.tile)
            .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .then(
                when {
                    selected -> Modifier.border(1.5.dp, tokens.accent, shape)
                    highlighted -> Modifier.border(2.dp, tokens.accent, shape)
                    else -> Modifier
                },
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            )
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.padding(vertical = 6.dp).fillMaxHeight().aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp)).background(tokens.track),
            contentAlignment = Alignment.Center,
        ) {
            val image = entry.coverUrl ?: entry.previewUrl
            if (image == null) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = tokens.mutedText)
            } else {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = tsuzukiCoverFilter(),
                )
            }
        }
        Column(Modifier.padding(horizontal = 16.dp).weight(1f)) {
            Text(
                tsuzukiMenuLabel(entry.title),
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                color = main,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!entry.seen) {
                    Box(Modifier.padding(end = 4.dp).size(6.dp).clip(CircleShape).background(tokens.accent))
                }
                if (entry.bookmark) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                        modifier = Modifier.size(14.dp),
                        tint = tokens.accent,
                    )
                    Spacer(Modifier.width(2.dp))
                }
                Text(
                    tsuzukiMenuLabel(entry.name),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = main,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!entry.seen && entry.lastSecondSeen > 0) {
                    Text(
                        " • " + DateUtils.formatElapsedTime(entry.lastSecondSeen),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
            }
        }
        AnimeDownloadIndicator(downloadStatus, onDownload)
    }
}

@Composable
private fun AnimeDownloadIndicator(status: AnimeDownloadStatus?, onDownload: (() -> Unit)?) {
    val tokens = TsuzukiTheme.tokens
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        when (status) {
            AnimeDownloadStatus.Queued, AnimeDownloadStatus.Removing -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = tokens.mutedText,
            )
            AnimeDownloadStatus.Saved -> Icon(
                Icons.Filled.CheckCircle,
                contentDescription = stringResource(MR.strings.label_downloaded),
                modifier = Modifier.size(22.dp),
                tint = tokens.dotOn,
            )
            AnimeDownloadStatus.Failed, null -> IconButton(
                onClick = { onDownload?.invoke() },
                enabled =
                onDownload != null,
            ) {
                Icon(
                    if (status ==
                        AnimeDownloadStatus.Failed
                    ) {
                        Icons.Outlined.ErrorOutline
                    } else {
                        Icons.Outlined.ArrowDownward
                    },
                    contentDescription = stringResource(
                        if (status ==
                            AnimeDownloadStatus.Failed
                        ) {
                            MR.strings.action_retry
                        } else {
                            MR.strings.manga_download
                        },
                    ),
                    modifier = Modifier.size(22.dp),
                    tint = if (status == AnimeDownloadStatus.Failed) tokens.accent else tokens.mutedText,
                )
            }
        }
    }
}
