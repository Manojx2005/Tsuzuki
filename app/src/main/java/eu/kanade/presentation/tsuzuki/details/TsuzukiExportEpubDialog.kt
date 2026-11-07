package eu.kanade.presentation.tsuzuki.details

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import eu.kanade.presentation.components.imeAwareDialogProperties
import eu.kanade.presentation.library.components.EpubExportOptions
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.util.epub.EpubExportNaming
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.translation.model.TranslationMode
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiExportEpubDialog(
    manga: Manga,
    chapters: List<Chapter>,
    onDismissRequest: () -> Unit,
    onExport: (Uri, EpubExportOptions) -> Unit,
    documentName: (String, EpubExportOptions, Boolean) -> String,
) {
    TsuzukiEpubExportForm(
        initialFilename = EpubExportNaming.sanitizeFilename(manga.title) + ".epub",
        initialOptions = EpubExportOptions(downloadedOnly = true),
        selectionLabel = stringResource(TDMR.strings.epub_chapters_export_count, chapters.size),
        forceZip = false,
        onDismissRequest = onDismissRequest,
        onExport = onExport,
        documentName = documentName,
    )
}

@Composable
internal fun TsuzukiBatchExportEpubDialog(
    mangaList: List<Manga>,
    onDismissRequest: () -> Unit,
    onExport: (Uri, EpubExportOptions) -> Unit,
) {
    val single = mangaList.singleOrNull()
    val initialFilename = single?.title?.let { EpubExportNaming.sanitizeFilename(it) + ".epub" } ?: "novels_export.zip"
    TsuzukiEpubExportForm(
        initialFilename = initialFilename,
        initialOptions = EpubExportOptions(),
        selectionLabel = stringResource(TDMR.strings.epub_novels_export_count, mangaList.size),
        forceZip = single == null,
        onDismissRequest = onDismissRequest,
        onExport = onExport,
        documentName = { filename, _, asZip -> batchEpubDocumentName(filename, initialFilename, asZip) },
        selectionContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                mangaList.take(5).forEach { manga -> ExportSupportingText(manga.title) }
                if (mangaList.size > 5) {
                    ExportSupportingText(stringResource(TDMR.strings.epub_and_more, mangaList.size - 5))
                }
            }
        },
    )
}

internal fun batchEpubDocumentName(input: String, fallback: String, asZip: Boolean): String {
    fun stem(name: String): String = when {
        name.endsWith(".epub", ignoreCase = true) -> name.dropLast(5)
        name.endsWith(".zip", ignoreCase = true) -> name.dropLast(4)
        else -> name
    }
    val base = EpubExportNaming.sanitizeFilename(stem(input.trim())).ifBlank { stem(fallback) }
    return base + if (asZip) ".zip" else ".epub"
}

