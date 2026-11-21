package eu.kanade.tachiyomi.tsuzuki.anime.player

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.animeEpisodeNumberOrder
import java.text.NumberFormat
import java.util.Locale

internal enum class AnimePlayerSheet(@StringRes val title: Int) {
    SUBTITLES(R.string.tsz_anime_player_subtitles),
    AUDIO(R.string.tsz_anime_player_audio),
    QUALITY(R.string.tsz_anime_player_quality),
    SPEED(R.string.tsz_anime_player_speed),
    EPISODES(R.string.tsz_anime_player_episodes),
    VIDEO(R.string.tsz_anime_player_video),
}

internal val animePlaybackSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

@Composable
internal fun AnimePlayerSheets(
    active: AnimePlayerSheet,
    state: AnimePlayerState,
    model: AnimePlayerModel,
    onPick: (AnimePlayerSheet) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val settings = state.settings
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = maxWidth > 540.dp
        val panelWidth = if (isWide) (maxWidth * 0.52f).coerceAtMost(520.dp) else maxWidth
        Row(Modifier.fillMaxSize()) {
            if (isWide) {
                Box(
                    Modifier.weight(1f).fillMaxHeight().background(tokens.background.copy(alpha = 0.50f))
                        .tsuzukiClickable(role = Role.Button, onClick = onDismiss),
                )
            }
            Surface(
                modifier = Modifier.width(panelWidth).fillMaxHeight(),
                shape = if (isWide) RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp) else RectangleShape,
                color = tokens.background,
                contentColor = tokens.dotOn,
            ) {
                Column(
                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        TsuzukiPillRow(
                            labels = AnimePlayerSheet.entries.map { stringResource(it.title) },
                            selected = active.ordinal,
                            onClick = { onPick(AnimePlayerSheet.entries[it]) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, stringResource(R.string.tsz_anime_player_close_options))
                        }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (active) {
                            AnimePlayerSheet.SUBTITLES, AnimePlayerSheet.AUDIO -> {
                                val kind = if (active == AnimePlayerSheet.SUBTITLES) {
                                    AnimeTrackKind.SUBTITLE
                                } else {
                                    AnimeTrackKind.AUDIO
                                }
                                val tracks = state.tracks.filter { it.kind == kind }
                                items(tracks, key = AnimePlayerTrack::id) { track ->
                                    AnimePlayerOption(
                                        label = track.displayLabel(),
                                        detail = track.codec,
                                        selected = track.selected,
                                        onClick = { model.setTrack(kind, track) },
                                    )
                                }
                                if (kind == AnimeTrackKind.SUBTITLE) {
                                    item {
                                        AnimePlayerOption(
                                            stringResource(R.string.tsz_anime_player_track_off),
                                            selected = tracks.none(AnimePlayerTrack::selected),
                                        ) { model.setTrack(kind, null) }
                                    }
                                    item {
                                        AnimePlayerStepper(
                                            label = stringResource(R.string.tsz_anime_player_subtitle_size),
                                            value = formatAnimePercent(settings.subtitleScale),
                                            decrease = {
                                                model.setSettings(
                                                    settings.copy(subtitleScale = settings.subtitleScale - 0.1f),
                                                )
                                            },
                                            increase = {
                                                model.setSettings(
                                                    settings.copy(subtitleScale = settings.subtitleScale + 0.1f),
                                                )
                                            },
                                        )
                                    }
                                    item {
                                        AnimePlayerStepper(
                                            label = stringResource(R.string.tsz_anime_player_subtitle_delay),
                                            value = formatAnimeSeconds(settings.subtitleDelay),
                                            decrease = {
                                                model.setSettings(
                                                    settings.copy(subtitleDelay = settings.subtitleDelay - 0.25f),
                                                )
                                            },
                                            increase = {
                                                model.setSettings(
                                                    settings.copy(subtitleDelay = settings.subtitleDelay + 0.25f),
                                                )
                                            },
                                        )
                                    }
                                } else if (tracks.isEmpty()) {
                                    item { AnimePlayerEmptyOptions() }
                                }
                            }
                            AnimePlayerSheet.QUALITY -> {
                                if (state.qualities.isEmpty()) {
                                    item { AnimePlayerEmptyOptions() }
                                } else {
                                    item {
                                        AnimePlayerOption(stringResource(R.string.tsz_anime_player_auto_quality)) {
                                            model.setQuality(0)
                                            onDismiss()
                                        }
                                    }
                                    items(state.qualities) { resolution ->
                                        AnimePlayerOption(
                                            stringResource(R.string.tsz_anime_player_quality_value, resolution),
                                            selected = resolution == state.resolution,
                                        ) {
                                            model.setQuality(resolution)
                                            onDismiss()
                                        }
                                    }
                                }
                                state.hosters.forEachIndexed { index, entry ->
                                    if (entry.isPlainList) return@forEachIndexed
                                    when (val server = entry.state) {
                                        is AnimeHosterState.Ready -> {
                                            if (server.videos.isEmpty()) return@forEachIndexed
                                            item(key = "server_${index}_title") {
                                                TsuzukiSectionLabel(entry.hoster.hosterName)
                                            }
                                            server.videos.forEachIndexed { videoIndex, video ->
                                                item(key = "server_${index}_video_$videoIndex") {
                                                    AnimePlayerOption(
                                                        label = video.videoTitle.ifBlank { entry.hoster.hosterName },
                                                        detail = video.animeResolution?.let {
                                                            stringResource(R.string.tsz_anime_player_quality_value, it)
                                                        }?.takeUnless {
                                                            it.equals(video.videoTitle, ignoreCase = true)
                                                        },
                                                    ) {
                                                        model.selectHosterVideo(index, videoIndex)
                                                        onDismiss()
                                                    }
                                                }
                                            }
                                        }
                                        else -> item(key = "server_$index") {
                                            val detail = when (server) {
                                                AnimeHosterState.Loading -> R.string.tsz_anime_player_server_loading
                                                is AnimeHosterState.Error -> R.string.tsz_anime_player_server_failed
                                                else -> R.string.tsz_anime_player_server_load
                                            }
                                            AnimePlayerOption(
                                                entry.hoster.hosterName,
                                                detail = stringResource(detail),
                                                enabled = server != AnimeHosterState.Loading,
                                            ) { model.loadHoster(index) }
                                        }
                                    }
                                }
                            }
                            AnimePlayerSheet.SPEED -> {
                                items(animePlaybackSpeeds) { speed ->
                                    AnimePlayerOption(formatAnimeSpeed(speed), selected = speed == settings.speed) {
                                        model.setSettings(settings.copy(speed = speed))
                                    }
                                }
                            }
                            AnimePlayerSheet.EPISODES -> {
                                if (state.episodes.isEmpty()) item { AnimePlayerEmptyOptions() }
                                items(state.episodes.sortedWith(animeEpisodeNumberOrder), key = { it.id }) { episode ->
                                    AnimePlayerOption(episode.name, selected = episode.id == state.episode?.id) {
                                        model.selectEpisode(episode.id)
                                        onDismiss()
                                    }
                                }
                            }
                            AnimePlayerSheet.VIDEO -> {
                                item {
                                    AnimePlayerOption(
                                        stringResource(R.string.tsz_anime_player_decoder_hardware),
                                        selected = settings.hardwareDecoding,
                                    ) { model.setSettings(settings.copy(hardwareDecoding = true)) }
                                }
                                item {
                                    AnimePlayerOption(
                                        stringResource(R.string.tsz_anime_player_decoder_software),
                                        selected = !settings.hardwareDecoding,
                                    ) { model.setSettings(settings.copy(hardwareDecoding = false)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AnimePlayerOption(
    label: String,
    detail: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val content = if (selected) tokens.onAccent else tokens.dotOn
    Surface(
        modifier = Modifier.semantics { this.selected = selected },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) tokens.accent else tokens.tile,
        border = tokens.tileBorder?.takeUnless { selected }?.let { BorderStroke(1.dp, it) },
        contentColor = content,
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                tsuzukiMenuLabel(label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detail.isNullOrBlank()) {
                Text(tsuzukiMenuLabel(detail), color = content, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
internal fun AnimePlayerStepper(
    label: String?,
    value: String,
    decrease: () -> Unit,
    increase: () -> Unit,
    contentColor: Color? = null,
) {
    val tokens = TsuzukiTheme.tokens
    val ink = contentColor ?: tokens.dotOn
    val labelColor = contentColor ?: tokens.mutedText
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 300.dp
        Column {
            if (stacked && label != null) {
                Text(
                    tsuzukiMenuLabel(label),
                    color = labelColor,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (!stacked && label != null) {
                    Text(
                        tsuzukiMenuLabel(label),
                        modifier = Modifier.weight(1f),
                        color = labelColor,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = decrease) {
                    Icon(Icons.Outlined.Remove, stringResource(R.string.tsz_anime_player_decrease), tint = ink)
                }
                Text(tsuzukiMenuLabel(value), color = ink, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = increase) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.tsz_anime_player_increase), tint = ink)
                }
            }
        }
    }
}

@Composable
private fun AnimePlayerEmptyOptions() {
    Text(
        tsuzukiMenuLabel(stringResource(R.string.tsz_anime_player_no_options)),
        modifier = Modifier.padding(12.dp),
        color = TsuzukiTheme.tokens.mutedText,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun AnimePlayerTrack.displayLabel(): String = title?.takeIf(String::isNotBlank)
    ?: language?.takeIf(String::isNotBlank)?.let { Locale.forLanguageTag(it).displayLanguage.ifBlank { it } }
    ?: stringResource(R.string.tsz_anime_player_track_number, id)

internal fun formatAnimeSpeed(speed: Float): String = NumberFormat.getNumberInstance().format(speed) + "×"

internal fun formatAnimePercent(level: Float): String = NumberFormat.getPercentInstance().format(level)

private fun formatAnimeSeconds(seconds: Float): String = NumberFormat.getNumberInstance().format(seconds) + " s"
