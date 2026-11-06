package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiDetailsDescription(
    defaultExpanded: Boolean,
    hasSummary: Boolean,
    tags: List<String>,
    onTagSearch: (String) -> Unit,
    onCopyTag: (String) -> Unit,
    modifier: Modifier = Modifier,
    summary: @Composable (expanded: Boolean) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(defaultExpanded) }
    Column(
        modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (hasSummary) {
            summary(expanded)
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    stringResource(if (expanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand),
                )
            }
        }
        if (tags.isNotEmpty()) {
            TsuzukiGenreRows(tags, onTagSearch, onCopyTag)
        }
    }
}

private enum class GenreSlot { TAGS, MEASURE_MORE, MORE }

@Composable
private fun TsuzukiGenreRows(
    tags: List<String>,
    onSearch: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
        val width = constraints.maxWidth
        val gap = 8.dp.roundToPx()
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val chips = subcompose(GenreSlot.TAGS) {
            tags.forEachIndexed { index, tag ->
                key(index, tag) {
                    GenreChip(tag, onSearch, onCopy)
                }
            }
        }.map { it.measure(itemConstraints) }
        val widths = chips.map { it.width }
        val shown = genresFittingTwoLines(widths, width, gap)
        val visible = if (shown == chips.size) {
            chips
        } else {
            // Reserve the width of the largest possible count before deciding which tags fit.
            val moreWidth = subcompose(GenreSlot.MEASURE_MORE) {
                TsuzukiPill("+${tags.size}", false, {}, role = Role.Button)
            }.single().measure(itemConstraints).width
            val visibleCount = genresFittingTwoLines(widths, width, gap, moreWidth)
            val more = subcompose(GenreSlot.MORE) {
                MoreGenresChip(tags.drop(visibleCount), onSearch, onCopy)
            }.single().measure(itemConstraints)
            chips.take(visibleCount) + more
        }

        var x = 0
        var y = 0
        var lineHeight = 0
        val offsets = visible.map { chip ->
            if (x > 0 && x + chip.width > width) {
                y += lineHeight + gap
                x = 0
                lineHeight = 0
            }
            val offset = x to y
            x += chip.width + gap
            lineHeight = maxOf(lineHeight, chip.height)
            offset
        }
        layout(width, y + lineHeight) {
            visible.forEachIndexed { index, chip ->
                val (left, top) = offsets[index]
                chip.placeRelative(left, top)
            }
        }
    }
}

/** Count only chips that fit in two rows, optionally reserving room for the overflow chip. */
internal fun genresFittingTwoLines(widths: List<Int>, maxWidth: Int, gap: Int, moreWidth: Int? = null): Int {
    var row = 1
    var used = 0
    var shown = 0
    for (width in widths) {
        if (used > 0 && used + gap + width > maxWidth) {
            if (row == 2) break
            row++
            used = 0
        }
        val afterChip = if (used == 0) width else used + gap + width
        if (moreWidth != null && row == 2 && afterChip + gap + moreWidth > maxWidth) break
        used = afterChip
        shown++
    }
    return shown
}

@Composable
private fun GenreChip(tag: String, onSearch: (String) -> Unit, onCopy: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        TsuzukiPill(tag, false, { menu = true }, role = Role.Button)
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            GenreActions(tag, onSearch, onCopy) { menu = false }
        }
    }
}

@Composable
private fun MoreGenresChip(tags: List<String>, onSearch: (String) -> Unit, onCopy: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    val dismiss = {
        menu = false
        selected = null
    }
    Box {
        TsuzukiPill("+${tags.size}", false, { menu = true }, role = Role.Button)
        DropdownMenu(expanded = menu, onDismissRequest = dismiss) {
            val tag = selected
            if (tag == null) {
                tags.forEach { hiddenTag ->
                    TsuzukiMenuItem(hiddenTag, onClick = { selected = hiddenTag })
                }
            } else {
                GenreActions(tag, onSearch, onCopy, dismiss)
            }
        }
    }
}

@Composable
private fun GenreActions(tag: String, onSearch: (String) -> Unit, onCopy: (String) -> Unit, dismiss: () -> Unit) {
    TsuzukiMenuItem(stringResource(MR.strings.action_search), onClick = {
        dismiss()
        onSearch(tag)
    })
    TsuzukiMenuItem(stringResource(MR.strings.action_copy_to_clipboard), onClick = {
        dismiss()
        onCopy(tag)
    })
}