@Composable
private fun TsuzukiEpubExportForm(
    initialFilename: String,
    initialOptions: EpubExportOptions,
    selectionLabel: String,
    forceZip: Boolean,
    onDismissRequest: () -> Unit,
    onExport: (Uri, EpubExportOptions) -> Unit,
    documentName: (String, EpubExportOptions, Boolean) -> String,
    selectionContent: @Composable () -> Unit = {},
) {
    val tokens = TsuzukiTheme.tokens
    val exportLabel = stringResource(TDMR.strings.action_export_epub)
    var filename by remember(initialFilename) { mutableStateOf(initialFilename) }
    var options by remember(initialOptions) { mutableStateOf(initialOptions) }
    val asZip = forceZip || epubExportUsesZip(options)
    val preview = documentName(filename, options, asZip)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(if (asZip) "application/zip" else "application/epub+zip"),
    ) { uri ->
        if (uri != null) {
            onExport(uri, options)
            onDismissRequest()
        }
    }

    Dialog(onDismissRequest = onDismissRequest, properties = imeAwareDialogProperties) {
        Box(
            Modifier.fillMaxSize().tsuzukiBackground(tokens).safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxSize().padding(horizontal = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.background(tokens.tile, CircleShape).then(
                            tokens.tileBorder?.let { Modifier.border(1.dp, it, CircleShape) } ?: Modifier,
                        ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(MR.strings.action_cancel),
                            tint = tokens.dotOn,
                        )
                    }
                    Column {
                        Text(
                            text = tsuzukiCase(exportLabel).orEmpty(),
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.headlineLarge,
                            color = tokens.dotOn,
                        )
                        Text(
                            text = tsuzukiCase(selectionLabel).orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.dotOn.copy(alpha = 0.72f),
                        )
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    selectionContent()
                    ExportEpubContentOptions(options) { options = it }
                    ExportEpubFilenameOptions(filename, { filename = it }, preview, options) { options = it }
                    ExportEpubSnippetOptions(options) { options = it }
                }

                Button(
                    onClick = { launcher.launch(preview) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.accent,
                        contentColor = tokens.onAccent,
                    ),
                ) {
                    Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(tsuzukiCase(exportLabel).orEmpty(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ExportEpubContentOptions(options: EpubExportOptions, onOptionsChange: (EpubExportOptions) -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val downloadedInfo = stringResource(
        if (options.downloadedOnly) TDMR.strings.epub_downloaded_only_info else TDMR.strings.epub_source_info,
    )
    val modes = listOf(TranslationMode.ORIGINAL, TranslationMode.TRANSLATED, TranslationMode.BOTH)
    TsuzukiTile(Modifier.fillMaxWidth(), padding = 0.dp) {
        ExportSwitch(stringResource(TDMR.strings.epub_downloaded_only), options.downloadedOnly, downloadedInfo) {
            onOptionsChange(options.copy(downloadedOnly = it))
        }
        HorizontalDivider(color = tokens.track)
        ExportSwitch(stringResource(TDMR.strings.epub_join_volumes), options.joinVolumes) {
            onOptionsChange(options.copy(joinVolumes = it))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExportSectionLabel(stringResource(TDMR.strings.epub_translation_mode))
        TsuzukiSegments(
            labels = listOf(
                stringResource(TDMR.strings.epub_translation_original),
                stringResource(TDMR.strings.epub_translation_translated),
                stringResource(TDMR.strings.novel_chapter_display_both),
            ),
            selected = modes.indexOf(options.translationMode),
            onClick = { onOptionsChange(options.copy(translationMode = modes[it])) },
            modifier = Modifier,
        )
        when (options.translationMode) {
            TranslationMode.ORIGINAL -> Unit
            TranslationMode.TRANSLATED -> ExportSupportingText(
                stringResource(TDMR.strings.epub_translation_translated_info),
            )
            TranslationMode.BOTH -> ExportSupportingText(stringResource(TDMR.strings.epub_translation_both_info))
        }
    }
}

@Composable
private fun ExportEpubFilenameOptions(
    filename: String,
    onFilenameChange: (String) -> Unit,
    preview: String,
    options: EpubExportOptions,
    onOptionsChange: (EpubExportOptions) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExportSectionLabel(stringResource(TDMR.strings.epub_filename_options))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilenameOption(stringResource(TDMR.strings.epub_include_volume_number), options.includeVolumeNumber) {
                onOptionsChange(options.copy(includeVolumeNumber = !options.includeVolumeNumber))
            }
            FilenameOption(
                stringResource(TDMR.strings.epub_include_chapter_range),
                options.includeChapterRange,
            ) {
                onOptionsChange(options.copy(includeChapterRange = !options.includeChapterRange))
            }
            FilenameOption(stringResource(TDMR.strings.epub_include_chapter_count), options.includeChapterCount) {
                onOptionsChange(options.copy(includeChapterCount = !options.includeChapterCount))
            }
            FilenameOption(stringResource(TDMR.strings.epub_include_status), options.includeStatus) {
                onOptionsChange(options.copy(includeStatus = !options.includeStatus))
            }
        }
        OutlinedTextField(
            value = filename,
            onValueChange = onFilenameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tsuzukiCase(stringResource(TDMR.strings.epub_filename)).orEmpty()) },
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = tokens.dotOn,
                unfocusedTextColor = tokens.dotOn,
                focusedContainerColor = tokens.tile,
                unfocusedContainerColor = tokens.tile,
                cursorColor = tokens.accent,
                focusedBorderColor = tokens.accent,
                unfocusedBorderColor = tokens.tileBorder ?: tokens.track,
                focusedLabelColor = tokens.accent,
                unfocusedLabelColor = tokens.dotOn.copy(alpha = 0.72f),
            ),
        )
        if (preview != filename) {
            TsuzukiTile(Modifier.fillMaxWidth(), padding = 12.dp) {
                Text(preview, style = MaterialTheme.typography.bodyMedium, color = tokens.dotOn)
            }
        }
    }
}

@Composable
private fun ExportEpubSnippetOptions(options: EpubExportOptions, onOptionsChange: (EpubExportOptions) -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExportSectionLabel(stringResource(TDMR.strings.epub_include_custom_assets_section))
        TsuzukiTile(Modifier.fillMaxWidth(), padding = 0.dp) {
            ExportSwitch(stringResource(TDMR.strings.epub_include_custom_css), options.includeCustomCss) {
                onOptionsChange(options.copy(includeCustomCss = it))
            }
            HorizontalDivider(color = tokens.track)
            ExportSwitch(stringResource(TDMR.strings.epub_include_custom_js), options.includeCustomJs) {
                onOptionsChange(options.copy(includeCustomJs = it))
            }
        }
        if (options.includeCustomCss || options.includeCustomJs) {
            ExportSupportingText(stringResource(TDMR.strings.epub_include_custom_assets_info))
        }
    }
}

@Composable
private fun ExportSectionLabel(label: String) {
    TsuzukiSectionLabel(label, contentPadding = PaddingValues(start = 4.dp))
}

@Composable
private fun ExportSupportingText(text: String) {
    Text(
        text = tsuzukiCase(text).orEmpty(),
        style = MaterialTheme.typography.bodySmall,
        color = TsuzukiTheme.tokens.dotOn.copy(alpha = 0.72f),
    )
}

@Composable
private fun FilenameOption(label: String, selected: Boolean, onClick: () -> Unit) {
    TsuzukiPill(
        text = label,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.semantics { toggleableState = ToggleableState(selected) },
        role = Role.Checkbox,
    )
}

@Composable
private fun ExportSwitch(
    label: String,
    checked: Boolean,
    subtitle: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = tsuzukiCase(label).orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = tokens.dotOn,
            )
            subtitle?.let { ExportSupportingText(it) }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = tokens.accent,
                checkedThumbColor = tokens.onAccent,
                checkedBorderColor = tokens.accent,
                uncheckedTrackColor = tokens.tile,
                uncheckedThumbColor = tokens.dotOn.copy(alpha = 0.65f),
                uncheckedBorderColor = tokens.track,
            ),
        )
    }
}

internal fun epubExportUsesZip(options: EpubExportOptions): Boolean =
    options.translationMode == TranslationMode.BOTH || !options.joinVolumes
