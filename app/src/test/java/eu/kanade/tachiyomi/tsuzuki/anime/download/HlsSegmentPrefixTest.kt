package eu.kanade.tachiyomi.tsuzuki.anime.download

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HlsSegmentPrefixTest {
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(62) { 1 }

    private fun ts(packets: Int) = ByteArray(packets * 188) { if (it % 188 == 0) 0x47 else 7 }

    @Test
    fun `an image before the transport stream is found`() {
        assertEquals(png.size, HlsSegmentPrefix.payloadOffset(png + ts(4)))
    }

    @Test
    fun `plain segments and non-images are left alone`() {
        assertNull(HlsSegmentPrefix.payloadOffset(ts(4)))
        assertNull(HlsSegmentPrefix.payloadOffset(byteArrayOf(1, 2, 3, 4) + ts(4)))
        assertNull(HlsSegmentPrefix.payloadOffset(png + ByteArray(1000)))
    }

    @Test
    fun `strip rewrites the file and a folder is repaired once`(@TempDir dir: File) {
        val segment = File(dir, "a.ts").apply { writeBytes(png + ts(5)) }
        val plain = File(dir, "b.ts").apply { writeBytes(ts(5)) }
        val playlist = File(dir, "index.m3u8").apply { writeText("#EXTM3U\n#EXTINF:4,\na.ts\n#EXTINF:4,\nb.ts\n") }
        HlsSegmentPrefix.repairFolder(dir, playlist)
        assertArrayEquals(ts(5), segment.readBytes())
        assertArrayEquals(ts(5), plain.readBytes())
        assertTrue(File(dir, ".segments-clean").isFile)
        segment.writeBytes(png + ts(5))
        HlsSegmentPrefix.repairFolder(dir, playlist)
        assertFalse(segment.readBytes().contentEquals(ts(5)), "the marker skips a second pass")
    }
}
