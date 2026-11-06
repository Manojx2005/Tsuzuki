package eu.kanade.presentation.tsuzuki.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.theme.tsuzuki.TsuzukiModalBottomSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private data class FontChoice(val value: String, val label: StringResource, val preview: FontFamily)

private val literataFamily = FontFamily(Font(R.font.lnr_literata))

/** Margins from the board: Narrow 14 / Medium 26 / Wide 44, applied to both sides. */
private val marginChoices = listOf(
    14 to MR.strings.tsz_rs_narrow,
    26 to MR.strings.tsz_rs_medium,
    44 to MR.strings.tsz_rs_wide,
)

/**
 * The ReaderSettings board: Look / Layout / Controls over the live page. Every control writes the same
 * [ReaderPreferences] the stock novel sheet uses, so the page updates as you change things; [onAllSettings]
 * opens the full stock sheet for everything the board leaves out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TsuzukiReaderSettingsSheet(
    preferences: ReaderPreferences,
    onDismissRequest: () -> Unit,
    onAllSettings: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    TsuzukiModalBottomSheet(
        onDismissRequest = onDismissRequest,
        // Keep the page visible behind the sheet, so changes can be judged live.
        scrimColor = Color.Transparent,
    ) { close ->
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            TsuzukiSegments(
                labels = listOf(
                    stringResource(MR.strings.tsz_rs_look),
                    stringResource(MR.strings.tsz_rs_layout),
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
            TextButton(
                onClick = { close(onAllSettings) },
                modifier = Modifier.align(Alignment.End).padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Outlined.Tune, null, Modifier.padding(end = 8.dp).size(18.dp))
                Text(cased(stringResource(MR.strings.tsz_rs_all_settings)))
            }
        }
    }
}

@Composable
private fun LookTab(preferences: ReaderPreferences) {
    val font by preferences.novelFontFamily.collectAsState()
    val size by preferences.novelFontSize.collectAsState()
    val lines by preferences.novelLineHeight.collectAsState()

    TsuzukiPageColourPicker(preferences)

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
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stepper(
            label = stringResource(MR.strings.tsz_rs_size),
            value = size.toString(),
            canDecrease = size > 13,
            canIncrease = size < 28,
            onDecrease = { preferences.novelFontSize.set((size - 1).coerceIn(13, 28)) },
            onIncrease = { preferences.novelFontSize.set((size + 1).coerceIn(13, 28)) },
            modifier = Modifier.weight(1f),
        )
        // Line height in tenths so repeated steps don't drift (1.6 + 0.1 + 0.1 …).
        val tenths = (lines * 10).roundToInt()
        Stepper(
            label = stringResource(MR.strings.tsz_rs_lines),
            value = String.format(Locale.ROOT, "%.1f", tenths / 10f),
            canDecrease = tenths > 12,
            canIncrease = tenths < 22,
            onDecrease = { preferences.novelLineHeight.set((tenths - 1).coerceIn(12, 22) / 10f) },
            onIncrease = { preferences.novelLineHeight.set((tenths + 1).coerceIn(12, 22) / 10f) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LayoutTab(preferences: ReaderPreferences) {
    val renderingMode by preferences.novelRenderingMode.collectAsState()
    val paged by preferences.novelPagedMode.collectAsState()
    val marginLeft by preferences.novelMarginLeft.collectAsState()
    val marginRight by preferences.novelMarginRight.collectAsState()
    val align by preferences.novelTextAlign.collectAsState()
    val keepOn by preferences.novelKeepScreenOn.collectAsState()

    // Paged mode exists only in the WebView renderer, as in the stock sheet.
    if (renderingMode == "webview") {
        TsuzukiSectionLabel(stringResource(MR.strings.tsz_rs_reading_mode))
        TsuzukiSegments(
            labels = listOf(stringResource(MR.strings.tsz_rs_scroll), stringResource(MR.strings.tsz_rs_pages)),
            selected = if (paged) 1 else 0,
            onClick = { preferences.novelPagedMode.set(it == 1) },
        )
    }
    TsuzukiSectionLabel(stringResource(MR.strings.tsz_rs_margins))
    // Custom margins from "All settings" (e.g. the default 16) light up the nearest preset.
    val currentMargin = (marginLeft + marginRight) / 2
    val nearestMargin = marginChoices.minBy { (value, _) -> abs(value - currentMargin) }.first
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        marginChoices.forEach { (value, label) ->
            ChoiceTile(
                label = stringResource(label),
                selected = value == nearestMargin,
                modifier = Modifier.weight(1f),
            ) {
                preferences.novelMarginLeft.set(value)
                preferences.novelMarginRight.set(value)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    TsuzukiTile(modifier = Modifier.padding(horizontal = 16.dp), padding = 14.dp) {
        SwitchRow(stringResource(MR.strings.tsz_rs_justify), align == "justify") {
            preferences.novelTextAlign.set(if (it) "justify" else "left")
        }
        SwitchRow(stringResource(MR.strings.tsz_rs_keep_screen_on), keepOn) { preferences.novelKeepScreenOn.set(it) }
    }
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
        Modifier.tsuzukiPress(press).then(modifier).height(44.dp).clip(shape)
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
            label,
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
private fun Stepper(
    label: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(modifier = modifier, padding = 8.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 180.dp * LocalDensity.current.fontScale
            Column {
                if (compact) {
                    Text(
                        cased(label),
                        Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!compact) {
                        Text(
                            cased(label),
                            Modifier.padding(start = 6.dp).weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    StepButton(Icons.Outlined.Remove, "$label −", canDecrease, onDecrease)
                    Text(
                        value,
                        (if (compact) Modifier.weight(1f) else Modifier)
                            .padding(horizontal = if (compact) 0.dp else 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.accent,
                        textAlign = TextAlign.Center,
                    )
                    StepButton(Icons.Outlined.Add, "$label +", canIncrease, onIncrease)
                }
            }
        }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(48.dp)
            .semantics { contentDescription = description }
            .tsuzukiClickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) {
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
