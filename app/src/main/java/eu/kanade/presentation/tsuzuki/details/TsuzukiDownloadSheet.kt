package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.more.settings.screen.SettingsDownloadScreen
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiModalBottomSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.delay
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Optional toolbar slot, keeping the legacy download menu and the upstream screen layout intact. */
@Composable
fun tsuzukiDownloadSheet(
    state: MangaViewModel.State.Success,
    onDownload: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
): (@Composable (onDismissRequest: () -> Unit) -> Unit)? {
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE || onDownload == null) return null
    val navigator = LocalNavigator.currentOrThrow
    return { dismiss ->
        val readerPreferences = remember { Injekt.get<ReaderPreferences>() }
        val skipFiltered by readerPreferences.skipFiltered.collectAsState()
        val items = if (skipFiltered) state.processedChapters else state.chapters
        val ordered = remember(items, state.manga.sorting) {
            val sort = getChapterSort(state.manga, sortDescending = false)
            items.sortedWith { a, b -> sort(a.chapter, b.chapter) }
        }
        TsuzukiDownloadSheet(
            chapters = ordered,
            total = state.chapters.size,
            unread = state.chapters.count { !it.chapter.read },
            onDismissRequest = dismiss,
            onOpenSettings = { navigator.push(SettingsDownloadScreen) },
            onSave = { onDownload(it, ChapterDownloadAction.START) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TsuzukiDownloadSheet(
    chapters: List<ChapterList.Item>,
    total: Int,
    unread: Int,
    onDismissRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onSave: (List<ChapterList.Item>) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val numbers = remember(chapters) { chapters.mapIndexed { index, item -> downloadChapterNumber(item, index) } }
    val minimum = numbers.minOrNull()
    val maximum = numbers.maxOrNull()
    var option by rememberSaveable { mutableStateOf(TsuzukiDownloadOption.NEXT_25) }
    var fromText by rememberSaveable { mutableStateOf(minimum?.asChapterText().orEmpty()) }
    var toText by rememberSaveable { mutableStateOf(maximum?.asChapterText().orEmpty()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var openedCustom by rememberSaveable { mutableStateOf(false) }
    val custom = option == TsuzukiDownloadOption.CUSTOM
    val from = parseDownloadChapterNumber(fromText)
    val to = parseDownloadChapterNumber(toText)
    val allowed = if (minimum != null && maximum != null) minimum..maximum else null
    val range = if (from != null && to != null && allowed != null && from in allowed && to in allowed && from <= to) {
        from..to
    } else {
        null
    }
    val selected = remember(chapters, option, range) { chaptersToSave(chapters, option, range) }
    val saved = remember(chapters, range, custom) {
        if (custom && range != null) {
            chaptersInNumberRange(chapters, range.start, range.endInclusive).count { it.isDownloaded }
        } else {
            0
        }
    }
    val validation = when {
        !custom -> null
        chapters.isEmpty() -> stringResource(MR.strings.tsz_download_none_available)
        from == null || to == null -> stringResource(MR.strings.tsz_download_number_required)
        from > to -> stringResource(MR.strings.tsz_download_reversed)
        range == null -> stringResource(
            MR.strings.tsz_download_out_of_range,
            minimum?.asChapterText().orEmpty(),
            maximum?.asChapterText().orEmpty(),
        )
        selected.isEmpty() -> stringResource(MR.strings.tsz_download_none_available)
        else -> null
    }
    val resumeNumber = remember(chapters, numbers) {
        val lastRead = chapters.indexOfLast { it.chapter.read }
        chapters.indices.firstOrNull { it > lastRead && !chapters[it].chapter.read }?.let(numbers::get)
    }

    TsuzukiModalBottomSheet(onDismissRequest = onDismissRequest) { close ->
        Column(Modifier.fillMaxWidth().heightIn(max = 740.dp)) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.tsz_download_title)),
                        Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.tsz_download_totals, unread, total)),
                        Modifier.weight(0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.dotOn.copy(alpha = 0.72f),
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
                                TsuzukiMenuItem(label, onClick = {
                                    option = value
                                    menuExpanded = false
                                })
                            }
                        }
                    }
                }
                val options = listOf(
                    TsuzukiDownloadOption.NEXT_1 to stringResource(MR.strings.tsz_download_next, 1),
                    TsuzukiDownloadOption.NEXT_5 to stringResource(MR.strings.tsz_download_next, 5),
                    TsuzukiDownloadOption.NEXT_25 to stringResource(MR.strings.tsz_download_next, 25),
                    TsuzukiDownloadOption.UNREAD to stringResource(MR.strings.tsz_download_unread),
                    TsuzukiDownloadOption.ALL to stringResource(MR.strings.tsz_download_all),
                    TsuzukiDownloadOption.CUSTOM to stringResource(MR.strings.tsz_download_custom),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { (value, label) ->
                        TsuzukiPill(
                            text = label,
                            selected = option == value,
                            modifier = Modifier.semantics { this.selected = option == value },
                            role = Role.RadioButton,
                            onClick = {
                                if (value == TsuzukiDownloadOption.CUSTOM && !openedCustom) {
                                    openedCustom = true
                                    val ids = selected.mapTo(hashSetOf()) { it.id }
                                    val presetNumbers = chapters.indices.filter {
                                        chapters[it].id in ids
                                    }.map(numbers::get)
                                    presetNumbers.minOrNull()?.let { fromText = it.asChapterText() }
                                    presetNumbers.maxOrNull()?.let { toText = it.asChapterText() }
                                }
                                option = value
                            },
                        )
                    }
                }

                if (custom) {
                    TsuzukiTile(padding = 16.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ChapterNumberInput(
                                stringResource(MR.strings.tsz_download_from),
                                fromText,
                                { fromText = it },
                                allowed,
                            )
                            ChapterNumberInput(
                                stringResource(MR.strings.tsz_download_to),
                                toText,
                                { toText = it },
                                allowed,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (resumeNumber != null) {
                                    TsuzukiPill(stringResource(MR.strings.tsz_download_from_here), false, {
                                        fromText = resumeNumber.asChapterText()
                                    }, role = Role.Button)
                                }
                                if (maximum != null) {
                                    TsuzukiPill(stringResource(MR.strings.tsz_download_to_last), false, {
                                        toText = maximum.asChapterText()
                                    }, role = Role.Button)
                                }
                            }
                            HorizontalDivider(color = tokens.track)
                            FlowRow(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    tsuzukiMenuLabel(stringResource(MR.strings.tsz_download_count, selected.size)),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                if (saved > 0) {
                                    Text(
                                        tsuzukiMenuLabel(stringResource(MR.strings.tsz_download_skipped_saved, saved)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = tokens.dotOn.copy(alpha = 0.72f),
                                    )
                                }
                            }
                            validation?.let {
                                Text(
                                    tsuzukiMenuLabel(it),
                                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                    color = tokens.accent,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                } else if (option == TsuzukiDownloadOption.ALL) {
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.tsz_download_all_eligible)),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.dotOn.copy(alpha = 0.72f),
                    )
                }
                if (chapters.size < total) {
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.tsz_download_filtered)),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.dotOn.copy(alpha = 0.72f),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) {
                        close {
                            onDismissRequest()
                            onOpenSettings()
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.tsz_download_settings)),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.dotOn.copy(alpha = 0.72f),
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = tokens.dotOn.copy(alpha = 0.72f))
                }
            }

            Button(
                onClick = {
                    val batch = chaptersToSave(chapters, option, range)
                    if (batch.isNotEmpty()) {
                        close {
                            onSave(batch)
                            onDismissRequest()
                        }
                    }
                },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.onAccent),
            ) {
                Icon(Icons.Outlined.Download, null, Modifier.padding(end = 8.dp))
                Text(
                    tsuzukiMenuLabel(
                        if (custom && from != null && to != null) {
                            stringResource(MR.strings.tsz_download_save_range, from.asChapterText(), to.asChapterText())
                        } else {
                            stringResource(MR.strings.tsz_download_save, selected.size)
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ChapterNumberInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    bounds: ClosedFloatingPointRange<Double>?,
) {
    val tokens = TsuzukiTheme.tokens
    val focus = LocalFocusManager.current
    val parsed = parseDownloadChapterNumber(value)
    fun step(delta: Int) {
        if (bounds == null) return
        val next = if (parsed == null) {
            if (delta > 0) bounds.start else bounds.endInclusive
        } else {
            (parsed.toBigDecimal() + delta.toBigDecimal()).toDouble().coerceIn(bounds.start, bounds.endInclusive)
        }
        onValueChange(next.asChapterText())
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            tsuzukiMenuLabel(label),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.dotOn.copy(alpha = 0.72f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ChapterNumberStepper(
                stringResource(MR.strings.tsz_download_decrease, label),
                increase = false,
                enabled = bounds != null && (parsed == null || parsed > bounds.start),
            ) { step(-1) }
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 32) onValueChange(it) },
                modifier = Modifier.weight(1f).heightIn(min = 56.dp).semantics { contentDescription = label },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = tokens.background,
                    unfocusedContainerColor = tokens.background,
                    focusedBorderColor = tokens.accent,
                    unfocusedBorderColor = tokens.tileBorder ?: tokens.track,
                ),
            )
            ChapterNumberStepper(
                stringResource(MR.strings.tsz_download_increase, label),
                increase = true,
                enabled = bounds != null && (parsed == null || parsed < bounds.endInclusive),
            ) { step(1) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterNumberStepper(label: String, increase: Boolean, enabled: Boolean, onStep: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val currentStep by rememberUpdatedState(onStep)
    var repeating by remember { mutableStateOf(false) }
    LaunchedEffect(pressed, repeating, enabled) {
        if (!pressed || !enabled) {
            repeating = false
        } else if (repeating) {
            while (true) {
                delay(95)
                currentStep()
            }
        }
    }
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(tokens.tile)
            .border(1.dp, tokens.tileBorder ?: tokens.track, CircleShape)
            .combinedClickable(
                interactionSource = interactions,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onStep,
                onLongClick = {
                    onStep()
                    repeating = pressed
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (increase) Icons.Outlined.Add else Icons.Outlined.Remove,
            label,
            tint = tokens.dotOn.copy(alpha = if (enabled) 1f else 0.38f),
        )
    }
}

private fun Double.asChapterText(): String = toBigDecimal().stripTrailingZeros().toPlainString()
