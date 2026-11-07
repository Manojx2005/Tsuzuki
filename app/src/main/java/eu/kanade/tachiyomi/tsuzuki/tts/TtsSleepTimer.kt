package eu.kanade.tachiyomi.tsuzuki.tts

internal enum class TtsSleepTimer(val minutes: Int? = null) {
    Off,
    ChapterEnd,
    Fifteen(15),
    Thirty(30),
    Sixty(60),
}
