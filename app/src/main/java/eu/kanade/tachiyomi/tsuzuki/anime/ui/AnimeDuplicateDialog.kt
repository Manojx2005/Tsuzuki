package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.tsuzuki.browse.rememberAnimeBrowseSources
import eu.kanade.presentation.tsuzuki.browse.rememberAnimeSourceStubs
import eu.kanade.presentation.tsuzuki.details.TsuzukiDuplicateDialog
import eu.kanade.presentation.tsuzuki.migration.TsuzukiMigrationPreviewTile
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun AnimeDuplicateDialog(
    existing: Anime,
    adding: Anime,
    addingSource: AnimeSource,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
    onMigrate: () -> Unit,
) {
    val sources by rememberAnimeBrowseSources()
    val stubs = rememberAnimeSourceStubs(sources)
    val existingSource = sources.firstOrNull { it.source.id == existing.source }?.source
    val existingName = existingSource?.name ?: stubs[existing.source]?.name ?: existing.source.toString()
    TsuzukiDuplicateDialog(
        onDismissRequest = onDismiss,
        onConfirm = onAdd,
        onOpen = onOpen,
        onMigrate = onMigrate,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TsuzukiMigrationPreviewTile(
                label = stringResource(MR.strings.in_library),
                title = existing.title,
                subtitle = existingName,
                highlighted = false,
                modifier = Modifier.weight(1f),
                cover = { AnimeCover(existingSource, existing.toSourceAnime(), Modifier.fillMaxWidth()) },
            )
            TsuzukiMigrationPreviewTile(
                label = stringResource(MR.strings.add_to_library),
                title = adding.title,
                subtitle = addingSource.name,
                highlighted = true,
                modifier = Modifier.weight(1f),
                cover = { AnimeCover(addingSource, adding.toSourceAnime(), Modifier.fillMaxWidth()) },
            )
        }
    }
}
