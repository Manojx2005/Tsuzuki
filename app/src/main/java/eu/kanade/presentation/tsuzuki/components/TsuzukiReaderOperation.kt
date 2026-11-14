package eu.kanade.presentation.tsuzuki.components

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiTokensFor
import eu.kanade.tachiyomi.databinding.ReaderErrorBinding
import eu.kanade.tachiyomi.jsplugin.source.JsSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal fun usesTsuzukiErrors() =
    tsuzukiTokensFor(Injekt.get<UiPreferences>().appTheme.get()).style != TsuzukiStyle.NONE

/** Reuses the exact Compose board in View-based readers; composition dies when detached. */
internal fun ReaderActivity.createReaderErrorView(error: Throwable, retry: (() -> Unit)?): View? {
    if (!usesTsuzukiErrors()) return null
    return ComposeView(this).apply {
        layoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setContent {
            TachiyomiTheme {
                TsuzukiErrorState(
                    error,
                    viewModel.state.value.viewerChapters?.currChapter?.chapter?.name.orEmpty(),
                    onRetry = retry,
                    onWebView = readerWebViewAction(),
                )
            }
        }
    }
}

internal fun ReaderActivity.createReaderLoadingView(key: Any): View? {
    if (!usesTsuzukiErrors()) return null
    return ComposeView(this).apply {
        layoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setContent {
            TachiyomiTheme {
                TsuzukiLoadingStages(
                    viewModel.state.value.viewerChapters?.currChapter?.chapter?.name.orEmpty(),
                    listOf(stringResource(MR.strings.tsz_error_fetch)),
                    0,
                    operationKey = key,
                    onRetry = { viewModel.reloadChapter(fromSource = false) },
                    onCancel = { finish() },
                )
            }
        }
    }
}

private fun ReaderActivity.readerWebViewAction(): (() -> Unit)? {
    val source = viewModel.getSource()
    if (source !is HttpSource && source !is JsSource) return null
    return {
        lifecycleScope.launch {
            // Source implementations can resolve URLs; never ask them on the UI thread.
            val url = withContext(Dispatchers.IO) { viewModel.getChapterUrl() }
            if (url != null) startActivity(WebViewActivity.newIntent(this@readerWebViewAction, url, source.id))
        }
    }
}

/** Covers stale WebView documents without putting error text into the reader's progress or TTS DOM. */
internal class TsuzukiReaderOperation(private val activity: ReaderActivity, private val parent: FrameLayout) {
    private data class State(val key: Any, val step: Int, val error: Throwable? = null)
    private var state by mutableStateOf<State?>(null)
    private var view: ComposeView? = null

    fun loading(key: Any, step: Int = 0): Boolean = show(State(key, step))
    fun error(error: Throwable): Boolean = show(State(error, 0, error))

    private fun show(next: State): Boolean {
        if (!usesTsuzukiErrors()) return false
        state = next
        if (view == null) {
            view = ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                setContent {
                    TachiyomiTheme {
                        val current = state
                        Box(
                            Modifier.fillMaxSize().background(TsuzukiTheme.tokens.background)
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (current != null) {
                                val title = activity.viewModel.state.value.viewerChapters
                                    ?.currChapter?.chapter?.name.orEmpty()
                                if (current.error != null) {
                                    TsuzukiErrorState(
                                        current.error,
                                        title,
                                        onRetry = { activity.viewModel.reloadChapter(fromSource = false) },
                                        onWebView = activity.readerWebViewAction(),
                                    )
                                } else {
                                    TsuzukiLoadingStages(
                                        title,
                                        listOf(
                                            stringResource(MR.strings.tsz_error_fetch),
                                            stringResource(MR.strings.tsz_error_layout),
                                        ),
                                        current.step,
                                        operationKey = current.key,
                                        onRetry = { activity.viewModel.reloadChapter(fromSource = false) },
                                        onCancel = { activity.finish() },
                                    )
                                }
                            }
                        }
                    }
                }
                this@TsuzukiReaderOperation.parent.addView(
                    this,
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
            }
        }
        view?.bringToFront()
        return true
    }

    fun clear() {
        state = null
        view?.let(parent::removeView)
        view = null
    }
}

/** Existing binding keeps holder sizing and lifecycle; only its themed content changes. */
internal fun bindTsuzukiPageError(binding: ReaderErrorBinding, error: Throwable?, title: String) {
    if (!usesTsuzukiErrors()) return
    val parent = binding.root
    val canOpen = binding.actionOpenInWebView.isVisible && binding.actionOpenInWebView.hasOnClickListeners()
    val tag = "tsuzuki-page-error"
    parent.children.filter { it.tag != tag }.forEach { it.isVisible = false }
    val view = parent.findViewWithTag<ComposeView>(tag) ?: ComposeView(parent.context).apply {
        this.tag = tag
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        parent.addView(
            this,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
    }
    view.setContent {
        TachiyomiTheme {
            TsuzukiErrorState(
                error ?: IllegalStateException(stringResource(MR.strings.decode_image_error)),
                title,
                onRetry = { binding.actionRetry.performClick() },
                onWebView = {
                    binding.actionOpenInWebView.performClick()
                    Unit
                }.takeIf { canOpen },
            )
        }
    }
}
