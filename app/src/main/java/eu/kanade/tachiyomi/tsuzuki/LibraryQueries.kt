package eu.kanade.tachiyomi.tsuzuki

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences

/**
 * A saved library query (board 08-tools/LibraryQuery): a named snapshot of the filters and sort that the stock
 * library filter sheet already sets. Nothing new is filtered; applying a query only writes the same
 * [LibraryPreferences] values, so search, paging and the stock sheet all agree with it.
 *
 * Captured: the tri-state filters (downloaded, unread, started, bookmarked, completed, content type, custom
 * interval, chapter count, no tags), the chapter-count threshold, one tri-state per tracker, the included and
 * excluded tag sets, the source-extension filters and the sort (type and direction). Only tri-states that are on
 * are stored, so a query written before a filter existed leaves that filter off.
 */
@Serializable
data class LibraryQuery(
    val id: String,
    val name: String,
    /** Key (see [LibraryQuerySnapshot]) to TriState name; absent means DISABLED. */
    val triStates: Map<String, String> = emptyMap(),
    val chapterCountThreshold: Int = DEFAULT_CHAPTER_COUNT_THRESHOLD,
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    val filterExtensions: Set<String> = emptySet(),
    val excludedExtensions: Set<String> = emptySet(),
    /** [LibrarySort.serialize] form, for example LAST_READ,DESCENDING. */
    val sort: String = LibrarySort.default.serialize(),
) {
    /** The filters and sort without the identity, used to tell which query is currently applied. */
    fun content(): LibraryQuery = copy(id = "", name = "")

    fun triState(key: String): TriState =
        triStates[key]?.let { name -> TriState.entries.firstOrNull { it.name == name } } ?: TriState.DISABLED

    companion object {
        const val DEFAULT_CHAPTER_COUNT_THRESHOLD = 10
    }
}

/** Keys of the tri-state filters in [LibraryQuery.triStates]; tracker filters are tracker:ID. */
object LibraryQuerySnapshot {
    const val DOWNLOADED = "downloaded"
    const val UNREAD = "unread"
    const val STARTED = "started"
    const val BOOKMARKED = "bookmarked"
    const val COMPLETED = "completed"
    const val NOVEL = "novel"
    const val INTERVAL_CUSTOM = "intervalCustom"
    const val CHAPTER_COUNT = "chapterCount"
    const val NO_TAGS = "noTags"
    private const val TRACKER_PREFIX = "tracker:"

    fun trackerKey(id: Int) = "$TRACKER_PREFIX$id"

    private fun fixed(p: LibraryPreferences): List<Pair<String, Preference<TriState>>> = listOf(
        DOWNLOADED to p.filterDownloaded,
        UNREAD to p.filterUnread,
        STARTED to p.filterStarted,
        BOOKMARKED to p.filterBookmarked,
        COMPLETED to p.filterCompleted,
        NOVEL to p.filterNovel(),
        INTERVAL_CUSTOM to p.filterIntervalCustom(),
        CHAPTER_COUNT to p.filterChapterCount(),
        NO_TAGS to p.filterNoTags(),
    )

    private fun trackerPrefs(p: LibraryPreferences, ids: Collection<Int>) =
        ids.map { trackerKey(it) to p.filterTracking(it) }

    /** Reads the current library filters and sort into a query. */
    fun capture(p: LibraryPreferences, trackerIds: Collection<Int>, id: String = "", name: String = ""): LibraryQuery {
        val on = (fixed(p) + trackerPrefs(p, trackerIds))
            .mapNotNull { (key, pref) -> pref.get().takeIf { it != TriState.DISABLED }?.let { key to it.name } }
            .toMap()
        return LibraryQuery(
            id = id,
            name = name,
            triStates = on,
            chapterCountThreshold = p.filterChapterCountThreshold.get(),
            includedTags = p.includedTags.get(),
            excludedTags = p.excludedTags.get(),
            filterExtensions = p.filterExtensions.get(),
            excludedExtensions = p.excludedExtensions.get(),
            sort = p.sortingMode.get().serialize(),
        )
    }

    /** Writes every filter and the sort the way the stock filter sheet would. Filters not in the query turn off. */
    fun apply(p: LibraryPreferences, query: LibraryQuery, trackerIds: Collection<Int>) {
        val storedTrackers = query.triStates.keys
            .filter { it.startsWith(TRACKER_PREFIX) }
            .mapNotNull { it.removePrefix(TRACKER_PREFIX).toIntOrNull() }
        for ((key, pref) in fixed(p) + trackerPrefs(p, (trackerIds + storedTrackers).toSet())) {
            pref.set(query.triState(key))
        }
        p.filterChapterCountThreshold.set(query.chapterCountThreshold)
        p.includedTags.set(query.includedTags)
        p.excludedTags.set(query.excludedTags)
        p.filterExtensions.set(query.filterExtensions)
        p.excludedExtensions.set(query.excludedExtensions)
        p.sortingMode.set(LibrarySort.deserialize(query.sort))
    }

    /** The defaults: every filter off, sort back to [LibrarySort.default]. */
    fun clear(p: LibraryPreferences, trackerIds: Collection<Int>) =
        apply(p, LibraryQuery(id = "", name = ""), trackerIds)

    /** Emits the current content whenever any captured preference changes. */
    fun changes(p: LibraryPreferences, trackerIds: Collection<Int>): Flow<LibraryQuery> {
        val flows: List<Flow<Any?>> = (fixed(p) + trackerPrefs(p, trackerIds)).map { it.second.changes() } +
            listOf(
                p.filterChapterCountThreshold.changes(),
                p.includedTags.changes(),
                p.excludedTags.changes(),
                p.filterExtensions.changes(),
                p.excludedExtensions.changes(),
                p.sortingMode.changes(),
            )
        return combine(flows) { capture(p, trackerIds) }
    }
}

/** The stored list: JSON in one string preference, with the list operations the manage screen needs. */
class LibraryQueryStore(preferenceStore: PreferenceStore) {

    val preference: Preference<String> = preferenceStore.getString(KEY, "[]")

    fun all(): List<LibraryQuery> = decode(preference.get())

    fun changes(): Flow<List<LibraryQuery>> = combine(listOf(preference.changes())) { decode(preference.get()) }

    fun add(name: String, content: LibraryQuery, id: String = newId()): LibraryQuery? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val query = content.copy(id = id, name = trimmed)
        write(all() + query)
        return query
    }

    fun rename(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        write(all().map { if (it.id == id) it.copy(name = trimmed) else it })
    }

    fun delete(id: String) = write(all().filterNot { it.id == id })

    /** Moves the query one place up (delta -1) or down (+1); out-of-range moves do nothing. */
    fun move(id: String, delta: Int) = write(moved(all(), id, delta))

    private fun write(list: List<LibraryQuery>) = preference.set(encode(list))

    companion object {
        const val KEY = "tsz_library_queries"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun newId(): String = java.util.UUID.randomUUID().toString()

        fun encode(list: List<LibraryQuery>): String = json.encodeToString(list)

        /** A damaged or empty value reads as no queries rather than crashing the library. */
        fun decode(raw: String): List<LibraryQuery> = runCatching {
            if (raw.isBlank()) emptyList() else json.decodeFromString<List<LibraryQuery>>(raw)
        }.getOrDefault(emptyList())

        fun moved(list: List<LibraryQuery>, id: String, delta: Int): List<LibraryQuery> {
            val from = list.indexOfFirst { it.id == id }
            val to = from + delta
            if (from < 0 || to !in list.indices) return list
            return list.toMutableList().apply { add(to, removeAt(from)) }
        }
    }
}
