package tachiyomi.domain.history.anime.interactor

import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
import java.util.Date

class UpsertAnimeHistory(private val repository: AnimeHistoryRepository) {
    suspend operator fun invoke(episodeId: Long, lastSeen: Date?, watchSeconds: Long) =
        repository.upsert(episodeId, lastSeen, watchSeconds)
}

class GetAnimeContinueWatching(private val repository: AnimeHistoryRepository) {
    fun subscribe() = repository.subscribeContinueWatching()
}
