package eu.kanade.presentation.tsuzuki.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import sh.calvin.reorderable.ReorderableCollectionItemScope
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * A shelf in the Tsuzuki themes (board 07-system "Categories"): drag handle, name, content type (and "Default
 * category" for the one new books go to), the include-in-global-update switch, and a menu with rename / delete.
 * Tapping the row renames, as in the stock list. The board's per-shelf book count is left out: there's no count query.
 */
@Composable
fun ReorderableCollectionItemScope.TsuzukiCategoryListItem(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    libraryPreferences: LibraryPreferences = remember { Injekt.get() },
) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val defaultCategory by libraryPreferences.defaultCategory.collectAsState()
    val include by libraryPreferences.updateCategories.collectAsState()
    val exclude by libraryPreferences.updateCategoriesExclude.collectAsState()
    val id = category.id.toString()
    val updates = includedInUpdates(id, include, exclude)

    val shape = RoundedCornerShape(24.dp)
    val press = remember { MutableInteractionSource() }
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier
            .tsuzukiPress(press)
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .clickable(interactionSource = press, indication = ripple(), role = Role.Button, onClick = onRename)
            .padding(start = 6.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.DragIndicator,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.padding(10.dp).size(22.dp).draggableHandle(),
        )
        Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
            Text(
                up(category.name),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val type = when (category.contentType) {
                Category.CONTENT_TYPE_MANGA -> stringResource(TDMR.strings.category_content_type_manga)
                Category.CONTENT_TYPE_NOVEL -> stringResource(TDMR.strings.category_content_type_novel)
                else -> stringResource(TDMR.strings.category_content_type_all)
            }
            val sub = if (defaultCategory.toLong() == category.id) {
                "$type · ${stringResource(MR.strings.default_category)}"
            } else {
                type
            }
            Text(
                up(sub),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = muted,
                maxLines = 2,
            )
        }
        val updateLabel = stringResource(MR.strings.pref_category_library_update)
        Switch(
            checked = updates,
            onCheckedChange = { setIncludedInUpdates(libraryPreferences, id, it) },
            modifier = Modifier.semantics { contentDescription = updateLabel },
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
                TsuzukiMenuItem(stringResource(MR.strings.action_rename_category), onClick = {
                    menu = false
                    onRename()
                })
                TsuzukiMenuItem(stringResource(MR.strings.action_delete), onClick = {
                    menu = false
                    onDelete()
                }, danger = true)
            }
        }
    }
}

/** Mihon's rule: an empty include set means every shelf, and an excluded shelf is never updated. */
internal fun includedInUpdates(id: String, include: Set<String>, exclude: Set<String>): Boolean =
    id !in exclude && (include.isEmpty() || id in include)

/**
 * The include and exclude sets after switching shelf [id] on or off. Off only adds it to the exclude set: removing it
 * from the include set could empty that set, which would mean "every shelf".
 */
internal fun toggledUpdateSets(
    id: String,
    on: Boolean,
    include: Set<String>,
    exclude: Set<String>,
): Pair<Set<String>, Set<String>> = if (on) {
    // With an include list, a shelf has to be on it to be updated
    (if (include.isEmpty()) include else include + id) to (exclude - id)
} else {
    include to (exclude + id)
}

private fun setIncludedInUpdates(prefs: LibraryPreferences, id: String, on: Boolean) {
    val (include, exclude) = toggledUpdateSets(
        id,
        on,
        prefs.updateCategories.get(),
        prefs.updateCategoriesExclude.get(),
    )
    prefs.updateCategories.set(include)
    prefs.updateCategoriesExclude.set(exclude)
}
