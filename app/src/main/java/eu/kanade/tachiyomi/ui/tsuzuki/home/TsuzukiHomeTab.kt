package eu.kanade.tachiyomi.ui.tsuzuki.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.tsuzuki.home.HomeActions
import eu.kanade.presentation.tsuzuki.home.TsuzukiHomeScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.ImportEpubScreen
import eu.kanade.tachiyomi.ui.library.NovelsTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Tsuzuki bento Home: the first tab when the Colour or Dot theme is active. */
data object TsuzukiHomeTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 10u,
            title = stringResource(MR.strings.tsz_home),
            icon = rememberVectorPainter(Icons.Outlined.GridView),
        )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val viewModel = viewModel<TsuzukiHomeViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        androidx.compose.runtime.LaunchedEffect(state.loading) {
            if (!state.loading) eu.kanade.presentation.tsuzuki.startup.StartupSignal.markHomeReady()
        }

        val actions = remember(navigator, tabNavigator, viewModel) {
            HomeActions(
                onResume = { book ->
                    scope.launch {
                        val chapter = viewModel.chapterToResume(book)
                        if (chapter != null) {
                            context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
                        } else {
                            navigator.push(MangaScreen(book.mangaId))
                        }
                    }
                },
                onOpenBook = { navigator.push(MangaScreen(it.mangaId)) },
                onAllBooks = { tabNavigator.current = NovelsTab },
                onUpdates = { tabNavigator.current = UpdatesTab },
                onHistory = { tabNavigator.current = HistoryTab },
                onSearch = { tabNavigator.current = NovelsTab },
                onBrowse = { tabNavigator.current = BrowseTab },
                onImportBackup = { navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage)) },
                onOpenEpub = { navigator.push(ImportEpubScreen()) },
                onOpenChapter = { book, chapterId ->
                    context.startActivity(ReaderActivity.newIntent(context, book.mangaId, chapterId))
                },
            )
        }

        // The nav bar lives in the Scaffold bottomBar slot, so content is already padded above it.
        TsuzukiHomeScreen(state = state, actions = actions, bottomBarOverlay = false)
    }
}
