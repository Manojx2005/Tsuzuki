package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground

/**
 * The selected section in the tablet settings list. TabletSettings board: an accent outline around the row
 * in the Tsuzuki themes; the stock filled row otherwise.
 */
@Composable
fun settingsPaneSelection(selected: Boolean): Modifier {
    if (!selected) return Modifier
    val tokens = TsuzukiTheme.tokens
    return if (tokens.style == TsuzukiStyle.NONE) {
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    } else {
        Modifier.border(2.dp, tokens.accent, RoundedCornerShape(24.dp))
    }
}

/** True when a list preference's subtitle is just its selected entry, in a Tsuzuki theme. */
internal fun showsValueLabel(style: TsuzukiStyle, subtitle: String?, selectedEntry: String?): Boolean =
    style != TsuzukiStyle.NONE && !subtitle.isNullOrBlank() && subtitle == selectedEntry

/**
 * The boards' value rows: when a list preference's subtitle is just its selected entry, the Tsuzuki themes show
 * that entry on the right in the accent (uppercase in Dot). Returns null to keep the stock subtitle.
 */
@Composable
fun tsuzukiValueLabel(subtitle: String?, selectedEntry: String?): (@Composable () -> Unit)? {
    val tokens = TsuzukiTheme.tokens
    if (!showsValueLabel(tokens.style, subtitle, selectedEntry)) return null
    if (subtitle == null) return null
    return {
        androidx.compose.material3.Text(
            if (tokens.isDot) subtitle.uppercase() else subtitle,
            modifier = Modifier.padding(start = 12.dp, end = 16.dp).widthIn(max = 180.dp),
            color = tokens.accent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/**
 * Settings section rows (Settings and TabletSettings boards): icons in the content colour instead of the accent, so
 * red stays for the selection in Dot. Null keeps the stock tint.
 */
@Composable
fun tsuzukiSectionIconTint(): Color? {
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) return null
    return LocalContentColor.current.copy(alpha = 0.78f)
}

/** Dot sets Latin labels in uppercase (boards); other themes and non-Latin text are unchanged. */
@Composable
fun tsuzukiCase(text: String?): String? = applyDotCase(TsuzukiTheme.tokens.isDot, text)

internal fun applyDotCase(isDot: Boolean, text: String?): String? = if (isDot) text?.uppercase() else text

/** The app language row's value (Appearance boards): the chosen language, or "Default" when following the system. */
@Composable
fun tsuzukiAppLanguageValue(): (@Composable () -> Unit)? {
    val tag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag().orEmpty()
    val name = if (tag.isEmpty()) {
        tachiyomi.presentation.core.i18n.stringResource(tachiyomi.i18n.MR.strings.label_default)
    } else {
        eu.kanade.tachiyomi.util.system.LocaleHelper.getLocalizedDisplayName(tag)
    }
    return tsuzukiValueLabel(name, name)
}

/** Settings screens draw the theme background (Dot: black + dot grid) behind a transparent scaffold. */
@Composable
fun tsuzukiSettingsBackground(): Modifier {
    val tokens = TsuzukiTheme.tokens
    if (tokens.style == TsuzukiStyle.NONE) return Modifier
    return Modifier.tsuzukiBackground(tokens)
}

/** Transparent in the Tsuzuki themes so [tsuzukiSettingsBackground] shows; null keeps the stock colour. */
@Composable
fun tsuzukiSettingsContainer(): Color? =
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) null else Color.Transparent

/** Preference titles are bold in the Tsuzuki themes (Settings boards); null keeps the stock weight. */
@Composable
fun tsuzukiPreferenceTitleWeight(): androidx.compose.ui.text.font.FontWeight? =
    if (isTsuzukiStyle()) androidx.compose.ui.text.font.FontWeight.Bold else null

/**
 * Import EPUB group cards (board 08-tools "ImportEpub"): the theme's tile, its radius and, in Dot, the hairline border,
 * drawn over the stock Card. Nothing in stock themes.
 */
@Composable
fun tsuzukiImportCard(): Modifier {
    val t = TsuzukiTheme.tokens
    if (t.style == TsuzukiStyle.NONE) return Modifier
    val shape = RoundedCornerShape(t.tileRadius)
    return Modifier
        .clip(shape)
        .background(t.tile, shape)
        .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
}

/** The theme's tile colour for stock cards (Downloads board: plain tiles, red only for progress); null in stock. */
@Composable
fun tsuzukiTileColor(): Color? = if (isTsuzukiStyle()) TsuzukiTheme.tokens.tile else null

/** Import EPUB volume row: "N chapters · file.epub" in the Tsuzuki themes (when the table of contents is known). */
@Composable
fun tsuzukiEpubVolumeLine(chapters: Int, fileName: String): String {
    if (!isTsuzukiStyle() || chapters <= 0) return fileName
    val line = tachiyomi.presentation.core.i18n.stringResource(
        tachiyomi.i18n.MR.strings.tsz_epub_volume_line,
        chapters,
        fileName,
    )
    return tsuzukiCase(line) ?: line
}

/** Import EPUB header: "N files · M novels" in the Tsuzuki themes; null keeps the stock text. */
@Composable
fun tsuzukiEpubSummary(files: Int, novels: Int): String? {
    if (!isTsuzukiStyle()) return null
    val line = tachiyomi.presentation.core.i18n.stringResource(
        tachiyomi.i18n.MR.strings.tsz_epub_summary,
        files,
        novels,
    )
    return tsuzukiCase(line)
}

/** A dialog that fills the window (Mass import board's full page), inside the system bars; null in stock themes. */
@Composable
fun tsuzukiFullPageDialog(): Modifier? = if (isTsuzukiStyle()) {
    Modifier
        .fillMaxSize()
        .systemBarsPadding()
} else {
    null
}

/** Dialog surfaces in the Tsuzuki themes: one step above the background (Dot: the tile black). Null in stock. */
@Composable
fun tsuzukiDialogColor(): Color? = if (isTsuzukiStyle()) {
    if (TsuzukiTheme.tokens.isDot) TsuzukiTheme.tokens.tile else MaterialTheme.colorScheme.surfaceContainer
} else {
    null
}

/** The board's 56 dp Import bar; nothing in stock themes. */
@Composable
fun tsuzukiImportButton(): Modifier =
    if (isTsuzukiStyle()) Modifier.height(56.dp) else Modifier

@Composable
fun isTsuzukiStyle(): Boolean = TsuzukiTheme.tokens.style != TsuzukiStyle.NONE

/**
 * A list preference as a title and a pill row (board 08-tools "UpdateSchedule": check-for-new-chapters interval).
 * The selected entry is the accent pill.
 */
@Composable
fun <T> TsuzukiPillPreference(
    title: String,
    value: T,
    entries: Map<out T, String>,
    enabled: Boolean,
    onValueChange: (T) -> Unit,
    segmented: Boolean = false,
    wrap: Boolean = false,
) {
    val keys = entries.keys.toList()
    val labels = entries.values.toList()
    val selected = keys.indexOf(value).coerceAtLeast(0)
    val select: (Int) -> Unit = { index -> if (enabled) onValueChange(keys[index]) }
    Column(Modifier.padding(vertical = 12.dp)) {
        androidx.compose.material3.Text(
            tsuzukiCase(title) ?: title,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        when {
            segmented -> eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments(
                labels = labels,
                selected = selected,
                onClick = select,
            )
            wrap -> androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            ) {
                labels.forEachIndexed { index, label ->
                    eu.kanade.presentation.theme.tsuzuki.TsuzukiPill(
                        text = label,
                        selected = index == selected,
                        onClick = { select(index) },
                        role = androidx.compose.ui.semantics.Role.RadioButton,
                    )
                }
            }
            else -> eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow(
                labels = labels,
                selected = selected,
                onClick = select,
            )
        }
    }
}
