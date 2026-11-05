package eu.kanade.tachiyomi.extension.util

import mihon.domain.extension.model.ContentWarning

/**
 * Pure helpers for Mihon's content warnings (#3951) and "load installed regardless" option (#3952).
 */
object ContentWarningPolicy {

    val ALL: Set<ContentWarning> = ContentWarning.entries.toSet()

    /**
     * Reads the warning from an extension's manifest metadata. `tachiyomix.contentWarning` wins when
     * present (1 = mixed, 2 or more = 18+); otherwise the legacy `nsfw` flag marks the extension 18+.
     */
    fun fromMetadata(contentWarning: Int?, legacyNsfw: Int): ContentWarning = when {
        contentWarning != null -> when {
            contentWarning >= 2 -> ContentWarning.NSFW
            contentWarning == 1 -> ContentWarning.MIXED
            else -> ContentWarning.SAFE
        }
        legacyNsfw == 1 -> ContentWarning.NSFW
        else -> ContentWarning.SAFE
    }

    /** Whether an installed extension may load. With [applyToInstalled] off, installed ones always load. */
    fun canLoadInstalled(
        warning: ContentWarning,
        enabled: Set<ContentWarning>,
        applyToInstalled: Boolean,
    ): Boolean = !applyToInstalled || warning in enabled

    /** Whether an available (not yet installed) extension is listed. */
    fun isListed(warning: ContentWarning, enabled: Set<ContentWarning>): Boolean = warning in enabled

    /**
     * Default for the allowed warnings, migrated from the old "show NSFW" switch (`show_nsfw_source`,
     * default on): off keeps only Safe, anything else allows all.
     */
    fun defaultAllowed(legacyShowNsfwIsSet: Boolean, legacyShowNsfw: Boolean): Set<ContentWarning> =
        if (legacyShowNsfwIsSet && !legacyShowNsfw) setOf(ContentWarning.SAFE) else ALL
}
