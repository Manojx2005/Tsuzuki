package eu.kanade.presentation.theme.tsuzuki

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import eu.kanade.presentation.components.dialogProperties
import eu.kanade.presentation.util.isTabletUi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Chapter filters retain the existing centered, 460 dp tablet dialog and bottom sheet on phones. */
@Composable
fun TsuzukiAdaptiveSheet(
    onDismissRequest: () -> Unit,
    content: @Composable (close: (() -> Unit) -> Unit) -> Unit,
) {
    if (!isTabletUi()) {
        TsuzukiModalBottomSheet(
            onDismissRequest = onDismissRequest,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            showDragHandle = false,
            maxWidth = 460.dp,
        ) { close -> content(close) }
        return
    }
    val reduced = rememberReduceMotion()
    val enter = tsuzukiTween<Float>(
        TsuzukiMotion.SHEET_MS,
        if (TsuzukiTheme.tokens.isDot) TsuzukiMotion.Enter else TsuzukiMotion.Spring,
    )
    val exit = tsuzukiTween<Float>(200, TsuzukiMotion.Exit)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    val fadeClock = remember {
        object : MotionDurationScale {
            override val scaleFactor = 1f
        }
    }
    val close: (() -> Unit) -> Unit = { after ->
        if (!closing) {
            closing = true
            scope.launch {
                if (reduced) {
                    withContext(fadeClock) { progress.animateTo(0f, tween(TsuzukiMotion.REDUCED_MS)) }
                } else {
                    progress.animateTo(0f, exit)
                }
                after()
            }
        }
    }
    LaunchedEffect(Unit) {
        if (reduced) {
            withContext(fadeClock) { progress.animateTo(1f, tween(TsuzukiMotion.REDUCED_MS)) }
        } else {
            progress.animateTo(1f, enter)
        }
    }
    Dialog(onDismissRequest = { close(onDismissRequest) }, properties = dialogProperties) {
        BackHandler { close(onDismissRequest) }
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) {
                close(onDismissRequest)
            },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.requiredWidthIn(max = 460.dp)
                    .clickable(interactionSource = null, indication = null, onClick = {})
                    .systemBarsPadding().padding(vertical = 16.dp)
                    .graphicsLayer {
                        alpha = progress.value.coerceIn(0f, 1f)
                        translationY = if (reduced) 0f else (1f - progress.value) * 32.dp.toPx()
                    },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) { content(close) }
        }
    }
}
