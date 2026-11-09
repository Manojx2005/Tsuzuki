package eu.kanade.presentation.tsuzuki.reader

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.PixelCopy
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.doOnPreDraw
import eu.kanade.presentation.theme.tsuzuki.tsuzukiReaderTransition
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Keeps the last drawn viewport over the reader while it loads and lays out the next chapter. */
internal class NovelChapterEndTransition(
    private val activity: ReaderActivity,
    private val container: FrameLayout,
) {
    private class Switch(val fromId: Long, val targetId: Long, val animations: Pair<Int, Int>)

    private var current by mutableStateOf<Switch?>(null)
    private var outgoing: ImageView? = null
    private var entering: View? = null

    val isPending: Boolean get() = current != null

    fun isLeaving(chapterId: Long?): Boolean = chapterId != null && current?.fromId == chapterId

    fun start(scope: CoroutineScope, chapters: ViewerChapters, onRejected: () -> Unit = {}): Boolean {
        if (isPending) return false
        val from = chapters.currChapter.chapter.id ?: return false
        val target = chapters.nextChapter?.chapter?.id ?: return false
        val animations = tsuzukiReaderTransition(activity, opening = true) ?: return false
        val switch = Switch(from, target, animations)
        current = switch

        scope.launch {
            // Give the pressed state a frame to draw before copying. A surface that cannot be
            // copied must not delay navigation indefinitely.
            val frame = withTimeoutOrNull(150L) { captureViewport() }
            if (current !== switch) return@launch
            if (frame != null) {
                outgoing = ImageView(activity).apply {
                    scaleType = ImageView.ScaleType.FIT_XY
                    setImageBitmap(frame)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    isClickable = true
                    container.addView(
                        this,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                }
            }
            if (!activity.loadNextChapterFromEnd(from) && current === switch &&
                activity.viewModel.state.value.viewerChapters?.currChapter?.chapter?.id != target
            ) {
                clear()
                onRejected()
            }
        }
        return true
    }

    fun onChapterChanging(chapterId: Long?) {
        if (current?.targetId != chapterId) clear()
    }

    fun revealWhenReady(content: View, chapterId: Long? = null, animate: Boolean = true) {
        val switch = current ?: return
        if (animate && switch.targetId != chapterId) return
        fun reveal() {
            if (current !== switch) return
            val previousFrame = outgoing
            if (!animate || previousFrame == null) {
                clear()
                return
            }

            current = null
            entering = content
            content.startAnimation(AnimationUtils.loadAnimation(activity, switch.animations.first))
            previousFrame.startAnimation(
                AnimationUtils.loadAnimation(activity, switch.animations.second).apply {
                    setAnimationListener(
                        object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation) = Unit
                            override fun onAnimationRepeat(animation: Animation) = Unit
                            override fun onAnimationEnd(animation: Animation) {
                                if (outgoing === previousFrame) {
                                    container.removeView(previousFrame)
                                    outgoing = null
                                    entering = null
                                }
                            }
                        },
                    )
                },
            )
        }

        if (content is WebView) {
            content.postVisualStateCallback(
                switch.targetId,
                object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) = reveal()
                },
            )
        } else {
            content.doOnPreDraw { reveal() }
        }
    }

    fun clear() {
        current = null
        entering?.clearAnimation()
        entering = null
        val previousFrame = outgoing
        outgoing = null
        previousFrame?.clearAnimation()
        previousFrame?.let(container::removeView)
    }

    private suspend fun captureViewport(): Bitmap? = suspendCancellableCoroutine { continuation ->
        // The first callback runs before traversal, so request the buffer on the following frame.
        container.postOnAnimation {
            container.postOnAnimation capture@{
                if (!continuation.isActive) return@capture
                val handler = container.handler
                if (handler == null || container.width == 0 || container.height == 0) {
                    continuation.resume(null)
                    return@capture
                }
                val position = IntArray(2).also(container::getLocationInWindow)
                val (x, y) = position
                val width = container.width
                val height = container.height
                val source = Rect(x, y, x + width, y + height)
                val scale = (1600f / maxOf(width, height)).coerceAtMost(1f)
                val bitmap = Bitmap.createBitmap(
                    (width * scale).roundToInt().coerceAtLeast(1),
                    (height * scale).roundToInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
                try {
                    PixelCopy.request(activity.window, source, bitmap, { result ->
                        if (continuation.isActive) {
                            continuation.resume(bitmap.takeIf { result == PixelCopy.SUCCESS })
                        }
                    }, handler)
                } catch (_: IllegalArgumentException) {
                    // The window can lose its backing surface between the tap and the copy.
                    continuation.resume(null)
                }
            }
        }
    }
}
