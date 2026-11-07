package eu.kanade.presentation.theme.tsuzuki

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.ThemeMode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Dot and Colour are dark-only designs: their tokens never change with light/dark, so a light Material scheme or
 * dark status-bar icons under them break the screens. The stored light/dark choice only applies to legacy themes.
 */
fun effectiveThemeMode(appTheme: AppTheme, themeMode: ThemeMode): ThemeMode =
    if (appTheme == AppTheme.DOT || appTheme == AppTheme.COLOUR) ThemeMode.DARK else themeMode

/**
 * Pure-black ("AMOLED") dark mode only applies to legacy themes. Dot and Colour draw their own surfaces from tokens, so
 * a stored AMOLED flag (e.g. restored from Tsundoku) would turn Material's background black under navy Tsuzuki
 * surfaces; Tsuzuki has no switch for it, so it is ignored there.
 */
fun effectiveAmoled(appTheme: AppTheme, isAmoled: Boolean): Boolean =
    isAmoled && appTheme != AppTheme.DOT && appTheme != AppTheme.COLOUR

/** [effectiveThemeMode] for the app theme that is currently stored. */
fun effectiveThemeMode(themeMode: ThemeMode): ThemeMode =
    effectiveThemeMode(Injekt.get<UiPreferences>().appTheme.get(), themeMode)
