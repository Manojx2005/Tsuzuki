package eu.kanade.tachiyomi.tsuzuki.tts

import java.util.Locale

internal fun ttsPreviewText(locale: Locale): String = when (locale.language) {
    "ja" -> "窓の外では雨がやみ、静かな朝が始まっていました。"
    "zh" -> "窗外的雨停了，一个安静的早晨开始了。"
    "ko" -> "창밖의 비가 그치고 조용한 아침이 시작되었습니다."
    "es" -> "La lluvia había cesado y comenzaba una mañana tranquila."
    "fr" -> "La pluie avait cessé et une matinée paisible commençait."
    "de" -> "Der Regen hatte aufgehört und ein ruhiger Morgen begann."
    "hi" -> "बारिश रुक गई थी और एक शांत सुबह शुरू हो रही थी।"
    else -> "The rain had stopped outside the window, and a quiet morning was beginning."
}

internal fun installedPhoneVoices(voices: Set<android.speech.tts.Voice>): List<android.speech.tts.Voice> =
    voices.filterNot {
        android.speech.tts.TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED in it.features.orEmpty()
    }.sortedWith(
        compareBy<android.speech.tts.Voice> { it.locale.toLanguageTag() }
            .thenByDescending { it.quality }
            .thenBy { it.name },
    )
