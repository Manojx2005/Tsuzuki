package eu.kanade.presentation.tsuzuki.startup

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor

/*
 * Startup animation (design board "MotionSplash", Motion page).
 *
 * The system splash is blank (same colour as the Dot background), so this overlay draws
 * everything: the ribbon appears (Dot: printed row by row; Colour: dropped in with an
 * overshoot), the three "…" dots light one after another and repeat while the app is still
 * loading, the wordmark appears, then the logo flies into its spot in the real nav
 * (tablet: the rail's logo tile; phone: the HOME pill) while the overlay fades away.
 */

private const val LOGO_SIZE_DP = 150f
private const val MAX_LOADING_MS = 4_000L

private val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccel = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private val Overshoot = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** Stepped easing for the Dot theme: moves in hard jumps like a dot-matrix display. */
private fun steps(n: Int) = Easing { t -> floor(t * n).coerceAtMost(n.toFloat()) / n }

/** True when the user turned animations off (Developer options / Accessibility). */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

/**
 * @param isReady whether the app has finished loading; the "…" loop repeats until it is.
 * @param isTablet the logo lands on the rail's logo tile instead of the phone's HOME pill.
 * @param onFinished remove the overlay.
 */
@Composable
fun TsuzukiStartupOverlay(
    isReady: () -> Boolean,
    isTablet: Boolean,
    onFinished: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val dot = t.isDot
    val ready by rememberUpdatedState(isReady)

    // Animation state
    var rowsShown by remember { mutableIntStateOf(0) } // Dot: ribbon rows printed so far
    val drop = remember { Animatable(0f) } // Colour: ribbon drop 0..1
    var litDots by remember { mutableIntStateOf(0) } // "…" dots lit (0..3)
    var lettersShown by remember { mutableIntStateOf(0) }
    val wordAlpha = remember { Animatable(0f) }
    val tagAlpha = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) } // logo flight 0..1
    val fade = remember { Animatable(1f) } // overlay alpha
    var skip by remember { mutableStateOf(false) }

    val word = if (dot) "TSUZUKI" else "Tsuzuki"

    LaunchedEffect(skip) {
        if (skip) {
            launch { wordAlpha.animateTo(0f, tween(120)) }
            fade.animateTo(0f, tween(180))
            onFinished()
            return@LaunchedEffect
        }
        val start = System.currentTimeMillis()
        delay(80)
        // 1. Ribbon
        if (dot) {
            launch {
                repeat(RibbonRows) {
                    rowsShown = it + 1
                    delay(28)
                }
            }
        } else {
            launch { drop.animateTo(1f, tween(520, easing = Overshoot)) }
        }
        delay(540)
        // 2–4. The three dots, then the wordmark
        for (i in 1..3) {
            litDots = i
            if (i < 3) delay(180)
        }
        launch {
            if (dot) {
                wordAlpha.snapTo(1f)
                for (i in 1..word.length) {
                    lettersShown = i
                    delay(55)
                }
            } else {
                lettersShown = word.length
                wordAlpha.animateTo(1f, tween(500, easing = Emphasized))
            }
        }
        delay(340)
        launch { tagAlpha.animateTo(1f, tween(400, easing = Emphasized)) }
        delay(400)
        // Still loading: repeat the "…" until ready (capped).
        while (!ready() && System.currentTimeMillis() - start < MAX_LOADING_MS) {
            litDots = 0
            delay(180)
            for (i in 1..3) {
                litDots = i
                delay(180)
            }
            delay(160)
        }
        // 6. Exit: wordmark out, logo flies into the nav, overlay fades to reveal the app.
        launch { wordAlpha.animateTo(0f, tween(180, easing = EmphasizedAccel)) }
        launch { tagAlpha.animateTo(0f, tween(180, easing = EmphasizedAccel)) }
        launch {
            delay(200)
            fade.animateTo(0f, tween(320, easing = Emphasized))
        }
        exit.animateTo(1f, tween(450, easing = if (dot) steps(5) else EmphasizedAccel))
        onFinished()
    }

    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val navBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .tsuzukiBackground(t)
            .pointerInput(Unit) { detectTapGestures { skip = true } }
            .semantics { contentDescription = "Tsuzuki is starting. Tap to skip." },
    ) {
        val w = maxWidth
        val h = maxHeight
        val logoTop = h * 0.36f - (LOGO_SIZE_DP / 2).dp
        val logoLeft = (w - LOGO_SIZE_DP.dp) / 2

        // Where the logo lands, and how small it gets.
        val target: Triple<Dp, Dp, Float> = if (isTablet) {
            // Rail logo tile: 48 dp tile centred in the 96 dp rail, 20 dp below the status bar.
            Triple(48.dp, statusTop + 20.dp + 24.dp, 48f / LOGO_SIZE_DP)
        } else {
            // HOME pill: left end of the floating nav bar.
            Triple(16.dp + 7.dp + (if (dot) 33.dp else 54.dp), h - navBottom - 16.dp - 31.dp, 0.2f)
        }
        val startCx = logoLeft + (LOGO_SIZE_DP / 2).dp
        val startCy = logoTop + (LOGO_SIZE_DP / 2).dp

        Box(
            Modifier
                .offset(x = logoLeft, y = logoTop)
                .size(LOGO_SIZE_DP.dp)
                .graphicsLayer {
                    val e = exit.value
                    translationX = ((target.first - startCx) * e).toPx()
                    translationY = ((target.second - startCy) * e).toPx()
                    val s = 1f - (1f - target.third) * e
                    scaleX = s
                    scaleY = s
                },
        ) {
            if (dot) {
                DotRibbon(rowsShown = rowsShown, litDots = litDots)
            } else {
                ColourRibbon(drop = { drop.value }, litDots = litDots, accent = t.accent, ink = t.onAccent)
            }
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = logoTop + LOGO_SIZE_DP.dp + 28.dp)
                .graphicsLayer { alpha = wordAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row {
                word.forEachIndexed { i, c ->
                    Text(
                        text = c.toString(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = if (dot) 44.sp else 54.sp,
                            lineHeight = if (dot) 44.sp else 54.sp,
                            letterSpacing = if (dot) 1.sp else 0.sp,
                        ),
                        color = Color.White.copy(alpha = if (i < lettersShown) 1f else 0f),
                    )
                }
            }
            Text(
                text = if (dot) "続き · READ WHAT HAPPENS NEXT" else "続き · read what happens next",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (dot) 12.sp else 14.sp,
                    letterSpacing = if (dot) 1.2.sp else 0.sp,
                    fontWeight = if (dot) FontWeight.Normal else FontWeight.Bold,
                ),
                color = if (dot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f),
                modifier = Modifier.graphicsLayer { alpha = tagAlpha.value },
            )
        }
    }
}

