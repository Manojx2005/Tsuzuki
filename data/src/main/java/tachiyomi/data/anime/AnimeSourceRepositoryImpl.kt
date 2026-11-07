package tachiyomi.data.anime

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import tachiyomi.data.Database
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository

class AnimeSourceRepositoryImpl(database: Database) : AnimeSourceRepository {
    private val queries = database.anime_sourcesQueries

    override suspend fun awaitById(id: Long) = queries.getById(id, ::StubAnimeSource).awaitAsOneOrNull()

    override suspend fun awaitAll() = queries.getAll(::StubAnimeSource).awaitAsList()

    override suspend fun upsert(source: StubAnimeSource) {
        queries.upsert(source.id, source.lang, source.name)
    }
}
