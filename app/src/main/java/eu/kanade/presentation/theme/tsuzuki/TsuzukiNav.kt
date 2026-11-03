package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One destination in the Tsuzuki nav bar / rail (design boards: Nav, Rail).
 *
 * @param label full label (Colour theme, rail, accessibility).
 * @param shortLabel Dot bottom-bar label (HOME / NEW / PAST / FIND / •••).
 */
data class TsuzukiNavItem(
    val key: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
    val selected: Boolean,
    val badge: Int = 0,
    val onClick: () -> Unit,
)

private val DotNavMuted = Color(0xFF9A9A9A)
private val DotRailMuted = Color(0xFF8A8A8A)

/** Floating pill bottom bar for phones. */
@Composable
fun TsuzukiNavBar(items: List<TsuzukiNavItem>, modifier: Modifier = Modifier) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(31.dp)
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
            .height(62.dp)
            .clip(shape)
            .then(
                if (t.isDot) {
                    Modifier.background(Color(0xFF111111)).border(1.dp, Color(0xFF2A2A2A), shape)
                } else {
                    // Solid, never see-through: the book colour darkened (board "Nav").
                    Modifier.background(
                        Color.Black.copy(alpha = 0.45f).compositeOver(t.background.takeOrElse(Color.Black)),
                    )
                },
            )
            .padding(horizontal = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item -> NavBarItem(item) }
    }
}

@Composable
private fun NavBarItem(item: TsuzukiNavItem) {
    val t = TsuzukiTheme.tokens
    val pill = RoundedCornerShape(24.dp)
    val base = Modifier
        .height(48.dp)
        .clip(pill)
        .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick)
        .semantics { contentDescription = item.label }
        .animateContentSize()

    if (t.isDot) {
        val showBadge = item.badge > 0 && !item.selected
        val text = if (showBadge) "${item.shortLabel} ${item.badge}" else item.shortLabel
        Box(
            base
                .then(if (item.selected) Modifier.background(Color.White) else Modifier)
                .padding(horizontal = if (item.selected) 18.dp else 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = navLabel(MaterialTheme.typography.labelLarge, dot = true),
                color = when {
                    item.selected -> Color.Black
                    item.badge > 0 -> t.accent
                    else -> DotNavMuted
                },
                maxLines = 1,
            )
        }
    } else {
        Row(
            base
                .then(
                    if (item.selected) {
                        Modifier.background(Color.White).padding(horizontal = 16.dp)
                    } else {
                        Modifier.width(48.dp)
                    },
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tint = if (item.selected) t.background.takeOrElse(Color.Black) else Color.White.copy(alpha = 0.8f)
            BadgedIcon(item, tint, size = if (item.selected) 20 else 22)
            if (item.selected) {
                Text(
                    text = item.label,
                    style = navLabel(MaterialTheme.typography.labelLarge, dot = false),
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Navigation rail for tablets, foldables and desktop windows. */
@Composable
fun TsuzukiRail(
    items: List<TsuzukiNavItem>,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = TsuzukiTheme.tokens
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(96.dp)
            .then(
                if (t.isDot) {
                    Modifier
                        .background(Color(0xFF070707))
                        .drawRightBorder(Color(0xFF222222))
                } else {
                    Modifier.background(Color.Black.copy(alpha = 0.2f))
                },
            )
            .systemBarsPadding()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Logo tile (Brand board). Dot: the dot ribbon on black, like the launcher icon.
        // Colour: the solid ribbon in the book colour on the accent.
        val logoShape = RoundedCornerShape(16.dp)
        Box(
            Modifier
                .size(48.dp)
                .clip(logoShape)
                .then(
                    if (t.isDot) {
                        Modifier.background(Color.Black).border(1.dp, Color(0xFF333333), logoShape)
                    } else {
                        Modifier.background(t.accent)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (t.isDot) {
                Image(
                    painter = painterResource(R.drawable.ic_tsuzuki_dots),
                    contentDescription = "Tsuzuki",
                    modifier = Modifier.size(48.dp),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_mihon),
                    contentDescription = "Tsuzuki",
                    tint = t.onAccent,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        // Search
        val fabShape = RoundedCornerShape(18.dp)
        Box(
            Modifier
                .padding(top = 6.dp, bottom = 12.dp)
                .size(56.dp)
                .clip(fabShape)
                .then(
                    if (t.isDot) {
                        Modifier.border(1.dp, Color(0xFF333333), fabShape)
                    } else {
                        Modifier.background(Color.White.copy(alpha = 0.12f))
                    },
                )
                .clickable(role = Role.Button, onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = stringResource(MR.strings.tsz_search),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }

        items.forEach { item -> RailItem(item) }

        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(MR.strings.tsz_settings),
                tint = if (t.isDot) DotRailMuted else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun RailItem(item: TsuzukiNavItem) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) DotRailMuted else Color.White.copy(alpha = 0.7f)
    val labelColor by animateColorAsState(if (item.selected) Color.White else muted, label = "railLabel")
    val pillColor by animateColorAsState(if (item.selected) t.accent else Color.Transparent, label = "railPill")
    Column(
        Modifier
            .heightIn(min = 60.dp)
            .fillMaxWidth()
            .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center,
        ) {
            BadgedIcon(item, if (item.selected) t.onAccent else labelColor, size = 22)
        }
        Text(
            text = if (t.isDot) item.label.uppercase() else item.label,
            style = if (t.isDot) {
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
            },
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun BadgedIcon(item: TsuzukiNavItem, tint: Color, size: Int) {
    val t = TsuzukiTheme.tokens
    Box {
        Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(size.dp))
        if (item.badge > 0 && !item.selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-6).dp)
                    .heightIn(min = 16.dp)
                    .clip(CircleShape)
                    .background(if (t.isDot) t.accent else Color(0xFFFF8A70))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (item.badge > 99) "99+" else item.badge.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }
    }
}

private fun navLabel(base: TextStyle, dot: Boolean): TextStyle = if (dot) {
    base.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
} else {
    base.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
}

private fun Color.takeOrElse(other: Color) = if (this == Color.Unspecified) other else this

private fun Modifier.drawRightBorder(color: Color) = this.then(
    Modifier.drawBehind {
        drawLine(
            color,
            androidx.compose.ui.geometry.Offset(size.width, 0f),
            androidx.compose.ui.geometry.Offset(size.width, size.height),
            1.dp.toPx(),
        )
    },
)
