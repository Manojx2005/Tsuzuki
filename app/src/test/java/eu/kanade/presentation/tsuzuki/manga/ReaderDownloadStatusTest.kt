package eu.kanade.presentation.tsuzuki.manga

import android.os.Trace
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ReaderDownloadStatusTest {
    @BeforeEach
    fun setUp() {
        mockkStatic(Trace::class)
        every { Trace.beginSection(any()) } returns Unit
        every { Trace.endSection() } returns Unit
        every { Trace.isEnabled() } returns false
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `blocked storage query runs off the composing thread and publishes when ready`() {
        val caller = Thread.currentThread()
        val queryThread = AtomicReference<Thread>()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        Host().use { host ->
            try {
                host.render("chapter A", false) {
                    queryThread.set(Thread.currentThread())
                    started.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    true
                }
                started.await(5, TimeUnit.SECONDS) shouldBe true
                (queryThread.get() === caller) shouldBe false
                host.state.value shouldBe false
                release.countDown()
                awaitValue(host, true)
                host.errors.isEmpty() shouldBe true
            } finally {
                release.countDown()
            }
        }
    }

    @Test
    fun `recycled transition gets new state and discards a late old result`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        Host().use { host ->
            try {
                host.render("chapter A", false) {
                    started.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    false
                }
                started.await(5, TimeUnit.SECONDS) shouldBe true
                val oldState = host.state
                val oldJobs = host.effects()
                host.render("local chapter B", true, null)
                (host.state === oldState) shouldBe false
                host.state.value shouldBe true
                release.countDown()
                join(oldJobs)
                host.state.value shouldBe true
                host.errors.isEmpty() shouldBe true
            } finally {
                release.countDown()
            }
        }
    }

    @Test
    fun `previous downloaded result is reset immediately for another chapter`() {
        Host().use { host ->
            host.render("downloaded A", false) { true }
            awaitValue(host, true)
            host.render("missing B", false, null)
            host.state.value shouldBe false
        }
    }

    @Test
    fun `storage exception leaves status unavailable without crashing composition`() {
        val entered = CountDownLatch(1)
        Host().use { host ->
            host.render("denied chapter", false) {
                entered.countDown()
                throw SecurityException("Storage permission revoked")
            }
            entered.await(5, TimeUnit.SECONDS) shouldBe true
            join(host.effects())
            host.state.value shouldBe false
            host.errors.isEmpty() shouldBe true
        }
    }

    private fun awaitValue(host: Host, value: Boolean) = runBlocking {
        withTimeout(5000) {
            while (host.state.value != value) delay(10)
        }
    }

    private fun join(jobs: List<Job>) = runBlocking {
        withTimeout(5000) { jobs.forEach { it.join() } }
    }

    private class Host : AutoCloseable {
        val errors = CopyOnWriteArrayList<Throwable>()
        private val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Unconfined + BroadcastFrameClock() +
                CoroutineExceptionHandler { _, throwable -> errors.add(throwable) },
        )
        private val recomposer = Recomposer(scope.coroutineContext)
        private val composition = Composition(
            object : AbstractApplier<Unit>(Unit) {
                override fun insertBottomUp(index: Int, instance: Unit) = Unit
                override fun insertTopDown(index: Int, instance: Unit) = Unit
                override fun move(from: Int, to: Int, count: Int) = Unit
                override fun remove(index: Int, count: Int) = Unit
                override fun onClear() = Unit
            },
            recomposer,
        )
        lateinit var state: State<Boolean>
            private set

        init {
            scope.launch { recomposer.runRecomposeAndApplyChanges() }
        }

        fun render(key: String, initial: Boolean, lookup: (() -> Boolean)?) {
            composition.setContent {
                state = rememberReaderDownloadStatus(key, initial, lookup)
            }
        }

        fun effects() = recomposer.effectCoroutineContext[Job]!!.children.toList()

        override fun close() {
            composition.dispose()
            recomposer.cancel()
            scope.cancel()
        }
    }
}
