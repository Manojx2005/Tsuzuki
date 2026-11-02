package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * LightNovelReader "Colour" theme.
 *
 * Night-ink navy with a lamp-gold accent. This is the default palette; the reader and
 * novel screens may further tint it from the current book's cover (see docs/PLAN.md §3).
 */
internal object LnrColourColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFE9B44C),
        onPrimary = Color(0xFF1C2B4A),
        primaryContainer = Color(0xFF3A4A6B),
        onPrimaryContainer = Color(0xFFFFE6B0),
        secondary = Color(0xFF9FB4D9),
        onSecondary = Color(0xFF14213B),
        secondaryContainer = Color(0xFF2E4166),
        onSecondaryContainer = Color(0xFFDCE6F7),
        tertiary = Color(0xFFF08A5D),
        onTertiary = Color(0xFF3A1406),
        tertiaryContainer = Color(0xFF5B2F1E),
        onTertiaryContainer = Color(0xFFFFDBCC),
        error = Color(0xFFFF8A70),
        onError = Color(0xFF3A0A00),
        errorContainer = Color(0xFF5C1A0E),
        onErrorContainer = Color(0xFFFFDAD2),
        background = Color(0xFF1C2B4A),
        onBackground = Color(0xFFF2F0EA),
        surface = Color(0xFF1C2B4A),
        onSurface = Color(0xFFF2F0EA),
        surfaceVariant = Color(0xFF26375A),
        onSurfaceVariant = Color(0xFFC9D2E3),
        outline = Color(0xFF6F7FA0),
        outlineVariant = Color(0xFF3A4A6B),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFF2F0EA),
        inverseOnSurface = Color(0xFF1C2B4A),
        inversePrimary = Color(0xFF8A6418),
        surfaceDim = Color(0xFF16233D),
        surfaceBright = Color(0xFF34476E),
        surfaceContainerLowest = Color(0xFF172440),
        surfaceContainerLow = Color(0xFF1F2E4E),
        surfaceContainer = Color(0xFF233456),
        surfaceContainerHigh = Color(0xFF283A5E),
        surfaceContainerHighest = Color(0xFF2E4166),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF1C2B4A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE9B44C),
        onPrimaryContainer = Color(0xFF1C2B4A),
        secondary = Color(0xFF4D5D7E),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD9E2F2),
        onSecondaryContainer = Color(0xFF14213B),
        tertiary = Color(0xFF9A4A26),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDBCC),
        onTertiaryContainer = Color(0xFF3A1406),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = Color(0xFFF6F2EA),
        onBackground = Color(0xFF17202F),
        surface = Color(0xFFF6F2EA),
        onSurface = Color(0xFF17202F),
        surfaceVariant = Color(0xFFE6E1D6),
        onSurfaceVariant = Color(0xFF4A4F5C),
        outline = Color(0xFF7A7F8C),
        outlineVariant = Color(0xFFCBC6BA),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF1C2B4A),
        inverseOnSurface = Color(0xFFF2F0EA),
        inversePrimary = Color(0xFFE9B44C),
        surfaceDim = Color(0xFFE2DDD2),
        surfaceBright = Color(0xFFFFFCF5),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1ECE2),
        surfaceContainer = Color(0xFFECE6DA),
        surfaceContainerHigh = Color(0xFFE6E0D3),
        surfaceContainerHighest = Color(0xFFE0D9CB),
    )
}
