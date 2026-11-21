package eu.kanade.tachiyomi.tsuzuki.anime.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Base64

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
        val first = writeAnimeDataSubtitle(dir, sub)
        val modified = first.lastModified()
        val second = writeAnimeDataSubtitle(dir, sub)
        assertEquals(first, second)
        assertEquals(modified, second.lastModified())
        assertEquals(1, dir.listFiles()!!.size)
        assertEquals("vtt", first.extension)
    }
}
