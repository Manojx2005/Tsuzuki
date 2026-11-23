package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiAdaptiveSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtension
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeStoreAction
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeStoreRow
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * AP-15: an installed anime extension's details, like the book extension details: name, version, language and
 * package, its sources (with their settings), and the same Update / Trust / Uninstall actions as its row.
 */
@Composable
internal fun AnimeExtensionDetailsSheet(
    row: AnimeStoreRow,
    configurable: List<AnimeSource>,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    onRemove: () -> Unit,
    onSourceSettings: (AnimeSource) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val context = LocalContext.current
    val installed = row.extension as? AnimeExtension.Installed
    val sources = installed?.sources.orEmpty()
    TsuzukiAdaptiveSheet(onDismissRequest = onDismiss) { close ->
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                tsuzukiMenuLabel(cleanAnimeExtensionName(row.name)),
                color = tokens.dotOn,
                style = MaterialTheme.typography.headlineSmall,
            )
            val language = row.extension?.lang?.takeIf(String::isNotBlank)
                ?.let { LocaleHelper.getSourceDisplayName(it, context) }
            Text(
                tsuzukiMenuLabel(listOfNotNull(row.version.takeIf(String::isNotBlank), language).joinToString(" · ")),
                color = tokens.mutedText,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(row.pkgName, color = tokens.mutedText, style = MaterialTheme.typography.labelSmall)
            if (sources.isNotEmpty()) {
                TsuzukiSectionLabel(stringResource(MR.strings.label_sources))
                sources.forEach { source ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            tsuzukiMenuLabel(
                                listOf(source.name, LocaleHelper.getSourceDisplayName(source.lang, context))
                                    .joinToString(" · "),
                            ),
                            modifier = Modifier.weight(1f),
                            color = tokens.dotOn,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (configurable.any { it.id == source.id }) {
                            IconButton(onClick = { close { onSourceSettings(source) } }) {
                                Icon(
                                    Icons.Outlined.Settings,
                                    stringResource(MR.strings.source_settings),
                                    Modifier.size(22.dp),
                                    tint = tokens.mutedText,
                                )
                            }
                        }
                    }
                }
            }
            val action = when (row.action) {
                AnimeStoreAction.UPDATE -> MR.strings.ext_update
                AnimeStoreAction.TRUST -> MR.strings.ext_trust
                else -> null
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (row.removable) {
                    Button(
                        onClick = { close(onRemove) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.tile, contentColor = tokens.dotOn),
                    ) { Text(tsuzukiMenuLabel(stringResource(MR.strings.ext_uninstall))) }
                }
                if (action != null) {
                    Button(
                        onClick = { close(onAction) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accent,
                            contentColor = tokens.onAccent,
                        ),
                    ) { Text(tsuzukiMenuLabel(stringResource(action))) }
                }
            }
        }
    }
}
