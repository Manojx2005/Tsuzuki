package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.tachiyomi.ui.browse.extension.NovelExtensionReposScreen
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import mihon.domain.extension.interactor.GetExtensionStoreCountAsFlow
import mihon.domain.extension.model.ContentWarning
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsBrowseScreen : SearchableSettings {

    override val supportsReset: Boolean get() = true

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.browse

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val getExtensionStoreCountAsFlow = remember { Injekt.get<GetExtensionStoreCountAsFlow>() }

        val reposCount by getExtensionStoreCountAsFlow().collectAsState(0)

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_sources),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.hideInLibraryItems,
                        title = stringResource(MR.strings.pref_hide_in_library_items),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = sourcePreferences.pageLoadDelay,
                        title = stringResource(MR.strings.pref_source_page_load_delay),
                        subtitle = stringResource(MR.strings.pref_source_page_load_delay_summary),
                        entries = (0..15).associateWith { stringResource(MR.strings.seconds_short, it) },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.showPageNumber,
                        title = stringResource(MR.strings.pref_show_page_number),
                        subtitle = stringResource(MR.strings.pref_source_page_number_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.skipCoverLoading,
                        title = stringResource(MR.strings.pref_source_skip_cover_loading),
                        subtitle = stringResource(MR.strings.pref_source_skip_cover_loading_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = sourcePreferences.confirmBackAfterPages,
                        title = stringResource(MR.strings.pref_source_confirm_back_after_pages),
                        subtitle = stringResource(MR.strings.pref_source_confirm_back_after_pages_summary),
                        entries = mapOf(0 to stringResource(MR.strings.disabled)) +
                            listOf(3, 5, 10, 20).associateWith { pluralStringResource(MR.plurals.pref_pages, it, it) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.extensionStores),
                        subtitle = pluralStringResource(MR.plurals.num_repos, reposCount.toInt(), reposCount),
                        onClick = {
                            navigator.push(ExtensionStoresScreen())
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(TDMR.strings.pref_novel_extension_repos),
                        subtitle = stringResource(TDMR.strings.pref_novel_extension_repos_summary),
                        onClick = {
                            navigator.push(NovelExtensionReposScreen())
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_extensions),
                preferenceItems = listOf(
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = sourcePreferences.enabledContentWarnings,
                        entries = mapOf(
                            ContentWarning.SAFE to stringResource(MR.strings.ext_content_warning_safe),
                            ContentWarning.MIXED to stringResource(MR.strings.ext_content_warning_mixed),
                            ContentWarning.NSFW to stringResource(MR.strings.ext_content_warning_nsfw),
                        ),
                        title = stringResource(MR.strings.pref_allowed_content_warnings),
                        subtitleProvider = { value, entries ->
                            remember(value, entries) {
                                entries.filterKeys { it in value }.values.joinToString()
                            }
                                .takeUnless { it.isBlank() }
                                ?: stringResource(MR.strings.none)
                        },
                        onValueChanged = { newValue ->
                            val added = newValue - sourcePreferences.enabledContentWarnings.get()
                            if (added.any { it != ContentWarning.SAFE }) {
                                (context as FragmentActivity).authenticate(
                                    title = context.stringResource(MR.strings.pref_allowed_content_warnings),
                                )
                            } else {
                                true
                            }
                        },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.applyContentWarningsToInstalled,
                        title = stringResource(MR.strings.pref_apply_content_warnings_to_installed),
                        subtitle = stringResource(MR.strings.pref_apply_content_warnings_to_installed_summary),
                        onValueChanged = { newValue ->
                            if (newValue) {
                                true
                            } else {
                                (context as FragmentActivity).authenticate(
                                    title = context.stringResource(
                                        MR.strings.pref_apply_content_warnings_to_installed,
                                    ),
                                )
                            }
                        },
                    ),
                    Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.content_warnings_info)),
                ),
            ),
        )
    }
}
