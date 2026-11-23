package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.library.components.MangaListItem
import eu.kanade.presentation.library.components.UnreadBadge
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Owner 2026-11-15/21 (AY-INT item 1): Library › All shows the anime too, as their own section on the All page,
 * while the Anime pill keeps the full anime library. Empty while anime is off, so the books page is unchanged.
 * More › Downloaded only narrows it like the anime library does.
 */
@Composable
internal fun rememberAllPageAnime(searchQuery: String? = null): List<Anime> {
    if (!TsuzukiAnime.enabled) return emptyList()
    // Only the All pill: the Novels and Manga pills share this filter and never show anime.
    val filter by remember { Injekt.get<LibraryPreferences>().filterNovel() }.collectAsState()
    if (filter != TriState.DISABLED) return emptyList()
    val store = remember { Injekt.get<AnimeLibraryStore>() }
    val library by remember { store.observeLibrary() }.collectAsState(emptyList())
    val downloadedOnly by remember { Injekt.get<BasePreferences>().downloadedOnly }.collectAsState()
    val downloads by remember { Injekt.get<AnimeDownloads>().state }.collectAsState()
    return remember(library, downloadedOnly, downloads, searchQuery) {
        allPageAnime(library.map { it.anime }, downloadedOnly, downloads.savedAnimeIds(), searchQuery)
    }
}

/**
 * The anime shown on Library › All: alphabetical, only downloaded ones while Downloaded only is on, and only
 * titles containing [searchQuery] (ignoring case) while the library is searched.
 */
internal fun allPageAnime(
    library: List<Anime>,
    downloadedOnly: Boolean,
    savedAnimeIds: Set<Long>,
    searchQuery: String? = null,
): List<Anime> {
    val query = searchQuery?.trim().orEmpty()
    return library.asSequence()
        .filter { !downloadedOnly || it.id in savedAnimeIds }
        .filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) }
        .distinctBy { it.id }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        .toList()
}

/**
 * The shelf pages for Library › All. With no books, the default shelf is hidden and there are no pages at all,
 * so an anime-only library gets the default shelf back to hold its anime section.
 */
internal fun allPageShelves(
    displayed: List<Category>,
    allCategories: List<Category>,
    hasAnime: Boolean,
): List<Category> {
    if (displayed.isNotEmpty() || !hasAnime) return displayed
    val default = allCategories.firstOrNull { it.isSystemCategory }
        ?: Category(id = Category.UNCATEGORIZED_ID, name = "", order = 0, flags = 0)
    return listOf(default)
}

/** An Anime section with the same selection as the book grid around it. */
internal fun LazyGridScope.allPageAnimeSection(
    anime: List<Anime>,
    title: String,
    selection: AnimeLibrarySelection?,
    selecting: Boolean,
) {
    if (anime.isEmpty()) return
    item(
        key = "tsuzuki_all_anime_header",
        span = { GridItemSpan(maxLineSpan) },
        contentType = "tsuzuki_all_anime_header",
    ) {
        TsuzukiSectionLabel(title, contentPadding = PaddingValues(top = 14.dp, bottom = 2.dp))
    }
    val ids = anime.map(Anime::id)
    items(anime, key = { "tsuzuki_all_anime_" + it.id }, contentType = { "tsuzuki_all_anime" }) { item ->
        AllPageAnimeItemActions(item.id, ids, selection, selecting) { open, longClick ->
            AnimeLibraryCover(
                item,
                selected = selection?.ids?.contains(item.id) == true,
                selectionMode = selecting,
                onLongClick = longClick,
                onOpen = open,
            )
        }
    }
}

internal fun LazyListScope.allPageAnimeRows(
    anime: List<Anime>,
    selection: AnimeLibrarySelection?,
    booksSelected: Boolean,
) {
    if (anime.isEmpty()) return
    item(key = "tsuzuki_all_anime_header") {
        TsuzukiSectionLabel(stringResource(MR.strings.tsz_anime_media))
    }
    val ids = anime.map(Anime::id)
    items(anime, key = { "tsuzuki_all_anime_" + it.id }, contentType = { "tsuzuki_all_anime" }) { item ->
        val selecting = booksSelected || selection?.ids?.isNotEmpty() == true
        val selected = selection?.ids?.contains(item.id) == true
        AllPageAnimeItemActions(item.id, ids, selection, selecting) { open, longClick ->
            MangaListItem(
                title = tsuzukiMenuLabel(item.title),
                onClick = open,
                onLongClick = longClick ?: {},
                badge = { UnreadBadge(item.totalCount - item.seenCount) },
                isSelected = selected,
                coverAlpha = if (selecting && !selected) 0.55f else 1f,
                cover = { MangaCover.Square(item.thumbnailUrl, modifier = it, generatedTitle = item.title) },
            )
        }
    }
}

@Composable
private fun AllPageAnimeItemActions(
    id: Long,
    visible: List<Long>,
    selection: AnimeLibrarySelection?,
    selecting: Boolean,
    content: @Composable (() -> Unit, (() -> Unit)?) -> Unit,
) {
    val openDetails = rememberAnimeDetailsOpener()
    val haptic = LocalHapticFeedback.current
    content(
        { if (selecting && selection != null) selection.toggle(id) else openDetails(id) },
        selection?.let {
            {
                it.range(id, visible)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        },
    )
}
