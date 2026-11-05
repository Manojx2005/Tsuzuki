package eu.kanade.presentation.tsuzuki.customsource

import eu.kanade.tachiyomi.ui.customsource.SourceFeatures
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiSourceFeaturesTest {
    @Test
    fun `page chips follow their section`() {
        val f = SourceFeatures(hasPopular = false)
        visibleFeatureChips(f).contains(SourceFeatureChip.POPULAR_PAGES) shouldBe false
        visibleFeatureChips(f).contains(SourceFeatureChip.LATEST_PAGES) shouldBe true
    }

    @Test
    fun `url pattern hides the chapter page options`() {
        val f = SourceFeatures(chapterGenerateFromPattern = true)
        visibleFeatureChips(f).contains(SourceFeatureChip.CHAPTER_PAGE) shouldBe false
        visibleFeatureChips(f).contains(SourceFeatureChip.CHAPTER_PAGES) shouldBe false
        visibleFeatureChips(f).contains(SourceFeatureChip.CHAPTER_PATTERN) shouldBe true
    }

    @Test
    fun `toggle flips exactly one flag and round trips`() {
        SourceFeatureChip.entries.forEach { chip ->
            val base = SourceFeatures()
            val once = chip.toggled(base)
            chip.isOn(once) shouldBe !chip.isOn(base)
            chip.toggled(once) shouldBe base
        }
        SourceFeatureChip.SEARCH.toggled(SourceFeatures()) shouldBe SourceFeatures(hasSearch = false)
    }

    @Test
    fun `start needs a url and a listing`() {
        canStartSourceWizard("", SourceFeatures()) shouldBe false
        canStartSourceWizard("a.com", SourceFeatures(hasPopular = false, hasLatest = false)) shouldBe false
        canStartSourceWizard("a.com", SourceFeatures(hasPopular = false)) shouldBe true
    }
}
