package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

// These rows also appear in the upstream novel settings screen. Keep the rendering
// choice here so that screen's preference definitions and legacy rendering stay intact.
internal fun isReaderSegmentedPreference(preference: Preference<*>): Boolean = when (preference.key()) {
    "pref_novel_rendering_mode", "pref_novel_status_bar_position", "pref_novel_status_bar_size" -> true
    else -> false
}

@Composable
internal fun TsuzukiStatusBarSettings(
    onBack: () -> Unit,
    onReset: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = tsuzukiSettingsBackground(),
        containerColor = TsuzukiTheme.tokens.background.copy(alpha = 0f),
        topBar = {
            Column {
                AppBar(
                    title = stringResource(TDMR.strings.pref_novel_status_bar_customize),
                    subtitle = stringResource(TDMR.strings.pref_novel_status_bar_customize_summary),
                    navigateUp = onBack,
                    actions = {
                        IconButton(onClick = onReset) {
                            Icon(Icons.Outlined.RestartAlt, stringResource(MR.strings.action_reset))
                        }
                    },
                )
                TsuzukiSectionLabel(stringResource(TDMR.strings.pref_novel_status_bar))
            }
        },
        content = content,
    )
}

@Composable
internal fun TsuzukiStatusBarElement(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    first: Boolean,
    last: Boolean,
    shadowElevation: Dp,
    dragHandle: Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    Row(
        modifier = tsuzukiGroupedTile(first = first, last = last)
            .fillMaxWidth()
            .shadow(shadowElevation)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.DragHandle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = dragHandle.padding(end = 12.dp),
        )
        Text(
            text = tsuzukiCase(label).orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = tokens.accent,
                checkedThumbColor = tokens.onAccent,
            ),
        )
    }
}
