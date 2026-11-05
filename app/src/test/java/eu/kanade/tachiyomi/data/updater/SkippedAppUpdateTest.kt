package eu.kanade.tachiyomi.data.updater

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkippedAppUpdateTest {

    @Test
    fun `nothing skipped never hides an update`() {
        assertFalse(isAppUpdateSkipped("v0.2.0", ""))
    }

    @Test
    fun `the skipped version is hidden`() {
        assertTrue(isAppUpdateSkipped("v0.2.0", "v0.2.0"))
    }

    @Test
    fun `a newer version is announced again`() {
        assertFalse(isAppUpdateSkipped("v0.2.1", "v0.2.0"))
        assertFalse(isAppUpdateSkipped("v0.3.0", "v0.2.0"))
    }

    @Test
    fun `an older or different version is not hidden`() {
        assertFalse(isAppUpdateSkipped("v0.1.9", "v0.2.0"))
        assertFalse(isAppUpdateSkipped("v0.2.0-preview", "v0.2.0"))
    }
}
