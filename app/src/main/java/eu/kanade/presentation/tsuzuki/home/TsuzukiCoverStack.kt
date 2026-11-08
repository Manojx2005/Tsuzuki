package eu.kanade.presentation.tsuzuki.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMotion
import eu.kanade.presentation.theme.tsuzuki.rememberReduceMotion
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiTween
import eu.kanade.tachiyomi.ui.tsuzuki.home.HomeBook
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs

/** Temporary Home selection; never changes reading history or the Continue chapter. */
val LocalShelfPaletteCover = androidx.compose.runtime.staticCompositionLocalOf<(Any?) -> Unit> { {} }

private data class StackSlot(val x: Float, val y: Float, val rotation: Float, val scale: Float, val alpha: Float)

private data class StackShuffle(val flyingBookId: Long, val reversed: Boolean)

// MotionStack's slots, scaled to the existing Home cover width (142 dp versus the demo's 220 dp).
private val slots = listOf(
    StackSlot(2f, 0f, -3f, 1f, 1f),
    StackSlot(18f, 8f, 4f, .94f, .75f),
    StackSlot(30f, 16f, 9f, .88f, .5f),
    StackSlot(40f, 23f, 13f, .82f, .3f),
)

@Composable
fun TsuzukiCoverStack(books: List<HomeBook>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val reduced = rememberReduceMotion()
    val spec = tsuzukiTween<Float>(TsuzukiMotion.STACK_MS)
    val progress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var shuffle by remember { mutableStateOf<StackShuffle?>(null) }
    var busy by remember { mutableStateOf(false) }
    val fadeClock = remember {
        object : MotionDurationScale {
            override val scaleFactor = 1f
        }
    }
    val canShuffle = books.size > 1
    val onShuffle: (Int) -> Unit = { step ->
        // The board ignores a second shuffle until the current 400 ms deal completes.
        if (canShuffle && !busy) {
            busy = true
            val next = (selectedIndex + step).mod(books.size)
            val reversed = step < 0
            val flyingBook = books[if (reversed) next else selectedIndex]
            scope.launch {
                try {
                    progress.snapTo(0f)
                    shuffle = StackShuffle(flyingBook.mangaId, reversed)
                    onSelect(next)
                    if (reduced) {
                        withContext(fadeClock) { progress.animateTo(1f, tween(TsuzukiMotion.REDUCED_MS)) }
                    } else {
                        progress.animateTo(1f, spec)
                    }
                } finally {
                    shuffle = null
                    busy = false
                }
            }
        }
    }
    val latestShuffle by rememberUpdatedState(onShuffle)
    val activeShuffle = shuffle
    val reversed = activeShuffle?.reversed == true
    Box(
        Modifier.width(162.dp).fillMaxHeight()
            .pointerInput(canShuffle) {
                if (!canShuffle) return@pointerInput
                var drag = 0f
                val minimumTravel = 24.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        if (abs(drag) >= minimumTravel) latestShuffle(if (drag < 0f) 1 else -1)
                    },
                    onHorizontalDrag = { _, amount -> drag += amount },
                )
            }
            .tsuzukiClickable(
                role = Role.Button,
                enabled = canShuffle,
                onClickLabel = stringResource(MR.strings.tsz_show_next_book),
            ) { onShuffle(1) },
    ) {
        books.forEachIndexed { index, book ->
            val slot = (index - selectedIndex + books.size) % books.size
            val originalSlot = (slot + if (reversed) -1 else 1).mod(books.size)
            // Run the same deal backwards so the previous book returns from the back of the stack.
            val endSlot = if (reversed) originalSlot else slot
            val target = slots[endSlot]
            val previous = slots[if (reversed) slot else originalSlot]
            val flying = activeShuffle?.flyingBookId == book.mangaId
            fun motionProgress() = if (reversed) 1f - progress.value else progress.value
            MangaCover.Book(
                data = book.cover,
                generatedTitle = book.title,
                contentDescription = book.title,
                colorFilter = eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                modifier = Modifier.width(142.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            val depth = when {
                                reduced -> books.size - slot
                                flying && motionProgress() < .65f -> 10
                                else -> books.size - endSlot
                            }.toFloat()
                            placeable.placeRelative(0, 0, zIndex = depth)
                        }
                    }
                    .graphicsLayer {
                        val p = motionProgress()
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, .5f)
                        if (reduced) {
                            val resting = slots[slot]
                            translationX = resting.x.dp.toPx()
                            translationY = resting.y.dp.toPx()
                            rotationZ = resting.rotation
                            scaleX = resting.scale
                            scaleY = resting.scale
                            alpha = resting.alpha * progress.value
                        } else if (flying && p < .65f) {
                            val fly = p / .65f
                            translationX = (slots[0].x + 271f * fly).dp.toPx()
                            translationY = (-26f * fly).dp.toPx()
                            rotationZ = -3f + 25f * fly
                            scaleX = 1f - .08f * fly
                            scaleY = scaleX
                            alpha = 1f - fly
                        } else {
                            val moving = activeShuffle != null && !flying
                            fun mix(a: Float, b: Float) = if (moving) a + (b - a) * p else b
                            translationX = mix(previous.x, target.x).dp.toPx()
                            translationY = mix(previous.y, target.y).dp.toPx()
                            rotationZ = mix(previous.rotation, target.rotation)
                            scaleX = mix(previous.scale, target.scale)
                            scaleY = scaleX
                            alpha =
                                if (flying) target.alpha * ((p - .65f) / .35f) else mix(previous.alpha, target.alpha)
                        }
                    },
            )
        }
    }
}
