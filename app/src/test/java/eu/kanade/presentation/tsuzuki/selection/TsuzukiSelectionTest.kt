package eu.kanade.presentation.tsuzuki.selection

import androidx.compose.ui.graphics.compositeOver
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.theme.tsuzuki.ColourTokens
import eu.kanade.presentation.theme.tsuzuki.DotTokens
import eu.kanade.presentation.theme.tsuzuki.textContrast
import eu.kanade.presentation.tsuzuki.history.historySelectionRange
import eu.kanade.presentation.tsuzuki.settings.splitAdvancedPreferences
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiSelectionTest {
    @Test
    fun `selection leaves room for large type`() {
        selectionActionSlots(300f, 1f) shouldBe 4
        selectionActionSlots(328f, 1f) shouldBe 5
        selectionActionSlots(328f, 2f) shouldBe 2
    }

    @Test
    fun `ranges follow the visible history order in either direction`() {
        val order = listOf(40L, 12L, 6L, 88L)
        historySelectionRange(order, 88L, 12L) shouldBe listOf(12L, 6L, 88L)
        historySelectionRange(order, 40L, 6L) shouldBe listOf(40L, 12L, 6L)
        historySelectionRange(order, 999L, 6L) shouldBe listOf(6L)
        historySelectionRange(order, 40L, 999L) shouldBe emptyList()
    }

    @Test
    fun `advanced preferences keep the original title discoverable`() {
        val common = Preference.PreferenceItem.TextPreference("Common")
        val rare = Preference.PreferenceItem.TextPreference("Rare")
        val group = Preference.PreferenceGroup(title = "Group", preferenceItems = listOf(common, rare))
        val split = splitAdvancedPreferences(listOf(group), emptySet(), setOf("Rare"))
        split.primary shouldBe listOf(group.copy(preferenceItems = listOf(common)))
        split.advanced shouldBe listOf(group.copy(preferenceItems = listOf(rare)))
        split.containsHighlight("Rare") shouldBe true
        split.containsHighlight("Common") shouldBe false
    }

    @Test
    fun `muted text clears 4 point 5 on tile and page in both themes`() {
        for (tokens in listOf(DotTokens, ColourTokens)) {
            (textContrast(tokens.mutedText, tokens.background) >= 4.5f) shouldBe true
            (textContrast(tokens.mutedText, tokens.tile.compositeOver(tokens.background)) >= 4.5f) shouldBe true
        }
    }
}
