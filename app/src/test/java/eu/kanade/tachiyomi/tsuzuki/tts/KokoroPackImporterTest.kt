package eu.kanade.tachiyomi.tsuzuki.tts

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class KokoroPackImporterTest {
    @TempDir lateinit var root: File

    private val lexicon = "hello h e l o\n".toByteArray()
    private val tokens = "h 1\n".toByteArray()

    private fun sha(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private val pack: KokoroPack by lazy {
        val manifest = """{"files":{
            "lexicon-us-en.txt":{"size":${lexicon.size},"sha256":"${sha(lexicon)}"},
            "tokens.txt":{"size":${tokens.size},"sha256":"${sha(tokens)}"}}}"""
        val assets = mockk<AssetManager> {
            every { open("kokoro-test-manifest.json") } answers { manifest.byteInputStream() }
        }
        val context = mockk<Context> {
            every { filesDir } returns root
            every { this@mockk.assets } returns assets
        }
        KokoroPack(context)
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, data) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }

    private val good = arrayOf(
        "kokoro-cmu-v1/lexicon-us-en.txt" to lexicon,
        "kokoro-cmu-v1/tokens.txt" to tokens,
        "kokoro-cmu-v1/manifest.json" to "{}".toByteArray(),
    )

    private fun staging() = File(pack.directory.parentFile, pack.directory.name + ".importing")

    @Test
    fun `a genuine pack installs and replaces the previous install`() = runTest {
        pack.directory.mkdirs()
        File(pack.directory, "stale.bin").writeText("old install")
        KokoroPackImporter(pack).import(zip(*good))
        assertTrue(pack.isInstalled())
        assertEquals(setOf("hello"), pack.verify())
        assertFalse(File(pack.directory, "stale.bin").exists())
        assertFalse(staging().exists())
    }

    @Test
    fun `tampered bytes of the right size are rejected and the previous install survives`() = runTest {
        KokoroPackImporter(pack).import(zip(*good))
        val tampered = lexicon.clone().apply { this[0] = 'j'.code.toByte() }
        val bad = good.map { if (it.first.endsWith("lexicon-us-en.txt")) it.first to tampered else it }
        assertThrows(KokoroPackImporter.RejectedPackException::class.java) {
            kotlinx.coroutines.runBlocking { KokoroPackImporter(pack).import(zip(*bad.toTypedArray())) }
        }
        assertFalse(staging().exists())
        assertEquals(setOf("hello"), pack.verify())
    }

    @Test
    fun `extra files unsafe paths and other folders are rejected`() = runTest {
        val rejected = listOf(
            good.toList() + ("kokoro-cmu-v1/extra.bin" to ByteArray(4)),
            good.toList() + ("kokoro-cmu-v1/espeak-ng-data/en_dict" to ByteArray(4)),
            good.toList() + ("other/lexicon-us-en.txt" to lexicon),
            good.toList() + ("../kokoro-cmu-v1/tokens.txt" to tokens),
            good.toList() + ("kokoro-cmu-v1/../escape.txt" to tokens),
            good.toList() + ("/kokoro-cmu-v1/tokens.txt" to tokens),
            good.toList() + ("kokoro-cmu-v1\\tokens.txt" to tokens),
            good.take(1), // missing files
        )
        for (entries in rejected) {
            assertThrows(KokoroPackImporter.RejectedPackException::class.java) {
                kotlinx.coroutines.runBlocking { KokoroPackImporter(pack).import(zip(*entries.toTypedArray())) }
            }
            assertFalse(staging().exists())
            assertFalse(File(root, "escape.txt").exists())
        }
        assertFalse(pack.isInstalled())
    }

    @Test
    fun `an entry bigger than its manifest size is cut off`() = runTest {
        val oversize = good.map { if (it.first.endsWith("tokens.txt")) it.first to ByteArray(10_000) else it }
        assertThrows(KokoroPackImporter.RejectedPackException::class.java) {
            kotlinx.coroutines.runBlocking { KokoroPackImporter(pack).import(zip(*oversize.toTypedArray())) }
        }
        assertFalse(staging().exists())
    }
}
