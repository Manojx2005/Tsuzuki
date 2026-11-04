package eu.kanade.presentation.browse.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.runtime.Composable
import tachiyomi.presentation.core.components.Badge

@Composable
internal fun InLibraryBadge(enabled: Boolean) {
    // Tsuzuki themes: "IN LIBRARY" pill in the accent (board 04-browse "Catalogue")
    if (enabled && eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle()) {
        eu.kanade.presentation.tsuzuki.browse.TsuzukiInLibraryBadge()
        return
    }
    if (enabled) {
        Badge(
            imageVector = Icons.Outlined.CollectionsBookmark,
        )
    }
}
