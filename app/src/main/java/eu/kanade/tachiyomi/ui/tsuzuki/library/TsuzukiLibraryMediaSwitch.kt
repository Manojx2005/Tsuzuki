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
fun TsuzukiLibraryMediaSwitch(
    isManga: Boolean,
    manhwaOnly: Boolean = false,
    onManhwaOnlyChanged: ((Boolean) -> Unit)? = null,
) {
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) return
    val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
    val basePreferences = remember { Injekt.get<BasePreferences>() }
    val joined by libraryPreferences.joinedLibrary.collectAsState()
    val hideMangaUi by basePreferences.hideMangaUi.collectAsState()
    val tabNavigator = LocalTabNavigator.current
    androidx.compose.foundation.layout.Column {
        if (!joined && !hideMangaUi) {
            val labels = listOf(stringResource(TDMR.strings.label_novels), stringResource(TDMR.strings.label_manga))
            val manhwa = stringResource(MR.strings.tsz_manhwa)
            TsuzukiPillRow(
                labels = if (onManhwaOnlyChanged == null) labels else labels + manhwa,
                selected = when {
                    !isManga -> 0
                    manhwaOnly -> 2
                    else -> 1
                },
                onClick = { index ->
                    onManhwaOnlyChanged?.invoke(index == 2)
                    tabNavigator.current = if (index == 0) NovelsTab else LibraryTab
                },
            )
        }
        if (!isManga) eu.kanade.presentation.tsuzuki.library.TsuzukiLibraryQueryPills()
    }
}
