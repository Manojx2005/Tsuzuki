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
import java.util.Locale

internal class KokoroPack(private val context: Context) {
    val directory = File(context.filesDir, "tts/kokoro-cmu-v1")

    private fun files() = context.assets.open("kokoro-test-manifest.json").bufferedReader().use {
        Json.parseToJsonElement(it.readText()).jsonObject.getValue("files").jsonObject
    }

    suspend fun isInstalled(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            files().all { (name, metadata) ->
                val file = File(directory, name)
                file.canonicalFile.parentFile == directory.canonicalFile && file.isFile &&
                    file.length() == metadata.jsonObject.getValue("size").jsonPrimitive.long
            }
        }.getOrDefault(false)
    }

    suspend fun verify(): Set<String> = withContext(Dispatchers.IO) {
        require(!File(directory, "espeak-ng-data").exists()) { "Unsupported voice pack" }
        val expected = files()
        require(directory.listFiles().orEmpty().none { it.name !in expected && it.name != "manifest.json" })
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
}

/** Keep the checker and native input identical; unsupported text goes intact to the phone engine. */
internal fun kokoroModelText(text: String): String = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC)
    .replace('’', '\'')
    .replace('“', ' ').replace('”', ' ').replace('"', ' ')
    .replace('(', ' ').replace(')', ' ').replace('[', ' ').replace(']', ' ')
    .replace('—', '-').replace('–', '-')

/** Match the pinned runtime's ASCII space/punctuation boundaries conservatively, including edge apostrophes. */
internal fun isKokoroTextCovered(text: String, words: Set<String>): Boolean {
    val normalized = kokoroModelText(text).lowercase(Locale.US)
    if (normalized.any { it.code > 127 } ||
        Regex("[0-9@]|https?://|www\\.", RegexOption.IGNORE_CASE).containsMatchIn(normalized)
    ) {
        return false
    }
    val tokens = normalized.split(Regex("[ \\t\\r\\n,.!?;:]+")).filter(String::isNotBlank)
    return tokens.isNotEmpty() && tokens.all { it in words }
}
