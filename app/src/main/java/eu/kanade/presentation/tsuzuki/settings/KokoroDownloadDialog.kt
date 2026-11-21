package eu.kanade.presentation.tsuzuki.settings

import android.content.Context
import android.os.StatFs
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.tsuzuki.tts.KokoroPack
import eu.kanade.tachiyomi.tsuzuki.tts.KokoroPackDownload
import eu.kanade.tachiyomi.tsuzuki.tts.KokoroPackImporter
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private sealed interface DownloadState {
    data class Running(val done: Long, val total: Long) : DownloadState
    data class Paused(val done: Long, val total: Long) : DownloadState
    data object Installing : DownloadState
    data class Failed(val message: String) : DownloadState
}

/** The zip is kept between Pause and Resume, next to the install folder, so it shares its volume. */
internal fun kokoroDownloadFile(context: Context) = File(context.filesDir, "tts/kokoro-cmu-v1.download.zip")

/**
 * Debug Kokoro listening test: download the voice pack from [url] with progress, Pause/Resume and Cancel, then
 * install it through [KokoroPackImporter] (every file checked before anything is replaced). Owner 2026-11-15:
 * "add a downloading screen for it". Layout follows board 5 Voices' downloading tile (progress, "58 of 147 MB ·
 * Wi-Fi", Pause/Cancel, free-space line). The release Voices page (NT3) will run this under WorkManager so it
 * survives process death; here the download lives while the dialog is open.
 */
@Composable
internal fun KokoroDownloadDialog(url: String, onDismiss: () -> Unit, onInstalled: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val target = remember { kokoroDownloadFile(context) }
    var state by remember { mutableStateOf<DownloadState>(DownloadState.Running(target.lengthOrZero(), -1)) }
    var job by remember { mutableStateOf<Job?>(null) }
    // KOKORO-TRANSFER-F: every Resume/Pause/Cancel bumps this; callbacks from an older transfer are ignored.
    val generation = remember { AtomicInteger() }
    val notPublished = stringResource(MR.strings.tsz_kokoro_download_not_published)

    fun start() {
        val previous = job
        val current = generation.incrementAndGet()
        val knownTotal = (state as? DownloadState.Paused)?.total ?: -1
        state = DownloadState.Running(target.lengthOrZero(), knownTotal)
        job = scope.launch {
            // The old writer must release the partial file before this one opens it.
            previous?.cancelAndJoin()
            fun update(next: DownloadState) {
                if (generation.get() == current) state = next
            }
            try {
                val client = Injekt.get<NetworkHelper>().client
                KokoroPackDownload(target) { offset ->
                    val request = Request.Builder().url(url)
                        .apply { if (offset > 0) header("Range", "bytes=$offset-") }
                        .build()
                    val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                    val body = response.body
                    KokoroPackDownload.Opened(
                        code = response.code,
                        length = body.contentLength(),
                        body = body.byteStream(),
                        rangeStart = KokoroPackDownload.rangeStart(response.header("Content-Range")),
                    )
                }.run { done, total -> update(DownloadState.Running(done, total)) }
                if (generation.get() != current) return@launch
                state = DownloadState.Installing
                withContext(Dispatchers.IO) {
                    target.inputStream().use { KokoroPackImporter(KokoroPack(context)).import(it) }
                    target.delete()
                }
                onInstalled()
            } catch (error: CancellationException) {
                throw error
            } catch (error: KokoroPackDownload.NotPublishedException) {
                update(DownloadState.Failed(notPublished))
            } catch (error: Exception) {
                update(DownloadState.Failed(error.message ?: error.javaClass.simpleName))
            }
        }
    }

    LaunchedEffect(Unit) { start() }

    val freeSpace = remember { StatFs(context.filesDir.path).availableBytes }
    val installedBytes = remember { runCatching { KokoroPack(context).expectedSizes().values.sum() }.getOrDefault(0) }
    val tokens = TsuzukiTheme.tokens
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(MR.strings.tsz_kokoro_download_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (val current = state) {
                    is DownloadState.Running, is DownloadState.Paused -> {
                        val (done, total) = when (current) {
                            is DownloadState.Running -> current.done to current.total
                            is DownloadState.Paused -> current.done to current.total
                            else -> 0L to -1L
                        }
                        TsuzukiProgressBar(if (total > 0) done.toFloat() / total else 0f)
                        val network = stringResource(
                            if (context.isConnectedToWifi()) {
                                MR.strings.tsz_kokoro_download_wifi
                            } else {
                                MR.strings.tsz_kokoro_download_mobile
                            },
                        )
                        Text(
                            if (total > 0) {
                                stringResource(
                                    MR.strings.tsz_kokoro_download_progress,
                                    mb(done),
                                    mb(total),
                                    network,
                                )
                            } else {
                                stringResource(MR.strings.tsz_kokoro_download_starting)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (current is DownloadState.Paused) {
                            Text(stringResource(MR.strings.tsz_kokoro_download_paused), color = tokens.mutedText)
                        }
                    }
                    DownloadState.Installing -> {
                        TsuzukiProgressBar(1f)
                        Text(stringResource(MR.strings.tsz_kokoro_test_importing))
                    }
                    is DownloadState.Failed -> Text(current.message)
                }
                val zipBytes = (state as? DownloadState.Running)?.total?.takeIf { it > 0 }
                    ?: (state as? DownloadState.Paused)?.total?.takeIf { it > 0 }
                Text(
                    stringResource(
                        MR.strings.tsz_kokoro_download_space,
                        Formatter.formatShortFileSize(context, freeSpace),
                        Formatter.formatShortFileSize(
                            context,
                            KokoroPackDownload.spaceNeeded(zipBytes ?: installedBytes, installedBytes),
                        ),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.mutedText,
                )
            }
        },
        confirmButton = {
            when (val current = state) {
                is DownloadState.Running -> TextButton(onClick = {
                    val transfer = job
                    val pausing = generation.incrementAndGet()
                    scope.launch {
                        // Show Paused (and offer Resume) only once the old writer has stopped.
                        transfer?.cancelAndJoin()
                        if (generation.get() == pausing) {
                            state = DownloadState.Paused(target.lengthOrZero(), current.total)
                        }
                    }
                }) { Text(stringResource(MR.strings.tsz_kokoro_download_pause)) }
                is DownloadState.Paused, is DownloadState.Failed -> TextButton(onClick = ::start) {
                    Text(stringResource(MR.strings.tsz_kokoro_download_resume))
                }
                DownloadState.Installing -> Unit
            }
        },
        dismissButton = {
            if (state != DownloadState.Installing) {
                TextButton(onClick = {
                    val transfer = job
                    generation.incrementAndGet()
                    scope.launch {
                        // Delete the partial file only after the writer has let go of it.
                        transfer?.cancelAndJoin()
                        target.delete()
                        onDismiss()
                    }
                }) { Text(stringResource(MR.strings.action_cancel)) }
            }
        },
    )
}

private fun File.lengthOrZero() = if (isFile) length() else 0L

private fun mb(bytes: Long) = "%.0f".format(bytes / 1_048_576.0)
