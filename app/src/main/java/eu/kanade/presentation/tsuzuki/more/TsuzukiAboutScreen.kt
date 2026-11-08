package eu.kanade.presentation.tsuzuki.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Projects Tsuzuki is built on, with the licence of each (checked against their repositories, 2026-11-03). */
private val Credits = listOf(
    "Mihon" to "Apache-2.0",
    "Tsundoku" to "Apache-2.0",
    "LNReader plugins" to "MIT",
    "Keiyoushi extensions" to "Apache-2.0",
    "Princeton University WordNet" to "WordNet License",
    "EDRDG JMdict" to "CC BY-SA 4.0",
    // Bundled fonts; licence texts in docs/licenses/ (OFL-*.txt).
    "Doto, Instrument Serif, Space Mono, Manrope, Literata" to "OFL-1.1",
    "DotGothic16, M PLUS 1 Code, Noto Serif JP, Zen Kaku Gothic New" to "OFL-1.1",
)

/**
 * About in the Tsuzuki themes (board 07-system "About"): app tile, name and version, action tiles, credits.
 * Only real actions are shown: What's new and Check for updates follow the stock screen's build conditions.
 * Left out from the board: "Report a bug · with logs" (no log upload flow), the update interval and the privacy
 * blurb (telemetry depends on the build, so "no tracking" isn't always true).
 */
@Composable
fun TsuzukiAboutScreen(
    version: String,
    showWhatsNew: Boolean,
    showCheckUpdates: Boolean,
    onBack: (() -> Unit)?,
    onVersionClick: () -> Unit,
    onWhatsNew: () -> Unit,
    onSourceCode: () -> Unit,
    onLicences: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onNewUpdate: (GetApplicationRelease.Result.NewUpdate) -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    val checkUpdates: () -> Unit = {
        if (!checking) {
            scope.launch {
                checking = true
                try {
                    when (val result = withIOContext { AppUpdateChecker().checkForUpdate(forceCheck = true) }) {
                        is GetApplicationRelease.Result.NewUpdate -> onNewUpdate(result)
                        is GetApplicationRelease.Result.NoNewUpdate -> context.toast(
                            MR.strings.update_check_no_new_updates,
                        )
                        is GetApplicationRelease.Result.OsTooOld -> context.toast(MR.strings.update_check_eol)
                    }
                } catch (e: Exception) {
                    context.toast(e.message)
                    logcat(LogPriority.ERROR, e)
                } finally {
                    checking = false
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .tsuzukiBackground(t)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(tileFill(t, CircleShape))
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
        Box(
            Modifier
                .padding(top = 20.dp, start = 4.dp)
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(t.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = t.onAccent,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            t.case("Tsuzuki"),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 52.sp, lineHeight = 56.sp),
            color = Color.White,
            modifier = Modifier.padding(start = 4.dp, top = 14.dp).semantics { heading() },
        )
        Text(
            t.case(version),
            style = body(14.sp, FontWeight.Medium),
            color = muted,
            modifier = Modifier
                .padding(start = 4.dp, top = 4.dp, bottom = 18.dp)
                .clickable(role = Role.Button, onClick = onVersionClick),
        )

        Column(Modifier.widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showWhatsNew || showCheckUpdates) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showWhatsNew) {
                        ActionTile(
                            Icons.Outlined.AutoAwesome,
                            stringResource(MR.strings.whats_new),
                            Modifier.weight(1f),
                            onWhatsNew,
                        )
                    }
                    if (showCheckUpdates) {
                        ActionTile(
                            Icons.Outlined.Refresh,
                            stringResource(MR.strings.check_for_updates),
                            Modifier.weight(1f),
                            checkUpdates,
                            busy = checking,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(
                    Icons.Outlined.Code,
                    stringResource(MR.strings.tsz_about_source_code),
                    Modifier.weight(1f),
                    onSourceCode,
                    sub = "Apache-2.0",
                )
                ActionTile(
                    Icons.Outlined.Gavel,
                    stringResource(MR.strings.licenses),
                    Modifier.weight(1f),
                    onLicences,
                )
            }

            val shape = RoundedCornerShape(24.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .then(tileFill(t, shape))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(MR.strings.tsz_about_built_on).uppercase(),
                    style = body(11.sp, FontWeight.ExtraBold).copy(letterSpacing = 1.2.sp),
                    color = muted,
                )
                Credits.forEach { (name, licence) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            t.case(name),
                            style = body(16.sp, FontWeight.ExtraBold),
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        Text(licence, style = body(14.sp, FontWeight.Medium), color = muted)
                    }
                }
            }

            Text(
                t.case(stringResource(MR.strings.privacy_policy)),
                style = body(13.sp, FontWeight.Medium),
                color = muted,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable(role = Role.Button, onClick = onPrivacyPolicy)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    sub: String? = null,
    busy: Boolean = false,
    height: Dp = 112.dp,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .tsuzukiPress(press)
            .height(height)
            .clip(shape)
            .then(tileFill(t, shape))
            .clickable(interactionSource = press, indication = ripple(), role = Role.Button, onClick = onClick)
            .padding(14.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp), color = t.accent, strokeWidth = 2.5.dp)
        } else {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(
            t.case(label),
            style = body(15.sp, FontWeight.ExtraBold),
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        sub?.let {
            Text(t.case(it), style = body(11.sp, FontWeight.Medium), color = muted, maxLines = 1)
        }
    }
}

private fun tileFill(t: TsuzukiTokens, shape: androidx.compose.ui.graphics.Shape): Modifier = Modifier
    .background(t.tile)
    .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)

private fun TsuzukiTokens.case(s: String) = if (isDot) s.uppercase() else s

@Composable
private fun body(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight) =
    MaterialTheme.typography.bodyMedium.copy(fontSize = size, fontWeight = weight)
