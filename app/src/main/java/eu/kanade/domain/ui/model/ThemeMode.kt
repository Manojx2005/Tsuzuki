package eu.kanade.domain.ui.model

import androidx.appcompat.app.AppCompatDelegate

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

fun setAppCompatDelegateThemeMode(themeMode: ThemeMode) {
    AppCompatDelegate.setDefaultNightMode(
        // Tsuzuki: Dot and Colour are dark-only, so they always use night mode.
        when (eu.kanade.presentation.theme.tsuzuki.effectiveThemeMode(themeMode)) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        },
    )
}
