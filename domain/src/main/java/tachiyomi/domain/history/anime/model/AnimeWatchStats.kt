package tachiyomi.domain.history.anime.model

data class AnimeWatchStats(
    val weekSeconds: Long,
    val monthSeconds: Long,
    val allTimeSeconds: Long,
    val episodesSeen: Long,
    val completedAnime: Long,
) {
    val hasWatchData: Boolean get() = allTimeSeconds > 0L || episodesSeen > 0L
}
