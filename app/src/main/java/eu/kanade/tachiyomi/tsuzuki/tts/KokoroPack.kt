package eu.kanade.tachiyomi.tsuzuki.tts

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

internal class KokoroPack(
    private val context: Context,
    val directory: File = File(context.filesDir, "tts/$NAME"),
) {
    private fun files() = context.assets.open("kokoro-test-manifest.json").bufferedReader().use {
        Json.parseToJsonElement(it.readText()).jsonObject.getValue("files").jsonObject
    }

    /** Allowlisted pack files and their expected sizes. */
    fun expectedSizes(): Map<String, Long> = files().mapValues { (_, metadata) ->
        metadata.jsonObject.getValue("size").jsonPrimitive.long
    }

    suspend fun remove() = withContext(Dispatchers.IO) { directory.deleteRecursively() }

    suspend fun isInstalled(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            files().all { (name, metadata) ->
                val file = File(directory, name)
                file.canonicalFile.parentFile == directory.canonicalFile && file.isFile &&
                    file.length() == metadata.jsonObject.getValue("size").jsonPrimitive.long
            }
        }.getOrDefault(false)
    }

    suspend fun verify(directory: File = this.directory): Set<String> = withContext(Dispatchers.IO) {
        require(!File(directory, "espeak-ng-data").exists()) { "Unsupported voice pack" }
        val expected = files()
        require(directory.listFiles().orEmpty().none { it.name !in expected && it.name != MANIFEST })
        expected.forEach { (name, metadata) ->
            val file = File(directory, name)
            require(file.canonicalFile.parentFile == directory.canonicalFile && file.isFile)
            require(file.length() == metadata.jsonObject.getValue("size").jsonPrimitive.long)
            val hash = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    hash.update(buffer, 0, count)
                }
            }
            require(
                hash.digest().joinToString("") { "%02x".format(it) } ==
                    metadata.jsonObject.getValue("sha256").jsonPrimitive.content,
            )
        }
        File(directory, "lexicon-us-en.txt").useLines { lines ->
            lines.map { it.substringBefore(' ') }.toSet()
        }
    }

    companion object {
        const val NAME = "kokoro-cmu-v1"
        const val MANIFEST = "manifest.json"
    }
}

private val COMBINING_MARKS = Regex("\\p{Mn}+")
private val CURLY_APOSTROPHES = Regex("[\u2018\u2019\u201A\u201B\u02BC\uFF07]")
private val DISCARDED_PUNCTUATION = Regex("[\"\u201C\u201D\u201E\u00AB\u00BB()\\[\\]{}]")
private val DASHES = Regex("\\s*(?:[\u2014\u2015\u2013]|--+|\\s-\\s)\\s*")
private val INNER_HYPHEN = Regex("(?<=\\p{L})-(?=\\p{L})")
private val EDGE_APOSTROPHE = Regex("(?<!\\p{L})'|'(?!\\p{L})")
private val UNSUPPORTED_TEXT = Regex("[0-9@]|https?://|www\\.", RegexOption.IGNORE_CASE)
private val BOUNDARIES = Regex("[ \\t\\r\\n,.!?;:]+")

/**
 * The exact text the native runtime receives, and the text the coverage check inspects. Everything it emits is ASCII
 * letters, apostrophes between letters, digits, spaces and the `, . ! ? ; :` boundaries the checker splits on, so the
 * checker and the native tokenizer see identical input by construction. The mapping was not compared with the runtime
 * source; the on-device listening test is the check.
 */
internal fun kokoroModelText(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKD)
    .replace(COMBINING_MARKS, "")
    .replace(CURLY_APOSTROPHES, "'")
    .replace(DISCARDED_PUNCTUATION, " ")
    .replace(DASHES, ", ")
    .replace(INNER_HYPHEN, " ")
    .replace('-', ' ')
    .replace(EDGE_APOSTROPHE, " ")

/** Match the pinned runtime's ASCII space/punctuation boundaries; unsupported text goes to the phone voice. */
internal fun isKokoroTextCovered(text: String, words: Set<String>): Boolean {
    val normalized = kokoroModelText(text).lowercase(Locale.US)
    if (normalized.any { it.code > 127 } || UNSUPPORTED_TEXT.containsMatchIn(normalized)) return false
    val tokens = normalized.split(BOUNDARIES).filter(String::isNotBlank)
    return tokens.isNotEmpty() && tokens.all { it in words }
}
