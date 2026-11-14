package eu.kanade.tachiyomi.tsuzuki.tts

import android.content.Context
import android.icu.util.ULocale
import android.os.Build
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.logcat
import java.lang.Character.UnicodeScript
import java.util.Locale

internal const val TTS_LANGUAGE_SAMPLE_LENGTH = 4096

internal const val TTS_LANGUAGE_MIN_CLASSIFIER_LENGTH = 24

internal suspend fun detectTtsLanguage(context: Context, sample: String, sourceTag: String?): Locale =
    withContext(Dispatchers.IO) {
        val source = sourceTag?.let { Locale.forLanguageTag(it.replace('_', '-')) }
            ?.takeIf { it.language.length == 2 }
        val latinSource = source?.let { ULocale.addLikelySubtags(ULocale.forLocale(it)).script == "Latn" } == true
        val byScript = fallbackTtsLanguage(sample, source, latinSource, Locale.getDefault())
        // Kana and Hangul are decisive, and very short text is too small for the classifier.
        val decisive = byScript.language == "ja" || byScript.language == "ko"
        if (decisive || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            sample.length < TTS_LANGUAGE_MIN_CLASSIFIER_LENGTH
        ) {
            return@withContext byScript
        }
        val detected = try {
            classifyTtsLanguage(context, sample)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            logcat(LogPriority.WARN) { "TTS: Language classification unavailable (${e.javaClass.simpleName})" }
            null
        }
        detected?.let(::ttsLanguageOnly) ?: byScript
    }

@RequiresApi(Build.VERSION_CODES.Q)
private fun classifyTtsLanguage(context: Context, sample: String): Locale? {
    val classifier = context.getSystemService(TextClassificationManager::class.java)?.textClassifier ?: return null
    val result = classifier.detectLanguage(TextLanguage.Request.Builder(sample).build())
    if (result.localeHypothesisCount == 0) return null
    val best = result.getLocale(0)
    return best.toLocale().takeIf { it.language.isNotEmpty() && result.getConfidenceScore(best) >= 0.5f }
}

internal fun fallbackTtsLanguage(text: String, source: Locale?, latinSource: Boolean, device: Locale): Locale {
    val scripts = text.codePoints().iterator().asSequence()
        .filter(Character::isLetter)
        .map(UnicodeScript::of)
        .filter { it != UnicodeScript.COMMON && it != UnicodeScript.INHERITED }
        .groupingBy { it }
        .eachCount()
    val kana = scripts.getOrDefault(UnicodeScript.HIRAGANA, 0) + scripts.getOrDefault(UnicodeScript.KATAKANA, 0)
    val kanaAndHan = kana + scripts.getOrDefault(UnicodeScript.HAN, 0)
    val dominant = scripts.maxByOrNull { it.value }
    val language = when {
        dominant == null -> source ?: device
        kana > 0 && kanaAndHan >= dominant.value -> Locale.JAPANESE
        dominant.key == UnicodeScript.HANGUL -> Locale.KOREAN
        dominant.key == UnicodeScript.HAN -> source?.takeIf { it.language in setOf("zh", "ja", "ko") } ?: Locale.CHINESE
        dominant.key == UnicodeScript.LATIN -> source?.takeIf { latinSource } ?: Locale.ENGLISH
        else -> source ?: device
    }
    return ttsLanguageOnly(language)
}

internal fun ttsLanguageOnly(locale: Locale): Locale = Locale.Builder()
    .setLanguage(locale.language)
    .setScript(locale.script)
    .build()
