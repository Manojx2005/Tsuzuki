package eu.kanade.presentation.theme.tsuzuki

import android.content.Context
import android.provider.Settings
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** MotionReader approximation: Activity windows can scale/fade; Compose covers cannot cross windows. */
fun tsuzukiReaderTransition(context: Context, opening: Boolean): Pair<Int, Int>? {
    val theme = Injekt.get<UiPreferences>().appTheme.get()
    if (theme != AppTheme.COLOUR && theme != AppTheme.DOT) return null
    if (Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) {
        return R.anim.tsuzuki_reader_fade_in to R.anim.tsuzuki_reader_fade_out
    }
    return when {
        opening && theme == AppTheme.DOT -> R.anim.tsuzuki_reader_enter_dot to R.anim.tsuzuki_reader_behind_dot
        opening -> R.anim.tsuzuki_reader_enter to R.anim.tsuzuki_reader_behind
        theme == AppTheme.DOT -> R.anim.tsuzuki_reader_return_dot to R.anim.tsuzuki_reader_exit_dot
        else -> R.anim.tsuzuki_reader_return to R.anim.tsuzuki_reader_exit
    }
}
