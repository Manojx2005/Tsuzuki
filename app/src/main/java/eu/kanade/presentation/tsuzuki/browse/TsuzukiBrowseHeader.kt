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
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal enum class BrowseMedia { NOVELS, MANGA, ANIME }

internal enum class BrowseSection { SOURCES, EXTENSIONS, MIGRATION }

internal enum class BrowsePage(val media: BrowseMedia, val section: BrowseSection) {
    NOVEL_SOURCES(BrowseMedia.NOVELS, BrowseSection.SOURCES),
    MANGA_SOURCES(BrowseMedia.MANGA, BrowseSection.SOURCES),
    NOVEL_EXTENSIONS(BrowseMedia.NOVELS, BrowseSection.EXTENSIONS),
    MANGA_EXTENSIONS(BrowseMedia.MANGA, BrowseSection.EXTENSIONS),
    NOVEL_MIGRATION(BrowseMedia.NOVELS, BrowseSection.MIGRATION),
    MANGA_MIGRATION(BrowseMedia.MANGA, BrowseSection.MIGRATION),
    ANIME_SOURCES(BrowseMedia.ANIME, BrowseSection.SOURCES),
    ANIME_EXTENSIONS(BrowseMedia.ANIME, BrowseSection.EXTENSIONS),
    ANIME_MIGRATION(BrowseMedia.ANIME, BrowseSection.MIGRATION),
}

internal fun browsePages(
    hideManga: Boolean,
    animeEnabled: Boolean = TsuzukiAnime.enabled,
    hasAnimeRepos: Boolean = false,
    hasAnimeLibrary: Boolean = false,
): List<BrowsePage> =
    BrowsePage.entries.filter {
        when (it.media) {
            BrowseMedia.NOVELS -> true
            BrowseMedia.MANGA -> !hideManga
            BrowseMedia.ANIME -> animeEnabled && animeBrowseSectionAvailable(it.section, hasAnimeRepos, hasAnimeLibrary)
        }
    }

private fun animeBrowseSectionAvailable(
    section: BrowseSection,
    hasRepos: Boolean,
    hasLibrary: Boolean,
) = when (section) {
    BrowseSection.SOURCES -> true
    // AP-18: reachable before any repository is added (private installs, and a way to add one).
    BrowseSection.EXTENSIONS -> true
    BrowseSection.MIGRATION -> hasLibrary
}

internal fun List<BrowsePage>.pageIndex(media: BrowseMedia, section: BrowseSection): Int =
    indexOfFirst { it.media == media && it.section == section }

/** The pages the pager shows for one media: its Sources, Extensions and Migrate, in that order. */
internal fun browsePagesFor(all: List<BrowsePage>, media: BrowseMedia): List<BrowsePage> =
    BrowseSection.entries.mapNotNull { section -> all.firstOrNull { it.media == media && it.section == section } }

/** The header's segments: Sources, Extensions and Migrate, always all three. */
internal val browseHeaderSections: List<BrowseSection> = BrowseSection.entries

/**
 * Browse header in the Tsuzuki themes: the media pills choose Novels or Manga (swiping never crosses media), and
 * the segments choose Sources / Extensions / Migrate within that media.
 */
@Composable
internal fun TsuzukiBrowseHeader(
    hasManga: Boolean,
    active: BrowsePage,
    extensionBadge: Int?,
    onSelectMedia: (BrowseMedia) -> Unit,
    onSelectSection: (BrowseSection) -> Unit,
    hasAnime: Boolean = false,
    hasAnimeRepos: Boolean = false,
    hasAnimeLibrary: Boolean = false,
) {
    val mediaOptions = buildList {
        add(BrowseMedia.NOVELS to stringResource(MR.strings.tsz_storage_novels))
        if (hasManga) add(BrowseMedia.MANGA to stringResource(MR.strings.tsz_storage_manga))
        if (hasAnime && (active.section != BrowseSection.MIGRATION || hasAnimeLibrary)) {
            add(BrowseMedia.ANIME to stringResource(MR.strings.tsz_anime_media))
        }
    }
    val sections = if (active.media == BrowseMedia.ANIME) {
        browseHeaderSections.filter { animeBrowseSectionAvailable(it, hasAnimeRepos, hasAnimeLibrary) }
    } else {
        browseHeaderSections
    }
    val extensions = stringResource(MR.strings.label_extensions)
    val extensionLabel = if (extensionBadge != null &&
        extensionBadge > 0
    ) {
        "$extensions · $extensionBadge"
    } else {
        extensions
    }
    val sourcesLabel = stringResource(MR.strings.label_sources)
    val migrationLabel = stringResource(MR.strings.label_migration)

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
                    onClick = { if (!isSelected) onSelectMedia(media) },
                    role = Role.Tab,
                )
            }
        }
        TsuzukiSegments(
            labels = sections.map {
                when (it) {
                    BrowseSection.SOURCES -> sourcesLabel
                    BrowseSection.EXTENSIONS -> extensionLabel
                    BrowseSection.MIGRATION -> migrationLabel
                }
            },
            selected = sections.indexOf(active.section),
            onClick = { segment -> onSelectSection(sections[segment]) },
        )
    }
}
