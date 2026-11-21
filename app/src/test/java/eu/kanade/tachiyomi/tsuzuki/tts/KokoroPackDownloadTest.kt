package eu.kanade.tachiyomi.tsuzuki.tts

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

class KokoroPackDownloadTest {
    @TempDir
    lateinit var dir: File

    private val bytes = ByteArray(700_000) { (it % 251).toByte() }

    private fun server(requests: MutableList<Long>, code: (Long) -> Int = { if (it > 0) 206 else 200 }) =
        { offset: Long ->
            requests += offset
            val c = code(offset)
            val body = when (c) {
                200 -> bytes
                206 -> bytes.copyOfRange(offset.toInt(), bytes.size)
                else -> ByteArray(0)
            }
            KokoroPackDownload.Opened(c, body.size.toLong(), ByteArrayInputStream(body))
        }

    @Test
    fun `a fresh download writes the whole file and reports the total`() = runTest {
        val target = File(dir, "pack.zip")
        val requests = mutableListOf<Long>()
        var last = 0L to 0L
        KokoroPackDownload(target, server(requests)).run { done, total -> last = done to total }
        assertEquals(listOf(0L), requests)
        assertArrayEquals(bytes, target.readBytes())
        assertEquals(bytes.size.toLong() to bytes.size.toLong(), last)
    }

    @Test
    fun `a partial file resumes with a range request and appends`() = runTest {
        val target = File(dir, "pack.zip").apply { writeBytes(bytes.copyOfRange(0, 300_000)) }
        val requests = mutableListOf<Long>()
        KokoroPackDownload(target, server(requests)).run { _, _ -> }
        assertEquals(listOf(300_000L), requests)
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test
    fun `a server that ignores the range restarts the file instead of appending`() = runTest {
        val target = File(dir, "pack.zip").apply { writeBytes(bytes.copyOfRange(0, 300_000)) }
        KokoroPackDownload(target, server(mutableListOf()) { 200 }).run { _, _ -> }
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test
    fun `an unpublished pack says so and leaves no file`() = runTest {
        val target = File(dir, "pack.zip")
        assertThrows<KokoroPackDownload.NotPublishedException> {
            KokoroPackDownload(target, server(mutableListOf()) { 404 }).run { _, _ -> }
        }
        assertFalse(target.exists())
    }

    @Test
    fun `a body shorter than announced is an error`() = runTest {
        val target = File(dir, "pack.zip")
        val short = { _: Long -> KokoroPackDownload.Opened(200, 1_000, ByteArrayInputStream(ByteArray(10))) }
        assertThrows<IOException> { KokoroPackDownload(target, short).run { _, _ -> } }
    }

    @Test
    fun `space needed covers the zip and the installed pack`() {
        assertEquals(130L + 167L + 16L * 1024 * 1024, KokoroPackDownload.spaceNeeded(130, 167))
    }
}
