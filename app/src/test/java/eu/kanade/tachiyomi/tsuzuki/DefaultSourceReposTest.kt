package eu.kanade.tachiyomi.tsuzuki

import eu.kanade.tachiyomi.data.updater.GITHUB_REPO
import eu.kanade.tachiyomi.jsplugin.JsPluginManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class DefaultSourceReposTest {

    private class Fixture(lnreaderSeeded: Boolean, keiyoushiSeeded: Boolean, insertResult: Result<Unit>) {
        val lnreader = mockk<Preference<Boolean>>(relaxed = true)
        val keiyoushi = mockk<Preference<Boolean>>(relaxed = true)
        val store = mockk<PreferenceStore> {
            every { getBoolean("tsuzuki_seeded_repo_lnreader", false) } returns lnreader
            every { getBoolean("tsuzuki_seeded_repo_keiyoushi", false) } returns keiyoushi
        }
        val plugins = mockk<JsPluginManager>(relaxed = true)
        val stores = mockk<ExtensionStoreRepository>()

        init {
            every { lnreader.get() } returns lnreaderSeeded
            every { keiyoushi.get() } returns keiyoushiSeeded
            coEvery { stores.insert(any(), any()) } returns insertResult
        }

        fun run(enabled: Boolean = true) = runBlocking { DefaultSourceRepos.seed(store, plugins, stores, enabled) }
    }

    @Test
    fun `shipped builds add no repos and leave the seeded flags alone`() {
        val f = Fixture(lnreaderSeeded = false, keiyoushiSeeded = false, insertResult = Result.success(Unit))
        f.run(enabled = false)
        verify(exactly = 0) { f.plugins.addRepository(any(), any()) }
        coVerify(exactly = 0) { f.stores.insert(any(), any()) }
        verify(exactly = 0) { f.lnreader.set(any()) }
        verify(exactly = 0) { f.keiyoushi.set(any()) }
    }

    @Test
    fun `first launch adds both repos and marks them seeded`() {
        val f = Fixture(lnreaderSeeded = false, keiyoushiSeeded = false, insertResult = Result.success(Unit))
        f.run()
        verify(exactly = 1) {
            f.plugins.addRepository(DefaultSourceRepos.LNREADER_NAME, DefaultSourceRepos.LNREADER_URL)
        }
        coVerify(exactly = 1) { f.stores.insert(DefaultSourceRepos.KEIYOUSHI_URL, false) }
        verify { f.lnreader.set(true) }
        verify { f.keiyoushi.set(true) }
    }

    @Test
    fun `repos that were seeded before are not added again`() {
        val f = Fixture(lnreaderSeeded = true, keiyoushiSeeded = true, insertResult = Result.success(Unit))
        f.run()
        verify(exactly = 0) { f.plugins.addRepository(any(), any()) }
        coVerify(exactly = 0) { f.stores.insert(any(), any()) }
        verify(exactly = 0) { f.lnreader.set(any()) }
        verify(exactly = 0) { f.keiyoushi.set(any()) }
    }

    @Test
    fun `a failed Keiyoushi insert is not marked so it retries on the next launch`() {
        val f = Fixture(
            lnreaderSeeded = false,
            keiyoushiSeeded = false,
            insertResult = Result.failure(Exception("offline")),
        )
        f.run()
        verify { f.lnreader.set(true) }
        verify(exactly = 0) { f.keiyoushi.set(any()) }
    }

    @Test
    fun `only the missing repo is seeded`() {
        val f = Fixture(lnreaderSeeded = true, keiyoushiSeeded = false, insertResult = Result.success(Unit))
        f.run()
        verify(exactly = 0) { f.plugins.addRepository(any(), any()) }
        coVerify(exactly = 1) { f.stores.insert(DefaultSourceRepos.KEIYOUSHI_URL, false) }
    }

    @Test
    fun `repo urls point at raw github hosted indexes`() {
        DefaultSourceRepos.LNREADER_URL shouldStartWith "https://raw.githubusercontent.com/LNReader/"
        DefaultSourceRepos.KEIYOUSHI_URL shouldBe
            "https://raw.githubusercontent.com/keiyoushi/extensions/repo/repo.json"
    }

    @Test
    fun `privacy policy link points at PRIVACY md in the configured repo`() {
        PRIVACY_POLICY_URL shouldContain GITHUB_REPO
        PRIVACY_POLICY_URL shouldBe "https://github.com/$GITHUB_REPO/blob/main/PRIVACY.md"
    }
}
