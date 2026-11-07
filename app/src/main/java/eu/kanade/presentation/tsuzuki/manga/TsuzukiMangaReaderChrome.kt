package eu.kanade.presentation.tsuzuki.manga

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.UpIcon
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.reader.TsuzukiReaderTitle
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiMangaReaderTopBar(
    mangaTitle: String?,
    chapterTitle: String?,
    readingMode: ReadingMode?,
    navigateUp: () -> Unit,
    modifier: Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val modeLabel = readingMode?.let { stringResource(it.stringRes) }
    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = navigateUp,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = tokens.tile,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            UpIcon(navigationIcon = Icons.AutoMirrored.Outlined.NavigateBefore)
        }
        Column(Modifier.weight(1f)) {
            TsuzukiReaderTitle(
                title = mangaTitle,
                subtitle = listOfNotNull(chapterTitle, modeLabel).joinToString(" · ").ifEmpty { null },
            )
        }
        actions()
    }
}

@Composable
internal fun MangaReaderFooter(content: @Composable ColumnScope.() -> Unit) {
    val tokens = TsuzukiTheme.tokens
    if (tokens.style == TsuzukiStyle.NONE) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small), content = content)
        return
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = tokens.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = tokens.tileBorder?.let { BorderStroke(1.dp, it) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
internal fun TsuzukiMangaReaderActions(
    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    onClickSettings: () -> Unit,
    modifier: Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = tokens.tile,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    Row(
        modifier = modifier.pointerInput(Unit) {},
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(onClick = onClickReadingMode, colors = colors) {
            Icon(painterResource(readingMode.iconRes), stringResource(MR.strings.viewer))
        }
        FilledIconButton(onClick = onClickOrientation, colors = colors) {
            Icon(orientation.icon, stringResource(MR.strings.rotation_type))
        }
        FilledIconButton(onClick = onClickCropBorder, colors = colors) {
            val icon = if (cropEnabled) R.drawable.ic_crop_24dp else R.drawable.ic_crop_off_24dp
            Icon(painterResource(icon), stringResource(MR.strings.pref_crop_borders))
        }
        FilledIconButton(onClick = onClickSettings, colors = colors) {
            Icon(Icons.Outlined.Settings, stringResource(MR.strings.action_settings))
        }
    }
}

@Composable
internal fun TsuzukiMangaPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier,
    spreadStart: Int? = null,
) {
    val tokens = TsuzukiTheme.tokens
    Surface(
        modifier = modifier.padding(bottom = 8.dp),
        color = tokens.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = CircleShape,
        border = tokens.tileBorder?.let { BorderStroke(1.dp, it) },
    ) {
        Text(
            text = if (spreadStart != null) "$spreadStart–$currentPage / $totalPages" else "$currentPage / $totalPages",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = if (tokens.isDot) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
        )
    }
}
