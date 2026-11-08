package eu.kanade.presentation.tsuzuki.reader

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.tsuzuki.tts.TtsSleepTimer
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.TtsController
import eu.kanade.tachiyomi.ui.reader.viewer.text.textview.NovelViewer
import eu.kanade.tachiyomi.ui.reader.viewer.text.webview.NovelWebViewViewer
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

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
): State<ReaderListeningState?> {
    val appLocale = LocalConfiguration.current.locales[0]
    val offlineLabel = stringResource(MR.strings.tsz_tts_voice_offline)
    val onlineLabel = stringResource(MR.strings.tsz_tts_voice_online)
    return produceState<ReaderListeningState?>(null, controller, enabled, appLocale) {
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
                    voice = controller.getCurrentVoiceLabel(appLocale, offlineLabel, onlineLabel),
                    timer = controller.sleepTimer,
                )
            } else {
                null
            }
            delay(300)
        }
    }
}

internal class ReaderListenPanel(
    val fold: ReaderFold?,
    val sessionActive: Boolean,
    val paused: Boolean,
    val content: (@Composable (onClose: () -> Unit) -> Unit)?,
)

@Composable
internal fun rememberTsuzukiListenPanel(
    activity: Activity,
    state: ReaderViewModel.State,
    preferences: ReaderPreferences,
    ttsActive: Boolean,
    ttsPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStepParagraph: (Boolean) -> Unit,
    onSeekParagraph: (Int) -> Unit,
    onPausedChanged: (Boolean) -> Unit,
): ReaderListenPanel {
    val themed = TsuzukiTheme.tokens.style != TsuzukiStyle.NONE
    val fold by rememberReaderFold(activity, themed)
    val controller = when (val viewer = state.viewer) {
        is NovelViewer -> viewer.listeningController
        is NovelWebViewViewer -> viewer.listeningController
        else -> null
    }
    val canDock =
        themed && (fold != null || shouldDockReaderPanel(isTabletUi(), LocalConfiguration.current.screenWidthDp))
    val listening by rememberReaderListeningState(controller, canDock)
    val snapshot = listening
    val content: (@Composable (() -> Unit) -> Unit)? = if (snapshot != null && controller != null) {
        { close ->
            val chapters = state.viewerChapters
            val chapterTitle = listOfNotNull(chapters?.currChapter, chapters?.prevChapter, chapters?.nextChapter)
                .firstOrNull { it.chapter.id == controller.ttsPlaybackChapterId }?.chapter?.name
            TsuzukiReaderListenPanel(
                manga = state.manga,
                chapterTitle = chapterTitle,
                state = snapshot,
                controller = controller,
                preferences = preferences,
                compact = fold != null,
                onTogglePlayback = {
                    if (controller.isPaused()) onResume() else onPause()
                    onPausedChanged(controller.isPaused())
                },
                onStepParagraph = onStepParagraph,
                onSeekParagraph = onSeekParagraph,
                onClose = close,
            )
        }
    } else {
        null
    }
    return ReaderListenPanel(
        fold = fold,
        sessionActive = if (canDock) snapshot != null else ttsActive,
        paused = snapshot?.paused ?: ttsPaused,
        content = content,
    )
}
