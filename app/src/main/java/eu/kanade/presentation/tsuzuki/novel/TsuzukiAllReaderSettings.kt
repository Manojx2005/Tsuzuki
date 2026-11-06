package eu.kanade.presentation.tsuzuki.novel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.reader.settings.NovelAdvancedTab
import eu.kanade.presentation.reader.settings.NovelAppearanceOptions
import eu.kanade.presentation.reader.settings.NovelAutoSplitSettings
import eu.kanade.presentation.reader.settings.NovelControlsTab
import eu.kanade.presentation.reader.settings.NovelFontSetting
import eu.kanade.presentation.reader.settings.NovelIndividualMargins
import eu.kanade.presentation.reader.settings.NovelStatusBarSettings
import eu.kanade.presentation.reader.settings.NovelTtsTab
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.font.FontManager
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsViewModel
import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.ThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.LocalStackedSettingsLabel
import tachiyomi.presentation.core.components.StepperItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import kotlin.math.roundToInt

@Composable
internal fun TsuzukiAllReaderSettings(viewModel: ReaderSettingsViewModel, onBack: () -> Unit) {
    val preferences = viewModel.preferences
    val renderer by preferences.novelRenderingMode.collectAsState()
    var tab by rememberSaveable { mutableStateOf(NovelSettingsTab.TEXT) }
    var reset by rememberSaveable { mutableStateOf(false) }
    val activity = LocalContext.current.readerActivity()
    val tokens = TsuzukiTheme.tokens

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = tokens.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(MR.strings.tsz_rs_back))
                    }
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.tsz_q11_all)),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = { reset = true }) {
                        Text(tsuzukiMenuLabel(stringResource(MR.strings.action_reset)))
                    }
                }
                ReaderTabs(
                    labels = NovelSettingsTab.entries.map { stringResource(it.label) },
                    selected = tab.ordinal,
                    onSelect = { tab = NovelSettingsTab.entries[it] },
                )
                val labelTransform: (String) -> String = { if (tokens.isDot) it.uppercase() else it }
                CompositionLocalProvider(LocalStackedSettingsLabel provides labelTransform) {
                    key(tab) {
                        Column(
                            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            tab.groups.forEach { group ->
                                when (group) {
                                    ReaderPreferenceGroup.TYPOGRAPHY -> {
                                        ReaderTypography(viewModel, renderer)
                                        ReaderPreview(preferences, activity)
                                    }
                                    ReaderPreferenceGroup.PARAGRAPHS -> FineTune {
                                        ReaderParagraphSettings(preferences)
                                        NovelAutoSplitSettings(viewModel)
                                    }
                                    ReaderPreferenceGroup.COLOURS -> TsuzukiPageColourPicker(preferences)
                                    ReaderPreferenceGroup.PAGE_BASICS -> ReaderPageBasics(preferences)
                                    ReaderPreferenceGroup.PAGE_OPTIONS -> FineTune {
                                        NovelIndividualMargins(viewModel)
                                        NovelAppearanceOptions(viewModel, renderer, includeKeepScreenOn = false)
                                        NovelStatusBarSettings(viewModel, includeBasics = false)
                                    }
                                    ReaderPreferenceGroup.NAVIGATION ->
                                        NovelControlsTab(viewModel, renderer, includeStatusBar = false)
                                    ReaderPreferenceGroup.TTS -> NovelTtsTab(viewModel, highlightLayout = {
                                        FineTune(it)
                                    })
                                    ReaderPreferenceGroup.ADVANCED -> NovelAdvancedTab(viewModel, renderer)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (reset) ReaderSettingsResetDialog(preferences) { reset = false }
    }
}

@Composable
private fun ReaderTypography(viewModel: ReaderSettingsViewModel, renderer: String) {
    val preferences = viewModel.preferences
    val align by preferences.novelTextAlign.collectAsState()
    val alignments = listOf("left", "center", "right", "justify")

    TsuzukiSectionLabel(stringResource(TDMR.strings.pref_novel_rendering_mode))
    ReaderChoices(
        labels = listOf(
            stringResource(TDMR.strings.novel_render_default),
            stringResource(TDMR.strings.novel_render_webview),
        ),
        selected = if (renderer == "webview") 1 else 0,
        onSelect = { preferences.novelRenderingMode.set(if (it == 1) "webview" else "default") },
    )
    NovelFontSetting(viewModel)
    if (renderer == "webview") {
        CheckboxItem(
            label = stringResource(TDMR.strings.pref_novel_use_original_fonts),
            pref = preferences.novelUseOriginalFonts,
        )
    }
    ReaderTextMetrics(preferences)
    TsuzukiSectionLabel(stringResource(TDMR.strings.pref_novel_text_align))
    ReaderChoices(
        labels = listOf(
            stringResource(MR.strings.tsz_q11_left),
            stringResource(MR.strings.tsz_q11_centre),
            stringResource(MR.strings.tsz_q11_right),
            stringResource(MR.strings.tsz_q11_justify),
        ),
        selected = alignments.indexOf(align),
        onSelect = { preferences.novelTextAlign.set(alignments[it]) },
    )
}

@Composable
internal fun ReaderTextMetrics(preferences: ReaderPreferences, quick: Boolean = false) {
    val size by preferences.novelFontSize.collectAsState()
    val spacing by preferences.novelLineHeight.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    val tenths = (spacing * 10).roundToInt()
    val sizeControl: @Composable () -> Unit = {
        StepperItem(
            label = stringResource(MR.strings.tsz_q11_size),
            value = size,
            valueRange = 10..40,
            onChange = { preferences.novelFontSize.set(it) },
            defaultValue = preferences.novelFontSize.defaultValue(),
            displayValue = formatReaderSize(size),
        )
    }
    val spacingControl: @Composable () -> Unit = {
        StepperItem(
            label = stringResource(MR.strings.tsz_q11_spacing),
            value = tenths,
            valueRange = 1..50,
            onChange = { preferences.novelLineHeight.set(it / 10f) },
            defaultValue = (preferences.novelLineHeight.defaultValue() * 10).roundToInt(),
            displayValue = formatReaderSpacing(tenths, locale),
            decimalInput = true,
        )
    }
    val isDot = TsuzukiTheme.tokens.isDot
    CompositionLocalProvider(
        LocalStackedSettingsLabel provides { label ->
            if (isDot) label.uppercase() else label
        },
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (quick && maxWidth >= 350.dp * LocalDensity.current.fontScale) {
                Row {
                    Column(Modifier.weight(1f)) { sizeControl() }
                    Column(Modifier.weight(1f)) { spacingControl() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizeControl()
                    spacingControl()
                }
            }
        }
    }
}

@Composable
internal fun ReaderPageBasics(preferences: ReaderPreferences) {
    val keepOn by preferences.novelKeepScreenOn.collectAsState()
    val status by preferences.novelStatusBarEnabled.collectAsState()
    val position by preferences.novelStatusBarPosition.collectAsState()
    val isDot = TsuzukiTheme.tokens.isDot
    CompositionLocalProvider(
        LocalStackedSettingsLabel provides { label ->
            if (isDot) label.uppercase() else label
        },
    ) {
        TsuzukiSectionLabel(stringResource(MR.strings.tsz_rs_margins))
        LinkedMarginControl(
            stringResource(MR.strings.tsz_q11_side_margins),
            preferences.novelMarginLeft,
            preferences.novelMarginRight,
            0..100,
        )
        LinkedMarginControl(
            stringResource(MR.strings.tsz_q11_vertical_margins),
            preferences.novelMarginTop,
            preferences.novelMarginBottom,
            0..300,
        )
    }
    TsuzukiTile(modifier = Modifier.padding(horizontal = 16.dp), padding = 12.dp) {
        SwitchRow(stringResource(MR.strings.tsz_rs_keep_screen_on), keepOn, preferences.novelKeepScreenOn::set)
        SwitchRow(stringResource(TDMR.strings.pref_novel_status_bar), status, preferences.novelStatusBarEnabled::set)
    }
    if (status) {
        TsuzukiSectionLabel(stringResource(MR.strings.tsz_q11_status_position))
        ReaderChoices(
            labels = listOf(stringResource(MR.strings.tsz_q11_top), stringResource(MR.strings.tsz_q11_bottom)),
            selected = if (position == "top") 0 else 1,
            onSelect = { preferences.novelStatusBarPosition.set(if (it == 0) "top" else "bottom") },
        )
    }
}

@Composable
private fun LinkedMarginControl(label: String, first: Preference<Int>, second: Preference<Int>, range: IntRange) {
    val firstValue by first.collectAsState()
    val secondValue by second.collectAsState()
    StepperItem(
        label = label,
        value = linkedReaderMargin(firstValue, secondValue),
        valueRange = range,
        onChange = {
            first.set(it)
            second.set(it)
        },
        displayValue = formatReaderMargins(firstValue, secondValue),
    )
}

/** The five settings tabs in one row of equal segments: they must never wrap onto a second line (card Q14). */
@Composable
private fun ReaderTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            Text(
                tsuzukiMenuLabel(label),
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                    .background(if (active) tokens.accent else tokens.tile)
                    .selectable(active, role = Role.Tab, onClick = { onSelect(index) })
                    .heightIn(min = 48.dp).padding(horizontal = 2.dp, vertical = 15.dp),
                color = if (active) tokens.onAccent else MaterialTheme.colorScheme.onSurface,
                // labelSmall: "CONTROLS" must fit a fifth of a 360 dp screen without an ellipsis.
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Paragraph indent and spacing with their unit (stored in em; Tsundoku's stepper showed tenths with no unit). */
@Composable
private fun ReaderParagraphSettings(preferences: ReaderPreferences) {
    EmStepper(stringResource(TDMR.strings.pref_novel_paragraph_indent), preferences.novelParagraphIndent, 0..100)
    EmStepper(stringResource(TDMR.strings.pref_novel_paragraph_spacing), preferences.novelParagraphSpacing, 0..30)
}

@Composable
private fun EmStepper(label: String, pref: Preference<Float>, range: IntRange) {
    val value by pref.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    val tenths = (value * 10).roundToInt()
    StepperItem(
        label = label,
        value = tenths,
        onChange = { pref.set(it / 10f) },
        valueRange = range,
        defaultValue = (pref.defaultValue() * 10).roundToInt(),
        displayValue = formatReaderEm(tenths, locale),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderChoices(
    labels: List<String>,
    selected: Int,
    role: Role = Role.RadioButton,
    onSelect: (Int) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            Text(
                tsuzukiMenuLabel(label),
                Modifier.clip(RoundedCornerShape(18.dp))
                    .background(if (active) tokens.accent else tokens.tile)
                    .selectable(active, role = role, onClick = { onSelect(index) })
                    .heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 14.dp),
                color = if (active) tokens.onAccent else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun FineTune(content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
    ) {
        Text(tsuzukiMenuLabel(stringResource(MR.strings.tsz_q11_fine_tune)), Modifier.weight(1f))
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
    }
    if (expanded) content()
}

@Composable
private fun ReaderPreview(preferences: ReaderPreferences, activity: Activity?) {
    val theme by preferences.novelTheme.collectAsState()
    val customPage by preferences.novelBackgroundColor.collectAsState()
    val customInk by preferences.novelFontColor.collectAsState()
    val face by preferences.novelFontFamily.collectAsState()
    val size by preferences.novelFontSize.collectAsState()
    val spacing by preferences.novelLineHeight.collectAsState()
    val alignment by preferences.novelTextAlign.collectAsState()
    val context = LocalContext.current
    val family by produceState<FontFamily?>(null, face, context) {
        value = when (face) {
            "literata" -> FontFamily(Font(R.font.lnr_literata))
            "serif", "Georgia, serif", "Times New Roman, serif" -> FontFamily.Serif
            "sans-serif", "Arial, sans-serif" -> FontFamily.SansSerif
            "monospace" -> FontFamily.Monospace
            else -> withContext(Dispatchers.IO) {
                val fonts = FontManager(context)
                fonts.getInstalledFonts().firstOrNull { it.path == face }?.let(fonts::getTypeface)
                    ?.let { FontFamily(it) }
            }
        }
    }
    val colours = when {
        theme == NovelPageColour.CUSTOM.key ->
            Color(customPage.takeUnless { it == 0 } ?: 0xFFFFFFFF.toInt()) to
                Color(customInk.takeUnless { it == 0 } ?: 0xFF000000.toInt())
        activity != null -> ThemeUtils.getThemeColors(activity, preferences, theme).let {
            Color(it.first) to Color(it.second)
        }
        else -> TsuzukiTheme.tokens.background to MaterialTheme.colorScheme.onSurface
    }
    TsuzukiSectionLabel(stringResource(MR.strings.tsz_q11_preview))
    Text(
        stringResource(MR.strings.tsz_q11_sample),
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp))
            .background(colours.first).padding(16.dp),
        color = colours.second,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = family,
            fontSize = size.sp,
            lineHeight = (size * spacing).sp,
            textAlign = when (alignment) {
                "center" -> TextAlign.Center
                "right" -> TextAlign.Right
                "justify" -> TextAlign.Justify
                else -> TextAlign.Left
            },
        ),
    )
}

private tailrec fun Context.readerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.readerActivity()
    else -> null
}

@Composable
internal fun ReaderSettingsResetDialog(preferences: ReaderPreferences, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_reset))) },
        text = { Text(stringResource(MR.strings.tsz_q11_reset_body)) },
        confirmButton = {
            TextButton(onClick = {
                preferences.resetNovelSettings()
                onDismiss()
            }) { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_reset))) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_cancel))) }
        },
    )
}
