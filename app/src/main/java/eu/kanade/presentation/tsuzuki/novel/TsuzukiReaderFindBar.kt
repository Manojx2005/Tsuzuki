package eu.kanade.presentation.tsuzuki.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Find bar of the novel reader (board 06-reader "ReaderFind", "This chapter" mode): query field, "n/m", previous /
 * next and close. The whole-book list on the board has no search backing, so it is left out.
 *
 * [onSearch] runs a new search for the text; [onResult] feeds back (0-based active match, total matches).
 */
@Composable
internal fun TsuzukiReaderFindBar(
    onSearch: (String) -> Unit,
    onNext: (forward: Boolean) -> Unit,
    onClose: () -> Unit,
    resultState: FindResultState,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    val styled = tokens.style != TsuzukiStyle.NONE
    val shape = RoundedCornerShape(if (styled) tokens.tileRadius else 16.dp)
    val ink = MaterialTheme.colorScheme.onSurface
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Row(
        modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (styled) tokens.tile else MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .then(tokens.tileBorder?.takeIf { styled }?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        FindIcon(Icons.Outlined.Close, stringResource(MR.strings.tsz_find_close), true, onClose)
        Box(Modifier.weight(1f).padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    stringResource(MR.strings.tsz_find_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = ink),
                cursorBrush = SolidColor(tokens.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext(true) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        Text(
            findCounterText(resultState.active, resultState.total),
            style = MaterialTheme.typography.labelLarge,
            color = if (resultState.total > 0) tokens.accent else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 44.dp).padding(horizontal = 4.dp),
        )
        val enabled = resultState.total > 0
        FindIcon(Icons.Outlined.KeyboardArrowDown, stringResource(MR.strings.tsz_find_next), enabled) { onNext(true) }
        FindIcon(Icons.Outlined.KeyboardArrowUp, stringResource(MR.strings.tsz_find_previous), enabled) {
            onNext(false)
        }
    }
}

/** Latest result reported by the WebView's find listener. */
internal class FindResultState {
    var active by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
}

@Composable
private fun FindIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (enabled) Modifier.tsuzukiClickable(role = Role.Button, onClick = onClick) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
    }
}
