package eu.kanade.presentation.tsuzuki.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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

/** Temporary Home selection; never changes reading history or the Continue chapter. */
val LocalShelfPaletteCover = androidx.compose.runtime.staticCompositionLocalOf<(Any?) -> Unit> { {} }

private data class StackSlot(val x: Float, val y: Float, val rotation: Float, val scale: Float, val alpha: Float)

// MotionStack's slots, scaled to the existing Home cover width (142 dp versus the demo's 220 dp).
private val slots = listOf(
    StackSlot(2f, 0f, -3f, 1f, 1f),
    StackSlot(18f, 8f, 4f, .94f, .75f),
    StackSlot(30f, 16f, 9f, .88f, .5f),
    StackSlot(40f, 23f, 13f, .82f, .3f),
)

@Composable
fun TsuzukiCoverStack(books: List<HomeBook>, selectedIndex: Int, onAdvance: () -> Unit) {
    val reduced = rememberReduceMotion()
    val spec = tsuzukiTween<Float>(TsuzukiMotion.STACK_MS)
    val progress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var outgoing by remember { mutableStateOf<Long?>(null) }
    var busy by remember { mutableStateOf(false) }
    val fadeClock = remember {
        object : MotionDurationScale {
            override val scaleFactor = 1f
        }
    }
    Box(
        Modifier.width(162.dp).fillMaxHeight().tsuzukiClickable(
            role = Role.Button,
            enabled = books.size > 1,
            onClickLabel = stringResource(MR.strings.tsz_show_next_book),
        ) {
            // The board ignores a second shuffle until the current 400 ms deal completes.
            if (!busy) {
                busy = true
                scope.launch {
                    try {
                        outgoing = books[selectedIndex].mangaId
                        progress.snapTo(0f)
                        onAdvance()
                        if (reduced) {
                            withContext(fadeClock) { progress.animateTo(1f, tween(TsuzukiMotion.REDUCED_MS)) }
                        } else {
                            progress.animateTo(1f, spec)
                        }
                    } finally {
                        outgoing = null
                        busy = false
                    }
                }
            }
        },
    ) {
        books.forEachIndexed { index, book ->
            val slot = (index - selectedIndex + books.size) % books.size
            val target = slots[slot]
            val previous = slots[(slot + 1) % books.size]
            val flying = outgoing == book.mangaId
            MangaCover.Book(
                data = book.cover,
                generatedTitle = book.title,
                contentDescription = book.title,
                colorFilter = eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                modifier = Modifier.width(142.dp)
                    .zIndex(if (flying && progress.value < .65f) 10f else (books.size - slot).toFloat())
                    .graphicsLayer {
                        val p = progress.value
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, .5f)
                        if (reduced) {
                            translationX = target.x.dp.toPx()
                            translationY = target.y.dp.toPx()
                            rotationZ = target.rotation
                            scaleX = target.scale
                            scaleY = target.scale
                            alpha = target.alpha * p
                        } else if (flying && p < .65f) {
                            val fly = p / .65f
                            translationX = (slots[0].x + 271f * fly).dp.toPx()
                            translationY = (-26f * fly).dp.toPx()
                            rotationZ = -3f + 25f * fly
                            scaleX = 1f - .08f * fly
                            scaleY = scaleX
                            alpha = 1f - fly
                        } else {
                            val moving = outgoing != null && !flying
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
