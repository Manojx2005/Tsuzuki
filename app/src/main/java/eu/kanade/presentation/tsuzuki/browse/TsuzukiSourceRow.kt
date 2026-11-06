package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.SourceTypeBadge
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiSourceRow(
    source: Source,
    showLanguage: Boolean,
    onOpen: () -> Unit,
    onOptions: () -> Unit,
    onPreset: (() -> Unit)?,
    onLatest: (() -> Unit)?,
    pin: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    BaseSourceItem(
        source = source,
        modifier = modifier.heightIn(min = 65.dp),
        showLanguageInContent = showLanguage,
        onClickItem = onOpen,
        onLongClickItem = onOptions,
        content = { item, language ->
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
                Text(
                    text = tsuzukiMenuLabel(item.name),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (language != null || onPreset != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (language != null) {
                            Text(
                                text = tsuzukiMenuLabel(language),
                                color = tokens.dotOn.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        SourceTypeBadge(item)
                        if (onPreset != null) {
                            IconButton(onClick = onPreset) {
                                Icon(
                                    Icons.Outlined.FilterList,
                                    contentDescription = stringResource(MR.strings.action_filter),
                                    tint = tokens.accent,
                                )
                            }
                        }
                    }
                }
            }
        },
        action = {
            if (onLatest != null) {
                OutlinedButton(
                    onClick = onLatest,
                    border = BorderStroke(1.dp, tokens.tileBorder ?: tokens.track),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.accent),
                ) {
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.latest)),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
            pin()
        },
    )
}
