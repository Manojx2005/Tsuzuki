package eu.kanade.presentation.tsuzuki.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.tsuzuki.error.ErrorAction
import eu.kanade.tachiyomi.tsuzuki.error.ErrorCopy
import eu.kanade.tachiyomi.tsuzuki.error.ErrorReason
import eu.kanade.tachiyomi.tsuzuki.error.LoadingPhase
import eu.kanade.tachiyomi.tsuzuki.error.errorCopy
import eu.kanade.tachiyomi.tsuzuki.error.loadingPhase
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.delay
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.util.concurrent.TimeoutException

internal fun Context.errorHeading(copy: ErrorCopy): String = when (copy.reason) {
    ErrorReason.NETWORK -> stringResource(MR.strings.tsz_error_network)
    ErrorReason.TIMEOUT -> stringResource(MR.strings.tsz_error_timeout)
    ErrorReason.CHALLENGE -> stringResource(MR.strings.tsz_error_challenge)
    ErrorReason.NOT_FOUND -> stringResource(MR.strings.tsz_error_not_found)
    ErrorReason.SERVER -> stringResource(MR.strings.tsz_error_server)
    ErrorReason.PARSE -> stringResource(MR.strings.tsz_error_parse)
    ErrorReason.EXTENSION -> stringResource(MR.strings.tsz_error_extension)
    ErrorReason.STORAGE_FULL -> stringResource(MR.strings.tsz_error_storage_full)
    ErrorReason.PERMISSION -> stringResource(MR.strings.tsz_error_permission)
    ErrorReason.OTHER -> stringResource(MR.strings.tsz_error_other)
}

internal fun Context.errorAdvice(copy: ErrorCopy): String? = when (copy.reason) {
    ErrorReason.NETWORK -> stringResource(MR.strings.tsz_error_network_help)
    ErrorReason.TIMEOUT, ErrorReason.SERVER -> stringResource(MR.strings.tsz_error_retry_help)
    ErrorReason.CHALLENGE -> stringResource(MR.strings.tsz_error_challenge_help)
    ErrorReason.NOT_FOUND -> stringResource(MR.strings.tsz_error_not_found_help)
    ErrorReason.PARSE -> stringResource(MR.strings.tsz_error_parse_help)
    ErrorReason.EXTENSION -> stringResource(MR.strings.tsz_error_extension_help)
    ErrorReason.STORAGE_FULL, ErrorReason.PERMISSION -> stringResource(MR.strings.tsz_error_storage_help)
    ErrorReason.OTHER -> null
}

/** Board 1. Callbacks are offered only when the host can perform the action. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TsuzukiErrorState(
    error: Throwable,
    whatFailed: String,
    modifier: Modifier = Modifier,
    sourceUrl: String? = null,
    onRetry: (() -> Unit)? = null,
    onWebView: (() -> Unit)? = null,
    onExtensions: (() -> Unit)? = null,
    onStorage: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val tokens = TsuzukiTheme.tokens
    val copy = remember(error, whatFailed, sourceUrl) { errorCopy(error, whatFailed, sourceUrl) }
    val preferred = when (copy.primaryAction) {
        ErrorAction.WEBVIEW -> onWebView?.let { context.stringResource(MR.strings.action_open_in_web_view) to it }
        ErrorAction.EXTENSIONS -> onExtensions?.let { context.stringResource(MR.strings.label_extensions) to it }
        ErrorAction.STORAGE -> onStorage?.let { context.stringResource(MR.strings.pref_storage_location) to it }
        ErrorAction.RETRY -> null
    }
    TsuzukiTile(modifier = modifier.padding(16.dp).widthIn(max = 600.dp), cornerRadius = 24.dp) {
        Text(
            tsuzukiMenuLabel(context.stringResource(MR.strings.tsz_error_failed, copy.whatFailed)),
            color = tokens.mutedText,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            tsuzukiMenuLabel(context.errorHeading(copy)),
            color = tokens.dotOn,
            style = MaterialTheme.typography.headlineMedium,
        )
        context.errorAdvice(copy)?.let {
            Spacer(Modifier.height(8.dp))
            Text(tsuzukiMenuLabel(it), color = tokens.mutedText, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            copy.technical,
            color = tokens.mutedText,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).drawBehind {
                drawRoundRect(
                    tokens.mutedText.copy(alpha = 0.35f),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(
                        1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            }.tsuzukiClickable(role = Role.Button) {
                context.copyToClipboard(context.stringResource(MR.strings.action_copy_to_clipboard), copy.technical)
            }.padding(10.dp),
        )
        if (preferred != null || onRetry != null || onCancel != null) {
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                preferred?.let { (label, action) -> TsuzukiPill(label, true, action, role = Role.Button) }
                if (preferred == null &&
                    onRetry == null
                ) {
                    onCancel?.let {
                        TsuzukiPill(context.stringResource(MR.strings.action_cancel), true, it, role = Role.Button)
                    }
                }
                onRetry?.let {
                    TsuzukiPill(
                        context.stringResource(MR.strings.action_retry),
                        preferred == null,
                        it,
                        role = Role.Button,
                    )
                }
            }
        }
    }
}

/** Board 2: elapsed time is per step, keyed by the actual operation and current step. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TsuzukiLoadingStages(
    what: String,
    steps: List<String>,
    currentStep: Int,
    modifier: Modifier = Modifier,
    operationKey: Any = Unit,
    onRetry: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val tokens = TsuzukiTheme.tokens
    var retryGeneration by remember(operationKey) { mutableIntStateOf(0) }
    var elapsedMs by remember(operationKey, currentStep, retryGeneration) { mutableLongStateOf(0L) }
    LaunchedEffect(operationKey, currentStep, retryGeneration) {
        val started = android.os.SystemClock.elapsedRealtime()
        while (true) {
            elapsedMs = android.os.SystemClock.elapsedRealtime() - started
            if (elapsedMs >= 30_000L) break
            delay(250)
        }
    }
    when (loadingPhase(elapsedMs)) {
        LoadingPhase.SPINNER -> Box(modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = tokens.accent)
        }
        LoadingPhase.TIMED_OUT -> TsuzukiErrorState(
            remember(operationKey, currentStep, retryGeneration) { TimeoutException() },
            what,
            modifier,
            onRetry = onRetry?.let { retry ->
                {
                    retryGeneration++
                    retry()
                }
            },
            onCancel = onCancel,
        )
        LoadingPhase.STAGES -> TsuzukiTile(modifier.padding(16.dp).widthIn(max = 600.dp), cornerRadius = 24.dp) {
            Text(
                tsuzukiMenuLabel(context.stringResource(MR.strings.tsz_error_opening, what)),
                color = tokens.mutedText,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                tsuzukiMenuLabel(context.stringResource(MR.strings.tsz_error_loading)),
                color = tokens.dotOn,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(14.dp))
            steps.forEachIndexed { index, step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(
                            when {
                                index == currentStep -> tokens.accent
                                index < currentStep -> tokens.dotOn
                                else -> tokens.track
                            },
                        ),
                    )
                    Text(
                        tsuzukiMenuLabel(
                            if (index ==
                                currentStep
                            ) {
                                context.stringResource(MR.strings.tsz_error_seconds, step, elapsedMs / 1000)
                            } else {
                                step
                            },
                        ),
                        color = if (index <=
                            currentStep
                        ) {
                            tokens.dotOn
                        } else {
                            tokens.mutedText
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            onCancel?.let {
                Spacer(Modifier.height(16.dp))
                TsuzukiPill(context.stringResource(MR.strings.action_cancel), false, it, role = Role.Button)
            }
        }
    }
}
