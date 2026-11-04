package eu.kanade.presentation.tsuzuki.browse

import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel.Header
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

class ExtensionLanguageFilterTest {

    private val updates = Header.Resource(MR.strings.ext_updates_pending)
    private val installed = Header.Resource(MR.strings.ext_installed)
    private val items = linkedMapOf<Header, List<eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel.Item>>(
        updates to emptyList(),
        installed to emptyList(),
        Header.Text("English") to emptyList(),
        Header.Text("日本語") to emptyList(),
    )

    @Test
    fun `languages are the text headers in list order`() {
        assertEquals(listOf("English", "日本語"), tsuzukiExtensionLanguages(items))
    }

    @Test
    fun `no language keeps every group`() {
        assertEquals(items.keys.toList(), tsuzukiExtensionsByLanguage(items, null).keys.toList())
    }

    @Test
    fun `a language keeps the stock sections and only its own group`() {
        assertEquals(
            listOf(updates, installed, Header.Text("日本語")),
            tsuzukiExtensionsByLanguage(items, "日本語").keys.toList(),
        )
    }
}
