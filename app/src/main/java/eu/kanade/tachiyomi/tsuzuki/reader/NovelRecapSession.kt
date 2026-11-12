package eu.kanade.tachiyomi.tsuzuki.reader

import android.content.Context
import android.util.AtomicFile
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.data.download.ChapterContentReader
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalNovelSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.IOException
import kotlin.time.Clock

internal class NovelRecapSession(
    context: Context,
    downloadProvider: DownloadProvider,
    private val historyFor: suspend (Long) -> List<History> = { Injekt.get<GetHistory>().await(it) },
) {
    private val directory = File(context.cacheDir, "novel-recap-excerpts")
    private val downloads = ChapterContentReader(context, downloadProvider)

    @Volatile private var recap: NovelRecap? = null

    fun forChapter(chapterId: Long?): NovelRecap? = recap?.takeIf { it.chapterId == chapterId }

    fun dismiss(chapterId: Long) {
        if (recap?.chapterId == chapterId) recap = null
    }

    /** Called before the novel reader stamps its new history entry, on IO. */
    suspend fun prepare(manga: Manga, chapterId: Long, previous: ReaderChapter, source: Source) {
        try {
            val history = historyFor(manga.id)
            val lastRead = history.mapNotNull { it.readAt?.time }.maxOrNull()
            if (!shouldShowNovelRecap(lastRead, Clock.System.now().toEpochMilliseconds())) return

            val previousModel = previous.chapter.toDomainChapter() ?: return
            val cached = readExcerpt(manga.id, previousModel.id)
            val paragraphs = cached ?: run {
                val content = previous.pages?.firstNotNullOfOrNull { it.text }
                    ?: downloads.readDownloadedContent(manga, previousModel, source)
                    // Local pages carry only a URL; the text is read from the file, as the reader does.
                    ?: (source as? LocalNovelSource)?.let { local ->
                        local.getPageList(previous.chapter).firstOrNull()?.let { local.fetchPageText(it) }
                    }
                content?.let(::novelRecapParagraphs).orEmpty()
            }
            if (paragraphs.isNotEmpty()) recap = NovelRecap(chapterId, paragraphs)
        } catch (e: CancellationException) {
            throw e
        } catch (error: Exception) {
            // A missing or unreadable excerpt never prevents opening the chapter.
            logcat(LogPriority.WARN, error) { "Could not prepare chapter recap" }
        }
    }

    fun cacheExcerpt(mangaId: Long, chapterId: Long, content: String) {
        val paragraphs = novelRecapParagraphs(content)
        if (paragraphs.isEmpty()) return
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
        private const val MAX_ENTRIES = 64
        private const val MAX_BYTES = 24 * 1024
        private val cacheLock = Any()
    }
}
