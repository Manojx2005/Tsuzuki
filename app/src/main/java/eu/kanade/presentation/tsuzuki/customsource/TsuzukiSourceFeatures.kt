package eu.kanade.presentation.tsuzuki.customsource

import eu.kanade.tachiyomi.ui.customsource.SourceFeatures

/** The "What does the site have?" choices, one per [SourceFeatures] flag. */
internal enum class SourceFeatureChip {
    POPULAR,
    POPULAR_PAGES,
    LATEST,
    LATEST_PAGES,
    SEARCH,
    SEARCH_PAGES,
    CHAPTER_PATTERN,
    CHAPTER_PAGE,
    CHAPTER_PAGES,
}

/** Same visibility rules as the stock checklist: pagination follows its section, page options hide with the URL pattern. */
internal fun SourceFeatureChip.isVisible(f: SourceFeatures): Boolean = when (this) {
    SourceFeatureChip.POPULAR_PAGES -> f.hasPopular
    SourceFeatureChip.LATEST_PAGES -> f.hasLatest
    SourceFeatureChip.SEARCH_PAGES -> f.hasSearch
    SourceFeatureChip.CHAPTER_PAGE, SourceFeatureChip.CHAPTER_PAGES -> !f.chapterGenerateFromPattern
    else -> true
}

internal fun SourceFeatureChip.isOn(f: SourceFeatures): Boolean = when (this) {
    SourceFeatureChip.POPULAR -> f.hasPopular
    SourceFeatureChip.POPULAR_PAGES -> f.popularPagination
    SourceFeatureChip.LATEST -> f.hasLatest
    SourceFeatureChip.LATEST_PAGES -> f.latestPagination
    SourceFeatureChip.SEARCH -> f.hasSearch
    SourceFeatureChip.SEARCH_PAGES -> f.searchPagination
    SourceFeatureChip.CHAPTER_PATTERN -> f.chapterGenerateFromPattern
    SourceFeatureChip.CHAPTER_PAGE -> f.chapterListSeparatePage
    SourceFeatureChip.CHAPTER_PAGES -> f.chapterListPagination
}

internal fun SourceFeatureChip.toggled(f: SourceFeatures): SourceFeatures {
    val on = !isOn(f)
    return when (this) {
        SourceFeatureChip.POPULAR -> f.copy(hasPopular = on)
        SourceFeatureChip.POPULAR_PAGES -> f.copy(popularPagination = on)
        SourceFeatureChip.LATEST -> f.copy(hasLatest = on)
        SourceFeatureChip.LATEST_PAGES -> f.copy(latestPagination = on)
        SourceFeatureChip.SEARCH -> f.copy(hasSearch = on)
        SourceFeatureChip.SEARCH_PAGES -> f.copy(searchPagination = on)
        SourceFeatureChip.CHAPTER_PATTERN -> f.copy(chapterGenerateFromPattern = on)
        SourceFeatureChip.CHAPTER_PAGE -> f.copy(chapterListSeparatePage = on)
        SourceFeatureChip.CHAPTER_PAGES -> f.copy(chapterListPagination = on)
    }
}

internal fun visibleFeatureChips(f: SourceFeatures): List<SourceFeatureChip> =
    SourceFeatureChip.entries.filter { it.isVisible(f) }

/** A source needs a URL and at least one listing (Popular or Latest) to be browsable. */
internal fun canStartSourceWizard(url: String, f: SourceFeatures): Boolean =
    url.isNotBlank() && (f.hasPopular || f.hasLatest)
