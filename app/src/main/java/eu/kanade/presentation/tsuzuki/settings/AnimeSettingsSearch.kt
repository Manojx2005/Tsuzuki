package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.R

/**
 * Settings search entries for the anime player (AY-INT item 4). Its page is a grid of cards rather than preference
 * rows, so search indexes the card titles; a result opens the player settings page.
 */
@Composable
internal fun animePlayerSearchPreferences(): List<Preference> = listOf(
    R.string.tsz_anime_aniskip,
    R.string.tsz_anime_player_autoplay,
    R.string.tsz_anime_player_speed,
    R.string.tsz_anime_player_subtitles,
    R.string.tsz_anime_player_audio,
    R.string.tsz_anime_player_video,
    R.string.tsz_anime_player_gestures,
).map { Preference.PreferenceItem.TextPreference(title = stringResource(it)) }
