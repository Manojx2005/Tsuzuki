package eu.kanade.tachiyomi.tsuzuki.anime.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.preview.EpisodePreviewMode
import eu.kanade.tachiyomi.tsuzuki.anime.ui.supportsAnimePictureInPicture
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

/**
 * AP11 (owner 2026-11-23: "standard Settings layout"): the anime player settings as ordinary preference groups, like
 * every other Settings page, instead of the A4.1 card grid. Values that live in [AnimePlayerSettings] go through
 * [AnimePlayerPreferences.set] so its bounds still apply; the rest bind their stored preference directly.
 */
internal object AnimePlayerSettingsScreen : SearchableSettings {
    @ReadOnlyComposable
    @Composable
    override fun getTitleRes(): StringResource = MR.strings.tsz_anime_player_settings

    @Composable
    override fun Content() {
        if (!TsuzukiAnime.enabled) return
        super.Content()
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val preferences = remember { AnimePlayerPreferences(Injekt.get()) }
        var settings by remember { mutableStateOf(preferences.get()) }
        var languageKind by remember { mutableStateOf<AnimeTrackKind?>(null) }
        val supportsPip = LocalContext.current.supportsAnimePictureInPicture()
        val subtitleLanguages by preferences.languages(AnimeTrackKind.SUBTITLE).collectAsState()
        val audioLanguages by preferences.languages(AnimeTrackKind.AUDIO).collectAsState()

        // Read the stored values again before writing: the switch rows below write their preference directly, so
        // the remembered copy can be behind them.
        fun update(change: (AnimePlayerSettings) -> AnimePlayerSettings) {
            preferences.set(change(preferences.get()))
            settings = preferences.get()
        }
        fun updateStyle(change: (AnimeSubtitleStyle) -> AnimeSubtitleStyle) =
            update { it.copy(subtitleStyle = change(it.subtitleStyle).bounded()) }

        languageKind?.let { kind ->
            val preference = preferences.languages(kind)
            AnimeLanguageOrderDialog(
                kind = kind,
                initial = animeLanguageOrder(preference.get(), kind),
                onDismiss = { languageKind = null },
                onSave = {
                    preference.set(it.joinToString(","))
                    languageKind = null
                },
            )
        }

        val style = settings.subtitleStyle
        val off = stringResource(R.string.tsz_anime_player_track_off)
        val context = androidx.compose.ui.platform.LocalContext.current
        val players = androidx.compose.runtime.remember { installedAnimeVideoPlayers(context) }
        val chosenPlayer = preferences.externalPlayer.get()
        val playWith = buildMap {
            put(AnimeExternalChoice.Tsuzuki.stored, stringResource(R.string.tsz_anime_external_tsuzuki))
            put(AnimeExternalChoice.Ask.stored, stringResource(R.string.tsz_anime_external_ask))
            players.forEach { (packageName, label) -> put(packageName, label) }
            // A saved app that was uninstalled stays listed (and falls back to the chooser when played).
            if (chosenPlayer !in this) put(chosenPlayer, stringResource(R.string.tsz_anime_external_uninstalled, chosenPlayer))
        }
        return listOf(
            Preference.PreferenceGroup(
                title = mokoStringResource(MR.strings.tsz_tts_playback_settings),
                preferenceItems = listOfNotNull(
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.externalPlayer,
                        entries = playWith,
                        title = stringResource(R.string.tsz_anime_external_play_with),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.autoplay,
                        title = stringResource(R.string.tsz_anime_player_autoplay),
                    ),
                    Preference.PreferenceItem.BasicListPreference(
                        value = settings.speed.toString(),
                        entries = animePlaybackSpeeds.associate { it.toString() to formatAnimeSpeed(it) },
                        title = stringResource(R.string.tsz_anime_player_speed),
                        onValueChanged = { value ->
                            value.toFloatOrNull()?.let { speed -> update { it.copy(speed = speed) } }
                        },
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.episodePreviews,
                        entries = EpisodePreviewMode.entries.associateWith { mode ->
                            when (mode) {
                                EpisodePreviewMode.ON -> stringResource(R.string.tsz_anime_previews_on)
                                EpisodePreviewMode.WIFI_ONLY -> stringResource(R.string.tsz_anime_previews_wifi)
                                EpisodePreviewMode.OFF -> off
                            }
                        },
                        title = stringResource(R.string.tsz_anime_episode_previews),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.tsz_anime_aniskip),
                preferenceItems = listOfNotNull(
                    Preference.PreferenceItem.BasicListPreference(
                        value = settings.aniSkipMode.name,
                        entries = AniSkipMode.entries.associate { mode ->
                            mode.name to when (mode) {
                                AniSkipMode.OFF -> off
                                AniSkipMode.BUTTON -> stringResource(R.string.tsz_anime_skip_button)
                                AniSkipMode.AUTO -> stringResource(R.string.tsz_anime_skip_auto)
                            }
                        },
                        title = stringResource(R.string.tsz_anime_aniskip),
                        onValueChanged = { value ->
                            AniSkipMode.entries.firstOrNull { it.name == value }
                                ?.let { mode -> update { it.copy(aniSkipMode = mode) } }
                        },
                    ),
                    if (settings.aniSkipMode != AniSkipMode.OFF) {
                        Preference.PreferenceItem.SwitchPreference(
                            preference = preferences.aniSkipIgnoreWithChapters,
                            title = stringResource(R.string.tsz_anime_aniskip_ignore_chapters),
                        )
                    } else {
                        null
                    },
                    if (settings.aniSkipMode == AniSkipMode.AUTO) {
                        Preference.PreferenceItem.SliderPreference(
                            value = settings.aniSkipIntroDelay,
                            title = stringResource(R.string.tsz_anime_skip_intro_delay),
                            valueString = stringResource(R.string.tsz_anime_skip_seconds, settings.aniSkipIntroDelay),
                            valueRange = 0..30,
                            onValueChanged = { delay -> update { it.copy(aniSkipIntroDelay = delay) } },
                        )
                    } else {
                        null
                    },
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.tsz_anime_player_subtitles),
                preferenceItems = listOfNotNull(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.tsz_anime_player_preferred_languages),
                        subtitle = languageSummary(subtitleLanguages, AnimeTrackKind.SUBTITLE, off),
                        onClick = { languageKind = AnimeTrackKind.SUBTITLE },
                    ),
                    // Stored as a scale in tenths (0.5–2.0); the slider works in whole steps.
                    Preference.PreferenceItem.SliderPreference(
                        value = Math.round(settings.subtitleScale * 10),
                        title = stringResource(R.string.tsz_anime_player_subtitle_size),
                        valueString = formatAnimePercent(settings.subtitleScale),
                        valueRange = 5..20,
                        onValueChanged = { tenths -> update { it.copy(subtitleScale = tenths / 10f) } },
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        value = style.fontSize / AnimeSubtitleStyle.FONT_SIZE_STEP,
                        title = stringResource(R.string.tsz_anime_player_sub_font_size),
                        valueString = style.fontSize.toString(),
                        valueRange = AnimeSubtitleStyle.FONT_SIZES.first / AnimeSubtitleStyle.FONT_SIZE_STEP..
                            AnimeSubtitleStyle.FONT_SIZES.last / AnimeSubtitleStyle.FONT_SIZE_STEP,
                        onValueChanged = { step ->
                            updateStyle { it.copy(fontSize = step * AnimeSubtitleStyle.FONT_SIZE_STEP) }
                        },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.subtitleStyle.bold,
                        title = stringResource(R.string.tsz_anime_player_sub_bold),
                    ),
                    Preference.PreferenceItem.BasicListPreference(
                        value = style.color.name,
                        entries = AnimeSubtitleColor.entries.associate { it.name to stringResource(it.label) },
                        title = stringResource(R.string.tsz_anime_player_sub_color),
                        onValueChanged = { value ->
                            AnimeSubtitleColor.entries.firstOrNull { it.name == value }
                                ?.let { color -> updateStyle { it.copy(color = color) } }
                        },
                    ),
                    Preference.PreferenceItem.BasicListPreference(
                        value = style.border.name,
                        entries = AnimeSubtitleBorder.entries.associate { it.name to stringResource(it.label) },
                        title = stringResource(R.string.tsz_anime_player_sub_border),
                        onValueChanged = { value ->
                            AnimeSubtitleBorder.entries.firstOrNull { it.name == value }
                                ?.let { border -> updateStyle { it.copy(border = border) } }
                        },
                    ),
                    if (style.border != AnimeSubtitleBorder.NONE) {
                        Preference.PreferenceItem.SliderPreference(
                            value = style.borderSize,
                            title = stringResource(R.string.tsz_anime_player_sub_border_size),
                            valueRange = AnimeSubtitleStyle.BORDER_SIZES,
                            onValueChanged = { size -> updateStyle { it.copy(borderSize = size) } },
                        )
                    } else {
                        null
                    },
                    Preference.PreferenceItem.SliderPreference(
                        value = style.position / AnimeSubtitleStyle.POSITION_STEP,
                        title = stringResource(R.string.tsz_anime_player_sub_position),
                        valueString = "${style.position}%",
                        valueRange = AnimeSubtitleStyle.POSITIONS.first / AnimeSubtitleStyle.POSITION_STEP..
                            AnimeSubtitleStyle.POSITIONS.last / AnimeSubtitleStyle.POSITION_STEP,
                        onValueChanged = { step ->
                            updateStyle { it.copy(position = step * AnimeSubtitleStyle.POSITION_STEP) }
                        },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.subtitleStyle.background,
                        title = stringResource(R.string.tsz_anime_player_sub_background),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.subtitleStyle.overrideAss,
                        title = stringResource(R.string.tsz_anime_player_sub_override_ass),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.tsz_anime_player_audio),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.tsz_anime_player_preferred_languages),
                        subtitle = languageSummary(audioLanguages, AnimeTrackKind.AUDIO, off),
                        onClick = { languageKind = AnimeTrackKind.AUDIO },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.tsz_anime_player_video),
                preferenceItems = listOfNotNull(
                    if (supportsPip) {
                        Preference.PreferenceItem.SwitchPreference(
                            preference = preferences.pictureInPicture,
                            title = stringResource(R.string.tsz_anime_player_picture_in_picture),
                        )
                    } else {
                        null
                    },
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.hardwareDecoding,
                        title = stringResource(R.string.tsz_anime_player_decoder_hardware),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.tsz_anime_player_gestures),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.seekGesture,
                        title = stringResource(R.string.tsz_anime_player_seek_gesture),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.brightnessGesture,
                        title = stringResource(R.string.tsz_anime_player_brightness),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = preferences.volumeGesture,
                        title = stringResource(R.string.tsz_anime_player_volume),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.tsz_anime_watching),
                preferenceItems = listOf(
                    // Aniyomi parity (owner 2026-11-28): another release of the same episode counts as watched.
                    Preference.PreferenceItem.SwitchPreference(
                        preference = remember {
                            eu.kanade.tachiyomi.tsuzuki.anime.animeMarkDuplicatesSeen(Injekt.get())
                        },
                        title = stringResource(R.string.tsz_anime_mark_duplicates_seen),
                        subtitle = stringResource(R.string.tsz_anime_mark_duplicates_seen_summary),
                    ),
                ),
            ),
        )
    }
}

