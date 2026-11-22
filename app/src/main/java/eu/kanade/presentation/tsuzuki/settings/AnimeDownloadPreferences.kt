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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

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
