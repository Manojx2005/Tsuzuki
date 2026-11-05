package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Ballot
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dropdown menus in the Tsuzuki themes (board 17-drafts "Menus" / "MenuPick", owner-approved 2026-11-04).
 * Colour: a rounded tile one step lighter than the screen behind it, so menus follow the book palette, with a soft
 * shadow. Dot: a black tile with a hairline border and no shadow.
 */
data class TsuzukiMenuStyle(
    val shape: Shape,
    val container: Color,
    val border: BorderStroke?,
    val shadow: Dp,
)

/** The menu style for the current theme, or null in stock themes (keep Material's menu). */
@Composable
fun tsuzukiMenuStyle(): TsuzukiMenuStyle? {
    val t = TsuzukiTheme.tokens
    return when (t.style) {
        TsuzukiStyle.NONE -> null
        TsuzukiStyle.DOT -> TsuzukiMenuStyle(
            shape = RoundedCornerShape(16.dp),
            container = Color(0xFF0D0D0D),
            border = BorderStroke(1.dp, Color(0xFF333333)),
            shadow = 0.dp,
        )
        else -> TsuzukiMenuStyle(
            shape = RoundedCornerShape(22.dp),
            container = lerp(t.background, Color.White, 0.08f),
            border = null,
            shadow = 12.dp,
        )
    }
}

/**
 * The pick-one marker for [eu.kanade.presentation.components.RadioMenuItem]: an accent ring with a dot when
 * chosen (Dot: a filled accent square), a muted ring otherwise. Null in stock themes.
 */
@Composable
fun tsuzukiMenuRadio(checked: Boolean): (@Composable () -> Unit)? {
    val t = TsuzukiTheme.tokens
    if (t.style == TsuzukiStyle.NONE) return null
    return {
        val shape = if (t.isDot) RoundedCornerShape(3.dp) else CircleShape
        val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
        Box(
            Modifier
                .size(20.dp)
                .clip(shape)
                .border(2.dp, if (checked) t.accent else muted, shape)
                .then(if (checked && t.isDot) Modifier.background(t.accent) else Modifier),
        ) {
            if (checked && !t.isDot) {
                Box(
                    Modifier
                        .padding(5.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(t.accent),
                )
            }
        }
    }
}

/** A menu item's label: uppercase in Dot (board "Menus"), unchanged elsewhere. */
@Composable
fun tsuzukiMenuLabel(text: String): String = if (TsuzukiTheme.tokens.isDot) text.uppercase() else text

/** Menu labels are bold in the Tsuzuki themes; null keeps the stock weight. */
@Composable
fun tsuzukiMenuWeight(): FontWeight? = when (TsuzukiTheme.tokens.style) {
    TsuzukiStyle.NONE -> null
    TsuzukiStyle.DOT -> FontWeight.Bold
    else -> FontWeight.SemiBold
}

/**
 * A menu row in Tsuzuki-owned menus (board "Menus"): the cased, bold label, and [danger] rows (delete, clear) in the
 * accent. Use inside [eu.kanade.presentation.components.DropdownMenu].
 */
@Composable
fun TsuzukiMenuItem(text: String, onClick: () -> Unit, danger: Boolean = false) {
    val t = TsuzukiTheme.tokens
    androidx.compose.material3.DropdownMenuItem(
        text = {
            androidx.compose.material3.Text(
                tsuzukiMenuLabel(text),
                fontWeight = tsuzukiMenuWeight(),
                color = if (danger) t.accent else Color.Unspecified,
            )
        },
        onClick = onClick,
    )
}

/** The accent for destructive rows in the Tsuzuki themes; Unspecified (stock colour) otherwise. */
@Composable
fun tsuzukiMenuDangerColor(destructive: Boolean): Color =
    if (destructive && TsuzukiTheme.tokens.style != TsuzukiStyle.NONE) TsuzukiTheme.tokens.accent else Color.Unspecified

/** A menu row's leading icon in the Tsuzuki themes (muted, or the accent on destructive rows); null in stock. */
@Composable
fun tsuzukiMenuIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    destructive: Boolean,
): (@Composable () -> Unit)? {
    val t = TsuzukiTheme.tokens
    if (icon == null || t.style == TsuzukiStyle.NONE) return null
    val tint = if (destructive) {
        t.accent
    } else if (t.isDot) {
        Color(0xFF8A8A8A)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    return { androidx.compose.material3.Icon(icon, contentDescription = null, tint = tint) }
}

/** Icons for the Library overflow menu (board "Menus"), kept here so the upstream toolbar needs no new imports. */
object TsuzukiMenuIcons {
    val update = Icons.Outlined.Refresh
    val reload = Icons.Outlined.Sync
    val random = Icons.Outlined.Shuffle
    val massImport = Icons.Outlined.Download
    val importEpub = Icons.Outlined.FileOpen
    val duplicates = Icons.Outlined.ContentCopy
    val delete = Icons.Outlined.Delete

    // Book details overflow
    val refresh = Icons.Outlined.Refresh
    val categories = Icons.AutoMirrored.Outlined.Label
    val migrate = Icons.Outlined.SwapHoriz
    val similar = Icons.Outlined.Explore
    val edit = Icons.Outlined.Edit
    val clearInfo = Icons.Outlined.LayersClear
    val share = Icons.Outlined.Share
    val translate = Icons.Outlined.Translate
    val searchChapters = Icons.AutoMirrored.Outlined.ManageSearch
    val exportEpub = Icons.Outlined.FileOpen
    val wordCount = Icons.Outlined.Ballot
    val notes = Icons.AutoMirrored.Outlined.Notes
    val notebook = Icons.AutoMirrored.Outlined.MenuBook

    // Novel reader overflow
    val webView = Icons.Outlined.Public
    val browser = Icons.Outlined.OpenInBrowser

    // Catalogue, extension details, web view
    val help = Icons.AutoMirrored.Outlined.HelpOutline
    val settings = Icons.Outlined.Settings
    val enableAll = Icons.Outlined.ToggleOn
    val disableAll = Icons.Outlined.ToggleOff
    val cookies = Icons.Outlined.Cookie

    // Migration, extension tabs, download queue
    val pinned = Icons.Outlined.PushPin
    val filter = Icons.Outlined.FilterList
    val repos = Icons.Outlined.Inventory2
    val cancel = Icons.Outlined.Close
}

/** The board's hairline above a destructive menu row; nothing for other rows or in stock themes. */
@Composable
fun TsuzukiMenuSeparator(beforeDestructive: Boolean) {
    val t = TsuzukiTheme.tokens
    if (!beforeDestructive || t.style == TsuzukiStyle.NONE) return
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        color = t.tileBorder ?: Color.White.copy(alpha = 0.10f),
    )
}
