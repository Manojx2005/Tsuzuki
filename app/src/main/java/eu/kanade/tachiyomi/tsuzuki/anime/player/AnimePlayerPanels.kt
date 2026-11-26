package eu.kanade.tachiyomi.tsuzuki.anime.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.animeEpisodeNumberOrder
import eu.kanade.tachiyomi.tsuzuki.anime.preview.AnimeEpisodeThumbnail
import tachiyomi.domain.items.episode.model.Episode
import kotlin.math.ceil

/** Board "Dot · Episodes panel": thumbnails with progress, the playing one outlined in the accent. */
@Composable
internal fun AnimeEpisodesDrawer(state: AnimePlayerState, model: AnimePlayerModel, onDismiss: () -> Unit) {
    val episodes = remember(state.episodes) { state.episodes.sortedWith(animeEpisodeNumberOrder) }
    val currentIndex = episodes.indexOfFirst { it.id == state.episode?.id }.coerceAtLeast(0)
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0))
    AnimePlayerDrawer(title = stringResource(R.string.tsz_anime_player_episodes), onDismiss = onDismiss) {
        if (episodes.isEmpty()) AnimePlayerEmptyOptions()
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            state = list,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(episodes, key = Episode::id) { episode ->
                AnimeEpisodeRow(
                    episode = episode,
                    state = state,
                    current = episode.id == state.episode?.id,
                ) {
                    model.selectEpisode(episode.id)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun AnimeEpisodeRow(episode: Episode, state: AnimePlayerState, current: Boolean, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(if (tokens.isDot) tokens.tileRadius else 16.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { selected = current },
        shape = shape,
        color = if (current) tokens.accent.copy(alpha = 0.08f) else Color.Transparent,
        contentColor = tokens.dotOn,
        border = BorderStroke(1.dp, if (current) tokens.accent else Color.Transparent),
    ) {
        Row(
            Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sourceId = state.sourceId
            if (sourceId != null) {
                AnimeEpisodeThumbnail(
                    sourceId = sourceId,
                    episode = episode,
                    coverUrl = state.coverUrl,
                    modifier = Modifier.width(112.dp).height(63.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    tsuzukiMenuLabel(animeEpisodeLabel(episode)),
                    color = if (episode.seen && !current) tokens.mutedText else tokens.dotOn,
                    style = animeActionStyle(bold = true),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = if (current) {
                    val left = (state.duration - state.position).takeIf { state.duration > 0 && it > 0 }
                    val playing = stringResource(R.string.tsz_anime_player_now_playing)
                    left?.let {
                        "$playing · " + stringResource(R.string.tsz_anime_player_minutes_left, ceil(it / 60).toInt())
                    } ?: playing
                } else {
                    animeEpisodeMeta(episode.id, episode.totalSeconds, watched = episode.seen)
                }
                meta?.let {
                    Text(
                        tsuzukiMenuLabel(it),
                        color = tokens.mutedText,
                        style = animeCaptionStyle().copy(fontSize = 10.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Board "Dot · Audio & subtitles": audio, subtitles and the subtitle look side by side over the dimmed video.
 * The look column keeps the in-player subtitle delay, which the board doesn't show.
 */
@Composable
internal fun AnimeAudioSubsPanel(state: AnimePlayerState, model: AnimePlayerModel, onDismiss: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val settings = state.settings
    val audio = state.tracks.filter { it.kind == AnimeTrackKind.AUDIO }
    val subtitles = state.tracks.filter { it.kind == AnimeTrackKind.SUBTITLE }
    Box(
        Modifier.fillMaxSize().background(tokens.background.copy(alpha = 0.82f))
            .tsuzukiClickable(role = Role.Button, onClick = {}),
    ) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tsuzukiMenuLabel(stringResource(R.string.tsz_anime_player_audio_subtitles)),
                    modifier = Modifier.weight(1f),
                    color = tokens.dotOn,
                    style = animePanelTitleStyle(),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        stringResource(R.string.tsz_anime_player_close_options),
                        modifier = Modifier.size(20.dp),
                        tint = tokens.dotOn,
                    )
                }
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                AnimeTrackColumn(stringResource(R.string.tsz_anime_player_audio), Modifier.weight(1f)) {
                    if (audio.isEmpty()) AnimePlayerEmptyOptions()
                    audio.forEach { track ->
                        AnimePlayerOption(track.displayLabel(), detail = track.codec, selected = track.selected) {
                            model.setTrack(AnimeTrackKind.AUDIO, track)
                        }
                    }
                }
                AnimeTrackColumn(stringResource(R.string.tsz_anime_player_subtitles), Modifier.weight(1f)) {
                    AnimePlayerOption(
                        stringResource(R.string.tsz_anime_player_track_off),
                        selected = subtitles.none(AnimePlayerTrack::selected),
                    ) { model.setTrack(AnimeTrackKind.SUBTITLE, null) }
                    subtitles.forEach { track ->
                        AnimePlayerOption(track.displayLabel(), detail = track.codec, selected = track.selected) {
                            model.setTrack(AnimeTrackKind.SUBTITLE, track)
                        }
                    }
                }
                AnimeTrackColumn(stringResource(R.string.tsz_anime_player_subtitle_look), Modifier.weight(1f)) {
                    val scale = settings.subtitleScale
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        subtitleSizes.forEach { (preset, label) ->
                            AnimeLookChip(
                                stringResource(label),
                                selected = kotlin.math.abs(scale - preset) < 0.01f,
                                modifier = Modifier.weight(1f),
                            ) { model.setSettings(settings.copy(subtitleScale = preset)) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        val style = settings.subtitleStyle
                        AnimeLookChip(
                            stringResource(R.string.tsz_anime_player_sub_border),
                            selected = !style.background,
                            modifier = Modifier.weight(1f),
                        ) { model.setSettings(settings.copy(subtitleStyle = style.copy(background = false))) }
                        AnimeLookChip(
                            stringResource(R.string.tsz_anime_player_sub_box),
                            selected = style.background,
                            modifier = Modifier.weight(1f),
                        ) { model.setSettings(settings.copy(subtitleStyle = style.copy(background = true))) }
                    }
                    AnimePlayerStepper(
                        label = stringResource(R.string.tsz_anime_player_subtitle_delay),
                        value = formatAnimeSeconds(settings.subtitleDelay),
                        decrease = {
                            model.setSettings(settings.copy(subtitleDelay = settings.subtitleDelay - 0.25f))
                        },
                        increase = {
                            model.setSettings(settings.copy(subtitleDelay = settings.subtitleDelay + 0.25f))
                        },
                    )
                    AnimeSubtitleSample(scale, settings.subtitleStyle)
                }
            }
        }
    }
}

@Composable
private fun AnimeTrackColumn(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Column(
        modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            tsuzukiMenuLabel(title),
            modifier = Modifier.padding(bottom = 6.dp),
            color = tokens.mutedText,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 2.sp),
        )
        content()
    }
}

@Composable
private fun AnimeLookChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val dot = tokens.isDot
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        shape = animeControlShape(),
        color = when {
            dot -> Color.Transparent
            selected -> tokens.accent
            else -> tokens.tile
        },
        contentColor = if (!dot && selected) tokens.onAccent else tokens.dotOn,
        border = if (dot) {
            BorderStroke(
                1.dp,
                if (selected) tokens.accent else tokens.tileBorder ?: tokens.track,
            )
        } else {
            null
        },
    ) {
        Box(Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
            Text(
                tsuzukiMenuLabel(label),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    letterSpacing = if (dot) 1.sp else 0.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                ),
                maxLines = 1,
            )
        }
    }
}

/** A sample line in the chosen size and outline or box, as mpv will draw plain subtitles. */
@Composable
private fun AnimeSubtitleSample(scale: Float, style: AnimeSubtitleStyle) {
    val tokens = TsuzukiTheme.tokens
    val box = style.background
    val ink = remember(style.color) { Color(android.graphics.Color.parseColor(style.color.mpv)) }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 96.dp)
            .background(
                tokens.dotOn.copy(alpha = 0.08f),
                RoundedCornerShape(if (tokens.isDot) tokens.tileRadius else 16.dp),
            )
            .padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            stringResource(R.string.tsz_anime_player_sub_sample),
            modifier = if (box) {
                Modifier.background(
                    Color.Black.copy(alpha = 0.6f),
                ).padding(horizontal = 6.dp, vertical = 2.dp)
            } else {
                Modifier
            },
            color = ink,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (15 * scale).sp,
                fontWeight = FontWeight.Bold,
                shadow = if (box) null else Shadow(Color.Black, blurRadius = 6f),
            ),
            maxLines = 2,
        )
    }
}

/** S / M / L presets of the in-player subtitle size; M is the default scale. */
private val subtitleSizes = listOf(
    0.8f to R.string.tsz_anime_player_sub_size_small,
    1f to R.string.tsz_anime_player_sub_size_medium,
    1.2f to R.string.tsz_anime_player_sub_size_large,
)
