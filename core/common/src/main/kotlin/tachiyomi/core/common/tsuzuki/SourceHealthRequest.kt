package tachiyomi.core.common.tsuzuki

import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Request

/** In-process metadata only. Never sent as an HTTP header. */
data class SourceHealthRequest(val sourceId: Long)

fun Request.forSourceHealth(sourceId: Long): Request =
    newBuilder().tag(SourceHealthRequest::class.java, SourceHealthRequest(sourceId)).build()

private val activeSource = ThreadLocal<SourceHealthRequest>()

suspend fun <T> withSourceHealthRequest(sourceId: Long, block: suspend () -> T): T =
    withContext(activeSource.asContextElement(SourceHealthRequest(sourceId))) { block() }

val sourceHealthCallTags = EventListener.Factory { call ->
    val source = activeSource.get()
    if (source == null || call.request().tag(SourceHealthRequest::class.java) != null) {
        EventListener.NONE
    } else {
        // Capture when the extension creates the call. Tag it at start, once OkHttp has
        // initialized the call's tags and before dispatching it to its network executor.
        object : EventListener() {
            override fun callStart(call: Call) {
                call.tag(SourceHealthRequest::class.java) { source }
            }
        }
    }
}
