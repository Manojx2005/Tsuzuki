package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.components.errorHeading
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.error.errorCopyForUi
import kotlinx.coroutines.delay
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

/**
 * The anime player screen (PLAYER-REDESIGN, owner-reviewed board "Tsuzuki Anime Player Redesign"): a top bar with
 * the title and audio/subtitles, picture-in-picture and settings; bare play/pause between ±10 s; a flat scrubber with
 * AniSkip chapter ticks; and a bottom row for speed, lock, episodes, audio & subtitles and the next episode. With the
 * controls hidden only a hairline progress bar, the skip cluster and the double-tap seek ripple remain.
 */
@Composable
internal fun AnimePlayerContent(
    state: AnimePlayerState,
    model: AnimePlayerModel,
    onBack: () -> Unit,
    onOrientationLock: (Boolean) -> Unit,
    orientationLocked: Boolean,
    pictureInPicture: Boolean,
    onLevelGesture: (volume: Boolean, delta: Float, start: Boolean) -> Float,
    onPictureInPicture: (() -> Unit)? = null,
    surface: @Composable (Modifier) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    var visible by rememberSaveable { mutableStateOf(true) }
    var locked by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<AnimePlayerSheet?>(null) }
    var level by remember { mutableStateOf<Pair<Boolean, Float>?>(null) }
    var ripple by remember { mutableStateOf<AnimeSeekRipple?>(null) }
    val settings = state.settings
    var interaction by remember { mutableIntStateOf(0) }
    var lastNotice by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val fallback = stringResource(R.string.tsz_anime_player_fallback)
    val covered = !state.revealed && !state.failed
    val showControls = visible && !locked && !covered && state.nextIn == null

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
    LaunchedEffect(ripple) {
        if (ripple != null) {
            delay(SEEK_RIPPLE_MS)
            ripple = null
        }
    }
    BackHandler(enabled = !pictureInPicture && sheet != null) { sheet = null }
    BackHandler(enabled = !pictureInPicture && locked && sheet == null) {
        locked = false
        visible = true
    }

    Box(Modifier.fillMaxSize().background(tokens.background)) {
        surface(Modifier.fillMaxSize())
        if (pictureInPicture) return@Box
        Box(
            Modifier.fillMaxSize()
                .pointerInput(locked, settings.seekGesture) {
                    fun seek(forward: Boolean) {
                        model.seekBy(if (forward) SEEK_STEP else -SEEK_STEP)
                        val current = ripple
                        ripple = if (current != null && current.forward == forward) {
                            current.copy(seconds = current.seconds + SEEK_STEP, taps = current.taps + 1)
                        } else {
                            AnimeSeekRipple(forward, SEEK_STEP)
                        }
                        interaction++
                    }
                    detectTapGestures(
                        onTap = { offset ->
                            val forward = offset.x >= size.width / 2
                            // Board "TAP AGAIN FOR MORE": while the ripple shows, a tap on its side seeks again.
                            if (!locked && ripple?.forward == forward) {
                                seek(forward)
                            } else if (!locked) {
                                visible = !visible
                            }
                        },
                        onDoubleTap = if (!locked && settings.seekGesture) {
                            { offset -> seek(offset.x >= size.width / 2) }
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
        ripple?.let { AnimeSeekRippleOverlay(it, Modifier.align(if (it.forward) Alignment.CenterEnd else Alignment.CenterStart)) }
        // Owner 2026-11-25: resuming looked stuck for a frame or two (mpv holds its first frame until the audio
        // output starts). Keep the video covered until its clock moves, then fade it in (board "Dot · Resume").
        androidx.compose.animation.AnimatedVisibility(
            visible = covered,
            enter = androidx.compose.animation.EnterTransition.None,
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(REVEAL_FADE_MS)),
        ) {
            AnimeResumeCover(state, onBack)
        }
        if (!showControls && !covered && state.nextIn == null && state.duration > 0) {
            AnimeHairlineProgress(state.position / state.duration, Modifier.align(Alignment.BottomCenter))
        }
        if (showControls) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to tokens.background.copy(alpha = 0.85f),
                        0.29f to Color.Transparent,
                        0.61f to Color.Transparent,
                        1f to tokens.background.copy(alpha = 0.92f),
                    ),
                ),
            )
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                AnimePlayerTopBar(
                    state = state,
                    orientationLocked = orientationLocked,
                    onOrientationLock = { onOrientationLock(!orientationLocked) },
                    onBack = onBack,
                    model = model,
                    onPictureInPicture = onPictureInPicture,
                    onSheet = { sheet = it },
                    modifier = Modifier.align(Alignment.TopCenter).padding(start = 16.dp, end = 16.dp, top = 14.dp),
                )
                if (!state.loading && !state.failed) {
                    AnimePlayerTransport(state, model, Modifier.align(Alignment.Center)) { interaction++ }
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    AnimePlayerTimeline(
                        state,
                        onSeek = model::seekTo,
                        onInteract = { interaction++ },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    AnimePlayerControlsRow(
                        state = state,
                        onLock = { locked = true },
                        onSheet = { sheet = it },
                        onNext = {
                            interaction++
                            model.next()
                        },
                    )
                }
            }
        }
        if (!locked && sheet == null && !covered && !state.loading && !state.failed && state.nextIn == null &&
            state.duration > 0
        ) {
            AnimeSkipCluster(
                state = state,
                expanded = showControls,
                onSeek = {
                    interaction++
                    model.seekBy(it)
                },
                onSkipSegment = {
                    interaction++
                    model.skipCurrentSegment()
                },
                modifier = Modifier.align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(end = 24.dp, bottom = if (showControls) 124.dp else 40.dp),
            )
        }
        if (locked) {
            Box(Modifier.align(Alignment.BottomStart).windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
                AnimePlayerAction(
                    icon = Icons.Outlined.LockOpen,
                    label = null,
                    description = stringResource(R.string.tsz_anime_player_unlock),
                    framed = true,
                ) {
                    locked = false
                    visible = true
                    interaction++
                }
            }
        }
        if (!covered && (state.loading || (state.buffering && !state.failed))) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = tokens.accent)
        }
        if (state.failed) {
            val context = LocalContext.current
            val copy = remember(state.error) { state.error?.let { errorCopyForUi(it) } }
            TsuzukiTile(Modifier.align(Alignment.Center), padding = 16.dp) {
                Text(
                    tsuzukiMenuLabel(mokoStringResource(MR.strings.tsz_anime_debug_playback_error)),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (copy != null) {
                    Text(
                        tsuzukiMenuLabel(context.errorHeading(copy)),
                        color = tokens.dotOn,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (copy.technical.isNotBlank()) {
                        Text(
                            copy.technical,
                            color = tokens.mutedText,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Row {
                    TextButton(onClick = model::retry) {
                        Text(tsuzukiMenuLabel(mokoStringResource(MR.strings.action_retry)), color = tokens.dotOn)
                    }
                    TextButton(onClick = onBack) {
                        Text(tsuzukiMenuLabel(mokoStringResource(MR.strings.action_webview_back)), color = tokens.dotOn)
                    }
                }
            }
        }
        state.nextIn?.let { seconds ->
            AnimeNextUp(seconds, state, onPlay = model::next, onCancel = model::dismissCountdown)
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
        when (val active = sheet) {
            null -> Unit
            AnimePlayerSheet.EPISODES -> AnimeEpisodesDrawer(state, model, onDismiss = { sheet = null })
            AnimePlayerSheet.AUDIO_SUBS -> AnimeAudioSubsPanel(state, model, onDismiss = { sheet = null })
            else -> AnimePlayerSheets(active, state, model, onPick = { sheet = it }, onDismiss = { sheet = null })
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
    onPictureInPicture: (() -> Unit)?,
    onSheet: (AnimePlayerSheet) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    var showMenu by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimePlayerIcon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            mokoStringResource(MR.strings.action_webview_back),
            // Colour board: the back button sits in a soft circle.
            background = if (tokens.isDot) null else tokens.tile,
            onClick = onBack,
        )
        Column(Modifier.weight(1f).padding(start = if (tokens.isDot) 0.dp else 6.dp)) {
            if (state.title.isNotBlank()) {
                Text(
                    tsuzukiMenuLabel(state.title),
                    color = tokens.dotOn,
                    style = animeTitleStyle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            state.episode?.let {
                Text(
                    tsuzukiMenuLabel(animeEpisodeLabel(it)),
                    color = tokens.mutedText,
                    style = animeCaptionStyle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AnimePlayerIcon(Icons.Outlined.Subtitles, stringResource(R.string.tsz_anime_player_audio_subtitles)) {
            onSheet(AnimePlayerSheet.AUDIO_SUBS)
        }
        if (onPictureInPicture != null) {
            AnimePlayerIcon(
                Icons.Outlined.PictureInPictureAlt,
                stringResource(R.string.tsz_anime_player_picture_in_picture),
                enabled = !state.loading && !state.failed,
                onClick = onPictureInPicture,
            )
        }
        Box {
            AnimePlayerIcon(Icons.Outlined.Settings, stringResource(R.string.tsz_anime_player_settings)) {
                showMenu = true
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (state.qualities.isNotEmpty() || state.hosters.any { !it.isPlainList }) {
                    val quality = stringResource(R.string.tsz_anime_player_quality)
                    TsuzukiMenuItem(
                        text = state.resolution?.let {
                            "$quality · " + stringResource(R.string.tsz_anime_player_quality_value, it)
                        } ?: quality,
                        onClick = {
                            showMenu = false
                            onSheet(AnimePlayerSheet.QUALITY)
                        },
                    )
                }
                TsuzukiMenuItem(
                    text = stringResource(R.string.tsz_anime_player_video),
                    onClick = {
                        showMenu = false
                        onSheet(AnimePlayerSheet.VIDEO)
                    },
                )
                TsuzukiMenuItem(
                    text = stringResource(
                        if (orientationLocked) {
                            R.string.tsz_anime_player_orientation_unlock
                        } else {
                            R.string.tsz_anime_player_orientation_lock
                        },
                    ),
                    onClick = {
                        showMenu = false
                        onOrientationLock()
                    },
                )
                TsuzukiMenuItem(
                    text = stringResource(
                        if (state.autoplay) R.string.tsz_anime_player_autoplay_off else R.string.tsz_anime_player_autoplay_on,
                    ),
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
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(72.dp)) {
        AnimeSeekButton(forward = false) {
            onInteract()
            model.seekBy(-SEEK_STEP)
        }
        // Owner board comment: no circle around play/pause; a bare large icon, white in Dot, accent in Colour.
        val size = if (tokens.isDot) 76.dp else 80.dp
        IconButton(
            onClick = {
                onInteract()
                model.togglePlayback()
            },
            modifier = Modifier.size(size),
        ) {
            Icon(
                if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (state.playing) {
                    mokoStringResource(MR.strings.action_pause)
                } else {
                    mokoStringResource(MR.strings.tsz_widget_listening_play)
                },
                modifier = Modifier.size(if (tokens.isDot) 52.dp else 54.dp),
                tint = if (tokens.isDot) tokens.dotOn else tokens.accent,
            )
        }
        AnimeSeekButton(forward = true) {
            onInteract()
            model.seekBy(SEEK_STEP)
        }
    }
}

@Composable
private fun AnimeSeekButton(forward: Boolean, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = tokens.dotOn,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                Icons.Outlined.Replay,
                stringResource(if (forward) R.string.tsz_anime_player_forward else R.string.tsz_anime_player_rewind),
                modifier = Modifier.size(30.dp).graphicsLayer { if (forward) scaleX = -1f },
            )
            Text(
                SEEK_STEP.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (tokens.isDot) 10.sp else 11.sp,
                    fontWeight = if (tokens.isDot) FontWeight.Normal else FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun AnimePlayerControlsRow(
    state: AnimePlayerState,
    onLock: () -> Unit,
    onSheet: (AnimePlayerSheet) -> Unit,
    onNext: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 680.dp || LocalDensity.current.fontScale >= 1.3f
        Row(
            modifier = if (compact) Modifier.horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (TsuzukiTheme.tokens.isDot) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimePlayerAction(
                Icons.Outlined.Speed,
                stringResource(R.string.tsz_anime_player_speed_value, formatAnimeSpeed(state.settings.speed)),
            ) { onSheet(AnimePlayerSheet.SPEED) }
            AnimePlayerAction(
                Icons.Outlined.Lock,
                stringResource(R.string.tsz_anime_player_lock_short),
                description = stringResource(R.string.tsz_anime_player_lock),
                onClick = onLock,
            )
            if (compact) Spacer(Modifier.width(16.dp)) else Spacer(Modifier.weight(1f))
            if (state.episodes.isNotEmpty()) {
                AnimePlayerAction(Icons.Outlined.GridView, stringResource(R.string.tsz_anime_player_episodes)) {
                    onSheet(AnimePlayerSheet.EPISODES)
                }
            }
            AnimePlayerAction(Icons.Outlined.Subtitles, stringResource(R.string.tsz_anime_player_audio_subtitles)) {
                onSheet(AnimePlayerSheet.AUDIO_SUBS)
            }
            if (state.next != null) {
                AnimePlayerAction(
                    Icons.Filled.SkipNext,
                    stringResource(R.string.tsz_anime_player_next),
                    filled = true,
                    onClick = onNext,
                )
            }
        }
    }
}

@Composable
private fun AnimePlayerTimeline(
    state: AnimePlayerState,
    onSeek: (Double) -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    var preview by remember(state.episode?.id) { mutableStateOf<Float?>(null) }
    val duration = state.duration
    val progress = if (duration > 0) (state.position / duration).toFloat().coerceIn(0f, 1f) else 0f
    val buffered = if (duration > 0) (state.bufferedUntil / duration).toFloat().coerceIn(0f, 1f) else 0f
    val current = preview ?: progress
    // Chapter ticks: where an AniSkip segment starts or ends, not at the very start or end of the episode.
    val ticks = remember(state.aniSkipSegments, duration) {
        if (duration <= 0) {
            emptyList()
        } else {
            state.aniSkipSegments.flatMap { listOf(it.start, it.end) }
                .filter { it > 0.5 && it < duration - 0.5 }
                .map { (it / duration).toFloat() }
                .distinct()
        }
    }
    val timeStyle = animeTimeStyle()
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(DateUtils.formatElapsedTime((current * duration).toLong()), color = tokens.dotOn, style = timeStyle)
        Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
            AnimeScrubber(current, buffered, ticks, Modifier.fillMaxWidth().height(20.dp))
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
            "−" + DateUtils.formatElapsedTime(((1f - current) * duration).toLong()),
            color = tokens.mutedText,
            style = timeStyle,
        )
    }
}

/** Board scrubber: flat track, buffered, played (accent) and a round thumb; white ticks at AniSkip chapters. */
@Composable
private fun AnimeScrubber(progress: Float, buffered: Float, ticks: List<Float>, modifier: Modifier) {
    val tokens = TsuzukiTheme.tokens
    val dot = tokens.isDot
    val bufferedColor = tokens.dotOn.copy(alpha = if (dot) 0.24f else 0.36f)
    Canvas(modifier) {
        val thickness = (if (dot) 4.dp else 5.dp).toPx()
        val radius = if (dot) CornerRadius.Zero else CornerRadius(thickness / 2)
        val top = (size.height - thickness) / 2
        fun bar(fraction: Float, color: Color) {
            if (fraction <= 0f) return
            drawRoundRect(color, Offset(0f, top), Size(size.width * fraction.coerceIn(0f, 1f), thickness), radius)
        }
        bar(1f, tokens.track)
        bar(buffered, bufferedColor)
        bar(progress, tokens.accent)
        val tickWidth = 2.dp.toPx()
        val tickHeight = 10.dp.toPx()
        ticks.forEach { fraction ->
            drawRect(
                tokens.dotOn.copy(alpha = 0.6f),
                Offset(size.width * fraction - tickWidth / 2, (size.height - tickHeight) / 2),
                Size(tickWidth, tickHeight),
            )
        }
        val center = Offset(size.width * progress.coerceIn(0f, 1f), size.height / 2)
        if (dot) {
            drawCircle(tokens.accent, 7.dp.toPx(), center)
        } else {
            drawCircle(tokens.background, 8.dp.toPx(), center)
            drawCircle(tokens.accent, 5.dp.toPx(), center)
        }
    }
}

/** A bottom-row action: icon + label, 44 dp tall; Dot is square-cornered and uppercase, Colour a pill. */
@Composable
internal fun AnimePlayerAction(
    icon: ImageVector,
    label: String?,
    description: String? = null,
    filled: Boolean = false,
    framed: Boolean = false,
    onClick: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val shape = animeControlShape()
    val content = if (filled) tokens.onAccent else tokens.dotOn
    Surface(
        onClick = onClick,
        shape = shape,
        color = when {
            filled -> tokens.accent
            framed -> tokens.tile
            else -> Color.Transparent
        },
        border = tokens.tileBorder?.takeIf { framed }?.let { BorderStroke(1.dp, it) },
        contentColor = content,
    ) {
        Row(
            Modifier.heightIn(min = 44.dp).padding(
                if (label == null) {
                    PaddingValues(10.dp)
                } else {
                    PaddingValues(horizontal = if (filled) (if (tokens.isDot) 14.dp else 18.dp) else animeActionPadding())
                },
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = description ?: label, modifier = Modifier.size(if (label == null) 22.dp else 18.dp))
            if (label != null) {
                Text(
                    tsuzukiMenuLabel(label),
                    style = animeActionStyle(bold = filled),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AnimePlayerIcon(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    background: Color? = null,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp).then(
            if (background != null) Modifier.background(background, CircleShape) else Modifier,
        ),
    ) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(22.dp),
            tint = TsuzukiTheme.tokens.dotOn.copy(alpha = if (enabled) 1f else 0.38f),
        )
    }
}

/** The board's two corner styles: Dot keeps the tile radius, Colour rounds controls into pills. */
@Composable
internal fun animeControlShape(): Shape {
    val tokens = TsuzukiTheme.tokens
    return if (tokens.isDot) RoundedCornerShape(tokens.tileRadius) else RoundedCornerShape(percent = 50)
}

@Composable
private fun animeActionPadding() = if (TsuzukiTheme.tokens.isDot) 12.dp else 14.dp

@Composable
internal fun animeTitleStyle(): TextStyle = if (TsuzukiTheme.tokens.isDot) {
    MaterialTheme.typography.displaySmall.copy(fontSize = 22.sp, lineHeight = 24.sp)
} else {
    MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp, lineHeight = 30.sp)
}

@Composable
internal fun animeCaptionStyle(): TextStyle = if (TsuzukiTheme.tokens.isDot) {
    MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.sp)
} else {
    MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
internal fun animeActionStyle(bold: Boolean = false): TextStyle = if (TsuzukiTheme.tokens.isDot) {
    MaterialTheme.typography.labelMedium.copy(
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
} else {
    MaterialTheme.typography.labelLarge.copy(
        fontSize = 13.sp,
        fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold,
    )
}

@Composable
private fun animeTimeStyle(): TextStyle = if (TsuzukiTheme.tokens.isDot) {
    MaterialTheme.typography.headlineSmall.copy(fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
} else {
    MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
}

/** "E3 · Title" when the episode has a whole number, otherwise its name (the board's season isn't known). */
@Composable
internal fun animeEpisodeLabel(episode: Episode): String {
    val number = episode.episodeNumber.takeIf { it.isFinite() && it >= 0 && it % 1.0 == 0.0 }?.toLong()
    return number?.let { stringResource(R.string.tsz_anime_player_episode_label, it.toString(), episode.name) }
        ?: episode.name
}

internal const val SEEK_STEP = 10

/** How long the double-tap ripple stays, during which a tap on its side seeks again. */
private const val SEEK_RIPPLE_MS = 800L

/** How long the video takes to fade in once its clock moves. */
private const val REVEAL_FADE_MS = 180
