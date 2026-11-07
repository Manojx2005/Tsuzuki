package eu.kanade.tachiyomi.tsuzuki.reader

import android.app.Activity
import android.os.Build
import androidx.core.view.WindowInsetsCompat

/** Top and bottom space, in dp, that text must keep clear of: the notch / status bar area and the gesture bar. */
data class NovelSafeInsets(val topDp: Int, val bottomDp: Int) {
    companion object {
        val NONE = NovelSafeInsets(0, 0)
    }
}

/**
 * The novel reader hides the system bars, so without this the first lines sit under the camera notch. Like LNReader,
 * the user's top / bottom margins are measured from below the notch and status bar area and above the gesture bar.
 * Uses the insets the bars would have even while hidden, so text doesn't jump when the menu shows them.
 */
fun novelSafeInsets(activity: Activity): NovelSafeInsets {
    val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.windowManager.currentWindowMetrics.windowInsets
    } else {
        activity.window?.decorView?.rootWindowInsets
    } ?: return NovelSafeInsets.NONE
    val insets = WindowInsetsCompat.toWindowInsetsCompat(raw)
    val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
    val status = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())
    val navigation = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
    val density = activity.resources.displayMetrics.density
    return novelSafeInsetsDp(
        cutoutTopPx = cutout.top,
        statusTopPx = status.top,
        cutoutBottomPx = cutout.bottom,
        navigationBottomPx = navigation.bottom,
        density = density,
    )
}

internal fun novelSafeInsetsDp(
    cutoutTopPx: Int,
    statusTopPx: Int,
    cutoutBottomPx: Int,
    navigationBottomPx: Int,
    density: Float,
): NovelSafeInsets {
    if (density <= 0f) return NovelSafeInsets.NONE
    val top = maxOf(cutoutTopPx, statusTopPx).coerceAtLeast(0)
    val bottom = maxOf(cutoutBottomPx, navigationBottomPx).coerceAtLeast(0)
    return NovelSafeInsets(
        topDp = kotlin.math.ceil(top / density).toInt(),
        bottomDp = kotlin.math.ceil(bottom / density).toInt(),
    )
}
