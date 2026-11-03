package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Tsuzuki "Dot" theme.
 *
 * Monochrome dot-matrix look with a single red accent. Paired with Doto + Space Mono
 * typography (see [eu.kanade.presentation.theme.lnrTypography]).
 */
internal object LnrDotColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFFF2D2D),
        onPrimary = Color(0xFFFFFFFF),
        // Main actions (Start/Resume FAB, selected pills) use the red accent, as on the boards.
        primaryContainer = Color(0xFFFF2D2D),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF1A1A1A),
        onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFF8A8A8A),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF262626),
        onTertiaryContainer = Color(0xFFE0E0E0),
        error = Color(0xFFFF2D2D),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFF2A0A0A),
        onErrorContainer = Color(0xFFFF8A80),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF0D0D0D),
        onSurfaceVariant = Color(0xFFA0A0A0),
        outline = Color(0xFF3A3A3A),
        outlineVariant = Color(0xFF262626),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFFFFFFF),
        inverseOnSurface = Color(0xFF000000),
        inversePrimary = Color(0xFFB00000),
        surfaceDim = Color(0xFF000000),
        surfaceBright = Color(0xFF262626),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF070707),
        surfaceContainer = Color(0xFF0D0D0D),
        surfaceContainerHigh = Color(0xFF141414),
        surfaceContainerHighest = Color(0xFF1A1A1A),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFFD70000),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410000),
        secondary = Color(0xFF000000),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFEDEDED),
        onSecondaryContainer = Color(0xFF000000),
        tertiary = Color(0xFF5E5E5E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE0E0E0),
        onTertiaryContainer = Color(0xFF000000),
        error = Color(0xFFD70000),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410000),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF000000),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFF2F2F2),
        onSurfaceVariant = Color(0xFF4A4A4A),
        outline = Color(0xFFBDBDBD),
        outlineVariant = Color(0xFFDDDDDD),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF000000),
        inverseOnSurface = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFFFF2D2D),
        surfaceDim = Color(0xFFEDEDED),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F7F7),
        surfaceContainer = Color(0xFFF2F2F2),
        surfaceContainerHigh = Color(0xFFECECEC),
        surfaceContainerHighest = Color(0xFFE6E6E6),
    )
}
