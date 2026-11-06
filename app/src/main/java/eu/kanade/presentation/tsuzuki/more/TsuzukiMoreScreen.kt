package eu.kanade.presentation.tsuzuki.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * More in the Tsuzuki themes (design board "More"): two toggle tiles, then a bento of
 * destinations. History stays reachable here when Library takes its place in the main navigation.
 */
@Composable
fun TsuzukiMoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickHistory: () -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
    onClickHelp: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val queue = downloadQueueStateProvider()
    val pending = when (queue) {
        is DownloadQueueState.Downloading -> queue.pending
        is DownloadQueueState.Paused -> queue.pending
        DownloadQueueState.Stopped -> 0
    }
    val queueSub = when (queue) {
        DownloadQueueState.Stopped -> null
        is DownloadQueueState.Paused -> stringResource(MR.strings.paused)
        is DownloadQueueState.Downloading -> pluralStringResource(MR.plurals.download_queue_summary, pending, pending)
    }

    Column(
        Modifier
            .fillMaxSize()
            .tsuzukiBackground(t)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
    ) {
        Text(
            text = t.case(stringResource(MR.strings.label_more)),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, lineHeight = 46.sp),
            color = Color.White,
            modifier = Modifier.padding(start = 4.dp, bottom = 14.dp, top = 4.dp).semantics { heading() },
        )
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MoreTilePair {
                ToggleTile(
                    icon = Icons.Outlined.CloudDownload,
                    label = stringResource(MR.strings.label_downloaded_only),
                    sub = stringResource(MR.strings.downloaded_only_summary),
                    checked = downloadedOnly,
                    onChange = onDownloadedOnlyChange,
                    modifier = Modifier.weight(1f),
                )
                ToggleTile(
                    icon = Icons.Outlined.VisibilityOff,
                    label = stringResource(MR.strings.pref_incognito_mode),
                    sub = stringResource(MR.strings.pref_incognito_mode_summary),
                    checked = incognitoMode,
                    onChange = onIncognitoModeChange,
                    modifier = Modifier.weight(1f),
                )
            }
            NavTile(
                Icons.Outlined.History,
                stringResource(MR.strings.label_recent_manga),
                null,
                null,
                Modifier.fillMaxWidth(),
                onClickHistory,
                height = 96.dp,
            )
            MoreTilePair {
                NavTile(
                    Icons.Outlined.GetApp,
                    stringResource(MR.strings.label_download_queue),
                    queueSub,
                    pending.takeIf {
                        it >
                            0
                    }?.toString(),
                    Modifier.weight(1f),
                    onClickDownloadQueue,
                )
                NavTile(
                    Icons.Outlined.QueryStats,
                    stringResource(MR.strings.label_stats),
                    null,
                    null,
                    Modifier.weight(1f),
                    onClickStats,
                )
            }
            MoreTilePair {
                NavTile(
                    Icons.Outlined.Label,
                    stringResource(MR.strings.categories),
                    null,
                    null,
                    Modifier.weight(1f),
                    onClickCategories,
                )
                NavTile(
                    Icons.Outlined.Storage,
                    stringResource(MR.strings.label_data_storage),
                    null,
                    null,
                    Modifier.weight(1f),
                    onClickDataAndStorage,
                )
            }
            MoreTilePair {
                NavTile(
                    Icons.Outlined.Settings,
                    stringResource(MR.strings.label_settings),
                    null,
                    null,
                    Modifier.weight(1f),
                    onClickSettings,
                )
                NavTile(
                    Icons.AutoMirrored.Outlined.HelpOutline,
                    stringResource(MR.strings.label_help),
                    null,
                    null,
                    Modifier.weight(1f),
                    onClickHelp,
                )
            }
            NavTile(
                Icons.Outlined.Info,
                stringResource(MR.strings.pref_category_about),
                "Tsuzuki",
                null,
                Modifier.fillMaxWidth(),
                onClickAbout,
                height = 96.dp,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreTilePair(content: @Composable FlowRowScope.() -> Unit) {
    val singleColumn = LocalDensity.current.fontScale >= 1.5f && LocalConfiguration.current.screenWidthDp < 600
    FlowRow(
        Modifier.fillMaxWidth(),
        maxItemsInEachRow = if (singleColumn) 1 else 2,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun ToggleTile(
    icon: ImageVector,
    label: String,
    sub: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val t = TsuzukiTheme.tokens
    val ink = if (checked) t.onAccent else Color.White
    Column(
        modifier
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(if (checked) Modifier.background(t.accent) else tileFill(t))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
            // Small switch: knob right when on.
            Box(
                Modifier
                    .size(width = 42.dp, height = 26.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.18f))
                    .padding(3.dp),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(Color.White))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            t.case(label),
            style = body(15.sp, FontWeight.ExtraBold),
            color = ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            t.case(sub),
            style = body(11.sp, FontWeight.Medium),
            color = ink.copy(alpha = 0.75f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NavTile(
    icon: ImageVector,
    label: String,
    sub: String?,
    value: String?,
    modifier: Modifier,
    onClick: () -> Unit,
    height: Dp = 118.dp,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val press = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        modifier
            .tsuzukiPress(press)
            .heightIn(min = height)
            .clip(RoundedCornerShape(24.dp))
            .then(tileFill(t))
            .clickable(
                interactionSource = press,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            value?.let {
                Text(
                    it,
                    style = if (t.isDot) {
                        MaterialTheme.typography.displaySmall.copy(fontSize = 22.sp)
                    } else {
                        MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    },
                    color = t.accent,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            t.case(label),
            style = body(15.sp, FontWeight.ExtraBold),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        sub?.let {
            Text(
                t.case(it),
                style = body(11.sp, FontWeight.Medium),
                color = muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun tileFill(t: TsuzukiTokens): Modifier {
    val shape = RoundedCornerShape(24.dp)
    return Modifier
        .background(t.tile)
        .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
}

private fun TsuzukiTokens.case(s: String) = if (isDot) s.uppercase() else s

@Composable
private fun body(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight) =
    MaterialTheme.typography.bodyMedium.copy(fontSize = size, fontWeight = weight)
