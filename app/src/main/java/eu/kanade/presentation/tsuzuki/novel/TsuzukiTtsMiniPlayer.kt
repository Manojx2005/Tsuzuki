package eu.kanade.presentation.tsuzuki.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Board 06-reader/ListenMini: the floating listen bar over the page. Same parameters as the stock
 * overlay. Cover, chapter / voice / speed line, progress and sleep timer from the board have no
 * data here and are left out.
 */
@Composable
internal fun TsuzukiTtsMiniPlayer(
    isTtsActive: Boolean,
    isTtsPaused: Boolean,
    onPauseResume: () -> Unit,
    onPrevParagraph: () -> Unit,
    onNextParagraph: () -> Unit,
    onStartFromViewport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(37.dp)
    val playing = isTtsActive && !isTtsPaused
    Row(
        modifier = modifier
            .clip(shape)
            .background(tokens.tile)
            .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniButton(
            icon = Icons.Outlined.Visibility,
            description = stringResource(MR.strings.tsz_tts_read_from_here),
            onClick = onStartFromViewport,
        )
        MiniButton(
            icon = Icons.Outlined.FastRewind,
            description = stringResource(TDMR.strings.tts_prev_paragraph),
            onClick = onPrevParagraph,
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(tokens.accent)
                .tsuzukiClickable(role = Role.Button, onClick = onPauseResume),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = stringResource(TDMR.strings.pref_novel_tts),
                tint = tokens.onAccent,
                modifier = Modifier.size(26.dp),
            )
        }
        MiniButton(
            icon = Icons.Outlined.FastForward,
            description = stringResource(TDMR.strings.tts_next_paragraph),
            onClick = onNextParagraph,
        )
    }
}

@Composable
private fun MiniButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .tsuzukiClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}
