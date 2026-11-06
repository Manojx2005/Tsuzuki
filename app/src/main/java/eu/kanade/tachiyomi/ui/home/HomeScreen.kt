package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiNavBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiNavItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiRail
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.NovelsTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.tsuzuki.home.TsuzukiHomeTab
import eu.kanade.tachiyomi.ui.tsuzuki.navigation.TsuzukiNavigation
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.NavigationBar
import tachiyomi.presentation.core.components.material.NavigationRail
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    @Suppress("ConstPropertyName")
    private const val TabFadeDuration = 200

    @Suppress("ConstPropertyName")
    private const val TabNavigatorKey = "HomeTabs"

    private val TABS = listOf(
        NovelsTab,
        LibraryTab,
        UpdatesTab,
        HistoryTab,
        BrowseTab,
        MoreTab,
    )

    /** Tsuzuki bar and rail share Home, Library, Updates, Browse and More. */
    private val TSUZUKI_TABS = TsuzukiNavigation.tabs

    private val JOINED_TABS = listOf(
        NovelsTab,
        UpdatesTab,
        HistoryTab,
        BrowseTab,
        MoreTab,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val libraryPreferences = remember { Injekt.get<tachiyomi.domain.library.service.LibraryPreferences>() }
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val isJoined by libraryPreferences.joinedLibrary.collectAsState()
        val hideMangaUi by basePreferences.hideMangaUi.collectAsState()
        val isTsuzuki = TsuzukiTheme.tokens.style != TsuzukiStyle.NONE
        val tabs = when {
            isTsuzuki -> TSUZUKI_TABS
            isJoined || hideMangaUi -> JOINED_TABS
            else -> TABS
        }
        val homeTab = if (isTsuzuki) TsuzukiHomeTab else NovelsTab
        // Retain the Library tab when the outer navigator temporarily disposes Home during a cover transition.
        var retainedTsuzukiTab by rememberSaveable { mutableStateOf<String?>(null) }
        var retainedLibraryKey by rememberSaveable { mutableStateOf(NovelsTab.key) }
        val libraryTab = TsuzukiNavigation.library(retainedLibraryKey, joined = isJoined || hideMangaUi)
        val currentLibraryTab by rememberUpdatedState(libraryTab)
        val initialTab = if (isTsuzuki) {
            TsuzukiNavigation.restore(retainedTsuzukiTab, joined = isJoined || hideMangaUi)
        } else {
            homeTab
        }
        // Colour theme: the whole app takes the colours of the book you read last.
        val latestCover by remember {
            Injekt.get<tachiyomi.domain.history.interactor.GetHistory>().subscribe(query = "", limit = 1)
        }.collectAsState(initial = emptyList())
        val followBook by remember { Injekt.get<eu.kanade.domain.ui.UiPreferences>().tsuzukiFollowBook.changes() }
            .collectAsState(initial = true)
        val bookPalette = if (TsuzukiTheme.tokens.style == TsuzukiStyle.COLOUR && followBook) {
            eu.kanade.presentation.theme.tsuzuki.rememberBookPalette(latestCover.firstOrNull()?.coverData)
        } else {
            null
        }
        var shelfCover by remember { mutableStateOf<Any?>(null) }
        val shelfPalette = if (TsuzukiTheme.tokens.style == TsuzukiStyle.COLOUR && followBook) {
            eu.kanade.presentation.theme.tsuzuki.rememberBookPalette(shelfCover)
        } else {
            null
        }
        val setShelfCover: (Any?) -> Unit = remember { { shelfCover = it } }
        // Hand the tint to the novel reader so that book reads on its own colour.
        val latestMangaId = latestCover.firstOrNull()?.mangaId
        androidx.compose.runtime.LaunchedEffect(bookPalette, latestMangaId) {
            if (bookPalette != null && latestMangaId != null) {
                eu.kanade.presentation.theme.tsuzuki.BookTint.set(latestMangaId, bookPalette.background.toArgb())
            }
        }
        eu.kanade.presentation.theme.tsuzuki.ProvideBookPalette(shelfPalette ?: bookPalette) {
            val tsuzukiBackground = TsuzukiTheme.tokens.background
            val containerColor = if (isTsuzuki) {
                tsuzukiBackground.takeOrElse { MaterialTheme.colorScheme.background }
            } else {
                MaterialTheme.colorScheme.background
            }
            TabNavigator(
                tab = initialTab,
                key = TabNavigatorKey,
            ) { tabNavigator ->
                if (isTsuzuki) {
                    androidx.compose.runtime.SideEffect {
                        retainedTsuzukiTab = tabNavigator.current.key
                        if (tabNavigator.current is NovelsTab || tabNavigator.current is LibraryTab) {
                            retainedLibraryKey = tabNavigator.current.key
                        }
                    }
                }
                // Provide usable navigator to content screen
                CompositionLocalProvider(
                    LocalNavigator provides navigator,
                    eu.kanade.presentation.tsuzuki.home.LocalShelfPaletteCover provides setShelfCover,
                ) {
                    Scaffold(
                        containerColor = containerColor,
                        startBar = {
                            if (isTabletUi() && isTsuzuki) {
                                TsuzukiRail(
                                    items = tsuzukiNavItems(tabs, libraryTab),
                                    onSearch = { tabNavigator.current = NovelsTab },
                                    onSettings = { navigator.push(SettingsScreen()) },
                                )
                            } else if (isTabletUi()) {
                                NavigationRail {
                                    tabs.fastForEach {
                                        NavigationRailItem(it)
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            if (!isTabletUi()) {
                                val bottomNavVisible by produceState(initialValue = true) {
                                    showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                                }
                                AnimatedVisibility(
                                    visible = bottomNavVisible,
                                    enter = expandVertically(),
                                    exit = shrinkVertically(),
                                ) {
                                    if (isTsuzuki) {
                                        TsuzukiNavBar(items = tsuzukiNavItems(tabs, libraryTab))
                                    } else {
                                        NavigationBar {
                                            tabs.fastForEach {
                                                NavigationBarItem(it)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        contentWindowInsets = WindowInsets(0),
                    ) { contentPadding ->
                        Box(
                            modifier = Modifier
                                .padding(contentPadding)
                                .consumeWindowInsets(contentPadding),
                        ) {
                            AnimatedContent(
                                targetState = tabNavigator.current,
                                transitionSpec = {
                                    materialFadeThroughIn(
                                        initialScale = 1f,
                                        durationMillis = TabFadeDuration,
                                    ) togetherWith materialFadeThroughOut(durationMillis = TabFadeDuration)
                                },
                                label = "tabContent",
                            ) {
                                tabNavigator.saveableState(key = "currentTab", it) {
                                    it.Content()
                                }
                            }
                        }
                    }
                }

                val goToNovelsTab = { tabNavigator.current = NovelsTab }

                BackHandler(enabled = tabNavigator.current != homeTab, onBack = { tabNavigator.current = homeTab })

                LaunchedEffect(Unit) {
                    launch {
                        librarySearchEvent.receiveAsFlow().collectLatest {
                            goToNovelsTab()
                            NovelsTab.search(it)
                        }
                    }
                    launch {
                        openTabEvent.receiveAsFlow().collectLatest {
                            val target = when (it) {
                                Tab.Home -> homeTab
                                is Tab.Library -> if (isJoined || hideMangaUi) NovelsTab else LibraryTab
                                Tab.Updates -> UpdatesTab
                                Tab.History -> HistoryTab
                                is Tab.Browse -> {
                                    if (it.toExtensions) {
                                        BrowseTab.showExtension()
                                    }
                                    BrowseTab
                                }
                                is Tab.More -> MoreTab
                            }
                            tabNavigator.current =
                                if (isTsuzuki) TsuzukiNavigation.destination(it, currentLibraryTab) else target

                            if (it is Tab.Library && it.mangaIdToOpen != null) {
                                navigator.push(MangaScreen(it.mangaIdToOpen))
                            }
                            if (it is Tab.More && it.toDownloads) {
                                navigator.push(DownloadQueueScreen())
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RowScope.NavigationBarItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationBarItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
        )
    }

    @Composable
    fun NavigationRailItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationRailItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
        )
    }

    @Composable
    private fun NavigationIconItem(tab: eu.kanade.presentation.util.Tab) {
        BadgedBox(
            badge = {
                when {
                    tab is UpdatesTab -> {
                        val count by produceState(initialValue = 0) {
                            val pref = Injekt.get<LibraryPreferences>()
                            combine(
                                pref.newShowUpdatesCount.changes(),
                                pref.newUpdatesCount.changes(),
                            ) { show, count -> if (show) count else 0 }
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.notification_chapters_generic,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                    BrowseTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            Injekt.get<SourcePreferences>().extensionUpdatesCount.changes()
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.update_check_notification_ext_updates,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                }
            },
        ) {
            Icon(
                painter = tab.options.icon!!,
                contentDescription = tab.options.title,
            )
        }
    }

    /** Builds the same destinations for the phone bar and tablet rail. */
    @Composable
    private fun tsuzukiNavItems(
        tabs: List<eu.kanade.presentation.util.Tab>,
        libraryTab: eu.kanade.presentation.util.Tab,
    ): List<TsuzukiNavItem> {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val updatesCount by produceState(initialValue = 0) {
            val pref = Injekt.get<LibraryPreferences>()
            combine(pref.newShowUpdatesCount.changes(), pref.newUpdatesCount.changes()) { show, count ->
                if (show) count else 0
            }.collectLatest { value = it }
        }
        val current = tabNavigator.current
        return tabs.map { tab ->
            val destination = if (tab is NovelsTab) libraryTab else tab
            val selected = TsuzukiNavigation.selected(current) == tab
            val label = if (tab is NovelsTab) stringResource(MR.strings.label_library) else tab.options.title
            val icon = when (tab) {
                is TsuzukiHomeTab -> Icons.Outlined.GridView
                is NovelsTab -> Icons.AutoMirrored.Outlined.MenuBook
                is UpdatesTab -> Icons.Outlined.NotificationsNone
                is BrowseTab -> Icons.Outlined.Explore
                else -> Icons.Outlined.MoreHoriz
            }
            TsuzukiNavItem(
                key = tab.key,
                label = label,
                shortLabel = label.uppercase(),
                icon = icon,
                selected = selected,
                badge = if (tab is UpdatesTab) updatesCount else 0,
                onClick = {
                    if (current::class != destination::class) {
                        tabNavigator.current = destination
                    } else {
                        scope.launch { destination.onReselect(navigator) }
                    }
                },
            )
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data object Home : Tab // Tsuzuki: the Home tab, for the tablet settings rail
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
