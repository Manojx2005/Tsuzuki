package eu.kanade.tachiyomi.tsuzuki.anime

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.data.AlternativeTitlesColumnAdapter
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.History
import tachiyomi.data.Mangas
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.data.anime.AnimeCategoryRepositoryImpl
import tachiyomi.data.anime.AnimeDatabaseAdapters
import tachiyomi.data.anime.AnimeHistoryRepositoryImpl
import tachiyomi.data.anime.AnimeRepositoryImpl
import tachiyomi.data.anime.AnimeSourceRepositoryImpl
import tachiyomi.data.anime.AnimeTrackRepositoryImpl
import tachiyomi.data.anime.EpisodeRepositoryImpl
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.domain.track.anime.model.AnimeTrack
import java.util.Date

class AnimeDatabaseTest {
    @Test
    fun upgradePreservesBookRowsAndSeedsOnlyDefaultAnimeShelf() = runBlocking {
        withPreAnimeDatabase { driver ->
            driver.sql(
                """
                INSERT INTO mangas(_id, source, url, title, status, favorite, initialized, viewer,
                    chapter_flags, cover_last_modified, date_added)
                VALUES (11, 101, '/book', 'Existing novel', 1, 1, 1, 0, 0, 0, 123)
                """.trimIndent(),
            )
            driver.sql(
                """
                INSERT INTO chapters(_id, manga_id, url, name, read, bookmark, last_page_read,
                    chapter_number, source_order, date_fetch, date_upload)
                VALUES (21, 11, '/chapter', 'Existing chapter', 1, 0, 4, 2, 0, 100, 200)
                """.trimIndent(),
            )
            driver.sql("INSERT INTO history(_id, chapter_id, last_read, time_read) VALUES (31, 21, 333, 40)")
            driver.sql("INSERT INTO categories(_id, name, sort, flags, content_type) VALUES (41, 'Books', 1, 0, 0)")
            driver.sql("INSERT INTO mangas_categories(_id, manga_id, category_id) VALUES (51, 11, 41)")
            val bookTables = listOf("mangas", "chapters", "history", "categories", "mangas_categories")
            val before = bookTables.associateWith { driver.tableRows(it) }
            val oldSchema = driver.schema()

            Database.Schema.migrate(driver, PRE_ANIME_VERSION, Database.Schema.version).await()

            assertEquals(before, bookTables.associateWith { driver.tableRows(it) })
            assertTrue(driver.schema().containsAll(oldSchema))
            assertEquals(listOf(listOf("0", "", "-1", "0")), driver.tableRows("anime_categories"))
            ANIME_TABLES.filterNot { it == "anime_categories" }.forEach { table ->
                assertTrue(driver.tableRows(table).isEmpty(), table)
            }
            assertEquals(listOf(listOf("ok")), driver.rows("PRAGMA integrity_check"))
            assertTrue(driver.rows("PRAGMA foreign_key_check", 4).isEmpty())
        }
    }

    @Test
    fun upgradedSchemaMatchesFreshInstall() = runBlocking {
        withPreAnimeDatabase { upgraded ->
            Database.Schema.migrate(upgraded, PRE_ANIME_VERSION, Database.Schema.version).await()
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { fresh ->
                Database.Schema.create(fresh).await()
                assertEquals(fresh.schema(), upgraded.schema())
            }
        }
    }

