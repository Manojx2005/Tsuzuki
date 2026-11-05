package eu.kanade.tachiyomi.tsuzuki.download

import eu.kanade.tachiyomi.data.download.DownloadManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Whether the chapter's download exists in storage, looked up on [dispatcher] rather than on the caller's thread.
 *
 * This is the `skipCache = true` lookup of [DownloadManager.isChapterDownloaded], which resolves the source, manga and
 * chapter folders through the storage provider (a content resolver query on a document tree). Callers that may run on
 * the main thread, such as the reader's chapter transition, must use this instead of the synchronous call.
 */
suspend fun DownloadManager.isChapterOnDisk(
    chapterName: String,
    chapterScanlator: String?,
    chapterUrl: String,
    mangaTitle: String,
    sourceId: Long,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): Boolean = withContext(dispatcher) {
    isChapterDownloaded(chapterName, chapterScanlator, chapterUrl, mangaTitle, sourceId, skipCache = true)
}
