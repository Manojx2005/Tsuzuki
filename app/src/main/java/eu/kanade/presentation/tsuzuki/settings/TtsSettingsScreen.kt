package eu.kanade.presentation.tsuzuki.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

/** System voices are usable now. Neural voices remain behind their separate runtime/listening gate. */
internal object TtsSettingsScreen : SearchableSettings {
    override val supportsReset = true

    @Composable
    override fun getTitleRes() = MR.strings.tsz_tts_settings

    // Settings search must not bind a speech engine or play audio while indexing preferences.
    @Composable
    override fun getPreferences() = ttsSettingsPreferences()

    @Composable
    override fun Content() {
        val state = rememberPhoneSpeech()
        val back = LocalBackPress.current
        PreferenceScaffold(
            titleRes = getTitleRes(),
            onBackPressed = back?.let { { it() } },
            actions = { AppBarAction() },
            itemsProvider = { ttsSettingsPreferences(state) },
        )
    }
}

private class PhoneSpeech {
    var engine: TextToSpeech? = null
    var ready by mutableStateOf(false)
    var failed by mutableStateOf(false)
    var voices by mutableStateOf<List<Voice>>(emptyList())
    var engineName by mutableStateOf("")
    var defaultLocale by mutableStateOf<Locale?>(null)
}

