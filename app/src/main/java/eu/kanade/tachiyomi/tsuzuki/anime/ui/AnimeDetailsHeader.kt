package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Upcoming
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.presentation.manga.components.DotSeparatorText
import eu.kanade.presentation.manga.components.MangaActionButton
import eu.kanade.presentation.manga.components.MangaSummary
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiSharedCover
import eu.kanade.presentation.tsuzuki.anime.animeDetailsCover
import eu.kanade.presentation.tsuzuki.details.TsuzukiDetailsDescription
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/*
 * AP7: the anime details header is the book details header (MangaInfoHeader.kt: MangaInfoBox, MangaActionRow,
 * ExpandableMangaDescription). The cover/title block repeats MangaInfoBox's measurements because that one takes a
 * book; the action tiles and the description are the book's own composables.
 */

/** Cover, title and author/status/source lines; the book's MangaInfoBox (small on phones, large in the tablet pane). */
@Composable
internal fun AnimeDetailsHeader(
    source: AnimeSource?,
    anime: SAnime,
    isTabletUi: Boolean = false,
    appBarPadding: Dp = 0.dp,
    onCoverClick: (() -> Unit)? = null,
    sharedCoverKey: String? = null,
) {
    Box {
        // Backdrop, as MangaInfoBox draws it.
        val backdropGradientColors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
        AsyncImage(
            model = rememberAnimeImage(source, anime.thumbnail_url),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    drawRect(brush = Brush.verticalGradient(colors = backdropGradientColors))
                }
                .blur(4.dp)
                .alpha(0.2f),
        )
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            val coverLabel = stringResource(MR.strings.manga_cover)
            val cover = Modifier.animeDetailsCover(anime.url)
                .then(if (sharedCoverKey != null) Modifier.tsuzukiSharedCover(sharedCoverKey) else Modifier)
            if (isTabletUi) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimeCover(source, anime, Modifier.fillMaxWidth(0.65f).then(cover), onCoverClick, coverLabel)
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimeContentInfo(anime, source?.name, TextAlign.Center)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimeCover(
                        source,
                        anime,
                        Modifier.sizeIn(maxWidth = 100.dp).align(Alignment.Top).then(cover),
                        onCoverClick,
                        coverLabel,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        AnimeContentInfo(anime, source?.name, null)
                    }
                }
            }
        }
    }
}

