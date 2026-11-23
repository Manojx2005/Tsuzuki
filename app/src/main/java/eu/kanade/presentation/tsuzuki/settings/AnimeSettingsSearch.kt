package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerSettingsScreen

/**
 * Settings search entries for the anime player (AY-INT item 4). Since AP11 the page is a standard preference screen,
 * so search indexes its real rows and a result highlights the row it found.
 */
@Composable
internal fun animePlayerSearchPreferences(): List<Preference> = AnimePlayerSettingsScreen.getPreferences()
