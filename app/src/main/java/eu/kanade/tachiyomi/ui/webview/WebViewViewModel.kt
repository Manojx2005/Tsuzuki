package eu.kanade.tachiyomi.ui.webview

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toWebViewHeaders
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class WebViewViewModel(
    val sourceId: Long?,
    private val sourceManager: SourceManager = Injekt.get(),
    private val network: NetworkHelper = Injekt.get(),
) : ViewModel() {

    companion object {
        val SOURCE_ID_KEY = CreationExtras.Key<Long?>()

        val Factory = viewModelFactory {
            initializer {
                WebViewViewModel(
                    sourceId = get(SOURCE_ID_KEY),
                )
            }
        }
    }

    private val mutableHeaders = MutableStateFlow<Map<String, String>?>(null)
    val headers = mutableHeaders.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            mutableHeaders.value = try {
                val source = sourceId?.let { sourceManager.get(it) as? HttpSource }
                source?.headers?.toWebViewHeaders().orEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to build headers" }
                emptyMap()
            }
        }
    }

    fun shareWebpage(context: Context, url: String) {
        try {
            context.startActivity(url.toUri().toShareIntent(context, type = "text/plain"))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        context.openInBrowser(url, forceDefaultBrowser = true)
    }

    fun clearCookies(url: String) {
        url.toHttpUrlOrNull()?.let {
            val cleared = network.cookieJar.remove(it)
            logcat { "Cleared $cleared cookies for: $url" }
        }
    }

    fun defaultUserAgentProvider() = network.defaultUserAgentProvider()
}
