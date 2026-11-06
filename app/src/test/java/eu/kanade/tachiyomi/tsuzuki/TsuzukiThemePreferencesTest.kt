package eu.kanade.tachiyomi.tsuzuki

import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.tsuzuki.novel.NovelPageColour
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class TsuzukiThemePreferencesTest {

    @Test
    fun `startup persists Colour for every retired app theme and preserves Dot and Colour`() {
        val store = InMemoryPreferenceStore()
        val raw = store.getString(APP_THEME_KEY)
        AppTheme.entries.forEach { theme ->
            raw.set(theme.name)
            migrateStoredThemes(store)
            val expected = if (theme == AppTheme.DOT) AppTheme.DOT else AppTheme.COLOUR
            assertEquals(expected.name, raw.get(), theme.name)
            assertEquals(expected, appThemeFromStorage(raw.get()), theme.name)
        }
        assertEquals(listOf(AppTheme.COLOUR, AppTheme.DOT), supportedAppThemes)
    }

    @Test
    fun `legacy reader values map to the seven shared page choices`() {
        val store = InMemoryPreferenceStore()
        val raw = store.getString(NOVEL_THEME_KEY)
        val reader = ReaderPreferences(store)
        val expected = mapOf(
            "app" to "book",
            "light" to "paper",
            "dark" to "night",
            "grey" to "night",
            "sepia" to "sepia",
            "black" to "black",
            "custom" to "custom",
        )
        for ((old, new) in expected) {
            raw.set(old)
            assertEquals(new, novelThemeFromStorage(raw.get()), old)
            migrateStoredThemes(store)
            assertEquals(new, raw.get(), old)
        }
        assertEquals(
            listOf("book", "paper", "sepia", "sage", "night", "black", "custom"),
            NovelPageColour.entries.map { it.key },
        )
        reader.novelFontColor.set(0xFF654321.toInt())
        reader.novelBackgroundColor.set(0xFF123456.toInt())
        migrateStoredThemes(store)
        assertEquals(0xFF654321.toInt(), reader.novelFontColor.get())
        assertEquals(0xFF123456.toInt(), reader.novelBackgroundColor.get())
    }

    @Test
    fun `startup repairs theme values stored with the wrong type`() {
        val store = InMemoryPreferenceStore()
        store.getInt(APP_THEME_KEY).set(4)
        store.getBoolean(NOVEL_THEME_KEY).set(true)

        migrateStoredThemes(store)
        assertEquals("COLOUR", store.getString(APP_THEME_KEY).get())
        assertEquals("book", store.getString(NOVEL_THEME_KEY).get())
    }

    @Test
    fun `app backup restore canonicalizes theme keys without changing custom colours or unrelated preferences`() {
        val app = BackupPreference(APP_THEME_KEY, StringPreferenceValue("MONET"))
        val novel = BackupPreference(NOVEL_THEME_KEY, StringPreferenceValue("grey"))
        val unrelated = BackupPreference("novel_other", StringPreferenceValue("grey"))
        val restored = listOf(app, novel, unrelated).map(::normalizeBackupThemePreference)
        assertEquals(
            listOf(
                app.copy(value = StringPreferenceValue("COLOUR")),
                novel.copy(value = StringPreferenceValue("night")),
                unrelated,
            ),
            restored,
        )
    }
}
