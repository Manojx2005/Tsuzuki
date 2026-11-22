package eu.kanade.tachiyomi.tsuzuki.dictionary

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSource
import java.io.IOException

@Serializable
private data class DictionaryPackMetadata(
    val schema: Int,
    val language: String,
    val version: String,
    val sizeBytes: Long,
    val sha256: String,
)

private val manifestJson = Json { ignoreUnknownKeys = true }
private val shaPattern = Regex("[0-9a-f]{64}")

internal fun readManifest(source: BufferedSource): Pair<DictionaryLanguage, DictionaryPackManifest> {
    if (source.request(16_385L)) throw IOException("Dictionary manifest is too large")
    val metadata = try {
        manifestJson.decodeFromString<DictionaryPackMetadata>(source.readUtf8())
    } catch (error: SerializationException) {
        throw IOException("Invalid dictionary manifest", error)
    }
    val language = DictionaryLanguage.entries.firstOrNull { it.code == metadata.language }
    val sha256 = metadata.sha256.lowercase()
    if (metadata.schema != 1 || language == null || metadata.version.isBlank() ||
        metadata.version.length > 80 || metadata.sizeBytes !in 1..(256 * 1024 * 1024L) ||
        !shaPattern.matches(sha256)
    ) {
        throw IOException("Invalid dictionary manifest")
    }
    return language to DictionaryPackManifest(metadata.version, metadata.sizeBytes, sha256)
}
