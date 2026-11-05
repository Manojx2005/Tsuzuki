package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiNavItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiRail
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import eu.kanade.tachiyomi.ui.home.HomeScreen
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The settings title in the Tsuzuki themes (boards 07-system "Settings", 11-tablet "TabletSettings"): the display
 * face, large, with a round back button on phones. On tablets the rail sits beside it, so there's no back button.
 */
@Composable
fun TsuzukiSettingsTitle(title: String, onBack: (() -> Unit)?) {
    val t = TsuzukiTheme.tokens
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = if (onBack != null) 20.dp else 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .padding(end = 10.dp)
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(
                        t.tileBorder?.let { Modifier.border(1.dp, it, CircleShape) }
                            ?: Modifier.border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                    )
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(MR.strings.tsz_a11y_back),
                    tint = Color.White,
                )
            }
        }
        Text(
            if (t.isDot) title.uppercase() else title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, lineHeight = 48.sp),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * One settings section. Phone (board "Settings"): icon, bold title, one-line summary and a chevron, inside the shared
 * section tile ([modifier]). Tablet list (board "TabletSettings"): icon and bold title only; the open section is
 * outlined in the accent.
 */
@Composable
fun TsuzukiSettingsSectionRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    twoPane: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(if (t.isDot) 18.dp else 20.dp)
    Row(
        Modifier
            .tsuzukiPress(press)
            .then(modifier)
            .then(
                if (twoPane) {
                    Modifier
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(shape)
                        .then(if (selected) Modifier.border(2.dp, t.accent, shape) else Modifier)
                } else {
                    Modifier
                },
            )
            .clickable(interactionSource = press, indication = ripple(), role = Role.Button, onClick = onClick)
            .fillMaxWidth()
            .heightIn(min = if (twoPane) 56.dp else 64.dp)
            .padding(horizontal = if (twoPane) 18.dp else 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 18.dp, end = 8.dp)) {
            Text(
                if (t.isDot) title.uppercase() else title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!twoPane && !subtitle.isNullOrBlank()) {
                Text(
                    if (t.isDot) subtitle.uppercase() else subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!twoPane) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = muted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Tablet settings (board "TabletSettings"): the app rail stays on the left with More selected, then the section list
 * and the open section. Picking a rail destination leaves Settings for that tab. The list is narrower than the stock
 * half-and-half split so the section on the right gets the room, as on the board.
 */
@Composable
fun TsuzukiSettingsTwoPane(
    onLeave: () -> Unit,
    onSearch: () -> Unit,
    startContent: @Composable BoxScope.() -> Unit,
    endContent: @Composable BoxScope.() -> Unit,
) {
    val t = TsuzukiTheme.tokens

    // Home isn't composed while Settings is on top, so leave first; the event waits for Home to listen again.
    fun open(tab: HomeScreen.Tab) {
        onLeave()
        launchUI { HomeScreen.openTab(tab) }
    }
    val items = listOf(
        TsuzukiNavItem("home", stringResource(MR.strings.tsz_home), "HOME", Icons.Outlined.GridView, false) {
            open(HomeScreen.Tab.Home)
        },
        TsuzukiNavItem(
            "updates",
            stringResource(MR.strings.label_recent_updates),
            "NEW",
            Icons.Outlined.NotificationsNone,
            false,
        ) { open(HomeScreen.Tab.Updates) },
        TsuzukiNavItem(
            "history",
            stringResource(MR.strings.label_recent_manga),
            "PAST",
            Icons.Outlined.Schedule,
            false,
        ) {
            open(HomeScreen.Tab.History)
        },
        TsuzukiNavItem("browse", stringResource(MR.strings.browse), "FIND", Icons.Outlined.Explore, false) {
            open(HomeScreen.Tab.Browse())
        },
        TsuzukiNavItem("more", stringResource(MR.strings.label_more), "•••", Icons.Outlined.MoreHoriz, true) {
            onLeave()
        },
    )
    Row(Modifier.fillMaxSize().tsuzukiBackground(t)) {
        TsuzukiRail(items = items, onSearch = onSearch, onSettings = {})
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val listWidth = (maxWidth * 0.42f).coerceIn(280.dp, 380.dp)
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(listWidth).fillMaxHeight(), content = startContent)
                Box(Modifier.weight(1f).fillMaxHeight(), content = endContent)
            }
        }
    }
}
