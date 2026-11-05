package eu.kanade.presentation.tsuzuki.details

import eu.kanade.presentation.library.components.EpubExportOptions
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.translation.model.TranslationMode

class EpubExportContainerTest {
    @Test
    fun `joined single language uses EPUB`() {
        epubExportUsesZip(EpubExportOptions()) shouldBe false
        epubExportUsesZip(EpubExportOptions(translationMode = TranslationMode.TRANSLATED)) shouldBe false
    }

    @Test
    fun `both languages use ZIP even when joining volumes`() {
        epubExportUsesZip(EpubExportOptions(translationMode = TranslationMode.BOTH)) shouldBe true
    }

    @Test
    fun `separate volumes use ZIP`() {
        epubExportUsesZip(EpubExportOptions(joinVolumes = false)) shouldBe true
    }
}
