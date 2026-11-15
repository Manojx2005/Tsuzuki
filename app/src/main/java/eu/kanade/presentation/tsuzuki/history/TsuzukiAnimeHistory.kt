package eu.kanade.presentation.tsuzuki.history

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.theme.tsuzuki.DotCoverFilter
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.util.rememberTimestampFormatter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.animeEpisodeNumberOrder
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.history.anime.model.AnimeHistoryDisplay
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import androidx.compose.ui.res.stringResource as androidStringResource

internal fun showAnimeHistory(hasHistory: Boolean, animeEnabled: Boolean = TsuzukiAnime.enabled): Boolean =
    animeEnabled && hasHistory

internal fun groupAnimeHistory(
    rows: List<AnimeHistoryDisplay>,
    query: String?,
    timeZone: TimeZone,
): Map<LocalDate, List<AnimeHistoryDisplay>> {
    val search = query.orEmpty().trim()
    return rows.filter {
        search.isEmpty() || it.title.contains(search, ignoreCase = true) ||
            it.episodeName.contains(search, ignoreCase = true)
    }.sortedByDescending { it.lastSeen }.groupBy {
        Instant.fromEpochMilliseconds(it.lastSeen.time).toLocalDateTime(timeZone).date
    }
}

internal fun animeHistoryResumeTarget(history: AnimeHistoryDisplay, episodes: List<Episode>): Long {
    if (!history.seen) return history.episodeId
    return episodes.sortedWith(animeEpisodeNumberOrder)
        .dropWhile { it.id != history.episodeId }
        .drop(1)
        .firstOrNull { !it.seen }
        ?.id
        ?: history.episodeId
}

@Composable
internal fun rememberAnimeHistory(): List<AnimeHistoryDisplay> {
    if (!TsuzukiAnime.enabled || LocalInspectionMode.current) return emptyList()
    val repository = remember { Injekt.get<AnimeHistoryRepository>() }
    val rows by remember(repository) { repository.subscribeHistory() }.collectAsState(emptyList())
    return rows
}

@Composable
internal fun TsuzukiAnimeHistory(rows: List<AnimeHistoryDisplay>, searchQuery: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val library = remember { Injekt.get<AnimeLibraryStore>() }
    val repository = remember { Injekt.get<AnimeHistoryRepository>() }
    val timeZone = TimeZone.currentSystemDefault()
    val groups = remember(rows, searchQuery, timeZone) { groupAnimeHistory(rows, searchQuery, timeZone) }
    val timeFormatter = rememberTimestampFormatter()
    var selected by remember { mutableStateOf<AnimeHistoryDisplay?>(null) }
    var removeAll by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    fun perform(operation: suspend () -> Unit) {
        error = false
        scope.launch {
            try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = true
            }
        }
    }

    if (groups.isEmpty() && !error) {
        EmptyScreen(stringRes = MR.strings.no_results_found)
    } else {
        FastScrollLazyColumn {
            if (error) {
                item {
                    Text(
                        stringResource(MR.strings.unknown_error),
                        color = TsuzukiTheme.tokens.dotOn,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            for ((day, episodes) in groups) {
                item(key = "anime-history-$day") {
                    TsuzukiSectionLabel(relativeDateText(day))
                }
                items(episodes, key = { "anime-history-${it.episodeId}" }) { item ->
                    AnimeHistoryRow(
                        item = item,
                        timeFormatter = timeFormatter,
                        onResume = {
                            perform {
                                val target = if (item.seen) {
                                    animeHistoryResumeTarget(item, library.getEpisodes(item.animeId))
                                } else {
                                    item.episodeId
                                }
                                val track = !Injekt.get<BasePreferences>().incognitoMode.get()
                                AnimePlayback.launchEpisode(context, target, track)
                            }
                        },
                        onRemove = {
                            selected = item
                            removeAll = false
                        },
                    )
                }
            }
        }
    }
    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(stringResource(MR.strings.action_remove)) },
            text = {
                Column {
                    Text(item.title)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = removeAll, onCheckedChange = { removeAll = it })
                        Text(androidStringResource(R.string.tsz_anime_history_remove_all))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val episodeId = if (removeAll) null else item.episodeId
                    selected = null
                    perform { repository.removeHistory(item.animeId, episodeId) }
                }) { Text(stringResource(MR.strings.action_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}

@Composable
private fun AnimeHistoryRow(
    item: AnimeHistoryDisplay,
    timeFormatter: DateTimeFormatter,
    onResume: () -> Unit,
    onRemove: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val episode = if (item.episodeNumber.isFinite() && item.episodeNumber >= 0) {
        val number = NumberFormat.getNumberInstance().apply { isGroupingUsed = false }.format(item.episodeNumber)
        androidStringResource(R.string.tsz_anime_history_episode, number)
    } else {
        item.episodeName
    }
    val remaining = when {
        item.seen -> androidStringResource(R.string.tsz_anime_watched)
        item.duration > 0 -> androidStringResource(
            R.string.tsz_anime_history_remaining,
            DateUtils.formatElapsedTime((item.duration - item.position).coerceAtLeast(0)),
        )
        else -> null
    }
    val time = timeFormatter.format(item.lastSeen.toInstant().atZone(ZoneId.systemDefault()))
    TsuzukiTile(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
            .tsuzukiClickable(role = Role.Button, onClick = onResume),
        padding = 8.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(84.dp, 48.dp).clip(RoundedCornerShape(8.dp)).background(tokens.track),
                contentAlignment = Alignment.Center,
            ) {
                val cover = item.previewUrl ?: item.thumbnailUrl
                if (cover == null) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = tokens.mutedText)
                } else {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = DotCoverFilter.takeIf { tokens.isDot },
                    )
                }
                if (item.duration > 0) {
                    TsuzukiProgressBar(
                        progress = (item.position.toFloat() / item.duration).coerceIn(0f, 1f),
                        modifier = Modifier.align(Alignment.BottomCenter),
                        thickness = 3.dp,
                        showDots = false,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    tsuzukiMenuLabel(item.title),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tsuzukiMenuLabel(listOfNotNull(episode, remaining, time).joinToString(" · ")),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, stringResource(MR.strings.action_remove), tint = tokens.mutedText)
            }
        }
    }
}
