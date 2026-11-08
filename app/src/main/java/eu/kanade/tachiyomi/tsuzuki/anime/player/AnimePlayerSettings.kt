package eu.kanade.tachiyomi.tsuzuki.anime.player

import tachiyomi.core.common.preference.PreferenceStore
import java.util.Locale

internal enum class AnimeTrackKind(val mpvType: String) {
    SUBTITLE("sub"),
    AUDIO("audio"),
}

internal data class AnimePlayerTrack(
    val id: Int,
    val kind: AnimeTrackKind,
    val language: String?,
    val title: String?,
    val codec: String?,
    val selected: Boolean,
) {
    val preferenceKey: String
        get() = listOf(language.orEmpty().lowercase(Locale.ROOT), title.orEmpty(), codec.orEmpty()).joinToString("|")
}

internal fun preferredAnimeTrack(tracks: List<AnimePlayerTrack>, key: String): AnimePlayerTrack? {
    if (key.isBlank()) return null
    val language = key.substringBefore('|').takeIf(String::isNotBlank)
    return tracks.firstOrNull { it.preferenceKey == key }
        ?: language?.let { preferred -> tracks.firstOrNull { it.language.equals(preferred, ignoreCase = true) } }
}

internal data class AnimePlayerSettings(
    val speed: Float = 1f,
    val subtitleScale: Float = 1f,
    val subtitleDelay: Float = 0f,
    val hardwareDecoding: Boolean = true,
    val seekGesture: Boolean = true,
    val brightnessGesture: Boolean = true,
    val volumeGesture: Boolean = true,
)

internal class AnimePlayerPreferences(store: PreferenceStore) {
    val quality = store.getInt("tsuzuki_anime_player_quality", 0)
    val autoplay = store.getBoolean("tsuzuki_anime_player_autoplay", true)
    val preferredSubtitle = store.getString("tsuzuki_anime_player_subtitle", "")
    val preferredAudio = store.getString("tsuzuki_anime_player_audio", "")
    private val speed = store.getFloat("tsuzuki_anime_player_speed", 1f)
    private val subtitleScale = store.getFloat("tsuzuki_anime_player_subtitle_scale", 1f)
    private val subtitleDelay = store.getFloat("tsuzuki_anime_player_subtitle_delay", 0f)
    private val hardwareDecoding = store.getBoolean("tsuzuki_anime_player_hardware_decode", true)
    private val seekGesture = store.getBoolean("tsuzuki_anime_player_seek_gesture", true)
    private val brightnessGesture = store.getBoolean("tsuzuki_anime_player_brightness_gesture", true)
    private val volumeGesture = store.getBoolean("tsuzuki_anime_player_volume_gesture", true)

    fun get() = AnimePlayerSettings(
        speed = speed.get().bounded(0.25f, 3f, 1f),
        subtitleScale = subtitleScale.get().bounded(0.5f, 2f, 1f),
        subtitleDelay = subtitleDelay.get().bounded(-10f, 10f, 0f),
        hardwareDecoding = hardwareDecoding.get(),
        seekGesture = seekGesture.get(),
        brightnessGesture = brightnessGesture.get(),
        volumeGesture = volumeGesture.get(),
    )

    fun set(settings: AnimePlayerSettings) {
        speed.set(settings.speed.bounded(0.25f, 3f, 1f))
        subtitleScale.set(settings.subtitleScale.bounded(0.5f, 2f, 1f))
        subtitleDelay.set(settings.subtitleDelay.bounded(-10f, 10f, 0f))
        hardwareDecoding.set(settings.hardwareDecoding)
        seekGesture.set(settings.seekGesture)
        brightnessGesture.set(settings.brightnessGesture)
        volumeGesture.set(settings.volumeGesture)
    }

    companion object {
        const val SUBTITLES_OFF = "off"
    }
}

private fun Float.bounded(min: Float, max: Float, fallback: Float): Float =
    if (isFinite()) coerceIn(min, max) else fallback
