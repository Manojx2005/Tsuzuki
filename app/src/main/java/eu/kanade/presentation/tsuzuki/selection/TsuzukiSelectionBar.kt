package eu.kanade.presentation.tsuzuki.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal data class SelectionAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit = {},
    val shortTitle: String = title,
    val menu: (@Composable (Boolean, () -> Unit) -> Unit)? = null,
)

internal fun selectionActionSlots(widthDp: Float, fontScale: Float): Int =
    (widthDp / (64f * fontScale.coerceAtLeast(1f))).toInt().coerceIn(2, 5)

@Composable
internal fun selectionCountLabel(count: Int): String =
    if (TsuzukiTheme.tokens.style == eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle.NONE) {
        count.toString()
    } else {
        tsuzukiMenuLabel(stringResource(MR.strings.tsz_q9_selection_count, count))
    }

@Composable
internal fun TsuzukiSelectionBar(
    actions: List<SelectionAction>,
    modifier: Modifier = Modifier,
    additionalActions: List<SelectionAction> = emptyList(),
) {
    val tokens = TsuzukiTheme.tokens
    Surface(
        modifier = modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = tokens.tile,
        shape = RoundedCornerShape(26.dp),
        border = tokens.tileBorder?.let { androidx.compose.foundation.BorderStroke(1.dp, it) },
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            val slots = selectionActionSlots(maxWidth.value, LocalDensity.current.fontScale)
            // Keep actions with their own nested menus in the main row.
            val primaryCount = if (actions.size > slots || additionalActions.isNotEmpty()) slots - 1 else slots
            val ordered = actions.sortedByDescending { it.menu != null }.take(primaryCount).toSet()
            val primary = actions.filter { it in ordered }
            val overflow = actions.filterNot { it in ordered } + additionalActions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                primary.forEach { SelectionButton(it) }
                if (overflow.isNotEmpty()) {
                    SelectionButton(
                        SelectionAction(
                            title = stringResource(MR.strings.label_more),
                            icon = Icons.Outlined.MoreHoriz,
                            menu = { expanded, close ->
                                DropdownMenu(expanded = expanded, onDismissRequest = close) {
                                    overflow.forEach { action ->
                                        DropdownMenuItem(
                                            text = { Text(tsuzukiMenuLabel(action.title)) },
                                            leadingIcon = { Icon(action.icon, null) },
                                            onClick = {
                                                close()
                                                action.onClick()
                                            },
                                        )
                                    }
                                }
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.SelectionButton(action: SelectionAction) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.weight(1f)) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 60.dp)
                .semantics { contentDescription = action.title }
                .tsuzukiClickable(role = Role.Button) {
                    if (action.menu != null) expanded = true else action.onClick()
                }
                .padding(horizontal = 3.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Icon(action.icon, null, Modifier.size(20.dp))
            Text(
                tsuzukiMenuLabel(action.shortTitle),
                modifier = Modifier.clearAndSetSemantics {},
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        action.menu?.invoke(expanded) { expanded = false }
    }
}
