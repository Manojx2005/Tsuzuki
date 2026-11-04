package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.manga.model.downloadedFilter
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiAdaptiveSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** ChapterFilter: one page using the original book filters, sorting and defaults actions. */
@Composable
fun TsuzukiChapterFilterSheet(
    onDismissRequest: () -> Unit,
    manga: Manga?,
    onDownloadFilterChanged: ((TriState) -> Unit)?,
    onUnreadFilterChanged: (TriState) -> Unit,
    onBookmarkedFilterChanged: (TriState) -> Unit,
    scanlatorFilterActive: Boolean,
    onScanlatorFilterClicked: () -> Unit,
    onSortModeChanged: (Long) -> Unit,
    onSortOrderChanged: ((Long, Boolean) -> Unit)?,
    onDisplayModeChanged: (Long) -> Unit,
    onSetAsDefault: () -> Unit,
    onResetToDefault: () -> Unit,
    markPreviousCount: Int = 0,
    onMarkPreviousRead: (() -> Unit)? = null,
) {
    val tokens = TsuzukiTheme.tokens
    val sorting = manga?.sorting ?: Manga.CHAPTER_SORTING_SOURCE
    val descending = manga?.sortDescending() ?: true
    val display = manga?.displayMode ?: Manga.CHAPTER_DISPLAY_NAME
    val sortModes = listOf(
        MR.strings.sort_by_source to Manga.CHAPTER_SORTING_SOURCE,
        MR.strings.sort_by_number to Manga.CHAPTER_SORTING_NUMBER,
        MR.strings.sort_by_upload_date to Manga.CHAPTER_SORTING_UPLOAD_DATE,
        MR.strings.action_sort_alpha to Manga.CHAPTER_SORTING_ALPHABET,
    )
    val setOrder: (Long, Boolean) -> Unit = { mode, newest ->
        if (onSortOrderChanged != null) {
            onSortOrderChanged(mode, newest)
        } else if (sorting != mode || descending != newest) {
            onSortModeChanged(mode)
        }
    }
    var menuExpanded by remember { mutableStateOf(false) }

    TsuzukiAdaptiveSheet(onDismissRequest = onDismissRequest) { close ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 10.dp, bottom = 20.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(tokens.tileBorder ?: tokens.track),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = themedText(stringResource(MR.strings.chapters)),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 30.sp,
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, stringResource(MR.strings.label_more))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(themedText(stringResource(MR.strings.set_chapter_settings_as_default))) },
                            onClick = {
                                menuExpanded = false
                                onSetAsDefault()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(themedText(stringResource(MR.strings.action_reset))) },
                            onClick = {
                                menuExpanded = false
                                onResetToDefault()
                            },
                        )
                        TsuzukiSectionLabel(stringResource(MR.strings.tsz_chapter_filter_sort_method))
                        sortModes.forEach { (label, mode) ->
                            DropdownMenuItem(
                                text = { Text(themedText(stringResource(label))) },
                                trailingIcon = if (sorting == mode) {
                                    { Icon(Icons.Filled.Check, contentDescription = null) }
                                } else {
                                    null
                                },
                                onClick = {
                                    menuExpanded = false
                                    setOrder(mode, descending)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(themedText(stringResource(MR.strings.tsz_chapter_filter_both))) },
                            trailingIcon = if (display == Manga.CHAPTER_DISPLAY_BOTH) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else {
                                null
                            },
                            onClick = {
                                menuExpanded = false
                                onDisplayModeChanged(Manga.CHAPTER_DISPLAY_BOTH)
                            },
                        )
                    }
                }
            }
            FilterGroup {
                FilterRow(
                    label = stringResource(MR.strings.action_filter_unread),
                    value = manga?.unreadFilter ?: TriState.DISABLED,
                    onChange = onUnreadFilterChanged,
                )
                FilterRow(
                    label = stringResource(MR.strings.tsz_chapter_filter_saved),
                    value = manga?.downloadedFilter ?: TriState.DISABLED,
                    onChange = onDownloadFilterChanged,
                )
                FilterRow(
                    label = stringResource(MR.strings.action_filter_bookmarked),
                    value = manga?.bookmarkedFilter ?: TriState.DISABLED,
                    onChange = onBookmarkedFilterChanged,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onScanlatorFilterClicked)
                        .heightIn(min = 50.dp).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Outlined.PeopleAlt,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = if (scanlatorFilterActive) tokens.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        themedText(stringResource(MR.strings.scanlator)),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        themedText(
                            stringResource(
                                if (scanlatorFilterActive) {
                                    MR.strings.tsz_chapter_filter_only
                                } else {
                                    MR.strings.tsz_chapter_filter_any
                                },
                            ),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TsuzukiSectionLabel(stringResource(MR.strings.action_sort))
            val baseSorting = sorting.takeUnless { it == Manga.CHAPTER_SORTING_UPLOAD_DATE }
                ?: Manga.CHAPTER_SORTING_SOURCE
            TsuzukiSegments(
                labels = listOf(
                    stringResource(MR.strings.tsz_chapter_filter_newest),
                    stringResource(MR.strings.tsz_chapter_filter_oldest),
                    stringResource(MR.strings.tsz_chapter_filter_upload_date),
                ),
                selected = if (sorting == Manga.CHAPTER_SORTING_UPLOAD_DATE) {
                    2
                } else if (descending) {
                    0
                } else {
                    1
                },
                onClick = { index ->
                    when (index) {
                        0 -> setOrder(baseSorting, true)
                        1 -> setOrder(baseSorting, false)
                        2 -> setOrder(
                            Manga.CHAPTER_SORTING_UPLOAD_DATE,
                            sorting != Manga.CHAPTER_SORTING_UPLOAD_DATE || !descending,
                        )
                    }
                },
            )
            TsuzukiSectionLabel(stringResource(MR.strings.tsz_chapter_filter_show_as))
            TsuzukiSegments(
                labels = listOf(
                    stringResource(MR.strings.tsz_chapter_filter_titles),
                    stringResource(MR.strings.tsz_chapter_filter_numbers),
                ),
                selected = when (display) {
                    Manga.CHAPTER_DISPLAY_NAME -> 0
                    Manga.CHAPTER_DISPLAY_NUMBER -> 1
                    else -> -1
                },
                onClick = {
                    onDisplayModeChanged(
                        if (it == 0) Manga.CHAPTER_DISPLAY_NAME else Manga.CHAPTER_DISPLAY_NUMBER,
                    )
                },
            )
            if (display == Manga.CHAPTER_DISPLAY_BOTH) {
                Text(
                    themedText(stringResource(MR.strings.tsz_chapter_filter_both)),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                if (onMarkPreviousRead != null) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            onMarkPreviousRead()
                            close(onDismissRequest)
                        },
                        enabled = markPreviousCount > 0,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        Text(
                            themedText(
                                stringResource(MR.strings.tsz_chapter_filter_mark_previous, markPreviousCount),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
                Button(
                    onClick = { close(onDismissRequest) },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.accent,
                        contentColor = tokens.onAccent,
                    ),
                ) {
                    Text(
                        themedText(stringResource(MR.strings.tsz_chapter_filter_done)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterGroup(content: @Composable () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(shape).background(tokens.tile)
            .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(vertical = 4.dp),
    ) { content() }
}

@Composable
private fun FilterRow(label: String, value: TriState, onChange: ((TriState) -> Unit)?) {
    val tokens = TsuzukiTheme.tokens
    val enabled = onChange != null
    val active = value != TriState.DISABLED
    val stateLabel = stringResource(
        when (value) {
            TriState.DISABLED -> MR.strings.tsz_chapter_filter_any
            TriState.ENABLED_IS -> MR.strings.tsz_chapter_filter_only
            TriState.ENABLED_NOT -> MR.strings.tsz_chapter_filter_hide
        },
    )
    Row(
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f)
            .semantics(mergeDescendants = true) { stateDescription = stateLabel }
            .triStateToggleable(
                state = when (value) {
                    TriState.DISABLED -> ToggleableState.Off
                    TriState.ENABLED_IS -> ToggleableState.On
                    TriState.ENABLED_NOT -> ToggleableState.Indeterminate
                },
                enabled = enabled,
                role = Role.Checkbox,
                onClick = { onChange?.invoke(value.next()) },
            ).heightIn(min = 50.dp).padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(8.dp)
        Box(
            Modifier.size(26.dp).clip(shape)
                .then(
                    if (active) {
                        Modifier.background(tokens.accent)
                    } else {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, shape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (active) {
                Icon(
                    if (value == TriState.ENABLED_IS) Icons.Filled.Check else Icons.Filled.Remove,
                    contentDescription = null,
                    tint = tokens.onAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            themedText(label),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            themedText(stateLabel),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun themedText(text: String): String = if (TsuzukiTheme.tokens.isDot) text.uppercase() else text