@Composable
private fun rememberPhoneSpeech(): PhoneSpeech {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state = remember(context) { PhoneSpeech() }
    DisposableEffect(context, lifecycle) {
        var generation = 0
        fun release() {
            generation++
            state.ready = false
            state.engine?.stop()
            state.engine?.shutdown()
            state.engine = null
        }
        fun load() {
            release()
            state.failed = false
            state.voices = emptyList()
            state.engineName = ""
            state.defaultLocale = null
            val current = generation
            try {
                state.engine = TextToSpeech(context) { status ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (generation == current) {
                            val engine = state.engine
                            state.ready = status == TextToSpeech.SUCCESS && engine != null
                            state.failed = !state.ready
                            if (state.ready && engine != null) {
                                try {
                                    state.voices =
                                        eu.kanade.tachiyomi.tsuzuki.tts.installedPhoneVoices(engine.voices.orEmpty())
                                    state.defaultLocale = engine.defaultVoice?.locale
                                    state.engineName =
                                        engine.engines.firstOrNull { it.name == engine.defaultEngine }?.label.orEmpty()
                                } catch (_: Exception) {
                                    state.ready = false
                                    state.failed = true
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                state.failed = true
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> load()
                Lifecycle.Event.ON_PAUSE -> release()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            release()
        }
    }
    return state
}

@Composable
internal fun TtsSettingsPreferences() = ttsSettingsPreferences(rememberPhoneSpeech())

@Composable
private fun ttsSettingsPreferences(speech: PhoneSpeech? = null): List<Preference> {
    val context = LocalContext.current
    val prefs = remember { Injekt.get<ReaderPreferences>() }
    val speed by prefs.novelTtsSpeed.collectAsState()
    val pitch by prefs.novelTtsPitch.collectAsState()
    val selectedVoice by prefs.novelTtsVoice.collectAsState()
    var language by remember { mutableStateOf("") }
    val offline = stringResource(MR.strings.tsz_tts_voice_offline)
    val online = stringResource(MR.strings.tsz_tts_voice_online)
    val automatic = stringResource(MR.strings.tsz_tts_automatic_voice)
    val unavailable = stringResource(MR.strings.tsz_tts_voice_unavailable)
    val defaultLocale = context.resources.configuration.locales[0]
    val voices = speech?.voices.orEmpty()
    val entries = linkedMapOf("" to automatic).apply {
        voices.filter { language.isBlank() || it.locale.language == language || it.name == selectedVoice }.forEach {
            put(
                it.name,
                "${it.locale.getDisplayName(
                    defaultLocale,
                )} · ${it.name} · ${if (it.isNetworkConnectionRequired) online else offline}",
            )
        }
    }
    val languages = linkedMapOf("" to stringResource(MR.strings.tsz_tts_all_languages)).apply {
        voices.map {
            it.locale.language
        }.distinct().sorted().forEach { put(it, Locale.forLanguageTag(it).getDisplayLanguage(defaultLocale)) }
    }
    val sample = eu.kanade.tachiyomi.tsuzuki.tts.ttsPreviewText(
        voices.firstOrNull { it.name == selectedVoice }?.locale ?: speech?.defaultLocale ?: defaultLocale,
    )
    fun systemSettings() {
        try {
            context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (_: ActivityNotFoundException) {
                context.toast(unavailable)
            }
        }
    }
    return listOf(
        Preference.PreferenceGroup(
            title = stringResource(MR.strings.tsz_tts_phone_voice),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.tsz_tts_engine_settings),
                    subtitle = listOfNotNull(
                        speech?.engineName?.takeIf(String::isNotBlank),
                        stringResource(MR.strings.tsz_tts_engine_summary),
                    ).joinToString(" · "),
                    onClick = ::systemSettings,
                ),
                Preference.PreferenceItem.BasicListPreference(
                    value = language,
                    entries = languages,
                    title = stringResource(MR.strings.tsz_tts_filter_language),
                    enabled = speech?.ready == true,
                    onValueChanged = { language = it },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = prefs.novelTtsVoice,
                    entries = entries,
                    title = stringResource(TDMR.strings.pref_novel_tts_voice),
                    subtitleProvider = { value, options -> options[value] ?: unavailable },
                    enabled = speech?.ready == true,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.tsz_tts_play_sample),
                    subtitle = sample,
                    enabled = speech?.ready == true,
                    onClick = {
                        speech?.engine?.let { engine ->
                            val voice = voices.firstOrNull { it.name == prefs.novelTtsVoice.get() }
                            val voiceReady = if (voice != null) {
                                engine.setVoice(voice) == TextToSpeech.SUCCESS
                            } else if (prefs.novelTtsVoice.get().isBlank()) {
                                engine.defaultVoice?.let { engine.setVoice(it) == TextToSpeech.SUCCESS } ?: false
                            } else {
                                false
                            }
                            engine.setSpeechRate(prefs.novelTtsSpeed.get())
                            engine.setPitch(prefs.novelTtsPitch.get())
                            if (!voiceReady ||
                                engine.speak(sample, TextToSpeech.QUEUE_FLUSH, null, "tsuzuki-voice-sample") ==
                                TextToSpeech.ERROR
                            ) {
                                context.toast(unavailable)
                            }
                        }
                    },
                ),
                Preference.PreferenceItem.InfoPreference(
                    title = when {
                        speech?.failed == true -> unavailable
                        speech != null && !speech.ready -> stringResource(MR.strings.loading)
                        else -> stringResource(MR.strings.tsz_tts_engine_reopen)
                    },
                ),
            ),
        ),
        Preference.PreferenceGroup(
            title = stringResource(MR.strings.tsz_tts_playback_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.SliderPreference(
                    value = (speed * 10).toInt(),
                    valueRange = 1..30,
                    title = stringResource(TDMR.strings.pref_novel_tts_speed),
                    valueString = "$speed×",
                    preference = prefs.novelTtsSpeed,
                    onValueChanged = { prefs.novelTtsSpeed.set(it / 10f) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = (pitch * 10).toInt(),
                    valueRange = 1..30,
                    title = stringResource(TDMR.strings.pref_novel_tts_pitch),
                    valueString = "$pitch×",
                    preference = prefs.novelTtsPitch,
                    onValueChanged = { prefs.novelTtsPitch.set(it / 10f) },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    prefs.novelTtsAutoNextChapter,
                    title = stringResource(TDMR.strings.pref_novel_tts_auto_next),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    prefs.novelTtsBackgroundPlayback,
                    title = stringResource(TDMR.strings.pref_novel_tts_background),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    prefs.novelTtsAutoStartOnPanelOpen,
                    title = stringResource(TDMR.strings.pref_novel_tts_panel_autostart),
                ),
            ),
        ),
        Preference.PreferenceGroup(
            title = stringResource(MR.strings.tsz_tts_read_along),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    prefs.novelTtsEnableHighlight,
                    title = stringResource(TDMR.strings.pref_novel_tts_highlight),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    prefs.novelTtsKeepHighlightInView,
                    title = stringResource(TDMR.strings.pref_novel_tts_highlight_keep_visible),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = prefs.novelTtsHighlightStyle,
                    title = stringResource(TDMR.strings.pref_novel_tts_highlight_style),
                    entries = mapOf(
                        "background" to stringResource(TDMR.strings.novel_tts_highlight_style_background),
                        "underline" to stringResource(TDMR.strings.novel_tts_highlight_style_underline),
                        "outline" to stringResource(TDMR.strings.novel_tts_highlight_style_outline),
                    ),
                ),
            ),
        ),
    )
}
