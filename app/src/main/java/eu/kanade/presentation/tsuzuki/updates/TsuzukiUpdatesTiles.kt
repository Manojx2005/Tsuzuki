package eu.kanade.presentation.tsuzuki.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material3.Icon
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
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Board "Updates": a count tile for unread new chapters next to an accent "Save all" tile that
 * downloads every unread chapter not saved yet. Episodes join the count once anime lands.
 */
@Composable
fun TsuzukiUpdatesTiles(
    newCount: Int,
    toSave: Int,
    onSaveAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(18.dp)
    val tile = Modifier
        .height(104.dp)
        .clip(shape)
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val border = t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            tile
                .weight(1.3f)
                .background(t.tile)
                .then(border)
                .padding(14.dp),
        ) {
            Text(
                up(stringResource(MR.strings.tsz_new)),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$newCount",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 40.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    up(stringResource(MR.strings.tsz_ch)),
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val enabled = toSave > 0
        val ink = if (enabled) t.onAccent else MaterialTheme.colorScheme.onSurfaceVariant
        Column(
            tile
                .weight(1f)
                .then(if (enabled) Modifier.background(t.accent) else Modifier.background(t.tile).then(border))
                .clickable(enabled = enabled, role = Role.Button, onClick = onSaveAll)
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                if (enabled) Icons.Outlined.Download else Icons.Outlined.DownloadDone,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(20.dp),
            )
            Column {
                Text(
                    up(stringResource(MR.strings.tsz_save_all)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ink,
                )
                Text(
                    up(
                        if (enabled) {
                            stringResource(MR.strings.tsz_to_save, toSave)
                        } else {
                            stringResource(MR.strings.tsz_all_saved)
                        },
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
        }
    }
}
