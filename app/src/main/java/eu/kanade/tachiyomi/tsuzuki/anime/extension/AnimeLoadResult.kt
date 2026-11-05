package eu.kanade.tachiyomi.tsuzuki.anime.extension

sealed interface AnimeLoadResult {
    data class Success(val extension: AnimeExtension.Installed) : AnimeLoadResult
    data class Untrusted(val extension: AnimeExtension.Untrusted) : AnimeLoadResult
    data object Error : AnimeLoadResult
}
