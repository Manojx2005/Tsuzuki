package eu.kanade.presentation.tsuzuki.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import coil3.compose.AsyncImage
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.anime.openAnime
import eu.kanade.presentation.tsuzuki.anime.rememberAnimeDetailsOpener
import eu.kanade.presentation.util.rememberTimestampFormatter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayback
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toKotlinLocalDate
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.presentation.core.components.material.PullRefresh
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Clock

internal fun airingDay(now: Instant, zone: ZoneId): LongRange {
    val day = now.atZone(zone).toLocalDate()
    return day.atStartOfDay(zone).toInstant().toEpochMilli() until
        day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
}

@Composable
internal fun rememberAnimeAiringToday(store: AnimeLibraryStore): AnimeUpdateEpisode? {
    val now by produceState(Instant.now()) {
        while (true) {
            delay(30_000)
            value = Instant.now()
        }
    }
    val day = airingDay(now, ZoneId.systemDefault())
    val schedule by remember(store, day) {
        store.observeAiringEpisodes(day.first, day.last + 1)
    }.collectAsState(emptyList())
    // A6.6: AniList's real times win over the source's dates for the same anime.
    val tracking = remember { Injekt.get<eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTracking>() }
    val aniList by remember(tracking) { tracking.airing.observe() }.collectAsState(emptyMap())
    val library by remember(store) { store.observeLibrary() }.collectAsState(emptyList())
    val titles = library.associate { it.anime.id to it.anime.title }
    val episodeLabel = androidx.compose.ui.platform.LocalContext.current
    val today = mergeAiringToday(schedule, aniList, titles, day) { number ->
        episodeLabel.getString(R.string.tsz_anime_history_episode, number.toString())
    }
    return today.firstOrNull { it.timestamp >= now.toEpochMilli() } ?: today.lastOrNull()
}

/**
 * A6.6: today's airing entries, AniList first: an anime with an AniList time today shows that time (its source
 * estimates are dropped); only library anime ([titles]) are listed. Sorted by time.
 */
internal fun mergeAiringToday(
    source: List<AnimeUpdateEpisode>,
    aniList: Map<Long, eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeNextAiring>,
    titles: Map<Long, String>,
    day: LongRange,
    episodeName: (Int) -> String,
): List<AnimeUpdateEpisode> {
    val real = aniList.filter { (id, next) -> next.airingAt in day && id in titles }
        .map { (id, next) ->
            AnimeUpdateEpisode(id, titles.getValue(id), -1L, episodeName(next.episode), null, next.airingAt)
        }
    val realIds = real.mapTo(hashSetOf()) { it.animeId }
    return (source.filter { it.timestamp in day && it.animeId !in realIds } + real).sortedBy { it.timestamp }
}

