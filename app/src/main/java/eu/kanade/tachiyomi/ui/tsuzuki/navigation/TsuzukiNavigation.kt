package eu.kanade.tachiyomi.ui.tsuzuki.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.presentation.tsuzuki.anime.AnimeLibraryTab
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.NovelsTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.tsuzuki.home.TsuzukiHomeTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab

/** The phone bar and tablet rail share destinations; History remains a retained, hidden tab. */
object TsuzukiNavigation {
    val tabs: List<Tab> = listOf(TsuzukiHomeTab, NovelsTab, UpdatesTab, BrowseTab, MoreTab)

    fun selected(tab: cafe.adriel.voyager.navigator.tab.Tab): cafe.adriel.voyager.navigator.tab.Tab = when (tab) {
        LibraryTab, AnimeLibraryTab -> NovelsTab
        HistoryTab -> MoreTab
        else -> tab
    }

    fun restore(key: String?, joined: Boolean = false): Tab {
        val retained = if (isAnimeLibraryKey(key)) {
            AnimeLibraryTab
        } else {
            (tabs + LibraryTab + HistoryTab).firstOrNull { it.key == key } ?: TsuzukiHomeTab
        }
        return if (joined && retained == LibraryTab) NovelsTab else retained
    }

    fun library(key: String?, joined: Boolean): Tab = when {
        isAnimeLibraryKey(key) -> AnimeLibraryTab
        !joined && key == LibraryTab.key -> LibraryTab
        else -> NovelsTab
    }

    /** Route external shortcuts too, without turning History into a different destination. */
    fun destination(request: HomeScreen.Tab, library: Tab = NovelsTab): Tab = when (request) {
        HomeScreen.Tab.Home -> TsuzukiHomeTab
        is HomeScreen.Tab.Library -> library
        HomeScreen.Tab.Updates -> UpdatesTab
        HomeScreen.Tab.History -> HistoryTab
        is HomeScreen.Tab.Browse -> BrowseTab
        is HomeScreen.Tab.More -> MoreTab
    }
}

@Composable
internal fun keepAnimeLibraryTabAvailable(tabNavigator: TabNavigator) {
    val animeEnabled = TsuzukiAnime.enabled
    LaunchedEffect(animeEnabled, tabNavigator.current) {
        if (!animeEnabled && tabNavigator.current == AnimeLibraryTab) {
            tabNavigator.current = NovelsTab
        }
    }
}

internal fun isAnimeLibraryKey(key: String?, animeEnabled: Boolean = TsuzukiAnime.enabled): Boolean =
    animeEnabled && key == AnimeLibraryTab.key

/** Books library <-> anime library: the media segments change, the screen does not (owner 2026-11-28). */
internal fun isLibraryMediaSwitch(
    from: cafe.adriel.voyager.navigator.tab.Tab,
    to: cafe.adriel.voyager.navigator.tab.Tab,
): Boolean {
    val books = setOf<cafe.adriel.voyager.navigator.tab.Tab>(NovelsTab, LibraryTab)
    return (from in books && to == AnimeLibraryTab) || (from == AnimeLibraryTab && to in books)
}

/** No transition for the books <-> anime library media switch; null keeps the normal tab fade. */
internal fun libraryMediaTransition(
    from: cafe.adriel.voyager.navigator.tab.Tab,
    to: cafe.adriel.voyager.navigator.tab.Tab,
): androidx.compose.animation.ContentTransform? = if (isLibraryMediaSwitch(from, to)) {
    androidx.compose.animation.ContentTransform(
        androidx.compose.animation.EnterTransition.None,
        androidx.compose.animation.ExitTransition.None,
    )
} else {
    null
}
