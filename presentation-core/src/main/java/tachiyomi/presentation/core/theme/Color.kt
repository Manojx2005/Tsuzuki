package tachiyomi.presentation.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Set by themes that have their own accent for "active" states (Tsuzuki); unspecified keeps the yellow. */
val LocalActiveColor = staticCompositionLocalOf { Color.Unspecified }

val ColorScheme.active: Color
    @Composable
    get() {
        LocalActiveColor.current.takeIf { it != Color.Unspecified }?.let { return it }
        return if (isSystemInDarkTheme()) Color(255, 235, 59) else Color(255, 193, 7)
    }
