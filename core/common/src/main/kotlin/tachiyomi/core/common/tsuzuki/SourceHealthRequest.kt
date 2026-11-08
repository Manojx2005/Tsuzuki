package tachiyomi.core.common.tsuzuki

import okhttp3.Request

/** In-process metadata only. Never sent as an HTTP header. */
data class SourceHealthRequest(val sourceId: Long)

fun Request.forSourceHealth(sourceId: Long): Request =
    newBuilder().tag(SourceHealthRequest::class.java, SourceHealthRequest(sourceId)).build()
