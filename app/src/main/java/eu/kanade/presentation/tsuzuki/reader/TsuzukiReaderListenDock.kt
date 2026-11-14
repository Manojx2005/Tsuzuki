package eu.kanade.presentation.tsuzuki.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.readerDockFill
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.tsuzuki.tts.TtsSleepTimer
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat

internal fun sleepCountdownLabel(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
}

/** Boards 4 TabletListenDock and 06-reader ListenMini, using only live reader state. */
@Composable
internal fun TsuzukiReaderListenDock(
    manga: Manga?,
    chapterTitle: String?,
    state: ReaderListeningState?,
    speed: Float,
    onTogglePlayback: () -> Unit,
    onStepParagraph: (Boolean) -> Unit,
    onSetTimer: (TtsSleepTimer) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    var choosingTimer by remember { mutableStateOf(false) }
    val timer = state?.timer ?: TtsSleepTimer.Off
    val timerText = when (timer) {
        TtsSleepTimer.Off -> stringResource(MR.strings.tsz_reader_timer_short)
        TtsSleepTimer.ChapterEnd -> stringResource(MR.strings.tsz_reader_sleep_chapter_short)
        else -> sleepCountdownLabel(state?.remainingSeconds ?: checkNotNull(timer.minutes) * 60L)
    }
    val timerDescription = stringResource(MR.strings.tsz_reader_sleep_timer) + " · " +
        if (timer.minutes != null) timerText else sleepTimerLabel(timer)
    val rate = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 } }.format(speed)
    val paragraph = state?.position?.let {
        stringResource(MR.strings.tsz_reader_listen_position, it.index + 1, it.count)
    }
    val summary = listOfNotNull(paragraph, "$rate×").joinToString(" · ")
    val shape = RoundedCornerShape(32.dp)
    BoxWithConstraints(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    ) {
        val phone = maxWidth < 450.dp
        Row(
            Modifier.fillMaxWidth().height(64.dp).clip(shape).background(tokens.readerDockFill)
                .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val close = stringResource(MR.strings.action_close)
            if (phone && manga != null) {
                Box(
                    Modifier.size(48.dp).tsuzukiClickable(role = Role.Button, onClick = onClose)
                        .semantics { contentDescription = close },
                    contentAlignment = Alignment.Center,
                ) {
                    MangaCover.Book(data = manga, modifier = Modifier.width(28.dp).height(44.dp))
                }
            } else {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.KeyboardArrowDown, close, tint = tokens.dotOn)
                }
            }
            Column(Modifier.weight(1f)) {
                chapterTitle?.takeIf(String::isNotBlank)?.let {
                    Text(
                        tsuzukiMenuLabel(it),
                        color = tokens.dotOn,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    tsuzukiMenuLabel(summary),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier.height(48.dp).widthIn(max = 120.dp).tsuzukiClickable(role = Role.Button) {
                    choosingTimer = true
                }
                    .semantics { contentDescription = timerDescription },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier.height(32.dp).border(
                        1.dp,
                        if (timer ==
                            TtsSleepTimer.Off
                        ) {
                            tokens.track
                        } else {
                            tokens.accent
                        },
                        CircleShape,
                    )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    val ink = if (timer == TtsSleepTimer.Off) tokens.dotOn else tokens.accent
                    Icon(Icons.Outlined.Timer, null, Modifier.size(14.dp), tint = ink)
                    Text(
                        tsuzukiMenuLabel(timerText),
                        color = ink,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!phone) {
                IconButton(onClick = { onStepParagraph(false) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.FastRewind,
                        stringResource(TDMR.strings.tts_prev_paragraph),
                        tint = tokens.dotOn,
                    )
                }
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(tokens.accent)
                    .tsuzukiClickable(role = Role.Button, onClick = onTogglePlayback),
                contentAlignment = Alignment.Center,
            ) {
                val playing = state?.active == true && !state.paused
                Icon(
                    if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    stringResource(if (playing) MR.strings.action_pause else MR.strings.tsz_widget_listening_play),
                    tint = tokens.onAccent,
                )
            }
            if (!phone) {
                IconButton(onClick = { onStepParagraph(true) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.FastForward,
                        stringResource(TDMR.strings.tts_next_paragraph),
                        tint = tokens.dotOn,
                    )
                }
            }
        }
    }
    if (choosingTimer) {
        AlertDialog(
            onDismissRequest = { choosingTimer = false },
            title = { Text(tsuzukiMenuLabel(stringResource(MR.strings.tsz_reader_sleep_timer))) },
            text = {
                Column {
                    TtsSleepTimer.entries.forEach { choice ->
                        TextButton(onClick = {
                            onSetTimer(choice)
                            choosingTimer = false
                        }) {
                            Text(
                                sleepTimerLabel(choice),
                                color = if (choice == timer) tokens.accent else tokens.dotOn,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { choosingTimer = false }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}
