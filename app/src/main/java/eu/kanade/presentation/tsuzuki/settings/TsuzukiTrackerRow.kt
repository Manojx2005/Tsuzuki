package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Uses local link counts and display names in the themed account list. */
@Composable
fun TsuzukiTrackerRow(
    tracker: Tracker,
    isLoggedIn: Boolean,
    linkedCount: Int?,
    onClick: (() -> Unit)?,
) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, role = Role.Button) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (t.isDot) {
            Box(
                modifier = Modifier.size(42.dp)
                    .border(1.dp, t.tileBorder ?: t.mutedText, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tracker.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = t.dotOn,
                )
            }
        } else {
            TrackLogoIcon(tracker)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                up(tracker.name),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                color = t.dotOn,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val status = if (isLoggedIn) {
                val displayName = tracker.getDisplayUsername()
                val label = stringResource(MR.strings.login_success)
                if (displayName.isBlank()) label else "$label · $displayName"
            } else {
                stringResource(MR.strings.tsz_tracker_signed_out)
            }
            Text(
                up(status),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = t.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isLoggedIn && linkedCount != null) {
            Text(
                up(stringResource(MR.strings.tsz_tracker_linked_count, linkedCount)),
                style = MaterialTheme.typography.labelLarge,
                color = t.accent,
                maxLines = 1,
            )
        } else if (isLoggedIn) {
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
