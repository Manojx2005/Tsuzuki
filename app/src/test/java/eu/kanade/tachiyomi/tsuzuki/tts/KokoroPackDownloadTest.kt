package eu.kanade.tachiyomi.tsuzuki.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
            KokoroPackDownload.Opened(c, body.size.toLong(), ByteArrayInputStream(body), offset.takeIf { c == 206 })
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

    /** Gives [first] bytes, then blocks like a stalled socket until it is closed. */
    private class StalledBody(private val data: ByteArray, private val first: Int) : InputStream() {
        private var position = 0
        private val closed = CountDownLatch(1)
        val firstChunkRead = CountDownLatch(1)

        override fun read(): Int = throw UnsupportedOperationException()

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position < first) {
                val count = minOf(length, first - position)
                data.copyInto(buffer, offset, position, position + count)
                position += count
                if (position == first) firstChunkRead.countDown()
                return count
            }
            if (!closed.await(10, TimeUnit.SECONDS)) error("the stalled read was never released")
            throw IOException("Socket closed")
        }

        override fun close() = closed.countDown()
    }

    @Test
    fun `pause during a stalled read stops at once and writes nothing more; resume completes it`() = runBlocking {
        val target = File(dir, "pack.zip")
        val first = 64 * 1024
        val stalled = StalledBody(bytes, first)
        val reports = mutableListOf<Long>()
        val transfer = launch(Dispatchers.Default) {
            KokoroPackDownload(target) { KokoroPackDownload.Opened(200, bytes.size.toLong(), stalled) }
                .run { done, _ -> synchronized(reports) { reports += done } }
        }
        assertTrue(stalled.firstChunkRead.await(10, TimeUnit.SECONDS))
        // Pause: cancel and wait for the writer to let go of the file (as the dialog does).
        withTimeout(5_000) { transfer.cancelAndJoin() }
        val reportsAtPause = synchronized(reports) { reports.toList() }
        assertEquals(first.toLong(), target.length())
        Thread.sleep(100)
        assertEquals(first.toLong(), target.length())
        assertEquals(reportsAtPause, synchronized(reports) { reports.toList() })

        // Resume: a range request from the paused length finishes the same file.
        val requests = mutableListOf<Long>()
        KokoroPackDownload(target, server(requests)).run { _, _ -> }
        assertEquals(listOf(first.toLong()), requests)
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test
    fun `a 206 for another offset is refused and the partial file dropped`() = runTest {
        val target = File(dir, "pack.zip").apply { writeBytes(bytes.copyOfRange(0, 300_000)) }
        val wrong = { _: Long ->
            KokoroPackDownload.Opened(206, 10, ByteArrayInputStream(ByteArray(10)), rangeStart = 0)
        }
        assertThrows<IOException> { KokoroPackDownload(target, wrong).run { _, _ -> } }
        assertFalse(target.exists())
        val missing = { _: Long -> KokoroPackDownload.Opened(206, 10, ByteArrayInputStream(ByteArray(10))) }
        target.writeBytes(bytes.copyOfRange(0, 300_000))
        assertThrows<IOException> { KokoroPackDownload(target, missing).run { _, _ -> } }
    }

    @Test
    fun `content-range headers give the first byte`() {
        assertEquals(300_000L, KokoroPackDownload.rangeStart("bytes 300000-699999/700000"))
        assertEquals(5L, KokoroPackDownload.rangeStart("bytes 5-9/*"))
        assertEquals(null, KokoroPackDownload.rangeStart(null))
        assertEquals(null, KokoroPackDownload.rangeStart("bytes */700000"))
    }
}
