package eu.kanade.tachiyomi.tsuzuki.anime.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AnimeOfflineTracksTest {
    @TempDir
    lateinit var folder: File

    @Test
    fun sidecarRoundTripRebuildsTheVideo() {
        val video = File(folder, "video.mp4").apply { writeText("v") }
        File(folder, "subtitle-0.ass").writeText("s")
        File(folder, "audio-0.m4a").writeText("a")
        AnimeOfflineTracks.write(
            folder,
            AnimeOfflineSidecar(
                subtitles = listOf(AnimeOfflineTrack("subtitle-0.ass", "English")),
                audio = listOf(AnimeOfflineTrack("audio-0.m4a", "Japanese")),
                mpvArgs = listOf(listOf("demuxer-lavf-o", "fflags=+genpts")),
                quality = "1080p",
            ),
        )
        val rebuilt = AnimeOfflineTracks.offlineVideo(video, folder)
        assertEquals(video.absolutePath, rebuilt.videoUrl)
        assertEquals("1080p", rebuilt.videoTitle)
        assertEquals(listOf(File(folder, "subtitle-0.ass").absolutePath to "English"), rebuilt.subtitleTracks.map { it.url to it.lang })
        assertEquals(listOf(File(folder, "audio-0.m4a").absolutePath to "Japanese"), rebuilt.audioTracks.map { it.url to it.lang })
        assertEquals(listOf("demuxer-lavf-o" to "fflags=+genpts"), rebuilt.mpvArgs)
    }

    @Test
    fun missingOrEscapingTrackFilesAreLeftOut() {
        val video = File(folder, "video.mp4").apply { writeText("v") }
        AnimeOfflineTracks.write(
            folder,
            AnimeOfflineSidecar(
                subtitles = listOf(
                    AnimeOfflineTrack("subtitle-0.srt", "gone"),
                    AnimeOfflineTrack("../secret.srt", "escape"),
                ),
            ),
        )
        assertEquals(emptyList<Any>(), AnimeOfflineTracks.offlineVideo(video, folder).subtitleTracks)
    }

    @Test
    fun anEpisodeSavedBeforeAy7PlaysAsBefore() {
        val video = File(folder, "video.mp4").apply { writeText("v") }
        val rebuilt = AnimeOfflineTracks.offlineVideo(video, folder)
        assertEquals(video.absolutePath, rebuilt.videoUrl)
        assertEquals(emptyList<Any>(), rebuilt.subtitleTracks)
    }
}