    @Test
    fun repositoryRoundTripsAndAnimeDeletesCascade() = runBlocking {
        withDatabase { driver, db ->
            val animes = AnimeRepositoryImpl(db)
            val episodes = EpisodeRepositoryImpl(db)
            val history = AnimeHistoryRepositoryImpl(db)
            val shelves = AnimeCategoryRepositoryImpl(db)
            val tracks = AnimeTrackRepositoryImpl(db)
            val sources = AnimeSourceRepositoryImpl(db)
            val stub = StubAnimeSource(8, "ja", "Fixture source")
            sources.upsert(stub)
            sources.upsert(stub.copy(name = "Updated source"))
            assertEquals("Updated source", sources.awaitById(stub.id)?.name)

            val anime = Anime(
                source = stub.id,
                url = "/anime",
                title = "Fixture anime",
                genre = listOf("Adventure", "Comedy"),
                alternativeTitles = listOf("Volume 1, Part 2", "Another title"),
                favorite = true,
                updateStrategy = AnimeUpdateStrategy.ONLY_FETCH_ONCE,
                fetchType = FetchType.Seasons,
                seasonNumber = 2.0,
                memo = JsonObject(mapOf("fixture" to JsonPrimitive("anime"))),
            )
            val id = animes.insert(anime)
            assertEquals(id, animes.insert(anime))
            assertEquals(anime.copy(id = id), animes.awaitBySourceAndUrl(anime.source, anime.url))
            animes.update(anime.copy(id = id, notes = "Remember", description = "New description"))
            assertEquals("Remember", animes.awaitById(id)?.notes)
            assertEquals(id, animes.subscribeLibrary().first().single().anime.id)
            assertEquals(listOf(0L), shelves.awaitByAnimeId(id).map { it.id })

            val first = shelves.insert("First")
            val second = shelves.insert("Second")
            shelves.reorder(listOf(second, first))
            assertEquals(listOf(0L, second, first), shelves.awaitAll().map { it.id })
            shelves.setForAnime(id, setOf(second))
            val selected = shelves.awaitByAnimeId(id).single()
            shelves.update(selected.copy(name = "Renamed", flags = 2))
            assertEquals(selected.copy(name = "Renamed", flags = 2), shelves.awaitByAnimeId(id).single())
            assertTrue(runCatching { shelves.setForAnime(id, setOf(999)) }.isFailure)
            assertEquals(second, shelves.awaitByAnimeId(id).single().id)
            shelves.delete(0)
            assertEquals(0L, shelves.awaitAll().first().id)

            val episode = Episode(id, "/episode", "Episode", bookmark = true, dateFetch = 40, dateUpload = 50)
            val (episodeId, olderId) = episodes.insertAll(
                listOf(episode, episode.copy(url = "/older", name = "Older", dateFetch = 20, dateUpload = 30)),
            )
            assertEquals(episode.copy(id = episodeId), episodes.awaitByAnimeId(id).first())
            episodes.updateProgress(episodeId, seen = true, lastSecondSeen = 90, totalSeconds = 100)
            assertEquals(90L, episodes.awaitById(episodeId)?.lastSecondSeen)
            val stats = checkNotNull(animes.awaitById(id))
            assertEquals(
                listOf(2L, 1L, 2L, 50L, 40L),
                with(stats) {
                    listOf(totalCount, seenCount, bookmarkCount, latestUpload, episodeFetchedAt)
                },
            )

            history.upsert(olderId, Date(100), 9)
            history.upsert(episodeId, Date(200), 30)
            assertEquals(episodeId, history.subscribeContinueWatching().first().single().episodeId)
            assertEquals(200L, animes.awaitById(id)?.lastSeen)
            episodes.delete(episodeId)
            assertEquals(olderId, history.subscribeContinueWatching().first().single().episodeId)
            val remaining = checkNotNull(animes.awaitById(id))
            assertEquals(
                listOf(1L, 0L, 1L, 30L, 20L, 100L),
                with(remaining) {
                    listOf(totalCount, seenCount, bookmarkCount, latestUpload, episodeFetchedAt, lastSeen)
                },
            )

            val track = AnimeTrack(id, 4, 9, "Tracking", "https://example.org/anime", isPrivate = true)
            tracks.upsert(track)
            val stored = tracks.awaitByAnimeId(id).single()
            assertEquals(track.copy(id = stored.id), stored)
            tracks.upsert(track.copy(score = 7.0))
            assertEquals(stored.copy(score = 7.0), tracks.awaitByAnimeId(id).single())

            animes.delete(id)
            assertNull(animes.awaitById(id))
            assertTrue(episodes.awaitByAnimeId(id).isEmpty())
            assertTrue(tracks.awaitByAnimeId(id).isEmpty())
            assertNull(history.awaitByEpisodeId(olderId))
            assertTrue(driver.tableRows("animes_categories").isEmpty())
            assertNotNull(sources.awaitById(stub.id))
            assertTrue(driver.rows("PRAGMA foreign_key_check", 4).isEmpty())
        }
    }

