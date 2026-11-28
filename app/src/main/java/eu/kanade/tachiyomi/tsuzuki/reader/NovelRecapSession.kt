package eu.kanade.tachiyomi.tsuzuki.reader

import android.content.Context
import android.util.AtomicFile
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.data.download.ChapterContentReader
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.isNovelSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import tachiyomi.source.local.LocalNovelSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.time.Clock

internal class NovelRecapSession(
    private val context: Context,
    downloadProvider: DownloadProvider,
    private val historyFor: suspend (Long) -> List<History> = { Injekt.get<GetHistory>().await(it) },
) {
    private val directory = File(context.cacheDir, "novel-recap-excerpts")
    private val downloads = ChapterContentReader(context, downloadProvider)

    @Volatile private var recap: NovelRecap? = null

    /** A chapter reopened mid-way: waiting for its text so the recap can come from just before the resume point. */
    private class PendingResume(val chapterId: Long, val progress: Float, val fallback: List<String>)

    @Volatile private var pendingResume: PendingResume? = null
    private val resumeOverlay = MutableStateFlow<NovelRecap?>(null)

    /** The recap to float over the reader when the chapter resumed past its top. */
    val overlay: StateFlow<NovelRecap?> get() = resumeOverlay

    fun forChapter(chapterId: Long?): NovelRecap? = recap?.takeIf { it.chapterId == chapterId }

    fun dismiss(chapterId: Long) {
        if (recap?.chapterId == chapterId) recap = null
        if (pendingResume?.chapterId == chapterId) pendingResume = null
        resumeOverlay.value = resumeOverlay.value?.takeIf { it.chapterId != chapterId }
    }

    /** Debug builds only: `run-as <app id> touch cache/force-recap` makes the 24 hour rule true. */
    private fun forcedForTesting() = BuildConfig.DEBUG && File(context.cacheDir, "force-recap").exists()

    /**
     * The chapter's raw text, once a viewer has it. For a resumed chapter this is when the overlay recap is
     * built: the prose just before the restored position, or the previous chapter's excerpt when that
     * position is still in the opening paragraphs.
     */
    fun onChapterContent(chapterId: Long, content: String) {
        val pending = pendingResume?.takeIf { it.chapterId == chapterId } ?: return
        pendingResume = null
        val paragraphs = try {
            novelRecapParagraphsBefore(content, pending.progress).ifEmpty { pending.fallback }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            pending.fallback
        }
        if (paragraphs.isNotEmpty()) {
            resumeOverlay.value =
                NovelRecap(chapterId, paragraphs, (pending.progress * 100).roundToInt())
        }
    }

    /**
     * Called before the novel reader stamps its new history entry, on IO. [entryProgress] is the position
     * the viewers will restore (0 for a fresh open); [previous] is null for the first chapter.
     */
    suspend fun prepare(
        manga: Manga,
        chapterId: Long,
        previous: ReaderChapter?,
        source: Source,
        entryProgress: Float = 0f,
    ) {
        try {
            val history = historyFor(manga.id)
            val lastRead = history.mapNotNull { it.readAt?.time }.maxOrNull()
            val due = shouldShowNovelRecap(lastRead, Clock.System.now().toEpochMilliseconds()) || forcedForTesting()
            val mode = novelRecapMode(due, entryProgress)
            if (mode == NovelRecapMode.None) return
            val resume = mode == NovelRecapMode.ResumeOverlay
            val previousModel = previous?.chapter?.toDomainChapter()
            if (previous == null || previousModel == null) {
                if (resume) pendingResume = PendingResume(chapterId, entryProgress, emptyList())
                return
            }
            val cached = readExcerpt(manga.id, previousModel.id)
            val paragraphs = cached ?: run {
                val content = previous.pages?.firstNotNullOfOrNull { it.text }
                    ?: downloads.readDownloadedContent(manga, previousModel, source)
                    // Local pages carry only a URL; the text is read from the file, as the reader does.
                    ?: (source as? LocalNovelSource)?.let { local ->
                        local.getPageList(previous.chapter).firstOrNull()?.let { local.fetchPageText(it) }
                    }
                    // Nothing offline: ask the source once for the previous chapter, so online novels
                    // that were never opened here (or were read before the cache existed) still recap.
                    ?: fetchFromSource(previous, source)
                content?.let(::novelRecapParagraphs).orEmpty()
            }
            if (resume) {
                pendingResume = PendingResume(chapterId, entryProgress, paragraphs)
            } else if (paragraphs.isNotEmpty()) {
                recap = NovelRecap(chapterId, paragraphs)
            }
            if (paragraphs.isNotEmpty() && cached == null) {
                try {
                    writeExcerpt(manga.id, previousModel.id, paragraphs)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // The recap cache is opportunistic.
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (error: Exception) {
            // A missing or unreadable excerpt never prevents opening the chapter.
            logcat(LogPriority.WARN, error) { "Could not prepare chapter recap" }
        }
    }

    /** One short, failure-tolerant request; a slow or broken source just means no recap. */
    private suspend fun fetchFromSource(previous: ReaderChapter, source: Source): String? {
        if (source is StubSource || source is LocalNovelSource || !source.isNovelSource()) return null
        // The fetch runs in its own scope so a source that blocks a thread (JS plugins do) cannot hold
        // the chapter open past the timeout; the late result is simply dropped.
        val fetch = fetchScope.async {
            val page = source.getPageList(previous.chapter).firstOrNull() ?: return@async null
            page.text ?: source.fetchPageText(page)
        }
        return try {
            withTimeoutOrNull(FETCH_TIMEOUT_MS) { fetch.await() } ?: run {
                fetch.cancel()
                null
            }
        } catch (e: CancellationException) {
            fetch.cancel()
            throw e
        } catch (error: Exception) {
            logcat(LogPriority.DEBUG, error) { "Could not fetch the previous chapter for a recap" }
            null
        }
    }

    fun cacheExcerpt(mangaId: Long, chapterId: Long, content: String) {
        val paragraphs = novelRecapParagraphs(content)
        if (paragraphs.isEmpty()) return
        writeExcerpt(mangaId, chapterId, paragraphs)
    }

    private fun writeExcerpt(mangaId: Long, chapterId: Long, paragraphs: List<String>) {
        val bytes = paragraphs.joinToString("\n\n").encodeToByteArray()
        if (bytes.size > MAX_BYTES) return
        synchronized(cacheLock) {
            try {
                directory.mkdirs()
                val file = AtomicFile(File(directory, "$mangaId-$chapterId.txt"))
                val stream = file.startWrite()
                try {
                    stream.write(bytes)
                    file.finishWrite(stream)
                } catch (e: IOException) {
                    file.failWrite(stream)
                    throw e
                }
                directory.listFiles { child -> child.extension == "txt" }.orEmpty()
                    .sortedByDescending { it.lastModified() }
                    .drop(MAX_ENTRIES)
                    .forEach { it.delete() }
            } catch (_: IOException) {
                // The recap cache is opportunistic.
            }
        }
    }

    private fun readExcerpt(mangaId: Long, chapterId: Long): List<String>? = synchronized(cacheLock) {
        try {
            val file = AtomicFile(File(directory, "$mangaId-$chapterId.txt"))
            file.readFully().takeIf { it.size <= MAX_BYTES }?.decodeToString()
                ?.split("\n\n")?.takeIf { it.size in 2..3 && it.none(String::isBlank) }
        } catch (_: IOException) {
            null
        }
    }

    private companion object {
        private const val FETCH_TIMEOUT_MS = 3_000L
        private val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private const val MAX_ENTRIES = 64
        private const val MAX_BYTES = 24 * 1024
        private val cacheLock = Any()
    }
}
