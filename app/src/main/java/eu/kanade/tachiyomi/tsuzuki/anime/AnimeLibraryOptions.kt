package eu.kanade.tachiyomi.tsuzuki.anime

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum
import tachiyomi.domain.entries.anime.model.Anime

/** AP8: the anime library's sort fields, as the book library's sort tab. */
internal enum class AnimeLibrarySort { TITLE, LAST_WATCHED, LATEST_EPISODE, UNWATCHED, DATE_ADDED, TOTAL_EPISODES }

/** The book library's display modes: comfortable grid, compact grid (title on the cover), cover only, list. */
internal enum class AnimeLibraryDisplay { GRID, COMPACT, COVER_ONLY, LIST }

internal data class AnimeLibraryOptions(
    val sort: AnimeLibrarySort = AnimeLibrarySort.TITLE,
    val descending: Boolean = false,
    val downloaded: TriState = TriState.DISABLED,
    val unwatched: TriState = TriState.DISABLED,
    val started: TriState = TriState.DISABLED,
    val completed: TriState = TriState.DISABLED,
    val tracked: TriState = TriState.DISABLED,
    val display: AnimeLibraryDisplay = AnimeLibraryDisplay.GRID,
) {
    val hasFilters: Boolean
        get() = listOf(downloaded, unwatched, started, completed, tracked).any { it != TriState.DISABLED }
}

private fun TriState.accepts(value: Boolean): Boolean = when (this) {
    TriState.DISABLED -> true
    TriState.ENABLED_IS -> value
    TriState.ENABLED_NOT -> !value
}

/**
 * Filters then sorts [anime] like the book library: [saved] holds anime with at least one saved episode.
 * Completed means every episode watched (and at least one episode); started means some, but not all, watched.
 */
internal fun applyAnimeLibraryOptions(
    anime: List<Anime>,
    options: AnimeLibraryOptions,
    saved: Set<Long>,
    tracked: Set<Long> = emptySet(),
): List<Anime> {
    val filtered = anime.filter {
        val unwatched = it.totalCount - it.seenCount
        options.downloaded.accepts(it.id in saved) &&
            options.unwatched.accepts(unwatched > 0) &&
            options.started.accepts(it.seenCount > 0 && unwatched > 0) &&
            options.completed.accepts(it.totalCount > 0 && unwatched <= 0) &&
            options.tracked.accepts(it.id in tracked)
    }
    val byTitle = compareBy<Anime, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
    val primary: Comparator<Anime> = when (options.sort) {
        AnimeLibrarySort.TITLE -> byTitle
        AnimeLibrarySort.LAST_WATCHED -> compareBy { it.lastSeen }
        AnimeLibrarySort.LATEST_EPISODE -> compareBy { it.latestUpload }
        AnimeLibrarySort.UNWATCHED -> compareBy { it.totalCount - it.seenCount }
        AnimeLibrarySort.DATE_ADDED -> compareBy { it.dateAdded }
        AnimeLibrarySort.TOTAL_EPISODES -> compareBy { it.totalCount }
    }
    val ordered = if (options.descending) primary.reversed() else primary
    return filtered.sortedWith(ordered.then(byTitle))
}

/** The saved options (one preference each, so a later field never resets the others). */
internal class AnimeLibraryOptionPreferences(store: PreferenceStore) {
    val sort = store.getEnum("tsuzuki_anime_library_sort", AnimeLibrarySort.TITLE)
    val descending = store.getBoolean("tsuzuki_anime_library_sort_desc", false)
    val downloaded = store.getEnum("tsuzuki_anime_library_filter_downloaded", TriState.DISABLED)
    val unwatched = store.getEnum("tsuzuki_anime_library_filter_unwatched", TriState.DISABLED)
    val started = store.getEnum("tsuzuki_anime_library_filter_started", TriState.DISABLED)
    val completed = store.getEnum("tsuzuki_anime_library_filter_completed", TriState.DISABLED)
    val tracked = store.getEnum("tsuzuki_anime_library_filter_tracked", TriState.DISABLED)
    val display = store.getEnum("tsuzuki_anime_library_display", AnimeLibraryDisplay.GRID)
}
