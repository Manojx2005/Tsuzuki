package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme

/**
 * A reader font row in the Tsuzuki themes (board 08-tools "FontManager"): tile, name, kind line, accent check when
 * selected (instead of the stock filled card), and a muted delete button for custom fonts.
 */
@Composable
fun TsuzukiFontItem(name: String, kind: String, isSelected: Boolean, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(
                when {
                    isSelected -> Modifier.border(2.dp, t.accent, shape)
                    t.tileBorder != null -> Modifier.border(1.dp, t.tileBorder, shape)
                    else -> Modifier
                },
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (t.isDot) kind.uppercase() else kind,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = muted,
            )
        }
        if (isSelected) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = t.accent, modifier = Modifier.padding(8.dp))
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = muted)
            }
        }
    }
}
