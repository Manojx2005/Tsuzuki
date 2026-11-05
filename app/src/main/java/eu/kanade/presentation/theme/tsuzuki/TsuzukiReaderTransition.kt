package eu.kanade.presentation.theme.tsuzuki

import android.content.Context
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** MotionReader approximation: Activity windows can scale/fade; Compose covers cannot cross windows. */
fun tsuzukiReaderTransition(context: Context, opening: Boolean): Pair<Int, Int>? {
    val theme = Injekt.get<UiPreferences>().appTheme.get()
    if (theme != AppTheme.COLOUR && theme != AppTheme.DOT) return null
    if (tsuzukiMotionReduced(context)) {
        return R.anim.tsuzuki_reader_fade_in to R.anim.tsuzuki_reader_fade_out
    }
    return when {
        // Dot uses the smooth curve too; the stepped path looked like dropped frames (owner, 2026-11-06).
        opening -> R.anim.tsuzuki_reader_enter to R.anim.tsuzuki_reader_behind
        else -> R.anim.tsuzuki_reader_return to R.anim.tsuzuki_reader_exit
    }
}
