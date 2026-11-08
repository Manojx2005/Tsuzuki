package eu.kanade.tachiyomi.tsuzuki.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.View.MeasureSpec
import android.widget.TextView

internal class NovelIllustration : Drawable() {
    var image: Drawable? = null

    fun fitToWidth(width: Int) {
        val drawable = image ?: return
        val height = if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
            (drawable.intrinsicHeight * (width.toFloat() / drawable.intrinsicWidth)).toInt().coerceAtLeast(1)
        } else {
            bounds.height()
        }
        if (drawable.bounds.width() == width && drawable.bounds.height() == height &&
            bounds.width() == width && bounds.height() == height
        ) {
            return
        }
        drawable.setBounds(0, 0, width, height)
        setBounds(0, 0, width, height)
    }

    override fun draw(canvas: Canvas) {
        image?.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        image?.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        image?.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSPARENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int = PixelFormat.TRANSPARENT
}

/** Refit already loaded spans before TextView lays them out after a dock or window resize. */
internal class NovelIllustrationTextView(context: Context) : TextView(context) {
    private var fittedIllustrations = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        if (availableWidth > 0 && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY &&
            (fittedIllustrations || usesNovelReadingColumn(resources.configuration.smallestScreenWidthDp))
        ) {
            val spanned = text as? Spanned
            spanned?.getSpans(0, spanned.length, ImageSpan::class.java)?.forEach { span ->
                (span.drawable as? NovelIllustration)?.let {
                    it.fitToWidth(availableWidth)
                    fittedIllustrations = true
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
