package eu.kanade.tachiyomi.tsuzuki.error

import eu.kanade.tachiyomi.network.HttpException
import kotlinx.coroutines.CancellationException
import tachiyomi.domain.chapter.model.NoChaptersException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.TimeoutException

enum class ErrorReason {
    NETWORK,
    TIMEOUT,
    CHALLENGE,
    NOT_FOUND,
    SERVER,
    PARSE,
    EXTENSION,
    STORAGE_FULL,
    PERMISSION,
    NO_CHAPTERS,
    OTHER,
}
enum class ErrorAction { RETRY, WEBVIEW, EXTENSIONS, STORAGE }

class MissingSourceException : IllegalStateException("Source not installed")

data class ErrorCopy(
    val whatFailed: String,
    val reason: ErrorReason,
    val message: String,
    val technical: String,
    val primaryAction: ErrorAction,
)

/**
 * Non-throwing variant for UI and composition call sites: a cancellation that was stored as an error state becomes
 * the generic copy instead of crashing the caller. Suspend callers use [errorCopy], which rethrows cancellation.
 */
fun errorCopyForUi(error: Throwable, whatFailed: String = "", sourceUrl: String? = null): ErrorCopy = try {
    errorCopy(error, whatFailed, sourceUrl)
} catch (_: CancellationException) {
    ErrorCopy(
        whatFailed = redactErrorTitle(whatFailed),
        reason = ErrorReason.OTHER,
        message = "Cancelled",
        technical = "Cancelled",
        primaryAction = ErrorAction.RETRY,
    )
}

/** Pure classification; nothing from an exception reaches display or clipboard before redaction. */
fun errorCopy(error: Throwable, whatFailed: String = "", sourceUrl: String? = null): ErrorCopy {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    val chain = generateSequence(error) { it.cause }.takeWhile { seen.add(it) }.take(12).toList()
    chain.filterIsInstance<CancellationException>().firstOrNull {
        it !is kotlinx.coroutines.TimeoutCancellationException
    }?.let { throw it }
    val raw = chain.mapNotNull { it.message?.take(4096)?.takeIf(String::isNotBlank) }.distinct().joinToString(" · ")
    val text = (raw + " " + chain.joinToString { it.javaClass.simpleName }).lowercase()
    val status = chain.filterIsInstance<HttpException>().firstOrNull()?.code
        ?: Regex("""(?i)(?:http(?: error)?|status(?: code)?)\s*[:=]?\s*([45][0-9]{2})""").find(raw)
            ?.groupValues?.get(1)?.toIntOrNull()
    val challenge = listOf("cloudflare", "ddos-guard", "ddos guard", "browser check", "challenge").any(text::contains)
    val reason = when {
        listOf("enospc", "no space left", "storage full", "disk full").any(text::contains) -> ErrorReason.STORAGE_FULL
        chain.any {
            it is SecurityException
        } || listOf("eacces", "permission denied").any(text::contains) -> ErrorReason.PERMISSION
        chain.any { it is MissingSourceException } ||
            listOf(
                "untrusted",
                "extension not installed",
                "extension missing",
                "source not installed",
                "source not found",
                "missing plugin",
                "stubsource",
            ).any(text::contains) -> ErrorReason.EXTENSION
        chain.any { it is NoChaptersException } -> ErrorReason.NO_CHAPTERS
        status == 403 || (challenge && (status == null || status == 503)) -> ErrorReason.CHALLENGE
        chain.any { it is UnknownHostException || it is ConnectException } -> ErrorReason.NETWORK
        chain.any {
            it is SocketTimeoutException || it is TimeoutException ||
                it is kotlinx.coroutines.TimeoutCancellationException
        } ||
            "timed out" in text ||
            "timeout" in text -> ErrorReason.TIMEOUT
        status == 404 -> ErrorReason.NOT_FOUND
        status != null && status >= 500 -> ErrorReason.SERVER
        listOf(
            "parse",
            "serialization",
            "jsonexception",
            "selector",
            "unexpected token",
        ).any(text::contains) -> ErrorReason.PARSE
        else -> ErrorReason.OTHER
    }
    val message = redactErrorText(raw.ifBlank { error.javaClass.simpleName })
    val host = sourceUrl?.let(::safeErrorHost)
    return ErrorCopy(
        whatFailed = redactErrorTitle(whatFailed),
        reason = reason,
        message = message,
        technical = listOfNotNull(
            status?.let {
                "HTTP $it"
            },
            message.takeIf(String::isNotBlank),
            host,
        ).distinct().joinToString(" · "),
        primaryAction = when (reason) {
            ErrorReason.CHALLENGE -> ErrorAction.WEBVIEW
            ErrorReason.EXTENSION, ErrorReason.PARSE -> ErrorAction.EXTENSIONS
            ErrorReason.STORAGE_FULL, ErrorReason.PERMISSION -> ErrorAction.STORAGE
            else -> ErrorAction.RETRY
        },
    )
}

fun safeErrorHost(url: String): String? = try {
    URI(url).takeIf { it.scheme.equals("http", true) || it.scheme.equals("https", true) }?.host
} catch (_: java.net.URISyntaxException) {
    null
}

private val URL_RE = Regex("""(?i)(?:https?://|content://|file://)[^\s<>\"']+""")

/** Titles keep `?`, `&` and `#`; only URLs are reduced to their host. */
fun redactErrorTitle(text: String): String = text
    .replace(URL_RE) { safeErrorHost(it.value) ?: "[URL]" }
    .replace(Regex("""[\r\n\t ]+"""), " ")
    .trim()
    .take(600)

/** Keep a short diagnostic, never URL paths, queries, credentials, headers or stack traces. */
fun redactErrorText(text: String): String = text
    .replace(Regex("""(?im)^\s*(?:at\s+|caused by:).*$(?:\r?\n)?"""), " ")
    .replace(
        Regex("""(?i)(?:authorization|proxy-authorization|cookie|set-cookie)["']?\s*[:=][^\r\n]*"""),
        "[redacted]",
    )
    .replace(Regex("""(?i)\b(?:bearer|basic)\s+[A-Za-z0-9+/=._~-]+"""), "[redacted]")
    .replace(URL_RE) { safeErrorHost(it.value) ?: "[URL]" }
    .replace(
        Regex(
            """(?i)\b(?:token|api[_-]?key|password|secret|session|signature|access_token)["']?\s*[:=]\s*""" +
                """(?:"[^"]*"|'[^']*'|[^\s,;]+)""",
        ),
        "[redacted]",
    )
    .replace(Regex("""(?<![\w@])[\w.%+-]+:[^\s@/:]+@(?=[\w.-])"""), "")
    .replace(Regex("""[?&#][^\s]+"""), "[redacted]")
    // File-system paths: keep the file name, drop the directories (they can contain book titles and user names).
    .replace(Regex("""(?<![\w.:/~-])/(?:[^\s/<>\"'|?*:][^/\r\n<>\"'|?*:]*/)+"""), "[path]/")
    .replace(Regex("""(?i)(?<!\w)[A-Z]:\\(?:[^\s\\<>\"'|?*:][^\\\r\n<>\"'|?*:]*\\)*"""), "[path]/")
    .replace(Regex("""[\r\n\t ]+"""), " ")
    .trim()
    .take(600)

enum class LoadingPhase { SPINNER, STAGES, TIMED_OUT }
fun loadingPhase(elapsedMs: Long): LoadingPhase = when {
    elapsedMs >= 30_000L -> LoadingPhase.TIMED_OUT
    elapsedMs >= 2_000L -> LoadingPhase.STAGES
    else -> LoadingPhase.SPINNER
}
