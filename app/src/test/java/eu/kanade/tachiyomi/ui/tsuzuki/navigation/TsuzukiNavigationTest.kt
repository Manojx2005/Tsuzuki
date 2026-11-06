package eu.kanade.tachiyomi.ui.tsuzuki.navigation

import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.NovelsTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.tsuzuki.home.TsuzukiHomeTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiNavigationTest {
    @Test
    fun `phone and tablet use the same five destinations in board order`() {
        TsuzukiNavigation.tabs shouldBe listOf(TsuzukiHomeTab, NovelsTab, UpdatesTab, BrowseTab, MoreTab)
    }

    @Test
    fun `both library types select Library instead of Home`() {
        TsuzukiNavigation.selected(NovelsTab) shouldBe NovelsTab
        TsuzukiNavigation.selected(LibraryTab) shouldBe NovelsTab
        TsuzukiNavigation.selected(TsuzukiHomeTab) shouldBe TsuzukiHomeTab
    }

    @Test
    fun `History shortcut still reaches History with More selected`() {
        val destination = TsuzukiNavigation.destination(HomeScreen.Tab.History)
        destination shouldBe HistoryTab
        TsuzukiNavigation.selected(destination) shouldBe MoreTab
    }

    @Test
    fun `Library requests open the existing All Books destination`() {
        TsuzukiNavigation.destination(HomeScreen.Tab.Library()) shouldBe NovelsTab
        TsuzukiNavigation.destination(HomeScreen.Tab.Library(mangaIdToOpen = 42)) shouldBe NovelsTab
    }

    @Test
    fun `retained visible and hidden tabs survive outer navigation`() {
        (TsuzukiNavigation.tabs + LibraryTab + HistoryTab).forEach {
            TsuzukiNavigation.restore(it.key) shouldBe it
        }
        TsuzukiNavigation.restore(null) shouldBe TsuzukiHomeTab
        TsuzukiNavigation.restore("removed-tab") shouldBe TsuzukiHomeTab
    }

    @Test
    fun `separate library media survives tab switches and joined mode stays combined`() {
        val retained = TsuzukiNavigation.library(LibraryTab.key, joined = false)
        retained shouldBe LibraryTab
        TsuzukiNavigation.destination(HomeScreen.Tab.Library(), retained) shouldBe LibraryTab
        TsuzukiNavigation.library(NovelsTab.key, joined = false) shouldBe NovelsTab
        TsuzukiNavigation.library(LibraryTab.key, joined = true) shouldBe NovelsTab
        TsuzukiNavigation.restore(LibraryTab.key, joined = true) shouldBe NovelsTab
        TsuzukiNavigation.restore(HistoryTab.key, joined = true) shouldBe HistoryTab
        TsuzukiNavigation.library("unknown", joined = false) shouldBe NovelsTab
    }

    @Test
    fun `other shortcut destinations remain intact`() {
        TsuzukiNavigation.destination(HomeScreen.Tab.Home) shouldBe TsuzukiHomeTab
        TsuzukiNavigation.destination(HomeScreen.Tab.Updates) shouldBe UpdatesTab
        TsuzukiNavigation.destination(HomeScreen.Tab.Browse(toExtensions = true)) shouldBe BrowseTab
        TsuzukiNavigation.destination(HomeScreen.Tab.More(toDownloads = true)) shouldBe MoreTab
    }
}
