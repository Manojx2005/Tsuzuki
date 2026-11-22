package eu.kanade.tachiyomi.tsuzuki.anime.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerPreferences
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.networkStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import java.io.File
import kotlinx.coroutines.CancellationException

/** Private, disposable artwork. Android backup is disabled and app backups only serialize database records. */
internal class AnimeFrameStore(context: Context, scope: CoroutineScope, preferences: PreferenceStore) {
    private val root = File(context.filesDir, "anime-frames")
    private val lock = Mutex()
    private val captureSequences = java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicLong>()
    fun captureSequence(episodeId: Long) = captureSequences.getOrPut(episodeId) { java.util.concurrent.atomic.AtomicLong() }.incrementAndGet()
    private val generations = java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicLong>()
    fun generation(animeId: Long) = generations[animeId]?.get() ?: 0
    private val mutableVersions = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val versions = mutableVersions.asStateFlow()
    val mode = AnimePlayerPreferences(preferences).episodePreviews
    val network = context.networkStateFlow().stateIn(scope, SharingStarted.WhileSubscribed(5_000), context.activeNetworkState())

    init {
        scope.launch(Dispatchers.IO) {
            guarded {
                // A crashed process can leave mpv's uncapped temporary PNG behind. Ignore recent/live writes.
                val staleBefore = System.currentTimeMillis() - 24L * 60 * 60 * 1000
                context.cacheDir.listFiles()?.filter {
                    it.isFile && it.name.matches(Regex("anime-preview-[a-f0-9-]+\\.png")) && it.lastModified() < staleBefore
                }?.forEach { check(it.delete() || !it.exists()) }
                lock.withLock { trim(); publish() }
            }
        }
    }

    fun file(animeId: Long, episodeId: Long) = File(root, "$animeId/$episodeId.jpg")

    /** The async mpv writer owns [raw] until its PNG IEND marker appears. Never decode a partial PNG. */
    suspend fun accept(animeId: Long, episodeId: Long, raw: File, generation: Long, sequence: Long, permitted: () -> Boolean) = withContext(Dispatchers.IO) {
        try {
            var complete = false
            repeat(100) {
                if (!complete) {
                    complete = pngComplete(raw)
                    if (!complete) delay(50)
                }
            }
            if (!complete || !permitted()) return@withContext
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(raw.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext
            var sample = 1
            while (bounds.outWidth / sample > 1280) sample *= 2
            val original = BitmapFactory.decodeFile(raw.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return@withContext
            try {
                val width = minOf(640, original.width)
                val height = (original.height.toLong() * width / original.width).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(original, width, height, true)
                try {
                    val pixels = IntArray(width * height)
                    scaled.getPixels(pixels, 0, width, 0, 0, width, height)
                    if (!frameIsVisible(pixels)) return@withContext
                    lock.withLock {
                        if (!permitted() || generation(animeId) != generation || captureSequences[episodeId]?.get() != sequence) return@withLock
                        val target = file(animeId, episodeId)
                        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
                        val atomic = AtomicFile(target)
                        val stream = atomic.startWrite()
                        try {
                            check(scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream))
                            atomic.finishWrite(stream)
                        } catch (e: Exception) {
                            atomic.failWrite(stream)
                            throw e
                        }
                        // File systems can have coarse mtime resolution; ensure a changed Coil key.
                        val previous = mutableVersions.value[episodeId] ?: 0
                        check(target.setLastModified(maxOf(System.currentTimeMillis(), previous + 1)))
                        trim()
                        publish()
                    }
                } finally {
                    if (scaled !== original) scaled.recycle()
                }
            } finally {
                original.recycle()
            }
        } finally {
            if (raw.exists() && !raw.delete()) logcat(LogPriority.WARN) { "Could not remove temporary anime frame" }
        }
    }

    /** null removes the anime; a retained-ID set removes frames only for deleted episodes. */
    suspend fun prune(animeId: Long, retainedIds: Set<Long>?) = withContext(Dispatchers.IO) {
        guarded {
            lock.withLock {
                generations.getOrPut(animeId) { java.util.concurrent.atomic.AtomicLong() }.incrementAndGet()
                val directory = File(root, animeId.toString())
                directory.listFiles()?.forEach { file ->
                    if (retainedIds == null || file.nameWithoutExtension.toLongOrNull() !in retainedIds) {
                        check(file.delete() || !file.exists())
                    }
                }
                if (directory.listFiles()?.isEmpty() == true) check(directory.delete())
                publish()
            }
        }
    }

    private fun trim() {
        val files = root.walkTopDown().filter { it.isFile && it.extension == "jpg" }.toList()
        val remove = previewFilesToEvict(files.map { PreviewFile(it.path, it.length(), it.lastModified()) }, 100L * 1024 * 1024).toSet()
        files.filter { it.path in remove }.forEach { check(it.delete() || !it.exists()) }
    }

    private fun publish() {
        mutableVersions.value = root.walkTopDown().filter { it.isFile && it.extension == "jpg" }
            .mapNotNull { file -> file.nameWithoutExtension.toLongOrNull()?.let { it to file.lastModified() } }.toMap()
    }

    private fun pngComplete(file: File): Boolean {
        if (!file.isFile || file.length() < 12) return false
        return java.io.RandomAccessFile(file, "r").use {
            it.seek(it.length() - 12)
            val end = ByteArray(12)
            it.readFully(end)
            end.contentEquals(byteArrayOf(0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126))
        }
    }

    private suspend fun guarded(block: suspend () -> Unit) {
        try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Could not maintain anime preview frames" }
        }
    }
}
