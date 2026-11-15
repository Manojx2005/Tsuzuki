package eu.kanade.tachiyomi.tsuzuki.anime.download

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.util.system.NetworkState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.ByteString.Companion.encodeUtf8
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

class AnimeDownloadTest {
    @TempDir
    lateinit var folder: File

    private val host = "https://example.org"
    private val start = "$host/show/episode"
    private val base = start.toHttpUrl()

    @Test
    fun `queue survives serialization and a retry does not duplicate a failed episode`() {
        val entry = AnimeDownloadEntry(1, 2, 3, "Source", "Anime", "Episode")
        val queued = AnimeDownloadQueue(paused = true).enqueue(entry).enqueue(entry)
        assertEquals(listOf(entry), queued.entries)
        val failed = queued.change(3) { it.copy(status = AnimeDownloadStatus.Failed) }
        val restored = Json.decodeFromString<AnimeDownloadQueue>(Json.encodeToString(failed))
        assertTrue(restored.paused)
        assertTrue(restored.wifiOnly)
        assertFalse(restored.hasQueuedEpisodes)
        assertEquals(queued, restored.enqueue(entry))
        assertTrue(restored.change(3) { null }.entries.isEmpty())
    }

    @Test
    fun `all queue states protect their episode rows only for the matching anime`() {
        val entries = AnimeDownloadStatus.entries.mapIndexed { index, status ->
            AnimeDownloadEntry(1, 2, index.toLong(), "Source", "Anime", "Episode", status)
        }
        val other = AnimeDownloadEntry(1, 3, 100, "Source", "Other anime", "Episode")
        val queue = AnimeDownloadQueue(entries + other)
        assertEquals(entries.map { it.episodeId }.toSet(), queue.protectedEpisodeIds(2))
        assertEquals(setOf(100L), queue.protectedEpisodeIds(3))
        assertTrue(queue.protectedEpisodeIds(4).isEmpty())
    }

    @Test
    fun `wifi only requires a validated wifi connection`() {
        val cellular = NetworkState(isConnected = true, isValidated = true, isWifi = false)
        assertFalse(cellular.allowsAnimeDownloads(wifiOnly = true))
        assertTrue(cellular.allowsAnimeDownloads(wifiOnly = false))
        assertFalse(cellular.copy(isWifi = true, isValidated = false).allowsAnimeDownloads(wifiOnly = true))
        assertTrue(cellular.copy(isWifi = true).allowsAnimeDownloads(wifiOnly = true))
    }

