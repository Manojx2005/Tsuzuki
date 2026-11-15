package tachiyomi.domain.history.anime.model

import java.util.Date

data class AnimeHistoryDisplay(
    val animeId: Long,
    val title: String,
    val thumbnailUrl: String?,
    val episodeId: Long,
    val episodeName: String,
    val episodeNumber: Double,
    val previewUrl: String?,
    val seen: Boolean,
    val position: Long,
    val duration: Long,
    val lastSeen: Date,
)
