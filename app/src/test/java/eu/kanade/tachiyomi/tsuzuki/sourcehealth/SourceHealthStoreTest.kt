package eu.kanade.tachiyomi.tsuzuki.sourcehealth

import eu.kanade.tachiyomi.network.interceptor.CloudflareBypassException
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.tsuzuki.SourceHealthRequest
import tachiyomi.core.common.tsuzuki.forSourceHealth
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.ZoneOffset

class SourceHealthStoreTest {
    private val now = 864_000_000L

    @Test
    fun `measurements persist by source and ignore failures in the latency average`() {
        val preferences = InMemoryPreferenceStore()
        val store = SourceHealthStore(preferences)
        store.record(1, SourceHealthSample(now, 1_000), ZoneOffset.UTC)
        store.record(2, SourceHealthSample(now, 10_000), ZoneOffset.UTC)
        val failure = SourceHealthFailure(now + 20, SourceHealthError.HTTP, 503)
        store.record(1, SourceHealthSample(failure.at, 30_000, failure), ZoneOffset.UTC)

        val reloaded = SourceHealthStore(preferences)
        assertEquals(SourceHealthStatus.DOWN, reloaded.get(1).status(10))
        assertEquals(1_000L, reloaded.get(1).averageMillis(10))
        assertEquals(now, reloaded.get(1).lastSuccessAt)
        assertEquals(failure, reloaded.get(1).lastError)
        assertEquals(SourceHealthStatus.SLOW, reloaded.get(2).status(10))
        assertEquals(SourceHealthStatus.UNKNOWN, reloaded.get(3).status(10))
    }

    @Test
    fun `late failure writes preserve recovery and the seven day history rolls over`() {
        val newer = SourceHealthSample(now + 5, 500)
        val older = SourceHealthSample(now, 6_000, SourceHealthFailure(now, SourceHealthError.TIMEOUT))
        val health = SourceHealth().add(newer, 10).add(older, 10)
        assertEquals(SourceHealthStatus.WORKING, health.status(10))
        assertEquals(
            listOf(null, null, null, null, null, null, SourceHealthStatus.WORKING),
            health.recentDays(10).map {
                it?.status
            },
        )
        assertEquals(SourceHealthStatus.UNKNOWN, health.status(17))
        assertNull(health.averageMillis(17))
        assertEquals(listOf(17L), health.add(newer.copy(at = now + 7 * 86_400_000L), 17).days.map { it.day })
    }

    @Test
    fun `tags are per request and stay out of HTTP headers`() {
        val request = Request.Builder().url("https://example.invalid").build()
        val first = request.forSourceHealth(1)
        val second = request.forSourceHealth(2)
        assertNull(request.tag(SourceHealthRequest::class.java))
        assertEquals(1L, first.tag(SourceHealthRequest::class.java)?.sourceId)
        assertEquals(2L, second.tag(SourceHealthRequest::class.java)?.sourceId)
        assertEquals(request.headers, first.headers)
    }

    @Test
    fun `network error categories use typed causes`() {
        assertEquals(SourceHealthError.CLOUDFLARE, classifySourceHealthError(IOException(CloudflareBypassException())))
        assertEquals(SourceHealthError.TIMEOUT, classifySourceHealthError(SocketTimeoutException()))
        assertEquals(SourceHealthError.NETWORK, classifySourceHealthError(IOException()))
    }
}
