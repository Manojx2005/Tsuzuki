package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Selectable pill (boards "LibraryList", "Updates"): 38 dp, fully rounded; the selected one takes
 * the accent, the rest sit on a tile (with the tile border in Dot). Optional count after the label.
 */
@Composable
fun TsuzukiPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    role: Role = Role.Tab,
) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(19.dp)
    val ink = if (selected) t.onAccent else MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .height(38.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(t.accent)
                } else {
                    Modifier
                        .background(t.tile)
                        .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                },
            )
            .clickable(role = role, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = if (t.isDot) text.uppercase() else text,
            color = ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
        count?.let {
            Text(text = "$it", color = ink.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
