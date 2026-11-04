package eu.kanade.tachiyomi.util.system

import io.kotest.matchers.shouldBe
import okhttp3.Headers
import org.junit.jupiter.api.Test

class WebViewHeadersTest {
    @Test
    fun `origin and transport headers cannot reach a WebView load`() {
        val headers = Headers.Builder()
            .add("Origin", "https://source.example")
            .add("HOST", "source.example")
            .add("Content-Length", "42")
            .add("Proxy-Authorization", "proxy-token")
            .add("Connection", "UpGrAdE")
            .add("Upgrade", "websocket")
            .build()

        headers.toWebViewHeaders() shouldBe emptyMap()
    }

    @Test
    fun `source authentication user agent and ordinary connection headers survive`() {
        val headers = Headers.Builder()
            .add("Authorization", "Bearer source-token")
            .add("User-Agent", "SourceReader")
            .add("Referer", "https://source.example/")
            .add("Connection", "close")
            .build()

        headers.toWebViewHeaders() shouldBe mapOf(
            "Authorization" to "Bearer source-token",
            "User-Agent" to "SourceReader",
            "Referer" to "https://source.example/",
            "Connection" to "close",
        )
    }

    @Test
    fun `a repeated source header retains its first value`() {
        Headers.Builder().add("Accept", "text/html").add("Accept", "*/*")
            .build().toWebViewHeaders() shouldBe mapOf("Accept" to "text/html")
    }
}
