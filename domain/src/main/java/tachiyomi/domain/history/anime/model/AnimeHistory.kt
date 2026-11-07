package tachiyomi.domain.history.anime.model

import java.util.Date

data class AnimeHistory(
    val episodeId: Long,
    val lastSeen: Date?,
    val timeWatched: Long,
)

data class AnimeHistoryWithRelations(
    val animeId: Long,
    val episodeId: Long,
    val lastSeen: Date,
)
