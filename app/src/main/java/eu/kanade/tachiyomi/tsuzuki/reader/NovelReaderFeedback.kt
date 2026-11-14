package eu.kanade.tachiyomi.tsuzuki.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.tsuzukiTokensFor
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

internal data class NovelFeedbackColors(
    val selectionFill: Int,
    val selectionInk: Int,
    val readAlongFill: Int,
    val readAlongInk: Int,
)

internal fun novelFeedbackColors(tokens: TsuzukiTokens, pageInk: Int): NovelFeedbackColors = NovelFeedbackColors(
    selectionFill = tokens.accent.toArgb(),
    selectionInk = tokens.onAccent.toArgb(),
    readAlongFill = (if (tokens.isDot) tokens.track else tokens.accent.copy(alpha = .28f)).toArgb(),
    readAlongInk = if (tokens.isDot) tokens.dotOn.toArgb() else pageInk,
)

/** Non-Compose renderer adapter; uses the same stored accent as TachiyomiTheme. */
internal fun novelFeedbackColors(pageInk: Int): NovelFeedbackColors? {
    val ui = Injekt.get<UiPreferences>()
    val tokens = tsuzukiTokensFor(ui.appTheme.get(), Color(ui.tsuzukiColourAccent.get()))
    return tokens.takeIf { it.style != TsuzukiStyle.NONE }?.let { novelFeedbackColors(it, pageInk) }
}

internal fun novelSelectionCss(colors: NovelFeedbackColors): String =
    "::selection { background-color: ${novelCssColor(colors.selectionFill)} !important; " +
        "color: ${novelCssColor(colors.selectionInk)} !important; }"

internal fun novelCssColor(argb: Int) = String.format(
    Locale.ROOT,
    "#%02x%02x%02x%02x",
    argb ushr 16 and 255,
    argb ushr 8 and 255,
    argb and 255,
    argb ushr 24 and 255,
)

internal fun novelHighlightScrollTarget(lineTopPx: Int, topBarPx: Int, gapPx: Int, topInsetPx: Int = 0): Int =
    (lineTopPx + topInsetPx.coerceAtLeast(0) - topBarPx.coerceAtLeast(0) - gapPx.coerceAtLeast(0)).coerceAtLeast(0)

internal fun novelEndCardClearance(hasCard: Boolean, bottomBarPx: Int, gapPx: Int): Int =
    if (hasCard && bottomBarPx > 0) bottomBarPx + gapPx.coerceAtLeast(0) else 0