// ---------------------------------------------------------------- logo drawing (viewport 108)

private data class RibbonDot(val x: Float, val y: Float, val row: Int, val ellipsis: Int)

/** Dot positions of the dot-matrix ribbon, same grid as the Logo board and ic_tsuzuki_dots. */
private val RibbonDots: List<RibbonDot> = buildList {
    var row = 0
    var y = 3f
    while (y < 108f) {
        var x = 2f
        var any = false
        while (x < 108f) {
            val inRib = x in 41f..67f && y >= 17f && y <= 77f + 11f * abs(x - 54f) / 14f - 2f
            if (inRib) {
                val ell = if (abs(x - 54f) < 4f) listOf(36f, 49f, 62f).indexOfFirst { abs(y - it) < 4f } else -1
                add(RibbonDot(x, y, row, ell))
                any = true
            }
            x += 6.5f
        }
        if (any) row++
        y += 6.5f
    }
}

private val RibbonRows = RibbonDots.maxOf { it.row } + 1

private val DotRed = Color(0xFFFF2D2D)
private val DotOff = Color(0xFF2E2E2E)

@Composable
private fun DotRibbon(rowsShown: Int, litDots: Int) {
    Canvas(Modifier.fillMaxSize()) {
        val k = size.width / 108f
        RibbonDots.forEach { d ->
            if (d.row >= rowsShown) return@forEach
            val color = when {
                d.ellipsis < 0 -> DotRed
                d.ellipsis < litDots -> Color.White
                else -> DotOff
            }
            drawCircle(color, radius = 2.5f * k, center = Offset(d.x * k, d.y * k))
        }
    }
}

@Composable
private fun ColourRibbon(drop: () -> Float, litDots: Int, accent: Color, ink: Color) {
    val ribbon = remember {
        // M44 16h20a4 4 0 0 1 4 4v70l-14-11-14 11V20a4 4 0 0 1 4-4z
        Path().apply {
            moveTo(44f, 16f)
            lineTo(64f, 16f)
            quadraticTo(68f, 16f, 68f, 20f)
            lineTo(68f, 90f)
            lineTo(54f, 79f)
            lineTo(40f, 90f)
            lineTo(40f, 20f)
            quadraticTo(40f, 16f, 44f, 16f)
            close()
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val drop = drop()
        val k = size.width / 108f
        scale(k, k, pivot = Offset.Zero) {
            // Page behind the ribbon
            rotate(-9f, pivot = Offset(53f, 53f)) {
                drawRoundRect(
                    color = Color(0xFFF6F2EA).copy(alpha = 0.16f * drop.coerceIn(0f, 1f)),
                    topLeft = Offset(30f, 22f),
                    size = Size(46f, 62f),
                    cornerRadius = CornerRadius(5f, 5f),
                )
            }
            // Ribbon drops from above, stretching into place.
            translate(top = -60f * (1f - drop)) {
                scale(1f, 0.6f + 0.4f * drop, pivot = Offset(54f, 16f)) {
                    drawPath(ribbon, accent.copy(alpha = drop.coerceIn(0f, 1f)))
                }
            }
            listOf(36f, 49f, 62f).forEachIndexed { i, cy ->
                if (i < litDots) drawCircle(ink, radius = 4.4f, center = Offset(54f, cy))
            }
        }
    }
}
