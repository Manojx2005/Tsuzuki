package eu.kanade.tachiyomi.tsuzuki.tts

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.MessageDigest

class KokoroPackTest {
    @TempDir lateinit var root: File

    @Test
    fun validatesActualBytesAndRejectsSameSizeTamperingAndExtraFiles() = runTest {
        val bytes = "hello h e l o\n".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val manifest = """{"files":{"lexicon-us-en.txt":{"size":${bytes.size},"sha256":"$hash"}}}"""
        val assets = mockk<AssetManager> {
            every { open("kokoro-test-manifest.json") } answers { manifest.byteInputStream() }
        }
        val context = mockk<Context> {
            every { filesDir } returns root
            every { this@mockk.assets } returns assets
        }
        val pack = KokoroPack(context)
        assertFalse(pack.isInstalled())
        pack.directory.mkdirs()
        val lexicon = File(pack.directory, "lexicon-us-en.txt")
        lexicon.writeBytes(bytes)
        assertEquals(setOf("hello"), pack.verify())
        lexicon.writeBytes(bytes.clone().apply { this[0] = 'j'.code.toByte() })
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { pack.verify() } }
        lexicon.writeBytes(bytes)
        File(pack.directory, "unexpected.txt").writeText("not allowlisted")
        assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { pack.verify() } }
    }
}
