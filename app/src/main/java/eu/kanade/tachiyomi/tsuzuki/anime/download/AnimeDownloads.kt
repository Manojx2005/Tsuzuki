package eu.kanade.tachiyomi.tsuzuki.anime.download

import android.content.Context
import android.os.Environment
import android.util.AtomicFile
import androidx.work.ExistingWorkPolicy
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeStreamResolver
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerPreferences
import eu.kanade.tachiyomi.tsuzuki.anime.player.rankAnimeVideos
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceEpisode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
}

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

    init {
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

    suspend fun protectedEpisodeIds(animeId: Long): Set<Long> {
        initialized.await()
        return state.value.protectedEpisodeIds(animeId)
    }

    fun restoreWhenEnabled() = command {
        if (!TsuzukiAnime.enabled) return@command
        val restored = state.value
        restored.entries.filter { it.status == AnimeDownloadStatus.Removing }.forEach(::deleteAsync)
        if (restored.hasQueuedEpisodes && !restored.paused) {
            AnimeDownloadJob.schedule(context, restored.wifiOnly, ExistingWorkPolicy.KEEP)
        }
    }

    fun enqueue(sourceId: Long, sourceName: String, animeTitle: String, episodes: List<Episode>) = command {
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

    fun retry(id: Long) = command {
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

    fun remove(id: Long) = command {
        val entry = state.value.entries.firstOrNull { it.episodeId == id } ?: return@command
        if (edit { queue -> queue.change(id) { it.copy(status = AnimeDownloadStatus.Removing) } }) {
            deleteAsync(entry)
        }
    }

    suspend fun offlineVideo(sourceId: Long, animeId: Long, episodeId: Long): Video? = withContext(Dispatchers.IO) {
        initialized.await()
        val entry = state.value.entries.firstOrNull {
            it.sourceId == sourceId && it.animeId == animeId && it.episodeId == episodeId &&
                it.status == AnimeDownloadStatus.Saved
        } ?: return@withContext null
        val file = episodeFolder(entry)?.let(::playableFile)
        if (file != null) {
            Video(videoUrl = file.absolutePath)
        } else {
            null
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
            val candidates = rankAnimeVideos(
                AnimeStreamResolver.videos(source.source, source.libVersion, episode.toSourceEpisode()),
                preferred,
            )
            var saved = false
            for (candidate in candidates) {
                coroutineContext.ensureActive()
                val video = try {
                    AnimeStreamResolver.resolve(source.source, source.libVersion, candidate)
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
