package eu.kanade.presentation.tsuzuki.manga

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.BaseSliderItem
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

internal val LocalMangaReaderSettings = staticCompositionLocalOf { false }

@Composable
internal fun TsuzukiReadingModeGrid(selected: ReadingMode, onSelect: (ReadingMode) -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val modes = listOf(
        ReadingMode.RIGHT_TO_LEFT,
        ReadingMode.LEFT_TO_RIGHT,
        ReadingMode.VERTICAL,
        ReadingMode.WEBTOON,
        ReadingMode.CONTINUOUS_VERTICAL,
        ReadingMode.DEFAULT,
    )
    TsuzukiSectionLabel(
        text = stringResource(MR.strings.pref_category_reading_mode) + " · " +
            stringResource(MR.strings.pref_category_for_this_series),
    )
    Column(
        modifier = Modifier.padding(horizontal = 12.dp).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        modes.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { mode ->
                    val active = mode == selected
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(tokens.tileRadius),
                        color = if (active) tokens.accent else tokens.tile,
                        contentColor = if (active) tokens.onAccent else MaterialTheme.colorScheme.onSurface,
                        border = if (active) null else tokens.tileBorder?.let { BorderStroke(1.dp, it) },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 86.dp)
                                .selectable(active, role = Role.RadioButton, onClick = { onSelect(mode) })
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(painterResource(mode.iconRes), contentDescription = null)
                            Text(
                                text = tsuzukiCase(stringResource(mode.stringRes)).orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MangaReaderSettingHeading(label: StringResource) {
    if (LocalMangaReaderSettings.current) {
        TsuzukiSectionLabel(stringResource(label))
        return
    }
    HeadingItem(label)
}

@Composable
internal fun MangaReaderSettingsChoices(label: StringResource, content: @Composable FlowRowScope.() -> Unit) {
    if (LocalMangaReaderSettings.current) {
        MangaReaderSettingHeading(label)
        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
        return
    }
    SettingsChipRow(label, content)
}

@Composable
internal fun MangaReaderSettingChoice(selected: Boolean, onClick: () -> Unit, label: String) {
    if (LocalMangaReaderSettings.current) {
        TsuzukiPill(text = label, selected = selected, onClick = onClick, role = Role.Button)
        return
    }
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
internal fun MangaReaderSettingToggle(label: String, pref: Preference<Boolean>) {
    if (!LocalMangaReaderSettings.current) {
        CheckboxItem(label, pref)
        return
    }
    val tokens = TsuzukiTheme.tokens
    val checked by pref.collectAsState()
    Row(
        modifier = tsuzukiGroupedTile(first = true, last = true)
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = pref::set)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = tsuzukiCase(label).orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = tokens.accent,
                checkedThumbColor = tokens.onAccent,
            ),
        )
    }
}

@Composable
internal fun MangaReaderSettingSlider(
    value: Int,
    valueRange: IntProgression,
    label: String,
    onChange: (Int) -> Unit,
    steps: Int = valueRange.last - valueRange.first - 1,
    valueString: String = value.toString(),
    pillColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    if (!LocalMangaReaderSettings.current) {
        SliderItem(value, valueRange, label, onChange, steps, valueString, pillColor = pillColor)
        return
    }
    BaseSliderItem(
        value = value,
        valueRange = valueRange,
        title = tsuzukiCase(label).orEmpty(),
        onChange = onChange,
        modifier = tsuzukiGroupedTile(first = true, last = true).padding(horizontal = 12.dp, vertical = 6.dp),
        steps = steps,
        valueString = valueString,
        titleStyle = MaterialTheme.typography.titleSmall,
        pillColor = TsuzukiTheme.tokens.tile,
    )
}
