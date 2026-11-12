package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.SourceIcon
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
    sourceIcon: @Composable (Source) -> Unit = { SourceIcon(it) },
    showHealth: Boolean = false,
) {
    val tokens = TsuzukiTheme.tokens
    val latestBelow = LocalDensity.current.fontScale >= 1.5f
    BaseSourceItem(
        source = source,
        modifier = modifier.heightIn(min = 65.dp),
        showLanguageInContent = showLanguage,
        onClickItem = onOpen,
        onLongClickItem = onOptions,
        icon = { sourceIcon(it) },
        content = { item, language ->
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showHealth) SourceHealthDot(item.id)
                    Text(
                        text = tsuzukiMenuLabel(item.name),
                        color = tokens.dotOn,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (language != null || onPreset != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (language != null) {
                            Text(
                                text = tsuzukiMenuLabel(language),
                                color = tokens.dotOn.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!LocalInspectionMode.current) SourceTypeBadge(item)
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
                if (latestBelow && onLatest != null) SourceLatestButton(onLatest)
            }
        },
        action = {
            if (!latestBelow && onLatest != null) SourceLatestButton(onLatest)
            pin()
        },
    )
}

@Composable
private fun SourceLatestButton(onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    OutlinedButton(
        modifier = Modifier.heightIn(min = 48.dp),
        onClick = onClick,
        border = BorderStroke(1.dp, tokens.tileBorder ?: tokens.track),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.accent),
    ) {
        Text(
            tsuzukiMenuLabel(stringResource(MR.strings.latest)),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
