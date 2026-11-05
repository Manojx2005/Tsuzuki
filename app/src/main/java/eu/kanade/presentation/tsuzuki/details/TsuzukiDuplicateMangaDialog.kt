package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.presentation.tsuzuki.settings.tsuzukiDialogColor
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun TsuzukiDuplicateMangaDialog(
    duplicates: List<MangaWithChapterCount>,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onOpenManga: (Manga) -> Unit,
    onMigrate: (Manga) -> Unit,
    modifier: Modifier,
    canAddAnyway: Boolean,
) {
    val tokens = TsuzukiTheme.tokens
    val sourceManager = remember { Injekt.get<SourceManager>() }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = duplicates.firstOrNull { it.manga.id == selectedId } ?: duplicates.firstOrNull()
    val openLabel = stringResource(MR.strings.tsz_open)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp).widthIn(max = 440.dp).fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = tsuzukiDialogColor() ?: tokens.background,
            contentColor = tokens.dotOn,
            border = BorderStroke(1.dp, tokens.tileBorder ?: tokens.track),
        ) {
            Column(
                modifier = Modifier.verticalScroll(
                    rememberScrollState(),
                ).padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = tsuzukiCase(stringResource(MR.strings.in_library)).orEmpty(),
                    color = tokens.accent,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = tsuzukiCase(stringResource(MR.strings.possible_duplicates_title)).orEmpty(),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = stringResource(MR.strings.possible_duplicates_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.dotOn.copy(alpha = 0.72f),
                )
                if (selected != null) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val cardWidth = (maxWidth - 10.dp) / 2
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        ) {
                            items(duplicates, key = { it.manga.id }, contentType = { "duplicate_card" }) { duplicate ->
                                val source = sourceManager.getOrStub(duplicate.manga.source)
                                TsuzukiDuplicateCard(
                                    duplicate = duplicate,
                                    sourceName = source.name,
                                    isStubSource = source is StubSource,
                                    isSelected = duplicate.manga.id == selected.manga.id,
                                    openLabel = openLabel,
                                    onSelect = { selectedId = duplicate.manga.id },
                                    onOpen = { onOpenManga(duplicate.manga) },
                                    modifier = Modifier.width(cardWidth),
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            onDismissRequest()
                            onMigrate(selected.manga)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accent,
                            contentColor = tokens.onAccent,
                        ),
                    ) {
                        Text(
                            text = tsuzukiCase(stringResource(MR.strings.migrate)).orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    OutlinedButton(
                        onClick = { onOpenManga(selected.manga) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = tokens.tile,
                            contentColor = tokens.dotOn,
                        ),
                        border = BorderStroke(1.dp, tokens.tileBorder ?: tokens.track),
                    ) {
                        Text(text = tsuzukiCase(openLabel).orEmpty(), style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (canAddAnyway) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onConfirm()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = tsuzukiCase(stringResource(MR.strings.action_add_anyway)).orEmpty(),
                            color = tokens.accent,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                TextButton(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = tsuzukiCase(stringResource(MR.strings.action_cancel)).orEmpty(),
                        color = tokens.dotOn.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun TsuzukiDuplicateCard(
    duplicate: MangaWithChapterCount,
    sourceName: String,
    isStubSource: Boolean,
    isSelected: Boolean,
    openLabel: String,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(tokens.tileRadius)
    val outline = if (isSelected) Modifier.border(2.dp, tokens.accent, shape) else Modifier
    TsuzukiTile(
        modifier = modifier
            .clip(shape)
            .then(outline)
            .semantics { selected = isSelected }
            .tsuzukiClickable(
                role = Role.RadioButton,
                onLongClickLabel = openLabel,
                onLongClick = onOpen,
                onClick = onSelect,
            ),
        padding = 10.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MangaCover.Square(
                data = duplicate.manga,
                modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 140.dp).fillMaxWidth(),
            )
            Text(
                text = tsuzukiCase(duplicate.manga.title).orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isStubSource) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = tokens.accent,
                        )
                    }
                    Text(
                        text = tsuzukiCase(sourceName).orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.dotOn.copy(alpha = 0.72f),
                    )
                }
                Text(
                    text = tsuzukiCase(
                        pluralStringResource(
                            MR.plurals.manga_num_chapters,
                            duplicate.chapterCount.toInt(),
                            duplicate.chapterCount,
                        ),
                    ).orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.dotOn.copy(alpha = 0.72f),
                )
            }
        }
    }
}
