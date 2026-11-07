package tachiyomi.domain.source.anime.repository

import tachiyomi.domain.source.anime.model.StubAnimeSource

interface AnimeSourceRepository {
    suspend fun awaitById(id: Long): StubAnimeSource?
    suspend fun awaitAll(): List<StubAnimeSource>
    suspend fun upsert(source: StubAnimeSource)
}
