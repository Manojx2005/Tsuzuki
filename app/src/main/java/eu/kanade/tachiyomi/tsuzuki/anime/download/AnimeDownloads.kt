package eu.kanade.tachiyomi.tsuzuki.anime.download

import android.content.Context
import android.os.Environment
import android.util.AtomicFile
import androidx.compose.runtime.snapshotFlow
import androidx.work.ExistingWorkPolicy
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeStreamResolver
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimeHosterLoader
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerPreferences
import eu.kanade.tachiyomi.tsuzuki.anime.player.animePlaybackOrder
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceEpisode
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.networkStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.coroutineContext

@Serializable
internal enum class AnimeDownloadStatus { Queued, Saved, Failed, Removing }

@Serializable
internal data class AnimeDownloadEntry(
    val sourceId: Long,
    val animeId: Long,
    val episodeId: Long,
    val sourceName: String,
    val animeTitle: String,
    val episodeTitle: String,
    val status: AnimeDownloadStatus = AnimeDownloadStatus.Queued,
    val bytes: Long = 0L,
    val pendingAutoDelete: Boolean = false,
)

@Serializable
internal data class AnimeDownloadQueue(
    val entries: List<AnimeDownloadEntry> = emptyList(),
    val paused: Boolean = false,
    val wifiOnly: Boolean = true,
) {
    // Failed entries may have partial files; removing entries remain protected until deletion finishes.
    fun protectedEpisodeIds(animeId: Long): Set<Long> =
        entries.filter { it.animeId == animeId }.mapTo(hashSetOf()) { it.episodeId }

    val hasQueuedEpisodes: Boolean get() = entries.any { it.status == AnimeDownloadStatus.Queued }

    /** Anime with at least one finished download, for the library's "Downloaded only" filter (AY-INT item 5). */
    fun isSaved(id: Long): Boolean = entries.any { it.episodeId == id && it.status == AnimeDownloadStatus.Saved }

    fun savedAnimeIds(): Set<Long> =
        entries.filter { it.status == AnimeDownloadStatus.Saved }.mapTo(hashSetOf()) { it.animeId }

    fun enqueue(entry: AnimeDownloadEntry): AnimeDownloadQueue {
        val existing = entries.firstOrNull { it.episodeId == entry.episodeId }
        return when (existing?.status) {
            null -> copy(entries = entries + entry)
            AnimeDownloadStatus.Failed -> change(entry.episodeId) { it.copy(status = AnimeDownloadStatus.Queued) }
            else -> this
        }
    }

    fun change(id: Long, transform: (AnimeDownloadEntry) -> AnimeDownloadEntry?): AnimeDownloadQueue =
        copy(entries = entries.mapNotNull { if (it.episodeId == id) transform(it) else it })

    /** Moves a queued episode before every other queued one, or after them; downloads run in list order (AP11). */
    fun move(id: Long, top: Boolean): AnimeDownloadQueue {
        val entry = entries.firstOrNull { it.episodeId == id && it.status == AnimeDownloadStatus.Queued } ?: return this
        val rest = entries - entry
        val queued = rest.indices.filter { rest[it].status == AnimeDownloadStatus.Queued }
        val at = if (top) queued.firstOrNull() ?: rest.size else (queued.lastOrNull()?.plus(1)) ?: rest.size
        return copy(entries = rest.toMutableList().apply { add(at, entry) })
    }

    fun retryFailed(): AnimeDownloadQueue = copy(
        entries = entries.map {
            if (it.status == AnimeDownloadStatus.Failed) it.copy(status = AnimeDownloadStatus.Queued) else it
        },
    )
}

/**
 * Episodes that turned watched since the last look, per anime (AP11). An anime's first look only sets its baseline,
 * so episodes that were already watched (including ones downloaded later) are never counted.
 */
internal fun newlyWatchedDownloads(previous: Map<Long, Set<Long>>, current: Map<Long, Set<Long>>): Set<Long> =
    current.flatMapTo(hashSetOf()) { (animeId, seen) -> previous[animeId]?.let { seen - it }.orEmpty() }

internal fun animeDeleteAfterWatched(preferences: PreferenceStore) =
    preferences.getBoolean("tsuzuki_anime_delete_after_watched", false)

