package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

@Composable
internal fun AnimePlayerContent(
    state: AnimePlayerState,
    model: AnimePlayerModel,
    onBack: () -> Unit,
    onOrientationLock: (Boolean) -> Unit,
    orientationLocked: Boolean,
    onLevelGesture: (volume: Boolean, delta: Float, start: Boolean) -> Float,
    surface: @Composable (Modifier) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    var visible by rememberSaveable { mutableStateOf(true) }
    var locked by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<AnimePlayerSheet?>(null) }
    var level by remember { mutableStateOf<Pair<Boolean, Float>?>(null) }
    val settings = state.settings
    var interaction by remember { mutableIntStateOf(0) }
    var lastNotice by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val fallback = stringResource(R.string.tsz_anime_player_fallback)
    val showControls = visible && !locked

    LaunchedEffect(visible, locked, state.playing, interaction, sheet) {
        if (visible && state.playing && !locked && sheet == null) {
            delay(3_000)
            visible = false
        }
    }
    LaunchedEffect(state.fallbackNotice) {
        if (state.fallbackNotice > lastNotice) {
            lastNotice = state.fallbackNotice
            snackbar.showSnackbar(fallback)
        }
    }
    LaunchedEffect(level) {
        if (level != null) {
            delay(700)
            level = null
        }
    }
    BackHandler(enabled = sheet != null) { sheet = null }
    BackHandler(enabled = locked && sheet == null) {
        locked = false
        visible = true
    }

    Box(Modifier.fillMaxSize().background(tokens.background)) {
        surface(Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize()
                .pointerInput(locked, settings.seekGesture) {
                    detectTapGestures(
                        onTap = { if (!locked) visible = !visible },
                        onDoubleTap = if (!locked && settings.seekGesture) {
                            { offset ->
                                model.seekBy(if (offset.x < size.width / 2) -10 else 10)
                                visible = true
                                interaction++
                            }
                        } else {
                            null
                        },
                    )
                }
                .pointerInput(locked, settings.brightnessGesture, settings.volumeGesture) {
                    if (locked || (!settings.brightnessGesture && !settings.volumeGesture)) return@pointerInput
                    var volume = false
                    var enabled = false
                    var delta = 0f
                    detectVerticalDragGestures(
                        onDragStart = { point ->
                            volume = point.x >= size.width / 2
                            enabled = if (volume) settings.volumeGesture else settings.brightnessGesture
                            delta = 0f
                            if (enabled) level = volume to onLevelGesture(volume, 0f, true)
                        },
                        onVerticalDrag = { _, amount ->
                            if (enabled && size.height > 0) {
                                delta -= amount / size.height
                                level = volume to onLevelGesture(volume, delta, false)
                            }
                        },
                    )
                },
        )
        if (showControls) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to tokens.background.copy(alpha = 0.84f),
                        0.35f to Color.Transparent,
                        0.58f to Color.Transparent,
                        1f to tokens.background.copy(alpha = 0.92f),
                    ),
                ),
            )
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp)) {
                AnimePlayerTopBar(
                    state = state,
                    orientationLocked = orientationLocked,
                    onOrientationLock = { onOrientationLock(!orientationLocked) },
                    onBack = onBack,
                    model = model,
                    onSheet = { sheet = it },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                if (!state.loading && !state.failed && state.nextIn == null) {
                    AnimePlayerTransport(state, model, Modifier.align(Alignment.Center)) { interaction++ }
                }
                Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)) {
                    AnimePlayerTimeline(state, onSeek = model::seekTo, onInteract = { interaction++ })
                    AnimePlayerControlsRow(
                        settings = settings,
                        enabled = state.duration > 0 && !state.loading,
                        onLock = { locked = true },
                        onSheet = { sheet = it },
                        onSeek = {
                            interaction++
                            model.seekBy(it)
                        },
                    )
                }
            }
        }
        val segment = state.activeAniSkip
        if (!locked && sheet == null && segment != null) {
            val label = when (segment.kind) {
                AniSkipKind.OPENING -> R.string.tsz_anime_skip_opening
                AniSkipKind.ENDING -> R.string.tsz_anime_skip_ending
                AniSkipKind.RECAP -> R.string.tsz_anime_skip_recap
            }
            TsuzukiPill(
                text = stringResource(label),
                selected = true,
                onClick = model::skipCurrentSegment,
                role = Role.Button,
                modifier = Modifier.align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(end = 16.dp, bottom = if (showControls) 100.dp else 16.dp),
            )
        }
        if (locked) {
            Box(Modifier.align(Alignment.BottomStart).windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
                FilledIconButton(
                    onClick = {
                        locked = false
                        visible = true
                        interaction++
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = tokens.tile,
                        contentColor = tokens.dotOn,
                    ),
                ) {
                    Icon(Icons.Outlined.LockOpen, stringResource(R.string.tsz_anime_player_unlock))
                }
            }
        }
        if (state.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = tokens.accent)
        }
        if (state.failed) {
            TsuzukiTile(Modifier.align(Alignment.Center), padding = 16.dp) {
                Text(
                    tsuzukiMenuLabel(mokoStringResource(MR.strings.tsz_anime_debug_playback_error)),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.titleSmall,
                )
                Row {
                    TextButton(onClick = model::retry) { Text(mokoStringResource(MR.strings.action_retry)) }
                    TextButton(onClick = onBack) { Text(mokoStringResource(MR.strings.action_webview_back)) }
                }
            }
        }
        state.nextIn?.let { seconds ->
            TsuzukiTile(Modifier.align(Alignment.Center), padding = 16.dp) {
                Text(
                    tsuzukiMenuLabel(stringResource(R.string.tsz_anime_player_next_in, seconds)),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.titleSmall,
                )
                state.next?.let {
                    Text(
                        tsuzukiMenuLabel(it.name),
                        color = tokens.mutedText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row {
                    TextButton(onClick = model::dismissCountdown) {
                        Text(mokoStringResource(MR.strings.action_cancel))
                    }
                    TextButton(onClick = model::next) {
                        Text(stringResource(R.string.tsz_anime_player_next))
                    }
                }
            }
        }
        level?.let { (volume, fraction) ->
            val label = stringResource(
                if (volume) R.string.tsz_anime_player_volume else R.string.tsz_anime_player_brightness,
            )
            TsuzukiTile(Modifier.align(Alignment.TopCenter).padding(top = 72.dp), padding = 10.dp) {
                Text(
                    tsuzukiMenuLabel("$label ${formatAnimePercent(fraction)}"),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp),
        )
        sheet?.let {
            AnimePlayerSheets(it, state, model, onPick = { selected -> sheet = selected }, onDismiss = { sheet = null })
        }
    }
}

@Composable
private fun AnimePlayerTopBar(
    state: AnimePlayerState,
    orientationLocked: Boolean,
    onOrientationLock: () -> Unit,
    onBack: () -> Unit,
    model: AnimePlayerModel,
    onSheet: (AnimePlayerSheet) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    var showMenu by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PlayerIcon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            mokoStringResource(MR.strings.action_webview_back),
            onClick = onBack,
        )
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (state.title.isNotBlank()) {
                Text(
                    tsuzukiMenuLabel(state.title),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            state.episode?.let {
                Text(
                    tsuzukiMenuLabel(it.name),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (state.qualities.isNotEmpty()) {
            TsuzukiPill(
                text = state.resolution?.let { stringResource(R.string.tsz_anime_player_quality_value, it) }
                    ?: stringResource(R.string.tsz_anime_player_quality),
                selected = false,
                onClick = { onSheet(AnimePlayerSheet.QUALITY) },
                role = Role.Button,
            )
        }
        if (state.episodes.isNotEmpty()) {
            PlayerIcon(Icons.Outlined.List, stringResource(R.string.tsz_anime_player_episodes)) {
                onSheet(AnimePlayerSheet.EPISODES)
            }
        }
        PlayerIcon(
            icon = if (orientationLocked) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
            description = stringResource(
                if (orientationLocked) {
                    R.string.tsz_anime_player_orientation_unlock
                } else {
                    R.string.tsz_anime_player_orientation_lock
                },
            ),
            onClick = onOrientationLock,
        )
        Box {
            PlayerIcon(Icons.Outlined.MoreVert, mokoStringResource(MR.strings.label_more)) { showMenu = true }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                TsuzukiMenuItem(
                    text = stringResource(R.string.tsz_anime_player_video),
                    onClick = {
                        showMenu = false
                        onSheet(AnimePlayerSheet.VIDEO)
                    },
                )
                val autoplayLabel = if (state.autoplay) {
                    R.string.tsz_anime_player_autoplay_off
                } else {
                    R.string.tsz_anime_player_autoplay_on
                }
                TsuzukiMenuItem(
                    text = stringResource(autoplayLabel),
                    onClick = {
                        showMenu = false
                        model.setAutoplay(!state.autoplay)
                    },
                )
            }
        }
    }
}

@Composable
private fun AnimePlayerTransport(
    state: AnimePlayerState,
    model: AnimePlayerModel,
    modifier: Modifier,
    onInteract: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        PlayerIcon(
            Icons.Outlined.SkipPrevious,
            stringResource(R.string.tsz_anime_player_previous),
            enabled = state.previous != null,
        ) {
            onInteract()
            model.previous()
        }
        PlayerIcon(Icons.Outlined.Replay10, stringResource(R.string.tsz_anime_player_rewind)) {
            onInteract()
            model.seekBy(-10)
        }
        FilledIconButton(
            onClick = {
                onInteract()
                model.togglePlayback()
            },
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (tokens.isDot) tokens.accent else tokens.tile,
                contentColor = if (tokens.isDot) tokens.onAccent else tokens.dotOn,
            ),
        ) {
            Icon(
                if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (state.playing) {
                    mokoStringResource(MR.strings.action_pause)
                } else {
                    mokoStringResource(MR.strings.tsz_widget_listening_play)
                },
                modifier = Modifier.size(32.dp),
            )
        }
        PlayerIcon(Icons.Outlined.Forward10, stringResource(R.string.tsz_anime_player_forward)) {
            onInteract()
            model.seekBy(10)
        }
        PlayerIcon(
            Icons.Outlined.SkipNext,
            stringResource(R.string.tsz_anime_player_next),
            enabled = state.next != null,
        ) {
            onInteract()
            model.next()
        }
    }
}

