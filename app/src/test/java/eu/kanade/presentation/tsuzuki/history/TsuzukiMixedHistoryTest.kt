package eu.kanade.presentation.tsuzuki.history

import eu.kanade.presentation.history.HistoryUiModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.anime.model.AnimeHistoryDisplay
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.MangaCover
import java.util.Date

class TsuzukiMixedHistoryTest {

    private val day = 86_400_000L

    private fun book(id: Long, at: Long, title: String = "Book $id") = HistoryWithRelations(
        id = id, chapterId = id, mangaId = id, title = title, chapterNumber = 1.0, readAt = Date(at),
        readDuration = 0, coverData = MangaCover(id, 0, true, null, 0), chapterRead = false, lastPageRead = 0,
        isNovel = true,
    )

    private fun anime(id: Long, at: Long, title: String = "Anime $id", episode: String = "Episode 1") =
        AnimeHistoryDisplay(
            animeId = id, title = title, thumbnailUrl = null, episodeId = id, episodeName = episode,
            episodeNumber = 1.0, previewUrl = null, seen = false, position = 0, duration = 0, lastSeen = Date(at),
        )

    /** The paged book list as the view model builds it: a header per day, newest first. */
    private fun bookList(vararg books: HistoryWithRelations): List<HistoryUiModel> = buildList {
        var last: LocalDate? = null
        books.sortedByDescending { it.readAt!!.time }.forEach {
            val d = LocalDate.fromEpochDays((it.readAt!!.time / day).toInt())
            if (d != last) add(HistoryUiModel.Header(d)).also { last = d }
            add(HistoryUiModel.Item(it))
        }
    }

    private fun describe(rows: List<HistoryUiModel>) = rows.map {
        when (it) {
            is HistoryUiModel.Header -> "# ${it.date}"
            is HistoryUiModel.Item -> "b:${it.item.title}"
            is HistoryUiModel.Anime -> "a:${it.item.title}"
        }
    }

    private fun merge(
        books: List<HistoryUiModel>,
        anime: List<AnimeHistoryDisplay>,
        query: String? = null,
        all: Boolean = true,
    ) =
        describe(mergeAnimeIntoHistory(books, anime, query, all, TimeZone.UTC))

    @Test
    fun `no anime leaves the book list untouched`() {
        val books = bookList(book(1, 5 * day))
        assertSame(books, mergeAnimeIntoHistory(books, emptyList(), null, true, TimeZone.UTC))
    }

    @Test
    fun `anime fold in by time and share the day headers`() {
        val books = bookList(book(1, 2 * day + 10), book(2, 1 * day + 10))
        val anime = listOf(anime(3, 2 * day + 20), anime(4, 1 * day + 5), anime(5, 3 * day))
        assertEquals(
            listOf(
                "# 1970-01-04",
                "a:Anime 5",
                "# 1970-01-03",
                "a:Anime 3",
                "b:Book 1",
                "# 1970-01-02",
                "b:Book 2",
                "a:Anime 4",
            ),
            merge(books, anime),
        )
    }

    @Test
    fun `while more book pages may follow, older anime wait for them`() {
        val books = bookList(book(1, 5 * day), book(2, 4 * day))
        val anime = listOf(anime(3, 4 * day + 1), anime(4, 2 * day))
        assertEquals(
            listOf("# 1970-01-06", "b:Book 1", "# 1970-01-05", "a:Anime 3", "b:Book 2"),
            merge(books, anime, all = false),
        )
        assertEquals(
            listOf("# 1970-01-06", "b:Book 1", "# 1970-01-05", "a:Anime 3", "b:Book 2", "# 1970-01-03", "a:Anime 4"),
            merge(books, anime, all = true),
        )
    }

    @Test
    fun `anime follow the search on title or episode name`() {
        val books = bookList(book(1, 5 * day, title = "Frieren novel"))
        val anime = listOf(
            anime(2, 4 * day, title = "Frieren"),
            anime(3, 3 * day, title = "Other", episode = "frieren special"),
        )
        val other = listOf(anime(4, 2 * day, title = "Unrelated"))
        assertEquals(
            listOf("# 1970-01-06", "b:Frieren novel", "# 1970-01-05", "a:Frieren", "# 1970-01-04", "a:Other"),
            merge(books, anime + other, query = "frieren"),
        )
    }

    @Test
    fun `only anime still get headers`() {
        assertEquals(listOf("# 1970-01-03", "a:Anime 1"), merge(emptyList(), listOf(anime(1, 2 * day))))
    }
}
