package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import cafe.adriel.voyager.core.stack.StackEvent
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.abs
import kotlin.math.roundToInt

class TsuzukiMotionTest {

    @Test
    fun `only opening details uses the shorter synchronized cover duration`() {
        navigatorCoverDuration(true, StackEvent.Push, false) shouldBe TsuzukiMotion.DETAILS_OPEN_MS
        navigatorCoverDuration(false, StackEvent.Push, false) shouldBe TsuzukiMotion.SHARED_MS
        navigatorCoverDuration(true, StackEvent.Pop, false) shouldBe TsuzukiMotion.SHARED_MS
        navigatorCoverDuration(false, StackEvent.Pop, false) shouldBe TsuzukiMotion.SHARED_MS
    }

    @Test
    fun `reduced motion overrides either navigation duration`() {
        navigatorCoverDuration(true, StackEvent.Push, true) shouldBe TsuzukiMotion.REDUCED_MS
        navigatorCoverDuration(false, StackEvent.Push, true) shouldBe TsuzukiMotion.REDUCED_MS
        navigatorCoverDuration(true, StackEvent.Pop, true) shouldBe TsuzukiMotion.REDUCED_MS
    }

    private val linear = Easing { it }

    @Test
    fun `stepped starts at zero and ends at one`() {
        val e = TsuzukiMotion.stepped(linear, steps = 8)
        e.transform(0f) shouldBe 0f
        e.transform(1f) shouldBe 1f
    }

    @Test
    fun `stepped quantises a linear easing into eight equal jumps`() {
        val e = TsuzukiMotion.stepped(linear, steps = 8)
        e.transform(0.01f) shouldBe 0.125f
        e.transform(0.125f) shouldBe 0.125f
        e.transform(0.13f) shouldBe 0.25f
        e.transform(0.99f) shouldBe 1f
    }

    @Test
    fun `stepped honours a custom step count`() {
        val e = TsuzukiMotion.stepped(linear, steps = 4)
        e.transform(0.1f) shouldBe 0.25f
        e.transform(0.5f) shouldBe 0.5f
        e.transform(0.51f) shouldBe 0.75f
    }

    @Test
    fun `stepped output only takes multiples of one over steps and never decreases`() {
        val e = TsuzukiMotion.stepped(TsuzukiMotion.Standard, steps = 8)
        var previous = 0f
        for (i in 0..100) {
            val v = e.transform(i / 100f)
            v shouldBeGreaterThanOrEqual previous
            v shouldBeLessThanOrEqual 1f
            val scaled = v * 8
            abs(scaled - scaled.roundToInt()) shouldBeLessThanOrEqual 1e-4f
            previous = v
        }
    }

    @Test
    fun `stepped rejects missing dots`() {
        assertThrows<IllegalArgumentException> { TsuzukiMotion.stepped(linear, steps = 0) }
        assertThrows<IllegalArgumentException> { TsuzukiMotion.stepped(linear, steps = -1) }
    }

    @Test
    fun `discrete fill clamps an overshooting curve`() {
        val easing = TsuzukiMotion.stepped(TsuzukiMotion.Spring, steps = 3)
        (0..100).forEach { easing.transform(it / 100f) shouldBeLessThanOrEqual 1f }
    }

    @Test
    fun `normal motion preserves smooth easing and requested duration`() {
        val spec = motionTween<Float>(320, TsuzukiMotion.Standard, reduced = false)
        spec.durationMillis shouldBe 320
        spec.easing shouldBe TsuzukiMotion.Standard
        spec.easing.transform(0.13f) shouldBe TsuzukiMotion.Standard.transform(0.13f)
    }

    @Test
    fun `reduced motion uses the short linear fade`() {
        val spec = motionTween<Float>(320, TsuzukiMotion.Standard, reduced = true)
        spec.durationMillis shouldBe TsuzukiMotion.REDUCED_MS
        spec.easing shouldBe LinearEasing
    }

    @Test
    fun `curves are anchored at zero and one`() {
        listOf(TsuzukiMotion.Enter, TsuzukiMotion.Exit, TsuzukiMotion.Standard, TsuzukiMotion.Spring).forEach {
            it.transform(0f) shouldBe 0f
            it.transform(1f) shouldBe 1f
        }
    }

    @Test
    fun `spring overshoots past one while standard stays within range`() {
        val peak = (1..99).maxOf { TsuzukiMotion.Spring.transform(it / 100f) }
        peak shouldBeGreaterThan 1f
        (1..99).forEach { TsuzukiMotion.Standard.transform(it / 100f) shouldBeLessThanOrEqual 1f }
    }

    @Test
    fun `no duration is longer than the theme reveal`() {
        listOf(
            TsuzukiMotion.PRESS_MS,
            TsuzukiMotion.TOGGLE_MS,
            TsuzukiMotion.SHEET_MS,
            TsuzukiMotion.STACK_MS,
            TsuzukiMotion.SHARED_MS,
            TsuzukiMotion.DETAILS_OPEN_MS,
            TsuzukiMotion.REDUCED_MS,
        ).forEach { (it <= TsuzukiMotion.THEME_MS) shouldBe true }
    }

    @Test
    fun `motion is reduced when either the system scale is zero or the app setting is on`() {
        isMotionReduced(systemScaleIsZero = false, appPreference = false) shouldBe false
        isMotionReduced(systemScaleIsZero = true, appPreference = false) shouldBe true
        isMotionReduced(systemScaleIsZero = false, appPreference = true) shouldBe true
        isMotionReduced(systemScaleIsZero = true, appPreference = true) shouldBe true
    }
}
