package eu.kanade.tachiyomi.tsuzuki

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Every Tsuzuki string (`tsz_` key) ships in all eight app locales, with the same format placeholders
 * as the base text. The test devices run in Japanese, so a missing key shows English on screen and a
 * missing placeholder crashes the formatter.
 */
class TsuzukiLocaleCompletenessTest {

    private val resources = listOf("../i18n", "i18n")
        .map { File(it, "src/commonMain/moko-resources") }
        .first { it.isDirectory }

    private fun strings(locale: String): Map<String, String> {
        val text = File(resources, "$locale/strings.xml").readText()
        val plain = stringPattern.findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
        // Plurals have no single text to compare placeholders against; only presence is checked.
        val plurals = pluralsPattern.findAll(text).associate { it.groupValues[1] to "" }
        return plain + plurals
    }

    private fun placeholders(text: String) = placeholderPattern.findAll(text).map { it.value }.sorted().toList()

    @Test
    fun everyTsuzukiKeyShipsInEveryLocale() {
        val base = strings("base")
        assertTrue(base.size > 100, "base strings not found under $resources")
        for (locale in LOCALES) {
            val missing = base.keys - strings(locale).keys
            assertEquals(emptySet<String>(), missing, "tsz_ keys missing from $locale")
        }
    }

    @Test
    fun translationsKeepTheBasePlaceholders() {
        val base = strings("base")
        for (locale in LOCALES) {
            val translated = strings(locale)
            val wrong = base.filter { (key, text) ->
                key in translated && placeholders(text) != placeholders(translated.getValue(key))
            }.keys
            assertEquals(emptySet<String>(), wrong, "placeholders differ from base in $locale")
        }
    }

    private companion object {
        val LOCALES = listOf("de", "es", "fr", "ja", "ko", "pt-rBR", "zh-rCN")
        val stringPattern = Regex("""<string name="(tsz_[^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        val pluralsPattern = Regex("""<plurals name="(tsz_[^"]+)"""")
        val placeholderPattern = Regex("""%(\d+\$)?[sdf]""")
    }
}
