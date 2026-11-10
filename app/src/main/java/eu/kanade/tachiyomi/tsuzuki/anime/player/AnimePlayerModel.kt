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
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackProgressJob
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayback
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayerBackend
import eu.kanade.tachiyomi.tsuzuki.widget.refreshWatchingWidget
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
import kotlin.math.roundToLong

internal data class AnimePlayerState(
    val title: String = "",
    val episode: Episode? = null,
    val episodes: List<Episode> = emptyList(),
    val previous: Episode? = null,
    val next: Episode? = null,
    val position: Double = 0.0,
    val duration: Double = 0.0,
    val bufferedUntil: Double = 0.0,
    val qualities: List<Int> = emptyList(),
    val resolution: Int? = null,
    val autoplay: Boolean = true,
    val settings: AnimePlayerSettings = AnimePlayerSettings(),
    val tracks: List<AnimePlayerTrack> = emptyList(),
    val loading: Boolean = true,
    val playing: Boolean = false,
    val ended: Boolean = false,
    val failed: Boolean = false,
    val nextIn: Int? = null,
    val fallbackNotice: Int = 0,
    val aniSkipSegments: List<AniSkipSegment> = emptyList(),
) {
    val activeAniSkip: AniSkipSegment?
        get() = if (loading || failed || ended || settings.aniSkipMode == AniSkipMode.OFF) {
            null
        } else {
            aniSkipSegments.atPosition(position)
        }
}

