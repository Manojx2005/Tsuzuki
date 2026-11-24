package eu.kanade.presentation.tsuzuki.updates

import eu.kanade.presentation.updates.UpdatesUiModel
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import eu.kanade.tachiyomi.ui.updates.UpdatesFilter
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations

class TsuzukiMixedUpdatesTest {

    private val day = 86_400_000L

    private fun chapter(id: Long, at: Long) = UpdatesItem(
        update = UpdatesWithRelations(
            mangaId = id, mangaTitle = "Book $id", chapterId = id, chapterName = "c$id", scanlator = null,
            chapterUrl = "", read = false, bookmark = false, lastPageRead = 0, sourceId = 1, dateFetch = at,
            coverData = MangaCover(id, 1, true, null, 0),
        ),
        downloadStateProvider = { Download.State.NOT_DOWNLOADED },
        downloadProgressProvider = { 0 },
    )

    private fun episode(id: Long, at: Long) =
        AnimeUpdateEpisode(
            animeId = id,
            title = "Anime $id",
            episodeId = id,
            name = "Episode",
            previewUrl = null,
            timestamp = at,
        )

    private fun list(vararg items: UpdatesItem): List<UpdatesUiModel> = buildList {
        var last: LocalDate? = null
        items.sortedByDescending { it.update.dateFetch }.forEach {
            val d = LocalDate.fromEpochDays((it.update.dateFetch / day).toInt())
            if (d != last) add(UpdatesUiModel.Header(d)).also { last = d }
            add(UpdatesUiModel.Item(it))
        }
    }

    private fun describe(rows: List<UpdatesUiModel>) = rows.map {
        when (it) {
            is UpdatesUiModel.Header -> "# ${it.date}"
            is UpdatesUiModel.Item -> "c:${it.item.update.mangaTitle}"
            is UpdatesUiModel.Anime -> "e:${it.item.title}"
        }
    }

    @Test
    fun `no new episodes leaves the chapter list untouched`() {
        val chapters = list(chapter(1, day))
        assertSame(chapters, mergeAnimeIntoUpdates(chapters, emptyList(), TimeZone.UTC))
    }

    @Test
    fun `episodes fold in by time and share the day headers`() {
        val chapters = list(chapter(1, 2 * day + 10), chapter(2, day + 10))
        val episodes = listOf(episode(3, 2 * day + 20), episode(4, day + 5), episode(5, 3 * day))
        assertEquals(
            listOf(
                "# 1970-01-04",
                "e:Anime 5",
                "# 1970-01-03",
                "e:Anime 3",
                "c:Book 1",
                "# 1970-01-02",
                "c:Book 2",
                "e:Anime 4",
            ),
            describe(mergeAnimeIntoUpdates(chapters, episodes, TimeZone.UTC)),
        )
    }

    @Test
    fun `only episodes still get a header`() {
        assertEquals(
            listOf("# 1970-01-02", "e:Anime 1"),
            describe(mergeAnimeIntoUpdates(emptyList(), listOf(episode(1, day)), TimeZone.UTC)),
        )
    }

    @Test
    fun `episodes group by anime with count, newest name and time`() {
        val episodes = listOf(
            episode(1, 10).copy(animeId = 7, title = "Seven", name = "Ep 1"),
            episode(2, 30).copy(animeId = 7, title = "Seven", name = "Ep 2"),
            episode(3, 20).copy(animeId = 8, title = "Eight", name = "Ep 9"),
        )
        val groups = animeUpdateGroups(episodes)
        assertEquals(listOf(7L, 8L), groups.map { it.animeId })
        assertEquals(listOf(2, 1), groups.map { it.count })
        assertEquals(listOf("Ep 2", "Ep 9"), groups.map { it.latestName })
        assertEquals(listOf(30L, 20L), groups.map { it.latest })
    }

    @Test
    fun `anime groups sit among book groups by latest time`() {
        val anime = animeUpdateGroups(listOf(episode(5, 25), episode(6, 5)))
        val rows = mergeUpdateGroups(listOf("bookNew" to 30L, "bookOld" to 10L), { it.second }, anime)
        assertEquals(
            listOf("bookNew", "anime5", "bookOld", "anime6"),
            rows.map {
                when (it) {
                    is eu.kanade.presentation.tsuzuki.anime.MixedTimelineRow.Book -> it.item.first
                    is eu.kanade.presentation.tsuzuki.anime.MixedTimelineRow.Anime -> "anime" + it.item.animeId
                }
            },
        )
    }

    @Test
    fun `save all skips watched episodes and episodes already in the download queue`() {
        val episodes = listOf(
            episode(1, 10),
            episode(2, 20),
            episode(3, 30),
            episode(3, 30),
            episode(4, 40).copy(seen = true),
        )
        assertEquals(listOf(1L, 3L), episodesToSave(episodes, inQueue = setOf(2L)).map { it.episodeId })
        assertEquals(emptyList<Long>(), episodesToSave(episodes, inQueue = setOf(1L, 2L, 3L)).map { it.episodeId })
    }

    @Test
    fun `all counts only unwatched episodes as new`() {
        fun counts(episodes: List<AnimeUpdateEpisode>, filter: UpdatesFilter): Pair<Int, Int> {
            val anime = AnimeUpdatesSlots(
                mediaPills = null,
                airingStrip = null,
                screen = null,
                newEpisodes = episodes,
                episodesToSave = episodesToSave(episodes, inQueue = setOf(2L)),
            )
            return updatesTileCounts(unread = 3, chaptersToSave = 2, anime = anime, filter = filter)
        }

        val episodes = listOf(episode(1, 10), episode(2, 20), episode(3, 30).copy(seen = true))
        assertEquals(5 to 3, counts(episodes, UpdatesFilter.ALL))
        assertEquals(4 to 2, counts(listOf(episodes[0].copy(seen = true)) + episodes.drop(1), UpdatesFilter.ALL))
        assertEquals(3 to 2, counts(episodes, UpdatesFilter.NOVELS))
    }
}
