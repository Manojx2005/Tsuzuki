package eu.kanade.presentation.tsuzuki.track

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.DotCoverFilter
import eu.kanade.presentation.theme.tsuzuki.LocalTsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.ui.manga.track.TrackItem
import eu.kanade.tachiyomi.util.lang.toJavaLocalDate
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.format.DateTimeFormatter

@Composable
internal fun TsuzukiTrackInfoDialogHome(
    trackItems: List<TrackItem>,
    dateFormat: DateTimeFormatter,
    onStatusClick: (TrackItem) -> Unit,
    onChapterClick: (TrackItem) -> Unit,
    onScoreClick: (TrackItem) -> Unit,
    onStartDateEdit: (TrackItem) -> Unit,
    onEndDateEdit: (TrackItem) -> Unit,
    onNewSearch: (TrackItem) -> Unit,
    onOpenInBrowser: (TrackItem) -> Unit,
    onRemoved: (TrackItem) -> Unit,
    onCopyLink: (TrackItem) -> Unit,
    onTogglePrivate: (TrackItem) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val context = LocalContext.current
    val syncPreference = remember { runCatching { Injekt.get<TrackPreferences>().autoUpdateTrack }.getOrNull() }
    val autoSync = syncPreference?.collectAsState()?.value == true
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = tsuzukiCase(stringResource(MR.strings.track)).orEmpty(),
            color = tokens.dotOn,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        if (autoSync && trackItems.any { it.track != null }) {
            Text(
                text = tsuzukiCase(stringResource(MR.strings.tsz_tracking_chapter_sync)).orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CompositionLocalProvider(LocalTsuzukiTokens provides tokens.copy(tileRadius = 24.dp)) {
            trackItems.forEach { item ->
                key(item.tracker.id) {
                    val tracker = item.tracker
                    val track = item.track
                    if (track == null) {
                        UnlinkedTrackerTile(tracker) { onNewSearch(item) }
                    } else {
                        TsuzukiTile(Modifier.fillMaxWidth(), padding = 14.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (track.private) {
                                                Badge(containerColor = tokens.accent, contentColor = tokens.onAccent) {
                                                    Icon(
                                                        imageVector = Icons.Filled.VisibilityOff,
                                                        contentDescription = stringResource(
                                                            MR.strings.tracked_privately,
                                                        ),
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                }
                                            }
                                        },
                                    ) {
                                        TrackerLogo(
                                            tracker = tracker,
                                            modifier = Modifier.tsuzukiClickable(
                                                onClickLabel = stringResource(MR.strings.action_open_in_browser),
                                                onLongClickLabel = stringResource(MR.strings.action_copy_link),
                                                role = Role.Button,
                                                onClick = { onOpenInBrowser(item) },
                                                onLongClick = { onCopyLink(item) },
                                            ),
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).tsuzukiClickable(
                                            onClickLabel = stringResource(MR.strings.action_edit),
                                            onLongClickLabel = stringResource(MR.strings.action_copy_to_clipboard),
                                            role = Role.Button,
                                            onClick = { onNewSearch(item) },
                                            onLongClick = { context.copyToClipboard(track.title, track.title) },
                                        ),
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        TrackerName(tracker)
                                        Text(
                                            text = tsuzukiCase(track.title).orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = tokens.dotOn.copy(alpha = 0.72f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    TextButton(onClick = { onNewSearch(item) }) {
                                        Text(
                                            text = tsuzukiCase(stringResource(MR.strings.action_edit)).orEmpty(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = tokens.accent,
                                        )
                                    }
                                    TrackerTileMenu(
                                        isPrivate = track.private,
                                        onOpenInBrowser = { onOpenInBrowser(item) },
                                        onCopyLink = { onCopyLink(item) },
                                        onTogglePrivate = { onTogglePrivate(item) }
                                            .takeIf { tracker.supportsPrivateTracking },
                                        onRemoved = { onRemoved(item) },
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TrackerMetric(
                                        label = stringResource(MR.strings.track_status),
                                        value = tracker.getStatus(track.status)?.let { stringResource(it) },
                                        modifier = Modifier.weight(1f),
                                        accent = true,
                                        onClick = { onStatusClick(item) },
                                    )
                                    TrackerMetric(
                                        label = stringResource(MR.strings.chapters),
                                        value = trackerChapterProgress(track.lastChapterRead, track.totalChapters),
                                        modifier = Modifier.weight(1f),
                                        onClick = { onChapterClick(item) },
                                    )
                                    if (tracker.getScoreList().isNotEmpty()) {
                                        TrackerMetric(
                                            label = stringResource(MR.strings.score),
                                            value = if (track.score != 0.0) tracker.displayScore(track) else null,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onScoreClick(item) },
                                        )
                                    }
                                }
                                if (tracker.supportsReadingDates) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TrackerMetric(
                                            label = stringResource(MR.strings.track_started_reading_date),
                                            value = track.startDate.takeUnless { it == 0L }
                                                ?.let { dateFormat.format(it.toJavaLocalDate()) },
                                            modifier = Modifier.weight(1f),
                                            onClick = { onStartDateEdit(item) },
                                        )
                                        TrackerMetric(
                                            label = stringResource(MR.strings.track_finished_reading_date),
                                            value = track.finishDate.takeUnless { it == 0L }
                                                ?.let { dateFormat.format(it.toJavaLocalDate()) },
                                            modifier = Modifier.weight(1f),
                                            onClick = { onEndDateEdit(item) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlinkedTrackerTile(tracker: Tracker, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(
        modifier = Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button, onClick = onClick),
        padding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrackerLogo(tracker)
            Column(Modifier.weight(1f)) {
                TrackerName(tracker)
                Text(
                    text = tsuzukiCase(stringResource(MR.strings.add_tracking)).orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.accent,
                )
            }
            Icon(Icons.Filled.Add, contentDescription = null, tint = tokens.accent)
        }
    }
}

@Composable
private fun TrackerLogo(tracker: Tracker, modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(tracker.getLogo()),
            contentDescription = tracker.name,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)),
            colorFilter = DotCoverFilter.takeIf { TsuzukiTheme.tokens.isDot },
        )
    }
}

@Composable
private fun TrackerName(tracker: Tracker) {
    Text(
        text = tsuzukiCase(tracker.name).orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        color = TsuzukiTheme.tokens.dotOn,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TrackerMetric(
    label: String,
    value: String?,
    modifier: Modifier,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier.clip(shape).background(tokens.dotOn.copy(alpha = 0.06f))
            .tsuzukiClickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tsuzukiCase(label).orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = tokens.dotOn.copy(alpha = 0.72f),
        )
        Text(
            text = tsuzukiCase(value ?: stringResource(MR.strings.none)).orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            color = when {
                value == null -> tokens.dotOn.copy(alpha = 0.52f)
                accent -> tokens.accent
                else -> tokens.dotOn
            },
        )
    }
}

@Composable
private fun TrackerTileMenu(
    isPrivate: Boolean,
    onOpenInBrowser: () -> Unit,
    onCopyLink: () -> Unit,
    onTogglePrivate: (() -> Unit)?,
    onRemoved: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(MR.strings.label_more), tint = TsuzukiTheme.tokens.dotOn)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TsuzukiMenuItem(stringResource(MR.strings.action_open_in_browser), onClick = {
                onOpenInBrowser()
                expanded = false
            })
            TsuzukiMenuItem(stringResource(MR.strings.action_copy_link), onClick = {
                onCopyLink()
                expanded = false
            })
            if (onTogglePrivate != null) {
                val action = if (isPrivate) {
                    MR.strings.action_toggle_private_off
                } else {
                    MR.strings.action_toggle_private_on
                }
                TsuzukiMenuItem(stringResource(action), onClick = {
                    onTogglePrivate()
                    expanded = false
                })
            }
            TsuzukiMenuItem(stringResource(MR.strings.action_remove), danger = true, onClick = {
                onRemoved()
                expanded = false
            })
        }
    }
}

internal fun trackerChapterProgress(lastChapter: Double, totalChapters: Long): String {
    val current = lastChapter.toInt()
    return if (totalChapters > 0) "$current / $totalChapters" else current.toString()
}
