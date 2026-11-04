package eu.kanade.presentation.theme.tsuzuki

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.drawToBitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import kotlin.math.hypot
import kotlin.math.max

/** The screenshot survives Activity recreation; it is consumed by the new MainActivity only. */
object TsuzukiThemeReveal {
    private data class Snapshot(val bitmap: Bitmap, val origin: Offset)
    private var pending: Snapshot? = null

    private fun prepare(context: Context, from: AppTheme, to: AppTheme, origin: Offset) {
        if (from == to || from !in themes || to !in themes || !origin.isSpecified) return
        val activity = context.activity() ?: return
        if (Settings.Global.getFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) ==
            0f
        ) {
            return
        }
        val decor = activity.window.decorView
        if (!decor.isLaidOut || decor.width == 0 || decor.height == 0) return
        val bitmap = try {
            decor.drawToBitmap()
        } catch (_: RuntimeException) {
            // A hardware-only bitmap cannot be drawn into a software snapshot: keep the normal switch.
            return
        }
        pending?.bitmap?.recycle()
        pending = Snapshot(bitmap, origin)
    }

    /** Call after installing content, so the old window covers the first frame of the new theme. */
    fun attach(activity: BaseActivity) {
        val snapshot = pending ?: return
        pending = null
        val decor = activity.window.decorView as? ViewGroup ?: run {
            snapshot.bitmap.recycle()
            return
        }
        val overlay = RevealView(activity, snapshot.bitmap, snapshot.origin)
        var animator: ValueAnimator? = null
        var preDraw: ViewTreeObserver.OnPreDrawListener? = null
        var disposed = false
        fun dispose() {
            if (disposed) return
            disposed = true
            preDraw?.let { if (decor.viewTreeObserver.isAlive) decor.viewTreeObserver.removeOnPreDrawListener(it) }
            animator?.cancel()
            decor.overlay.remove(overlay)
            snapshot.bitmap.recycle()
        }
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = dispose()
        })
        preDraw = ViewTreeObserver.OnPreDrawListener {
            preDraw?.let { decor.viewTreeObserver.removeOnPreDrawListener(it) }
            preDraw = null
            overlay.layout(0, 0, decor.width, decor.height)
            decor.overlay.add(overlay)
            val x = snapshot.origin.x.coerceIn(0f, decor.width.toFloat())
            val y = snapshot.origin.y.coerceIn(0f, decor.height.toFloat())
            val radius = hypot(max(x, decor.width - x), max(y, decor.height - y))
            animator = ValueAnimator.ofFloat(0f, radius).apply {
                duration = TsuzukiMotion.THEME_MS.toLong()
                interpolator = android.animation.TimeInterpolator { TsuzukiMotion.Enter.transform(it) }
                addUpdateListener { overlay.radius = it.animatedValue as Float }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = dispose()
                })
                start()
            }
            true
        }
        decor.viewTreeObserver.addOnPreDrawListener(preDraw)
    }

    private val themes = setOf(AppTheme.COLOUR, AppTheme.DOT)

    @Composable
    fun click(from: AppTheme, to: AppTheme, onClick: () -> Unit): ThemeRevealClick {
        val context = LocalContext.current
        var origin by remember { mutableStateOf(Offset.Unspecified) }
        return ThemeRevealClick(
            modifier = Modifier.onGloballyPositioned { origin = it.boundsInWindow().center },
            onClick = {
                prepare(context, from, to, origin)
                onClick()
            },
        )
    }
}

data class ThemeRevealClick(val modifier: Modifier, val onClick: () -> Unit)

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** A hole grows in the old theme, exposing the real new window underneath. No input is intercepted. */
private class RevealView(context: Context, private val bitmap: Bitmap, private val origin: Offset) : View(context) {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val hole = Path()
    var radius = 0f
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        hole.reset()
        hole.addCircle(origin.x, origin.y, radius, Path.Direction.CW)
        canvas.clipOutPath(hole)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()
    }
}
