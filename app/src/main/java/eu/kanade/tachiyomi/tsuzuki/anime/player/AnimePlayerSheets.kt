package eu.kanade.tachiyomi.tsuzuki.anime.player

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import java.text.NumberFormat
import java.util.Locale

/** The player's overlays: Episodes and Audio & subtitles have their own boards; the rest share the side panel. */
internal enum class AnimePlayerSheet(@StringRes val title: Int) {
    QUALITY(R.string.tsz_anime_player_quality),
    SPEED(R.string.tsz_anime_player_speed),
    VIDEO(R.string.tsz_anime_player_video),
    EPISODES(R.string.tsz_anime_player_episodes),
    AUDIO_SUBS(R.string.tsz_anime_player_audio_subtitles),
}

private val panelSheets = listOf(AnimePlayerSheet.QUALITY, AnimePlayerSheet.SPEED, AnimePlayerSheet.VIDEO)

internal val animePlaybackSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

/** Quality and servers, speed and the video decoder, in the right-hand drawer from the Episodes board. */
@Composable
internal fun AnimePlayerSheets(
    active: AnimePlayerSheet,
    state: AnimePlayerState,
    model: AnimePlayerModel,
    onPick: (AnimePlayerSheet) -> Unit,
    onDismiss: () -> Unit,
) {
    val settings = state.settings
    AnimePlayerDrawer(title = stringResource(active.title), onDismiss = onDismiss) {
        TsuzukiPillRow(
            labels = panelSheets.map { stringResource(it.title) },
            selected = panelSheets.indexOf(active).coerceAtLeast(0),
            onClick = { onPick(panelSheets[it]) },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            when (active) {
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
                AnimePlayerSheet.EPISODES, AnimePlayerSheet.AUDIO_SUBS -> Unit
            }
        }
    }
}

/**
 * Board "Dot · Episodes panel": a right-hand drawer over a dimmed video (full width when the screen is narrow), a
 * display-face title and a close button. Tapping the dimmed side closes it.
 */
@Composable
internal fun AnimePlayerDrawer(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = maxWidth > 540.dp
        val panelWidth = if (isWide) (maxWidth * 0.44f).coerceIn(360.dp, 480.dp) else maxWidth
        Row(Modifier.fillMaxSize()) {
            if (isWide) {
                Box(
                    Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(alpha = 0.6f))
                        .tsuzukiClickable(role = Role.Button, onClick = onDismiss),
                )
            }
            Surface(
                modifier = Modifier.width(panelWidth).fillMaxHeight(),
                shape = if (isWide &&
                    !tokens.isDot
                ) {
                    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                } else {
                    RectangleShape
                },
                color = if (tokens.isDot) tokens.tile else tokens.background,
                contentColor = tokens.dotOn,
                border = tokens.tileBorder?.takeIf { isWide }?.let { BorderStroke(1.dp, it) },
            ) {
                Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            tsuzukiMenuLabel(title),
                            modifier = Modifier.weight(1f),
                            style = animePanelTitleStyle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        actions()
                        IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                            Icon(
                                Icons.Outlined.Close,
                                stringResource(R.string.tsz_anime_player_close_options),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    content()
                }
            }
        }
    }
}

/** A choice row from the Audio & subtitles board: an accent check for the chosen one, 44 dp tall. */
@Composable
internal fun AnimePlayerOption(
    label: String,
    detail: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val ink = when {
        !enabled -> tokens.mutedText
        selected -> tokens.dotOn
        else -> tokens.dotOn.copy(alpha = 0.74f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { this.selected = selected },
        shape = RoundedCornerShape(if (tokens.isDot) tokens.tileRadius else 12.dp),
        color = Color.Transparent,
        contentColor = ink,
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.width(18.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                tsuzukiMenuLabel(label),
                modifier = Modifier.weight(1f),
                style = animeOptionStyle(selected),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detail.isNullOrBlank()) {
                Text(
                    tsuzukiMenuLabel(detail),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun animeOptionStyle(selected: Boolean) = MaterialTheme.typography.bodyMedium.copy(
    fontSize = 13.sp,
    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
)

@Composable
internal fun animePanelTitleStyle() = MaterialTheme.typography.headlineSmall.copy(
    fontSize = if (TsuzukiTheme.tokens.isDot) 20.sp else 26.sp,
)

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
internal fun AnimePlayerEmptyOptions() {
    Text(
        tsuzukiMenuLabel(stringResource(R.string.tsz_anime_player_no_options)),
        modifier = Modifier.padding(12.dp),
        color = TsuzukiTheme.tokens.mutedText,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
internal fun AnimePlayerTrack.displayLabel(): String = title?.takeIf(String::isNotBlank)
    ?: language?.takeIf(String::isNotBlank)?.let { Locale.forLanguageTag(it).displayLanguage.ifBlank { it } }
    ?: stringResource(R.string.tsz_anime_player_track_number, id)

internal fun formatAnimeSpeed(speed: Float): String = NumberFormat.getNumberInstance().format(speed) + "×"

internal fun formatAnimePercent(level: Float): String = NumberFormat.getPercentInstance().format(level)

internal fun formatAnimeSeconds(seconds: Float): String = NumberFormat.getNumberInstance().format(seconds) + " s"
