package eu.kanade.tachiyomi.tsuzuki.update

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourcesAfterScanTest {

    @Test
    fun `nothing is ready before the scan, and the first ready value has the scanned sources`() = runTest {
        val map = MutableStateFlow(emptyMap<String, String>())
        val scanned = MutableStateFlow(false)
        val seen = mutableListOf<Map<String, String>?>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { afterScan(map, scanned).collect { seen += it } }
        testScheduler.advanceUntilIdle()
        // The manager's own order: the scan result first, then the flag.
        map.value = mapOf("ext" to "source")
        scanned.value = true
        testScheduler.advanceUntilIdle()
        job.cancel()
        assertEquals(null, seen.first())
        // Every ready value carries the scan; none is the empty pre-scan map.
        assertTrue(seen.filterNotNull().isNotEmpty())
        assertTrue(seen.filterNotNull().all { it == mapOf("ext" to "source") })
    }

    @Test
    fun `the flag arriving before a separately collected list no longer reports ready with the old list`() = runTest {
        // The old shape: a list derived through its own stateIn collector, combined with the flag.
        val map = MutableStateFlow(emptyMap<String, String>())
        val scanned = MutableStateFlow(false)
        // Its own scheduler, not advanced after start: the derived collector stays behind, as it can on a device.
        val laggingScope = TestScope(StandardTestDispatcher())
        val derived = map.map { it.values.toList() }.stateIn(laggingScope, SharingStarted.Eagerly, emptyList())
        laggingScope.testScheduler.runCurrent()

        map.value = mapOf("ext" to "source")
        scanned.value = true
        // Before the derived collector runs, the flag is true but the list is still the pre-scan one.
        val oldShape = combine(derived, scanned) { list, done -> list to done }.first { it.second }.first
        assertTrue(oldShape.isEmpty(), "reproduces the review's race: ready with no extension sources")

        // The new shape reads the manager's own state, so ready always carries the scan.
        val newShape = afterScan(map, scanned).filterNotNull().first()
        assertEquals(mapOf("ext" to "source"), newShape)
    }
}
