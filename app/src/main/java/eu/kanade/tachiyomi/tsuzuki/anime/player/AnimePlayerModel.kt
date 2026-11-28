package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.animesource.model.TimeStamp
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeNoPlayableVideoException
import eu.kanade.tachiyomi.tsuzuki.anime.AnimePlaybackCheckpoint
import eu.kanade.tachiyomi.tsuzuki.anime.AnimePlaybackProgress
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeStreamException
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeStreamResolver
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.describeAnimeVideo
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.tsuzuki.anime.excludedAnimeGroups
import eu.kanade.tachiyomi.tsuzuki.anime.filterAnimeGroups
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.model.editedAnimeTitle
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToLong

internal data class AnimePlayerState(
    val title: String = "",
    /** PLAYER-REDESIGN: the anime's source and cover, for episode thumbnails in the drawer and next-up card. */
    val sourceId: Long? = null,
    val coverUrl: String? = null,
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
    /**
     * False from each load until the video clock has moved (or playback is paused): the screen keeps the video
     * covered so the first frame mpv shows while its audio output starts never looks stuck (owner 2026-11-25).
     */
    val revealed: Boolean = false,
    val playing: Boolean = false,
    val ended: Boolean = false,
    val failed: Boolean = false,
    /** Why [failed] is set, for the error tile. */
    val error: Throwable? = null,
    val nextIn: Int? = null,
    val fallbackNotice: Int = 0,
    val aniSkipSegments: List<AniSkipSegment> = emptyList(),
    /** AY5: the file's own chapters (mpv `chapter-list`), read once the file has loaded. */
    val fileChapters: List<AnimeFileChapter> = emptyList(),
    /** AY5: the playing video's `Video.timestamps` from the source. */
    val timestamps: List<TimeStamp> = emptyList(),
    /** AY5: [fileChapters], [timestamps] and [aniSkipSegments] merged; see [mergeAnimeChapters]. */
    val chapters: List<AnimeChapter> = emptyList(),
    /** AY1: the episode's hosters and their loading state, for the quality sheet. */
    val hosters: List<AnimeHosterEntry> = emptyList(),
    /** AY4: mpv is waiting for data or a seek; [playing] stays true so the screen stays on. */
    val buffering: Boolean = false,
    /** A6.5: the episode goes to another app ("Play with"); the activity starts it and reports back. */
    val external: AnimeExternalLaunch? = null,
) {
    /**
     * The opening, ending or recap under the playhead. AniSkip segments only exist while AniSkip is on; a source's
     * own stamps are offered whatever the AniSkip mode (auto-skip still needs [AniSkipMode.AUTO]).
     */
    val activeAniSkip: AniSkipSegment?
        get() = if (loading || failed || ended) null else chapters.skipSegments().atPosition(position)

    /** The same state with [chapters] merged again from its inputs. */
    fun withMergedChapters(): AnimePlayerState =
        copy(chapters = mergeAnimeChapters(fileChapters, timestamps, aniSkipSegments, duration))
}

