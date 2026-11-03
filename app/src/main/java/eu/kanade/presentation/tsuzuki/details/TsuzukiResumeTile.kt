package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme

/**
 * Resume tile on book details (board "NovelDetails"): play disc, what comes next, and progress
 * through the book. Colour: accent fill. Dot: dark tile with a red play disc.
 */
@Composable
fun TsuzukiResumeTile(
    readCount: Int,
    total: Int,
    nextChapter: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = TsuzukiTheme.tokens
    val ink = if (t.isDot) Color.White else t.onAccent
    val shape = RoundedCornerShape(24.dp)
    val started = readCount > 0
    val label = (if (started) "Resume" else "Start") + " · $readCount / $total"
    Row(
        modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(76.dp)
            .clip(shape)
            .then(
                if (t.isDot) {
                    Modifier
                        .background(t.tile)
                        .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                } else {
                    Modifier.background(t.accent)
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (t.isDot) t.accent else t.background.takeUnlessUnspecified(Color.Black)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (t.isDot) Color.White else t.accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (t.isDot) label.uppercase() else label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                color = ink,
                maxLines = 1,
            )
            nextChapter?.let {
                Text(
                    text = if (t.isDot) it.uppercase() else it,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = ink.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (total > 0) {
                TsuzukiProgressBar(
                    progress = readCount.toFloat() / total,
                    dots = 18,
                    thickness = 5.dp,
                    fill = if (t.isDot) Color.Unspecified else t.onAccent,
                    track = if (t.isDot) Color.Unspecified else t.onAccent.copy(alpha = 0.2f),
                )
            }
        }
    }
}

private fun Color.takeUnlessUnspecified(other: Color) = if (this == Color.Unspecified) other else this
