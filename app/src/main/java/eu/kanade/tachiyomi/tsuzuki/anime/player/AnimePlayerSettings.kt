package eu.kanade.tachiyomi.tsuzuki.anime.player

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
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
    val forced: Boolean = false,
) {
    val preferenceKey: String
        get() = listOf(
            normalizeAnimeLanguage(language) ?: language.orEmpty().lowercase(Locale.ROOT),
            title.orEmpty(),
            codec.orEmpty(),
        ).joinToString("|")
}

internal data class AnimePlayerSettings(
    val speed: Float = 1f,
    val subtitleScale: Float = 1f,
    val subtitleDelay: Float = 0f,
    val hardwareDecoding: Boolean = true,
    val seekGesture: Boolean = true,
    val brightnessGesture: Boolean = true,
    val volumeGesture: Boolean = true,
    val aniSkipMode: AniSkipMode = AniSkipMode.OFF,
    val aniSkipIntroDelay: Int = 3,
)

internal class AnimePlayerPreferences(private val store: PreferenceStore) {
    val episodePreviews = store.getEnum("tsuzuki_anime_episode_previews", eu.kanade.tachiyomi.tsuzuki.anime.preview.EpisodePreviewMode.ON)
    val quality = store.getInt("tsuzuki_anime_player_quality", 0)
    val autoplay = store.getBoolean("tsuzuki_anime_player_autoplay", true)
    val pictureInPicture = store.getBoolean("tsuzuki_anime_player_picture_in_picture", true)
    val preferredSubtitle = store.getString("tsuzuki_anime_player_subtitle", "")
    val preferredAudio = store.getString("tsuzuki_anime_player_audio", "")
    val audioLanguages = store.getString("tsuzuki_anime_player_audio_languages", "")
    val subtitleLanguages = store.getString("tsuzuki_anime_player_subtitle_languages", "")

    fun languages(kind: AnimeTrackKind) = when (kind) {
        AnimeTrackKind.AUDIO -> audioLanguages
        AnimeTrackKind.SUBTITLE -> subtitleLanguages
    }

    fun trackOverride(animeId: Long, kind: AnimeTrackKind) =
        store.getString("tsuzuki_anime_player_track_${animeId}_${kind.mpvType}", "")

    private val speed = store.getFloat("tsuzuki_anime_player_speed", 1f)
    private val subtitleScale = store.getFloat("tsuzuki_anime_player_subtitle_scale", 1f)
    private val subtitleDelay = store.getFloat("tsuzuki_anime_player_subtitle_delay", 0f)
    private val hardwareDecoding = store.getBoolean("tsuzuki_anime_player_hardware_decode", true)
    private val seekGesture = store.getBoolean("tsuzuki_anime_player_seek_gesture", true)
    private val brightnessGesture = store.getBoolean("tsuzuki_anime_player_brightness_gesture", true)
    private val volumeGesture = store.getBoolean("tsuzuki_anime_player_volume_gesture", true)
    private val aniSkipMode = store.getString("tsuzuki_anime_player_aniskip", AniSkipMode.OFF.name)
    private val aniSkipIntroDelay = store.getInt("tsuzuki_anime_player_aniskip_intro_delay", 3)

    fun get() = AnimePlayerSettings(
        speed = speed.get().bounded(0.25f, 3f, 1f),
        subtitleScale = subtitleScale.get().bounded(0.5f, 2f, 1f),
        subtitleDelay = subtitleDelay.get().bounded(-10f, 10f, 0f),
        hardwareDecoding = hardwareDecoding.get(),
        seekGesture = seekGesture.get(),
        brightnessGesture = brightnessGesture.get(),
        volumeGesture = volumeGesture.get(),
        aniSkipMode = AniSkipMode.entries.firstOrNull { it.name == aniSkipMode.get() } ?: AniSkipMode.OFF,
        aniSkipIntroDelay = aniSkipIntroDelay.get().coerceIn(0, 30),
    )

    fun set(settings: AnimePlayerSettings) {
        speed.set(settings.speed.bounded(0.25f, 3f, 1f))
        subtitleScale.set(settings.subtitleScale.bounded(0.5f, 2f, 1f))
        subtitleDelay.set(settings.subtitleDelay.bounded(-10f, 10f, 0f))
        hardwareDecoding.set(settings.hardwareDecoding)
        seekGesture.set(settings.seekGesture)
        brightnessGesture.set(settings.brightnessGesture)
        volumeGesture.set(settings.volumeGesture)
        aniSkipMode.set(settings.aniSkipMode.name)
        aniSkipIntroDelay.set(settings.aniSkipIntroDelay.coerceIn(0, 30))
    }

    companion object {
        const val SUBTITLES_OFF = "off"
    }
}

private fun Float.bounded(min: Float, max: Float, fallback: Float): Float =
    if (isFinite()) coerceIn(min, max) else fallback
