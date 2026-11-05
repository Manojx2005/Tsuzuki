package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import eu.kanade.domain.ui.UiPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The accent choices for the Colour theme (Settings > Appearance). Used wherever no book palette applies
 * ("Follow the book" off, or no cover colours known yet). Dot always stays red.
 */
enum class TsuzukiAccentSwatch(val argb: Int) {
    GOLD(0xFFE9B44C.toInt()),
    AMBER(0xFFFFB000.toInt()),
    CORAL(0xFFFF6B5B.toInt()),
    ROSE(0xFFF06FA8.toInt()),
    VIOLET(0xFFB28CF5.toInt()),
    SKY(0xFF5BB8F5.toInt()),
    MINT(0xFF5FD3A0.toInt()),
    WHITE(0xFFFFFFFF.toInt()),
    ;

    val color: Color get() = Color(argb)

    companion object {
        val Default = GOLD

        /** The swatch stored as [argb]; an unknown value (old or hand-edited preference) falls back to gold. */
        fun fromArgb(argb: Int): TsuzukiAccentSwatch = entries.firstOrNull { it.argb == argb } ?: Default
    }
}

/** The chosen Colour accent, or null where Injekt isn't set up (previews). */
@Composable
fun rememberColourAccent(): Color? {
    val pref = remember {
        runCatching { Injekt.get<UiPreferences>().tsuzukiColourAccent }.getOrNull()
    } ?: return null
    val argb by remember(pref) { pref.changes() }.collectAsState(initial = pref.get())
    return TsuzukiAccentSwatch.fromArgb(argb).color
}
