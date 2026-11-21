package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Owner 2026-11-15/21 (AY-INT item 1): Library › All shows the anime too, as their own section on the All page,
 * while the Anime pill keeps the full anime library. Empty while anime is off, so the books page is unchanged.
 * More › Downloaded only narrows it like the anime library does.
 */
@Composable
internal fun rememberAllPageAnime(): List<Anime> {
    if (!TsuzukiAnime.enabled) return emptyList()
    val store = remember { Injekt.get<AnimeLibraryStore>() }
    val library by remember { store.observeLibrary() }.collectAsState(emptyList())
    val downloadedOnly by remember { Injekt.get<BasePreferences>().downloadedOnly }.collectAsState()
    val downloads by remember { Injekt.get<AnimeDownloads>().state }.collectAsState()
    return remember(library, downloadedOnly, downloads) {
        allPageAnime(library.map { it.anime }, downloadedOnly, downloads.savedAnimeIds())
    }
}

/** The anime shown on Library › All: alphabetical, and only downloaded ones while Downloaded only is on. */
internal fun allPageAnime(library: List<Anime>, downloadedOnly: Boolean, savedAnimeIds: Set<Long>): List<Anime> =
    library.asSequence()
        .filter { !downloadedOnly || it.id in savedAnimeIds }
        .distinctBy { it.id }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        .toList()

/** A full-width "Anime" heading followed by the anime tiles, inside a library grid. */
internal fun LazyGridScope.allPageAnimeSection(anime: List<Anime>, title: String, onOpen: (Long) -> Unit) {
    if (anime.isEmpty()) return
    item(key = "tsuzuki_all_anime_header", span = {
        GridItemSpan(maxLineSpan)
    }, contentType = "tsuzuki_all_anime_header") {
        TsuzukiSectionLabel(title, contentPadding = PaddingValues(top = 14.dp, bottom = 2.dp))
    }
    items(anime, key = { "tsuzuki_all_anime_${it.id}" }, contentType = { "tsuzuki_all_anime" }) { item ->
        AnimeLibraryCover(item, onOpen = { onOpen(item.id) })
    }
}
