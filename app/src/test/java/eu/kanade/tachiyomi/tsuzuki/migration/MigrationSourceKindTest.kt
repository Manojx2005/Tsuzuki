package eu.kanade.tachiyomi.tsuzuki.migration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MigrationSourceKindTest {

    @Test
    fun `a novel migrates only to novel sources`() {
        assertTrue(isMigrationTarget(sourceIsNovel = true, bookKinds = setOf(true)))
        assertFalse(isMigrationTarget(sourceIsNovel = false, bookKinds = setOf(true)))
    }

    @Test
    fun `a manga migrates only to manga sources`() {
        assertTrue(isMigrationTarget(sourceIsNovel = false, bookKinds = setOf(false)))
        assertFalse(isMigrationTarget(sourceIsNovel = true, bookKinds = setOf(false)))
    }

    @Test
    fun `a mixed or unknown selection keeps both kinds`() {
        assertTrue(isMigrationTarget(sourceIsNovel = true, bookKinds = setOf(true, false)))
        assertTrue(isMigrationTarget(sourceIsNovel = false, bookKinds = setOf(true, false)))
        assertTrue(isMigrationTarget(sourceIsNovel = true, bookKinds = emptySet()))
        assertTrue(isMigrationTarget(sourceIsNovel = false, bookKinds = emptySet()))
    }

    @Test
    fun `saving one kind keeps the other kind's saved sources`() {
        // Saved: manga 1, novel 10, manga 2, novel 11. The novel screen shows 10, 11, 12 and picks 12 then 10.
        val merged = mergeMigrationSources(
            saved = listOf(1L, 10L, 2L, 11L),
            shown = setOf(10L, 11L, 12L),
            selected = listOf(12L, 10L),
        )
        assertEquals(listOf(12L, 10L, 1L, 2L), merged)
    }

    @Test
    fun `clearing every shown source keeps the rest`() {
        assertEquals(
            listOf(1L, 2L),
            mergeMigrationSources(saved = listOf(1L, 10L, 2L), shown = setOf(10L), selected = emptyList()),
        )
    }
}