internal class AnimeDownloads(
    private val context: Context,
    private val library: AnimeLibraryStore,
    private val network: NetworkHelper,
    preferences: PreferenceStore,
    private val scope: CoroutineScope,
) {
    private val prefs = AnimePlayerPreferences(preferences)
    private val stored = AtomicFile(File(context.filesDir, "anime-download-queue.json"))
    private val edits = Mutex()
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val disk = Mutex()
    // Guarded by edits. A player keeps its local files until the backend closes.
    private val offlinePlaybackEpisodes = mutableMapOf<Any, MutableSet<Long>>()
    private val mutableState = MutableStateFlow(AnimeDownloadQueue())
    private val mutableProgress = MutableStateFlow<Pair<Long, AnimeDownloadProgress>?>(null)
    val state = mutableState.asStateFlow()
    val progress = mutableProgress.asStateFlow()

    private val initialized = scope.async(Dispatchers.IO) {
        mutableState.value = try {
            Json.decodeFromString<AnimeDownloadQueue>(stored.readFully().decodeToString())
        } catch (_: FileNotFoundException) {
            AnimeDownloadQueue()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Could not read the anime download queue" }
            AnimeDownloadQueue()
        }
    }

    private val deleteAfterWatched = animeDeleteAfterWatched(preferences)

    init {
        // AP11: with "Delete after watched" on, a saved episode is deleted once it becomes watched. Episodes that were
        // already watched when they were saved are left alone, as is everything while the setting is off.
        scope.launch(Dispatchers.IO) {
            initialized.await()
            var previous = emptyMap<Long, Set<Long>>()
            combine(state, deleteAfterWatched.changes()) { queue, enabled ->
                if (enabled) queue.savedAnimeIds() else emptySet()
            }
                .distinctUntilChanged()
                .flatMapLatest { animeIds ->
                    if (animeIds.isEmpty()) {
                        flowOf(emptyMap())
                    } else {
                        val ids = animeIds.toList()
                        combine(ids.map(library::observeWatchedEpisodeIds)) { lists ->
                            ids.zip(lists).toMap()
                        }
                    }
                }
                .distinctUntilChanged()
                .collect { watched ->
                    val newly = newlyWatchedDownloads(previous, watched).filterTo(hashSetOf(), state.value::isSaved)
                    previous = watched
                    command { removeWatched(newly, watched) }
                }
        }
        scope.launch(Dispatchers.IO) {
            initialized.await()
            val notifier = AnimeDownloadNotifier(context)
            combine(
                state,
                context.networkStateFlow().onStart { emit(context.activeNetworkState()) },
                snapshotFlow { TsuzukiAnime.enabled },
            ) { queue, network, enabled ->
                if (enabled) queue.deferredDownloadNotification(network) else null
            }.distinctUntilChanged().collect(notifier::showDeferred)
        }
        scope.launch(Dispatchers.IO) {
            initialized.await()
            for (action in commands) {
                try {
                    action()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Could not update the anime download queue" }
                }
            }
        }
    }

    suspend fun readyQueue(): AnimeDownloadQueue {
        initialized.await()
        return state.value
    }

    suspend fun protectedEpisodeIds(animeId: Long): Set<Long> {
        initialized.await()
        return state.value.protectedEpisodeIds(animeId)
    }

    fun restoreWhenEnabled() = command {
        if (!TsuzukiAnime.enabled) return@command
        val restored = state.value
        restored.entries.filter { it.status == AnimeDownloadStatus.Removing }.forEach(::deleteAsync)
        removeWatched(emptySet(), emptyMap())
        if (restored.hasQueuedEpisodes && !restored.paused) {
            AnimeDownloadJob.schedule(context, restored.wifiOnly, ExistingWorkPolicy.KEEP)
        }
    }

    fun enqueue(sourceId: Long, sourceName: String, animeTitle: String, episodes: List<Episode>) =
        command { enqueueEpisodes(sourceId, sourceName, animeTitle, episodes) }

    suspend fun enqueueAndAwait(sourceId: Long, sourceName: String, animeTitle: String, episodes: List<Episode>) =
        awaitCommand { enqueueEpisodes(sourceId, sourceName, animeTitle, episodes) }

    private suspend fun enqueueEpisodes(sourceId: Long, sourceName: String, animeTitle: String, episodes: List<Episode>) {
        val changed = edit { queue ->
            episodes.fold(queue) { acc, episode ->
                acc.enqueue(
                    AnimeDownloadEntry(sourceId, episode.animeId, episode.id, sourceName, animeTitle, episode.name),
                )
            }
        }
        if (changed &&
            !state.value.paused
        ) {
            AnimeDownloadJob.schedule(context, state.value.wifiOnly, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    fun retry(id: Long) = command { retryEpisode(id) }

    suspend fun retryAndAwait(id: Long) = awaitCommand { retryEpisode(id) }

    private suspend fun retryEpisode(id: Long) {
        val changed = edit { queue ->
            queue.change(id) {
                if (it.status == AnimeDownloadStatus.Failed) it.copy(status = AnimeDownloadStatus.Queued) else it
            }
        }
        if (changed &&
            !state.value.paused
        ) {
            AnimeDownloadJob.schedule(context, state.value.wifiOnly, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    fun move(id: Long, top: Boolean) = command { edit { it.move(id, top) } }

    fun retryFailed() = command {
        if (edit(AnimeDownloadQueue::retryFailed) && !state.value.paused) {
            AnimeDownloadJob.schedule(context, state.value.wifiOnly, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
    }

    /** Removes every failed episode and its partial files, as Remove does for one. */
    fun clearFailed() = command {
        editForRemoval { entry ->
            if (entry.status == AnimeDownloadStatus.Failed) entry.removing() else entry
        }
    }

    fun setPaused(paused: Boolean) = command {
        if (edit { it.copy(paused = paused) }) {
            if (paused) {
                AnimeDownloadJob.cancel(context)
            } else if (state.value.hasQueuedEpisodes) {
                AnimeDownloadJob.schedule(context, state.value.wifiOnly, ExistingWorkPolicy.REPLACE)
            }
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) = command {
        if (edit { it.copy(wifiOnly = wifiOnly) } && !state.value.paused && state.value.hasQueuedEpisodes) {
            AnimeDownloadJob.schedule(context, state.value.wifiOnly, ExistingWorkPolicy.REPLACE)
        }
    }

    fun cancelQueued() = command {
        var cancelled = emptyList<AnimeDownloadEntry>()
        if (edit { queue ->
                cancelled = queue.entries.filter { it.status == AnimeDownloadStatus.Queued }
                if (cancelled.isEmpty()) {
                    queue
                } else {
                    queue.copy(
                        paused = false,
                        entries = queue.entries.map { entry ->
                            if (entry.status == AnimeDownloadStatus.Queued) {
                                entry.copy(status = AnimeDownloadStatus.Removing)
                            } else {
                                entry
                            }
                        },
                    )
                }
            }
        ) {
            AnimeDownloadJob.cancel(context)
            cancelled.forEach(::deleteAsync)
        }
    }

    fun remove(id: Long) = command { removeEpisode(id) }

    suspend fun removeAndAwait(id: Long) = awaitCommand { removeEpisode(id) }

    private suspend fun removeEpisode(id: Long) {
        editForRemoval { entry -> if (entry.episodeId == id) entry.removing() else entry }
    }

    suspend fun offlineVideo(sourceId: Long, animeId: Long, episodeId: Long, playbackOwner: Any): Video? =
        withContext(Dispatchers.IO) {
            initialized.await()
            val entry = edits.withLock {
                coroutineContext.ensureActive()
                state.value.entries.firstOrNull {
                    it.sourceId == sourceId && it.animeId == animeId && it.episodeId == episodeId &&
                        it.status == AnimeDownloadStatus.Saved
                }?.also { offlinePlaybackEpisodes.getOrPut(playbackOwner) { mutableSetOf() }.add(episodeId) }
            } ?: return@withContext null
            val folder = episodeFolder(entry) ?: return@withContext null
            val file = playableFile(folder) ?: return@withContext null
            // AP3: episodes saved before segments were cleaned on download are cleaned once here.
            if (file.name == AnimeVideoTransfer.PLAYLIST) {
                try {
                    HlsSegmentPrefix.repairFolder(folder, file)
                } catch (e: IOException) {
                    AnimeLog.w("could not clean the saved segments of episode ${entry.episodeId}", e)
                }
            }
            Video(videoUrl = file.absolutePath)
        }

    fun releaseOfflinePlayback(playbackOwner: Any) {
        commands.trySend {
            edits.withLock { offlinePlaybackEpisodes.remove(playbackOwner) }
            removeWatched(emptySet(), emptyMap())
        }
    }

    private suspend fun removeWatched(newly: Set<Long>, watched: Map<Long, Set<Long>>) {
        val enabled = deleteAfterWatched.get() && TsuzukiAnime.enabled
        val pending = state.value.entries.filter { it.status == AnimeDownloadStatus.Saved && it.pendingAutoDelete }
        // An explicit Mark unwatched can beat the watch flow or player close. Recheck before removing saved files.
        val stillWatched = if (enabled) {
            (pending.map { it.episodeId } + newly).filterTo(hashSetOf()) { library.getEpisode(it)?.seen == true }
        } else {
            emptySet()
        }
        editForRemoval { entry ->
            val id = entry.episodeId
            val observedUnwatched = watched[entry.animeId]?.let { id !in it } == true
            when {
                entry.status != AnimeDownloadStatus.Saved || (!entry.pendingAutoDelete && id !in newly) -> entry
                !enabled || id !in stillWatched || observedUnwatched -> entry.copy(pendingAutoDelete = false)
                offlinePlaybackEpisodes.values.any { id in it } -> entry.copy(pendingAutoDelete = true)
                else -> entry.removing()
            }
        }
    }

    private fun AnimeDownloadEntry.removing() = copy(status = AnimeDownloadStatus.Removing, pendingAutoDelete = false)

    private suspend fun editForRemoval(transform: (AnimeDownloadEntry) -> AnimeDownloadEntry) {
        val marked = mutableListOf<AnimeDownloadEntry>()
        if (edit { queue ->
                queue.copy(entries = queue.entries.map { entry ->
                    val changed = transform(entry)
                    if (entry.status != AnimeDownloadStatus.Removing && changed.status == AnimeDownloadStatus.Removing) {
                        marked.add(changed)
                    }
                    changed
                })
            }
        ) {
            marked.forEach(::deleteAsync)
        }
    }

    suspend fun drain(wifiOnly: Boolean) = withContext(Dispatchers.IO) {
        initialized.await()
        while (true) {
            coroutineContext.ensureActive()
            val entry = state.value.takeUnless { it.paused || it.wifiOnly != wifiOnly }
                ?.entries?.firstOrNull { it.status == AnimeDownloadStatus.Queued } ?: break
            disk.withLock {
                transferUnlessRemoved(entry, wifiOnly)
            }
        }
    }

    private suspend fun transferUnlessRemoved(entry: AnimeDownloadEntry, wifiOnly: Boolean) {
        try {
            watchTransfer(entry, wifiOnly)
        } finally {
            mutableProgress.value = null
        }
    }

    private suspend fun watchTransfer(entry: AnimeDownloadEntry, wifiOnly: Boolean) = supervisorScope {
        val id = entry.episodeId
        if (!isQueued(state.value, id, wifiOnly)) return@supervisorScope
        val transfer = async { transfer(entry) }
        val removed = async { state.first { !isQueued(it, id, wifiOnly) } }
        try {
            val bytes = select<Long?> {
                transfer.onAwait { it }
                removed.onAwait { null }
            }
            if (bytes != null) finish(id, AnimeDownloadStatus.Saved, bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            // Source errors can include signed URLs or credentials.
            logcat(LogPriority.WARN) { "Anime episode $id download failed (${e.javaClass.simpleName})" }
            finish(id, AnimeDownloadStatus.Failed, 0L)
        } finally {
            transfer.cancel()
            removed.cancel()
        }
    }

    private suspend fun transfer(entry: AnimeDownloadEntry): Long {
        val folder = episodeFolder(entry) ?: throw IOException("Download storage is unavailable")
        if (playableFile(folder) == null) {
            val episode = checkNotNull(library.getEpisode(entry.episodeId))
            val anime = checkNotNull(library.getAnime(entry.animeId))
            check(episode.animeId == anime.id && anime.source == entry.sourceId)
            val sources = Injekt.get<AnimeSourceManager>()
            sources.ensureLoaded()
            val source = checkNotNull(sources.get(entry.sourceId))
            val client = (source.source as? AnimeHttpSource)?.client ?: network.client
            val preferred = prefs.quality.get()
            // AY1: the same hosters, order and Auto rule as the player, so a download gets the video playback would.
            val hosters = AnimeStreamResolver.hosters(source.source, source.libVersion, episode.toSourceEpisode())
            val loader = AnimeHosterLoader(hosters, { AnimeStreamResolver.hosterVideos(source.source, it) })
            loader.loadAll()
            val candidates = animePlaybackOrder(loader.videos(), preferred)
            var saved = false
            for (candidate in candidates) {
                coroutineContext.ensureActive()
                val video = try {
                    // Downloads used to go out without the source's Referer/User-Agent (seen with the AY0 fixture).
                    AnimeStreamResolver.withSourceHeaders(
                        source.source,
                        AnimeStreamResolver.resolve(source.source, source.libVersion, candidate),
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.WARN) {
                        "Could not resolve an anime download stream (${e.javaClass.simpleName})"
                    }
                    continue
                }
                try {
                    AnimeVideoTransfer(client, video, preferred).download(folder) {
                        mutableProgress.value = entry.episodeId to it
                    }
                    saved = true
                    break
                } catch (_: UnsupportedAnimeDownload) {
                    // A source can offer more than one host or stream format for the same episode.
                }
            }
            if (!saved) throw UnsupportedAnimeDownload()
        }
        return folder.listFiles()?.sumOf(File::length) ?: 0
    }

    private suspend fun finish(id: Long, status: AnimeDownloadStatus, bytes: Long) {
        edit { queue ->
            queue.change(id) {
                if (it.status == AnimeDownloadStatus.Queued) it.copy(status = status, bytes = bytes) else it
            }
        }
    }

    private fun deleteAsync(entry: AnimeDownloadEntry) {
        scope.launch(Dispatchers.IO) {
            initialized.await()
            try {
                disk.withLock {
                    val removing = state.value.entries.any {
                        it.episodeId == entry.episodeId && it.status == AnimeDownloadStatus.Removing
                    }
                    if (!removing) return@withLock
                    val folder = episodeFolder(entry)
                    val deleted = folder != null && (playableFile(folder)?.delete() != false) &&
                        folder.deleteRecursively()
                    if (!deleted) logcat(LogPriority.WARN) { "Could not remove the anime episode files" }
                    edit { queue ->
                        queue.change(entry.episodeId) {
                            if (deleted) null else it.copy(status = AnimeDownloadStatus.Failed)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Could not remove anime download ${entry.episodeId}" }
            }
        }
    }

    private fun command(action: suspend () -> Unit) {
        if (TsuzukiAnime.enabled) commands.trySend(action)
    }

    /** Waits for the queue edit and propagates its failure to the calling screen. */
    private suspend fun awaitCommand(action: suspend () -> Unit) {
        check(TsuzukiAnime.enabled)
        initialized.await()
        coroutineContext.ensureActive()
        val result = CompletableDeferred<Unit>()
        commands.send {
            try {
                action()
                result.complete(Unit)
            } catch (e: CancellationException) {
                result.cancel(e)
                throw e
            } catch (e: Exception) {
                result.completeExceptionally(e)
            }
        }
        result.await()
    }

    private suspend fun edit(transform: (AnimeDownloadQueue) -> AnimeDownloadQueue): Boolean = edits.withLock {
        val old = mutableState.value
        val new = transform(old)
        if (new == old) return@withLock false
        val bytes = Json.encodeToString(new).encodeToByteArray()
        val output = stored.startWrite()
        try {
            output.write(bytes)
            stored.finishWrite(output)
        } catch (e: IOException) {
            stored.failWrite(output)
            throw e
        }
        mutableState.value = new
        true
    }

    private fun episodeFolder(entry: AnimeDownloadEntry): File? =
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.resolve("anime/${entry.sourceId}/${entry.animeId}/${entry.episodeId}")

    private fun playableFile(folder: File): File? =
        listOf(AnimeVideoTransfer.VIDEO, AnimeVideoTransfer.PLAYLIST)
            .asSequence().map(folder::resolve).firstOrNull { it.isFile && it.length() > 0L }

    private fun isQueued(queue: AnimeDownloadQueue, id: Long, wifiOnly: Boolean): Boolean =
        !queue.paused && queue.wifiOnly == wifiOnly &&
            queue.entries.any { it.episodeId == id && it.status == AnimeDownloadStatus.Queued }
}
