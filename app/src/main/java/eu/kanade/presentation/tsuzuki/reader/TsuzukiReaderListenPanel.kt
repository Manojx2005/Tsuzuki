package eu.kanade.presentation.tsuzuki.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.tsuzuki.tts.TtsSleepTimer
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.TtsController
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.text.NumberFormat

@Composable
internal fun TsuzukiReaderListenPanel(
    manga: Manga?,
    chapterTitle: String?,
    state: ReaderListeningState,
    controller: TtsController,
    preferences: ReaderPreferences,
    compact: Boolean,
    onTogglePlayback: () -> Unit,
    onStepParagraph: (forward: Boolean) -> Unit,
    onSeekParagraph: (index: Int) -> Unit,
    onClose: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val speed by preferences.novelTtsSpeed.collectAsState()
    val rateFormat = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 } }
    val voice = state.voice.ifBlank { stringResource(TDMR.strings.pref_novel_tts_voice_default) }
    var chooser by remember { mutableStateOf<ListenChooser?>(null) }
    var voices by remember { mutableStateOf(emptyList<Pair<String, String>>()) }

    fun applyPlaybackSetting(setPreference: () -> Unit) {
        setPreference()
        controller.applySettings()
        if (controller.isSpeaking()) onSeekParagraph(controller.getParagraphProgress().first)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tsuzukiMenuLabel(stringResource(MR.strings.tsz_reader_listen)),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, stringResource(MR.strings.action_close))
                }
            }
        }
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!compact && manga != null) {
                    MangaCover.Book(
                        data = manga,
                        modifier = Modifier.width(110.dp),
                        contentDescription = stringResource(MR.strings.manga_cover),
                    )
                }
                val title = chapterTitle?.takeIf(String::isNotBlank) ?: manga?.title.orEmpty()
                if (title.isNotBlank()) {
                    Text(tsuzukiMenuLabel(title), style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    tsuzukiMenuLabel("$voice · ${rateFormat.format(speed)}×"),
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        state.position?.let { position ->
            item { TsuzukiProgressBar(position.progress, spreadDots = true) }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onStepParagraph(false) }, enabled = state.position != null) {
                    Icon(Icons.Outlined.FastRewind, stringResource(TDMR.strings.tts_prev_paragraph))
                }
                FilledIconButton(
                    onClick = onTogglePlayback,
                    modifier = Modifier.size(60.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = tokens.accent,
                        contentColor = tokens.onAccent,
                    ),
                ) {
                    Icon(
                        if (state.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                        stringResource(
                            if (state.paused) MR.strings.tsz_widget_listening_play else MR.strings.action_pause,
                        ),
                    )
                }
                IconButton(onClick = { onStepParagraph(true) }, enabled = state.position != null) {
                    Icon(Icons.Outlined.FastForward, stringResource(TDMR.strings.tts_next_paragraph))
                }
            }
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TsuzukiPill(
                    text = stringResource(TDMR.strings.pref_novel_tts_speed) + " " + rateFormat.format(speed) + "×",
                    selected = false,
                    onClick = { chooser = ListenChooser.Speed },
                    role = Role.Button,
                )
                TsuzukiPill(
                    text = stringResource(TDMR.strings.pref_novel_tts_voice),
                    selected = false,
                    onClick = {
                        voices = controller.getAvailableVoices()
                        chooser = ListenChooser.Voice
                    },
                    role = Role.Button,
                )
                TsuzukiPill(
                    text = stringResource(MR.strings.tsz_reader_sleep_timer) + " · " + sleepTimerLabel(state.timer),
                    selected = state.timer != TtsSleepTimer.Off,
                    onClick = { chooser = ListenChooser.Sleep },
                    role = Role.Button,
                )
            }
        }
        if (state.paragraphs.isNotEmpty()) {
            item { TsuzukiSectionLabel(stringResource(MR.strings.tsz_reader_listen_paragraphs)) }
            itemsIndexed(state.paragraphs) { index, paragraph ->
                val current = index == state.position?.index
                TsuzukiTile(
                    modifier = Modifier.fillMaxWidth()
                        .tsuzukiClickable(role = Role.Button, onClick = { onSeekParagraph(index) }),
                    padding = 10.dp,
                ) {
                    Text(
                        "${index + 1}. $paragraph",
                        color = if (current) tokens.accent else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    chooser?.let { selected ->
        val (title, choices) = when (selected) {
            ListenChooser.Speed -> stringResource(TDMR.strings.pref_novel_tts_speed) to
                listOf(0.75f, 1f, 1.25f, 1.5f, 2f).map { rate ->
                    ListenChoice(rateFormat.format(rate) + "×", rate == speed) {
                        applyPlaybackSetting { preferences.novelTtsSpeed.set(rate) }
                    }
                }
            ListenChooser.Voice -> stringResource(TDMR.strings.pref_novel_tts_voice) to
                (listOf("" to stringResource(TDMR.strings.pref_novel_tts_voice_default)) + voices).map { (id, label) ->
                    ListenChoice(label, id == preferences.novelTtsVoice.get()) {
                        applyPlaybackSetting { preferences.novelTtsVoice.set(id) }
                    }
                }
            ListenChooser.Sleep -> stringResource(MR.strings.tsz_reader_sleep_timer) to
                TtsSleepTimer.entries.map { timer ->
                    ListenChoice(sleepTimerLabel(timer), timer == state.timer) { controller.setSleepTimer(timer) }
                }
        }
        AlertDialog(
            onDismissRequest = { chooser = null },
            title = { Text(tsuzukiMenuLabel(title)) },
            text = {
                LazyColumn {
                    itemsIndexed(choices) { _, choice ->
                        TextButton(
                            onClick = {
                                choice.apply()
                                chooser = null
                            },
                        ) {
                            Text(
                                choice.label,
                                color = if (choice.selected) tokens.accent else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { chooser = null }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}

private enum class ListenChooser { Speed, Voice, Sleep }

private data class ListenChoice(val label: String, val selected: Boolean, val apply: () -> Unit)

@Composable
private fun sleepTimerLabel(timer: TtsSleepTimer): String = when (timer) {
    TtsSleepTimer.Off -> stringResource(MR.strings.tsz_reader_sleep_off)
    TtsSleepTimer.ChapterEnd -> stringResource(MR.strings.tsz_reader_sleep_chapter)
    else -> stringResource(MR.strings.minute_short, checkNotNull(timer.minutes))
}