/** MangaContentInfo for an anime: no alternative titles or shelves row, and no tap-to-search yet. */
@Composable
private fun ColumnScope.AnimeContentInfo(anime: SAnime, sourceName: String?, textAlign: TextAlign?) {
    val context = LocalContext.current
    val tokens = TsuzukiTheme.tokens
    val title = anime.title
    val shownTitle = title.ifBlank { stringResource(MR.strings.unknown_title) }
    Text(
        text = if (tokens.isDot) shownTitle.uppercase() else shownTitle,
        style = if (tokens.style != TsuzukiStyle.NONE) {
            MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, lineHeight = 34.sp)
        } else {
            MaterialTheme.typography.titleLarge
        },
        modifier = Modifier.clickableNoIndication(
            onLongClick = { if (title.isNotBlank()) context.copyToClipboard(title, title) },
            onClick = {},
        ),
        textAlign = textAlign,
    )

    // The book's (empty) alternative-titles row, kept so the gaps under the title match it in both layouts.
    Row(modifier = Modifier.fillMaxWidth()) {}

    Spacer(modifier = Modifier.height(2.dp))

    val author = anime.author?.trim()
    Row(
        modifier = Modifier.secondaryItemAlpha(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.PersonOutline, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(
            text = author?.takeIf { it.isNotBlank() } ?: stringResource(MR.strings.unknown_author),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickableNoIndication(
                onLongClick = { if (!author.isNullOrBlank()) context.copyToClipboard(author, author) },
                onClick = {},
            ),
            textAlign = textAlign,
        )
    }

    val artist = anime.artist?.trim()
    if (!artist.isNullOrBlank() && author != artist) {
        Row(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clickableNoIndication(
                    onLongClick = { context.copyToClipboard(artist, artist) },
                    onClick = {},
                ),
                textAlign = textAlign,
            )
        }
    }

    Spacer(modifier = Modifier.height(2.dp))

    Row(
        modifier = Modifier.secondaryItemAlpha(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (anime.status) {
                SAnime.ONGOING -> Icons.Outlined.Schedule
                SAnime.COMPLETED -> Icons.Outlined.DoneAll
                SAnime.LICENSED -> Icons.Outlined.AttachMoney
                SAnime.PUBLISHING_FINISHED -> Icons.Outlined.Done
                SAnime.CANCELLED -> Icons.Outlined.Close
                SAnime.ON_HIATUS -> Icons.Outlined.Pause
                SAnime.UPCOMING -> Icons.Outlined.Upcoming
                else -> Icons.Outlined.Block
            },
            contentDescription = null,
            modifier = Modifier.padding(end = 4.dp).size(16.dp),
        )
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Text(
                text = when (anime.status) {
                    SAnime.ONGOING -> stringResource(MR.strings.ongoing)
                    SAnime.COMPLETED -> stringResource(MR.strings.completed)
                    SAnime.LICENSED -> stringResource(MR.strings.licensed)
                    SAnime.PUBLISHING_FINISHED -> stringResource(MR.strings.publishing_finished)
                    SAnime.CANCELLED -> stringResource(MR.strings.cancelled)
                    SAnime.ON_HIATUS -> stringResource(MR.strings.on_hiatus)
                    SAnime.UPCOMING -> androidx.compose.ui.res.stringResource(R.string.tsz_anime_upcoming)
                    else -> stringResource(MR.strings.unknown)
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            if (!sourceName.isNullOrBlank()) {
                DotSeparatorText()
                Text(
                    text = sourceName,
                    modifier = Modifier.clickableNoIndication(
                        onLongClick = { context.copyToClipboard(sourceName, sourceName) },
                        onClick = {},
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The book's action row (MangaActionRow): In library, next airing (like the book's next-update tile), Tracking,
 * Save and WebView, with the book's tiles, sizes and paddings.
 */
@Composable
internal fun AnimeLibraryActions(
    favorite: Boolean,
    enabled: Boolean,
    onFavorite: () -> Unit,
    onTrack: (() -> Unit)?,
    onSave: (() -> Unit)?,
    onExternal: (() -> Unit)?,
    nextAiringDays: Int? = null,
    trackingCount: Int = 0,
) {
    val tokens = TsuzukiTheme.tokens
    val defaultColor = tokens.dotOn
    val activeColor = tokens.accent
    fun (() -> Unit).whenEnabled(): () -> Unit = { if (enabled) this() }
    Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
        MangaActionButton(
            title = stringResource(if (favorite) MR.strings.in_library else MR.strings.add_to_library),
            icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            color = if (favorite) activeColor else defaultColor,
            onClick = onFavorite.whenEnabled(),
        )
        if (nextAiringDays != null) {
            MangaActionButton(
                title = if (nextAiringDays == 0) {
                    stringResource(MR.strings.manga_interval_expected_update_soon)
                } else {
                    pluralStringResource(MR.plurals.day, nextAiringDays, nextAiringDays)
                },
                icon = Icons.Default.HourglassEmpty,
                color = defaultColor,
                onClick = {},
            )
        }
        if (onTrack != null) {
            MangaActionButton(
                title = if (trackingCount == 0) {
                    stringResource(MR.strings.manga_tracking_tab)
                } else {
                    pluralStringResource(MR.plurals.num_trackers, count = trackingCount, trackingCount)
                },
                icon = if (trackingCount == 0) Icons.Outlined.Sync else Icons.Outlined.Done,
                color = if (trackingCount == 0) defaultColor else activeColor,
                onClick = onTrack.whenEnabled(),
            )
        }
        if (onSave != null) {
            MangaActionButton(
                title = stringResource(MR.strings.action_save),
                icon = Icons.Outlined.Download,
                color = defaultColor,
                onClick = onSave.whenEnabled(),
            )
        }
        if (onExternal != null) {
            MangaActionButton(
                title = stringResource(MR.strings.action_web_view),
                icon = Icons.Outlined.Public,
                color = defaultColor,
                onClick = onExternal.whenEnabled(),
            )
        }
    }
}

/**
 * The book's description block (ExpandableMangaDescription in the Tsuzuki themes): summary with notes, the expand
 * arrow and two rows of genre pills. Genres copy to the clipboard; there is no anime tag search yet.
 */
@Composable
internal fun AnimeDetailsDescription(
    anime: SAnime,
    notes: String,
    defaultExpanded: Boolean,
    onEditNotes: () -> Unit,
) {
    val context = LocalContext.current
    val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
    val sortTags by libraryPreferences.sortMangaTags.changes().collectAsState(initial = false)
    val genres = anime.getGenres().orEmpty()
    val tags = if (sortTags) genres.sortedBy { it.lowercase() } else genres
    val description = anime.description
    TsuzukiDetailsDescription(
        defaultExpanded = defaultExpanded,
        hasSummary = !description.isNullOrBlank() || notes.isNotBlank(),
        tags = tags,
        onTagSearch = null,
        onCopyTag = { context.copyToClipboard(it, it) },
    ) { expanded ->
        MangaSummary(
            description = description.orEmpty(),
            expanded = expanded,
            notes = notes,
            onEditNotesClicked = onEditNotes,
            separateExpandControl = true,
        )
    }
}
