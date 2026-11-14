package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.CircularProgressIndicator
import eu.kanade.presentation.theme.TachiyomiTheme
import tachiyomi.presentation.core.components.CombinedCircularProgressIndicator

/**
 * A wrapper for [CircularProgressIndicator] that always rotates.
 *
 * By always rotating we give the feedback to the user that the application isn't 'stuck'.
 */
class ReaderProgressIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    init {
        layoutParams = FrameLayout.LayoutParams(
            if (eu.kanade.presentation.tsuzuki.components.usesTsuzukiErrors()) {
                FrameLayout.LayoutParams.MATCH_PARENT
            } else {
                WRAP_CONTENT
            },
            WRAP_CONTENT,
            Gravity.CENTER,
        )
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    }

    private var progress by mutableFloatStateOf(0f)
    private var showing by mutableStateOf(true)
    var onRetry: (() -> Unit)? = null
    var chapterName: String = ""

    @Composable
    override fun Content() {
        TachiyomiTheme {
            if (eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle() && showing) {
                eu.kanade.presentation.tsuzuki.components.TsuzukiLoadingStages(
                    chapterName,
                    listOf(tachiyomi.presentation.core.i18n.stringResource(tachiyomi.i18n.MR.strings.tsz_error_fetch)),
                    0,
                    onRetry = onRetry,
                )
            } else {
                CombinedCircularProgressIndicator(progress = { progress })
            }
        }
    }

    fun show() {
        showing = true
        isVisible = true
    }

    fun hide() {
        showing = false
        isVisible = false
    }

    fun setProgress(@IntRange(from = 0, to = 100) progress: Int) {
        this.progress = progress / 100f
    }
}
