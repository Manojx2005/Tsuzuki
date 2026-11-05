package eu.kanade.presentation.tsuzuki.manga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/** A recycled transition gets fresh state; a completed old storage query cannot update its replacement. */
@Composable
internal fun rememberReaderDownloadStatus(
    requestKey: Any,
    initiallyDownloaded: Boolean,
    lookup: (() -> Boolean)?,
): State<Boolean> = key(requestKey) {
    produceState(initiallyDownloaded) {
        if (lookup != null) {
            value = withContext(Dispatchers.IO) {
                try {
                    lookup()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to check the next reader chapter's download" }
                    false
                }
            }
        }
    }
}
