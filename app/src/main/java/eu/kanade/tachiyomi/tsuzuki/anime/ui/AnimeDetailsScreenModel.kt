package eu.kanade.tachiyomi.tsuzuki.anime.ui

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeEpisodeDisplay
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeStreamResolver
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceAnime
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceEpisode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime

internal data class AnimeDetailsState(
    val display: SAnime,
    val saved: Anime? = null,
    val refreshing: Boolean = true,
    val refreshFailed: Boolean = false,
) {
    val episodeDisplay get() = AnimeEpisodeDisplay.fromFlags(saved?.episodeFlags ?: 0L)
}

internal class AnimeDetailsScreenModel(
    private val entry: RegisteredAnimeSource,
    private val initial: SAnime,
    private val library: AnimeLibraryStore,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(AnimeDetailsState(initial))
    val state = mutableState.asStateFlow()
    private var refreshJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            mutableState.update { it.copy(refreshing = true, refreshFailed = false) }
            try {
                val cached = library.getAnime(entry.source.id, initial.url)
                val show = cached?.toSourceAnime() ?: initial
                mutableState.update { it.copy(display = show, saved = cached) }
                val oldEpisodes = cached?.let { library.getEpisodes(it.id) }.orEmpty()
                val update = withContext(Dispatchers.IO) {
                    fetchDetails(show, oldEpisodes.map { it.toSourceEpisode() })
                }
                val saved = library.syncEpisodes(entry.source.id, update.anime, update.episodes)
                mutableState.update { it.copy(display = saved.toSourceAnime(), saved = saved) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Could not refresh anime details" }
                mutableState.update { it.copy(refreshFailed = true) }
            } finally {
                mutableState.update { it.copy(refreshing = false) }
            }
        }
    }

    suspend fun setFavorite(favorite: Boolean, shelves: Set<Long>) {
        val id = library.setFavorite(entry.source.id, state.value.display, favorite, shelves)
        val saved = library.getAnime(id)
        mutableState.update { it.copy(saved = saved) }
    }

    suspend fun setEpisodeDisplay(display: AnimeEpisodeDisplay) {
        val id = state.value.saved?.id ?: return
        library.setEpisodeDisplay(id, display)
        val saved = library.getAnime(id)
        mutableState.update { it.copy(saved = saved) }
    }

    @Suppress("DEPRECATION")
    private suspend fun fetchDetails(show: SAnime, episodes: List<SEpisode>): SAnimeEpisodeUpdate {
        val source = entry.source
        val update = if (entry.libVersion >= 17) {
            source.getAnimeEpisodeUpdate(show, episodes, fetchDetails = true, fetchEpisodes = true)
        } else {
            SAnimeEpisodeUpdate(
                source.getAnimeDetails(show),
                AnimeStreamResolver.episodes(source, entry.libVersion, show),
            )
        }
        val refreshed = show.copy().apply {
            val remote = update.anime
            title = runCatching { remote.title }.getOrNull()?.takeIf(String::isNotBlank) ?: title
            artist = remote.artist ?: artist
            author = remote.author ?: author
            status = remote.status.takeUnless { it == SAnime.UNKNOWN } ?: status
            thumbnail_url = remote.thumbnail_url ?: thumbnail_url
            background_url = remote.background_url ?: background_url
            description = remote.description ?: description
            genre = remote.genre ?: genre
            initialized = true
            if (entry.libVersion >= 17) memo = remote.memo
        }
        return SAnimeEpisodeUpdate(refreshed, update.episodes)
    }
}
