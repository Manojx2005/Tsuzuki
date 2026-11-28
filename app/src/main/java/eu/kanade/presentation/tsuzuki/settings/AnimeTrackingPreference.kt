package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTracking
import eu.kanade.tachiyomi.util.system.openInBrowser
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

/**
 * Settings › Tracking: the anime "update trackers after watching" switch (AY-INT item 4), the same preference as
 * the anime tracking sheet's checkbox. Empty while anime is off.
 */
@Composable
internal fun animeTrackingPreferences(logout: (eu.kanade.tachiyomi.data.track.Tracker) -> Unit = {}): List<Preference> {
    if (!TsuzukiAnime.enabled) return emptyList()
    val context = androidx.compose.ui.platform.LocalContext.current
    val simkl = eu.kanade.tachiyomi.tsuzuki.anime.track.simkl.Simkl.instance
    return listOf(
        // A5.4: Simkl tracks anime only, so its sign-in row lives with the anime tracking settings.
        Preference.PreferenceGroup(
            title = stringResource(R.string.tsz_anime_trackers),
            preferenceItems = kotlinx.collections.immutable.persistentListOf(
                Preference.PreferenceItem.TrackerPreference(
                    tracker = simkl,
                    login = {
                        context.openInBrowser(
                            eu.kanade.tachiyomi.tsuzuki.anime.track.simkl.SimklAuth.authUrl(),
                            forceDefaultBrowser = true,
                        )
                    },
                    logout = { logout(simkl) },
                ),
            ),
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = Injekt.get<AnimeTracking>().autoUpdate,
            title = stringResource(R.string.tsz_anime_track_auto_update),
            // The sheet's label reads "update per episode"; out of the anime sheet it needs the media name.
            subtitle = mokoStringResource(MR.strings.tsz_anime_media),
        ),
    )
}
