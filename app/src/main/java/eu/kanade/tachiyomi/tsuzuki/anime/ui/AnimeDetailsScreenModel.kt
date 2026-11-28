package eu.kanade.tachiyomi.tsuzuki.anime.ui

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeEpisodeDisplay
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceAnime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUserEdits

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
    /** The stored row when the opener already has it (Library): the episodes start loading on the first frame. */
    initialSaved: Anime? = null,
) {
    private val mutableState = MutableStateFlow(AnimeDetailsState(initial, saved = initialSaved))
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
                AnimeLog.w("Could not refresh anime details", e)
                mutableState.update { it.copy(refreshFailed = true) }
            } finally {
                mutableState.update { it.copy(refreshing = false) }
            }
        }
    }

    suspend fun setFavorite(favorite: Boolean, shelves: Set<Long>) {
        // The stored row when there is one: the first frame may show the Library's entry with the user's edits.
        val display = state.value.display
        val show = library.getAnime(entry.source.id, display.url)?.toSourceAnime() ?: display
        val id = library.setFavorite(entry.source.id, show, favorite, shelves)
        val saved = library.getAnime(id)
        mutableState.update { it.copy(saved = saved) }
    }

    suspend fun setEpisodeDisplay(display: AnimeEpisodeDisplay, excludedGroups: Set<String>) {
        val id = state.value.saved?.id ?: return
        library.setEpisodeDisplay(id, display, excludedGroups)
        reloadSaved()
    }

    suspend fun setUserEdits(edits: AnimeUserEdits?) {
        val id = state.value.saved?.id ?: return
        library.setUserEdits(id, edits)
        reloadSaved()
    }

    suspend fun reloadSaved() {
        val saved = library.getAnime(entry.source.id, state.value.display.url)
        mutableState.update { it.copy(saved = saved) }
    }
}
