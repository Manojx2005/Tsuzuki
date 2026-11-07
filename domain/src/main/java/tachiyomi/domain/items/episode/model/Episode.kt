package tachiyomi.domain.items.episode.model

import kotlinx.serialization.json.JsonObject

data class Episode(
    val animeId: Long,
    val url: String,
    val name: String,
    val id: Long = -1,
    val scanlator: String? = null,
    val seen: Boolean = false,
    val bookmark: Boolean = false,
    val fillermark: Boolean = false,
    val lastSecondSeen: Long = 0,
    val totalSeconds: Long = 0,
    val episodeNumber: Double = -1.0,
    val sourceOrder: Long = 0,
    val dateFetch: Long = 0,
    val dateUpload: Long = 0,
    val summary: String? = null,
    val previewUrl: String? = null,
    val lastModifiedAt: Long = 0,
    val version: Long = 0,
    val isSyncing: Long = 0,
    val memo: JsonObject = JsonObject(emptyMap()),
)
