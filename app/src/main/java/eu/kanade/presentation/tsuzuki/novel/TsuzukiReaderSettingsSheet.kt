package eu.kanade.presentation.tsuzuki.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.theme.tsuzuki.TsuzukiModalBottomSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

private data class FontChoice(val value: String, val label: StringResource, val preview: FontFamily)

private val literataFamily = FontFamily(Font(R.font.lnr_literata))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TsuzukiReaderSettingsSheet(
    preferences: ReaderPreferences,
    onDismissRequest: () -> Unit,
    onAllSettings: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var reset by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    TsuzukiModalBottomSheet(
        onDismissRequest = onDismissRequest,
        // Keep the page visible behind the sheet, so changes can be judged live.
        scrimColor = Color.Transparent,
    ) { close ->
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            TsuzukiSegments(
                labels = listOf(
                    stringResource(MR.strings.tsz_q11_text),
                    stringResource(MR.strings.tsz_q11_page),
                    stringResource(MR.strings.tsz_rs_controls),
                ),
                selected = tab,
                onClick = { tab = it },
            )
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
            ) {
                when (tab) {
                    0 -> LookTab(preferences)
                    1 -> LayoutTab(preferences)
                    else -> ControlsTab(preferences)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { reset = true }) {
                    Text(cased(stringResource(MR.strings.action_reset)))
                }
                TextButton(onClick = { close(onAllSettings) }, modifier = Modifier.weight(1f, fill = false)) {
                    Text(cased(stringResource(MR.strings.tsz_q11_all)))
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp))
                }
            }
        }
    }
    if (reset) ReaderSettingsResetDialog(preferences) { reset = false }
}

@Composable
private fun LookTab(preferences: ReaderPreferences) {
    val font by preferences.novelFontFamily.collectAsState()
    TsuzukiPageColourPicker(preferences, includeCustom = false, columns = 6)

    // Only faces the app has: Literata is bundled, the rest are system families.
    val fonts = listOf(
        FontChoice("literata", MR.strings.tsz_font_literata, literataFamily),
        FontChoice("serif", MR.strings.tsz_rs_font_serif, FontFamily.Serif),
        FontChoice("sans-serif", MR.strings.tsz_rs_font_sans, FontFamily.SansSerif),
        FontChoice("monospace", MR.strings.tsz_rs_font_mono, FontFamily.Monospace),
    )
    TsuzukiSectionLabel(stringResource(MR.strings.tsz_rs_font))
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        fonts.forEach { choice ->
            ChoiceTile(
                label = stringResource(choice.label),
                selected = font == choice.value,
                fontFamily = choice.preview,
                modifier = Modifier.weight(1f),
            ) { preferences.novelFontFamily.set(choice.value) }
        }
    }
    Spacer(Modifier.height(12.dp))
    ReaderTextMetrics(preferences, quick = true)
}

@Composable
private fun LayoutTab(preferences: ReaderPreferences) {
    val renderingMode by preferences.novelRenderingMode.collectAsState()
    val paged by preferences.novelPagedMode.collectAsState()

    if (renderingMode == "webview") {
        TsuzukiSectionLabel(stringResource(MR.strings.tsz_rs_reading_mode))
        TsuzukiSegments(
            labels = listOf(stringResource(MR.strings.tsz_rs_scroll), stringResource(MR.strings.tsz_rs_pages)),
            selected = if (paged) 1 else 0,
            onClick = { preferences.novelPagedMode.set(it == 1) },
        )
    }
    ReaderPageBasics(preferences)
}

@Composable
private fun ControlsTab(preferences: ReaderPreferences) {
    val tokens = TsuzukiTheme.tokens
    val renderingMode by preferences.novelRenderingMode.collectAsState()
    val paged by preferences.novelPagedMode.collectAsState()
    val volumeKeys by preferences.novelVolumeKeysScroll.collectAsState()
    val swipe by preferences.novelSwipeNavigation.collectAsState()

    // A preview of the tap layout (1 : 1.3 : 1), as on the board.
    TsuzukiSectionLabel(stringResource(MR.strings.tsz_rs_tap_zones))
    Row(Modifier.padding(horizontal = 16.dp).height(150.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(MR.strings.tsz_rs_back to 1f, MR.strings.tsz_rs_menu to 1.3f, MR.strings.tsz_rs_next to 1f)
            .forEachIndexed { index, (label, weight) ->
                val menu = index == 1
                Box(
                    Modifier.weight(weight).fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (menu) tokens.accent else tokens.tile),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        cased(stringResource(label)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (menu) tokens.onAccent else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
    }
    Spacer(Modifier.height(12.dp))
    TsuzukiTile(modifier = Modifier.padding(horizontal = 16.dp), padding = 14.dp) {
        SwitchRow(stringResource(MR.strings.tsz_rs_volume_keys), volumeKeys) {
            preferences.novelVolumeKeysScroll.set(it)
        }
        // In WebView paged mode the swipe already turns pages, as in the stock sheet.
        if (!(renderingMode == "webview" && paged)) {
            SwitchRow(stringResource(MR.strings.tsz_rs_swipe_chapter), swipe) {
                preferences.novelSwipeNavigation.set(it)
            }
        }
    }
}

@Composable
private fun ChoiceTile(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
    onClick: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(14.dp)
    val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier.tsuzukiPress(press).then(modifier).heightIn(min = 48.dp).clip(shape)
            .background(if (selected) tokens.dotOn else tokens.tile)
            .then(
                if (!selected &&
                    tokens.tileBorder != null
                ) {
                    Modifier.border(1.dp, tokens.tileBorder, shape)
                } else {
                    Modifier
                },
            )
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = press,
                indication = androidx.compose.material3.ripple(),
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            cased(label),
            // The font tiles preview their own face; the rest follow the theme.
            style = if (fontFamily != null) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily)
            } else {
                MaterialTheme.typography.labelLarge
            },
            fontWeight = FontWeight.SemiBold,
            color = if (selected) tokens.background else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            cased(label),
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

@Composable
private fun cased(text: String): String = if (TsuzukiTheme.tokens.isDot) text.uppercase() else text
