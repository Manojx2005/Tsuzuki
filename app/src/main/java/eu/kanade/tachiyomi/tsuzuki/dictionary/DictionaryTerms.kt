package eu.kanade.tachiyomi.tsuzuki.dictionary

import java.text.Normalizer

internal enum class DictionaryLanguage(val code: String, val attribution: String, val licenseName: String) {
    English("en", "Princeton University WordNet", "WordNet License"),
    Japanese("ja", "EDRDG JMdict", "CC BY-SA 4.0"),
}

internal data class DictionaryTerm(val value: String, val language: DictionaryLanguage)

private val japaneseRun = Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}々ー]+")
private val latinWord = Regex("[\\p{IsLatin}]+(?:[’'\\-][\\p{IsLatin}]+)*")
private val englishPhrase = Regex("${latinWord.pattern}(?:\\s+${latinWord.pattern}){0,4}")
private val spaces = Regex("\\s+")

internal fun dictionaryTerm(selection: String): DictionaryTerm? {
    val text = Normalizer.normalize(selection.take(256), Normalizer.Form.NFKC)
    val japanese = japaneseRun.find(text)
    val english = englishPhrase.find(text)
    return when {
        japanese != null && (english == null || japanese.range.first < english.range.first) ->
            DictionaryTerm(japanese.value.take(64), DictionaryLanguage.Japanese)
        english != null ->
            DictionaryTerm(
                english.value.lowercase().replace('’', '\'').replace(spaces, " ").take(64),
                DictionaryLanguage.English,
            )
        else -> null
    }
}

private val englishEndings = listOf(
    "ies" to "y", "ches" to "ch", "shes" to "sh", "xes" to "x", "zes" to "z", "ses" to "s",
    "ves" to "f", "ves" to "fe", "s" to "", "ied" to "y", "ed" to "e", "ed" to "",
    "ing" to "e", "ing" to "", "er" to "e", "er" to "", "est" to "e", "est" to "",
)

internal fun englishDictionaryForms(word: String): List<String> = buildList {
    add(word)
    val last = word.substringAfterLast(' ')
    val prefix = word.dropLast(last.length)
    for ((ending, replacement) in englishEndings) {
        if (!last.endsWith(ending) || last.length - ending.length < 2) continue
        val stem = last.dropLast(ending.length)
        add(prefix + stem + replacement)
        if (replacement.isEmpty() && ending in setOf("ed", "ing", "er", "est") &&
            stem.length > 2 && stem.last() == stem[stem.lastIndex - 1]
        ) {
            add(prefix + stem.dropLast(1))
        }
    }
}.distinct()

private val polite = listOf("ませんでした", "ました", "ません", "ます")
private val godanI = mapOf(
    'い' to 'う', 'き' to 'く', 'ぎ' to 'ぐ', 'し' to 'す', 'ち' to 'つ',
    'に' to 'ぬ', 'び' to 'ぶ', 'み' to 'む', 'り' to 'る',
)
private val godanA = mapOf(
    'わ' to 'う', 'か' to 'く', 'が' to 'ぐ', 'さ' to 'す', 'た' to 'つ',
    'な' to 'ぬ', 'ば' to 'ぶ', 'ま' to 'む', 'ら' to 'る',
)
private val japaneseEndings = listOf(
    "くなかった" to listOf("い"), "くない" to listOf("い"), "かった" to listOf("い"),
    "して" to listOf("する", "す"), "した" to listOf("する", "す"),
    "って" to listOf("う", "つ", "る"), "った" to listOf("う", "つ", "る"),
    "んで" to listOf("ぬ", "ぶ", "む"), "んだ" to listOf("ぬ", "ぶ", "む"),
    "いて" to listOf("く"), "いた" to listOf("く"), "いで" to listOf("ぐ"), "いだ" to listOf("ぐ"),
    "なかった" to listOf("る"), "ない" to listOf("る"), "て" to listOf("る"), "た" to listOf("る"),
)
private val irregularJapanese = mapOf(
    "行った" to "行く",
    "行って" to "行く",
    "来た" to "来る",
    "来て" to "来る",
    "きた" to "くる",
    "きて" to "くる",
    "きました" to "くる",
    "こない" to "くる",
)

internal fun japaneseDictionaryForms(word: String): List<String> = buildList {
    add(word)
    irregularJapanese[word]?.let(::add)
    for ((ending, replacements) in japaneseEndings) {
        if (word.length <= ending.length || !word.endsWith(ending)) continue
        replacements.forEach { add(word.dropLast(ending.length) + it) }
    }
    for ((endings, godan) in listOf(polite to godanI, listOf("なかった", "ない") to godanA)) {
        val ending = endings.firstOrNull { word.endsWith(it) && word.length > it.length } ?: continue
        val stem = word.dropLast(ending.length)
        add(stem + "る")
        if (stem.endsWith('し')) add(stem.dropLast(1) + "する")
        godan[stem.last()]?.let { add(stem.dropLast(1) + it) }
    }
}.distinct()
