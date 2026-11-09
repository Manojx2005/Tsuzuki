package eu.kanade.tachiyomi.tsuzuki.sourcehealth

import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.tsuzuki.forSourceHealth
import tachiyomi.core.common.tsuzuki.sourceHealthCallTags
import tachiyomi.core.common.tsuzuki.withSourceHealthRequest
import java.io.IOException

class SourceHealthInterceptorTest {
    private val store = SourceHealthStore(InMemoryPreferenceStore())

    @Test
    fun `direct calls on a shared host keep their source across dispatchers`() = runBlocking {
        coroutineScope {
            val client = testClient(this).newBuilder().build()
            val first = async {
                withSourceHealthRequest(1) {
                    withContext(Dispatchers.Default) { client.newCall(request("ok")).await().close() }
                }
            }
            val second = async {
                withSourceHealthRequest(2) { client.newCall(request("unavailable")).await().close() }
            }
            first.await()
            second.await()
            client.newCall(request("ok")).await().close()
        }
        assertEquals(1L, store.get(1).days.single().successfulRequests)
        assertEquals(503, store.get(2).lastError?.httpCode)
        assertEquals(0L, store.get(2).lastSuccessAt)
    }

    @Test
    fun `explicit request tag takes precedence over ambient source`() = runBlocking {
        coroutineScope {
            val client = testClient(this)
            withSourceHealthRequest(1) {
                client.newCall(request("ok").forSourceHealth(2)).execute().close()
            }
        }
        assertEquals(SourceHealth(), store.get(1))
        assertEquals(1L, store.get(2).days.single().successfulRequests)
    }

    @Test
    fun `cache only synthetic and cancelled calls leave no measurement`() = runBlocking {
        coroutineScope {
            val client = testClient(this)
            withSourceHealthRequest(1) {
                client.newCall(request("cached")).execute().close()
                client.newCall(request("synthetic")).execute().close()
                val cacheMiss = request("cache-miss").newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
                client.newCall(cacheMiss).execute().close()
                assertThrows(IOException::class.java) {
                    client.newCall(request("cancelled")).execute().close()
                }
            }
        }
        assertEquals(SourceHealth(), store.get(1))
    }

    private fun request(path: String): Request = Request.Builder().url("https://example.invalid/$path").build()

    private fun testClient(scope: CoroutineScope): OkHttpClient = OkHttpClient.Builder()
        .eventListenerFactory(sourceHealthCallTags)
        .addInterceptor(SourceHealthInterceptor(store, scope))
        .addInterceptor { chain ->
            val request = chain.request()
            val path = request.url.pathSegments.single()
            val code = when (path) {
                "unavailable" -> 503
                "cache-miss" -> 504
                else -> 200
            }
            val response = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("Fixture")
                .build()
            if (path == "cancelled") chain.call().cancel()
            response.newBuilder().body("".toResponseBody()).apply {
                when (path) {
                    "cached" -> cacheResponse(response)
                    "cache-miss", "synthetic" -> Unit
                    else -> networkResponse(response)
                }
            }.build()
        }
        .build()
}
