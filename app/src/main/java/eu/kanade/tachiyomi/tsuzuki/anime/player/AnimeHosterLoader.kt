package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** One hoster's loading state (AY1; Aniyomi's `HosterState`). */
internal sealed interface AnimeHosterState {
    data object Idle : AnimeHosterState
    data object Loading : AnimeHosterState
    data class Ready(val videos: List<Video>) : AnimeHosterState
    data class Error(val cause: Throwable) : AnimeHosterState
}

internal data class AnimeHosterEntry(val hoster: Hoster, val state: AnimeHosterState) {
    /** The pseudo hoster that wraps a plain episode video list (lib 14, or a lib-16 source without hosters). */
    val isPlainList: Boolean get() = hoster.hosterName == Hoster.NO_HOSTER_LIST
}

internal const val ANIME_HOSTER_TIMEOUT_MS = 20_000L

/**
 * Loads an episode's hosters the way Aniyomi's `HosterLoader` does: every non-lazy hoster at once, each with its
 * own time limit, so one dead or hanging mirror can't fail or stall the episode; a lazy hoster waits until the
 * user picks it ([loadHoster]). [loadVideos] is the source's video list for one hoster (already `sortVideos()`-ed
 * and wrapped in `callAnimeSource`, on IO).
 */
internal class AnimeHosterLoader(
    hosters: List<Hoster>,
    private val loadVideos: suspend (Hoster) -> List<Video>,
    private val timeoutMs: Long = ANIME_HOSTER_TIMEOUT_MS,
) {
    private val mutableStates = MutableStateFlow(hosters.map { AnimeHosterEntry(it, AnimeHosterState.Idle) })
    val states: StateFlow<List<AnimeHosterEntry>> = mutableStates.asStateFlow()

    /** On Auto, a successfully resolved preferred video can start before the remaining hosters finish. */
    suspend fun loadAll(resolvePreferred: (suspend (Video) -> Video?)? = null): Pair<Video, Video>? = coroutineScope {
        val loading = launch {
            mutableStates.value.forEachIndexed { index, entry ->
                if (!entry.hoster.lazy && entry.state == AnimeHosterState.Idle) launch { load(index) }
            }
        }
        if (resolvePreferred == null) {
            loading.join()
            return@coroutineScope null
        }
        val attempted = mutableSetOf<Video>()
        var preferred: Pair<Video, Video>? = null
        states.first { entries ->
            val ready = entries.flatMap { (it.state as? AnimeHosterState.Ready)?.videos.orEmpty() }
            for (candidate in ready) {
                if (!candidate.preferred || candidate.videoUrl.isBlank() || !attempted.add(candidate)) continue
                val resolved = resolvePreferred(candidate) ?: continue
                preferred = candidate to resolved
                break
            }
            preferred != null || entries.none {
                !it.hoster.lazy && (it.state == AnimeHosterState.Idle || it.state == AnimeHosterState.Loading)
            }
        }
        if (preferred != null) loading.cancelAndJoin() else loading.join()
        preferred
    }

    /** Loads one hoster now (a lazy one the user picked, or a retry). */
    suspend fun loadHoster(index: Int) {
        if (index in mutableStates.value.indices) load(index)
    }

    private suspend fun load(index: Int) {
        val hoster = mutableStates.value[index].hoster
        set(index, AnimeHosterState.Loading)
        val started = System.currentTimeMillis()
        val result = try {
            // A hoster that brought its own list needs no request.
            val videos = hoster.videoList ?: withTimeoutOrNull(timeoutMs) { loadVideos(hoster) }
            if (videos == null) {
                val error = IllegalStateException("Server did not respond within ${timeoutMs / 1_000} s")
                AnimeLog.w("hoster ${hoster.hosterName.take(40)} timed out after $timeoutMs ms")
                AnimeHosterState.Error(error)
            } else {
                AnimeLog.i(
                    "hoster ${hoster.hosterName.take(
                        40,
                    )}: ${videos.size} video(s) (+${System.currentTimeMillis() - started} ms)",
                )
                AnimeHosterState.Ready(videos)
            }
        } catch (error: CancellationException) {
            set(index, AnimeHosterState.Idle)
            throw error
        } catch (error: Exception) {
            AnimeLog.w("hoster ${hoster.hosterName.take(40)} failed", error)
            AnimeHosterState.Error(error)
        }
        set(index, result)
    }

    private fun set(index: Int, state: AnimeHosterState) {
        mutableStates.update { list ->
            list.mapIndexed { i, entry ->
                if (i ==
                    index
                ) {
                    entry.copy(state = state)
                } else {
                    entry
                }
            }
        }
    }

    /** Every loaded video in source order: hosters in `sortHosters()` order, videos in `sortVideos()` order. */
    fun videos(): List<Video> =
        mutableStates.value.flatMap { (it.state as? AnimeHosterState.Ready)?.videos.orEmpty() }
}

/**
 * The order the player tries videos in. **On Auto (`quality <= 0`) the source decides:** its `sortHosters()` /
 * `sortVideos()` order is kept and the first video it marks `preferred` goes first (Aniyomi's `selectBestVideo`),
 * so an extension's own server/quality/sub-dub settings take effect. Only a quality the user picked in the player
 * re-sorts, by resolution, and stably, so the source order still breaks ties. Videos with a blank URL go last.
 */
internal fun animePlaybackOrder(videos: List<Video>, quality: Int): List<Video> {
    val usable = videos.filter { it.videoUrl.isNotBlank() } + videos.filter { it.videoUrl.isBlank() }
    return if (quality <= 0) {
        usable.sortedByDescending { it.preferred }
    } else {
        rankAnimeVideos(usable, quality)
    }
}
