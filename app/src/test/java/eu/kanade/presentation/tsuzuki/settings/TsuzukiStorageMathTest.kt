package eu.kanade.presentation.tsuzuki.settings

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

class TsuzukiStorageMathTest {

    // A tiny fake file tree: name -> children (null = file) and sizes.
    private val tree = mapOf(
        "root" to listOf("a", "dir"),
        "dir" to listOf("b", "c"),
    )
    private val sizes = mapOf("a" to 10L, "b" to 200L, "c" to 3000L)

    private fun measure(
        deadline: Long = Long.MAX_VALUE,
        keepGoing: () -> Boolean = {
            true
        },
        now: () -> Long = { 0L },
    ) =
        measureTree("root", { tree[it] }, { sizes[it] ?: 0L }, now, deadline, keepGoing)

    @Test
    fun `measureTree sums every file under the root`() {
        measure() shouldBe TreeMeasure(3210L, 3, partial = false)
    }

    @Test
    fun `measureTree of a plain file root counts nothing`() {
        measureTree("a", { null }, { 99L }, { 0L }, Long.MAX_VALUE) shouldBe TreeMeasure(0L, 0, partial = false)
    }

    @Test
    fun `measureTree stops at the deadline and reports partial`() {
        var t = 0L
        val result = measure(deadline = 2L, now = { t++ })
        result.partial shouldBe true
        (result.bytes < 3210L) shouldBe true
    }

    @Test
    fun `measureTree stops when cancelled`() {
        measure(keepGoing = { false }) shouldBe TreeMeasure(0L, 0, partial = true)
    }

    @Test
    fun `measureTree ignores negative lengths`() {
        measureTree("r", { if (it == "r") listOf("x") else null }, { -5L }, { 0L }, Long.MAX_VALUE).bytes shouldBe 0L
    }

    @Test
    fun `fractions normalise and treat unmeasured as zero`() {
        storageFractions(listOf(50L, 50L, null, 0L)) shouldBe listOf(0.5f, 0.5f, 0f, 0f)
        storageFractions(listOf(null, null)) shouldBe listOf(0f, 0f)
        storageFractions(listOf(0L, 0L)) shouldBe listOf(0f, 0f)
    }

    @Test
    fun `breakdown totals and completeness`() {
        val partial = StorageBreakdown(downloads = 5L, covers = 7L)
        partial.total shouldBe 12L
        partial.isComplete shouldBe false
        StorageBreakdown(1L, 2L, 3L, 4L).isComplete shouldBe true
        StorageBreakdown(1L, 2L, 3L, 4L).total shouldBe 10L
    }

    @Test
    fun `sizes format in decimal units`() {
        val us = Locale.US
        formatStorageSize(0, us) shouldBe "0 B"
        formatStorageSize(999, us) shouldBe "999 B"
        formatStorageSize(1_500, us) shouldBe "1.5 KB"
        formatStorageSize(412_000_000, us) shouldBe "412 MB"
        formatStorageSize(1_800_000_000, us) shouldBe "1.8 GB"
        formatStorageSize(-5, us) shouldBe "0 B"
    }

    @Test
    fun `counts format like the board`() {
        val us = Locale.US
        formatCompactCount(1284, us) shouldBe "1,284"
        formatCompactCount(412_000, us) shouldBe "412k"
        formatCompactCount(1_300_000, us) shouldBe "1.3M"
        formatCompactCount(0, us) shouldBe "0"
    }
}
