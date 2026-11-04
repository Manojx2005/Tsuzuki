package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.data.track.Tracker
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A tracker account row in the Tsuzuki themes (board 08-tools "TrackerAccounts"): logo, name, "Logged in · user" or
 * nothing, and on the right an accent check when signed in or an accent "Login" when not (the stock row's green check
 * breaks Dot's one-colour rule). Tapping logs in or out, as stock. The board's "Linked N" count isn't stored.
 */
@Composable
fun TsuzukiTrackerRow(tracker: Tracker, isLoggedIn: Boolean, onClick: (() -> Unit)?) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, role = Role.Button) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackLogoIcon(tracker)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                up(tracker.name),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                maxLines = 1,
            )
            val name = tracker.getDisplayUsername()
            if (isLoggedIn) {
                val status = stringResource(MR.strings.login_success)
                Text(
                    up(if (name.isNotBlank()) "$status · $name" else status),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isLoggedIn) {
            Icon(
                Icons.Outlined.Done,
                contentDescription = stringResource(MR.strings.login_success),
                tint = t.accent,
                modifier = Modifier.size(26.dp),
            )
        } else {
            Text(
                up(stringResource(MR.strings.login)),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                ),
                color = t.accent,
            )
        }
    }
}
