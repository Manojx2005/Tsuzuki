package eu.kanade.tachiyomi.tsuzuki.dictionary

import okio.Buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException

class DictionaryPackManifestTest {
    private val valid = """
        {"schema":1,"language":"ja","version":"2026-11-15","sizeBytes":100,"sha256":"${"A".repeat(64)}"}
    """.trimIndent()

    @Test
    fun readsLanguageAndNormalizesChecksum() {
        assertEquals(
            DictionaryLanguage.Japanese to DictionaryPackManifest("2026-11-15", 100, "a".repeat(64)),
            readManifest(Buffer().writeUtf8(valid)),
        )
    }

    @Test
    fun rejectsWrongSchemaLanguageSizeAndChecksum() {
        listOf(
            valid.replace("\"schema\":1", "\"schema\":2"),
            valid.replace("\"ja\"", "\"xx\""),
            valid.replace("\"sizeBytes\":100", "\"sizeBytes\":0"),
            valid.replace("A".repeat(64), "A".repeat(63)),
            valid.padEnd(16_385),
            "{",
        ).forEach { manifest ->
            assertThrows(IOException::class.java) { readManifest(Buffer().writeUtf8(manifest)) }
        }
    }
}
