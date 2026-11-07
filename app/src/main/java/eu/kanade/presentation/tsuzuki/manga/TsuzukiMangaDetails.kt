package eu.kanade.presentation.tsuzuki.manga

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.presentation.theme.tsuzuki.TsuzukiAdaptiveSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile
import eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun TsuzukiMangaReadingMode(manga: Manga, onChange: ((ReadingMode) -> Unit)? = null) {
    var showPicker by remember(manga.id) { mutableStateOf(false) }
    val configuredMode = ReadingMode.fromPreference(manga.readingMode.toInt())
    val mode = if (configuredMode == ReadingMode.DEFAULT) {
        val preferences = remember { Injekt.get<ReaderPreferences>() }
        val defaultMode by preferences.defaultReadingMode.collectAsState()
        ReadingMode.fromPreference(defaultMode)
    } else {
        configuredMode
    }
    if (mode == ReadingMode.NOVEL) return
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .then(if (onChange != null) Modifier.clickable(role = Role.Button) { showPicker = true } else Modifier),
        padding = 14.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(painterResource(mode.iconRes), contentDescription = null, tint = tokens.accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tsuzukiCase(
                        stringResource(
                            if (mode == ReadingMode.WEBTOON) {
                                MR.strings.tsz_manga_opens_webtoon
                            } else {
                                MR.strings.pref_category_reading_mode
                            },
                        ),
                    ).orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (onChange != null) {
                    Text(
                        text = tsuzukiCase(stringResource(MR.strings.tsz_manga_change_mode)).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = tsuzukiCase(stringResource(mode.stringRes)).orEmpty(),
                modifier = Modifier.widthIn(max = 120.dp),
                color = tokens.accent,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
    if (showPicker && onChange != null) {
        TsuzukiAdaptiveSheet(onDismissRequest = { showPicker = false }) { close ->
            Column(Modifier.padding(vertical = 16.dp)) {
                TsuzukiReadingModeGrid(configuredMode) { selected ->
                    onChange(selected)
                    close { showPicker = false }
                }
            }
        }
    }
}

@Composable
internal fun TsuzukiMangaScanlatorFilter(state: MangaViewModel.State.Success, onClick: (() -> Unit)?) {
    if (onClick == null || !isTsuzukiStyle() || state.isNovel || state.isAnySelected ||
        state.availableScanlators.isEmpty()
    ) {
        return
    }
    val excludedCount = state.availableScanlators.count { it in state.excludedScanlators }
    TsuzukiPill(
        text = stringResource(MR.strings.exclude_scanlators),
        selected = excludedCount > 0,
        onClick = onClick,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        count = excludedCount.takeIf { it > 0 },
        role = Role.Button,
    )
}

@Composable
internal fun TsuzukiScanlatorFilterDialog(
    availableScanlators: List<String>,
    excludedScanlators: MutableList<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiAdaptiveSheet(onDismissRequest = onDismissRequest) { close ->
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text(
                text = tsuzukiCase(stringResource(MR.strings.exclude_scanlators)).orEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (availableScanlators.isEmpty()) {
                Text(
                    text = tsuzukiCase(stringResource(MR.strings.no_scanlators_found)).orEmpty(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    itemsIndexed(availableScanlators, key = { _, name ->
                        name
                    }, contentType = { _, _ -> "scanlator" }) { index, name ->
                        val excluded = name in excludedScanlators
                        Row(
                            modifier = tsuzukiGroupedTile(
                                first = index == 0,
                                last =
                                index == availableScanlators.lastIndex,
                            )
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .toggleable(excluded, role = Role.Checkbox) { checked ->
                                    if (checked) excludedScanlators.add(name) else excludedScanlators.remove(name)
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Checkbox(
                                checked = excluded,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = tokens.accent,
                                    checkmarkColor = tokens.onAccent,
                                ),
                            )
                            Text(
                                text = tsuzukiCase(name).orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (availableScanlators.isNotEmpty()) {
                    val selectAll = excludedScanlators.isEmpty()
                    TextButton(
                        onClick = {
                            if (selectAll) {
                                excludedScanlators.addAll(
                                    availableScanlators,
                                )
                            } else {
                                excludedScanlators.clear()
                            }
                        },
                    ) {
                        val label = if (selectAll) MR.strings.action_select_all else MR.strings.action_reset
                        Text(tsuzukiCase(stringResource(label)).orEmpty())
                    }
                }
                TextButton(onClick = { close(onDismissRequest) }) {
                    Text(tsuzukiCase(stringResource(MR.strings.action_cancel)).orEmpty())
                }
                if (availableScanlators.isNotEmpty()) {
                    TextButton(onClick = {
                        val selected = excludedScanlators.toSet()
                        close {
                            onConfirm(selected)
                            onDismissRequest()
                        }
                    }) {
                        Text(tsuzukiCase(stringResource(MR.strings.action_ok)).orEmpty())
                    }
                }
            }
        }
    }
}
