package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One destination in the Tsuzuki nav bar / rail (design boards: Nav, Rail).
 *
 * @param label full label (Colour theme, rail, accessibility).
 * @param shortLabel Dot bottom-bar label.
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

/** Fixed cells keep tab changes out of measurement; only the shared indicator layer moves. */
internal fun navIndicatorOffset(index: Int, count: Int, cellExtent: Float, reverse: Boolean = false): Float =
    if (index !in 0 until count) 0f else index * cellExtent * if (reverse) -1f else 1f

internal fun navHighlightedIndex(position: Float, count: Int, cellExtent: Float = 1f, reverse: Boolean = false): Int =
    if (cellExtent <= 0f) -1 else highlightedSegment(position / cellExtent * if (reverse) -1f else 1f, count)

/** All rail cells share enough room for the icon, gap and tallest scaled label. */
internal fun railCellHeight(labelHeightDp: Float): Float = maxOf(60f, 32f + 4f + labelHeightDp)

internal fun navIndicatorSpec(reduced: Boolean): FiniteAnimationSpec<Float> = if (reduced) {
    snap()
} else {
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}

/** Floating pill bottom bar for phones. */
@Composable
fun TsuzukiNavBar(items: List<TsuzukiNavItem>, modifier: Modifier = Modifier) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(31.dp)
    val cellHeight = 48.dp + 20.dp * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f)
    val selectedIndex = items.indexOfFirst { it.selected }
    val reverse = LocalLayoutDirection.current == LayoutDirection.Rtl
    val position = animateFloatAsState(
        targetValue = navIndicatorOffset(selectedIndex, items.size, 1f, reverse),
        animationSpec = navIndicatorSpec(rememberReduceMotion()),
        label = "navIndicator",
    )
    val highlightedIndex by remember(position, items.size, reverse) {
        derivedStateOf { navHighlightedIndex(position.value, items.size, reverse = reverse) }
    }
    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
            .heightIn(min = cellHeight + 14.dp)
            .clip(shape)
            .background(t.tile.compositeOver(t.background))
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (items.isNotEmpty()) {
            val cellWidth = maxWidth / items.size
            if (selectedIndex >= 0) {
                Box(
                    Modifier
                        .width(cellWidth)
                        .height(cellHeight)
                        .graphicsLayer {
                            translationX = position.value * cellWidth.toPx()
                        }
                        .background(t.dotOn, RoundedCornerShape(24.dp)),
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, item ->
                    NavBarItem(
                        item,
                        selectedIndex >= 0 && index == highlightedIndex,
                        Modifier.weight(1f).heightIn(min = cellHeight),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(item: TsuzukiNavItem, highlighted: Boolean, modifier: Modifier = Modifier) {
    val t = TsuzukiTheme.tokens
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier
            .heightIn(min = 48.dp)
            .selectable(
                selected = item.selected,
                role = Role.Tab,
                onClick = item.onClick,
                interactionSource = interactions,
                indication = null,
            )
            .semantics { contentDescription = item.label },
        contentAlignment = Alignment.Center,
    ) {
        // Clip the ripple only, so an unread badge is not cut by the cell's rounded corner.
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(24.dp)).indication(interactions, ripple()))
        Text(
            text = if (t.isDot) item.shortLabel else item.label,
            style = navLabel(MaterialTheme.typography.labelLarge, dot = t.isDot),
            color = if (highlighted) t.background else t.dotOn.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        if (item.badge > 0 && !item.selected) {
            NavBadge(item.badge, Modifier.align(Alignment.TopEnd))
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
                        .background(t.background)
                        .drawRightBorder(t.tileBorder ?: t.track)
                } else {
                    Modifier.background(t.tile.compositeOver(t.background))
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
                        Modifier.background(t.background).border(1.dp, t.track, logoShape)
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
                        Modifier.border(1.dp, t.track, fabShape)
                    } else {
                        Modifier.background(t.dotOn.copy(alpha = 0.12f))
                    },
                )
                .clickable(role = Role.Button, onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = stringResource(MR.strings.tsz_search),
                tint = t.dotOn,
                modifier = Modifier.size(22.dp),
            )
        }

        val labelStyle = railLabelStyle(t.isDot)
        val textMeasurer = rememberTextMeasurer()
        val labelHeightPx = items.maxOfOrNull {
            textMeasurer.measure(
                text = AnnotatedString(if (t.isDot) it.label.uppercase() else it.label),
                style = labelStyle,
                maxLines = 1,
            ).size.height
        } ?: 0
        val cellHeight = with(LocalDensity.current) { railCellHeight(labelHeightPx.toDp().value).dp }
        val cellExtent = cellHeight.value + 10f
        val selectedIndex = items.indexOfFirst { it.selected }
        val position = animateFloatAsState(
            targetValue = navIndicatorOffset(selectedIndex, items.size, cellExtent),
            animationSpec = navIndicatorSpec(rememberReduceMotion()),
            label = "railIndicator",
        )
        val highlightedIndex by remember(position, items.size, cellExtent) {
            derivedStateOf { navHighlightedIndex(position.value, items.size, cellExtent) }
        }
        Box(Modifier.fillMaxWidth()) {
            if (selectedIndex >= 0) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .size(width = 56.dp, height = 32.dp)
                        .graphicsLayer { translationY = position.value.dp.toPx() }
                        .background(t.accent, RoundedCornerShape(16.dp)),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEachIndexed { index, item ->
                    RailItem(item, selectedIndex >= 0 && index == highlightedIndex, cellHeight, labelStyle)
                }
            }
        }

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
                tint = t.dotOn.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun RailItem(item: TsuzukiNavItem, highlighted: Boolean, cellHeight: Dp, labelStyle: TextStyle) {
    val t = TsuzukiTheme.tokens
    val labelColor = t.dotOn.copy(alpha = if (highlighted) 1f else 0.7f)
    Column(
        Modifier
            .height(cellHeight)
            .fillMaxWidth()
            .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            BadgedIcon(item, if (highlighted) t.onAccent else labelColor, size = 22)
        }
        Text(
            text = if (t.isDot) item.label.uppercase() else item.label,
            style = labelStyle,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun railLabelStyle(dot: Boolean): TextStyle = if (dot) {
    MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
} else {
    MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun BadgedIcon(item: TsuzukiNavItem, tint: Color, size: Int) {
    Box {
        Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(size.dp))
        if (item.badge > 0 && !item.selected) {
            NavBadge(item.badge, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-6).dp))
        }
    }
}

@Composable
private fun NavBadge(count: Int, modifier: Modifier = Modifier) {
    val t = TsuzukiTheme.tokens
    Box(
        modifier
            .heightIn(min = 16.dp)
            .clip(CircleShape)
            .background(t.accent)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = t.onAccent,
        )
    }
}

private fun navLabel(base: TextStyle, dot: Boolean): TextStyle = if (dot) {
    base.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
} else {
    base.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
}

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
