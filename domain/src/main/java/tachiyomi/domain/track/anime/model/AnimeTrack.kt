package tachiyomi.domain.track.anime.model

data class AnimeTrack(
    val animeId: Long,
    val syncId: Long,
    val remoteId: Long,
    val title: String,
    val remoteUrl: String,
    val id: Long = -1,
    val libraryId: Long? = null,
    val lastEpisodeSeen: Double = 0.0,
    val totalEpisodes: Long = 0,
    val status: Long = 0,
    val score: Double = 0.0,
    val startDate: Long = 0,
    val finishDate: Long = 0,
    val isPrivate: Boolean = false,
)
