package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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

/** ANIME-FAST-START: once one hoster has a video that fits the quality, how long the others still get. */
internal const val ANIME_HOSTER_GRACE_MS = 900L

/** ANIME-FAST-START: with a chosen quality nobody has (yet), how long before the closest one plays. */
internal const val ANIME_HOSTER_CLOSEST_GRACE_MS = 3_000L

private val AnimeHosterState.isSettled: Boolean
    get() = this is AnimeHosterState.Ready || this is AnimeHosterState.Error

private fun List<AnimeHosterEntry>.readyVideos(): List<Video> =
    flatMap { (it.state as? AnimeHosterState.Ready)?.videos.orEmpty() }

/** On Auto ([quality] <= 0) any video with an address fits; otherwise only one of that resolution. */
internal fun animeVideoMatches(video: Video, quality: Int): Boolean =
    video.videoUrl.isNotBlank() && (quality <= 0 || video.animeResolution == quality)

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

    private var background: Job? = null

    /** Every non-lazy hoster at once, waiting for all of them (downloads). */
    suspend fun loadAll() = coroutineScope {
        mutableStates.value.forEachIndexed { index, entry ->
            if (!entry.hoster.lazy && entry.state == AnimeHosterState.Idle) launch { load(index) }
        }
    }

    /**
     * ANIME-FAST-START: starts every non-lazy hoster in [scope], which the player owns, so the hosters that are
     * still loading when playback starts keep loading and reach the quality sheet and the stream fallback.
     */
    fun start(scope: CoroutineScope) {
        if (background != null) return
        background = scope.launch {
            mutableStates.value.forEachIndexed { index, entry ->
                if (!entry.hoster.lazy && entry.state == AnimeHosterState.Idle) launch { load(index) }
            }
        }
    }

    /** True once every non-lazy hoster has an answer (videos, an error or a timeout), or loading was stopped. */
    fun settled(entries: List<AnimeHosterEntry> = mutableStates.value): Boolean =
        background?.isActive == false || entries.none { !it.hoster.lazy && !it.state.isSettled }

    /** Waits until [settled]; returns at once when loading was cancelled (a cancelled hoster goes back to Idle). */
    suspend fun awaitSettled() {
        background?.join()
    }

    /**
     * ANIME-FAST-START: waits until playback can start, after [start]. It returns
     * - at once with the resolved pair when a source-`preferred` video resolves ([resolvePreferred], Auto only),
     * - when every non-lazy hoster has answered (a single hoster never waits on a grace),
     * - [graceMs] after the first hoster with a video, once a video satisfies [quality] (Auto: any; a chosen
     *   quality: that resolution), so a preferred or matching one that is about to arrive can still win,
     * - [closestGraceMs] after the first hoster with a video, with whatever there is (the closest quality).
     * The source's own 20 s per-hoster limit stays the worst case.
     */
    suspend fun awaitPlayable(
        quality: Int,
        resolvePreferred: (suspend (Video) -> Video?)? = null,
        graceMs: Long = ANIME_HOSTER_GRACE_MS,
        closestGraceMs: Long = ANIME_HOSTER_CLOSEST_GRACE_MS,
    ): Pair<Video, Video>? {
        val attempted = mutableListOf<Video>()
        var preferred: Pair<Video, Video>? = null
        suspend fun check(entries: List<AnimeHosterEntry>): Boolean {
            if (resolvePreferred != null && preferred == null) {
                for (candidate in entries.readyVideos()) {
                    if (!candidate.preferred || candidate.videoUrl.isBlank()) continue
                    if (attempted.any { it === candidate }) continue
                    attempted += candidate
                    preferred = resolvePreferred(candidate)?.let { candidate to it } ?: continue
                    break
                }
            }
            return preferred != null || settled(entries)
        }
        // Phase 1: something playable, or nothing left to wait for.
        states.first { entries -> check(entries) || entries.readyVideos().any { it.videoUrl.isNotBlank() } }
        if (preferred != null || settled()) return preferred
        // Phase 2: a short grace for a preferred or matching video, then a longer one for the closest.
        val matched = withTimeoutOrNull(graceMs) { states.first { check(it) } } != null
        if (matched || states.value.readyVideos().any { animeVideoMatches(it, quality) }) return preferred
        withTimeoutOrNull((closestGraceMs - graceMs).coerceAtLeast(0)) {
            states.first { entries -> check(entries) || entries.readyVideos().any { animeVideoMatches(it, quality) } }
        }
        return preferred
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
    fun videos(): List<Video> = mutableStates.value.readyVideos()
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
