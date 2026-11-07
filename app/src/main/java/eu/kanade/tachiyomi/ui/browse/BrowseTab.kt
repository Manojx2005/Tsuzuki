package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.tsuzuki.browse.BrowseMedia
import eu.kanade.presentation.tsuzuki.browse.BrowsePage
import eu.kanade.presentation.tsuzuki.browse.BrowseSection
import eu.kanade.presentation.tsuzuki.browse.TsuzukiBrowseHeader
import eu.kanade.presentation.tsuzuki.browse.browsePages
import eu.kanade.presentation.tsuzuki.browse.browsePagesFor
import eu.kanade.presentation.tsuzuki.browse.pageIndex
import eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.extension.NovelExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.extension.extensionsTab
import eu.kanade.tachiyomi.ui.browse.extension.novelExtensionsTab
import eu.kanade.tachiyomi.ui.browse.migration.sources.migrateSourceTab
import eu.kanade.tachiyomi.ui.browse.migration.sources.novelMigrateSourceTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.NovelGlobalSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.novelSourcesTab
import eu.kanade.tachiyomi.ui.browse.source.sourcesTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 4u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        val hideMangaUi = Injekt.get<BasePreferences>().hideMangaUi.get()
        navigator.push(if (hideMangaUi) NovelGlobalSearchScreen() else GlobalSearchScreen())
    }

    private val switchToExtensionTabChannel = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToExtensionTabChannel.trySend(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val hideMangaUi by basePreferences.hideMangaUi.collectAsState()
        val themed = isTsuzukiStyle()

        // Hoisted for extensions tab's search bar
        val extensionsViewModel = viewModel<ExtensionsViewModel>()
        val extensionsState by extensionsViewModel.state.collectAsState()

        val novelExtensionsViewModel = viewModel<NovelExtensionsViewModel>()
        val novelExtensionsState by novelExtensionsViewModel.state.collectAsState()

        var mangaQuery by rememberSaveable { mutableStateOf(extensionsState.searchQuery) }
        var novelQuery by rememberSaveable { mutableStateOf(novelExtensionsState.searchQuery) }
        LaunchedEffect(extensionsViewModel, novelExtensionsViewModel) {
            extensionsViewModel.search(mangaQuery)
            novelExtensionsViewModel.search(novelQuery)
        }

        val allPages = remember(hideMangaUi) { browsePages(hideMangaUi) }
        // Tsuzuki: swipes stay inside the chosen media (Sources / Extensions / Migrate); the pills switch media.
        var media by rememberSaveable { mutableStateOf(BrowseMedia.NOVELS) }
        val activeMedia = if (allPages.any { it.media == media }) media else BrowseMedia.NOVELS
        val pages = if (themed) browsePagesFor(allPages, activeMedia) else allPages
        val state = rememberPagerState { pages.size }
        val scope = rememberCoroutineScope()
        val tabs = pages.map { page ->
            when (page) {
                BrowsePage.NOVEL_SOURCES -> novelSourcesTab()
                BrowsePage.MANGA_SOURCES -> sourcesTab(extensionsState.updates) {
                    scope.launch {
                        state.animateScrollToPage(pages.pageIndex(BrowseMedia.MANGA, BrowseSection.EXTENSIONS))
                    }
                }
                BrowsePage.NOVEL_EXTENSIONS -> novelExtensionsTab(novelExtensionsViewModel)
                BrowsePage.MANGA_EXTENSIONS -> extensionsTab(extensionsViewModel)
                BrowsePage.NOVEL_MIGRATION -> novelMigrateSourceTab()
                BrowsePage.MANGA_MIGRATION -> migrateSourceTab()
            }
        }
        val novelExtensionsTabIndex = pages.indexOf(BrowsePage.NOVEL_EXTENSIONS)
        val mangaExtensionsTabIndex = pages.indexOf(BrowsePage.MANGA_EXTENSIONS)

        TabbedScreen(
            titleRes = MR.strings.browse,
            tabs = tabs,
            state = state,
            tabsHeader = if (themed) {
                { currentPage, onSelectPage ->
                    TsuzukiBrowseHeader(
                        hasManga = allPages.any { it.media == BrowseMedia.MANGA },
                        active = pages[currentPage.coerceIn(pages.indices)],
                        extensionBadge = tabs.getOrNull(pages.pageIndex(activeMedia, BrowseSection.EXTENSIONS))
                            ?.badgeNumber,
                        onSelectMedia = { media = it },
                        onSelectSection = { onSelectPage(pages.pageIndex(activeMedia, it)) },
                    )
                }
            } else {
                null
            },
            searchQuery = when (state.currentPage) {
                novelExtensionsTabIndex -> novelExtensionsState.searchQuery
                mangaExtensionsTabIndex -> extensionsState.searchQuery
                else -> null
            },
            onChangeSearchQuery = { query ->
                when (state.currentPage) {
                    novelExtensionsTabIndex -> {
                        novelQuery = query
                        novelExtensionsViewModel.search(query)
                    }
                    mangaExtensionsTabIndex -> {
                        mangaQuery = query
                        extensionsViewModel.search(query)
                    }
                }
            },
        )
        LaunchedEffect(hideMangaUi) {
            switchToExtensionTabChannel.receiveAsFlow().collectLatest {
                val target = if (hideMangaUi) BrowseMedia.NOVELS else BrowseMedia.MANGA
                if (themed) {
                    media = target
                    state.scrollToPage(browsePagesFor(allPages, target).pageIndex(target, BrowseSection.EXTENSIONS))
                } else {
                    state.scrollToPage(pages.pageIndex(target, BrowseSection.EXTENSIONS))
                }
            }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