@Composable
private fun AnimePlayerControlsRow(
    settings: AnimePlayerSettings,
    enabled: Boolean,
    onLock: () -> Unit,
    onSheet: (AnimePlayerSheet) -> Unit,
    onSeek: (Int) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 560.dp
        Row(
            modifier = if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIcon(Icons.Outlined.Lock, stringResource(R.string.tsz_anime_player_lock), onClick = onLock)
            TsuzukiPill(
                text = formatAnimeSpeed(settings.speed),
                selected = false,
                onClick = { onSheet(AnimePlayerSheet.SPEED) },
                role = Role.Button,
            )
            TsuzukiPill(
                text = stringResource(R.string.tsz_anime_player_subtitles),
                selected = false,
                onClick = { onSheet(AnimePlayerSheet.SUBTITLES) },
                role = Role.Button,
            )
            TsuzukiPill(
                text = stringResource(R.string.tsz_anime_player_audio),
                selected = false,
                onClick = { onSheet(AnimePlayerSheet.AUDIO) },
                role = Role.Button,
            )
            if (enabled) {
                if (!compact) Box(Modifier.weight(1f))
                for (seconds in listOf(-85, 85)) {
                    TsuzukiPill(
                        text = stringResource(R.string.tsz_anime_player_skip, seconds),
                        selected = false,
                        onClick = { onSeek(seconds) },
                        role = Role.Button,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimePlayerTimeline(state: AnimePlayerState, onSeek: (Double) -> Unit, onInteract: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    var preview by remember(state.episode?.id) { mutableStateOf<Float?>(null) }
    val duration = state.duration
    val progress = if (duration > 0) (state.position / duration).toFloat().coerceIn(0f, 1f) else 0f
    val buffered = if (duration > 0) (state.bufferedUntil / duration).toFloat().coerceIn(0f, 1f) else 0f
    val current = preview ?: progress
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            DateUtils.formatElapsedTime((current * duration).toLong()),
            color = tokens.dotOn,
            style = MaterialTheme.typography.labelMedium,
        )
        Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
            TsuzukiProgressBar(
                progress = buffered,
                fill = tokens.mutedText,
                track = tokens.track,
                dots = 36,
                spreadDots = true,
                thickness = 5.dp,
            )
            TsuzukiProgressBar(
                progress = current,
                fill = if (tokens.isDot) tokens.dotOn else tokens.accent,
                track = Color.Transparent,
                dots = 36,
                spreadDots = true,
                thickness = 5.dp,
            )
            if (duration > 0 && state.settings.aniSkipMode != AniSkipMode.OFF && state.aniSkipSegments.isNotEmpty()) {
                Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                    state.aniSkipSegments.forEach { segment ->
                        val start = (segment.start / duration).toFloat().coerceIn(0f, 1f) * size.width
                        val end = (segment.end / duration).toFloat().coerceIn(0f, 1f) * size.width
                        drawLine(
                            tokens.accent.copy(alpha = 0.65f),
                            Offset(start, 0f),
                            Offset(end, 0f),
                            3.dp.toPx(),
                            StrokeCap.Round,
                        )
                    }
                }
            }
            Slider(
                value = current,
                onValueChange = {
                    onInteract()
                    preview = it
                },
                onValueChangeFinished = {
                    preview?.let { onSeek(it * duration) }
                    preview = null
                },
                enabled = duration > 0 && !state.loading,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    disabledThumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent,
                ),
            )
        }
        Text(
            "-" + DateUtils.formatElapsedTime(((1f - current) * duration).toLong()),
            color = tokens.dotOn,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun PlayerIcon(icon: ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            icon,
            contentDescription = description,
            tint = TsuzukiTheme.tokens.dotOn.copy(alpha = if (enabled) 1f else 0.38f),
        )
    }
}
