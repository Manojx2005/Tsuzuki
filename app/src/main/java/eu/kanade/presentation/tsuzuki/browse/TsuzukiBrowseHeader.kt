package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal enum class BrowseMedia { NOVELS, MANGA }

internal enum class BrowseSection { SOURCES, EXTENSIONS, MIGRATION }

internal enum class BrowsePage(val media: BrowseMedia, val section: BrowseSection) {
    NOVEL_SOURCES(BrowseMedia.NOVELS, BrowseSection.SOURCES),
    MANGA_SOURCES(BrowseMedia.MANGA, BrowseSection.SOURCES),
    NOVEL_EXTENSIONS(BrowseMedia.NOVELS, BrowseSection.EXTENSIONS),
    MANGA_EXTENSIONS(BrowseMedia.MANGA, BrowseSection.EXTENSIONS),
    NOVEL_MIGRATION(BrowseMedia.NOVELS, BrowseSection.MIGRATION),
    MANGA_MIGRATION(BrowseMedia.MANGA, BrowseSection.MIGRATION),
}

internal fun browsePages(hideManga: Boolean): List<BrowsePage> =
    BrowsePage.entries.filter { !hideManga || it.media == BrowseMedia.NOVELS }

internal fun List<BrowsePage>.pageIndex(media: BrowseMedia, section: BrowseSection): Int =
    indexOfFirst { it.media == media && it.section == section }

@Composable
internal fun TsuzukiBrowseHeader(
    pages: List<BrowsePage>,
    currentPage: Int,
    extensionBadges: List<Int?>,
    onSelectPage: (Int) -> Unit,
) {
    val active = pages[currentPage]
    val mediaOptions = if (pages.any { it.media == BrowseMedia.MANGA }) {
        listOf(
            BrowseMedia.NOVELS to stringResource(MR.strings.tsz_storage_novels),
            BrowseMedia.MANGA to stringResource(MR.strings.tsz_storage_manga),
        )
    } else {
        listOf(BrowseMedia.NOVELS to stringResource(MR.strings.tsz_storage_novels))
    }
    val extensions = stringResource(MR.strings.label_extensions)
    val extensionPage = pages.pageIndex(active.media, BrowseSection.EXTENSIONS)
    val updates = extensionBadges.getOrNull(extensionPage)
    val extensionLabel = if (updates != null && updates > 0) "$extensions · $updates" else extensions

    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.padding(horizontal = 16.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((media, label) in mediaOptions) {
                val isSelected = active.media == media
                TsuzukiPill(
                    text = label,
                    selected = isSelected,
                    modifier = Modifier.semantics { selected = isSelected },
                    onClick = { onSelectPage(pages.pageIndex(media, active.section)) },
                    role = Role.Tab,
                )
            }
        }
        TsuzukiSegments(
            labels = listOf(stringResource(MR.strings.label_sources), extensionLabel),
            selected = when (active.section) {
                BrowseSection.SOURCES -> 0
                BrowseSection.EXTENSIONS -> 1
                BrowseSection.MIGRATION -> -1
            },
            onClick = { segment ->
                val section = if (segment == 0) BrowseSection.SOURCES else BrowseSection.EXTENSIONS
                onSelectPage(pages.pageIndex(active.media, section))
            },
        )
    }
}
