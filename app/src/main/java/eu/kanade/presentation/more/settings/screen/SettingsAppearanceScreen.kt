package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.TimeFormat
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreen
import eu.kanade.presentation.util.rememberIs24HourSystem
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Clock

object SettingsAppearanceScreen : SearchableSettings {

    override val supportsReset: Boolean get() = true

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val libraryPreferences = remember { Injekt.get<tachiyomi.domain.library.service.LibraryPreferences>() }

        return listOf(
            getThemeGroup(uiPreferences = uiPreferences),
            getDisplayGroup(uiPreferences = uiPreferences),
            getLibraryLayoutGroup(libraryPreferences = libraryPreferences),
        )
    }

    @Composable
    private fun getThemeGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val themeModePref = uiPreferences.themeMode
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme
        val appTheme by appThemePref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_theme),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_app_theme),
                ) {
                    eu.kanade.presentation.tsuzuki.settings.TsuzukiAppearanceThemePicker(
                        appTheme = appTheme,
                        themeMode = themeMode,
                        onPickTheme = { appThemePref.set(it) },
                        onPickMode = {
                            themeModePref.set(it)
                            setAppCompatDelegateThemeMode(it)
                        },
                    )
                },
                // Tsuzuki: "Follow the book" (SettingsAppearance board). Colour only; disabled preferences are
                // hidden in this screen, so it simply isn't shown in Dot.
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.tsuzukiFollowBook,
                    title = stringResource(MR.strings.tsz_follow_book),
                    subtitle = stringResource(MR.strings.tsz_follow_book_sub),
                    enabled = appTheme == AppTheme.COLOUR,
                ),
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.tsz_appearance_accent),
                ) {
                    eu.kanade.presentation.tsuzuki.settings.TsuzukiAccentPicker(uiPreferences)
                },
                // Tsuzuki: app-level Reduce motion (both Tsuzuki themes), on top of the system animation setting.
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.tsuzukiReduceMotion,
                    title = stringResource(MR.strings.tsz_appearance_reduce_motion),
                    subtitle = stringResource(MR.strings.tsz_appearance_reduce_motion_sub),
                    enabled = appTheme == AppTheme.COLOUR || appTheme == AppTheme.DOT,
                ),
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.tsz_appearance_dot_covers),
                ) {
                    eu.kanade.presentation.tsuzuki.settings.TsuzukiDotCoverPicker(uiPreferences)
                },
            ),
        )
    }

    @Composable
    private fun getDisplayGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime() }

        val dateFormat by uiPreferences.dateFormat.collectAsState()
        val locale = LocalConfiguration.current.locales[0]
        val is24HourSystem = rememberIs24HourSystem()
        val formattedNow = remember(dateFormat, locale) {
            UiPreferences.dateFormat(dateFormat, locale).format(now)
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_app_language),
                    widget = eu.kanade.presentation.tsuzuki.settings.tsuzukiAppLanguageValue(),
                    onClick = { navigator.push(AppLanguageScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.tabletUiMode,
                    entries = TabletUiMode.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_tablet_ui_mode),
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.dateFormat,
                    entries = DateFormats
                        .associateWith {
                            val formattedDate = UiPreferences.dateFormat(it, locale).format(now)
                            "${it.ifEmpty { stringResource(MR.strings.label_default) }} ($formattedDate)"
                        },
                    title = stringResource(MR.strings.pref_date_format),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.timeFormat,
                    entries = TimeFormat.entries.associateWith {
                        val example = UiPreferences.timeFormat(it, locale, is24HourSystem).format(now)
                        "${stringResource(it.titleRes)} ($example)"
                    },
                    title = stringResource(MR.strings.tsz_time_format),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.relativeTime,
                    title = stringResource(MR.strings.pref_relative_format),
                    subtitle = stringResource(
                        MR.strings.pref_relative_format_summary,
                        stringResource(MR.strings.relative_time_today),
                        formattedNow,
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.imagesInDescription,
                    title = stringResource(MR.strings.pref_display_images_description),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.mangaScrollActionsInOverflow,
                    title = stringResource(TDMR.strings.pref_manga_scroll_actions_in_overflow),
                    subtitle = stringResource(TDMR.strings.pref_manga_scroll_actions_in_overflow_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getLibraryLayoutGroup(
        libraryPreferences: tachiyomi.domain.library.service.LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val basePreferences = remember { Injekt.get<eu.kanade.domain.base.BasePreferences>() }
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_library_layout),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.joinedLibrary,
                    title = stringResource(MR.strings.pref_combined_library),
                    subtitle = stringResource(MR.strings.pref_combined_library_summary),
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = basePreferences.hideMangaUi,
                    title = stringResource(TDMR.strings.pref_hide_manga_ui),
                    subtitle = stringResource(TDMR.strings.pref_hide_manga_ui_summary),
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
            ),
        )
    }
}

private val DateFormats = listOf(
    "", // Default
    "MM/dd/yy",
    "dd/MM/yy",
    "yyyy-MM-dd",
    "dd MMM yyyy",
    "MMM dd, yyyy",
)
