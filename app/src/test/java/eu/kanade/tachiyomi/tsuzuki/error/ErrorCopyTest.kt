package eu.kanade.tachiyomi.tsuzuki.error

import eu.kanade.tachiyomi.network.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

class ErrorCopyTest {
    @ParameterizedTest
    @MethodSource("errors")
    fun `known failures choose an actionable cause`(failure: Throwable, reason: ErrorReason, action: ErrorAction) {
        val copy = errorCopy(failure, "Chapter 42")
        assertEquals("Chapter 42", copy.whatFailed)
        assertEquals(reason, copy.reason)
        assertEquals(action, copy.primaryAction)
    }

    @Test
    fun `wrapper retains HTTP code and challenge marker`() {
        val copy = errorCopy(IOException("DDoS-Guard", HttpException(403)))
        assertEquals(ErrorReason.CHALLENGE, copy.reason)
        assertTrue(copy.technical.contains("HTTP 403"))
    }

    @Test
    fun `plain 503 does not invent a browser challenge`() {
        assertEquals(ErrorReason.SERVER, errorCopy(HttpException(503)).reason)
        assertEquals(ErrorAction.RETRY, errorCopy(HttpException(503)).primaryAction)
    }

    @Test
    fun `URL is reduced to host in message title and source`() {
        val url = "https://user:secret@example.org/private/chapter?token=privateToken#signed"
        val copy = errorCopy(IOException("Failed GET $url"), url, url)
        assertEquals("example.org", copy.whatFailed)
        assertEquals("Failed GET example.org", copy.message)
        assertEquals("Failed GET example.org · example.org", copy.technical)
        listOf("secret", "private", "signed", "token", "user").forEach { assertFalse(copy.toString().contains(it)) }
    }

    @Test
    fun `headers tokens and stack frames are never copyable`() {
        val raw = """
            HTTP 403 at https://example.org/p?a=secretQuery
            Authorization: Bearer secretAuth
            Cookie: session=secretCookie
            token=secretToken api_key=secretKey password=secretPassword
                at private.package.Reader.load(Reader.kt:123)
            Caused by: private.package.Error
        """.trimIndent()
        val diagnostic = errorCopy(IOException(raw)).technical
        listOf(
            "secretQuery",
            "secretAuth",
            "secretCookie",
            "secretToken",
            "secretKey",
            "secretPassword",
            "Reader.kt",
            "private.package",
        ).forEach {
            assertFalse(diagnostic.contains(it), diagnostic)
        }
        assertTrue(diagnostic.contains("HTTP 403"))
        assertTrue(diagnostic.contains("example.org"))
    }

    @Test
    fun `malformed and local URLs are not displayed`() {
        assertEquals("[URL]", redactErrorText("https://?token=secret"))
        assertEquals("[URL]", redactErrorText("content://files/private?key=secret"))
        assertEquals("[URL]", redactErrorText("file:///storage/private/book.txt"))
        assertEquals(null, safeErrorHost("not a URL"))
        assertEquals(null, safeErrorHost("content://files/private"))
    }

    @Test
    fun `query fragments without a full URL are stripped`() {
        assertEquals("HTTP 403 [redacted]", redactErrorText("HTTP 403 ?token=hidden&key=secret"))
    }

    @Test
    fun `diagnostic is bounded and whitespace flattened`() {
        val copy = errorCopy(IOException("a".repeat(1000) + "\nsecret"))
        assertEquals(600, copy.message.length)
        assertFalse(copy.technical.contains('\n'))
    }

    @Test
    fun `cyclic cause chain terminates`() {
        val first = IOException("first")
        val second = IOException("second", first)
        first.initCause(second)
        assertEquals("first · second", errorCopy(first).message)
    }

    @Test
    fun `cancellation is propagated even inside a wrapper`() {
        val cancellation = CancellationException("owner left")
        val thrown = assertThrows(CancellationException::class.java) { errorCopy(IOException("wrapped", cancellation)) }
        assertSame(cancellation, thrown)
    }

    @Test
    fun `reader timeout is an error while normal cancellation propagates`() = runTest {
        val timeout = try {
            withTimeout(1) { delay(2) }
            error("Expected timeout")
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            e
        }
        assertEquals(ErrorReason.TIMEOUT, errorCopy(timeout).reason)
    }

    @Test
    fun `stages start at two seconds and time out at thirty`() {
        assertEquals(LoadingPhase.SPINNER, loadingPhase(-1))
        assertEquals(LoadingPhase.SPINNER, loadingPhase(0))
        assertEquals(LoadingPhase.SPINNER, loadingPhase(1999))
        assertEquals(LoadingPhase.STAGES, loadingPhase(2000))
        assertEquals(LoadingPhase.STAGES, loadingPhase(29999))
        assertEquals(LoadingPhase.TIMED_OUT, loadingPhase(30000))
        assertEquals(LoadingPhase.TIMED_OUT, loadingPhase(Long.MAX_VALUE))
        // A new step starts its own clock, regardless of the previous step's elapsed time.
        assertEquals(LoadingPhase.SPINNER, loadingPhase(0))
    }

    companion object {
        @JvmStatic
        fun errors() = listOf(
            Arguments.of(UnknownHostException("offline"), ErrorReason.NETWORK, ErrorAction.RETRY),
            Arguments.of(ConnectException("refused"), ErrorReason.NETWORK, ErrorAction.RETRY),
            Arguments.of(IOException("wrapper", UnknownHostException()), ErrorReason.NETWORK, ErrorAction.RETRY),
            Arguments.of(SocketTimeoutException(), ErrorReason.TIMEOUT, ErrorAction.RETRY),
            Arguments.of(TimeoutException(), ErrorReason.TIMEOUT, ErrorAction.RETRY),
            Arguments.of(IOException("HTTP 403 Cloudflare"), ErrorReason.CHALLENGE, ErrorAction.WEBVIEW),
            Arguments.of(IOException("HTTP 503 DDoS-Guard"), ErrorReason.CHALLENGE, ErrorAction.WEBVIEW),
            Arguments.of(HttpException(404), ErrorReason.NOT_FOUND, ErrorAction.RETRY),
            Arguments.of(HttpException(500), ErrorReason.SERVER, ErrorAction.RETRY),
            Arguments.of(IOException("HTTP error 503"), ErrorReason.SERVER, ErrorAction.RETRY),
            Arguments.of(IllegalArgumentException("JSON parse failed"), ErrorReason.PARSE, ErrorAction.EXTENSIONS),
            Arguments.of(MissingSourceException(), ErrorReason.EXTENSION, ErrorAction.EXTENSIONS),
            Arguments.of(IllegalStateException("Untrusted extension"), ErrorReason.EXTENSION, ErrorAction.EXTENSIONS),
            Arguments.of(
                IOException("ENOSPC (No space left on device)"),
                ErrorReason.STORAGE_FULL,
                ErrorAction.STORAGE,
            ),
            Arguments.of(SecurityException("SAF grant revoked"), ErrorReason.PERMISSION, ErrorAction.STORAGE),
            Arguments.of(IOException("open failed: EACCES"), ErrorReason.PERMISSION, ErrorAction.STORAGE),
            Arguments.of(IOException("other failure"), ErrorReason.OTHER, ErrorAction.RETRY),
        )
    }
}
