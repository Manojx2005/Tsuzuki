package eu.kanade.tachiyomi.ui.tsuzuki.library

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.tsuzuki.library.TsuzukiLibraryQueriesScreen as QueriesContent

/** Saved library queries: manage screen reached from the library overflow menu (Tsuzuki themes only). */
class TsuzukiLibraryQueriesScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        QueriesContent(onBack = navigator::pop)
    }
}
