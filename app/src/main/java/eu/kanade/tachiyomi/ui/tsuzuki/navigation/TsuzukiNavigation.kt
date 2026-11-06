package eu.kanade.tachiyomi.ui.tsuzuki.navigation

import eu.kanade.presentation.util.Tab
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
        LibraryTab -> NovelsTab
        HistoryTab -> MoreTab
        else -> tab
    }

    fun restore(key: String?, joined: Boolean = false): Tab {
        val retained = (tabs + LibraryTab + HistoryTab).firstOrNull { it.key == key } ?: TsuzukiHomeTab
        return if (joined && retained == LibraryTab) NovelsTab else retained
    }

    fun library(key: String?, joined: Boolean): Tab =
        if (!joined && key == LibraryTab.key) LibraryTab else NovelsTab

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
