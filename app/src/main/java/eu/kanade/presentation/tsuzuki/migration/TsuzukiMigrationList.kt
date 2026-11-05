package eu.kanade.presentation.tsuzuki.migration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import mihon.feature.migration.list.models.MigratingManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

/**
 * Migration list in the Tsuzuki themes (04-browse/MigrationCompare): each book is an OLD card beside its NEW match,
 * the match outlined in the accent once found, with the row's actions underneath. Same callbacks as the stock list.
 */
@Composable
internal fun TsuzukiMigrationListContent(
    items: List<MigratingManga>,
    migrationComplete: Boolean,
    finishedCount: Int,
    onItemClick: (Manga) -> Unit,
    onSearchManually: (MigratingManga) -> Unit,
    onSkip: (Long) -> Unit,
    onMigrate: (Long) -> Unit,
    onCopy: (Long) -> Unit,
    openMigrationDialog: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = if (items.isNotEmpty()) {
                    stringResource(MR.strings.migrationListScreenTitleWithProgress, finishedCount, items.size)
                } else {
                    stringResource(MR.strings.migrationListScreenTitle)
                },
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.migrationListScreen_copyActionLabel),
                                icon = if (items.size == 1) Icons.Outlined.ContentCopy else Icons.Outlined.CopyAll,
                                onClick = { openMigrationDialog(true) },
                                enabled = migrationComplete,
                            ),
                            AppBar.Action(
                                title = stringResource(MR.strings.migrationListScreen_migrateActionLabel),
                                icon = if (items.size == 1) Icons.Outlined.Done else Icons.Outlined.DoneAll,
                                onClick = { openMigrationDialog(false) },
                                enabled = migrationComplete,
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        FastScrollLazyColumn(contentPadding = contentPadding + topSmallPaddingValues) {
            items(items, key = { it.manga.id }, contentType = { "migration_item" }) { item ->
                val result by item.searchResult.collectAsState()
                TsuzukiMigrationRow(
                    item = item,
                    result = result,
                    onItemClick = onItemClick,
                    onSearchManually = { onSearchManually(item) },
                    onSkip = { onSkip(item.manga.id) },
                    onMigrate = { onMigrate(item.manga.id) },
                    onCopy = { onCopy(item.manga.id) },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun MigrationRowAction.label(): StringResource = when (this) {
    MigrationRowAction.Skip -> MR.strings.migrationListScreen_skipActionLabel
    MigrationRowAction.SearchManually -> MR.strings.migrationListScreen_searchManuallyActionLabel
    MigrationRowAction.CopyNow -> MR.strings.migrationListScreen_copyNowActionLabel
    MigrationRowAction.MigrateNow -> MR.strings.migrationListScreen_migrateNowActionLabel
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TsuzukiMigrationRow(
    item: MigratingManga,
    result: MigratingManga.SearchResult,
    onItemClick: (Manga) -> Unit,
    onSearchManually: () -> Unit,
    onSkip: () -> Unit,
    onMigrate: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TsuzukiMigrationSide(
                label = stringResource(MR.strings.tsz_migration_old, item.source),
                manga = item.manga,
                chapterCount = item.chapterCount,
                latestChapter = item.latestChapter,
                highlighted = false,
                onClick = { onItemClick(item.manga) },
                modifier = Modifier.weight(1f),
            )
            when (result) {
                MigratingManga.SearchResult.Searching -> TsuzukiMigrationPlaceholder(
                    label = stringResource(MR.strings.tsz_migration_searching),
                    searching = true,
                    modifier = Modifier.weight(1f),
                )
                MigratingManga.SearchResult.NotFound -> TsuzukiMigrationPlaceholder(
                    label = stringResource(MR.strings.migrationListScreen_noMatchFoundText),
                    searching = false,
                    modifier = Modifier.weight(1f),
                )
                is MigratingManga.SearchResult.Success -> TsuzukiMigrationSide(
                    label = stringResource(MR.strings.tsz_migration_new, result.source),
                    manga = result.manga,
                    chapterCount = result.chapterCount,
                    latestChapter = result.latestChapter,
                    highlighted = true,
                    onClick = { onItemClick(result.manga) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (action in migrationRowActions(result)) {
                val text = tsuzukiCase(stringResource(action.label())).orEmpty()
                val onClick = when (action) {
                    MigrationRowAction.Skip -> onSkip
                    MigrationRowAction.SearchManually -> onSearchManually
                    MigrationRowAction.CopyNow -> onCopy
                    MigrationRowAction.MigrateNow -> onMigrate
                }
                if (action == MigrationRowAction.MigrateNow) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accent,
                            contentColor = tokens.onAccent,
                        ),
                    ) {
                        Text(text = text, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = tokens.tile,
                            contentColor = tokens.dotOn,
                        ),
                        border = BorderStroke(1.dp, tokens.tileBorder ?: tokens.track),
                    ) {
                        Text(text = text, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun TsuzukiMigrationSide(
    label: String,
    manga: Manga,
    chapterCount: Int,
    latestChapter: Double?,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(tokens.tileRadius)
    val outline = if (highlighted) Modifier.border(2.dp, tokens.accent, shape) else Modifier
    val muted = tokens.dotOn.copy(alpha = 0.72f)
    TsuzukiTile(
        modifier = modifier
            .then(outline)
            .clip(shape)
            .tsuzukiClickable(role = Role.Button, onClick = onClick),
        padding = 10.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = tsuzukiCase(label).orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = if (highlighted) tokens.accent else muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MangaCover.Book(
                data = manga,
                modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(),
            )
            Text(
                text = tsuzukiCase(manga.title).orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val latest = remember(latestChapter) { latestChapter?.let(::formatChapterNumber) }
            Text(
                text = tsuzukiCase(
                    pluralStringResource(MR.plurals.manga_num_chapters, chapterCount, chapterCount),
                ).orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
            Text(
                text = tsuzukiCase(
                    stringResource(
                        MR.strings.migrationListScreen_latestChapterLabel,
                        latest ?: stringResource(MR.strings.migrationListScreen_unknownLatestChapter),
                    ),
                ).orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TsuzukiMigrationPlaceholder(
    label: String,
    searching: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(modifier = modifier, padding = 10.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(MangaCover.Book.ratio),
                contentAlignment = Alignment.Center,
            ) {
                if (searching) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = tokens.accent)
                } else {
                    Image(
                        painter = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(MangaCover.Book.ratio)
                            .clip(MaterialTheme.shapes.extraSmall),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Text(
                text = tsuzukiCase(label).orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = tokens.dotOn.copy(alpha = 0.72f),
            )
        }
    }
}