    @Test
    fun `master picks a suitable muxed variant`() {
        val master = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="extra",URI="audio.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,AUDIO="extra"
            high.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1280x720
            ../medium.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=1000000,RESOLUTION=854x480
            low.m3u8
        """.trimIndent()
        assertEquals(
            OfflineHls.Parsed.Variant("$host/medium.m3u8".toHttpUrl()),
            OfflineHls.parse(master, base, 1080),
        )
        assertEquals(
            OfflineHls.Parsed.Variant("$host/show/low.m3u8".toHttpUrl()),
            OfflineHls.parse(master, base, 540),
        )
    }

    @Test
    fun `local manifest rewrites all files and keeps byte ranges and keys`() {
        val media = """
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-TARGETDURATION:5
            #EXT-X-KEY:METHOD=AES-128,URI="/key?token=secret",IV=0x1234
            #EXT-X-MAP:URI="init.mp4",BYTERANGE="16@0"
            #EXTINF:5,
            #EXT-X-BYTERANGE:40@16
            segments.bin
            #EXTINF:5,
            #EXT-X-BYTERANGE:30
            segments.bin
            #EXT-X-ENDLIST
        """.trimIndent()
        val hls = (OfflineHls.parse(media, base, 0) as OfflineHls.Parsed.Media).hls
        assertEquals(
            listOf("$host/key?token=secret", "$host/show/init.mp4", "$host/show/segments.bin"),
            hls.resources.values.map { it.toString() },
        )
        assertEquals(3, hls.resources.size)
        assertTrue("BYTERANGE=\"16@0\"" in hls.playlist)
        assertTrue("#EXT-X-BYTERANGE:40@16" in hls.playlist)
        assertFalse("secret" in hls.playlist)
        assertTrue(hls.resources.keys.all { it in hls.playlist })
    }

    @Test
    fun `reject live playlists and unsafe URIs`() {
        assertThrows<UnsupportedAnimeDownload> {
            OfflineHls.parse("#EXTM3U\n#EXTINF:5,\npart.ts", base, 0)
        }
        assertThrows<UnsupportedAnimeDownload> {
            OfflineHls.parse("#EXTM3U\n#EXTINF:5,\nfile:///a.ts\n#EXT-X-ENDLIST", base, 0)
        }
        assertThrows<UnsupportedAnimeDownload> {
            OfflineHls.parse(
                "#EXTM3U\n#EXT-X-KEY:METHOD=SAMPLE-AES,URI=\"key\"\n#EXTINF:5,\na.ts\n#EXT-X-ENDLIST",
                base,
                0,
            )
        }
    }

    @Test
    fun `direct video resumes validated partial data`() = runBlocking {
        val digest = start.encodeUtf8().sha256().hex()
        File(folder, "video.part").writeText("begin")
        File(folder, "video.validator").writeText("$digest\n$digest\n\"version\"")
        val client = client { request ->
            assertEquals("bytes=5-", request.header("Range"))
            assertEquals("\"version\"", request.header("If-Range"))
            response(request, 206, "-finish", "video/mp4")
                .header("Content-Range", "bytes 5-11/12")
                .header("ETag", "\"version\"")
                .build()
        }
        AnimeVideoTransfer(client, Video(videoUrl = start), 0).download(folder) {}
        assertEquals("begin-finish", File(folder, AnimeVideoTransfer.VIDEO).readText())
        assertFalse(File(folder, "video.part").exists())
        assertFalse(File(folder, "video.validator").exists())
    }

    @Test
    fun `cross origin redirects do not forward private source headers`() = runBlocking {
        val cdn = "https://cdn.example.net/video.mp4"
        val client = client { request ->
            if (request.url == base) {
                assertEquals("secret", request.header("X-Source-Token"))
                response(request, 302, "").header("Location", cdn).build()
            } else {
                assertEquals(cdn, request.url.toString())
                assertNull(request.header("X-Source-Token"))
                assertEquals("Test player", request.header("User-Agent"))
                response(request, 200, "video", "video/mp4").build()
            }
        }
        val headers = Headers.headersOf("X-Source-Token", "secret", "User-Agent", "Test player")
        AnimeVideoTransfer(client, Video(videoUrl = start, headers = headers), 0).download(folder) {}
        assertEquals("video", File(folder, AnimeVideoTransfer.VIDEO).readText())
    }

    @Test
    fun `failed HLS transfer publishes no playlist and retries only missing assets`() = runBlocking {
        val playlist = "#EXTM3U\n#EXT-X-TARGETDURATION:5\n#EXTINF:5,\na.ts\n#EXTINF:5,\nb.ts\n#EXT-X-ENDLIST\n"
        val requests = mutableListOf<String>()
        var failing = true
        val client = client { request ->
            val name = request.url.pathSegments.last()
            requests += name
            when (name) {
                "episode" -> response(request, 200, playlist, "application/vnd.apple.mpegurl").build()
                "b.ts" -> response(request, if (failing) 503 else 200, "bbb").build()
                else -> response(request, 200, "aaa").build()
            }
        }
        val transfer = AnimeVideoTransfer(client, Video(videoUrl = start), 0)
        assertTrue(runCatching { transfer.download(folder) {} }.exceptionOrNull() is IOException)
        assertFalse(File(folder, AnimeVideoTransfer.PLAYLIST).exists())
        failing = false
        transfer.download(folder) {}
        assertEquals(listOf("episode", "a.ts", "b.ts", "episode", "b.ts"), requests)
        assertTrue(File(folder, AnimeVideoTransfer.PLAYLIST).isFile)
        assertEquals(3, folder.listFiles()?.size)
    }

    private fun client(block: (Request) -> Response): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain -> block(chain.request()) }
        .build()

    private fun response(request: Request, code: Int, body: String, type: String = "application/octet-stream") =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Fixture")
            .body(body.encodeToByteArray().toResponseBody(type.toMediaType()))
}
