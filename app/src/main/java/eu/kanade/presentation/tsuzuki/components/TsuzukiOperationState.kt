package eu.kanade.presentation.tsuzuki.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.LocalNavigator
import eu.kanade.tachiyomi.tsuzuki.error.MissingSourceException
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import kotlinx.coroutines.launch
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TsuzukiErrorScreen(
    error: Throwable,
    whatFailed: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onWebView: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
        TsuzukiErrorState(
            error,
            whatFailed,
            onRetry = onRetry,
            onWebView = onWebView,
            onExtensions = extensionsAction(),
            onStorage = storageAction(),
        )
    }
}

@Composable
fun TsuzukiWaitingScreen(
    what: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
        TsuzukiLoadingStages(
            what,
            listOf(stringResource(MR.strings.tsz_error_fetch)),
            0,
            onRetry = onRetry,
            onCancel = onCancel,
        )
    }
}

@Composable
internal fun extensionsAction(): (() -> Unit)? {
    val navigator = LocalNavigator.current ?: return null
    val scope = (LocalContext.current as? ComponentActivity)?.lifecycleScope ?: return null
    return {
        navigator.popUntil { it is HomeScreen }
        scope.launch { HomeScreen.openTab(HomeScreen.Tab.Browse(toExtensions = true)) }
    }
}

@Composable
internal fun storageAction(): (() -> Unit)? {
    val navigator = LocalNavigator.current ?: return null
    return { navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage)) }
}

@Composable
fun TsuzukiDetailsSourceStatus(state: MangaViewModel.State.Success, onRetry: () -> Unit, onWebView: (() -> Unit)?) {
    val error = state.sourceError ?: if (state.source is StubSource) MissingSourceException() else null
    when {
        state.isRefreshingData -> TsuzukiLoadingStages(
            state.manga.title,
            listOf(stringResource(MR.strings.tsz_error_fetch)),
            0,
            operationKey = state.manga.id,
        )
        error != null -> TsuzukiErrorState(
            error,
            state.manga.title,
            onRetry = onRetry,
            onWebView = onWebView,
            onExtensions = extensionsAction(),
            onStorage = storageAction(),
        )
    }
}
