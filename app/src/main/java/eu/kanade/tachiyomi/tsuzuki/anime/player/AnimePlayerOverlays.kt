package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.text.format.DateUtils
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.tsuzuki.anime.preview.AnimeEpisodeThumbnail
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

/** A double-tap seek in progress: which side, and how far it has gone. */
internal data class AnimeSeekRipple(val forward: Boolean, val seconds: Int, val taps: Int = 0)

/** Board "Dot · Controls hidden": a half capsule on the tapped side with the running total. */
@Composable
internal fun AnimeSeekRippleOverlay(ripple: AnimeSeekRipple, modifier: Modifier) {
    val tokens = TsuzukiTheme.tokens
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val radius = maxHeight / 2
        val shape = if (ripple.forward) {
            RoundedCornerShape(topStart = radius, bottomStart = radius)
        } else {
            RoundedCornerShape(topEnd = radius, bottomEnd = radius)
        }
        Box(
            modifier.width(maxWidth * 0.33f).fillMaxHeight().clip(shape).background(tokens.dotOn.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val alphas = listOf(1f, 0.6f, 0.3f).let { if (ripple.forward) it.reversed() else it }
                    alphas.forEach { alpha ->
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).alpha(alpha).rotate(if (ripple.forward) 0f else 180f),
                            tint = tokens.dotOn,
                        )
                    }
                }
                Text(
                    tsuzukiMenuLabel(
                        stringResource(
                            R.string.tsz_anime_player_skip,
                            if (ripple.forward) ripple.seconds else -ripple.seconds,
                        ),
                    ),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                )
                Text(
                    tsuzukiMenuLabel(stringResource(R.string.tsz_anime_player_tap_again)),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                )
            }
        }
    }
}

/** Controls hidden: a 3 dp progress line along the bottom edge. */
@Composable
internal fun AnimeHairlineProgress(fraction: Double, modifier: Modifier) {
    val tokens = TsuzukiTheme.tokens
    Box(modifier.fillMaxWidth().height(3.dp).background(tokens.track)) {
        Box(
            Modifier.fillMaxWidth(fraction.toFloat().takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f)
                .fillMaxHeight()
                .background(tokens.accent),
        )
    }
}

/**
 * The skip cluster: "+85 s" always (the app's 85 s skip; "−85 s" too while the controls are shown) and the AniSkip
 * button only while the playhead is inside a segment AniSkip knows. Its underline is how far through that segment.
 */
