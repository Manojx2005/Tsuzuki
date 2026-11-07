package eu.kanade.tachiyomi.tsuzuki.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.updateAll
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiTokensFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

internal fun widgetTheme(ui: UiPreferences) = tsuzukiTokensFor(
    appTheme = if (ui.appTheme.get() == AppTheme.DOT) AppTheme.DOT else AppTheme.COLOUR,
    colourAccent = Color(ui.tsuzukiColourAccent.get()),
)

internal fun watchWidgetTheme(context: Context, ui: UiPreferences) {
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        combine(
            ui.appTheme.changes(),
            ui.tsuzukiColourAccent.changes(),
            ui.tsuzukiDotColourCovers.changes(),
            ::Triple,
        ).drop(1).collect {
            for (widget in listOf(TsuzukiReadingWidget(), TsuzukiListeningWidget())) {
                try {
                    widget.updateAll(context)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    logcat(LogPriority.WARN, error) { "Unable to refresh widget theme" }
                }
            }
        }
    }
}
