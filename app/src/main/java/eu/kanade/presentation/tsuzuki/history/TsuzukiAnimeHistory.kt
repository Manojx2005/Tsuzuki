package eu.kanade.presentation.tsuzuki.history

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.GridView
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.anime.AnimeDetailsRoute
import eu.kanade.presentation.tsuzuki.anime.openAnime
import eu.kanade.presentation.util.rememberTimestampFormatter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.animeEpisodeNumberOrder
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayback
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
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

/** History's "clear all" also clears anime watch history while anime is on (AY-INT item 3). */
internal fun clearAllAnimeHistory(animeEnabled: Boolean = TsuzukiAnime.enabled) {
    if (!animeEnabled) return
    Injekt.get<kotlinx.coroutines.CoroutineScope>().launchIO {
        try {
            Injekt.get<AnimeHistoryRepository>().removeAllHistory()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Could not clear anime history" }
        }
    }
}

/** The extra line for the "clear all" confirmation, or null while anime is off. */
@Composable
internal fun animeClearHistoryNote(): String? =
    if (TsuzukiAnime.enabled) stringResource(MR.strings.tsz_history_clear_includes_anime) else null

internal fun showAnimeHistory(hasHistory: Boolean, animeEnabled: Boolean = TsuzukiAnime.enabled): Boolean =
    animeEnabled && hasHistory

internal fun groupAnimeHistory(
    rows: List<AnimeHistoryDisplay>,
    query: String?,
    timeZone: TimeZone,
    lastOnly: Boolean = false,
): Map<LocalDate, List<AnimeHistoryDisplay>> {
    val search = query.orEmpty().trim()
    return rows.filter {
        search.isEmpty() || it.title.contains(search, ignoreCase = true) ||
            it.episodeName.contains(search, ignoreCase = true)
    }.sortedByDescending { it.lastSeen }
        // "Last only", as in book History: the newest episode of each anime.
        .let { sorted -> if (lastOnly) sorted.distinctBy { it.animeId } else sorted }
        .groupBy {
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
internal fun TsuzukiAnimeHistory(rows: List<AnimeHistoryDisplay>, searchQuery: String?, lastOnly: Boolean = false) {
    val timeZone = TimeZone.currentSystemDefault()
    val groups = remember(rows, searchQuery, timeZone, lastOnly) {
        groupAnimeHistory(rows, searchQuery, timeZone, lastOnly)
    }
    val timeFormatter = rememberTimestampFormatter()

    if (groups.isEmpty()) {
        EmptyScreen(stringRes = MR.strings.no_results_found)
    } else {
        FastScrollLazyColumn {
            // Above the list, as the book History tiles are.
            item(key = "anime-history-stats") {
                TsuzukiAnimeHistoryStatsTiles(rows)
            }
            for ((day, episodes) in groups) {
                item(key = "anime-history-$day") {
                    TsuzukiSectionLabel(relativeDateText(day))
                }
                items(episodes, key = { "anime-history-${it.episodeId}" }) { item ->
                    TsuzukiAnimeHistoryEntry(item, timeFormatter)
                }
            }
        }
    }
}

/**
 * One watched episode: tap resumes (the next unwatched episode when this one is finished), the remove button asks
 * whether to drop this episode or the whole anime from history. Used by the Anime pill and by History › All.
 */
@Composable
internal fun TsuzukiAnimeHistoryEntry(
    item: AnimeHistoryDisplay,
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // History is a Home tab inside the main navigator; null only in previews.
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val library = remember { Injekt.get<AnimeLibraryStore>() }
    val repository = remember { Injekt.get<AnimeHistoryRepository>() }
    var confirming by remember { mutableStateOf(false) }
    var removeAll by remember { mutableStateOf(false) }

    fun perform(operation: suspend () -> Unit) {
        scope.launch {
            try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                context.toast(MR.strings.unknown_error)
            }
        }
    }

    Box(modifier) {
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
                confirming = true
                removeAll = false
            },
            onOpenDetails = { navigator?.push(AnimeDetailsRoute(item.animeId)) ?: openAnime(context, item.animeId) },
        )
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
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
                    confirming = false
                    perform { repository.removeHistory(item.animeId, episodeId) }
                }) { Text(stringResource(MR.strings.action_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(MR.strings.action_cancel)) }
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
    onOpenDetails: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(18.dp)
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
    val progress = when {
        item.seen -> 1f
        item.duration > 0 -> (item.position.toFloat() / item.duration).coerceIn(0f, 1f)
        else -> null
    }
    // Same tile as a book history row (design board "History"): cover, title, "episode · time", progress, remove.
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.tile)
            .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .tsuzukiClickable(role = Role.Button, onClick = onResume)
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // As in book History, the cover opens the details; the rest of the row resumes.
        Box(
            Modifier.width(44.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(6.dp)).background(tokens.track)
                .clickable(role = Role.Button, onClick = onOpenDetails),
            contentAlignment = Alignment.Center,
        ) {
            val cover = item.thumbnailUrl ?: item.previewUrl
            if (cover == null) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = tokens.mutedText)
            } else {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = tsuzukiCoverFilter(),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                tsuzukiMenuLabel(item.title),
                color = tokens.dotOn,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                tsuzukiMenuLabel(listOfNotNull(episode, remaining, time).joinToString(" · ")),
                color = tokens.mutedText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            progress?.let { TsuzukiProgressBar(it, dots = 16, thickness = 4.dp) }
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(MR.strings.action_remove),
                tint = tokens.mutedText,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** History toolbar actions while the Anime pill is selected: clear the whole watch history, after a confirmation. */
@Composable
internal fun TsuzukiAnimeHistoryActions(lastOnly: Boolean, onLastOnlyChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf(false) }
    AppBarActions(
        listOf(
            // Same toggle and labels as the book History toolbar, sharing its setting.
            AppBar.Action(
                title = stringResource(if (lastOnly) MR.strings.tsz_action_list_view else MR.strings.tsz_action_last_only),
                icon = if (lastOnly) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                onClick = { onLastOnlyChanged(!lastOnly) },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.pref_clear_history),
                icon = Icons.Outlined.DeleteSweep,
                onClick = { confirming = true },
            ),
        ),
    )
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(MR.strings.action_remove_everything)) },
            text = { Text(androidStringResource(R.string.tsz_anime_history_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    scope.launch {
                        try {
                            Injekt.get<AnimeHistoryRepository>().removeAllHistory()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logcat(LogPriority.WARN, e) { "Could not clear anime history" }
                            context.toast(MR.strings.unknown_error)
                        }
                    }
                }) { Text(stringResource(MR.strings.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}
