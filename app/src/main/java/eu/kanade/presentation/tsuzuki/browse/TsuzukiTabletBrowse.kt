package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.SourceUiModel
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel.Listing
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.domain.manga.model.MangaCover as CoverData

@Composable
internal fun TsuzukiTabletBrowse(
    items: List<SourceUiModel>,
    contentPadding: PaddingValues,
    onOpen: (Source, Listing) -> Unit,
    sourceList: @Composable (Long?, (Source, Listing) -> Unit) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = remember(items, selectedId) {
        items.asSequence()
            .filterIsInstance<SourceUiModel.Item>()
            .map { it.source }
            .firstOrNull { it.id == selectedId }
    }
    val tokens = TsuzukiTheme.tokens
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(0.35f).fillMaxHeight().background(tokens.tile)) {
            sourceList(selected?.id) { source, listing ->
                if (listing == Listing.Popular && !source.isStub) {
                    selectedId = source.id
                } else {
                    onOpen(source, listing)
                }
            }
        }
        VerticalDivider(color = tokens.tileBorder ?: tokens.track)
        Box(Modifier.weight(0.65f).fillMaxHeight()) {
            if (selected == null) {
                Text(
                    text = tsuzukiCase(stringResource(MR.strings.tsz_browse_select_source)).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            } else {
                key(selected.id) {
                    CataloguePreview(
                        source = selected,
                        contentPadding = contentPadding,
                        onOpen = { onOpen(selected, Listing.Popular) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun Modifier.tsuzukiSelectedSource(
    selected: Boolean,
    shape: Shape = RoundedCornerShape(12.dp),
): Modifier = if (selected) {
    border(1.dp, TsuzukiTheme.tokens.accent, shape).semantics { this.selected = true }
} else {
    this
}

private sealed interface Preview {
    data object Loading : Preview
    data object Error : Preview
    data class Ready(val books: List<SManga>) : Preview
}

@Composable
private fun CataloguePreview(source: Source, contentPadding: PaddingValues, onOpen: () -> Unit) {
    val context = LocalContext.current
    val sourceManager = remember { Injekt.get<SourceManager>() }
    var attempt by remember { mutableIntStateOf(0) }
    val preview by produceState<Preview>(Preview.Loading, source.id, attempt) {
        value = Preview.Loading
        value = try {
            val books = withIOContext {
                (sourceManager.get(source.id) as? CatalogueSource)?.getPopularManga(1)?.mangas.orEmpty()
            }
            Preview.Ready(books)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Tablet catalogue preview failed for source ${source.id}" }
            Preview.Error
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(
                text = tsuzukiCase(source.name).orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tsuzukiCase(LocaleHelper.getSourceDisplayName(source.lang, context)).orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpen) {
                Text(
                    text = tsuzukiCase(stringResource(MR.strings.tsz_browse_open_catalogue)).orEmpty(),
                    color = TsuzukiTheme.tokens.accent,
                )
            }
        }
        TsuzukiSectionLabel(
            text = stringResource(MR.strings.popular),
            contentPadding = PaddingValues(bottom = 8.dp),
        )
        when (val result = preview) {
            Preview.Loading -> LoadingScreen(Modifier.fillMaxSize())
            Preview.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { attempt++ }) {
                    Text(tsuzukiCase(stringResource(MR.strings.action_retry)).orEmpty())
                }
            }
            is Preview.Ready -> if (result.books.isEmpty()) {
                EmptyScreen(stringRes = MR.strings.no_results_found, modifier = Modifier.fillMaxSize())
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(108.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 12.dp),
                ) {
                    items(result.books) { book ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MangaCover.Book(
                                data = CoverData(
                                    mangaId = 0L,
                                    sourceId = source.id,
                                    isMangaFavorite = false,
                                    url = book.thumbnail_url,
                                    lastModified = 0L,
                                    title = book.title,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = tsuzukiCase(book.title).orEmpty(),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
