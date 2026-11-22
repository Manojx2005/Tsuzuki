package eu.kanade.tachiyomi.tsuzuki.update

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class SourcesReadyTest {

    @Test
    fun `returns at once when sources are already loaded`() = runTest {
        assertTrue(awaitSourcesReady(MutableStateFlow(true)))
        assertEquals(0L, currentTime)
    }

    @Test
    fun `waits for loading to finish, then goes on`() = runTest {
        val initialized = MutableStateFlow(false)
        val ready = async { awaitSourcesReady(initialized, 30.seconds) }
        advanceTimeBy(5_000)
        assertFalse(ready.isCompleted)
        initialized.value = true
        assertTrue(ready.await())
        assertEquals(5_000L, currentTime)
    }

    @Test
    fun `gives up after the time limit so one broken extension cannot hold the update`() = runTest {
        assertFalse(awaitSourcesReady(MutableStateFlow(false), 30.seconds))
        assertEquals(30_000L, currentTime)
    }
}
