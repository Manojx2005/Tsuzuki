package eu.kanade.tachiyomi.tsuzuki.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
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

    var selectionForegroundColor: Int? = null
        set(value) {
            field = value
            updateSelectionInk(selectionStart, selectionEnd)
        }
    private var selectionInk: CharacterStyle? = null
    private var changingSelectionInk = false

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        updateSelectionInk(selStart, selEnd)
    }

    private fun updateSelectionInk(start: Int, end: Int) {
        if (changingSelectionInk) return
        val spans = text as? Spannable ?: return
        changingSelectionInk = true
        try {
            selectionInk?.let(spans::removeSpan)
            selectionInk = null
            val ink = selectionForegroundColor ?: return
            if (start < 0 || end < 0 || start == end) return
            // A separate CharacterStyle leaves the reader's TTS ForegroundColorSpans untouched.
            val span = object : CharacterStyle() {
                override fun updateDrawState(paint: TextPaint) {
                    paint.color = ink
                }
            }
            selectionInk = span
            spans.setSpan(span, minOf(start, end), maxOf(start, end), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } finally {
            changingSelectionInk = false
        }
    }

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
