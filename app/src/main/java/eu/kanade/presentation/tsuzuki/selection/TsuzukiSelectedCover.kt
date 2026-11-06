package eu.kanade.presentation.tsuzuki.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme

@Composable
internal fun BoxScope.TsuzukiSelectedCover() {
    val tokens = TsuzukiTheme.tokens
    androidx.compose.foundation.layout.Box(
        Modifier.matchParentSize().border(2.dp, tokens.accent, RoundedCornerShape(10.dp)),
    )
    Icon(
        Icons.Outlined.Check,
        contentDescription = null,
        modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(26.dp)
            .background(tokens.accent, CircleShape).padding(4.dp),
        tint = tokens.onAccent,
    )
}
