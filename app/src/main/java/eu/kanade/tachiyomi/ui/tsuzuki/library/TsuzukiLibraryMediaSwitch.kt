package eu.kanade.tachiyomi.ui.tsuzuki.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.NovelsTab
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Board "LibraryList": Novels / Manga switch under the library title. The Tsuzuki nav has Home
 * instead of the two library tabs, so this is how the manga library is reached when the
 * libraries aren't combined. Hidden with the combined library, with manga UI hidden, and in the
 * legacy themes. Anime joins once it lands.
 */
@Composable
fun TsuzukiLibraryMediaSwitch(isManga: Boolean) {
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) return
    val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
    val basePreferences = remember { Injekt.get<BasePreferences>() }
    val joined by libraryPreferences.joinedLibrary.collectAsState()
    val hideMangaUi by basePreferences.hideMangaUi.collectAsState()
    val tabNavigator = LocalTabNavigator.current
    androidx.compose.foundation.layout.Column {
        if ((!joined && !hideMangaUi) || TsuzukiAnime.enabled) {
            val labels = buildList {
                add(stringResource(TDMR.strings.label_novels))
                if (!joined && !hideMangaUi) add(stringResource(TDMR.strings.label_manga))
                if (TsuzukiAnime.enabled) add(stringResource(MR.strings.tsz_anime_media))
            }
            TsuzukiPillRow(
                labels = labels,
                selected = if (isManga && !joined && !hideMangaUi) 1 else 0,
                onClick = {
                    tabNavigator.current = when {
                        TsuzukiAnime.enabled && it == labels.lastIndex -> AnimeLibraryTab
                        it == 1 -> LibraryTab
                        else -> NovelsTab
                    }
                },
            )
        }
        if (!isManga) eu.kanade.presentation.tsuzuki.library.TsuzukiLibraryQueryPills()
    }
}
