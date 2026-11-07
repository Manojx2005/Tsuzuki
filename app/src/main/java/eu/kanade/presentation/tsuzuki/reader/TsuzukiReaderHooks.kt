package eu.kanade.presentation.tsuzuki.reader

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiReaderTransition
import eu.kanade.presentation.tsuzuki.novel.FindResultState
import eu.kanade.presentation.tsuzuki.novel.TsuzukiReaderContents
import eu.kanade.presentation.tsuzuki.novel.TsuzukiReaderFindBar
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.ReadingDays
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.text.webview.NovelWebViewViewer
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate

internal fun Activity.applyTsuzukiReaderTransition(opening: Boolean, beforeApply: () -> Unit = {}) {
    val themedAnimations = tsuzukiReaderTransition(this, opening)
    beforeApply()
    val (enter, exit) = readerTransitionAnimations(opening, themedAnimations)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(
            if (opening) Activity.OVERRIDE_TRANSITION_OPEN else Activity.OVERRIDE_TRANSITION_CLOSE,
            enter,
            exit,
        )
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(enter, exit)
    }
}

internal fun readerTransitionAnimations(opening: Boolean, themedAnimations: Pair<Int, Int>?): Pair<Int, Int> =
    themedAnimations ?: if (opening) {
        R.anim.shared_axis_x_push_enter to R.anim.shared_axis_x_push_exit
    } else {
        R.anim.shared_axis_x_pop_enter to R.anim.shared_axis_x_pop_exit
    }

/** Activity-owned requests survive a composition change. Drawer and find state stay in the reader composition. */
internal class TsuzukiReaderChromeRequests {
    private var openContentsRequested by mutableStateOf(false)

    fun openContents() {
        openContentsRequested = true
    }

    @Composable
    fun bind(viewModel: ReaderViewModel, state: ReaderViewModel.State): TsuzukiReaderChrome {
        val contentsVisible = rememberSaveable { mutableStateOf(false) }
        val contents = remember(contentsVisible) { TsuzukiReaderContentsHook(contentsVisible) }
        val openContents = contents.openAction()
        LaunchedEffect(openContentsRequested) {
            if (openContentsRequested) {
                openContentsRequested = false
                openContents?.invoke()
            }
        }
        val find = remember { TsuzukiReaderFindHook() }
        return TsuzukiReaderChrome(
            onOpenContents = openContents,
            onFind = find.openAction(state.viewer) { viewModel.showMenus(false) },
            hasNextChapter = viewModel.adjacentChapter(state, forward = true) != null,
            hasPreviousChapter = viewModel.adjacentChapter(state, forward = false) != null,
        ) { activity ->
            contents.Drawer(activity, viewModel, state)
            find.Bar(state.viewer)
        }
    }
}

internal class TsuzukiReaderChrome(
    val onOpenContents: (() -> Unit)?,
    val onFind: (() -> Unit)?,
    val hasNextChapter: Boolean,
    val hasPreviousChapter: Boolean,
    private val overlay: @Composable (ReaderActivity) -> Unit,
) {
    @Composable
    fun Overlay(activity: ReaderActivity) = overlay(activity)
}

private class TsuzukiReaderContentsHook(visibleState: MutableState<Boolean>) {
    private var visible by visibleState

    @Composable
    private fun docked(): Boolean =
        shouldDockReaderPanel(isTabletUi(), LocalConfiguration.current.screenWidthDp)

    @Composable
    fun openAction(): (() -> Unit)? {
        // The docked panel toggles; the modal drawer only opens.
        val toggle = docked()
        return { visible = !toggle || !visible }.takeIf { TsuzukiTheme.tokens.style != TsuzukiStyle.NONE }
    }

    @Composable
    fun Drawer(activity: ReaderActivity, viewModel: ReaderViewModel, state: ReaderViewModel.State) {
        if (docked()) {
            TsuzukiReaderPanel(
                activity = activity,
                visible = visible && TsuzukiTheme.tokens.style != TsuzukiStyle.NONE,
                widthDp = readerPanelWidthDp(LocalConfiguration.current.screenWidthDp),
                manga = state.manga,
                currentChapterId = state.contentsChapterId(),
                loadQuotes = viewModel::getQuotes,
                onDismissRequest = { visible = false },
                onOpenChapter = { chapterId -> openChapter(activity, viewModel.state.value, chapterId) },
            )
            return
        }
        TsuzukiReaderContents(
            visible = visible,
            manga = state.manga,
            currentChapterId = state.contentsChapterId(),
            onDismissRequest = { visible = false },
            onOpenChapter = { chapterId -> openChapter(activity, viewModel.state.value, chapterId) },
        )
    }

    private fun openChapter(activity: ReaderActivity, state: ReaderViewModel.State, chapterId: Long) {
        val mangaId = state.manga?.id ?: return
        if (chapterId == state.contentsChapterId()) return
        // Finish the singleTask reader so the chapter opens in a fresh activity.
        activity.finish()
        activity.startActivity(ReaderActivity.newIntent(activity, mangaId, chapterId))
    }
}

/** Find in chapter: only the WebView renderer can search (WebView.findAllAsync); the action is hidden otherwise. */
private class TsuzukiReaderFindHook {
    private var visible by mutableStateOf(false)
    private val result = FindResultState()

    fun openAction(viewer: Viewer?, beforeOpen: () -> Unit): (() -> Unit)? =
        if (viewer is NovelWebViewViewer) {
            {
                beforeOpen()
                visible = true
            }
        } else {
            null
        }

    @Composable
    fun Bar(viewer: Viewer?) {
        val webViewer = viewer as? NovelWebViewViewer
        if (!visible || webViewer == null) return
        val close = {
            webViewer.clearFind()
            result.active = 0
            result.total = 0
            visible = false
        }
        BackHandler(onBack = close)
        androidx.compose.runtime.DisposableEffect(webViewer) { onDispose { webViewer.clearFind() } }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            TsuzukiReaderFindBar(
                onSearch = { query ->
                    webViewer.find(query) { active, total ->
                        result.active = active
                        result.total = total
                    }
                },
                onNext = webViewer::findNext,
                onClose = close,
                resultState = result,
            )
        }
    }
}

internal fun ReaderViewModel.State.contentsChapterId(): Long? = (novelVisibleChapter ?: currentChapter?.chapter)?.id

internal fun recordTsuzukiReadingTime(sessionReadDuration: Long) {
    // History stores only the last read for each chapter. Keep the daily streak total separately.
    Injekt.get<ReadingDays>().add(LocalDate.now(), sessionReadDuration)
}
