package eu.kanade.presentation.tsuzuki.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MarkdownRender
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.tachiyomi.ui.more.NewUpdateScreenModel
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * "Update ready" in the Tsuzuki themes (board 17-drafts "NewUpdate", owner-approved 2026-11-04): close button, accent
 * download block, display title, "Version X · you have Y", the release notes in a What's new tile, then Download (which
 * shows progress, Install and Retry like stock), Not now, and Open on GitHub / Skip this version. The release has no size, so none is shown.
 */
@Composable
fun TsuzukiNewUpdateScreen(
    versionName: String,
    currentVersion: String,
    changelogInfo: String,
    stage: NewUpdateScreenModel.Stage,
    downloadProgress: () -> Int,
    onOpenInBrowser: () -> Unit,
    onAcceptUpdate: () -> Unit,
    onRejectUpdate: () -> Unit,
    onSkipUpdate: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val accept = when (stage) {
        NewUpdateScreenModel.Stage.Available -> stringResource(MR.strings.update_check_confirm)
        NewUpdateScreenModel.Stage.Downloading -> stringResource(
            MR.strings.downloading_with_progress,
            downloadProgress(),
        )
        NewUpdateScreenModel.Stage.Downloaded -> stringResource(MR.strings.action_install)
        NewUpdateScreenModel.Stage.Failed -> stringResource(MR.strings.action_retry)
    }
    val canAccept = stage != NewUpdateScreenModel.Stage.Downloading

    Column(
        Modifier
            .fillMaxSize()
            .tsuzukiBackground(t)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                Modifier
                    .padding(top = 16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(tileFill(t, CircleShape))
                    .clickable(role = Role.Button, onClick = onRejectUpdate),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, tint = Color.White)
            }
            Box(
                Modifier
                    .padding(top = 18.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(t.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = null,
                    tint = t.onAccent,
                    modifier = Modifier.size(30.dp),
                )
            }
            Text(
                t.case(stringResource(MR.strings.tsz_update_ready)),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 52.sp, lineHeight = 56.sp),
                color = Color.White,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                t.case(stringResource(MR.strings.tsz_update_versions, versionName, currentVersion)),
                style = body(15.sp, FontWeight.Medium),
                color = muted,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
            )
            val shape = RoundedCornerShape(24.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .then(tileFill(t, shape))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    stringResource(MR.strings.whats_new).uppercase(),
                    style = body(11.sp, FontWeight.ExtraBold).copy(letterSpacing = 1.2.sp),
                    color = muted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                MarkdownRender(content = changelogInfo, flavour = remember { GFMFlavourDescriptor() })
            }
        }

        Column(
            Modifier.widthIn(max = 640.dp).padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(t.accent)
                    .alpha(if (canAccept) 1f else 0.6f)
                    .clickable(enabled = canAccept, role = Role.Button, onClick = onAcceptUpdate),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = null,
                    tint = t.onAccent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    t.case(accept),
                    style = body(16.sp, FontWeight.ExtraBold),
                    color = t.onAccent,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            val ghost = RoundedCornerShape(28.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(ghost)
                    .then(tileFill(t, ghost))
                    .clickable(role = Role.Button, onClick = onRejectUpdate),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    t.case(stringResource(MR.strings.action_not_now)),
                    style = body(16.sp, FontWeight.ExtraBold),
                    color = Color.White,
                )
            }
            Row(
                Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    t.case(stringResource(MR.strings.update_check_open)),
                    style = body(13.sp, FontWeight.Medium),
                    color = muted,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onOpenInBrowser)
                        .padding(8.dp),
                )
                Text(
                    t.case(stringResource(MR.strings.update_check_skip)),
                    style = body(13.sp, FontWeight.Medium),
                    color = muted,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onSkipUpdate)
                        .padding(8.dp),
                )
            }
        }
    }
}

private fun tileFill(t: TsuzukiTokens, shape: androidx.compose.ui.graphics.Shape): Modifier = Modifier
    .background(t.tile)
    .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)

private fun TsuzukiTokens.case(s: String) = if (isDot) s.uppercase() else s

@Composable
private fun body(size: TextUnit, weight: FontWeight) =
    MaterialTheme.typography.bodyMedium.copy(fontSize = size, fontWeight = weight)
