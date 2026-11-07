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

/** Sources and Extensions always; Migrate only while it is the open section. */
internal fun browseHeaderSections(active: BrowseSection): List<BrowseSection> =
    if (active == BrowseSection.MIGRATION) {
        listOf(BrowseSection.SOURCES, BrowseSection.EXTENSIONS, BrowseSection.MIGRATION)
    } else {
        listOf(BrowseSection.SOURCES, BrowseSection.EXTENSIONS)
    }

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
        // Migrate is opened from the overflow menu; while it is open it shows as a third, selected segment so the
        // header says where you are (with only two segments nothing was selected and the indicator collapsed).
        val sections = browseHeaderSections(active.section)
        val migrationLabel = stringResource(MR.strings.label_migration)
        TsuzukiSegments(
            labels = sections.map {
                when (it) {
                    BrowseSection.SOURCES -> stringResource(MR.strings.label_sources)
                    BrowseSection.EXTENSIONS -> extensionLabel
                    BrowseSection.MIGRATION -> migrationLabel
                }
            },
            selected = sections.indexOf(active.section),
            onClick = { segment -> onSelectPage(pages.pageIndex(active.media, sections[segment])) },
        )
    }
}
