package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AnimeDataSubtitlesTest {
    private val srt = "1\n00:00:00,000 --> 00:00:02,000\nHello\n"

    @Test
    fun `base64 and plain data URLs decode with the right extension`() {
        val b64 = "data:application/x-subrip;base64," + Base64.getEncoder().encodeToString(srt.toByteArray())
        val decoded = decodeAnimeDataSubtitle(b64)!!
        assertEquals(srt, decoded.bytes.decodeToString())
        assertEquals("srt", decoded.extension)
        val vtt = decodeAnimeDataSubtitle("data:text/vtt,WEBVTT%0A%0A00:00.000%20--%3E%2000:02.000%0AHi")!!
        assertEquals("vtt", vtt.extension)
        assertEquals("WEBVTT\n\n00:00.000 --> 00:02.000\nHi", vtt.bytes.decodeToString())
        assertEquals("ass", decodeAnimeDataSubtitle("data:text/x-ssa,abc")!!.extension)
    }

    @Test
    fun `other schemes and broken data URLs are refused`() {
        assertNull(decodeAnimeDataSubtitle("https://h/en.srt"))
        assertNull(decodeAnimeDataSubtitle("file:///sdcard/en.srt"))
        assertNull(decodeAnimeDataSubtitle("content://x/en.srt"))
        assertNull(decodeAnimeDataSubtitle("data:no-comma"))
        assertNull(decodeAnimeDataSubtitle("data:text/plain;base64,@@@"))
    }

    @Test
    fun `the same subtitle is written once`(@TempDir dir: File) {
        val sub = decodeAnimeDataSubtitle("data:text/vtt,WEBVTT")!!
        val first = writeAnimeDataSubtitle(dir, sub)!!
        val modified = first.lastModified()
        val second = writeAnimeDataSubtitle(dir, sub)
        assertEquals(first, second)
        assertEquals(modified, second!!.lastModified())
        assertEquals(1, dir.listFiles()!!.size)
        assertEquals("vtt", first.extension)
    }

    @Test
    fun `percent escapes keep their bytes`() {
        // ISO-8859-1 "café" and Shift-JIS "字" (8E 9A): not UTF-8, must not become EF BF BD.
        val latin = decodeAnimeDataSubtitle("data:text/plain;charset=iso-8859-1,caf%E9")!!
        assertArrayEquals(byteArrayOf(0x63, 0x61, 0x66, 0xE9.toByte()), latin.bytes)
        val sjis = decodeAnimeDataSubtitle("data:text/plain;charset=shift_jis,%8E%9a")!!
        assertArrayEquals(byteArrayOf(0x8E.toByte(), 0x9A.toByte()), sjis.bytes)
        // Literal characters are UTF-8; '+' stays a plus.
        assertEquals("字幕 a+b", decodeAnimeDataSubtitle("data:,字幕%20a+b")!!.bytes.decodeToString())
        assertNull(decodeAnimeDataSubtitle("data:,bad%E"))
        assertNull(decodeAnimeDataSubtitle("data:,bad%zz"))
    }

    @Test
    fun `a cache that can't be written gives null, and a later write succeeds`(@TempDir root: File) {
        val sub = decodeAnimeDataSubtitle("data:text/vtt,WEBVTT")!!
        // A regular file where the cache directory should be: mkdirs fails.
        val blocked = File(root, "anime-subs").apply { writeText("not a directory") }
        assertNull(writeAnimeDataSubtitle(blocked, sub))
        blocked.delete()
        val written = writeAnimeDataSubtitle(blocked, sub)
        assertNotNull(written)
        assertEquals("WEBVTT", written!!.readText())
        assertTrue(File(blocked, written.name + ".part").exists().not())
    }

    @Test
    fun `a short file left by an earlier failure is rewritten`(@TempDir dir: File) {
        val sub = decodeAnimeDataSubtitle("data:text/vtt,WEBVTT%0A%0A00:00.000%20--%3E%2000:02.000%0AHi")!!
        val first = writeAnimeDataSubtitle(dir, sub)!!
        first.writeText("WEB") // truncated
        assertEquals(sub.bytes.size.toLong(), writeAnimeDataSubtitle(dir, sub)!!.length())
    }

    @Test
    fun `cache failure still returns HTTP subtitles and refuses supplied local paths`(
        @TempDir root: File,
    ) = runBlocking {
        val caller = Thread.currentThread()
        val cache = File(root, "anime-subs").apply { writeText("blocked") }
        val remote = Track("https://example.org/subs/en.vtt", "English")
        val result = prepareAnimeSubtitleTracks(
            listOf(
                Track("data:text/vtt,WEBVTT", "Inline"),
                Track("data:,bad%E", "Broken"),
                Track("file:///sdcard/subs.vtt", "File"),
                Track("content://provider/subs.vtt", "Content"),
                remote,
            ),
        ) {
            assertNotSame(caller, Thread.currentThread())
            cache
        }
        assertEquals(listOf(remote), result)
    }

    @Test
    fun `simultaneous inline requests publish the same complete subtitle`(@TempDir dir: File) = runBlocking {
        val supplied = listOf(Track("data:text/vtt,WEBVTT%0A", "Inline"))
        val first = async { prepareAnimeSubtitleTracks(supplied) { dir } }
        val second = async { prepareAnimeSubtitleTracks(supplied) { dir } }
        val result = first.await().single()
        assertEquals(result, second.await().single())
        assertEquals("WEBVTT\n", File(result.url).readText())
        assertEquals(1, dir.listFiles()!!.size)
    }

    @Test
    fun `canceled preparation does not return a subtitle to the player`(@TempDir dir: File) = runBlocking {
        val began = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        var delivered = false
        val preparation = launch {
            prepareAnimeSubtitleTracks(listOf(Track("data:text/vtt,WEBVTT", "Inline"))) {
                began.complete(Unit)
                check(release.await(5, TimeUnit.SECONDS))
                dir
            }
            delivered = true
        }
        try {
            withTimeout(5_000) { began.await() }
            preparation.cancel()
        } finally {
            release.countDown()
        }
        preparation.join()
        assertFalse(delivered)
    }
}