@Composable
internal fun AnimeSkipCluster(
    state: AnimePlayerState,
    expanded: Boolean,
    onSeek: (Int) -> Unit,
    onSkipSegment: () -> Unit,
    modifier: Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (expanded) {
            AnimeSkipButton(stringResource(R.string.tsz_anime_player_skip, -SKIP_SECONDS), forward = false) {
                onSeek(-SKIP_SECONDS)
            }
        }
        AnimeSkipButton(stringResource(R.string.tsz_anime_player_skip, SKIP_SECONDS), forward = true) {
            onSeek(SKIP_SECONDS)
        }
        state.activeAniSkip?.let { segment ->
            val label = when (segment.kind) {
                AniSkipKind.OPENING -> R.string.tsz_anime_skip_opening
                AniSkipKind.ENDING -> R.string.tsz_anime_skip_ending
                AniSkipKind.RECAP -> R.string.tsz_anime_skip_recap
            }
            val length = segment.end - segment.start
            val through = if (length > 0) ((state.position - segment.start) / length).toFloat().coerceIn(0f, 1f) else 0f
            Surface(
                onClick = onSkipSegment,
                shape = animeControlShape(),
                color = if (tokens.isDot) tokens.tile else tokens.accent,
                contentColor = if (tokens.isDot) tokens.dotOn else tokens.onAccent,
                border = tokens.tileBorder?.let { BorderStroke(1.dp, it) },
            ) {
                Box {
                    Row(
                        Modifier.heightIn(min = 48.dp).padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(tsuzukiMenuLabel(stringResource(label)), style = animeSkipStyle())
                    }
                    if (tokens.isDot) {
                        Box(
                            Modifier.align(Alignment.BottomStart).fillMaxWidth(through).height(3.dp)
                                .background(tokens.accent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeSkipButton(label: String, forward: Boolean, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Surface(
        onClick = onClick,
        shape = animeControlShape(),
        color = if (tokens.isDot) tokens.tile else tokens.background.copy(alpha = 0.72f),
        contentColor = tokens.dotOn,
        border = tokens.tileBorder?.let { BorderStroke(1.dp, it) },
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.Replay,
                contentDescription = null,
                modifier = Modifier.size(18.dp).graphicsLayer { if (forward) scaleX = -1f },
            )
            Text(tsuzukiMenuLabel(label), style = animeSkipStyle())
        }
    }
}

@Composable
private fun animeSkipStyle() = MaterialTheme.typography.labelLarge.copy(
    fontSize = 13.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = if (TsuzukiTheme.tokens.isDot) 1.sp else 0.sp,
)

/** Board "Dot · Resume": covers the video until its clock moves, with where it resumes and a back button. */
@Composable
internal fun AnimeResumeCover(state: AnimePlayerState, onBack: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Box(Modifier.fillMaxSize().background(tokens.background)) {
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AnimeLoadingDots()
            if (state.position >= 1.0) {
                Text(
                    tsuzukiMenuLabel(
                        stringResource(
                            R.string.tsz_anime_player_resuming_at,
                            DateUtils.formatElapsedTime(state.position.toLong()),
                        ),
                    ),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
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
        IconButton(
            onClick = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 16.dp, top = 14.dp).size(44.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                mokoStringResource(MR.strings.action_webview_back),
                modifier = Modifier.size(22.dp),
                tint = tokens.dotOn,
            )
        }
        val known = state.duration.takeIf { it > 0 } ?: state.episode?.totalSeconds?.toDouble()?.takeIf { it > 0 }
        if (known != null) AnimeHairlineProgress(state.position / known, Modifier.align(Alignment.BottomCenter))
    }
}

/** Three accent dots that brighten in turn, the board's loading mark. */
@Composable
private fun AnimeLoadingDots() {
    val tokens = TsuzukiTheme.tokens
    val phase by rememberInfiniteTransition(label = "resume").animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing), RepeatMode.Restart),
        label = "resumeDots",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val step = ((index - phase.toInt()) % 3 + 3) % 3
            Box(
                Modifier.size(8.dp).alpha(listOf(1f, 0.6f, 0.3f)[step]).background(tokens.accent, CircleShape),
            )
        }
    }
}

/**
 * Board "Dot · Next episode countdown". The countdown only starts once the episode has ended, so the board's
 * "credits keep playing" frame and WATCH CREDITS don't apply: the last frame stays visible on the left and
 * Cancel stops the countdown.
 */
@Composable
internal fun AnimeNextUp(seconds: Int, state: AnimePlayerState, onPlay: () -> Unit, onCancel: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    BoxWithConstraints(
        Modifier.fillMaxSize().background(
            Brush.horizontalGradient(
                0f to tokens.background.copy(alpha = 0.35f),
                0.42f to tokens.background.copy(alpha = 0.9f),
                1f to tokens.background.copy(alpha = 0.96f),
            ),
        ),
    ) {
        val start = (maxWidth * 0.485f).coerceAtMost(maxWidth - 360.dp).coerceAtLeast(24.dp)
        Column(
            Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = start, end = 28.dp, top = 28.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                tsuzukiMenuLabel(stringResource(R.string.tsz_anime_player_next_episode_in)),
                color = tokens.mutedText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 2.sp),
            )
            Text(
                seconds.toString(),
                color = tokens.accent,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, lineHeight = 60.sp),
            )
            state.next?.let { next ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val sourceId = state.sourceId
                    if (sourceId != null) {
                        AnimeEpisodeThumbnail(
                            sourceId = sourceId,
                            episode = next,
                            coverUrl = state.coverUrl,
                            modifier = Modifier.width(128.dp).height(72.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            tsuzukiMenuLabel(animeEpisodeLabel(next)),
                            color = tokens.dotOn,
                            style = animeActionStyle(bold = true).copy(fontSize = 13.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        animeEpisodeMeta(next.id, next.totalSeconds, watched = false)?.let {
                            Text(
                                tsuzukiMenuLabel(it),
                                color = tokens.mutedText,
                                style = animeCaptionStyle().copy(fontSize = 10.sp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                    shape = animeControlShape(),
                    color = tokens.accent,
                    contentColor = tokens.onAccent,
                ) {
                    Row(
                        Modifier.heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            tsuzukiMenuLabel(stringResource(R.string.tsz_anime_player_play_now)),
                            modifier = Modifier.padding(start = 8.dp),
                            style = animeActionStyle(bold = true).copy(fontSize = 13.sp),
                        )
                    }
                }
                Surface(
                    onClick = onCancel,
                    shape = animeControlShape(),
                    color = if (tokens.isDot) Color.Transparent else tokens.tile,
                    contentColor = tokens.dotOn,
                    border = tokens.tileBorder?.let { BorderStroke(1.dp, it) },
                ) {
                    Box(
                        Modifier.heightIn(min = 48.dp).padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(tsuzukiMenuLabel(mokoStringResource(MR.strings.action_cancel)), style = animeActionStyle())
                    }
                }
            }
        }
    }
}

/** "24 min · Watched" / "24 min · Saved": only what is known (duration once played, the download queue). */
@Composable
internal fun animeEpisodeMeta(episodeId: Long, totalSeconds: Long, watched: Boolean): String? {
    val downloads = remember { Injekt.get<AnimeDownloads>() }
    val queue by downloads.state.collectAsState()
    val parts = buildList {
        if (totalSeconds >= 60) add(stringResource(R.string.tsz_anime_player_minutes, (totalSeconds / 60).toInt()))
        if (watched) add(stringResource(R.string.tsz_anime_player_watched))
        if (queue.isSaved(episodeId)) add(stringResource(R.string.tsz_anime_player_saved))
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** The app's long skip (one opening's length). */
internal const val SKIP_SECONDS = 85
