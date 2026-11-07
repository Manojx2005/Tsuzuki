package eu.kanade.domain.ui

import android.text.format.DateFormat
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.TimeFormat
import eu.kanade.tachiyomi.tsuzuki.APP_THEME_KEY
import eu.kanade.tachiyomi.tsuzuki.appThemeFromStorage
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

class UiPreferences(
    preferenceStore: PreferenceStore,
) {

    val themeMode: Preference<ThemeMode> = preferenceStore.getEnum("pref_theme_mode_key", ThemeMode.SYSTEM)

    val appTheme: Preference<AppTheme> = preferenceStore.getObjectFromString(
        key = APP_THEME_KEY,
        defaultValue = AppTheme.COLOUR,
        serializer = { appThemeFromStorage(it.name).name },
        deserializer = ::appThemeFromStorage,
    )

    val themeDarkAmoled: Preference<Boolean> = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    // Tsuzuki: in the Colour theme, take the colours of the book you're reading.
    val tsuzukiFollowBook: Preference<Boolean> = preferenceStore.getBoolean("tsz_follow_book", false)

    // Tsuzuki Dot: keep manga and novel covers in colour instead of greyscale.
    val tsuzukiDotColourCovers: Preference<Boolean> = preferenceStore.getBoolean("tsz_dot_colour_covers", true)

    // Tsuzuki Colour: accent (ARGB) used when no book palette applies. Default = the original gold.
    val tsuzukiColourAccent: Preference<Int> = preferenceStore.getInt("tsz_colour_accent", 0xFFE9B44C.toInt())

    // Tsuzuki: app-level "Reduce motion", OR-ed with the system animator scale.
    val tsuzukiReduceMotion: Preference<Boolean> = preferenceStore.getBoolean("tsz_reduce_motion", false)

    val relativeTime: Preference<Boolean> = preferenceStore.getBoolean("relative_time_v2", true)

    val dateFormat: Preference<String> = preferenceStore.getString("app_date_format", "")

    val timeFormat: Preference<TimeFormat> = preferenceStore.getEnum("app_time_format", TimeFormat.FOLLOW_LANGUAGE)

    val tabletUiMode: Preference<TabletUiMode> = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    val imagesInDescription: Preference<Boolean> = preferenceStore.getBoolean("pref_render_images_description", true)

    val mangaScrollActionsInOverflow: Preference<Boolean> =
        preferenceStore.getBoolean("pref_manga_scroll_actions_in_overflow", false)

    companion object {
        fun dateFormat(format: String, locale: Locale = Locale.getDefault()): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
            else -> DateTimeFormatter.ofPattern(format, locale)
        }

        fun timeFormat(format: TimeFormat, locale: Locale, is24HourSystem: Boolean): DateTimeFormatter {
            val use24Hours = format.uses24HourClock(is24HourSystem)
                ?: return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            val skeleton = if (use24Hours) "Hm" else "hm"
            val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
            return try {
                DateTimeFormatter.ofPattern(pattern, locale)
            } catch (_: IllegalArgumentException) {
                DateTimeFormatter.ofPattern(if (use24Hours) "HH:mm" else "h:mm a", locale)
            }
        }

        fun dateTimeFormat(
            dateFormat: String,
            timeFormat: TimeFormat,
            locale: Locale,
            is24HourSystem: Boolean,
        ): DateTimeFormatter = DateTimeFormatterBuilder()
            .append(dateFormat(dateFormat, locale))
            .appendLiteral(' ')
            .append(timeFormat(timeFormat, locale, is24HourSystem))
            .toFormatter(locale)
    }
}
