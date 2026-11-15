package eu.kanade.tachiyomi.tsuzuki.anime

import android.util.Log
import eu.kanade.tachiyomi.animesource.model.Video
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import okhttp3.Headers
import org.junit.jupiter.api.Test

class AnimeLogTest {
    @Test
    fun `mpv warnings and errors always log and verbose lines only in debug builds`() {
        mpvLogPriority(10, debugBuild = false) shouldBe Log.ERROR
        mpvLogPriority(20, debugBuild = false) shouldBe Log.ERROR
        mpvLogPriority(30, debugBuild = false) shouldBe Log.WARN
        mpvLogPriority(40, debugBuild = false) shouldBe null
        mpvLogPriority(50, debugBuild = false) shouldBe null
        mpvLogPriority(50, debugBuild = true) shouldBe Log.DEBUG
        mpvLogPriority(60, debugBuild = true) shouldBe null
        mpvLogPriority(0, debugBuild = true) shouldBe null
    }

    @Test
    fun `mpv lines keep the prefix but lose url paths and query strings`() {
        val line = formatMpvLog("cplayer", "Failed to open https://cdn.example.org/v/ep1.m3u8?token=abc123&e=9.\n")
        line shouldContain "mpv/cplayer:"
        line shouldContain "cdn.example.org"
        line shouldNotContain "abc123"
        line shouldNotContain "ep1.m3u8"
    }

    @Test
    fun `video description names the host and header names but never values or the path`() {
        val video = Video(
            videoUrl = "https://cdn.example.org/secret/path/index.m3u8?sig=zzz",
            videoTitle = "720p",
            resolution = 720,
            headers = Headers.headersOf("Referer", "https://site.example/", "Cookie", "session=topsecret"),
        )
        val text = describeAnimeVideo(video)
        text shouldContain "host=cdn.example.org"
        text shouldContain "resolution=720"
        text shouldContain "headers=[Cookie,Referer]"
        text shouldNotContain "topsecret"
        text shouldNotContain "zzz"
        text shouldNotContain "secret/path"
    }

    @Test
    fun `a video without a url or headers is described without failing`() {
        describeAnimeVideo(Video()) shouldContain "headers=[-]"
    }

    @Test
    fun `error description lists the cause chain with redacted messages and the root stack`() {
        val root = NoSuchMethodError("No static method get\$default in Lfoo/Requests;")
        val wrapped = IllegalStateException("Incompatible anime source for https://x.example/a?token=t0p", root)
        val text = describeAnimeError(wrapped)
        text shouldContain "java.lang.IllegalStateException"
        text shouldContain "java.lang.NoSuchMethodError"
        text shouldContain "x.example"
        text shouldNotContain "t0p"
        text shouldContain "    at "
    }

    @Test
    fun `a self-referencing cause does not loop`() {
        val error = object : RuntimeException("boom") {
            override val cause: Throwable get() = this
        }
        describeAnimeError(error) shouldContain "boom"
    }

    @Test
    fun `stream and no-video failures carry readable messages`() {
        AnimeStreamException("loading failed").message shouldBe "The video stream could not be played: loading failed"
        AnimeStreamException(null).message shouldBe "The video stream could not be played"
        AnimeNoPlayableVideoException().message shouldBe "The source returned no playable video"
    }
}
