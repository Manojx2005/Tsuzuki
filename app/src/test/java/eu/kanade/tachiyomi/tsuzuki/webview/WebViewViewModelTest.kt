package eu.kanade.tachiyomi.tsuzuki.webview

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.webview.WebViewViewModel
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.Headers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
class WebViewViewModelTest {
    private val manager = mockk<tachiyomi.domain.source.service.SourceManager>()
    private val network = mockk<NetworkHelper>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `slow extension headers run off the caller and stay pending until ready`() {
        val caller = Thread.currentThread()
        val lookupThread = AtomicReference<Thread>()
        val headerThread = AtomicReference<Thread>()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val source = mockk<HttpSource>()
        every { manager.get(1L) } answers {
            lookupThread.set(Thread.currentThread())
            source
        }
        every { source.headers } answers {
            headerThread.set(Thread.currentThread())
            started.countDown()
            release.await(5, TimeUnit.SECONDS)
            Headers.Builder().add("Origin", "https://source.example")
                .add("Authorization", "Bearer test-token").build()
        }

        val model = WebViewViewModel(1L, manager, network)
        try {
            started.await(5, TimeUnit.SECONDS) shouldBe true
            (lookupThread.get() === caller) shouldBe false
            (headerThread.get() === caller) shouldBe false
            model.headers.value shouldBe null
        } finally {
            release.countDown()
        }
        awaitHeaders(model) shouldBe mapOf("Authorization" to "Bearer test-token")
    }

    @Test
    fun `a failing extension publishes an empty header map instead of staying pending`() {
        val source = mockk<HttpSource>()
        every { manager.get(1L) } returns source
        every { source.headers } throws IllegalStateException("Broken source headers")

        awaitHeaders(WebViewViewModel(1L, manager, network)) shouldBe emptyMap()
    }

    @Test
    fun `an unavailable source still finishes header initialization`() {
        every { manager.get(1L) } returns null

        awaitHeaders(WebViewViewModel(1L, manager, network)) shouldBe emptyMap()
    }

    @Test
    fun `a source lookup failure also finishes header initialization`() {
        every { manager.get(1L) } throws IllegalStateException("Source lookup failed")

        awaitHeaders(WebViewViewModel(1L, manager, network)) shouldBe emptyMap()
    }

    private fun awaitHeaders(model: WebViewViewModel): Map<String, String> = runBlocking {
        withTimeout(5_000) { model.headers.filterNotNull().first() }
    }
}
