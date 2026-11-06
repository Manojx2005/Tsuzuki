package eu.kanade.presentation.tsuzuki.selection

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.SwapCalls
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DownloadDropdownMenu
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiChapterSelectionBar(
    modifier: Modifier,
    onBookmarkClicked: (() -> Unit)?,
    onRemoveBookmarkClicked: (() -> Unit)?,
    onMarkAsReadClicked: (() -> Unit)?,
    onMarkAsUnreadClicked: (() -> Unit)?,
    onMarkPreviousAsReadClicked: (() -> Unit)?,
    onDownloadClicked: (() -> Unit)?,
    onDeleteClicked: (() -> Unit)?,
    onRemoveFromDbClicked: (() -> Unit)?,
    onDeleteRangeClicked: (() -> Unit)?,
    onDeleteTranslationClicked: (() -> Unit)?,
    onTranslateClicked: (() -> Unit)?,
    onRetranslateClicked: (() -> Unit)?,
) {
    val actions = listOfNotNull(
        onMarkAsReadClicked?.let {
            SelectionAction(
                stringResource(MR.strings.action_mark_as_read),
                Icons.Outlined.DoneAll,
                it,
                shortTitle = stringResource(MR.strings.tsz_q9_mark_read),
            )
        },
        onDownloadClicked?.let {
            SelectionAction(stringResource(MR.strings.action_save), Icons.Outlined.Download, it)
        },
        onDeleteClicked?.let {
            SelectionAction(stringResource(MR.strings.action_delete), Icons.Outlined.Delete, it)
        },
        onBookmarkClicked?.let {
            SelectionAction(stringResource(MR.strings.action_bookmark), Icons.Outlined.BookmarkAdd, it)
        },
        onMarkAsUnreadClicked?.let {
            SelectionAction(stringResource(MR.strings.action_mark_as_unread), Icons.Outlined.RemoveDone, it)
        },
        onRemoveBookmarkClicked?.let {
            SelectionAction(stringResource(MR.strings.action_remove_bookmark), Icons.Outlined.BookmarkRemove, it)
        },
    )
    val more = listOfNotNull(
        onMarkPreviousAsReadClicked?.let {
            SelectionAction(
                stringResource(MR.strings.action_mark_previous_as_read),
                ImageVector.vectorResource(R.drawable.ic_done_prev_24dp),
                it,
            )
        },
        onRemoveFromDbClicked?.let {
            SelectionAction(stringResource(TDMR.strings.action_remove_from_db), Icons.Outlined.DeleteForever, it)
        },
        onDeleteRangeClicked?.let {
            SelectionAction(stringResource(TDMR.strings.action_delete_range), Icons.Outlined.Delete, it)
        },
        onDeleteTranslationClicked?.let {
            SelectionAction(stringResource(TDMR.strings.action_delete_translation), Icons.Outlined.Delete, it)
        },
        onTranslateClicked?.let {
            SelectionAction(stringResource(TDMR.strings.action_translate), Icons.Outlined.MoreVert, it)
        },
        onRetranslateClicked?.let {
            SelectionAction(stringResource(TDMR.strings.action_retranslate), Icons.Outlined.MoreVert, it)
        },
    )
    TsuzukiSelectionBar(actions, modifier, more)
}

@Composable
internal fun TsuzukiLibrarySelectionBar(
    onChangeCategoryClicked: () -> Unit,
    onMarkAsReadClicked: () -> Unit,
    onMarkAsUnreadClicked: () -> Unit,
    onDownloadClicked: ((DownloadAction) -> Unit)?,
    onDeleteClicked: () -> Unit,
    onMigrateClicked: () -> Unit,
    modifier: Modifier,
    onCopyLinksClicked: (() -> Unit)?,
    onUpdateClicked: (() -> Unit)?,
    onTranslateClicked: (() -> Unit)?,
    onExportEpubClicked: (() -> Unit)?,
    onRemoveFromLibraryClicked: (() -> Unit)?,
) {
    val actions = listOfNotNull(
        SelectionAction(
            title = stringResource(MR.strings.action_move_category),
            icon = Icons.AutoMirrored.Outlined.Label,
            onClick = onChangeCategoryClicked,
            shortTitle = stringResource(MR.strings.tsz_q9_shelf),
        ),
        SelectionAction(
            stringResource(MR.strings.action_mark_as_read),
            Icons.Outlined.DoneAll,
            onMarkAsReadClicked,
        ),
        onDownloadClicked?.let {
            SelectionAction(
                title = stringResource(MR.strings.action_save),
                icon = Icons.Outlined.Download,
                menu = { expanded, close ->
                    DownloadDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = close,
                        onDownloadClicked = it,
                        offset = DpOffset(0.dp, 0.dp),
                    )
                },
            )
        },
        SelectionAction(
            stringResource(MR.strings.action_remove),
            Icons.Outlined.Delete,
            onRemoveFromLibraryClicked ?: onDeleteClicked,
        ),
    )
    val more = listOfNotNull(
        SelectionAction(
            stringResource(MR.strings.action_mark_as_unread),
            Icons.Outlined.RemoveDone,
            onMarkAsUnreadClicked,
        ),
        SelectionAction(stringResource(MR.strings.migrate), Icons.Outlined.SwapCalls, onMigrateClicked),
        SelectionAction(stringResource(MR.strings.action_delete), Icons.Outlined.Delete, onDeleteClicked),
        onUpdateClicked?.let {
            SelectionAction(stringResource(MR.strings.action_update_library), Icons.Outlined.MoreVert, it)
        },
        onCopyLinksClicked?.let {
            SelectionAction(stringResource(TDMR.strings.action_copy_links), Icons.Outlined.MoreVert, it)
        },
        onTranslateClicked?.let {
            SelectionAction(stringResource(TDMR.strings.action_translate), Icons.Outlined.MoreVert, it)
        },
        onExportEpubClicked?.let {
            SelectionAction(stringResource(TDMR.strings.action_export_epub), Icons.Outlined.MoreVert, it)
        },
    )
    TsuzukiSelectionBar(actions, modifier, more)
}
