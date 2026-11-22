package eu.kanade.presentation.tsuzuki.anime

import tachiyomi.domain.library.model.LibrarySort

/**
 * The values Library › All can sort by, for one entry (book or anime). Times are epoch milliseconds;
 * counts are chapters for a book and episodes for an anime.
 */
internal data class MixedLibrarySortKeys(
    val title: String,
    val lastRead: Long = 0,
    val lastUpdate: Long = 0,
    val unread: Long = 0,
    val total: Long = 0,
    val latestItem: Long = 0,
    val fetchDate: Long = 0,
    val dateAdded: Long = 0,
)

internal sealed interface MixedLibraryEntry<out B, out A> {
    data class Book<B>(val item: B) : MixedLibraryEntry<B, Nothing>
    data class Anime<A>(val item: A) : MixedLibraryEntry<Nothing, A>
}

/** Ascending comparator for [type], or null when anime has no value for it (they then go after the books). */
internal fun mixedLibraryComparator(type: LibrarySort.Type): Comparator<MixedLibrarySortKeys>? = when (type) {
    LibrarySort.Type.Alphabetical -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    LibrarySort.Type.LastRead -> compareBy { it.lastRead }
    LibrarySort.Type.LastUpdate -> compareBy { it.lastUpdate }
    LibrarySort.Type.UnreadCount -> compareBy { it.unread }
    LibrarySort.Type.TotalChapters -> compareBy { it.total }
    LibrarySort.Type.LatestChapter -> compareBy { it.latestItem }
    LibrarySort.Type.ChapterFetchDate -> compareBy { it.fetchDate }
    LibrarySort.Type.DateAdded -> compareBy { it.dateAdded }
    LibrarySort.Type.TrackerMean,
    LibrarySort.Type.DownloadedChapters,
    LibrarySort.Type.SourceName,
    LibrarySort.Type.Random,
    -> null
}

/**
 * Adds anime to the All list without moving any book: [books] is already in the library's own order (with
 * its tie-breaks), anime are sorted by the same key and folded in where they belong. On an equal key the book
 * comes first. With no anime the result is exactly [books].
 */
internal fun <B, A> mergeMixedLibrary(
    books: List<B>,
    bookKeys: (B) -> MixedLibrarySortKeys,
    anime: List<A>,
    animeKeys: (A) -> MixedLibrarySortKeys,
    sort: LibrarySort,
): List<MixedLibraryEntry<B, A>> {
    val bookRows = books.map { MixedLibraryEntry.Book(it) }
    if (anime.isEmpty()) return bookRows
    val ascending = mixedLibraryComparator(sort.type)
        ?: return bookRows + anime.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { animeKeys(it).title })
            .map { MixedLibraryEntry.Anime(it) }
    val order = if (sort.isAscending) ascending else ascending.reversed()
    val sortedAnime = anime.sortedWith { a, b -> order.compare(animeKeys(a), animeKeys(b)) }
    val result = ArrayList<MixedLibraryEntry<B, A>>(books.size + anime.size)
    var i = 0
    var j = 0
    while (i < books.size || j < sortedAnime.size) {
        val takeAnime = when {
            i == books.size -> true
            j == sortedAnime.size -> false
            // Strictly before: ties keep the book first.
            else -> order.compare(animeKeys(sortedAnime[j]), bookKeys(books[i])) < 0
        }
        if (takeAnime) {
            result += MixedLibraryEntry.Anime(sortedAnime[j++])
        } else {
            result += MixedLibraryEntry.Book(books[i++])
        }
    }
    return result
}
