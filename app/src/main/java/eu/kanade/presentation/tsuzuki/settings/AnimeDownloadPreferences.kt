package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import kotlinx.collections.immutable.toImmutableMap
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Settings › Downloads: delete a saved episode once it is watched (anime counterpart of the book option, AP11). */
@Composable
internal fun animeDeleteAfterWatchedPreference(): Preference.PreferenceItem.SwitchPreference? {
    if (!TsuzukiAnime.enabled) return null
    return Preference.PreferenceItem.SwitchPreference(
        preference = remember { eu.kanade.tachiyomi.tsuzuki.anime.download.animeDeleteAfterWatched(Injekt.get()) },
        title = stringResource(R.string.tsz_anime_delete_after_watched),
    )
}

@Composable
internal fun animeDownloadWifiPreference(): Preference? {
    if (!TsuzukiAnime.enabled) return null
    val downloads = remember { Injekt.get<AnimeDownloads>() }
    val queue by downloads.state.collectAsState()
    val title = stringResource(R.string.tsz_anime_download_wifi_only)
    return Preference.PreferenceItem.CustomPreference(title) {
        SwitchPreferenceWidget(
            title = title,
            checked = queue.wifiOnly,
            onCheckedChanged = downloads::setWifiOnly,
        )
    }
}

/** Settings › Downloads: Aniyomi's "Download ahead" while watching saved episodes (owner 2026-11-28). */
@Composable
internal fun animeDownloadAheadPreference(): Preference.PreferenceItem.ListPreference<Int>? {
    if (!TsuzukiAnime.enabled) return null
    val off = tachiyomi.presentation.core.i18n.stringResource(tachiyomi.i18n.MR.strings.disabled)
    val entries = eu.kanade.tachiyomi.tsuzuki.anime.ANIME_DOWNLOAD_AHEAD_CHOICES.associateWith { count ->
        if (count == 0) off else stringResource(R.string.tsz_anime_download_ahead_count, count)
    }
    return Preference.PreferenceItem.ListPreference(
        preference = remember { eu.kanade.tachiyomi.tsuzuki.anime.animeDownloadAhead(Injekt.get()) },
        entries = entries.toImmutableMap(),
        title = stringResource(R.string.tsz_anime_download_ahead),
        subtitle = stringResource(R.string.tsz_anime_download_ahead_summary),
    )
}
