package eu.kanade.tachiyomi.tsuzuki

import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

internal const val APP_THEME_KEY = "pref_app_theme"
internal const val NOVEL_THEME_KEY = "pref_novel_theme"
internal const val BOOK_PAGE_THEME = "book"

internal val supportedAppThemes = listOf(AppTheme.COLOUR, AppTheme.DOT)

internal fun appThemeFromStorage(value: String): AppTheme =
    if (value == AppTheme.DOT.name) AppTheme.DOT else AppTheme.COLOUR

internal fun novelThemeFromStorage(value: String): String = when (value) {
    "app", BOOK_PAGE_THEME -> BOOK_PAGE_THEME
    "light" -> "paper"
    "dark", "grey" -> "night"
    "paper", "sepia", "sage", "night", "black", "custom" -> value
    else -> BOOK_PAGE_THEME
}

/** Write the canonical keys once. Subsequent starts perform no writes. */
internal fun migrateStoredThemes(preferences: PreferenceStore) {
    preferences.getString(APP_THEME_KEY, AppTheme.COLOUR.name).writeCanonical { appThemeFromStorage(it).name }
    preferences.getString(NOVEL_THEME_KEY, BOOK_PAGE_THEME).writeCanonical(::novelThemeFromStorage)
}

private fun Preference<String>.writeCanonical(normalize: (String) -> String) {
    val old = try {
        get()
    } catch (_: ClassCastException) {
        null
    }
    val canonical = normalize(old.orEmpty())
    if (old != canonical) set(canonical)
}

/** App backups are normalized before the restored value can be observed by the UI. */
internal fun normalizeBackupThemePreference(preference: BackupPreference): BackupPreference {
    val raw = (preference.value as? StringPreferenceValue)?.value ?: return preference
    val normalized = when (preference.key) {
        APP_THEME_KEY -> appThemeFromStorage(raw).name
        NOVEL_THEME_KEY -> novelThemeFromStorage(raw)
        else -> return preference
    }
    return preference.copy(value = StringPreferenceValue(normalized))
}
