package eu.kanade.tachiyomi.tsuzuki.anime

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SAnimeSeasonUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.data.backup.BackupProtoReader
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupCategory
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupEntry
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupFormat
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupHistory
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupSource
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupStore
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupSummary
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupTrack
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import tachiyomi.domain.history.anime.model.AnimeWatchStats
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.domain.track.anime.model.AnimeTrack
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.Date
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class AnimeDatabaseTest {
    @Test
    fun animeBackupRoundTripsAlongsideExistingBookFields() = runBlocking {
        val fixture = ByteArrayOutputStream()
        GZIPOutputStream(fixture).use { gzip ->
            gzip.backupField(1, ProtoBuf.encodeToByteArray(BackupManga.serializer(), BackupManga(42, "/manga")))
            val novel = BackupManga(42, "/novel", isNovel = true)
            gzip.backupField(1, ProtoBuf.encodeToByteArray(BackupManga.serializer(), novel))
            withDatabase { _, db ->
                val store = AnimeBackupStore(db)
                val shelfIds = store.restoreCategories(listOf(AnimeBackupCategory("Watch later", id = 81, flags = 2)))
                val source = AnimeBackupSource("Anime catalogue", 42, "en")
                val season = store.restoreEntry(
                    AnimeBackupEntry(
                        source = 42,
                        url = "/season",
                        title = "Season",
                        seasonNumber = 2.5,
                        notes = "Keep this note",
                        alternativeTitles = listOf("Alternate"),
                        categories = listOf(81),
                        episodes = listOf(
                            AnimeBackupEpisode(
                                "/episode",
                                "Special",
                                bookmark = true,
                                resumeSeconds = 60,
                                durationSeconds = 600,
                            ),
                            AnimeBackupEpisode("/finished", "Finished", seen = true, preciseNumber = 1.123456789),
                        ),
                        history = listOf(AnimeBackupHistory("/episode", 50_000, 70)),
                        tracks = listOf(AnimeBackupTrack(service = 2, mediaId = 120, malId = 121, progress = 1.5)),
                    ),
                    shelfIds,
                    source,
                )
                val parent = store.restoreEntry(AnimeBackupEntry(42, "/show", "Show"), emptyMap(), source)
                store.restoreParents(mapOf(82L to parent), mapOf(season to 82L))
                store.export(BackupOptions(), ProtoBuf) { field, bytes -> gzip.backupField(field, bytes) }
            }
        }
        val file = fixture.toByteArray()
        val oldReader = GZIPInputStream(file.inputStream()).use {
            ProtoBuf.decodeFromByteArray(Backup.serializer(), it.readBytes())
        }
        assertEquals(listOf("/manga", "/novel"), oldReader.backupManga.map { it.url })
        assertEquals(listOf(false, true), oldReader.backupManga.map { it.isNovel })

        withDatabase { _, db ->
            val store = AnimeBackupStore(db)
            val shelfIds = store.restoreCategories(listOf(AnimeBackupCategory("Other shelf", id = 4)))
            store.restoreEntry(AnimeBackupEntry(100, "/other"), emptyMap(), null)
            val seasonId = store.restoreEntry(
                AnimeBackupEntry(
                    42,
                    "/season",
                    favorite = false,
                    categories = listOf(4),
                    episodes = listOf(AnimeBackupEpisode("/episode", "Special", resumeSeconds = 100)),
                    history = listOf(AnimeBackupHistory("/episode", 40_000, 90)),
                ),
                shelfIds,
                AnimeBackupSource("Anime catalogue", 42, "en"),
            )
            assertEquals(AnimeBackupFormat.TSUZUKI, restoreAnimeFixture(store, file))
            restoreAnimeFixture(store, file)
            val anime = AnimeRepositoryImpl(db)
            val season = checkNotNull(anime.awaitBySourceAndUrl(42, "/season"))
            assertEquals(seasonId, season.id)
            assertTrue(season.favorite)
            assertEquals(anime.awaitBySourceAndUrl(42, "/show")?.id, season.parentId)
            assertEquals(2.5, season.seasonNumber)
            assertEquals("Keep this note", season.notes)
            assertEquals(listOf("Alternate"), season.alternativeTitles)
            val shelves = AnimeCategoryRepositoryImpl(db).awaitByAnimeId(seasonId)
            assertEquals(setOf("Other shelf", "Watch later"), shelves.map { it.name }.toSet())
            val episodes = EpisodeRepositoryImpl(db).awaitByAnimeId(seasonId).associateBy { it.url }
            val episode = episodes.getValue("/episode")
            assertTrue(episode.bookmark)
            assertTrue(episodes.getValue("/finished").seen)
            assertEquals(1.123456789, episodes.getValue("/finished").episodeNumber)
            assertEquals(100L, episode.lastSecondSeen)
            assertEquals(600L, episode.totalSeconds)
            val history = checkNotNull(AnimeHistoryRepositoryImpl(db).awaitByEpisodeId(episode.id))
            assertEquals(90L, history.timeWatched)
            assertEquals(Date(50_000), history.lastSeen)
            val track = AnimeTrackRepositoryImpl(db).awaitByAnimeId(seasonId).single()
            assertEquals(121L, track.malId)
            assertEquals(1.5, track.lastEpisodeSeen)
            assertEquals("Anime catalogue", AnimeSourceRepositoryImpl(db).awaitById(42)?.name)
        }
    }

    @Test
    fun aniyomiAnimeImportsFromBothWireLayouts() = runBlocking {
        val externalEpisode = AniyomiEpisodeFixture("/episode", "Special", 61_000, 1.5f, 600_000)
        val externalEntry = AniyomiEntryFixture(42, "/season", listOf(externalEpisode), listOf(31))
        val externalShelf = AnimeBackupCategory("Aniyomi", id = 31)
        val entry = ProtoBuf.encodeToByteArray(AniyomiEntryFixture.serializer(), externalEntry)
        val shelf = ProtoBuf.encodeToByteArray(AnimeBackupCategory.serializer(), externalShelf)
        val source = ProtoBuf.encodeToByteArray(AnimeBackupSource.serializer(), AnimeBackupSource("External anime", 42))
        for (modern in listOf(true, false)) {
            val fixture = ByteArrayOutputStream()
            GZIPOutputStream(fixture).use { gzip ->
                if (modern) {
                    gzip.writeBackupVarint(500L shl 3)
                    gzip.writeBackupVarint(0)
                }
                gzip.backupField(if (modern) 501 else 3, entry)
                gzip.backupField(if (modern) 502 else 4, shelf)
                gzip.backupField(if (modern) 503 else 103, source)
            }
            withDatabase { _, db ->
                val expected = if (modern) AnimeBackupFormat.ANIYOMI else AnimeBackupFormat.LEGACY_ANIYOMI
                assertEquals(expected, restoreAnimeFixture(AnimeBackupStore(db), fixture.toByteArray()))
                val anime = checkNotNull(AnimeRepositoryImpl(db).awaitBySourceAndUrl(42, "/season"))
                val episode = EpisodeRepositoryImpl(db).awaitByAnimeId(anime.id).single()
                assertEquals(1.5, episode.episodeNumber)
                assertEquals(61L, episode.lastSecondSeen)
                assertEquals(600L, episode.totalSeconds)
                assertEquals("Aniyomi", AnimeCategoryRepositoryImpl(db).awaitByAnimeId(anime.id).single().name)
                assertEquals("External anime", AnimeSourceRepositoryImpl(db).awaitById(42)?.name)
            }
        }
    }

    @Test
    fun oldBackupOptionArraysDefaultAnimeOn() {
        assertTrue(BackupOptions.fromBooleanArray(BackupOptions().asBooleanArray().copyOf(12)).includeAnime)
        assertTrue(RestoreOptions.fromBooleanArray(RestoreOptions().asBooleanArray().copyOf(7)).includeAnime)
        assertFalse(BackupOptions.fromBooleanArray(BackupOptions(includeAnime = false).asBooleanArray()).includeAnime)
        assertFalse(RestoreOptions.fromBooleanArray(RestoreOptions(includeAnime = false).asBooleanArray()).includeAnime)
    }

    private suspend fun restoreAnimeFixture(store: AnimeBackupStore, file: ByteArray): AnimeBackupFormat? {
        val summary = AnimeBackupSummary()
        BackupProtoReader.readFields(file.inputStream()) { field, bytes -> summary.record(field, bytes, ProtoBuf) }
        store.restore(
            summary,
            ProtoBuf,
            includeEntries = true,
            includeCategories = true,
            read = { callback -> BackupProtoReader.readFields(file.inputStream(), onField = callback) },
            onEntry = { _, error -> if (error != null) throw error },
        )
        return summary.format
    }

    private fun OutputStream.backupField(field: Int, data: ByteArray) {
        writeBackupVarint((field.toLong() shl 3) or 2)
        writeBackupVarint(data.size.toLong())
        write(data)
    }

    private fun OutputStream.writeBackupVarint(value: Long) {
        var remaining = value
        while (remaining > 127) {
            write((remaining.toInt() and 127) or 128)
            remaining = remaining ushr 7
        }
        write(remaining.toInt())
    }

    @Serializable
    private data class AniyomiEntryFixture(
        @ProtoNumber(1) val source: Long,
        @ProtoNumber(2) val url: String,
        @ProtoNumber(16) val episodes: List<AniyomiEpisodeFixture>,
        @ProtoNumber(17) val categories: List<Long>,
    )

    @Serializable
    private data class AniyomiEpisodeFixture(
        @ProtoNumber(1) val url: String,
        @ProtoNumber(2) val name: String,
        @ProtoNumber(6) val position: Long,
        @ProtoNumber(9) val number: Float,
        @ProtoNumber(16) val duration: Long,
    )

    @Test
    fun animeShelfEditsKeepDefaultImplicitAndUpdateAnExistingFavorite() = runBlocking {
        withDatabase { _, db ->
            val anime = AnimeRepositoryImpl(db)
            val categories = AnimeCategoryRepositoryImpl(db)
            val store = AnimeLibraryStore(
                db,
                anime,
                categories,
                EpisodeRepositoryImpl(db),
                AnimeHistoryRepositoryImpl(db),
            )
            val existing = anime.insert(Anime(source = 8, url = "/anime", title = "Old title"))
            store.saveShelf(null, "Watching")
            val shelf = categories.awaitAll().single { !it.isSystem }
            val show = SAnime.create().apply {
                url = "/anime"
                title = "Updated title"
            }

            assertEquals(existing, store.setFavorite(8, show, true, setOf(0L, shelf.id)))
            assertEquals("Updated title", anime.awaitById(existing)?.title)
            assertEquals(setOf(shelf.id), store.observeLibrary().first().single().shelfIds)
            assertEquals(listOf(shelf.id), categories.awaitByAnimeId(existing).map { it.id })
            store.saveShelf(shelf, "On hold")
            assertEquals("On hold", categories.awaitByAnimeId(existing).single().name)
            val default = categories.awaitAll().single { it.isSystem }
            categories.update(default.copy(name = "Changed"))
            assertEquals(default, categories.awaitAll().single { it.isSystem })
            store.deleteShelf(shelf.id)
            assertEquals(setOf(0L), store.observeLibrary().first().single().shelfIds)
        }
    }

    @Test
    fun continueWatchingUsesTheMostRecentFavoriteAndAdvancesWhenItsEpisodeIsSeen() = runBlocking {
        withDatabase { _, db ->
            val anime = AnimeRepositoryImpl(db)
            val episodes = EpisodeRepositoryImpl(db)
            val history = AnimeHistoryRepositoryImpl(db)
            val store = AnimeLibraryStore(db, anime, AnimeCategoryRepositoryImpl(db), episodes, history)
            val first = anime.insert(Anime(source = 8, url = "/first", title = "First", favorite = true))
            val second = anime.insert(Anime(source = 8, url = "/second", title = "Second", favorite = true))
            val (older, latest, next) = episodes.insertAll(
                listOf(
                    Episode(first, "/older", "Older"),
                    Episode(second, "/latest", "Latest", episodeNumber = 1.0, sourceOrder = 1),
                    Episode(second, "/next", "Next", episodeNumber = 2.0, sourceOrder = 0),
                ),
            )
            history.upsert(older, Date(100), 10)
            history.upsert(latest, Date(200), 30)
            assertEquals(latest, store.observeContinueWatching().first()?.episode?.id)

            store.saveProgress(latest, AnimePlaybackCheckpoint(position = 85.0, duration = 100.0, watchSeconds = 5))
            assertEquals(next, store.observeContinueWatching().first()?.episode?.id)
            assertEquals(35L, history.awaitByEpisodeId(latest)?.timeWatched)
        }
    }

    @Test
    fun episodeSyncUpdatesSourceFieldsAndKeepsLocalStateAndOfflineEpisodes() = runBlocking {
        withDatabase { _, db ->
            val anime = AnimeRepositoryImpl(db)
            val episodes = EpisodeRepositoryImpl(db)
            val history = AnimeHistoryRepositoryImpl(db)
            val store = AnimeLibraryStore(db, anime, AnimeCategoryRepositoryImpl(db), episodes, history)
            val show = SAnime.create().apply {
                url = "/show"
                title = "Show"
            }
            fun sourceEpisode(url: String, number: Int, name: String = url) = SEpisode.create().apply {
                this.url = url
                this.name = name
                episode_number = number.toFloat()
            }
            val first = listOf("updated", "seen", "bookmarked", "offline", "started", "removed").mapIndexed { i, url ->
                sourceEpisode(url, i + 1)
            }
            val id = store.syncEpisodes(8, show, first).id
            val before = episodes.awaitByAnimeId(id).associateBy(Episode::url)
            val updated = before.getValue("updated")
            episodes.updateProgress(updated.id, seen = false, lastSecondSeen = 20, totalSeconds = 100)
            episodes.setBookmark(updated.id, true)
            store.setEpisodesSeen(setOf(before.getValue("seen").id), true)
            store.setEpisodesBookmarked(setOf(before.getValue("bookmarked").id), true)
            val started = before.getValue("started")
            episodes.updateProgress(started.id, seen = false, lastSecondSeen = 30, totalSeconds = 100)

            store.syncEpisodes(
                8,
                show,
                listOf(sourceEpisode("new", 8), sourceEpisode("updated", 1, "Edited")),
                downloadedEpisodeIds = setOf(before.getValue("offline").id),
            )
            val after = episodes.awaitByAnimeId(id).associateBy(Episode::url)
            assertEquals(setOf("updated", "new", "seen", "bookmarked", "offline", "started"), after.keys)
            assertEquals(
                updated.copy(
                    name = "Edited",
                    sourceOrder = 1,
                    lastSecondSeen = 20,
                    totalSeconds = 100,
                    bookmark = true,
                ),
                after.getValue("updated").copy(
                    lastModifiedAt = updated.lastModifiedAt,
                    version = updated.version,
                ),
            )
            assertTrue(after.getValue("new").dateFetch > 0)
        }
    }

    @Test
    fun openingASearchResultKeepsSourceMetadataAndReusesTheSavedEntry() = runBlocking {
        withDatabase { _, db ->
            val anime = AnimeRepositoryImpl(db)
            val store = AnimeLibraryStore(
                db,
                anime,
                AnimeCategoryRepositoryImpl(db),
                EpisodeRepositoryImpl(db),
                AnimeHistoryRepositoryImpl(db),
            )
            val result = SAnime.create().apply {
                url = "/result"
                title = "Anime result"
                memo = JsonObject(mapOf("source" to JsonPrimitive("metadata")))
            }
            val id = store.cacheSearchResult(8, result)
            val cached = checkNotNull(anime.awaitById(id))
            assertFalse(cached.favorite)
            assertEquals(result.memo, cached.memo)

            assertEquals(id, store.setFavorite(8, result, true, emptySet()))
            val outdated = result.copy().apply { title = "Outdated title" }
            assertEquals(id, store.cacheSearchResult(8, outdated))
            val saved = checkNotNull(anime.awaitById(id))
            assertTrue(saved.favorite)
            assertEquals(result.title, saved.title)
        }
    }

    @Test
    fun refreshDiscoversSeasonSourcesAndStoresOrderedChildrenWithoutFetchingEpisodes() = runBlocking {
        withDatabase { _, db ->
            val repository = AnimeRepositoryImpl(db)
            val store = AnimeLibraryStore(
                db,
                repository,
                AnimeCategoryRepositoryImpl(db),
                EpisodeRepositoryImpl(db),
                AnimeHistoryRepositoryImpl(db),
            )
            fun anime(url: String, number: Double = -1.0) = SAnime.create().apply {
                this.url = url
                title = url
                season_number = number
            }
            val parent = anime("/series")
            val details = parent.copy().apply { fetch_type = FetchType.Seasons }
            val season1 = anime("/season-1", 1.0)
            val season2 = anime("/season-2", 2.0)
            val movie = anime("/movie")
            val source = mockk<AnimeSource>()
            every { source.id } returns 42
            coEvery {
                source.getAnimeEpisodeUpdate(any(), emptyList(), fetchDetails = true, fetchEpisodes = false)
            } returns SAnimeEpisodeUpdate(details, emptyList())
            coEvery {
                source.getAnimeSeasonUpdate(any(), emptyList(), fetchDetails = false, fetchSeasons = true)
            } returns SAnimeSeasonUpdate(details, listOf(season2, movie, season1, season2, parent))
            val entry = RegisteredAnimeSource(source, "example.anime", "Fixture", 17.0)
            val favoriteId = store.setFavorite(42, season2, true, setOf(0L))

            val saved = checkNotNull(store.refresh(entry, parent, fetchDetails = true))
            val seasons = store.observeSeasons(saved.id).first()

            assertEquals(FetchType.Seasons, saved.fetchType)
            assertEquals(listOf("/season-1", "/season-2", "/movie"), seasons.map(Anime::url))
            assertEquals(listOf(2L, 0L, 1L), seasons.map(Anime::seasonSourceOrder))
            assertEquals(favoriteId, seasons[1].id)
            assertTrue(seasons[1].favorite)
            assertTrue(seasons.all { it.parentId == saved.id })
            coVerify(exactly = 0) { source.getAnimeEpisodeUpdate(any(), any(), any(), fetchEpisodes = true) }
        }
    }

    @Test
    fun animeMigrationCopiesDataAndReplaceOnlyRemovesTheOriginalFromTheLibrary() = runBlocking {
        withDatabase { _, db ->
            val repository = AnimeRepositoryImpl(db)
            val shelves = AnimeCategoryRepositoryImpl(db)
            val episodes = EpisodeRepositoryImpl(db)
            val tracks = AnimeTrackRepositoryImpl(db)
            val store = AnimeLibraryStore(db, repository, shelves, episodes, AnimeHistoryRepositoryImpl(db))
            fun title(path: String) = SAnime.create().apply {
                url = "/$path"
                title = path
            }
            fun release(path: String) = SEpisode.create().apply {
                url = "/$path"
                name = path
                episode_number = 1.5f
            }
            val original = title("original")
            val copy = title("copy")
            val replacement = title("replacement")
            val originalShelf = shelves.insert("Watching")
            val destinationShelf = shelves.insert("Other")
            val originalId = store.setFavorite(10, original, true, setOf(originalShelf))
            val copyId = store.syncEpisodes(11, copy, listOf(release("copy-episode"))).id
            val replacementId = store.setFavorite(12, replacement, true, setOf(destinationShelf))
            store.syncEpisodes(10, original, listOf(release("old-episode")))
            store.syncEpisodes(12, replacement, listOf(release("new-episode")))
            val originalEpisode = store.getEpisodes(originalId).single()
            episodes.updateProgress(originalEpisode.id, true, 45, 120)
            store.setEpisodeDisplay(originalId, AnimeEpisodeDisplay(), setOf("Old group"))
            store.setEpisodeDisplay(replacementId, AnimeEpisodeDisplay(), setOf("New group"))
            tracks.upsert(AnimeTrack(originalId, 2, 101, "Old binding", "https://example.com/101"))
            tracks.upsert(AnimeTrack(replacementId, 2, 102, "Existing binding", "https://example.com/102"))

            store.migrateFavorite(originalId, copyId, AnimeMigrationMode.COPY, tracks)
            assertTrue(checkNotNull(repository.awaitById(originalId)).favorite)
            assertTrue(checkNotNull(repository.awaitById(copyId)).favorite)
            assertEquals(setOf(originalShelf), store.shelfIds(copyId))
            assertEquals(setOf("Old group"), store.observeExcludedGroups(copyId).first())
            assertEquals(101L, tracks.awaitByAnimeId(copyId).single().remoteId)
            val copiedEpisode = store.getEpisodes(copyId).single()
            assertTrue(copiedEpisode.seen)
            assertEquals(45L, copiedEpisode.lastSecondSeen)
            assertEquals(120L, copiedEpisode.totalSeconds)

            store.migrateFavorite(originalId, replacementId, AnimeMigrationMode.REPLACE, tracks)
            assertFalse(checkNotNull(repository.awaitById(originalId)).favorite)
            assertTrue(checkNotNull(repository.awaitById(replacementId)).favorite)
            assertEquals(setOf(originalShelf, destinationShelf), store.shelfIds(replacementId))
            assertEquals(setOf("Old group", "New group"), store.observeExcludedGroups(replacementId).first())
            assertEquals(102L, tracks.awaitByAnimeId(replacementId).single().remoteId)
            assertTrue(store.getEpisodes(replacementId).single().seen)
            assertEquals(45L, store.getEpisodes(originalId).single().lastSecondSeen)
            assertEquals(101L, tracks.awaitByAnimeId(originalId).single().remoteId)
        }
    }

    @Test
    fun excludedGroupsFilterDetailsAndBothUpdatesListsWithoutChangingStoredEpisodes() = runBlocking {
        withDatabase { _, db ->
            val repository = AnimeRepositoryImpl(db)
            val store = AnimeLibraryStore(
                db,
                repository,
                AnimeCategoryRepositoryImpl(db),
                EpisodeRepositoryImpl(db),
                AnimeHistoryRepositoryImpl(db),
            )
            val show = SAnime.create().apply {
                url = "/show"
                title = "Fixture"
            }
            fun release(path: String, group: String?) = SEpisode.create().apply {
                url = "/$path"
                name = path
                scanlator = group
                date_upload = 100
            }
            val original = release("original", null)
            val hidden = release("hidden", "Release Team")
            val visible = release("visible", "Another Team")
            val id = store.setFavorite(42, show, true, setOf(0L))
            store.syncEpisodes(42, show, listOf(original))
            store.syncEpisodes(42, show, listOf(original, hidden, visible))
            assertEquals(setOf("hidden", "visible"), store.observeNewEpisodes(0).first().map { it.name }.toSet())

            store.setEpisodeDisplay(id, AnimeEpisodeDisplay(), setOf("release team"))
            val filtered = store.observeEpisodes(id).first()
            assertEquals(setOf("original", "visible"), filtered.items.map(Episode::name).toSet())
            assertEquals(listOf("Another Team", "release team"), filtered.groups)
            assertEquals(listOf("visible"), store.observeNewEpisodes(0).first().map { it.name })
            assertEquals(
                setOf("original", "visible"),
                store.observeAiringEpisodes(0, 200).first().map {
                    it.name
                }.toSet(),
            )
            assertEquals(3, store.getEpisodes(id).size)

            val saved = checkNotNull(repository.awaitById(id))
            assertFalse(ANIME_EXCLUDED_GROUPS in saved.toSourceAnime().memo)
            show.memo = JsonObject(mapOf(ANIME_EXCLUDED_GROUPS to JsonPrimitive("extension data")))
            store.syncEpisodes(42, show, listOf(original, hidden, visible))
            assertEquals(setOf("release team"), store.observeExcludedGroups(id).first())
            store.setEpisodeDisplay(id, AnimeEpisodeDisplay(), emptySet())
            assertEquals(3, store.observeEpisodes(id).first().items.size)
        }
    }

    @Test
    fun updatesExcludeInitialImportsAndRetainAnEpisodeAcrossRefreshes() = runBlocking {
        withDatabase { _, db ->
            val anime = AnimeRepositoryImpl(db)
            val store = AnimeLibraryStore(
                db,
                anime,
                AnimeCategoryRepositoryImpl(db),
                EpisodeRepositoryImpl(db),
                AnimeHistoryRepositoryImpl(db),
            )
            val show = SAnime.create().apply {
                url = "/updates"
                title = "Updates"
            }
            val first = SEpisode.create().apply {
                url = "/first"
                name = "First"
                memo = JsonObject(mapOf("tsuzuki:library-update" to JsonPrimitive(true)))
            }
            val next = SEpisode.create().apply {
                url = "/next"
                name = "Next"
            }
            assertFalse(store.observeHasFavorites().first())
            val id = store.setFavorite(8, show, true, emptySet())
            assertTrue(store.observeHasFavorites().first())
            val baseline = store.syncEpisodes(8, show, listOf(first))
            assertNotNull(baseline.lastUpdate)
            assertEquals(baseline.lastUpdate, anime.awaitById(id)?.lastUpdate)
            assertTrue(store.observeNewEpisodes(0).first().isEmpty())

            store.syncEpisodes(8, show, listOf(next, first))
            val update = store.observeNewEpisodes(0).first().single()
            assertEquals("Next", update.name)
            next.name = "Next edited"
            store.syncEpisodes(8, show, listOf(next, first))
            assertEquals(update.copy(name = "Next edited"), store.observeNewEpisodes(0).first().single())
            assertTrue(store.observeNewEpisodes(update.timestamp + 1).first().isEmpty())

            store.setFavorite(8, show, false, emptySet())
            assertFalse(store.observeHasFavorites().first())
            assertTrue(store.observeNewEpisodes(0).first().isEmpty())
        }
    }

    @Test
    fun airingRequiresAnActualUploadTimestampInTheDayAndAFavorite() = runBlocking {
        withDatabase { _, db ->
            val anime = AnimeRepositoryImpl(db)
            val episodes = EpisodeRepositoryImpl(db)
            val store = AnimeLibraryStore(
                db,
                anime,
                AnimeCategoryRepositoryImpl(db),
                episodes,
                AnimeHistoryRepositoryImpl(db),
            )
            val saved = anime.insert(Anime(source = 8, url = "/saved", title = "Saved", favorite = true))
            val other = anime.insert(Anime(source = 8, url = "/other", title = "Other"))
            episodes.insertAll(
                listOf(
                    Episode(saved, "/unknown", "Unknown", dateFetch = 120),
                    Episode(saved, "/yesterday", "Yesterday", dateUpload = 99),
                    Episode(saved, "/today", "Today", dateUpload = 100),
                    Episode(saved, "/today2", "Today late", dateUpload = 199),
                    Episode(saved, "/tomorrow", "Tomorrow", dateUpload = 200),
                    Episode(other, "/today", "Other", dateUpload = 150),
                ),
            )
            val airing = store.observeAiringEpisodes(100, 200).first()
            assertEquals(listOf("Today", "Today late"), airing.map { it.name })
            assertEquals(listOf(100L, 199L), airing.map { it.timestamp })
        }
    }

    @Test
    fun detailsResumesMostRecentlyPlayedUnseenEpisodeBeforeFirstUnseen() = runBlocking {
        withDatabase { _, db ->
            val anime = AnimeRepositoryImpl(db)
            val episodes = EpisodeRepositoryImpl(db)
            val history = AnimeHistoryRepositoryImpl(db)
            val store = AnimeLibraryStore(db, anime, AnimeCategoryRepositoryImpl(db), episodes, history)
            val id = anime.insert(Anime(source = 8, url = "/show", title = "Show"))
            val (first, earlier, latest) = episodes.insertAll(
                listOf(
                    Episode(id, "/1", "One", episodeNumber = 1.0),
                    Episode(id, "/2", "Two", episodeNumber = 2.0, lastSecondSeen = 60),
                    Episode(id, "/3", "Three", episodeNumber = 3.0, lastSecondSeen = 30),
                ),
            )
            history.upsert(earlier, Date(100), 10)
            history.upsert(latest, Date(200), 10)
            assertEquals(latest, store.observeEpisodes(id).first().resume?.id)
            store.setEpisodesSeen(setOf(latest), true)
            assertEquals(earlier, store.observeEpisodes(id).first().resume?.id)
            store.setEpisodesSeen(setOf(earlier), true)
            assertEquals(first, store.observeEpisodes(id).first().resume?.id)
        }
    }

    @Test
    fun detailsPersistsFiltersAndSortPerAnimeAndPreservesExistingFlags() = runBlocking {
        withDatabase { _, db ->
            val anime = AnimeRepositoryImpl(db)
            val store = AnimeLibraryStore(
                db,
                anime,
                AnimeCategoryRepositoryImpl(db),
                EpisodeRepositoryImpl(db),
                AnimeHistoryRepositoryImpl(db),
            )
            val id = anime.insert(Anime(source = 8, url = "/show", title = "Show", episodeFlags = 0x123))
            val other = anime.insert(Anime(source = 8, url = "/other", title = "Other"))
            val display = AnimeEpisodeDisplay(true, true, true, true)
            store.setEpisodeDisplay(id, display)
            val flags = checkNotNull(store.getAnime(id)).episodeFlags
            assertEquals(0x123L, flags and 0xffffL)
            assertEquals(display, AnimeEpisodeDisplay.fromFlags(flags))
            assertEquals(0L, store.getAnime(other)?.episodeFlags)
            val rows = listOf(
                Episode(id, "/1", "One", dateUpload = 80, bookmark = true),
                Episode(id, "/2", "Two", dateUpload = 20, bookmark = true),
                Episode(id, "/3", "Three", dateUpload = 10, bookmark = true, seen = true),
                Episode(id, "/4", "Four", dateUpload = 30),
            )
            assertEquals(listOf("/2", "/1"), display.apply(rows).map(Episode::url))
        }
    }

    @Test
    fun playbackMarksSeenAtEightyFivePercentAndDoesNotCountSeekDistance() {
        assertTrue(!animeEpisodeSeen(84.99, 100.0))
        assertTrue(animeEpisodeSeen(85.0, 100.0))
        assertTrue(!animeEpisodeSeen(85.0, 0.0))
        val progress = AnimePlaybackProgress()
        progress.onDuration(100.0)
        progress.onPosition(10.0)
        progress.onPosition(11.0)
        progress.skipNextIncrement()
        progress.onPosition(85.0)
        assertEquals(AnimePlaybackCheckpoint(85.0, 100.0, 1), progress.checkpoint())
        assertEquals(85.0, progress.resumePosition())
        assertNull(progress.checkpoint())
    }

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

            Database.Schema.migrate(driver, PRE_ANIME_VERSION, PRE_ANIME_VERSION + 1).await()

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
            upgraded.sql("ALTER TABLE extension_store DROP COLUMN media")
            Database.Schema.migrate(upgraded, PRE_ANIME_VERSION, PRE_TRACK_MAL_ID_VERSION).await()
            upgraded.sql(
                """
                INSERT INTO animes(_id, source, url, title, status, favorite, initialized,
                    viewer, episode_flags, cover_last_modified, date_added)
                VALUES (1, 8, '/show', 'Show', 0, 1, 1, 0, 0, 0, 0)
                """.trimIndent(),
            )
            upgraded.sql(
                """
                INSERT INTO anime_sync(anime_id, sync_id, remote_id, title, last_episode_seen,
                    total_episodes, status, score, remote_url, start_date, finish_date)
                VALUES (1, 2, 31, 'Show', 3, 12, 1, 8, 'https://anilist.co/anime/31', 0, 0)
                """.trimIndent(),
            )
            Database.Schema.migrate(upgraded, PRE_TRACK_MAL_ID_VERSION, Database.Schema.version).await()
            assertEquals(
                listOf(listOf("31", "3", null)),
                upgraded.rows("SELECT remote_id, CAST(last_episode_seen AS INTEGER), mal_id FROM anime_sync", 3),
            )
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { fresh ->
                Database.Schema.create(fresh).await()
                assertEquals(
                    fresh.schema().filter { it[2] in ANIME_TABLES || it[2] == "anime_watch_days" },
                    upgraded.schema().filter { it[2] in ANIME_TABLES || it[2] == "anime_watch_days" },
                )
            }
        }
    }

    @Test
    fun extensionStoreMigrationKeepsNovelAndMangaRowsAndAddsAnAnimeMediaType() = runBlocking {
        withPreAnimeDatabase { driver ->
            driver.sql("ALTER TABLE extension_store DROP COLUMN media")
            driver.sql(
                """
                INSERT INTO extension_store(
                    index_url, name, badge_label, signing_key, contact_website, is_legacy, is_novel
                )
                VALUES ('https://manga.example', 'Manga', 'Manga', '', '', 1, 0),
                       ('https://novels.example', 'Novels', 'Novels', '', '', 1, 1)
                """.trimIndent(),
            )

            Database.Schema.migrate(driver, PRE_ANIME_VERSION, Database.Schema.version).await()

            assertEquals(
                listOf(listOf("Manga", "0", "manga"), listOf("Novels", "1", "novel")),
                driver.rows("SELECT name, is_novel, media FROM extension_store ORDER BY name", 3),
            )
            assertEquals(listOf(listOf("ok")), driver.rows("PRAGMA integrity_check"))
        }
    }

    @Test
    fun animeExtensionStoresStaySeparateFromBookStoreOperations() = runBlocking {
        withDatabase { driver, db ->
            val stores = db.extension_storeQueries
            val animeUrl = "https://anime.example/index.min.json"
            val novelUrl = "https://novel.example/index.min.json"
            stores.insertAnimeStore(animeUrl, "Anime")
            stores.upsert(novelUrl, "Novels", "Novels", "", "", null, true, true, null)
            stores.upsert(animeUrl, "Wrong type", "Wrong type", "", "", null, true, false, null)
            stores.delete(animeUrl)

            assertEquals(
                listOf("Novels"),
                driver.rows("SELECT name FROM extension_store WHERE media = 'novel'").flatten(),
            )
            assertEquals(novelUrl, stores.getAll().awaitAsList().single().index_url)
            assertEquals(animeUrl, stores.getAnimeStores().awaitAsList().single().index_url)
            stores.deleteAnimeStore(animeUrl)
            assertTrue(stores.getAnimeStores().awaitAsList().isEmpty())
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
            tracks.upsert(track.copy(score = 7.0, malId = 120))
            val edited = stored.copy(score = 7.0, malId = 120)
            assertEquals(edited, tracks.awaitByAnimeId(id).single())
            assertEquals(edited, tracks.subscribeByAnimeId(id).first().single())

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
    fun clearingAnimeHistoryRetainsWatchTimeAndRecomputesLastSeen() = runBlocking {
        withDatabase { _, db ->
            val anime = AnimeRepositoryImpl(db)
            val episodes = EpisodeRepositoryImpl(db)
            val history = AnimeHistoryRepositoryImpl(db)
            val id = anime.insert(Anime(8, "/show", "Show"))
            val (earlier, latest) = episodes.insertAll(
                listOf(
                    Episode(id, "/1", "Earlier", lastSecondSeen = 10),
                    Episode(id, "/2", "Latest", lastSecondSeen = 20),
                ),
            )
            history.upsert(earlier, Date(100), 10)
            history.upsert(latest, Date(200), 20)
            assertEquals(listOf(latest, earlier), history.subscribeHistory().first().map { it.episodeId })

            history.removeHistory(id, latest)
            assertEquals(listOf(earlier), history.subscribeHistory().first().map { it.episodeId })
            assertEquals(20L, history.awaitByEpisodeId(latest)?.timeWatched)
            assertEquals(100L, anime.awaitById(id)?.lastSeen)

            history.removeHistory(id)
            assertTrue(history.subscribeHistory().first().isEmpty())
            assertEquals(0L, anime.awaitById(id)?.lastSeen)
            assertEquals(10L, history.awaitByEpisodeId(earlier)?.timeWatched)
            assertEquals(10L, episodes.awaitById(earlier)?.lastSecondSeen)
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

    @Test
    fun watchStatsKeepCalendarPeriodsSeparateFromLifetimeHistory() = runBlocking {
        withDatabase { _, db ->
            val anime = AnimeRepositoryImpl(db)
            val episodes = EpisodeRepositoryImpl(db)
            val history = AnimeHistoryRepositoryImpl(db)
            val today = LocalDate.of(2026, 11, 1)
            val empty = history.subscribeWatchStats(today).first()
            assertEquals(AnimeWatchStats(0, 0, 0, 0, 0), empty)
            assertFalse(empty.hasWatchData)

            val ended = anime.insert(Anime(1, "/ended", "Ended", status = SAnime.COMPLETED.toLong()))
            val airing = anime.insert(Anime(1, "/airing", "Airing", status = SAnime.ONGOING.toLong()))
            anime.insert(Anime(1, "/empty", "Empty", status = SAnime.COMPLETED.toLong()))
            val (first, last) = episodes.insertAll(
                listOf(Episode(ended, "/1", "First", seen = true), Episode(ended, "/2", "Last")),
            )
            episodes.insertAll(listOf(Episode(airing, "/1", "First", seen = true)))
            history.upsert(first, Date(1), 7_200) // A legacy total with no trustworthy per-day breakdown.
            for ((day, seconds) in listOf(
                today.minusDays(7) to 60L,
                today.minusDays(6) to 61L,
                today.minusDays(1) to 120L,
                today to 180L,
                today to 30L,
                today.plusDays(1) to 240L,
            )) {
                db.transaction {
                    history.upsert(first, Date(2), seconds)
                    history.recordWatchDay(first, day, seconds)
                }
            }
            val before = history.subscribeWatchStats(today).first()
            assertEquals(AnimeWatchStats(391, 210, 7_891, 2, 0), before)
            assertTrue(before.hasWatchData)

            episodes.updateProgress(last, seen = true, lastSecondSeen = 0, totalSeconds = 600)
            history.removeHistory(ended)
            assertEquals(AnimeWatchStats(391, 210, 7_891, 3, 1), history.subscribeWatchStats(today).first())
            anime.delete(ended)
            assertEquals(AnimeWatchStats(0, 0, 0, 1, 0), history.subscribeWatchStats(today).first())
        }
    }

    @Test
    fun watchDayMigrationPreservesUndatedHistoryAndStartsWithAnEmptyCalendar() = runBlocking {
        withDatabase { driver, db ->
            val anime = AnimeRepositoryImpl(db)
            val episodes = EpisodeRepositoryImpl(db)
            val history = AnimeHistoryRepositoryImpl(db)
            val id = anime.insert(Anime(1, "/show", "Show"))
            val episode = episodes.insertAll(listOf(Episode(id, "/1", "Episode", seen = true))).single()
            history.upsert(episode, Date(1), 750)
            val before = driver.tableRows("anime_history")
            driver.sql("DROP TABLE anime_watch_days")

            Database.Schema.migrate(driver, 39, Database.Schema.version).await()

            assertEquals(before, driver.tableRows("anime_history"))
            assertTrue(driver.rows("SELECT episode_id FROM anime_watch_days").isEmpty())
            val today = LocalDate.of(2026, 11, 1)
            assertEquals(AnimeWatchStats(0, 0, 750, 1, 0), history.subscribeWatchStats(today).first())
            assertTrue(driver.rows("PRAGMA foreign_key_check", 4).isEmpty())
        }
    }

    private suspend fun withPreAnimeDatabase(block: suspend (JdbcSqliteDriver) -> Unit) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            Database.Schema.create(driver).await()
            // No historical schema snapshot exists. Migration 36 is additive, so removing its
            // tables (and their attached indexes and triggers) reconstructs the prior schema.
            driver.sql("DROP TABLE anime_watch_days")
            ANIME_TABLES.forEach { driver.sql("DROP TABLE $it") }
            driver.sql("PRAGMA user_version = $PRE_ANIME_VERSION")
            driver.sql("PRAGMA foreign_keys = ON")
            assertTrue(Database.Schema.version >= PRE_ANIME_VERSION + 1)
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
        const val PRE_TRACK_MAL_ID_VERSION = 38L
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
