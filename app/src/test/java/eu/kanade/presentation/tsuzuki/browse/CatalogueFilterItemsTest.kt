package eu.kanade.presentation.tsuzuki.browse

import eu.kanade.tachiyomi.source.model.Filter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CatalogueFilterItemsTest {
    @Test
    fun `tri-state cycles through ignore include and exclude`() {
        nextCatalogueTriState(Filter.TriState.STATE_IGNORE) shouldBe Filter.TriState.STATE_INCLUDE
        nextCatalogueTriState(Filter.TriState.STATE_INCLUDE) shouldBe Filter.TriState.STATE_EXCLUDE
        nextCatalogueTriState(Filter.TriState.STATE_EXCLUDE) shouldBe Filter.TriState.STATE_IGNORE
    }

    @Test
    fun `unrecognized tri-state can be replaced with a valid selection`() {
        nextCatalogueTriState(-1) shouldBe Filter.TriState.STATE_INCLUDE
    }

    @Test
    fun `first sort selection defaults to ascending`() {
        nextCatalogueSort(null, 2) shouldBe Filter.Sort.Selection(2, ascending = true)
    }

    @Test
    fun `reselecting a sort reverses its direction`() {
        nextCatalogueSort(Filter.Sort.Selection(2, ascending = true), 2) shouldBe
            Filter.Sort.Selection(2, ascending = false)
        nextCatalogueSort(Filter.Sort.Selection(2, ascending = false), 2) shouldBe
            Filter.Sort.Selection(2, ascending = true)
    }

    @Test
    fun `choosing another sort preserves the current direction`() {
        nextCatalogueSort(Filter.Sort.Selection(0, ascending = false), 2) shouldBe
            Filter.Sort.Selection(2, ascending = false)
    }
}
