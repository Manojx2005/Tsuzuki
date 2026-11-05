package eu.kanade.presentation.tsuzuki.reader

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiReaderTransition
import eu.kanade.presentation.tsuzuki.novel.TsuzukiReaderContents
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.ReadingDays
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
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

@Composable
internal fun rememberTsuzukiReaderContents() = remember { TsuzukiReaderContentsHook() }

internal class TsuzukiReaderContentsHook {
    private var visible by mutableStateOf(false)

    @Composable
    fun openAction(): (() -> Unit)? =
        { visible = true }.takeIf { TsuzukiTheme.tokens.style != TsuzukiStyle.NONE }

    @Composable
    fun Drawer(activity: ReaderActivity, viewModel: ReaderViewModel, state: ReaderViewModel.State) {
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

internal fun ReaderViewModel.State.contentsChapterId(): Long? = (novelVisibleChapter ?: currentChapter?.chapter)?.id

internal fun recordTsuzukiReadingTime(sessionReadDuration: Long) {
    // History stores only the last read for each chapter. Keep the daily streak total separately.
    Injekt.get<ReadingDays>().add(LocalDate.now(), sessionReadDuration)
}
