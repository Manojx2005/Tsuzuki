package eu.kanade.presentation.tsuzuki.category

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CategoryUpdateToggleTest {

    @Test
    fun `empty include set means every shelf, exclude wins`() {
        assertTrue(includedInUpdates("1", emptySet(), emptySet()))
        assertFalse(includedInUpdates("1", emptySet(), setOf("1")))
        assertFalse(includedInUpdates("1", setOf("2"), emptySet()))
        assertFalse(includedInUpdates("1", setOf("1"), setOf("1")))
    }

    @Test
    fun `switching off the only included shelf doesn't turn every other shelf on`() {
        val (include, exclude) = toggledUpdateSets("1", on = false, include = setOf("1"), exclude = emptySet())
        assertFalse(includedInUpdates("1", include, exclude))
        assertFalse(includedInUpdates("2", include, exclude))
    }

    @Test
    fun `switching on with no include list just removes the exclusion`() {
        val (include, exclude) = toggledUpdateSets("1", on = true, include = emptySet(), exclude = setOf("1", "3"))
        assertEquals(emptySet<String>(), include)
        assertEquals(setOf("3"), exclude)
        assertTrue(includedInUpdates("1", include, exclude))
    }

    @Test
    fun `switching on with an include list adds the shelf to it`() {
        val (include, exclude) = toggledUpdateSets("1", on = true, include = setOf("2"), exclude = setOf("1"))
        assertTrue(includedInUpdates("1", include, exclude))
        assertTrue(includedInUpdates("2", include, exclude))
        assertFalse(includedInUpdates("3", include, exclude))
    }

    @Test
    fun `off then on returns the shelf to updates`() {
        val off = toggledUpdateSets("1", on = false, include = emptySet(), exclude = emptySet())
        val on = toggledUpdateSets("1", on = true, include = off.first, exclude = off.second)
        assertTrue(includedInUpdates("1", on.first, on.second))
    }
}
