package eu.kanade.presentation.tsuzuki.selection

import android.content.Context
import android.os.SystemClock
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.core.common.i18n.stringResource as contextStringResource

private const val UNDO_WINDOW_MS = 6_000L

private class UndoVisuals(
    override val message: String,
    override val actionLabel: String,
    val persistent: Boolean,
) : SnackbarVisuals {
    override val withDismissAction = true
    override val duration = SnackbarDuration.Indefinite
    var expiry by mutableLongStateOf(0L)
}

/** Starts the undo window when its snackbar reaches the front of the queue. */
internal suspend fun SnackbarHostState.awaitTsuzukiUndo(context: Context, message: String): Boolean = coroutineScope {
    val accessibility = context.getSystemService(AccessibilityManager::class.java)
    val visuals = UndoVisuals(
        message,
        context.contextStringResource(MR.strings.action_undo),
        persistent = accessibility?.isEnabled == true && accessibility.isTouchExplorationEnabled,
    )
    val timeout = launch {
        if (!visuals.persistent) {
            val snackbar = snapshotFlow { currentSnackbarData }.filterNotNull().first { it.visuals === visuals }
            visuals.expiry = SystemClock.elapsedRealtime() + UNDO_WINDOW_MS
            delay(UNDO_WINDOW_MS)
            snackbar.dismiss()
        }
    }
    try {
        showSnackbar(visuals) == SnackbarResult.ActionPerformed
    } finally {
        timeout.cancel()
    }
}

@Composable
internal fun TsuzukiUndoSnackbarHost(state: SnackbarHostState) {
    SnackbarHost(hostState = state) { data ->
        val visuals = data.visuals as? UndoVisuals
        if (visuals == null) {
            Snackbar(data)
        } else {
            val tokens = TsuzukiTheme.tokens
            val expiry = visuals.expiry
            val progress = remember(visuals) { Animatable(1f) }
            LaunchedEffect(expiry) {
                if (expiry > 0L) {
                    val remaining = (expiry - SystemClock.elapsedRealtime()).coerceIn(0L, UNDO_WINDOW_MS)
                    progress.snapTo(remaining.toFloat() / UNDO_WINDOW_MS)
                    progress.animateTo(0f, tween(remaining.toInt(), easing = LinearEasing))
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(16.dp),
                color = tokens.dotOn,
                contentColor = tokens.background,
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(visuals.message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = data::performAction) {
                            Text(tsuzukiMenuLabel(visuals.actionLabel), color = tokens.background)
                        }
                        IconButton(onClick = data::dismiss) {
                            Icon(Icons.Outlined.Close, stringResource(MR.strings.action_close))
                        }
                    }
                    if (!visuals.persistent) {
                        Box(
                            Modifier.fillMaxWidth().height(3.dp).graphicsLayer {
                                scaleX = progress.value
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            }.background(tokens.accent),
                        )
                    }
                }
            }
        }
    }
}
