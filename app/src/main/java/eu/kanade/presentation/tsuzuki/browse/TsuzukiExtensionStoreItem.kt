package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An extension repository in the Tsuzuki themes (board 04-browse "ExtensionRepos"): icon (accent while enabled),
 * name, index URL, badge and novel / manga pills, the enable switch, and a menu with the stock actions. Source (or extension)
 * counts come from the loaded available-extension lists and stay hidden until known. Left out from
 * the board: the signed / unsigned pill (no label for it yet).
 */
@Composable
fun TsuzukiExtensionStoreItem(
    store: ExtensionStore,
    enabled: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenDiscord: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    counts: TsuzukiRepoCounts? = null,
) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val shape = RoundedCornerShape(24.dp)
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (enabled) t.accent else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Extension,
                    contentDescription = null,
                    tint = if (enabled) t.onAccent else muted,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    up(store.name),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    store.indexUrl.removePrefix("https://"),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onSetEnabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = t.accent,
                    checkedThumbColor = t.onAccent,
                    checkedBorderColor = t.accent,
                ),
            )
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(MR.strings.action_menu),
                        tint = Color.White,
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = {
                    menu = false
                }, offset = androidx.compose.ui.unit.DpOffset.Zero) {
                    TsuzukiMenuItem(stringResource(MR.strings.action_open_in_browser), onClick = {
                        menu = false
                        onOpenWebsite()
                    })
                    if (store.contact.discord != null) {
                        TsuzukiMenuItem("Discord", onClick = {
                            menu = false
                            onOpenDiscord()
                        })
                    }
                    TsuzukiMenuItem(stringResource(MR.strings.action_copy_to_clipboard), onClick = {
                        menu = false
                        onCopy()
                    })
                    TsuzukiMenuItem(stringResource(MR.strings.action_delete), onClick = {
                        menu = false
                        onDelete()
                    }, danger = true)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            store.badgeLabel.takeIf { it.isNotBlank() }?.let { StorePill(up(it)) }
            StorePill(up(stringResource(if (store.isNovel) TDMR.strings.label_novels else TDMR.strings.label_manga)))
            // Unknown until the index has loaded (or while the repository is disabled): show nothing.
            counts?.let {
                if (it.sources > 0) {
                    StorePill(up(stringResource(MR.strings.tsz_repo_sources, it.sources)))
                } else if (it.extensions > 0) {
                    StorePill(up(stringResource(MR.strings.tsz_repo_extensions, it.extensions)))
                }
            }
        }
    }
}

@Composable
private fun StorePill(text: String) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
        color = Color.White,
    )
}
