package eu.kanade.presentation.tsuzuki.novel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SourcePriorityStepTest {

    @Test
    fun `steps normally away from zero`() {
        assertEquals(3, storedSourcePriority(next = 3, shown = 2, typeValue = 2))
        assertEquals(-2, storedSourcePriority(next = -2, shown = -1, typeValue = 0))
    }

    @Test
    fun `steps over zero when the type is not zero`() {
        assertEquals(-1, storedSourcePriority(next = 0, shown = 1, typeValue = 2))
        assertEquals(1, storedSourcePriority(next = 0, shown = -1, typeValue = 2))
    }

    @Test
    fun `zero is allowed when the type is zero too`() {
        assertEquals(0, storedSourcePriority(next = 0, shown = 1, typeValue = 0))
    }

    @Test
    fun `stays inside the stock range`() {
        assertEquals(5, storedSourcePriority(next = 6, shown = 5, typeValue = 0))
        assertEquals(-5, storedSourcePriority(next = -6, shown = -5, typeValue = 0))
    }
}
