package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.theme.tsuzuki.TsuzukiModalBottomSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.track.TrackDateSelector
import eu.kanade.presentation.track.TrackScoreSelector
import eu.kanade.presentation.tsuzuki.track.TsuzukiTrackerSearch
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackEdit
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackSearch
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackStatus
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTracking
import eu.kanade.tachiyomi.tsuzuki.anime.track.toTrackDate
import eu.kanade.tachiyomi.tsuzuki.anime.track.toTrackLocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.res.stringResource as androidStringResource

private enum class EditTrackField { STATUS, SCORE, START, FINISH }

@Composable
internal fun AnimeTrackingSheet(anime: Anime, onDismiss: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val scope = rememberCoroutineScope()
    val tracking = remember { Injekt.get<AnimeTracking>() }
    val tracks by remember(anime.id) { tracking.observe(anime.id) }.collectAsState(emptyList())
    val autoUpdate by tracking.autoUpdate.collectAsState()
    var search by remember { mutableStateOf<Tracker?>(null) }
    var edit by remember { mutableStateOf<Pair<AnimeTrack, EditTrackField>?>(null) }
    var unlink by remember { mutableStateOf<AnimeTrack?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    fun runTask(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = false
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = true
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(anime.id) {
        try {
            tracking.refresh(anime.id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = true
        }
    }

    TsuzukiModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tsuzukiMenuLabel(stringResource(MR.strings.track)),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineLarge,
                    color = tokens.dotOn,
                )
                if (tracks.isNotEmpty()) {
                    Text(
                        tsuzukiMenuLabel(androidStringResource(R.string.tsz_anime_track_linked_count, tracks.size)),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.mutedText,
                    )
                }
            }
            if (error) {
                Text(stringResource(MR.strings.unknown_error), color = tokens.accent)
            }
            tracking.services.forEach { service ->
                key(service.id) {
                    val loggedIn by service.isLoggedInFlow.collectAsState(service.isLoggedIn)
                    val track = tracks.firstOrNull { it.syncId == service.id }
                    AnimeTrackerTile(
                        service = service,
                        track = track,
                        loggedIn = loggedIn,
                        enabled = !busy,
                        onSearch = { search = service },
                        onEdit = { field -> track?.let { edit = it to field } },
                        onUnlink = { unlink = track },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().toggleable(
                    value = autoUpdate,
                    role = Role.Checkbox,
                    onValueChange = tracking.autoUpdate::set,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = autoUpdate, onCheckedChange = null)
                Text(
                    tsuzukiMenuLabel(androidStringResource(R.string.tsz_anime_track_auto_update)),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    search?.let { service ->
        AnimeTrackSearchDialog(
            anime = anime,
            service = service,
            tracking = tracking,
            onDismiss = { search = null },
        )
    }
    edit?.let { (track, field) ->
        EditTrackDialog(track, field, onDismiss = { edit = null }) { changes ->
            edit = null
            runTask { tracking.edit(track, changes) }
        }
    }
    unlink?.let { track ->
        AlertDialog(
            onDismissRequest = { unlink = null },
            title = { Text(androidStringResource(R.string.tsz_anime_track_unlink)) },
            text = { Text(track.title) },
            confirmButton = {
                TextButton(onClick = {
                    unlink = null
                    runTask { tracking.unlink(track) }
                }) { Text(stringResource(MR.strings.action_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { unlink = null }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}

@Composable
private fun AnimeTrackerTile(
    service: Tracker,
    track: AnimeTrack?,
    loggedIn: Boolean,
    enabled: Boolean,
    onSearch: () -> Unit,
    onEdit: (EditTrackField) -> Unit,
    onUnlink: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(Modifier.fillMaxWidth(), padding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tsuzukiMenuLabel(service.name),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.dotOn,
                )
                if (loggedIn && track != null) {
                    val status = AnimeTrackStatus.entries.firstOrNull { it.idFor(service.id) == track.status }
                    TextButton(enabled = enabled, onClick = { onEdit(EditTrackField.STATUS) }) {
                        Text(tsuzukiMenuLabel(status?.label() ?: stringResource(MR.strings.status)))
                    }
                }
            }
            if (!loggedIn) {
                Text(
                    tsuzukiMenuLabel(androidStringResource(R.string.tsz_anime_track_login)),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else if (track == null) {
                TextButton(onClick = onSearch, enabled = enabled) {
                    Text(tsuzukiMenuLabel(stringResource(MR.strings.add_tracking)))
                }
            }
            if (track != null) {
                val progress = formatTrackNumber(track.lastEpisodeSeen)
                val episodes = if (track.totalEpisodes > 0) {
                    progress + " / " + formatTrackNumber(track.totalEpisodes.toDouble())
                } else {
                    progress
                }
                val canEdit = enabled && loggedIn
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TrackMetric(
                        stringResource(MR.strings.tsz_anime_debug_episodes),
                        episodes,
                        Modifier.weight(1f),
                    )
                    TrackMetric(
                        stringResource(MR.strings.score),
                        track.score.takeIf { it > 0.0 }?.let(::formatTrackNumber) ?: "—",
                        Modifier.weight(1f),
                        { onEdit(EditTrackField.SCORE) }.takeIf { canEdit },
                    )
                    TrackMetric(
                        stringResource(MR.strings.track_started_reading_date),
                        track.startDate.displayDate(),
                        Modifier.weight(1f),
                        { onEdit(EditTrackField.START) }.takeIf { canEdit },
                    )
                }
                if (loggedIn) {
                    TextButton(onClick = { onEdit(EditTrackField.FINISH) }, enabled = enabled) {
                        val label = stringResource(MR.strings.track_finished_reading_date)
                        Text(tsuzukiMenuLabel(label + " · " + track.finishDate.displayDate()))
                    }
                }
                TextButton(onClick = onUnlink, enabled = enabled) {
                    Text(tsuzukiMenuLabel(androidStringResource(R.string.tsz_anime_track_unlink)))
                }
            }
        }
    }
}

@Composable
private fun TrackMetric(label: String, value: String, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(
        modifier = if (onClick == null) modifier else modifier.tsuzukiClickable(role = Role.Button, onClick = onClick),
        padding = 8.dp,
    ) {
        Column {
            Text(
                tsuzukiMenuLabel(label),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.mutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                tsuzukiMenuLabel(value),
                style = MaterialTheme.typography.titleSmall,
                color = tokens.dotOn,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTrackDialog(
    track: AnimeTrack,
    field: EditTrackField,
    onDismiss: () -> Unit,
    onSave: (AnimeTrackEdit) -> Unit,
) {
    when (field) {
        EditTrackField.STATUS -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(MR.strings.status)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AnimeTrackStatus.entries.forEach { status ->
                        TextButton(onClick = { onSave(AnimeTrackEdit(status = status)) }) {
                            Text(tsuzukiMenuLabel(status.label()))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) } },
        )
        EditTrackField.SCORE -> Dialog(onDismissRequest = onDismiss) {
            val steps = if (track.syncId == TrackerManager.ANILIST) 10 else 1
            val scores = remember(steps) { (0..10 * steps).map { it.toDouble() / steps } }
            val labels = remember(scores) { listOf("—") + scores.map(::formatTrackNumber) }
            var selection by remember { mutableStateOf(formatTrackNumber(track.score.coerceIn(0.0, 10.0))) }
            TrackScoreSelector(
                selection = selection,
                onSelectionChange = { selection = it },
                selections = labels,
                onConfirm = {
                    val value = scores.getOrNull(labels.indexOf(selection) - 1) ?: 0.0
                    onSave(AnimeTrackEdit(score = value))
                },
                onDismissRequest = onDismiss,
            )
        }
        EditTrackField.START, EditTrackField.FINISH -> Dialog(onDismissRequest = onDismiss) {
            val start = field == EditTrackField.START
            val date = if (start) track.startDate else track.finishDate
            val today = LocalDate.now().toTrackDate()
            val selectable = remember(today) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= today
                }
            }
            fun edit(value: Long) =
                if (start) AnimeTrackEdit(startDate = value) else AnimeTrackEdit(finishDate = value)
            TrackDateSelector(
                title = stringResource(
                    if (start) MR.strings.track_started_reading_date else MR.strings.track_finished_reading_date,
                ),
                initialSelectedDateMillis = if (date == 0L) today else date,
                selectableDates = selectable,
                onConfirm = { onSave(edit(it)) },
                onRemove = { onSave(edit(0L)) }.takeIf { date != 0L },
                onDismissRequest = onDismiss,
            )
        }
    }
}

@Composable
private fun AnimeTrackSearchDialog(
    anime: Anime,
    service: Tracker,
    tracking: AnimeTracking,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberTextFieldState(anime.title)
    var results by remember { mutableStateOf<Result<List<TrackSearch>>?>(null) }
    var choices by remember { mutableStateOf(emptyMap<Long, AnimeTrackSearch>()) }
    var selected by remember { mutableStateOf<TrackSearch?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var binding by remember { mutableStateOf(false) }

    fun search() {
        if (binding) return
        searchJob?.cancel()
        selected = null
        results = null
        searchJob = scope.launch {
            val text = state.text.toString().trim()
            try {
                choices = if (text.isEmpty()) emptyMap() else tracking.search(service.id, text).associateBy { it.id }
                results = Result.success(
                    choices.values.map { item ->
                        TrackSearch.create(service.id).apply {
                            remote_id = item.id
                            title = item.title
                            tracking_url = item.url
                            cover_url = item.cover.orEmpty()
                            total_chapters = item.totalEpisodes
                        }
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                results = Result.failure(IllegalStateException())
            }
        }
    }

    LaunchedEffect(service.id) { search() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        TsuzukiTrackerSearch(
            state = state,
            onDispatchQuery = ::search,
            queryResult = results,
            selected = selected,
            onSelectedChange = { if (!binding) selected = it },
            onConfirmSelection = {
                val choice = selected?.let { choices[it.remote_id] }
                if (choice != null && !binding) {
                    binding = true
                    selected = null
                    scope.launch {
                        try {
                            tracking.bind(anime.id, service.id, choice)
                            onDismiss()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            results = Result.failure(IllegalStateException())
                        } finally {
                            binding = false
                        }
                    }
                }
            },
            onDismissRequest = onDismiss,
            supportsPrivateTracking = false,
            altTitles = anime.alternativeTitles.orEmpty(),
            countLabel = { androidStringResource(R.string.tsz_anime_track_episode_count, it) },
        )
    }
}

private fun Long.displayDate(): String =
    toTrackLocalDate()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "—"

private fun formatTrackNumber(value: Double): String =
    NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(value)

@Composable
private fun AnimeTrackStatus.label(): String = when (this) {
    AnimeTrackStatus.WATCHING -> androidStringResource(R.string.tsz_anime_track_watching)
    AnimeTrackStatus.COMPLETED -> stringResource(MR.strings.completed)
    AnimeTrackStatus.ON_HOLD -> stringResource(MR.strings.on_hold)
    AnimeTrackStatus.DROPPED -> stringResource(MR.strings.dropped)
    AnimeTrackStatus.PLANNING -> androidStringResource(R.string.tsz_anime_track_planning)
    AnimeTrackStatus.REWATCHING -> androidStringResource(R.string.tsz_anime_track_rewatching)
}
