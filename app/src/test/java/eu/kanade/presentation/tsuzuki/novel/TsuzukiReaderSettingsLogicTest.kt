package eu.kanade.presentation.tsuzuki.novel

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

class TsuzukiReaderSettingsLogicTest {
    @Test
    fun `tabs expose every preference group exactly once`() {
        NovelSettingsTab.entries.flatMap { it.groups.toList() }.groupingBy { it }.eachCount() shouldBe
            ReaderPreferenceGroup.entries.associateWith { 1 }
        NovelSettingsTab.PAGE.groups.toList() shouldBe listOf(
            ReaderPreferenceGroup.COLOURS,
            ReaderPreferenceGroup.PAGE_BASICS,
            ReaderPreferenceGroup.PAGE_OPTIONS,
        )
    }

    @Test
    fun `reader metrics use stable units and local decimal separators`() {
        formatReaderSize(13) shouldBe "13 sp"
        formatReaderSpacing(11, Locale.US) shouldBe "1.1×"
        formatReaderSpacing(20, Locale.GERMANY) shouldBe "2,0×"
    }

    @Test
    fun `linking asymmetric margins starts from their rounded midpoint`() {
        linkedReaderMargin(14, 26) shouldBe 20
        linkedReaderMargin(14, 15) shouldBe 15
        linkedReaderMargin(0, 300) shouldBe 150
        formatReaderMargins(0, 300) shouldBe "0 / 300 dp"
        formatReaderMargins(26, 26) shouldBe "26 dp"
    }
}
