package eu.kanade.tachiyomi.extension.util

import mihon.domain.extension.model.ContentWarning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentWarningPolicyTest {

    @Test
    fun `metadata warning wins over legacy flag`() {
        assertEquals(ContentWarning.SAFE, ContentWarningPolicy.fromMetadata(0, legacyNsfw = 1))
        assertEquals(ContentWarning.MIXED, ContentWarningPolicy.fromMetadata(1, legacyNsfw = 0))
        assertEquals(ContentWarning.NSFW, ContentWarningPolicy.fromMetadata(2, legacyNsfw = 0))
        assertEquals(ContentWarning.NSFW, ContentWarningPolicy.fromMetadata(5, legacyNsfw = 0))
    }

    @Test
    fun `legacy nsfw flag is used only when the warning is absent`() {
        assertEquals(ContentWarning.NSFW, ContentWarningPolicy.fromMetadata(null, legacyNsfw = 1))
        assertEquals(ContentWarning.SAFE, ContentWarningPolicy.fromMetadata(null, legacyNsfw = 0))
    }

    @Test
    fun `installed extensions load regardless when the option is off`() {
        val onlySafe = setOf(ContentWarning.SAFE)
        assertFalse(ContentWarningPolicy.canLoadInstalled(ContentWarning.NSFW, onlySafe, applyToInstalled = true))
        assertFalse(ContentWarningPolicy.canLoadInstalled(ContentWarning.MIXED, onlySafe, applyToInstalled = true))
        assertTrue(ContentWarningPolicy.canLoadInstalled(ContentWarning.SAFE, onlySafe, applyToInstalled = true))
        assertTrue(ContentWarningPolicy.canLoadInstalled(ContentWarning.NSFW, onlySafe, applyToInstalled = false))
        assertTrue(ContentWarningPolicy.canLoadInstalled(ContentWarning.NSFW, emptySet(), applyToInstalled = false))
    }

    @Test
    fun `available extensions are listed only for allowed warnings`() {
        val safeAndMixed = setOf(ContentWarning.SAFE, ContentWarning.MIXED)
        assertTrue(ContentWarningPolicy.isListed(ContentWarning.MIXED, safeAndMixed))
        assertFalse(ContentWarningPolicy.isListed(ContentWarning.NSFW, safeAndMixed))
        assertFalse(ContentWarningPolicy.isListed(ContentWarning.SAFE, emptySet()))
    }

    @Test
    fun `old show nsfw switch migrates to the new default`() {
        // Never set (default was on): everything allowed.
        assertEquals(ContentWarningPolicy.ALL, ContentWarningPolicy.defaultAllowed(false, true))
        // Explicitly on: everything allowed.
        assertEquals(ContentWarningPolicy.ALL, ContentWarningPolicy.defaultAllowed(true, true))
        // Explicitly off: only safe extensions, as before.
        assertEquals(setOf(ContentWarning.SAFE), ContentWarningPolicy.defaultAllowed(true, false))
    }
}
