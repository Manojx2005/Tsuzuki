package eu.kanade.tachiyomi.tsuzuki.anime

import tachiyomi.core.common.preference.PreferenceStore

internal fun animeSeasonGrouping(store: PreferenceStore) = store.getBoolean("tsuzuki_anime_group_seasons", true)

/** Collapse direct seasons only when their parent is a visible entry in this shelf. */
internal fun groupAnimeSeasons(entries: List<AnimeLibraryEntry>): List<AnimeLibraryEntry> {
    val byId = entries.associateBy { it.anime.id }
    return entries.filterNot { entry ->
        val parent = byId[entry.anime.parentId]?.anime ?: return@filterNot false
        parent.id != entry.anime.id && parent.source == entry.anime.source && parent.parentId !in byId
    }
}