internal class AnimePlayerModel(
    private val context: Context,
    private val request: AnimePlayback.Request,
    private val backend: AnimePlayerBackend,
    private val library: AnimeLibraryStore,
    private val downloads: AnimeDownloads,
    preferences: PreferenceStore,
    private val aniSkip: AniSkipLookup,
    private val processScope: CoroutineScope,
    restoredPosition: Double?,
    restoredPaused: Boolean,
) : ViewModel() {
    private val previewGate = eu.kanade.tachiyomi.tsuzuki.anime.preview.PreviewCaptureGate()
    private val frames by lazy { Injekt.get<eu.kanade.tachiyomi.tsuzuki.anime.preview.AnimeFrameStore>() }
    private val basePreferences by lazy { Injekt.get<eu.kanade.domain.base.BasePreferences>() }
    private val playerPreferences = AnimePlayerPreferences(preferences)
    private val quality = playerPreferences.quality
    private val autoplay = playerPreferences.autoplay
    private val externalPlayer = playerPreferences.externalPlayer

    /** A6.5: set once no app could open the video, so Tsuzuki plays this episode itself. */
    private var externalUnavailable = false
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
    private val offlinePlaybackOwner = Any()
    private var loadingJob: Job? = null
    private var episodeGroupsJob: Job? = null
    private var countdownJob: Job? = null
    private var cancelledCountdown = false
    private data class PendingLoad(val video: Video, val subtitles: List<Track>)

    private var pendingLoad: PendingLoad? = null
    private var videoPreparation: Job? = null
    private var streams: AnimeStreamQueue? = null
    private var hosterLoader: AnimeHosterLoader? = null
    private var hosterChoiceJob: Job? = null

    /** ANIME-FAST-START: the hosters still loading after playback started; cancelled with the episode. */
    private var hosterJob: Job? = null
    private var fileWatchdog: Job? = null
    private var source: RegisteredAnimeSource? = null
    private var candidates = emptyList<Video>()
    private val directTrackOverrides = mutableMapOf<AnimeTrackKind, String>()
    private var lastFailure: Throwable? = null
    private var currentVideo: Video? = null
    private var loadedStart = 0.0
    private var resumeChecked = true

    /** AY4: the first load after process death keeps its restored position even near the end. */
    private var keepRestoredStart = false
    private var restartPending = false
    private var aniSkipJob: Job? = null
    private var aniSkipAttempt: Pair<Long, Long>? = null
    private var aniSkipGateLogged: Pair<Long, Long>? = null
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
                    queueVideo(request.video)
                }
                is AnimePlayback.Request.Episode -> openEpisode(request.id, restoredPosition)
            }
        } catch (e: Exception) {
            AnimeLog.e("Could not initialize anime playback", e)
            fail(e)
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

    fun detach(closing: Boolean = false) {
        if (!attached) return
        // Surface destruction can precede onStop/onCleared. Request while the native video output still exists.
        capturePreview(closing = closing)
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
        if (play && state.value.ended) restartAfterEnd()
        wantsPlayback = play
        if (play) {
            requestFocus()
        } else if (!retainAudioFocus) {
            abandonFocus()
        }
        syncPause()
    }

    private fun restartAfterEnd() {
        when (animePlayAfterEnd(state.value.duration)) {
            AnimePlayAfterEnd.SEEK_TO_START -> seekTo(0.0)
            AnimePlayAfterEnd.RELOAD -> {
                val video = currentVideo ?: return
                AnimeLog.i("play after end with unknown duration: loading the stream again from 0")
                cancelCountdown()
                tracker.skipNextIncrement()
                mutableState.update {
                    it.copy(position = 0.0, bufferedUntil = 0.0, ended = false, loading = true, revealed = false)
                }
                queueVideo(video)
            }
        }
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
        val value = track?.preferenceKey ?: if (kind == AnimeTrackKind.SUBTITLE) {
            AnimePlayerPreferences.SUBTITLES_OFF
        } else {
            ""
        }
        val animeId = state.value.episode?.animeId
        if (animeId == null) {
            directTrackOverrides[kind] = value
        } else {
            playerPreferences.trackOverride(animeId, kind).set(value)
        }
        backend.selectTrack(kind, track?.id)
        refreshTracks()
    }

    fun setQuality(resolution: Int) {
        if (candidates.isEmpty() || (resolution == quality.get() && resolution == state.value.resolution)) return
        quality.set(resolution)
        saveProgress()
        streams = AnimeStreamQueue(candidates, resolution, loader = hosterLoader)
        tryNextStream()
    }

    /**
     * AY1: load a hoster the user picked in the quality sheet (a lazy one, or a retry of a failed one) and play
     * its videos, in the source's order: an explicit choice of server is not overridden by another hoster.
     */
    fun loadHoster(index: Int) {
        val loader = hosterLoader ?: return
        val entry = loader.states.value.getOrNull(index) ?: return
        if (entry.state == AnimeHosterState.Loading || hosterChoiceJob?.isActive == true) return
        hosterChoiceJob = viewModelScope.launch {
            val stateJob = launch { loader.states.collect { list -> mutableState.update { it.copy(hosters = list) } } }
            try {
                withContext(Dispatchers.IO) { loader.loadHoster(index) }
            } finally {
                stateJob.cancel()
            }
            val picked = (loader.states.value[index].state as? AnimeHosterState.Ready)?.videos.orEmpty()
            mutableState.update { it.copy(hosters = loader.states.value) }
            if (picked.isEmpty()) return@launch
            candidates = loader.videos()
            val qualities = candidates.mapNotNull(Video::animeResolution).distinct().sortedDescending()
            mutableState.update { it.copy(qualities = qualities) }
            playHosterVideos(picked)
        }
    }

    fun selectHosterVideo(hosterIndex: Int, videoIndex: Int) {
        val entry = hosterLoader?.states?.value?.getOrNull(hosterIndex) ?: return
        val videos = (entry.state as? AnimeHosterState.Ready)?.videos ?: return
        val selected = videos.getOrNull(videoIndex) ?: return
        playHosterVideos(videos, selected)
    }

    private fun playHosterVideos(videos: List<Video>, selected: Video? = null) {
        saveProgress()
        streams = AnimeStreamQueue(videos, 0, selected?.let { it to it })
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
            val current = state.value
            openEpisode(episode, current.position.takeUnless { animeStartIsAtEnd(it, current.duration) } ?: 0.0)
        } else if (request is AnimePlayback.Request.Direct) {
            mutableState.update { it.copy(loading = true, failed = false, revealed = false) }
            queueVideo(request.video)
        }
    }

    private fun navigateTo(id: Long) {
        wantsPlayback = true
        requestFocus()
        openEpisode(id)
    }

    private fun openEpisode(id: Long, restoredPosition: Double? = null) {
        capturePreview()
        targetEpisodeId = id
        resetAniSkip()
        skippedSegments.clear()
        saveProgress()
        cancelCountdown()
        cancelledCountdown = false
        loadingJob?.cancel()
        hosterChoiceJob?.cancel()
        hosterJob?.cancel()
        hosterJob = null
        fileWatchdog?.cancel()
        episodeGroupsJob?.cancel()
        waitingForFile = false
        cancelPendingLoad()
        streams = null
        candidates = emptyList()
        hosterLoader = null
        source = null
        lastFailure = null
        tracker = AnimePlaybackProgress()
        mutableState.update { current ->
            AnimePlayerState(
                title = current.title,
                sourceId = current.sourceId,
                coverUrl = current.coverUrl,
                autoplay = current.autoplay,
                settings = current.settings,
                fallbackNotice = current.fallbackNotice,
            )
        }
        syncPause()
        val openedAt = SystemClock.elapsedRealtime()
        loadingJob = viewModelScope.launch {
            try {
                val episode = checkNotNull(library.getEpisode(id))
                val anime = checkNotNull(library.getAnime(episode.animeId))
                val episodes = library.getEpisodes(anime.id)
                val restored = restoredPosition?.takeIf { it.isFinite() && it >= 0 }
                val start = animeResumeStart(
                    saved = restored ?: episode.lastSecondSeen.toDouble(),
                    seen = restored == null && episode.seen,
                    knownDuration = episode.totalSeconds.toDouble(),
                    restored = restored != null,
                )
                keepRestoredStart = restored != null
                mutableState.update {
                    it.copy(
                        title = anime.memo.editedAnimeTitle(anime.title),
                        sourceId = anime.source,
                        coverUrl = anime.thumbnailUrl,
                        episode = episode,
                        position = start,
                    )
                }
                updateEpisodeQueue(episodes, id, anime.memo.excludedAnimeGroups())
                episodeGroupsJob = viewModelScope.launch {
                    library.observeExcludedGroups(anime.id)
                        .catch { error -> logcat(LogPriority.WARN, error) { "Could not observe anime episode groups" } }
                        .collect { groups -> updateEpisodeQueue(episodes, id, groups) }
                }
                val offline = downloads.offlineVideo(anime.source, anime.id, episode.id, offlinePlaybackOwner)
                if (offline != null) {
                    queueVideo(offline)
                    return@launch
                }
                AnimeLog.i("open episode ${episode.id}: source=${anime.source} start=${"%.1f".format(start)}s")
                val (entry, loader) = withContext(Dispatchers.IO) {
                    val sources = Injekt.get<AnimeSourceManager>()
                    sources.ensureLoaded()
                    AnimeLog.i("extensions loaded (+${SystemClock.elapsedRealtime() - openedAt} ms)")
                    val entry = checkNotNull(sources.get(anime.source)) { "Source not installed: ${anime.source}" }
                    AnimeLog.i("requesting videos from ${entry.extensionName} (lib ${entry.libVersion})")
                    // AY1: every non-lazy hoster at once; a dead or hanging one doesn't fail the episode.
                    val hosters = withTimeoutOrNull(ANIME_HOSTER_TIMEOUT_MS) {
                        AnimeStreamResolver.hosters(entry.source, entry.libVersion, episode.toSourceEpisode())
                    } ?: throw AnimeStreamException("The source did not respond within 20 s")
                    entry to AnimeHosterLoader(hosters, { AnimeStreamResolver.hosterVideos(entry.source, it) })
                }
                val selectedQuality = quality.get()
                // ANIME-FAST-START: the hosters load in a job of their own, so the ones still loading when
                // playback starts keep going (quality sheet, fallback) until the episode changes or closes.
                val hosters = Job(viewModelScope.coroutineContext[Job])
                hosterJob = hosters
                loader.start(CoroutineScope(viewModelScope.coroutineContext + hosters + Dispatchers.IO))
                val first = withContext(Dispatchers.IO) {
                    val resolvePreferred: (suspend (Video) -> Video?)? = if (selectedQuality <= 0) {
                        { candidate ->
                            try {
                                resolveStream(entry, candidate)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                AnimeLog.w("could not resolve preferred video ${describeAnimeVideo(candidate)}", e)
                                null
                            }
                        }
                    } else {
                        null
                    }
                    loader.awaitPlayable(selectedQuality, resolvePreferred)
                }
                hosterLoader = loader
                val videos = loader.videos()
                val answered = loader.states.value.filterNot { it.hoster.lazy }
                AnimeLog.i(
                    "source returned ${videos.size} video(s) from " +
                        "${answered.count { it.state is AnimeHosterState.Ready }}/${answered.size} hoster(s)" +
                        "${if (loader.settled()) "" else ", the rest still loading"} " +
                        "(+${SystemClock.elapsedRealtime() - openedAt} ms)",
                )
                videos.forEach { AnimeLog.d("candidate ${describeAnimeVideo(it)}") }
                source = entry
                candidates = videos
                if (videos.isEmpty()) {
                    // Every hoster failed: show the first real cause, not a generic "no video".
                    lastFailure = hosterLoader?.states?.value
                        ?.firstNotNullOfOrNull { (it.state as? AnimeHosterState.Error)?.cause }
                }
                val qualities = candidates.mapNotNull(Video::animeResolution).distinct().sortedDescending()
                mutableState.update { it.copy(qualities = qualities, hosters = hosterLoader?.states?.value.orEmpty()) }
                streams = AnimeStreamQueue(candidates, selectedQuality, first, loader)
                tryNextStream()
                if (!loader.settled()) followLateHosters(loader, hosters, openedAt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AnimeLog.e("Could not open anime episode", e)
                fail(e)
            }
        }
    }

    /** ANIME-FAST-START: hosters that answer after playback started reach the quality sheet and [candidates]. */
    private fun followLateHosters(loader: AnimeHosterLoader, hosters: Job, openedAt: Long) {
        val scope = CoroutineScope(viewModelScope.coroutineContext + hosters)
        scope.launch {
            loader.states.collect { list ->
                if (hosterLoader !== loader) return@collect
                candidates = loader.videos()
                val qualities = candidates.mapNotNull(Video::animeResolution).distinct().sortedDescending()
                mutableState.update { it.copy(hosters = list, qualities = qualities) }
            }
        }
        scope.launch {
            loader.awaitSettled()
            AnimeLog.i(
                "all hosters answered: ${loader.videos().size} video(s) " +
                    "(+${SystemClock.elapsedRealtime() - openedAt} ms)",
            )
        }
    }

    private fun updateEpisodeQueue(episodes: List<Episode>, episodeId: Long, excludedGroups: Set<String>) {
        if (targetEpisodeId != episodeId) return
        val (previous, next) = adjacentAnimeEpisodes(episodes.filterAnimeGroups(excludedGroups, episodeId), episodeId)
        mutableState.update {
            it.copy(episodes = episodes.filterAnimeGroups(excludedGroups), previous = previous, next = next)
        }
        if (next == null) cancelCountdown()
    }

    private suspend fun resolveStream(entry: RegisteredAnimeSource, candidate: Video): Video {
        val video = withTimeoutOrNull(ANIME_HOSTER_TIMEOUT_MS) {
            AnimeStreamResolver.resolve(entry.source, entry.libVersion, candidate)
        } ?: throw AnimeStreamException("The source did not resolve the stream within 20 s")
        return AnimeStreamResolver.withSourceHeaders(entry.source, video)
            .also { AnimeLog.i("resolved video ${describeAnimeVideo(it)}") }
    }

    private fun tryNextStream(fallback: Boolean = false) {
        val entry = source ?: return fail(lastFailure)
        val queue = streams ?: return fail(lastFailure)
        fileWatchdog?.cancel()
        waitingForFile = false
        cancelPendingLoad()
        tracker.skipNextIncrement()
        mutableState.update {
            it.copy(
                loading = true,
                revealed = false,
                failed = false,
                ended = false,
                tracks = emptyList(),
                fileChapters = emptyList(),
                timestamps = emptyList(),
                aniSkipSegments = emptyList(),
                chapters = emptyList(),
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
                queue.next { candidate ->
                    try {
                        resolveStream(entry, candidate)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AnimeLog.w("could not resolve video ${describeAnimeVideo(candidate)}", e)
                        lastFailure = e
                        throw e
                    }
                }
            }
            if (video == null) {
                AnimeLog.e("no playable video left (fallback=$fallback)")
                fail(lastFailure ?: AnimeNoPlayableVideoException())
            } else {
                queueVideo(video)
            }
        }
        oldJob?.cancel()
    }

    private fun queueVideo(video: Video) {
        cancelPendingLoad()
        if (!externalUnavailable) {
            val launch = animeExternalLaunch(
                choice = AnimeExternalChoice.from(externalPlayer.get()),
                url = video.videoUrl,
                title = animeMediaTitle(state.value.title, state.value.episode?.name),
                positionSeconds = state.value.position,
                headers = video.headers?.toMap().orEmpty(),
                subtitles = video.subtitleTracks.map { AnimeExternalLaunch.Subtitle(it.url, it.lang) },
            )
            if (launch != null) {
                currentVideo = video
                AnimeLog.i("external player ${launch.packageName ?: "chooser"} for ${describeAnimeVideo(video)}")
                mutableState.update { it.copy(external = launch) }
                return
            }
        }
        videoPreparation = viewModelScope.launch {
            val subtitles = prepareAnimeSubtitleTracks(video.subtitleTracks) { File(context.cacheDir, "anime-subs") }
            pendingLoad = PendingLoad(video, subtitles)
            loadPending()
        }
    }

    private fun cancelPendingLoad() {
        videoPreparation?.cancel()
        videoPreparation = null
        pendingLoad = null
    }

    private fun loadPending() {
        if (!attached) return
        val (video, subtitles) = pendingLoad ?: return
        pendingLoad = null
        waitingForFile = true
        currentVideo = video
        loadedStart = state.value.position
        resumeChecked = loadedStart <= 0 || keepRestoredStart
        keepRestoredStart = false
        restartPending = false
        resetAniSkip()
        mutableState.update {
            it.copy(
                resolution = video.animeResolution,
                buffering = false,
                fileChapters = emptyList(),
                timestamps = video.timestamps,
            ).withMergedChapters()
        }
        backend.setMediaTitle(animeMediaTitle(state.value.title, state.value.episode?.name))
        backend.load(video, state.value.position, subtitles)
        // AY1: mpv can stall without FILE_LOADED or an error; treat silence as a failed stream.
        fileWatchdog?.cancel()
        fileWatchdog = viewModelScope.launch {
            delay(ANIME_FILE_LOADED_WATCHDOG_MS)
            if (waitingForFile && currentVideo === video) {
                AnimeLog.w("mpv FILE_LOADED watchdog: nothing after ${ANIME_FILE_LOADED_WATCHDOG_MS / 1000} s")
                val failure = AnimeStreamException("The stream did not start within 30 s")
                lastFailure = failure
                if (streams == null) fail(failure) else tryNextStream(fallback = true)
            }
        }
    }

    private fun playerEvents() = object : AnimePlayerBackend.Listener {
        override fun onPosition(seconds: Double) = onMain {
            if (state.value.loading || state.value.ended || !seconds.isFinite() || seconds < 0) return@onMain
            if (restartPending && seconds < state.value.duration - ANIME_RESUME_END_MARGIN_SECONDS) {
                restartPending = false
            }
            if (foreground) tracker.onPosition(seconds, state.value.settings.speed)
            if (!state.value.revealed) {
                // mpv first reports the requested start, then plays from the keyframe before it: measure from the
                // lowest clock value seen, or the cover would stay up for the stretch back to the keyframe.
                if (revealFrom.isNaN() || seconds < revealFrom) revealFrom = seconds
                if (seconds >= revealFrom + REVEAL_CLOCK_MOVED_SECONDS && !state.value.buffering) reveal()
            }
            if (abs(state.value.position - seconds) >= 0.1) {
                mutableState.update { it.copy(position = seconds) }
            }
            autoSkipIfReady()
        }

        override fun onDuration(seconds: Double) = onMain {
            if (!seconds.isFinite() || seconds <= 0 || (!waitingForFile && state.value.loading)) return@onMain
            tracker.onDuration(seconds)
            mutableState.update { it.copy(duration = seconds).withMergedChapters() }
            restartIfStartedAtEnd()
            loadAniSkip()
        }

        override fun onBuffered(seconds: Double) = onMain {
            if (seconds.isFinite() && !state.value.loading) {
                mutableState.update { it.copy(bufferedUntil = seconds) }
            }
        }

        override fun onSeek() = onMain { tracker.skipNextIncrement() }

        override fun onBuffering(buffering: Boolean) = onMain {
            if (state.value.buffering != buffering) mutableState.update { it.copy(buffering = buffering) }
        }

        override fun onLoaded() = onMain {
            if (!waitingForFile) return@onMain
            fileWatchdog?.cancel()
            waitingForFile = false
            val chapters = backend.chapters()
            if (chapters.isNotEmpty()) AnimeLog.i("file has ${chapters.size} chapter(s)")
            mutableState.update { it.copy(loading = false, fileChapters = chapters).withMergedChapters() }
            revealFrom = Double.NaN
            revealTimeout?.cancel()
            revealTimeout = viewModelScope.launch {
                // Only a guard for a clock that never reports; a buffering start keeps its cover.
                delay(REVEAL_TIMEOUT_MS)
                while (state.value.buffering) delay(REVEAL_POLL_MS)
                reveal()
            }
            restartIfStartedAtEnd()
            refreshTracks()
            syncPause()
            loadAniSkip()
        }

        override fun onTracksChanged() = onMain {
            if (!state.value.loading && !state.value.failed) refreshTracks()
        }

        override fun onEnded() = onMain {
            val current = state.value
            if (current.loading || current.ended || current.failed || restartPending) return@onMain
            if (current.duration > 0) tracker.onPosition(current.duration, current.settings.speed)
            saveProgress()
            mutableState.update { it.copy(ended = true) }
            syncPause()
            startCountdown()
        }

        override fun onError(detail: String?) = onMain {
            if (state.value.failed || (!waitingForFile && state.value.loading)) return@onMain
            fileWatchdog?.cancel()
            val failure = AnimeStreamException(detail)
            lastFailure = failure
            if (streams == null) fail(failure) else tryNextStream(fallback = true)
        }
    }

    /** The duration was unknown when the file was opened: if the start we passed is at its end, start over. */
    private fun restartIfStartedAtEnd() {
        val current = state.value
        if (resumeChecked || current.loading || current.duration <= 0) return
        resumeChecked = true
        if (animeStartIsAtEnd(loadedStart, current.duration)) {
            AnimeLog.w(
                "resume ${loadedStart.toInt()}s is at the end of a ${current.duration.toInt()}s file: restarting",
            )
            restartPending = true
            seekTo(0.0)
        }
    }

    private fun onMain(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) { block() }
    }

    private fun resetAniSkip() {
        aniSkipJob?.cancel()
        aniSkipJob = null
        aniSkipAttempt = null
        mutableState.update { it.copy(aniSkipSegments = emptyList()).withMergedChapters() }
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
        if (!shouldRequestAniSkip(
                current.settings.aniSkipMode,
                current.settings.aniSkipIgnoreWithChapters,
                current.fileChapters,
                current.timestamps,
            )
        ) {
            if (aniSkipGateLogged != attempt) {
                aniSkipGateLogged = attempt
                AnimeLog.i(
                    "AniSkip not requested: the video has ${current.fileChapters.size} chapter(s) and " +
                        "${current.timestamps.size} source timestamp(s)",
                )
            }
            if (aniSkipAttempt != null || current.aniSkipSegments.isNotEmpty()) resetAniSkip()
            return
        }
        if (aniSkipAttempt == attempt) return
        AnimeLog.i("AniSkip lookup for episode ${episode.id}")
        resetAniSkip()
        aniSkipAttempt = attempt
        aniSkipJob = viewModelScope.launch {
            try {
                val segments = aniSkip.segments(episode, current.duration)
                if (aniSkipAttempt == attempt) {
                    mutableState.update { it.copy(aniSkipSegments = segments).withMergedChapters() }
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
        val animeId = state.value.episode?.animeId
        for (kind in AnimeTrackKind.entries) {
            val override = animeId?.let { playerPreferences.trackOverride(it, kind).get() }
                ?: directTrackOverrides[kind]
            val legacy = when (kind) {
                AnimeTrackKind.AUDIO -> playerPreferences.preferredAudio.get()
                AnimeTrackKind.SUBTITLE -> playerPreferences.preferredSubtitle.get()
            }
            val languages = animeLanguageOrder(playerPreferences.languages(kind).get(), kind)
            val choice = chooseAnimeTrack(kind, tracks, override, languages, legacy) ?: continue
            val selected = tracks.firstOrNull { it.kind == kind && it.selected }
            if (choice.track?.id != selected?.id) backend.selectTrack(kind, choice.track?.id)
        }
        mutableState.update { it.copy(tracks = tracks) }
    }

    private fun fail(error: Throwable? = null) {
        cancelPendingLoad()
        waitingForFile = false
        mutableState.update { it.copy(loading = false, failed = true, error = error) }
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

    private var revealFrom = 0.0
    private var revealTimeout: Job? = null

    private fun reveal() {
        revealTimeout?.cancel()
        if (!state.value.revealed) mutableState.update { it.copy(revealed = true) }
    }

    private fun syncPause() {
        val current = state.value
        val playing = foreground && attached && focused && wantsPlayback &&
            !current.loading && !current.ended && !current.failed
        if (current.playing && !playing) capturePreview()
        if (current.playing != playing) tracker.skipNextIncrement()
        if (initialized) backend.setPaused(!playing)
        mutableState.update { it.copy(playing = playing) }
        // A paused or finished video has nothing to wait for: show the frame it stopped on.
        if (!playing && !current.loading && !current.revealed) reveal()
    }

    private fun capturePreview(closing: Boolean = false) {
        if (!initialized || !attached || state.value.loading || state.value.failed) return
        if ((request as? AnimePlayback.Request.Episode)?.track != true) return
        val current = state.value
        val episode = current.episode ?: return
        if (!previewGate.acquire(
                episode.id,
                current.position,
                SystemClock.elapsedRealtime(),
                closing,
                basePreferences.incognitoMode.get(),
            )
        ) {
            return
        }
        val generation = frames.generation(episode.animeId)
        val raw = try {
            backend.capturePreview()
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Could not request anime preview capture" }
            null
        } ?: return
        val sequence = frames.captureSequence(episode.id)
        processScope.launch(Dispatchers.IO) {
            try {
                frames.accept(episode.animeId, episode.id, raw, generation, sequence) {
                    !basePreferences.incognitoMode.get()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Could not save anime preview capture" }
            }
        }
    }

    /** A6.5: the other app returned; save where it stopped (it may report nothing). */
    fun onExternalResult(result: AnimeExternalResult?) {
        val start = state.value.external?.positionMs?.div(1000.0) ?: 0.0
        mutableState.update { it.copy(external = null) }
        AnimeLog.i("external player returned ${result ?: "no position"}")
        if (result == null || (request as? AnimePlayback.Request.Episode)?.track != true) return
        val episodeId = state.value.episode?.id ?: return
        val watched = (result.position - start).coerceIn(0.0, result.duration).toLong()
        writes.trySend(
            PlaybackWrite.Progress(episodeId, AnimePlaybackCheckpoint(result.position, result.duration, watched)),
        )
        writes.trySend(PlaybackWrite.RefreshWidget)
    }

    /** A6.5: no app could open the video; play it here instead. */
    fun onExternalUnavailable() {
        externalUnavailable = true
        mutableState.update { it.copy(external = null) }
        currentVideo?.let(::queueVideo)
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
        capturePreview(closing = true)
        cancelPendingLoad()
        saveProgress()
        writes.close()
        abandonFocus()
        backend.close()
        downloads.releaseOfflinePlayback(offlinePlaybackOwner)
    }
}

/** The clock counts as moving once it is this far past where the file was loaded. */
/** About seven frames at 24 fps: the picture is flowing, not just the audio clock (owner 2026-11-28). */
private const val REVEAL_CLOCK_MOVED_SECONDS = 0.3

/** Never keep the video covered longer than this after the file loads. */
private const val REVEAL_TIMEOUT_MS = 3_000L
private const val REVEAL_POLL_MS = 100L
