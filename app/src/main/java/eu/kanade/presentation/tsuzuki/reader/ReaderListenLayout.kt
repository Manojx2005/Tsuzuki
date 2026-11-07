package eu.kanade.presentation.tsuzuki.reader

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import eu.kanade.tachiyomi.tsuzuki.tts.TtsSleepTimer
import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.TtsController
import kotlinx.coroutines.delay

internal data class ReaderFold(val top: Int, val bottom: Int)

internal fun readerFold(halfOpened: Boolean, horizontal: Boolean, top: Int, bottom: Int): ReaderFold? =
    if (halfOpened && horizontal && top > 0 && bottom >= top) ReaderFold(top, bottom) else null

internal data class FoldedReaderPane(val readerBottomInset: Int, val panelHeight: Int)

internal fun foldedReaderPane(fold: ReaderFold, windowHeight: Int): FoldedReaderPane? =
    if (fold.top > 0 && fold.bottom >= fold.top && fold.bottom < windowHeight) {
        FoldedReaderPane(windowHeight - fold.top, windowHeight - fold.bottom)
    } else {
        null
    }

@Composable
internal fun rememberReaderFold(activity: Activity, enabled: Boolean): State<ReaderFold?> =
    produceState<ReaderFold?>(null, activity, enabled) {
        value = null
        if (!enabled) return@produceState
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { layout ->
            value = layout.displayFeatures.filterIsInstance<FoldingFeature>().firstNotNullOfOrNull { feature ->
                readerFold(
                    halfOpened = feature.state == FoldingFeature.State.HALF_OPENED,
                    horizontal = feature.orientation == FoldingFeature.Orientation.HORIZONTAL,
                    top = feature.bounds.top,
                    bottom = feature.bounds.bottom,
                )
            }
        }
    }

internal data class ListeningParagraphPosition(val index: Int, val count: Int) {
    val progress: Float get() = (index + 1).toFloat() / count
}

internal fun listeningParagraphPosition(index: Int, count: Int): ListeningParagraphPosition? =
    count.takeIf { it > 0 }?.let { ListeningParagraphPosition(index.coerceIn(0, it - 1), it) }

internal data class ReaderListeningState(
    val paused: Boolean,
    val position: ListeningParagraphPosition?,
    val paragraphs: List<String>,
    val voice: String,
    val timer: TtsSleepTimer,
)

@Composable
internal fun rememberReaderListeningState(
    controller: TtsController?,
    enabled: Boolean,
): State<ReaderListeningState?> = produceState<ReaderListeningState?>(null, controller, enabled) {
    value = null
    if (controller == null || !enabled) return@produceState
    while (true) {
        val paused = controller.isPaused()
        value = if (paused || controller.isTtsAutoPlay || controller.isSpeaking() || controller.isStarting()) {
            val (index, count) = controller.getParagraphProgress()
            ReaderListeningState(
                paused = paused,
                position = listeningParagraphPosition(index, count),
                paragraphs = controller.listenParagraphs,
                voice = controller.getCurrentVoiceName(),
                timer = controller.sleepTimer,
            )
        } else {
            null
        }
        delay(300)
    }
}
