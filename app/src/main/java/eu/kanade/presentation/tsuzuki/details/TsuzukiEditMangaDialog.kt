package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import eu.kanade.presentation.components.imeAwareDialogProperties
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiEditMangaDialog(
    manga: Manga,
    onDismissRequest: () -> Unit,
    onSaveTitle: (String) -> Unit,
    onSaveUrl: (String) -> Unit,
    onSaveAltTitles: (List<String>) -> Unit,
    onSaveInfo: (description: String, tags: List<String>, author: String, artist: String, status: Long) -> Unit,
    onSwapMainTitle: ((String, List<String>) -> Unit)?,
    sourceValues: @Composable () -> Unit,
    alternativeTitles: @Composable (
        title: String,
        altTitles: List<String>,
        onAltTitlesChange: (List<String>) -> Unit,
        onSwap: ((String, List<String>) -> Unit)?,
    ) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    var title by remember { mutableStateOf(manga.title) }
    var description by remember { mutableStateOf(manga.description.orEmpty()) }
    var url by remember { mutableStateOf(manga.url) }
    var tags by remember { mutableStateOf(manga.genre.orEmpty()) }
    var altTitles by remember { mutableStateOf(manga.alternativeTitles) }
    var author by remember { mutableStateOf(manga.author.orEmpty()) }
    var artist by remember { mutableStateOf(manga.artist.orEmpty()) }
    var status by remember { mutableLongStateOf(manga.status) }
    var didFinish by remember { mutableStateOf(false) }

    // The existing editor saves on dismissal, including Back. Keep that behavior in the new layout.
    val saveAndClose: () -> Unit = {
        if (!didFinish) {
            didFinish = true
            onSaveTitle(title)
            onSaveUrl(url)
            onSaveAltTitles(altTitles)
            onSaveInfo(description, tags, author, artist, status)
            onDismissRequest()
        }
    }
    val swapAndClose = onSwapMainTitle?.let { swap ->
        { newTitle: String, updatedAlts: List<String> ->
            if (!didFinish) {
                didFinish = true
                onSaveUrl(url)
                onSaveInfo(description, tags, author, artist, status)
                swap(newTitle, updatedAlts)
                onDismissRequest()
            }
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
                        Icon(Icons.Outlined.Close, stringResource(MR.strings.action_close), tint = tokens.dotOn)
                    }
                    Text(
                        text = tsuzukiCase(stringResource(MR.strings.tsz_edit_info)).orEmpty(),
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
                            text = tsuzukiCase(stringResource(MR.strings.action_save)).orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TsuzukiEditCoverTile(manga)
                    EditInfoTile(stringResource(MR.strings.title), title, { title = it })
                    EditInfoTile(stringResource(MR.strings.author), author, { author = it })
                    EditInfoTile(stringResource(MR.strings.artist), artist, { artist = it })
                    EditInfoTile(
                        stringResource(TDMR.strings.edit_label_description),
                        description,
                        { description = it },
                        minLines = 3,
                        maxLines = 8,
                    )
                    EditStatusTile(status = status, onStatusChange = { status = it })
                    EditTagsTile(tags = tags, onTagsChange = { tags = it })
                    EditInfoTile(
                        stringResource(TDMR.strings.js_plugin_repo_url),
                        url,
                        { url = it },
                        maxLines = 3,
                        keyboardType = KeyboardType.Uri,
                    )
                    TsuzukiTile(Modifier.fillMaxWidth(), padding = 14.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditSectionLabel(stringResource(TDMR.strings.action_edit_alt_titles))
                            alternativeTitles(title, altTitles, { altTitles = it }, swapAndClose)
                        }
                    }
                    sourceValues()
                }
            }
        }
    }
}

@Composable
private fun EditInfoTile(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    TsuzukiTile(Modifier.fillMaxWidth(), padding = 0.dp) {
        EditTileField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            maxLines = maxLines,
            keyboardType = keyboardType,
        )
    }
}

@Composable
private fun EditTileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val tokens = TsuzukiTheme.tokens
    val clear = tokens.tile.copy(alpha = 0f)
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(tsuzukiCase(label).orEmpty(), style = MaterialTheme.typography.labelMedium) },
        textStyle = MaterialTheme.typography.bodyLarge,
        minLines = minLines,
        maxLines = maxLines,
        singleLine = maxLines == 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedTextColor = tokens.dotOn,
            unfocusedTextColor = tokens.dotOn,
            focusedContainerColor = clear,
            unfocusedContainerColor = clear,
            cursorColor = tokens.accent,
            focusedIndicatorColor = tokens.accent,
            unfocusedIndicatorColor = clear,
            focusedLabelColor = tokens.accent,
            unfocusedLabelColor = tokens.dotOn.copy(alpha = 0.7f),
        ),
    )
}

@Composable
private fun EditStatusTile(status: Long, onStatusChange: (Long) -> Unit) {
    val options = listOf(
        SManga.UNKNOWN to MR.strings.unknown,
        SManga.ONGOING to MR.strings.ongoing,
        SManga.COMPLETED to MR.strings.completed,
        SManga.LICENSED to MR.strings.licensed,
        SManga.PUBLISHING_FINISHED to MR.strings.publishing_finished,
        SManga.CANCELLED to MR.strings.cancelled,
        SManga.ON_HIATUS to MR.strings.on_hiatus,
    )
    TsuzukiTile(Modifier.fillMaxWidth(), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EditSectionLabel(stringResource(MR.strings.status))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                options.forEach { (value, label) ->
                    val isSelected = value.toLong() == status
                    TsuzukiPill(
                        text = stringResource(label),
                        selected = isSelected,
                        onClick = { onStatusChange(value.toLong()) },
                        modifier = Modifier.semantics { selected = isSelected },
                        role = Role.RadioButton,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditTagsTile(tags: List<String>, onTagsChange: (List<String>) -> Unit) {
    val tokens = TsuzukiTheme.tokens
    var newTag by remember { mutableStateOf("") }
    val addLabel = stringResource(MR.strings.action_add)
    val removeLabel = stringResource(MR.strings.action_remove)
    TsuzukiTile(Modifier.fillMaxWidth(), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EditTileField(
                    label = stringResource(TDMR.strings.edit_label_tags),
                    value = newTag,
                    onValueChange = { newTag = it },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    enabled = newTag.isNotBlank(),
                    onClick = {
                        onTagsChange(tags + newTag.trim())
                        newTag = ""
                    },
                ) {
                    Icon(Icons.Outlined.Add, addLabel, tint = if (newTag.isNotBlank()) tokens.accent else tokens.dotOff)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tags.forEach { tag ->
                    Row(
                        Modifier.clip(CircleShape).background(tokens.track)
                            .tsuzukiClickable(role = Role.Button, onClickLabel = removeLabel) {
                                onTagsChange(tags - tag)
                            }
                            .heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tsuzukiCase(tag).orEmpty(),
                            modifier = Modifier.weight(1f, fill = false),
                            color = tokens.dotOn,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = null,
                            Modifier.size(16.dp),
                            tint = tokens.accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSectionLabel(label: String) {
    TsuzukiSectionLabel(label, contentPadding = PaddingValues(0.dp))
}
