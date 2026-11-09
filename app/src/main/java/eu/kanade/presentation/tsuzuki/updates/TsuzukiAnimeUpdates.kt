package eu.kanade.presentation.tsuzuki.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import eu.kanade.presentation.theme.tsuzuki.DotCoverFilter
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.anime.openAnime
import eu.kanade.presentation.util.rememberTimestampFormatter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayback
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toKotlinLocalDate
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
    val today = schedule.filter { it.timestamp in day }
    return today.firstOrNull { it.timestamp >= now.toEpochMilli() } ?: today.lastOrNull()
}

@Composable
internal fun AnimeAiringStrip(entry: AnimeUpdateEpisode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tokens = TsuzukiTheme.tokens
    val time = rememberTimestampFormatter().format(Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()))
    TsuzukiTile(
        modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button) { openAnime(context, entry.animeId) },
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
    store: AnimeLibraryStore,
    padding: PaddingValues,
    pills: @Composable () -> Unit,
    airing: AnimeUpdateEpisode?,
) {
    val context = LocalContext.current
    val jobs by remember(context) {
        context.workManager.getWorkInfosForUniqueWorkFlow(AnimeLibraryUpdateJob.MANUAL)
    }.collectAsState(emptyList())
    val isRefreshing = jobs.any { it.state == WorkInfo.State.RUNNING }
    val since = remember {
        Clock.System.now().minus(3, DateTimeUnit.MONTH, TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
    val entries by remember(store, since) { store.observeNewEpisodes(since) }.collectAsState(emptyList())
    val zone = ZoneId.systemDefault()
    val groups = remember(entries, zone) {
        entries.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate().toKotlinLocalDate() }
    }

    PullRefresh(
        refreshing = isRefreshing,
        enabled = !isRefreshing,
        onRefresh = { AnimeLibraryUpdateJob.start(context) },
        indicatorPadding = padding,
    ) {
        LazyColumn(contentPadding = padding) {
            item(key = "media_pills") { pills() }
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
                item(key = "new_episode_count") {
                    TsuzukiSectionLabel(
                        pluralStringResource(R.plurals.tsz_anime_new_episodes, entries.size, entries.size),
                    )
                }
            }
            for ((day, rows) in groups) {
                item(key = "anime_updates_day_" + day) { TsuzukiSectionLabel(relativeDateText(day)) }
                items(rows, key = { it.episodeId }) { entry ->
                    AnimeUpdateRow(entry) {
                        val track = !Injekt.get<BasePreferences>().incognitoMode.get()
                        AnimePlayback.launchEpisode(context, entry.episodeId, track)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeUpdateRow(entry: AnimeUpdateEpisode, onPlay: () -> Unit) {
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
                        colorFilter = DotCoverFilter.takeIf { tokens.isDot },
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
            Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = tokens.accent)
        }
    }
}