/** "English, Japanese" for a stored language order; null (no subtitle line) when none is set. */
private fun languageSummary(value: String, kind: AnimeTrackKind, off: String): String? =
    animeLanguageOrder(value, kind)
        .map { code ->
            if (code == AnimePlayerPreferences.SUBTITLES_OFF) {
                off
            } else {
                Locale.forLanguageTag(code).getDisplayName(Locale.getDefault()).ifBlank { code }
            }
        }
        .takeIf { it.isNotEmpty() }
        ?.joinToString()

private val AnimeSubtitleColor.label: Int
    get() = when (this) {
        AnimeSubtitleColor.WHITE -> R.string.tsz_anime_player_sub_white
        AnimeSubtitleColor.YELLOW -> R.string.tsz_anime_player_sub_yellow
        AnimeSubtitleColor.CYAN -> R.string.tsz_anime_player_sub_cyan
        AnimeSubtitleColor.GREEN -> R.string.tsz_anime_player_sub_green
    }

private val AnimeSubtitleBorder.label: Int
    get() = when (this) {
        AnimeSubtitleBorder.BLACK -> R.string.tsz_anime_player_sub_black
        AnimeSubtitleBorder.GREY -> R.string.tsz_anime_player_sub_grey
        AnimeSubtitleBorder.NONE -> R.string.tsz_anime_player_sub_none
    }
