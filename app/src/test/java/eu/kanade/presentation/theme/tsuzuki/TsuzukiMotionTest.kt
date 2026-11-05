package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.animation.core.Easing
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.roundToInt

class TsuzukiMotionTest {

    private val linear = Easing { it }

    @Test
    fun `stepped starts at zero and ends at one`() {
        val e = TsuzukiMotion.stepped(linear)
        e.transform(0f) shouldBe 0f
        e.transform(1f) shouldBe 1f
    }

    @Test
    fun `stepped quantises a linear easing into eight equal jumps`() {
        val e = TsuzukiMotion.stepped(linear)
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
        val e = TsuzukiMotion.stepped(TsuzukiMotion.Standard)
        var previous = 0f
        for (i in 0..100) {
            val v = e.transform(i / 100f)
            v shouldBeGreaterThanOrEqual previous
            v shouldBeLessThanOrEqual 1f
            val scaled = v * TsuzukiMotion.DOT_STEPS
            abs(scaled - scaled.roundToInt()) shouldBeLessThanOrEqual 1e-4f
            previous = v
        }
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
