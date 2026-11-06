package eu.kanade.presentation.tsuzuki.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Slider
import tachiyomi.presentation.core.i18n.stringResource

internal data class NovelChromeAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    val active: Boolean = false,
)

@Composable
internal fun TsuzukiNovelNavigation(
    enabledPrevious: Boolean,
    onPrevious: () -> Unit,
    enabledNext: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    progress: @Composable () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = onPrevious,
            enabled = enabledPrevious,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = tokens.tile,
                contentColor = tokens.dotOn,
            ),
        ) {
            Icon(Icons.AutoMirrored.Outlined.NavigateBefore, stringResource(MR.strings.action_previous_chapter))
        }
        Column(Modifier.weight(1f)) { progress() }
        FilledIconButton(
            onClick = onNext,
            enabled = enabledNext,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = tokens.accent,
                contentColor = tokens.onAccent,
            ),
        ) {
            Icon(Icons.AutoMirrored.Outlined.NavigateNext, stringResource(MR.strings.action_next_chapter))
        }
    }
}

@Composable
internal fun TsuzukiNovelActions(actions: List<NovelChromeAction>, modifier: Modifier = Modifier) {
    if (actions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        actions.take(4).forEach { action ->
            NovelActionButton(action, Modifier.weight(1f))
        }
        if (actions.size > 4) {
            Column(Modifier.weight(1f)) {
                NovelActionButton(
                    NovelChromeAction(Icons.Outlined.MoreHoriz, stringResource(MR.strings.label_more), {
                        expanded = true
                    }),
                    Modifier.fillMaxWidth(),
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    actions.drop(4).forEach { action ->
                        TsuzukiMenuItem(action.label, onClick = {
                            expanded = false
                            action.onClick()
                        })
                        action.onLongClick?.let { onLongClick ->
                            TsuzukiMenuItem(
                                stringResource(
                                    MR.strings.tsz_reader_action_options,
                                    action.label,
                                ),
                                onClick = {
                                    expanded = false
                                    onLongClick()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NovelActionButton(action: NovelChromeAction, modifier: Modifier = Modifier) {
    val tokens = TsuzukiTheme.tokens
    val color = if (action.active) tokens.accent else tokens.dotOn
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .background(if (action.active) tokens.tile else androidx.compose.ui.graphics.Color.Transparent)
            .combinedClickable(role = Role.Button, onClick = action.onClick, onLongClick = action.onLongClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Icon(action.icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = color)
        Text(
            text = tsuzukiMenuLabel(action.label),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            textAlign = TextAlign.Center,
            // One line per action: two-line labels collided with their neighbours at 360 dp (S22, 2026-11-07).
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun TsuzukiNovelProgress(
    currentProgress: Int,
    pageLabel: String?,
    onProgressChange: (Int) -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$currentProgress%", style = MaterialTheme.typography.labelSmall, color = tokens.dotOn)
            if (pageLabel != null) {
                Text(
                    tsuzukiMenuLabel(stringResource(MR.strings.tsz_reader_page_position, pageLabel)),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.dotOn.copy(alpha = 0.72f),
                )
            }
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = currentProgress,
            valueRange = 0..100,
            onValueChange = onProgressChange,
            interactionSource = interactionSource,
            track = { TsuzukiReaderSliderTrack(it, showDots = true) },
            thumb = { TsuzukiReaderSliderThumb(interactionSource) },
        )
    }
}