internal class AnimePlayerModel(
    private val context: Context,
    private val request: AnimePlayback.Request,
    private val backend: AnimePlayerBackend,
    private val library: AnimeLibraryStore,
    private val downloads: AnimeDownloads,
    preferences: PreferenceStore,
    private val aniSkip: AniSkipLookup,
    processScope: CoroutineScope,
    restoredPosition: Double?,
    restoredPaused: Boolean,
) : ViewModel() {
    private val playerPreferences = AnimePlayerPreferences(preferences)
    private val quality = playerPreferences.quality
    private val autoplay = playerPreferences.autoplay
    private val mutableState = MutableStateFlow(
        AnimePlayerState(autoplay = autoplay.get(), settings = playerPreferences.get()),
    )
    val state = mutableState.asStateFlow()

    private val writes = Channel<PlaybackWrite>(Channel.UNLIMITED)
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
    private var aniSkipJob: Job? = null
    private var aniSkipAttempt: Pair<Long, Long>? = null
    private val skippedSegments = mutableSetOf<AniSkipSegment>()

    val shouldResume: Boolean get() = wantsPlayback
    var targetEpisodeId: Long? = (request as? AnimePlayback.Request.Episode)?.id
        private set

    init {
        processScope.launch {
            for (write in writes) {
                try {
                    when (write) {
                        is PlaybackWrite.Progress -> library.saveProgress(write.episodeId, write.checkpoint)
                            ?.let { AnimeTrackProgressJob.enqueue(context, it) }
                        PlaybackWrite.RefreshWidget -> refreshWatchingWidget(context)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "Could not apply anime playback update" }
                }
            }
        }
        viewModelScope.launch { repeatAnimeCheckpoints(::saveProgress) }
        try {
            backend.initialize(context, playerEvents())
            initialized = true
            backend.applySettings(state.value.settings)
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
            writes.trySend(PlaybackWrite.RefreshWidget)
        }
        syncPause()
    }

    fun togglePlayback() = setPlaying(state.value.ended || !state.value.playing)

    fun setPlaying(play: Boolean, retainAudioFocus: Boolean = false) {
        if (state.value.failed) return
        if (play && state.value.ended) seekTo(0.0)
        wantsPlayback = play
        if (play) {
            requestFocus()
        } else if (!retainAudioFocus) {
            abandonFocus()
        }
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

    fun selectEpisode(id: Long) {
        if (id != targetEpisodeId && state.value.episodes.any { it.id == id }) navigateTo(id)
    }

    fun setSettings(settings: AnimePlayerSettings) {
        playerPreferences.set(settings)
        val updated = playerPreferences.get()
        if (updated.speed != state.value.settings.speed) tracker.skipNextIncrement()
        mutableState.update { it.copy(settings = updated) }
        if (initialized) backend.applySettings(updated)
        if (updated.aniSkipMode == AniSkipMode.OFF) {
            resetAniSkip()
        } else {
            loadAniSkip()
            autoSkipIfReady()
        }
    }

    fun skipCurrentSegment() {
        val segment = state.value.activeAniSkip ?: return
        skippedSegments.add(segment)
        seekTo(segment.end)
    }

    fun setTrack(kind: AnimeTrackKind, track: AnimePlayerTrack?) {
        if (!initialized || state.value.loading || state.value.failed) return
        when (kind) {
            AnimeTrackKind.SUBTITLE ->
                playerPreferences.preferredSubtitle.set(track?.preferenceKey ?: AnimePlayerPreferences.SUBTITLES_OFF)
            AnimeTrackKind.AUDIO -> playerPreferences.preferredAudio.set(track?.preferenceKey.orEmpty())
        }
        backend.selectTrack(kind, track?.id)
        refreshTracks()
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
        resetAniSkip()
        skippedSegments.clear()
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
                settings = current.settings,
                fallbackNotice = current.fallbackNotice,
            )
        }
        syncPause()
        loadingJob = viewModelScope.launch {
            try {
                val episode = checkNotNull(library.getEpisode(id))
                val anime = checkNotNull(library.getAnime(episode.animeId))
                val episodes = library.getEpisodes(anime.id)
                val (previous, next) = adjacentAnimeEpisodes(episodes, episode.id)
                val start = restoredPosition?.takeIf { it.isFinite() && it >= 0 }
                    ?: episode.lastSecondSeen.toDouble().takeUnless { episode.seen }
                    ?: 0.0
                mutableState.update {
                    it.copy(
                        title = anime.title,
                        episode = episode,
                        episodes = episodes,
                        previous = previous,
                        next = next,
                        position = start,
                    )
                }
                val offline = downloads.offlineVideo(anime.source, anime.id, episode.id)
                if (offline != null) {
                    pendingVideo = offline
                    loadPending()
                    return@launch
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
                tracks = emptyList(),
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
            if (foreground) tracker.onPosition(seconds, state.value.settings.speed)
            if (abs(state.value.position - seconds) >= 0.1) {
                mutableState.update { it.copy(position = seconds) }
            }
            autoSkipIfReady()
        }

        override fun onDuration(seconds: Double) = onMain {
            if (!seconds.isFinite() || seconds <= 0 || (!waitingForFile && state.value.loading)) return@onMain
            tracker.onDuration(seconds)
            mutableState.update { it.copy(duration = seconds) }
            loadAniSkip()
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
            refreshTracks()
            syncPause()
            loadAniSkip()
        }

        override fun onTracksChanged() = onMain {
            if (!state.value.loading && !state.value.failed) refreshTracks()
        }

        override fun onEnded() = onMain {
            val current = state.value
            if (current.loading || current.ended || current.failed) return@onMain
            if (current.duration > 0) tracker.onPosition(current.duration, current.settings.speed)
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

    private fun resetAniSkip() {
        aniSkipJob?.cancel()
        aniSkipJob = null
        aniSkipAttempt = null
        mutableState.update { it.copy(aniSkipSegments = emptyList()) }
    }

    private fun loadAniSkip() {
        val current = state.value
        val episode = current.episode ?: return
        if (!TsuzukiAnime.enabled || current.loading || current.failed || current.duration < 1.0 ||
            current.settings.aniSkipMode == AniSkipMode.OFF
        ) {
            return
        }
        val attempt = episode.id to current.duration.roundToLong()
        if (aniSkipAttempt == attempt) return
        resetAniSkip()
        aniSkipAttempt = attempt
        aniSkipJob = viewModelScope.launch {
            try {
                val segments = aniSkip.segments(episode, current.duration)
                if (aniSkipAttempt == attempt) {
                    mutableState.update { it.copy(aniSkipSegments = segments) }
                    autoSkipIfReady()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logcat(LogPriority.WARN, error) { "AniSkip interval lookup failed" }
            }
        }
    }

    private fun autoSkipIfReady() {
        val current = state.value
        val segment = current.activeAniSkip ?: return
        if (current.settings.aniSkipMode == AniSkipMode.AUTO && current.playing &&
            segment.canAutoSkip(current.position, current.settings.aniSkipIntroDelay) &&
            skippedSegments.add(segment)
        ) {
            seekTo(segment.end)
        }
    }

    private fun refreshTracks() {
        val tracks = backend.tracks()
        val subtitles = tracks.filter { it.kind == AnimeTrackKind.SUBTITLE }
        val audio = tracks.filter { it.kind == AnimeTrackKind.AUDIO }
        val subtitlePreference = playerPreferences.preferredSubtitle.get()
        val subtitle = preferredAnimeTrack(subtitles, subtitlePreference)
        val preferredAudio = preferredAnimeTrack(audio, playerPreferences.preferredAudio.get())
        if (subtitlePreference == AnimePlayerPreferences.SUBTITLES_OFF && subtitles.any(AnimePlayerTrack::selected)) {
            backend.selectTrack(AnimeTrackKind.SUBTITLE, null)
        } else if (subtitle != null && !subtitle.selected) {
            backend.selectTrack(AnimeTrackKind.SUBTITLE, subtitle.id)
        }
        if (preferredAudio != null && !preferredAudio.selected) {
            backend.selectTrack(AnimeTrackKind.AUDIO, preferredAudio.id)
        }
        mutableState.update { it.copy(tracks = tracks) }
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
        writes.trySend(PlaybackWrite.Progress(episodeId, checkpoint))
    }

    private sealed interface PlaybackWrite {
        data class Progress(val episodeId: Long, val checkpoint: AnimePlaybackCheckpoint) : PlaybackWrite
        data object RefreshWidget : PlaybackWrite
    }

    override fun onCleared() {
        saveProgress()
        writes.close()
        abandonFocus()
        backend.close()
    }
}
