package eu.kanade.tachiyomi.ui.tsuzuki.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.NovelsTab
import tachiyomi.domain.library.service.LibraryPreferences
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
    if (joined || hideMangaUi) return
    val tabNavigator = LocalTabNavigator.current
    TsuzukiPillRow(
        labels = listOf(stringResource(TDMR.strings.label_novels), stringResource(TDMR.strings.label_manga)),
        selected = if (isManga) 1 else 0,
        onClick = { tabNavigator.current = if (it == 1) LibraryTab else NovelsTab },
    )
}
