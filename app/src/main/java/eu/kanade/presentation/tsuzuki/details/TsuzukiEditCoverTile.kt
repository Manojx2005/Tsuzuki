package eu.kanade.presentation.tsuzuki.details

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.ui.manga.MangaCoverViewModel
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal

@Composable
internal fun TsuzukiEditCoverTile(manga: Manga) {
    val tokens = TsuzukiTheme.tokens
    val context = LocalContext.current
    val model = viewModel<MangaCoverViewModel>(
        key = "tsuzuki-edit-cover-${manga.id}",
        factory = MangaCoverViewModel.Factory,
        extras = CreationExtras {
            set(MangaCoverViewModel.MANGA_ID_KEY, manga.id)
        },
    )
    val currentManga by model.state.collectAsStateWithLifecycle()
    val liveCover = currentManga
    val hasCustomCover by produceState(false, liveCover?.id, liveCover?.coverLastModified) {
        value = liveCover != null && !liveCover.isLocal() && withIOContext { liveCover.hasCustomCover() }
    }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) model.editCover(context, uri)
    }

    TsuzukiTile(Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaCover.Book(
                data = liveCover ?: manga,
                modifier = Modifier.width(92.dp),
                contentDescription = stringResource(MR.strings.manga_cover),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = { pickCover.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    enabled = liveCover?.let { it.favorite || it.isLocal() } == true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.track,
                        contentColor = tokens.dotOn,
                        disabledContainerColor = tokens.tile,
                        disabledContentColor = tokens.dotOn.copy(alpha = 0.38f),
                    ),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            tsuzukiCase(stringResource(MR.strings.action_edit_cover)).orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                TextButton(
                    onClick = { model.deleteCustomCover(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasCustomCover,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = tokens.dotOn.copy(alpha = 0.72f),
                        disabledContentColor = tokens.dotOn.copy(alpha = 0.38f),
                    ),
                ) {
                    Text(
                        tsuzukiCase(stringResource(MR.strings.tsz_edit_reset_cover)).orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        SnackbarHost(model.snackbarHostState)
    }
}
