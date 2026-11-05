package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.theme.tsuzuki.TsuzukiModalBottomSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.roundToInt

/** Optional toolbar slot, keeping the legacy download menu and the upstream screen layout intact. */
@Composable
fun tsuzukiDownloadSheet(
    state: MangaViewModel.State.Success,
    onDownload: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
): (@Composable (onDismissRequest: () -> Unit) -> Unit)? {
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE || onDownload == null) return null
    return { dismiss ->
        val readerPreferences = remember { Injekt.get<ReaderPreferences>() }
        val skipFiltered by readerPreferences.skipFiltered.collectAsState()
        val items = if (skipFiltered) state.processedChapters else state.chapters
        val ordered = remember(items, state.manga.sorting) {
            items.sortedWith { a, b -> getChapterSort(state.manga, sortDescending = false)(a.chapter, b.chapter) }
        }
        TsuzukiDownloadSheet(
            chapters = ordered,
            onDismissRequest = dismiss,
            onSave = { onDownload(it, ChapterDownloadAction.START) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TsuzukiDownloadSheet(
    chapters: List<ChapterList.Item>,
    onDismissRequest: () -> Unit,
    onSave: (List<ChapterList.Item>) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val preferences = remember { Injekt.get<DownloadPreferences>() }
    val wifiOnly by preferences.downloadOnlyOverWifi.collectAsState()
    val deleteSlots by preferences.removeAfterReadSlots.collectAsState()
    var previousDeleteSlots by rememberSaveable { mutableStateOf(deleteSlots.coerceAtLeast(0)) }
    var option by rememberSaveable { mutableStateOf(TsuzukiDownloadOption.NEXT_25) }
    var rangeStart by rememberSaveable { mutableStateOf(0) }
    var rangeEnd by rememberSaveable { mutableStateOf(chapters.lastIndex.coerceAtLeast(0)) }
    var menuExpanded by remember { mutableStateOf(false) }
    val lastIndex = chapters.lastIndex.coerceAtLeast(0)
    val customRange = rangeStart.coerceIn(0, lastIndex)..rangeEnd.coerceIn(0, lastIndex)
    val selected = chaptersToSave(chapters, option, customRange)
    val selectedIds = selected.map { it.id }.toSet()
    val selectedIndices = chapters.indices.filter { chapters[it].id in selectedIds }
    val sliderRange = if (option == TsuzukiDownloadOption.CUSTOM) {
        customRange.first.toFloat()..customRange.last.toFloat()
    } else {
        (selectedIndices.firstOrNull() ?: 0).toFloat()..(selectedIndices.lastOrNull() ?: 0).toFloat()
    }
    fun case(text: String) = if (tokens.isDot) text.uppercase() else text
    TsuzukiModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) { close ->
        Column(Modifier.fillMaxWidth().heightIn(max = 700.dp)) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        case(stringResource(MR.strings.tsz_download_title)),
                        Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 30.sp,
                    )
                    Column {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Outlined.MoreVert, stringResource(MR.strings.label_more))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            listOf(
                                TsuzukiDownloadOption.NEXT_10 to stringResource(MR.strings.tsz_download_next, 10),
                                TsuzukiDownloadOption.BOOKMARKED to stringResource(MR.strings.download_bookmarked),
                            ).forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(case(label)) },
                                    onClick = {
                                        option = value
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                val options = listOf(
                    Triple(TsuzukiDownloadOption.NEXT_1, "1", stringResource(MR.strings.tsz_download_next, 1)),
                    Triple(TsuzukiDownloadOption.NEXT_5, "5", stringResource(MR.strings.tsz_download_next, 5)),
                    Triple(TsuzukiDownloadOption.NEXT_25, "25", stringResource(MR.strings.tsz_download_next, 25)),
                    Triple(
                        TsuzukiDownloadOption.UNREAD,
                        chaptersToSave(chapters, TsuzukiDownloadOption.UNREAD).size.toString(),
                        stringResource(MR.strings.tsz_download_unread),
                    ),
                    Triple(
                        TsuzukiDownloadOption.ALL,
                        chaptersToSave(chapters, TsuzukiDownloadOption.ALL).size.toString(),
                        stringResource(MR.strings.tsz_download_all),
                    ),
                    Triple(TsuzukiDownloadOption.CUSTOM, "…", stringResource(MR.strings.tsz_download_custom)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (value, number, label) ->
                                val active = option == value
                                val shape = RoundedCornerShape(20.dp)
                                val ink = if (active) tokens.onAccent else MaterialTheme.colorScheme.onSurface
                                val press =
                                    remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                Column(
                                    modifier = Modifier.tsuzukiPress(press).weight(1f).height(84.dp).clip(shape)
                                        .background(if (active) tokens.accent else tokens.tile)
                                        .then(
                                            if (!active && tokens.tileBorder != null) {
                                                Modifier.border(1.dp, tokens.tileBorder, shape)
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .semantics { this.selected = active }
                                        .clickable(
                                            interactionSource = press,
                                            indication = androidx.compose.material3.ripple(),
                                            role = Role.RadioButton,
                                        ) {
                                            if (value == TsuzukiDownloadOption.CUSTOM && option != value) {
                                                // Open Custom on the range already shown, not the whole book.
                                                rangeStart = sliderRange.start.roundToInt()
                                                rangeEnd = sliderRange.endInclusive.roundToInt()
                                            }
                                            option = value
                                        }
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        number,
                                        style = if (tokens.isDot) {
                                            MaterialTheme.typography.headlineSmall
                                        } else {
                                            MaterialTheme.typography.titleLarge
                                        },
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ink,
                                    )
                                    Text(
                                        case(label),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ink,
                                        maxLines = 2,
                                    )
                                }
                            }
                        }
                    }
                }
                TsuzukiTile(padding = 14.dp) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TsuzukiSectionLabel(
                            stringResource(MR.strings.tsz_download_range),
                            Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp),
                        )
                        if (chapters.isNotEmpty()) {
                            // Positions in reading order: sources' own numbers (e.g. "v8c6") don't parse reliably.
                            Text(
                                stringResource(
                                    MR.strings.tsz_download_range_value,
                                    sliderRange.start.roundToInt() + 1,
                                    sliderRange.endInclusive.roundToInt() + 1,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        }
                    }
                    val thumb: @Composable (RangeSliderState) -> Unit = {
                        Box(Modifier.size(24.dp).background(tokens.dotOn, CircleShape))
                    }
                    RangeSlider(
                        value = sliderRange,
                        onValueChange = {
                            rangeStart = it.start.roundToInt()
                            rangeEnd = it.endInclusive.roundToInt()
                            option = TsuzukiDownloadOption.CUSTOM
                        },
                        valueRange = 0f..lastIndex.coerceAtLeast(1).toFloat(),
                        steps = if (chapters.size in 3..100) chapters.size - 2 else 0,
                        enabled = chapters.size > 1,
                        startThumb = thumb,
                        endThumb = thumb,
                        track = { sliderState -> RangeTrack(sliderState) },
                    )
                    Text(
                        case(stringResource(MR.strings.tsz_download_count, selected.size)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TsuzukiTile(padding = 14.dp) {
                    DownloadPreferenceRow(stringResource(MR.strings.tsz_download_wifi), wifiOnly) {
                        preferences.downloadOnlyOverWifi.set(it)
                    }
                    DownloadPreferenceRow(stringResource(MR.strings.tsz_download_delete), deleteSlots >= 0) { enabled ->
                        if (enabled) {
                            preferences.removeAfterReadSlots.set(previousDeleteSlots)
                        } else {
                            previousDeleteSlots = deleteSlots.coerceAtLeast(0)
                            preferences.removeAfterReadSlots.set(-1)
                        }
                    }
                    Text(
                        case(stringResource(MR.strings.tsz_download_shared_preferences)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = {
                    // Recompute from the latest chapter states, rather than enqueueing a stale preview.
                    val batch = chaptersToSave(chapters, option, customRange)
                    if (batch.isNotEmpty()) {
                        onSave(batch)
                        close(onDismissRequest)
                    }
                },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.onAccent),
            ) {
                Icon(Icons.Outlined.Download, null, Modifier.padding(end = 8.dp))
                Text(
                    case(stringResource(MR.strings.tsz_download_save, selected.size)),
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun DownloadPreferenceRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Row(
        Modifier.fillMaxWidth().heightIn(min = 50.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (tokens.isDot) label.uppercase() else label,
            Modifier.weight(1f).padding(end = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = label },
            colors = SwitchDefaults.colors(checkedTrackColor = tokens.accent, checkedThumbColor = tokens.onAccent),
        )
    }
}

/** The board's track: 4 dp, accent between the thumbs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeTrack(state: RangeSliderState) {
    val tokens = TsuzukiTheme.tokens
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        val span = (state.valueRange.endInclusive - state.valueRange.start).takeIf { it > 0f } ?: 1f
        val from = (state.activeRangeStart - state.valueRange.start) / span * size.width
        val to = (state.activeRangeEnd - state.valueRange.start) / span * size.width
        val y = size.height / 2
        val stroke = size.height
        drawLine(tokens.track, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
        drawLine(tokens.accent, Offset(from, y), Offset(to, y), stroke, StrokeCap.Round)
    }
}
