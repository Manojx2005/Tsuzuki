package eu.kanade.presentation.tsuzuki.browse

import android.util.DisplayMetrics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.browse.components.label
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Extension details header in the Tsuzuki themes (board 04-browse "ExtensionDetail"): icon tile, display name,
 * "version · language · N sites", the NSFW rating in the accent (tap explains it, as stock), then Uninstall and
 * App info. Health, speed and last-checked tiles are left out: there's no source health data.
 */
@Composable
fun TsuzukiExtensionDetailsHeader(
    extension: Extension,
    siteCount: Int,
    onClickAgeRating: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickAppInfo: (() -> Unit)?,
    incognito: Boolean,
    onIncognitoChange: (Boolean) -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val context = LocalContext.current
    val tileShape = RoundedCornerShape(28.dp)

    Column(
        Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.clickable {
                context.copyToClipboard("Extension Debug information", extensionDebugInfo(extension))
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(tileShape)
                    .background(t.tile)
                    .then(t.tileBorder?.let { Modifier.border(1.dp, it, tileShape) } ?: Modifier),
                contentAlignment = Alignment.Center,
            ) {
                ExtensionIcon(
                    extension = extension,
                    modifier = Modifier.size(64.dp),
                    density = DisplayMetrics.DENSITY_XXXHIGH,
                )
            }
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    up(extension.name),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, lineHeight = 38.sp),
                    color = Color.White,
                    maxLines = 2,
                )
                val parts = buildList {
                    add(extension.versionName)
                    add(LocaleHelper.getSourceDisplayName(extension.lang, context))
                    if (siteCount > 1) add("$siteCount ${stringResource(MR.strings.label_sources).lowercase()}")
                }
                Text(
                    up(parts.joinToString(" · ")),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = muted,
                )
                val warningLabel = extension.contentWarning.label
                if (warningLabel != null) {
                    Text(
                        up(stringResource(warningLabel.title)) + " ⓘ",
                        modifier = Modifier.clickable(role = Role.Button, onClick = onClickAgeRating),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = t.accent,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onClickAppInfo != null) {
                DetailsButton(
                    stringResource(MR.strings.ext_app_info),
                    filled = true,
                    onClickAppInfo,
                    Modifier.weight(1f),
                )
            }
            DetailsButton(
                stringResource(MR.strings.ext_uninstall),
                filled = false,
                onClickUninstall,
                Modifier.weight(1f),
            )
        }
        eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget(
            title = stringResource(MR.strings.pref_incognito_mode),
            subtitle = stringResource(MR.strings.pref_incognito_mode_extension_summary),
            checked = incognito,
            onCheckedChanged = onIncognitoChange,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(t.tile)
                .then(t.tileBorder?.let { Modifier.border(1.dp, it, RoundedCornerShape(24.dp)) } ?: Modifier),
        )
    }
}

/** Same text as the stock header's tap-to-copy. */
private fun extensionDebugInfo(extension: Extension): String = buildString {
    append(
        """
        Extension name: ${extension.name} (lang: ${extension.lang}; package: ${extension.pkgName})
        Extension version: ${extension.versionName} (lib: ${extension.libVersion}; version code: ${extension.versionCode})
        Content warning: ${extension.contentWarning}
        """.trimIndent(),
    )
    if (extension is Extension.Installed) {
        append("\n\n")
        appendLine(
            """
            Update available: ${extension.hasUpdate}
            Orphaned: ${extension.isObsolete}
            Shared: ${extension.isShared}
            """.trimIndent(),
        )
        extension.store?.let { append("Repository: ${it.indexUrl}") }
    }
}

@Composable
private fun DetailsButton(label: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .height(56.dp)
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(t.accent)
                } else {
                    Modifier
                        .background(t.tile)
                        .border(1.dp, t.tileBorder ?: Color.White.copy(alpha = 0.12f), shape)
                },
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (t.isDot) label.uppercase() else label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
            color = if (filled) t.onAccent else t.accent,
        )
    }
}
