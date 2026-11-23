package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.tsuzuki.browse.rememberAnimeBrowseSources
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadQueue
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data class AnimeCoverBadges(
    val unwatched: Long = 0,
    val downloaded: Int = 0,
    val language: String = "",
)

internal class AnimeLibraryBadges(
    private val showUnwatched: Boolean,
    private val savedCounts: Map<Long, Int>,
    private val sourceLanguages: Map<Long, String>,
) {
    fun forAnime(anime: Anime) = AnimeCoverBadges(
        unwatched = if (showUnwatched) anime.totalCount - anime.seenCount else 0,
        downloaded = savedCounts[anime.id] ?: 0,
        language = sourceLanguages[anime.source].orEmpty(),
    )
}

@Composable
internal fun rememberAnimeLibraryBadges(downloads: AnimeDownloadQueue? = null): AnimeLibraryBadges {
    val preferences = remember { Injekt.get<LibraryPreferences>() }
    val showUnwatched by preferences.unreadBadge.collectAsState()
    val showDownloaded by preferences.downloadBadge.collectAsState()
    val showLanguage by preferences.languageBadge.collectAsState()
    val savedCounts = if (showDownloaded) {
        val queue = downloads ?: remember { Injekt.get<AnimeDownloads>().state }.collectAsState().value
        remember(queue) {
            queue.entries.filter { it.status == AnimeDownloadStatus.Saved }.groupingBy { it.animeId }.eachCount()
        }
    } else {
        emptyMap()
    }
    val sourceLanguages = if (showLanguage) {
        val sources by rememberAnimeBrowseSources()
        remember(sources) { sources.associate { it.source.id to it.source.lang } }
    } else {
        emptyMap()
    }
    return remember(showUnwatched, savedCounts, sourceLanguages) {
        AnimeLibraryBadges(showUnwatched, savedCounts, sourceLanguages)
    }
}
