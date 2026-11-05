package eu.kanade.presentation.tsuzuki.browse

import eu.kanade.tachiyomi.extension.model.Extension
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TsuzukiRepoCountsTest {

    private fun store(url: String) = ExtensionStore(
        indexUrl = "$url/index.json",
        name = url,
        badgeLabel = "",
        signingKey = "",
        contact = ExtensionStore.Contact(website = url, discord = null),
        isLegacy = false,
        extensionListUrl = null,
    )

    private fun ext(pkg: String, store: ExtensionStore, sources: Int) = Extension.Available(
        name = pkg,
        pkgName = pkg,
        versionName = "1",
        versionCode = 1,
        libVersion = 1.4,
        lang = "en",
        contentWarning = ContentWarning.SAFE,
        isNovel = false,
        sources = List(sources) { Extension.Available.Source(it.toLong(), "en", "s$it", "https://x") },
        apkUrl = "",
        iconUrl = "",
        store = store,
    )

    @Test
    fun `groups extensions and sums sources per repository`() {
        val a = store("https://a")
        val b = store("https://b")
        val counts = tsuzukiRepoCounts(listOf(ext("p1", a, 2), ext("p2", a, 3), ext("p3", b, 1)))
        assertEquals(TsuzukiRepoCounts(extensions = 2, sources = 5), counts[a.indexUrl])
        assertEquals(TsuzukiRepoCounts(extensions = 1, sources = 1), counts[b.indexUrl])
    }

    @Test
    fun `index with no listed sources still counts extensions`() {
        val a = store("https://a")
        assertEquals(TsuzukiRepoCounts(1, 0), tsuzukiRepoCounts(listOf(ext("p1", a, 0)))[a.indexUrl])
    }

    @Test
    fun `repository not loaded is absent`() {
        assertNull(tsuzukiRepoCounts(emptyList())["https://a/index.json"])
    }
}
