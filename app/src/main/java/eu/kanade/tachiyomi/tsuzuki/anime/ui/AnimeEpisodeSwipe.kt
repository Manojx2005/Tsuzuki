package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownloadOff
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.ChapterSwipeAction
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * AP6: anime episode rows swipe like book chapter rows, with the same Library settings (swipe left / right: toggle
 * watched, toggle bookmark, download or delete the download).
 */
@Composable
internal fun AnimeEpisodeSwipe(
    episode: Episode,
    downloadStatus: AnimeDownloadStatus?,
    enabled: Boolean,
    onToggleSeen: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDownload: () -> Unit,
    content: @Composable () -> Unit,
) {
    val preferences = remember { Injekt.get<LibraryPreferences>() }
    val startAction by preferences.swipeToStartAction.collectAsState()
    val endAction by preferences.swipeToEndAction.collectAsState()
    if (!enabled) {
        content()
        return
    }
    val background = MaterialTheme.colorScheme.primaryContainer
    fun action(kind: ChapterSwipeAction): SwipeAction? = when (kind) {
        ChapterSwipeAction.ToggleRead -> swipeAction(
            if (episode.seen) Icons.Outlined.RemoveDone else Icons.Outlined.Done,
            background,
            isUndo = episode.seen,
            onSwipe = onToggleSeen,
        )
        ChapterSwipeAction.ToggleBookmark -> swipeAction(
            if (episode.bookmark) Icons.Outlined.BookmarkRemove else Icons.Outlined.BookmarkAdd,
            background,
            isUndo = episode.bookmark,
            onSwipe = onToggleBookmark,
        )
        ChapterSwipeAction.Download -> swipeAction(
            when (downloadStatus) {
                null, AnimeDownloadStatus.Failed -> Icons.Outlined.Download
                AnimeDownloadStatus.Queued, AnimeDownloadStatus.Removing -> Icons.Outlined.FileDownloadOff
                AnimeDownloadStatus.Saved -> Icons.Outlined.Delete
            },
            background,
            onSwipe = onDownload,
        )
        ChapterSwipeAction.Disabled -> null
    }
    SwipeableActionsBox(
        modifier = Modifier.clip(RoundedCornerShape(TsuzukiTheme.tokens.tileRadius)),
        startActions = listOfNotNull(action(startAction)),
        endActions = listOfNotNull(action(endAction)),
        swipeThreshold = 56.dp,
        backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        content()
    }
}

private fun swipeAction(icon: ImageVector, background: Color, isUndo: Boolean = false, onSwipe: () -> Unit) =
    SwipeAction(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(16.dp),
                tint = contentColorFor(background),
            )
        },
        background = background,
        onSwipe = onSwipe,
        isUndo = isUndo,
    )
