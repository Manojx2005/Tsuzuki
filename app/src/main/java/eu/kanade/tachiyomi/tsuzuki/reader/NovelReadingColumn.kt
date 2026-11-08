package eu.kanade.tachiyomi.tsuzuki.reader

import android.content.Context
import android.content.res.Configuration
import android.view.View.MeasureSpec
import android.widget.LinearLayout
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlin.math.roundToInt

internal const val NOVEL_READING_WIDTH_DP = 600

internal fun usesNovelReadingColumn(smallestWidthDp: Int): Boolean = smallestWidthDp >= NOVEL_READING_WIDTH_DP

internal data class NovelColumnInsets(val left: Int, val right: Int)

internal fun novelColumnInsets(
    paneWidth: Int,
    leftMargin: Int,
    rightMargin: Int,
    maxTextWidth: Int,
): NovelColumnInsets {
    val extra = (paneWidth - leftMargin - rightMargin - maxTextWidth).coerceAtLeast(0)
    val left = extra / 2
    return NovelColumnInsets(left, extra - left)
}

internal class NovelReadingColumn(
    context: Context,
    private val preferences: ReaderPreferences,
) : LinearLayout(context) {

    private val density get() = resources.displayMetrics.density
    private val leftMargin get() = (preferences.novelMarginLeft.get() * density).toInt()
    private val rightMargin get() = (preferences.novelMarginRight.get() * density).toInt()
    private val maxTextWidth get() = (NOVEL_READING_WIDTH_DP * density).roundToInt()
    private val hasColumn get() = usesNovelReadingColumn(resources.configuration.smallestScreenWidthDp)

    fun illustrationWidth(): Int? {
        if (!hasColumn) return null
        val availableWidth = measuredWidth - leftMargin - rightMargin
        return availableWidth.takeIf { it > 0 }?.coerceAtMost(maxTextWidth) ?: maxTextWidth
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val insets = if (hasColumn && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            novelColumnInsets(MeasureSpec.getSize(widthMeasureSpec), leftMargin, rightMargin, maxTextWidth)
        } else {
            NovelColumnInsets(0, 0)
        }
        // Chunks still apply the user's own padding. Only the excess pane width is shared here.
        if (paddingLeft != insets.left || paddingRight != insets.right) {
            setPadding(insets.left, paddingTop, insets.right, paddingBottom)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

internal fun novelReadingColumnCss(
    leftMargin: Int,
    rightMargin: Int,
    pagedClass: String,
    styleImportance: String,
): String {
    val minPaneWidth = NOVEL_READING_WIDTH_DP + leftMargin + rightMargin
    // Paged mode uses body.clientWidth for the column stride and clips at the body edge.
    // Its gutters stay outside that box so adjacent columns cannot show through them.
    val gutter = "calc((100% - ${minPaneWidth}px) / 2"
    return """
        @media (min-width: ${minPaneWidth}px) {
            :where(html:not(.$pagedClass)) body {
                box-sizing: content-box !important;
                max-width: ${NOVEL_READING_WIDTH_DP}px$styleImportance;
                margin-left: auto$styleImportance;
                margin-right: auto$styleImportance;
                padding-left: ${leftMargin}px$styleImportance;
                padding-right: ${rightMargin}px$styleImportance;
            }
            :where(html.$pagedClass) body {
                max-width: ${NOVEL_READING_WIDTH_DP}px$styleImportance;
                margin-left: $gutter + ${leftMargin}px)$styleImportance;
                margin-right: $gutter + ${rightMargin}px)$styleImportance;
            }
        }
    """.trimIndent()
}
