package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal

private val TileRadius = 24.dp

/** Board "Browse": the wide search field that searches every source at once. */
@Composable
fun TsuzukiGlobalSearchTile(sourceCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(26.dp)
    val text = stringResource(MR.strings.tsz_search_sources, sourceCount)
    val press = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier
            .tsuzukiPress(press)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .clickable(
                interactionSource = press,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Icon(Icons.Outlined.Search, contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
        Text(
            if (t.isDot) text.uppercase() else text,
            color = muted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/**
 * One row of a grouped tile: consecutive rows share one rounded tile, so only the first row gets
 * the top corners and only the last gets the bottom corners.
 */
fun tsuzukiGroupedTile(first: Boolean, last: Boolean): Modifier = Modifier.composed {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(
        topStart = if (first) TileRadius else 0.dp,
        topEnd = if (first) TileRadius else 0.dp,
        bottomStart = if (last) TileRadius else 0.dp,
        bottomEnd = if (last) TileRadius else 0.dp,
    )
    Modifier
        .padding(horizontal = 12.dp)
        .clip(shape)
        .background(t.tile)
        .padding(top = if (first) 4.dp else 0.dp, bottom = if (last) 4.dp else 0.dp)
}

/**
 * Board "Browse": pinned sources as big two-up tiles (icon, pin, name, language). Tap opens the
 * catalogue, long press opens the source options, the pin unpins.
 */
@Composable
fun TsuzukiPinnedSources(
    sources: List<tachiyomi.domain.source.model.Source>,
    onClick: (tachiyomi.domain.source.model.Source) -> Unit,
    onLongClick: (tachiyomi.domain.source.model.Source) -> Unit,
    onUnpin: (tachiyomi.domain.source.model.Source) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: Long? = null,
    showLocalMangaInfo: Boolean = false,
) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(TileRadius)
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.foundation.layout.Column(
        modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sources.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { source ->
                    val localManga = showLocalMangaInfo && source.isLocal()
                    androidx.compose.foundation.layout.Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 128.dp)
                            .clip(shape)
                            .background(t.tile)
                            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                            .tsuzukiSelectedSource(source.id == selectedId, shape)
                            .tsuzukiClickable(
                                role = Role.Button,
                                onClick = { onClick(source) },
                                onLongClick = { onLongClick(source) },
                            )
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            eu.kanade.presentation.browse.components.SourceIcon(source = source)
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                            if (!localManga) {
                                androidx.compose.material3.IconButton(
                                    onClick = { onUnpin(source) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        androidx.compose.material.icons.Icons.Filled.PushPin,
                                        contentDescription = stringResource(MR.strings.action_unpin),
                                        tint = t.accent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        Text(
                            tsuzukiMenuLabel(
                                if (localManga) stringResource(MR.strings.tsz_local_manga) else source.name,
                            ),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val lang = if (localManga) {
                            stringResource(MR.strings.tsz_local_manga_formats)
                        } else {
                            eu.kanade.tachiyomi.util.system.LocaleHelper.getSourceDisplayName(source.lang, context)
                        }
                        Text(
                            if (t.isDot) lang.uppercase() else lang,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Extensions' language pills (board 04-browse "Extensions"). [language] null shows every group; otherwise only that
 * language's group of available extensions is shown, and the update / installed / untrusted sections stay.
 */
fun tsuzukiExtensionsByLanguage(
    items: Map<
        eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel.Header,
        List<eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel.Item>,
        >,
    language: String?,
): Map<
    eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel.Header,
    List<eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel.Item>,
    > {
    if (language == null) return items
    return items.filterKeys {
        it !is eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel.Header.Text || it.text == language
    }
}

/** The language groups present, in list order. */
fun tsuzukiExtensionLanguages(
    items: Map<eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel.Header, *>,
): List<String> = items.keys
    .filterIsInstance<eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel.Header.Text>()
    .map { it.text }

/** "All" + each language, as the shared pill row. Shown only with two or more languages. */
@Composable
fun TsuzukiExtensionLanguagePills(
    languages: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (languages.size < 2) return
    val labels = listOf(tachiyomi.presentation.core.i18n.stringResource(tachiyomi.i18n.MR.strings.all)) + languages
    eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow(
        labels = labels,
        selected = selected?.let { languages.indexOf(it) + 1 }?.takeIf { it > 0 } ?: 0,
        onClick = { i -> onSelect(if (i == 0) null else languages[i - 1]) },
        modifier = modifier,
    )
}

/** [tsuzukiGroupedTile] in the Tsuzuki themes, nothing in stock themes. */
@Composable
fun tsuzukiGroupedTileOrNone(index: Int, lastIndex: Int): Modifier =
    if (eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle()) {
        tsuzukiGroupedTile(first = index == 0, last = index == lastIndex)
    } else {
        Modifier
    }

/**
 * A source's Popular / Latest listing as the segmented control (board 04-browse "Catalogue"). Neither is selected
 * while a search or filter is showing. Sources without a latest listing get a single segment.
 */
@Composable
fun TsuzukiListingSegments(
    supportsLatest: Boolean,
    popular: Boolean,
    latest: Boolean,
    onPopular: () -> Unit,
    onLatest: () -> Unit,
) {
    val labels = buildList {
        add(tachiyomi.presentation.core.i18n.stringResource(tachiyomi.i18n.MR.strings.popular))
        if (supportsLatest) add(tachiyomi.presentation.core.i18n.stringResource(tachiyomi.i18n.MR.strings.latest))
    }
    eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments(
        labels = labels,
        selected = when {
            popular -> 0
            latest && supportsLatest -> 1
            else -> -1
        },
        onClick = { if (it == 0) onPopular() else onLatest() },
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
    )
}

/** The board's in-library marker on catalogue covers: an accent pill, uppercase in Dot. */
@Composable
fun TsuzukiInLibraryBadge() {
    val t = eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme.tokens
    val label = tachiyomi.presentation.core.i18n.stringResource(tachiyomi.i18n.MR.strings.in_library)
    androidx.compose.material3.Text(
        if (t.isDot) label.uppercase() else label,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(t.accent)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = t.onAccent,
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(0.8f, androidx.compose.ui.unit.TextUnitType.Sp),
        ),
        maxLines = 1,
    )
}
