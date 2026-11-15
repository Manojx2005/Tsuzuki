package eu.kanade.tachiyomi.tsuzuki.anime.debug

import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
internal enum class AnimeDebugInstallAction {
    Install,
    Trust,
    Installed,
    Retry,
}

internal fun animeDebugInstallAction(extension: AnimeLoadResult?) =
    when (extension) {
        null -> AnimeDebugInstallAction.Install
        is AnimeLoadResult.Untrusted -> AnimeDebugInstallAction.Trust
        is AnimeLoadResult.Success -> AnimeDebugInstallAction.Installed
        AnimeLoadResult.Error -> AnimeDebugInstallAction.Retry
    }