@Composable
internal fun AnimeAiringStrip(entry: AnimeUpdateEpisode, modifier: Modifier = Modifier) {
    val openDetails = eu.kanade.presentation.tsuzuki.anime.rememberAnimeDetailsOpener()
    val tokens = TsuzukiTheme.tokens
    val time = rememberTimestampFormatter().format(Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()))
    TsuzukiTile(
        modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button) { openDetails(entry.animeId) },
        padding = 12.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = tokens.accent)
            Column(Modifier.weight(1f)) {
                Text(
                    tsuzukiMenuLabel(stringResource(R.string.tsz_anime_airing_today)),
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.accent,
                )
                Text(
                    tsuzukiMenuLabel(listOf(entry.title, entry.name).joinToString(" · ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.dotOn,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(tsuzukiMenuLabel(time), style = MaterialTheme.typography.labelSmall, color = tokens.mutedText)
        }
    }
}

@Composable
internal fun TsuzukiAnimeUpdates(
    entries: List<AnimeUpdateEpisode>,
    padding: PaddingValues,
    pills: @Composable (twoPane: Boolean) -> Unit,
    airing: AnimeUpdateEpisode?,
    selected: Set<Long>,
    downloadStatus: Map<Long, AnimeDownloadStatus>,
    toSave: Int,
    onSaveAll: () -> Unit,
    onToggleSelection: (AnimeUpdateEpisode) -> Unit,
    onDownload: (AnimeUpdateEpisode) -> Unit,
    onMarkSeen: (AnimeUpdateEpisode, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val jobs by remember(context) {
        context.workManager.getWorkInfosForUniqueWorkFlow(AnimeLibraryUpdateJob.MANUAL)
    }.collectAsState(emptyList())
    val isRefreshing = jobs.any { it.state == WorkInfo.State.RUNNING }
    val zone = ZoneId.systemDefault()
    val groups = remember(entries, zone) {
        entries.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate().toKotlinLocalDate() }
    }
    val selectionMode = selected.isNotEmpty()
    // AP9: on tablets the list sits on the left and the tapped update is previewed on the right, as for books.
    val twoPane = tsuzukiUpdatesTwoPaneEnabled()
    var previewKey by rememberSaveable { mutableStateOf<Long?>(null) }
    val previewed = if (twoPane) resolveUpdatesSelection(entries, previewKey) { it.episodeId } else null

    @Composable
    fun UpdatesList(modifier: Modifier, contentPadding: PaddingValues) {
        LazyColumn(modifier, contentPadding = contentPadding) {
            if (!selectionMode) item(key = "media_pills") { pills(twoPane) }
            airing?.let { entry ->
                item(key = "airing_today") { AnimeAiringStrip(entry, Modifier.padding(horizontal = 16.dp)) }
            }
            if (entries.isEmpty()) {
                item(key = "empty_updates") {
                    Text(
                        tsuzukiMenuLabel(stringResource(R.string.tsz_anime_updates_empty)),
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TsuzukiTheme.tokens.mutedText,
                    )
                }
            } else {
                item(key = "new_episode_tiles") {
                    TsuzukiUpdatesTiles(
                        newCount = entries.count { !it.seen },
                        toSave = toSave,
                        onSaveAll = onSaveAll,
                        unit = stringResource(R.string.tsz_anime_ep),
                    )
                }
            }
            for ((day, rows) in groups) {
                item(key = "anime_updates_day_" + day) { TsuzukiSectionLabel(relativeDateText(day)) }
                items(rows, key = { it.episodeId }) { entry ->
                    AnimeUpdateTile(
                        entry = entry,
                        selected = entry.episodeId in selected,
                        highlighted = !selectionMode && entry.episodeId == previewed?.episodeId,
                        downloadStatus = downloadStatus[entry.episodeId],
                        onClick = {
                            when {
                                selectionMode -> onToggleSelection(entry)
                                twoPane -> previewKey = entry.episodeId
                                else -> playAnimeEpisode(context, entry)
                            }
                        },
                        onLongClick = { onToggleSelection(entry) },
                        onDownload = { onDownload(entry) }.takeUnless { selectionMode },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    PullRefresh(
        refreshing = isRefreshing,
        enabled = !isRefreshing && !selectionMode,
        onRefresh = { AnimeLibraryUpdateJob.start(context) },
        indicatorPadding = padding,
    ) {
        if (!twoPane) {
            UpdatesList(Modifier, padding)
            return@PullRefresh
        }
        val tokens = TsuzukiTheme.tokens
        val openDetails = rememberAnimeDetailsOpener()
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UpdatesList(Modifier.weight(0.46f).fillMaxSize(), PaddingValues(bottom = 8.dp))
            val shape = RoundedCornerShape(tokens.tileRadius)
            Box(
                Modifier
                    .weight(0.54f)
                    .fillMaxSize()
                    .clip(shape)
                    .background(tokens.tile)
                    .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (previewed == null) {
                    Text(
                        tsuzukiMenuLabel(stringResource(R.string.tsz_anime_updates_empty)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.mutedText,
                    )
                } else {
                    AnimeEpisodePreview(
                        entry = previewed,
                        others = otherNewEpisodes(entries, previewed),
                        downloadStatus = downloadStatus[previewed.episodeId],
                        onSelectOther = { previewKey = it.episodeId },
                        onPlay = { playAnimeEpisode(context, previewed) },
                        onMarkSeen = { onMarkSeen(previewed, it) },
                        onDownload = { onDownload(previewed) },
                        onOpenAnime = { openDetails(previewed.animeId) },
                    )
                }
            }
        }
    }
}

private fun playAnimeEpisode(context: android.content.Context, entry: AnimeUpdateEpisode) {
    val track = !Injekt.get<BasePreferences>().incognitoMode.get()
    AnimePlayback.launchEpisode(context, entry.episodeId, track)
}

/** One anime's new episodes as a group (grouped Updates › All); tapping opens the anime. */
@Composable
internal fun TsuzukiAnimeUpdateGroupEntry(group: AnimeUpdateGroup, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val openDetails = eu.kanade.presentation.tsuzuki.anime.rememberAnimeDetailsOpener()
    val summary = AnimeUpdateEpisode(
        animeId = group.animeId,
        title = group.title,
        episodeId = -group.animeId,
        name = animeGroupSubtitle(group),
        previewUrl = group.cover,
        timestamp = group.latest,
    )
    Box(modifier) {
        AnimeUpdateRow(summary, showPlay = false) {
            openDetails(group.animeId)
        }
    }
}

/**
 * One new episode in Updates › All, drawn like a book update row; tapping plays it. With [selection]
 * (AP9-ALL-SELECT) a long press selects it next to books, and taps toggle while anything is selected.
 */
@Composable
internal fun TsuzukiAnimeUpdateEntry(
    entry: AnimeUpdateEpisode,
    modifier: Modifier = Modifier,
    selection: AnimeAllSelection? = null,
) {
    val context = LocalContext.current
    val downloads = remember { Injekt.get<AnimeDownloads>() }
    val queue by downloads.state.collectAsState()
    val status = queue.entries.firstOrNull { it.episodeId == entry.episodeId }?.status
    val scope = rememberCoroutineScope()
    AnimeUpdateTile(
        entry = entry,
        selected = selection?.isSelected(entry) == true,
        downloadStatus = status,
        onClick = {
            if (selection?.selecting == true) selection.toggle(entry) else playAnimeEpisode(context, entry)
        },
        onLongClick = selection?.let { { it.toggle(entry) } },
        onDownload = {
            if (status == AnimeDownloadStatus.Failed) {
                downloads.retry(entry.episodeId)
            } else {
                scope.launchIO { queueAnimeUpdates(Injekt.get(), downloads, listOf(entry)) }
            }
            Unit
        }.takeUnless { selection?.selecting == true },
        modifier = modifier,
    )
}

@Composable
private fun AnimeUpdateRow(entry: AnimeUpdateEpisode, showPlay: Boolean = true, onPlay: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
            .tsuzukiClickable(role = Role.Button, onClick = onPlay),
        padding = 8.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(84.dp, 48.dp).clip(RoundedCornerShape(8.dp)).background(tokens.track),
                contentAlignment = Alignment.Center,
            ) {
                if (entry.previewUrl == null) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = tokens.mutedText)
                } else {
                    AsyncImage(
                        model = entry.previewUrl,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = tsuzukiCoverFilter(),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    tsuzukiMenuLabel(entry.title),
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.dotOn,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tsuzukiMenuLabel(entry.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showPlay) Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = tokens.accent)
        }
    }
}
