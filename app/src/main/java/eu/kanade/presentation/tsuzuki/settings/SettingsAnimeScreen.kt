package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.res.stringResource as androidStringResource

internal object SettingsAnimeScreen : SearchableSettings {
    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.tsz_anime_media

    @Composable
    override fun getPreferences(): List<Preference> = listOfNotNull(animeBetaPreference())
}

@Composable
internal fun animeBetaPreference(title: String? = null): Preference.PreferenceItem.SwitchPreference? {
    if (!TsuzukiAnime.supported) return null
    return Preference.PreferenceItem.SwitchPreference(
        preference = remember { TsuzukiAnime.preference(Injekt.get()) },
        title = title ?: androidStringResource(R.string.tsz_anime_beta),
        subtitle = androidStringResource(R.string.tsz_anime_beta_summary),
    )
}
