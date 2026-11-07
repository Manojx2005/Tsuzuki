package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimePlaybackCheckpoint
import eu.kanade.tachiyomi.tsuzuki.anime.AnimePlaybackProgress
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeStreamResolver
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayback
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayerBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.items.episode.model.Episode
import kotlin.math.abs

internal data class AnimePlayerState(
    val title: String = "",
    val episode: Episode? = null,
    val previous: Episode? = null,
    val next: Episode? = null,
    val position: Double = 0.0,
    val duration: Double = 0.0,
    val bufferedUntil: Double = 0.0,
    val qualities: List<Int> = emptyList(),
    val resolution: Int? = null,
    val autoplay: Boolean = true,
    val loading: Boolean = true,
    val playing: Boolean = false,
    val ended: Boolean = false,
    val failed: Boolean = false,
    val nextIn: Int? = null,
    val fallbackNotice: Int = 0,
)

internal class AnimePlayerModel(
    private val context: Context,
    private val request: AnimePlayback.Request,
    private val backend: AnimePlayerBackend,
    private val library: AnimeLibraryStore,
    preferences: PreferenceStore,
    processScope: CoroutineScope,
    restoredPosition: Double?,
    restoredPaused: Boolean,
) : ViewModel() {
    private val quality = preferences.getInt("tsuzuki_anime_player_quality", 0)
    private val autoplay = preferences.getBoolean("tsuzuki_anime_player_autoplay", true)
    private val mutableState = MutableStateFlow(AnimePlayerState(autoplay = autoplay.get()))
    val state = mutableState.asStateFlow()

    private val writes = Channel<Pair<Long, AnimePlaybackCheckpoint>>(Channel.UNLIMITED)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build(),
        )
        .setOnAudioFocusChangeListener(::onFocusChanged, Handler(Looper.getMainLooper()))
        .build()

    private var foreground = false
    private var focusRegistered = false
    private var focused = false
    private var attached = false
    private var initialized = false
    private var wantsPlayback = !restoredPaused
    private var waitingForFile = false
    private var tracker = AnimePlaybackProgress()
    private var loadingJob: Job? = null
    private var countdownJob: Job? = null
    private var cancelledCountdown = false
    private var pendingVideo: Video? = null
    private var streams: AnimeStreamQueue? = null
    private var source: RegisteredAnimeSource? = null
    private var candidates = emptyList<Video>()

    val shouldResume: Boolean get() = wantsPlayback
    var targetEpisodeId: Long? = (request as? AnimePlayback.Request.Episode)?.id
        private set

    init {
        processScope.launch {
            for ((episodeId, checkpoint) in writes) {
                try {
                    library.saveProgress(episodeId, checkpoint)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "Could not save anime playback progress" }
                }
            }
        }
        viewModelScope.launch { repeatAnimeCheckpoints(::saveProgress) }
        try {
            backend.initialize(context, playerEvents())
            initialized = true
            when (request) {
                is AnimePlayback.Request.Direct -> {
                    mutableState.update { it.copy(title = request.video.videoTitle) }
                    pendingVideo = request.video
                }
                is AnimePlayback.Request.Episode -> openEpisode(request.id, restoredPosition)
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Could not initialize anime playback" }
            fail()
        }
    }

    fun attach(surface: Surface) {
        if (!initialized) return
        backend.attach(surface)
        attached = true
        loadPending()
        syncPause()
    }

    fun resize(width: Int, height: Int) {
        if (attached) backend.resize(width, height)
    }

    fun detach() {
        if (!attached) return
        attached = false
        syncPause()
        backend.detach()
    }

    fun setForeground(visible: Boolean) {
        foreground = visible
        if (visible) {
            requestFocus()
            if (state.value.ended) startCountdown()
        } else {
            cancelCountdown()
            abandonFocus()
            saveProgress()
        }
        syncPause()
    }

    fun togglePlayback() {
        if (state.value.failed) return
        if (state.value.ended) {
            seekTo(0.0)
            wantsPlayback = true
        } else {
            wantsPlayback = !state.value.playing
        }
        if (wantsPlayback) requestFocus() else abandonFocus()
        syncPause()
    }

    fun seekBy(seconds: Int) = seekTo(state.value.position + seconds)

    fun seekTo(seconds: Double) {
        val current = state.value
        if (current.duration <= 0 || current.loading || current.failed || !seconds.isFinite()) return
        val position = seconds.coerceIn(0.0, current.duration)
        cancelCountdown()
        tracker.skipNextIncrement()
        tracker.onPosition(position)
        tracker.skipNextIncrement()
        backend.seekTo(position)
        mutableState.update { it.copy(position = position, ended = false) }
        syncPause()
    }

    fun previous() {
        state.value.previous?.let { navigateTo(it.id) }
    }

    fun next() {
        state.value.next?.let { navigateTo(it.id) }
    }

    fun setQuality(resolution: Int) {
        if (candidates.isEmpty() || (resolution == quality.get() && resolution == state.value.resolution)) return
        quality.set(resolution)
        saveProgress()
        streams = AnimeStreamQueue(candidates, resolution)
        tryNextStream()
    }

    fun setAutoplay(enabled: Boolean) {
        autoplay.set(enabled)
        mutableState.update { it.copy(autoplay = enabled) }
        cancelledCountdown = !enabled
        if (enabled) startCountdown() else cancelCountdown()
    }

    fun dismissCountdown() {
        cancelledCountdown = true
        cancelCountdown()
    }

    fun retry() {
        if (!initialized) return
        val episode = state.value.episode?.id ?: (request as? AnimePlayback.Request.Episode)?.id
        if (episode != null) {
            openEpisode(episode, state.value.position)
        } else if (request is AnimePlayback.Request.Direct) {
            mutableState.update { it.copy(loading = true, failed = false) }
            pendingVideo = request.video
            loadPending()
        }
    }

    private fun navigateTo(id: Long) {
        wantsPlayback = true
        requestFocus()
        openEpisode(id)
    }

    private fun openEpisode(id: Long, restoredPosition: Double? = null) {
        targetEpisodeId = id
        saveProgress()
        cancelCountdown()
        cancelledCountdown = false
        loadingJob?.cancel()
        waitingForFile = false
        pendingVideo = null
        streams = null
        candidates = emptyList()
        source = null
        tracker = AnimePlaybackProgress()
        mutableState.update { current ->
            AnimePlayerState(
                title = current.title,
                autoplay = current.autoplay,
                fallbackNotice = current.fallbackNotice,
            )
        }
        syncPause()
        loadingJob = viewModelScope.launch {
            try {
                val episode = checkNotNull(library.getEpisode(id))
                val anime = checkNotNull(library.getAnime(episode.animeId))
                val (previous, next) = adjacentAnimeEpisodes(library.getEpisodes(anime.id), episode.id)
                val start = restoredPosition?.takeIf { it.isFinite() && it >= 0 }
                    ?: episode.lastSecondSeen.toDouble().takeUnless { episode.seen }
                    ?: 0.0
                mutableState.update {
                    it.copy(title = anime.title, episode = episode, previous = previous, next = next, position = start)
                }
                val (entry, videos) = withContext(Dispatchers.IO) {
                    val sources = AnimeSourceManager.create(context)
                    sources.refresh()
                    val entry = checkNotNull(sources.get(anime.source))
                    entry to AnimeStreamResolver.videos(entry.source, entry.libVersion, episode.toSourceEpisode())
                }
                source = entry
                candidates = videos
                val qualities = candidates.mapNotNull(Video::animeResolution).distinct().sortedDescending()
                mutableState.update { it.copy(qualities = qualities) }
                streams = AnimeStreamQueue(candidates, quality.get())
                tryNextStream()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Could not open anime episode" }
                fail()
            }
        }
    }

    private fun tryNextStream(fallback: Boolean = false) {
        val entry = source ?: return fail()
        val queue = streams ?: return fail()
        waitingForFile = false
        pendingVideo = null
        tracker.skipNextIncrement()
        mutableState.update {
            it.copy(
                loading = true,
                failed = false,
                ended = false,
                bufferedUntil = it.position,
                fallbackNotice = it.fallbackNotice + if (fallback) 1 else 0,
            )
        }
        syncPause()
        // When called after obtaining the source, that job is about to return. Other calls replace
        // any pending resolution from an earlier quality choice.
        val oldJob = loadingJob
        loadingJob = viewModelScope.launch {
            val video = withContext(Dispatchers.IO) {
                queue.next { AnimeStreamResolver.resolve(entry.source, entry.libVersion, it) }
            }
            if (video == null) {
                fail()
            } else {
                pendingVideo = video
                loadPending()
            }
        }
        oldJob?.cancel()
    }

    private fun loadPending() {
        if (!attached) return
        val video = pendingVideo ?: return
        pendingVideo = null
        waitingForFile = true
        mutableState.update { it.copy(resolution = video.animeResolution) }
        backend.load(video, state.value.position)
    }

    private fun playerEvents() = object : AnimePlayerBackend.Listener {
        override fun onPosition(seconds: Double) = onMain {
            if (state.value.loading || state.value.ended || !seconds.isFinite() || seconds < 0) return@onMain
            if (foreground) tracker.onPosition(seconds)
            if (abs(state.value.position - seconds) >= 0.1) {
                mutableState.update { it.copy(position = seconds) }
            }
        }

        override fun onDuration(seconds: Double) = onMain {
            if (!seconds.isFinite() || seconds <= 0 || (!waitingForFile && state.value.loading)) return@onMain
            tracker.onDuration(seconds)
            mutableState.update { it.copy(duration = seconds) }
        }

        override fun onBuffered(seconds: Double) = onMain {
            if (seconds.isFinite() && !state.value.loading) {
                mutableState.update { it.copy(bufferedUntil = seconds) }
            }
        }

        override fun onSeek() = onMain { tracker.skipNextIncrement() }

        override fun onLoaded() = onMain {
            if (!waitingForFile) return@onMain
            waitingForFile = false
            mutableState.update { it.copy(loading = false) }
            syncPause()
        }

        override fun onEnded() = onMain {
            val current = state.value
            if (current.loading || current.ended || current.failed) return@onMain
            if (current.duration > 0) tracker.onPosition(current.duration)
            saveProgress()
            mutableState.update { it.copy(ended = true) }
            syncPause()
            startCountdown()
        }

        override fun onError() = onMain {
            if (state.value.failed || (!waitingForFile && state.value.loading)) return@onMain
            if (streams == null) fail() else tryNextStream(fallback = true)
        }
    }

    private fun onMain(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) { block() }
    }

    private fun fail() {
        waitingForFile = false
        mutableState.update { it.copy(loading = false, failed = true) }
        syncPause()
    }

    private fun startCountdown() {
        val current = state.value
        if (!foreground || !current.ended || !current.autoplay || current.next == null ||
            cancelledCountdown || countdownJob?.isActive == true
        ) {
            return
        }
        countdownJob = viewModelScope.launch {
            for (seconds in 5 downTo 1) {
                mutableState.update { it.copy(nextIn = seconds) }
                delay(1000)
            }
            next()
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        mutableState.update { it.copy(nextIn = null) }
    }

    private fun requestFocus() {
        if (!initialized || !foreground || !wantsPlayback || focusRegistered) return
        focused = audio?.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        focusRegistered = focused
    }

    private fun abandonFocus() {
        if (focusRegistered) audio?.abandonAudioFocusRequest(focusRequest)
        focusRegistered = false
        focused = false
        if (initialized) backend.setDucked(false)
    }

    private fun onFocusChanged(change: Int) {
        if (!foreground || !initialized) return
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                focused = true
                backend.setDucked(false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> backend.setDucked(true)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> focused = false
            AudioManager.AUDIOFOCUS_LOSS -> {
                wantsPlayback = false
                abandonFocus()
            }
        }
        syncPause()
    }

    private fun syncPause() {
        val current = state.value
        val playing = foreground && attached && focused && wantsPlayback &&
            !current.loading && !current.ended && !current.failed
        if (current.playing != playing) tracker.skipNextIncrement()
        if (initialized) backend.setPaused(!playing)
        mutableState.update { it.copy(playing = playing) }
    }

    private fun saveProgress() {
        if ((request as? AnimePlayback.Request.Episode)?.track != true) return
        val episodeId = state.value.episode?.id ?: return
        val checkpoint = tracker.checkpoint() ?: return
        writes.trySend(episodeId to checkpoint)
    }

    override fun onCleared() {
        saveProgress()
        writes.close()
        abandonFocus()
        backend.close()
    }
}
