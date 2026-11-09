package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.AppTheme

/**
 * Tsuzuki design tokens that Material's ColorScheme has no slot for (docs/PLAN.md §3).
 *
 * Both themes share one layout; only these values change. Read them with
 * `TsuzukiTheme.tokens` inside any composable under TachiyomiTheme.
 */
enum class TsuzukiStyle { COLOUR, DOT, NONE }

@Immutable
data class TsuzukiTokens(
    val style: TsuzukiStyle,
    /** Screen background. Colour: the current book's cover colour. Dot: pure black + dot grid. */
    val background: Color,
    /** Bento tile fill. */
    val tile: Color,
    /** Tile border (Dot only; Colour tiles are borderless). */
    val tileBorder: Color?,
    val tileRadius: Dp,
    /** Accent. Colour: the book's second colour. Dot: red, always. */
    val accent: Color,
    /** Content drawn on top of [accent] (selected nav pill, logo tile). */
    val onAccent: Color,
    /** Unfilled part of a solid bar / ring. */
    val track: Color,
    /** Dot progress: read dots, unread dots, and the "you are here" dot. */
    val dotOn: Color,
    val dotOff: Color,
    val dotHere: Color,
    /** Background dot grid (Dot only). */
    val gridDot: Color?,
    val gridSpacing: Dp,
) {
    val isDot get() = style == TsuzukiStyle.DOT

    val sourceWorking: Color get() = if (isDot) dotOn else Color(0xFF7DDFB4)
    val sourceSlow: Color get() = if (isDot) mutedText else Color(0xFFE9B44C)
    val sourceDown: Color get() = if (isDot) accent else Color(0xFFFF8D73)

    /** Read and secondary labels still need to be readable on both the base and the tile. */
    val mutedText: Color
        get() {
            val muted = if (isDot) Color(0xFF8A8A8A) else dotOn.copy(alpha = 0.72f)
            val fill = tile.compositeOver(background)
            return if (textContrast(muted, fill) >= 4.5f && textContrast(muted, background) >= 4.5f) {
                muted
            } else {
                dotOn
            }
        }
}

internal fun textContrast(ink: Color, surface: Color): Float {
    val first = ink.compositeOver(surface).luminance()
    val second = surface.luminance()
    return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
}

/** Default Colour palette: navy book + gold accent, used until a cover palette is known. */
val ColourTokens = TsuzukiTokens(
    style = TsuzukiStyle.COLOUR,
    background = Color(0xFF1C2B4A),
    tile = Color.White.copy(alpha = 0.08f),
    tileBorder = null,
    tileRadius = 28.dp,
    accent = Color(0xFFE9B44C),
    onAccent = Color(0xFF1C2B4A),
    track = Color.White.copy(alpha = 0.18f),
    dotOn = Color.White,
    dotOff = Color.White.copy(alpha = 0.18f),
    dotHere = Color(0xFFE9B44C),
    gridDot = null,
    gridSpacing = 12.dp,
)

val DotTokens = TsuzukiTokens(
    style = TsuzukiStyle.DOT,
    background = Color.Black,
    tile = Color(0xFF0D0D0D),
    tileBorder = Color(0xFF262626),
    tileRadius = 6.dp,
    accent = Color(0xFFFF2D2D),
    onAccent = Color.White,
    track = Color(0xFF2E2E2E),
    dotOn = Color.White,
    dotOff = Color(0xFF2E2E2E),
    dotHere = Color(0xFFFF2D2D),
    gridDot = Color(0xFF1C1C1C),
    gridSpacing = 12.dp,
)

/** Fallback for the legacy Mihon themes: neutral tokens so Tsuzuki components still render. */
val NeutralTokens = ColourTokens.copy(
    style = TsuzukiStyle.NONE,
    background = Color.Unspecified,
)

fun tsuzukiTokensFor(appTheme: AppTheme, colourAccent: Color? = null): TsuzukiTokens = when (appTheme) {
    AppTheme.COLOUR -> if (colourAccent == null || colourAccent == ColourTokens.accent) {
        ColourTokens
    } else {
        ColourTokens.copy(accent = colourAccent, dotHere = colourAccent)
    }
    AppTheme.DOT -> DotTokens
    else -> NeutralTokens
}

val LocalTsuzukiTokens = staticCompositionLocalOf { NeutralTokens }

object TsuzukiTheme {
    val tokens: TsuzukiTokens
        @Composable get() = LocalTsuzukiTokens.current
}

/**
 * Re-skins the Colour theme with one book's cover colours (e.g. on Novel details and in
 * the reader). In the Dot theme the cover never changes the colours: red stays the only accent.
 */
@Composable
fun ProvideBookPalette(palette: BookPalette?, content: @Composable () -> Unit) {
    val base = LocalTsuzukiTokens.current
    val active = palette != null && base.style == TsuzukiStyle.COLOUR
    // Colours morph when the book changes (Motion board "Stack shuffle + colour morph").
    val spec = androidx.compose.animation.core.tween<Color>(TsuzukiMotion.STACK_MS, easing = TsuzukiMotion.Standard)
    val background by androidx.compose.animation.animateColorAsState(
        if (active) palette!!.background else base.background,
        spec,
        label = "bookBg",
    )
    val accent by androidx.compose.animation.animateColorAsState(
        if (active) palette!!.accent else base.accent,
        spec,
        label = "bookAccent",
    )
    // A chosen Colour accent (no book palette) still needs MaterialTheme's primary to follow it.
    val customAccent = base.style == TsuzukiStyle.COLOUR && base.accent != ColourTokens.accent
    if (!active && !customAccent) {
        CompositionLocalProvider(LocalTsuzukiTokens provides base, content = content)
        return
    }
    val tokens = base.copy(background = background, accent = accent, onAccent = background, dotHere = accent)
    // Upstream screens (History, Updates, Library…) paint from MaterialTheme, so re-colour it too;
    // otherwise their background would not match the rail and Home.
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme.copy(
        background = background,
        surface = background,
        primary = accent,
        onPrimary = background,
        primaryContainer = accent,
        onPrimaryContainer = background,
    )
    androidx.compose.material3.MaterialTheme(
        colorScheme = scheme,
        typography = androidx.compose.material3.MaterialTheme.typography,
        shapes = androidx.compose.material3.MaterialTheme.shapes,
    ) {
        CompositionLocalProvider(
            LocalTsuzukiTokens provides tokens,
            tachiyomi.presentation.core.theme.LocalActiveColor provides tokens.activeColor,
            content = content,
        )
    }
}

/** Upstream's "active" tint (filter icons and the like): the accent in the Tsuzuki themes. */
val TsuzukiTokens.activeColor: Color
    get() = if (style == TsuzukiStyle.NONE) Color.Unspecified else accent