    @Test
    fun historyNullDoesNotPoisonOrClearLastSeen() = runBlocking {
        withDatabase { driver, db ->
            val animes = AnimeRepositoryImpl(db)
            val episodes = EpisodeRepositoryImpl(db)
            val history = AnimeHistoryRepositoryImpl(db)
            val id = animes.insert(Anime(8, "/anime", "Fixture", favorite = true))
            val episodeId = episodes.insertAll(listOf(Episode(id, "/episode", "Episode"))).single()

            history.upsert(episodeId, null, 10)
            assertNull(history.awaitByEpisodeId(episodeId)?.lastSeen)
            assertEquals(0L, animes.awaitById(id)?.lastSeen)
            history.upsert(episodeId, Date(123), 15)
            history.upsert(episodeId, null, 20)
            assertEquals(Date(123), history.awaitByEpisodeId(episodeId)?.lastSeen)
            assertEquals(45L, history.awaitByEpisodeId(episodeId)?.timeWatched)
            driver.sql("UPDATE anime_history SET last_seen = NULL WHERE episode_id = $episodeId")
            assertEquals(123L, animes.awaitById(id)?.lastSeen)
        }
    }

    private suspend fun withPreAnimeDatabase(block: suspend (JdbcSqliteDriver) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            Database.Schema.create(driver).await()
            // No historical schema snapshot exists. Migration 36 is additive, so removing its
            // seven tables (and their attached indexes and triggers) reconstructs the prior schema.
            ANIME_TABLES.forEach { driver.sql("DROP TABLE $it") }
            driver.sql("PRAGMA user_version = $PRE_ANIME_VERSION")
            driver.sql("PRAGMA foreign_keys = ON")
            assertEquals(PRE_ANIME_VERSION + 1, Database.Schema.version)
            block(driver)
        }
    }

    private suspend fun withDatabase(block: suspend (JdbcSqliteDriver, Database) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            driver.sql("PRAGMA foreign_keys = ON")
            Database.Schema.create(driver).await()
            block(
                driver,
                Database(
                    driver = driver,
                    anime_historyAdapter = AnimeDatabaseAdapters.history,
                    animesAdapter = AnimeDatabaseAdapters.animes,
                    episodesAdapter = AnimeDatabaseAdapters.episodes,
                    chaptersAdapter = Chapters.Adapter(MemoColumnAdapter),
                    historyAdapter = History.Adapter(DateColumnAdapter),
                    mangasAdapter = Mangas.Adapter(
                        genreAdapter = StringListColumnAdapter,
                        alternative_titlesAdapter = AlternativeTitlesColumnAdapter,
                        update_strategyAdapter = UpdateStrategyColumnAdapter,
                        memoAdapter = MemoColumnAdapter,
                    ),
                ),
            )
        }
    }

    private suspend fun JdbcSqliteDriver.sql(statement: String) =
        execute(null, statement, 0).await()

    private suspend fun JdbcSqliteDriver.rows(query: String, columns: Int = 1): List<List<String?>> =
        executeQuery(
            null,
            query,
            { cursor ->
                QueryResult.Value(
                    buildList {
                        while (cursor.next().value) add(List(columns) { cursor.getString(it) })
                    },
                )
            },
            0,
        ).await()

    private suspend fun JdbcSqliteDriver.schema() =
        rows("SELECT type, name, tbl_name, sql FROM sqlite_master ORDER BY type, name", 4).map { row ->
            row.map { it?.replace(Regex("\\s+"), " ")?.trim() }
        }

    private suspend fun JdbcSqliteDriver.tableRows(table: String): List<List<String?>> {
        val columns = rows("PRAGMA table_info($table)").size
        return rows("SELECT * FROM $table ORDER BY _id", columns)
    }

    private companion object {
        // SQLDelight starts at schema version 1. Migration 35 ends at user_version 36.
        const val PRE_ANIME_VERSION = 36L
        val ANIME_TABLES = listOf(
            "animes_categories",
            "anime_sync",
            "anime_history",
            "episodes",
            "anime_categories",
            "anime_sources",
            "animes",
        )
    }
}
