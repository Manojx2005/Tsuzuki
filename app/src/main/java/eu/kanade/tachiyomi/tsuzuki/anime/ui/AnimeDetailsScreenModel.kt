package eu.kanade.tachiyomi.tsuzuki.anime.ui

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeEpisodeDisplay
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceAnime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
                val saved = checkNotNull(library.refresh(entry, show, fetchDetails = true))
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

    suspend fun setEpisodeDisplay(display: AnimeEpisodeDisplay, excludedGroups: Set<String>) {
        val id = state.value.saved?.id ?: return
        library.setEpisodeDisplay(id, display, excludedGroups)
        val saved = library.getAnime(id)
        mutableState.update { it.copy(saved = saved) }
    }
}
