package eu.kanade.tachiyomi.ui.tsuzuki.library

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.tsuzuki.anime.library.AnimeLibraryUi

/** Shared Novels / Manga / Anime dispatch; legacy themes retain their existing tabs. */
@Composable
fun TsuzukiLibraryMediaSwitch(isManga: Boolean) {
    AnimeLibraryUi.mediaSwitch(isManga)
}
