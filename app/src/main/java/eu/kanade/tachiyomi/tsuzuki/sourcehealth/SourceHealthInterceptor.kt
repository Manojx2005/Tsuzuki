package eu.kanade.tachiyomi.tsuzuki.sourcehealth

import eu.kanade.tachiyomi.network.interceptor.CloudflareBypassException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import tachiyomi.core.common.tsuzuki.SourceHealthRequest
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.time.TimeSource

internal class SourceHealthInterceptor(
    private val store: SourceHealthStore,
    private val scope: CoroutineScope,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val source = request.tag(SourceHealthRequest::class.java) ?: return chain.proceed(request)
        val start = TimeSource.Monotonic.markNow()

        fun record(error: SourceHealthError? = null, httpCode: Int? = null) {
            val at = System.currentTimeMillis()
            val failure = error?.let { SourceHealthFailure(at, it, httpCode) }
            val sample = SourceHealthSample(at, start.elapsedNow().inWholeMilliseconds, failure)
            scope.launch(Dispatchers.IO) { store.record(source.sourceId, sample) }
        }

        return try {
            chain.proceed(request).also { response ->
                // A cache hit has no measured network latency.
                if (response.networkResponse == null && response.cacheResponse != null) return@also
                when {
                    response.header("cf-mitigated") == "challenge" -> record(SourceHealthError.CLOUDFLARE)
                    response.isSuccessful -> record()
                    else -> record(SourceHealthError.HTTP, response.code)
                }
            }
        } catch (error: IOException) {
            if (!chain.call().isCanceled()) record(classifySourceHealthError(error))
            throw error
        }
    }
}

internal fun classifySourceHealthError(error: IOException): SourceHealthError = when {
    generateSequence<Throwable>(error) {
        it.cause
    }.any { it is CloudflareBypassException } -> SourceHealthError.CLOUDFLARE
    error is InterruptedIOException -> SourceHealthError.TIMEOUT
    else -> SourceHealthError.NETWORK
}
