package eu.kanade.tachiyomi.tsuzuki.anime.data

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import kotlinx.coroutines.Dispatchers
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.data.subscribeToOne
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Additive anime storage. Never maps anime into manga IDs or changes reader tables. */
class AnimeLibraryStore(private val database: Database = Injekt.get()) {
    private val queries get() = database.tsuzukiAnimeQueries

    fun library() = queries.getAnimeLibrary().subscribeToList(Dispatchers.IO)

    fun mediaCounts() = queries.getAnimeMediaCounts().subscribeToOne(Dispatchers.IO)

    suspend fun get(id: Long) = queries.getAnimeById(id).awaitAsOneOrNull()

    suspend fun id(source: Long, url: String) = queries.getAnimeIdBySourceUrl(source, url).awaitAsOneOrNull()

    suspend fun add(source: Long, anime: SAnime, episodes: List<SEpisode>? = null): Long {
        require(anime.url.isNotBlank() && anime.title.isNotBlank())
        return database.transactionWithResult {
            queries.addAnimeFavorite(
                source = source,
                url = anime.url,
                title = anime.title,
                thumbnailUrl = anime.thumbnail_url,
                description = anime.description,
                genre = anime.genre,
                status = anime.status.toLong(),
                dateAdded = System.currentTimeMillis(),
            )
            val id = queries.getAnimeIdBySourceUrl(source, anime.url).awaitAsOne()
            if (episodes != null) syncEpisodesInTransaction(id, episodes)
            id
        }
    }

    suspend fun remove(id: Long) {
        queries.removeAnimeFavorite(id)
    }

    suspend fun setWatchStatus(id: Long, status: Int) {
        require(status in 0..2)
        queries.setAnimeWatchStatus(status.toLong(), id)
    }

    suspend fun syncEpisodes(id: Long, episodes: List<SEpisode>) {
        database.transaction { syncEpisodesInTransaction(id, episodes) }
    }

    private suspend fun syncEpisodesInTransaction(id: Long, episodes: List<SEpisode>) {
        val fetchedAt = System.currentTimeMillis()
        episodes.distinctBy { it.url }.forEachIndexed { index, episode ->
            require(episode.url.isNotBlank() && episode.name.isNotBlank())
            queries.upsertAnimeEpisode(
                animeId = id,
                url = episode.url,
                name = episode.name,
                scanlator = episode.scanlator,
                episodeNumber = episode.episode_number.toDouble(),
                sourceOrder = index.toLong(),
                dateUpload = episode.date_upload,
                dateFetch = fetchedAt,
            )
        }
        queries.markAnimeEpisodesFetched(id)
    }
}
