package eu.kanade.tachiyomi.extension.model

sealed interface LoadResult {
    data class Success(val extension: Extension.Installed) : LoadResult
    data class Untrusted(val extension: Extension.Untrusted) : LoadResult
    data class Failed(val extension: Extension.Failed) : LoadResult

    /** The package could not be read at all (not found, hidden by the NSFW setting). Nothing to show. */
    data object Error : LoadResult
}
