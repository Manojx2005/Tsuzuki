package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.Video

/** Per-file options, like Aniyomi's PlayerActivity.setVideo; never leak a source's options into the next file. */
internal fun animeMpvLoadCommand(video: Video, resumePosition: Double): Array<String> {
    val start = resumePosition.takeIf { it.isFinite() && it > 0 } ?: 0.0
    val options = (video.mpvArgs + ("start" to start.toString())).joinToString(",") { (name, value) ->
        // The JNI wrapper uses GetStringUTFChars (modified UTF-8, including six-byte surrogate pairs).
        // Length quoting keeps commas, quotes and nested option lists inside this value.
        val bytes = value.sumOf {
            if (it.code in 1..127) {
                1
            } else if (it.code <= 2047) {
                2
            } else {
                3
            }
        }
        "$name=%$bytes%$value"
    }
    return arrayOf("loadfile", video.videoUrl, "replace", "0", options)
}
