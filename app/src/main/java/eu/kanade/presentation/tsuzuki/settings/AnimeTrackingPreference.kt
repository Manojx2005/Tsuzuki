package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTracking
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

/**
 * Settings › Tracking: the anime "update trackers after watching" switch (AY-INT item 4), the same preference as
 * the anime tracking sheet's checkbox. Empty while anime is off.
 */
@Composable
internal fun animeTrackingPreferences(): List<Preference> {
    if (!TsuzukiAnime.enabled) return emptyList()
    return listOf(
        Preference.PreferenceItem.SwitchPreference(
            preference = Injekt.get<AnimeTracking>().autoUpdate,
            title = stringResource(R.string.tsz_anime_track_auto_update),
            // The sheet's label reads "update per episode"; out of the anime sheet it needs the media name.
            subtitle = mokoStringResource(MR.strings.tsz_anime_media),
        ),
    )
}
