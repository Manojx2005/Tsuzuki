package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import eu.kanade.presentation.components.imeAwareDialogProperties
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.R
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUserEdits
import tachiyomi.domain.entries.anime.model.animeUserEdits
import tachiyomi.domain.entries.anime.model.withUserEdits
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

/**
 * AP7-EDIT: the book "Edit info" screen for a saved anime, built from the same tiles as [TsuzukiEditMangaDialog]
 * (no cover, URL or alternative titles: anime keep none of those). [anime] is the stored row, so its fields are the
 * source's; the fields start at what the details show. Like the book editor it saves on Close and Back. Only fields
 * that differ from the source are kept, and Reset removes every edit.
 */
@Composable
internal fun TsuzukiEditAnimeDialog(
    anime: Anime,
    onDismissRequest: () -> Unit,
    onSave: (AnimeUserEdits?) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val shown = remember(anime) { anime.withUserEdits() }
    var title by remember { mutableStateOf(shown.title) }
    var author by remember { mutableStateOf(shown.author.orEmpty()) }
    var artist by remember { mutableStateOf(shown.artist.orEmpty()) }
    var description by remember { mutableStateOf(shown.description.orEmpty()) }
    var tags by remember { mutableStateOf(shown.genre.orEmpty()) }
    var status by remember { mutableLongStateOf(shown.status) }
    var didFinish by remember { mutableStateOf(false) }
    val hasEdits = !anime.memo.animeUserEdits().isEmpty

    // Saves on dismissal, Back included, as the book editor does; the guard keeps Reset from saving again after it.
    val saveAndClose: () -> Unit = {
        if (!didFinish) {
            didFinish = true
            onSave(AnimeUserEdits.diff(anime, title, author, artist, description, tags, status))
            onDismissRequest()
        }
    }
    val resetAndClose: () -> Unit = {
        if (!didFinish) {
            didFinish = true
            onSave(null)
            onDismissRequest()
        }
    }

    Dialog(onDismissRequest = saveAndClose, properties = imeAwareDialogProperties) {
        Box(
            modifier = Modifier.fillMaxSize().tsuzukiBackground(tokens).systemBarsPadding().imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = saveAndClose,
                        modifier = Modifier.clip(CircleShape).background(tokens.tile),
                    ) {
                        Icon(Icons.Outlined.Close, mokoStringResource(MR.strings.action_close), tint = tokens.dotOn)
                    }
                    Text(
                        text = tsuzukiCase(mokoStringResource(MR.strings.tsz_edit_info)).orEmpty(),
                        modifier = Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                        color = tokens.dotOn,
                    )
                    Button(
                        onClick = saveAndClose,
                        modifier = Modifier.heightIn(min = 44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accent,
                            contentColor = tokens.onAccent,
                        ),
                    ) {
                        Text(
                            text = tsuzukiCase(mokoStringResource(MR.strings.action_save)).orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EditInfoTile(mokoStringResource(MR.strings.title), title, { title = it })
                    EditInfoTile(mokoStringResource(MR.strings.author), author, { author = it })
                    EditInfoTile(mokoStringResource(MR.strings.artist), artist, { artist = it })
                    EditInfoTile(
                        mokoStringResource(TDMR.strings.edit_label_description),
                        description,
                        { description = it },
                        minLines = 3,
                        maxLines = 8,
                    )
                    // SManga and SAnime share the status numbers, so the book's status pills fit as they are.
                    EditStatusTile(status = status, onStatusChange = { status = it })
                    EditTagsTile(tags = tags, onTagsChange = { tags = it })
                    Text(
                        text = stringResource(R.string.tsz_anime_edit_info_note),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.dotOn.copy(alpha = 0.7f),
                    )
                    if (hasEdits) {
                        TextButton(onClick = resetAndClose, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(
                                text = tsuzukiCase(mokoStringResource(MR.strings.action_reset)).orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.accent,
                            )
                        }
                    }
                }
            }
        }
    }
}
