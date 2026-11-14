package eu.kanade.tachiyomi.tsuzuki.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DictionaryTermsTest {
    @Test
    fun selectsWordsAtPunctuationAndNormalizesWidthAndCase() {
        assertEquals(DictionaryTerm("steadied", DictionaryLanguage.English), dictionaryTerm("…“ＳＴＥＡＤＩＥＤ!”"))
        assertEquals(DictionaryTerm("食べました", DictionaryLanguage.Japanese), dictionaryTerm("「食べました。」"))
        assertEquals(DictionaryTerm("ゲーム", DictionaryLanguage.Japanese), dictionaryTerm("ｹﾞｰﾑ？"))
        assertEquals(DictionaryTerm("hang on", DictionaryLanguage.English), dictionaryTerm("  Hang   on!"))
        assertNull(dictionaryTerm("?! … 123"))
    }

    @Test
    fun englishSuffixesKeepExactFormFirstAndHandleCommonInflections() {
        assertEquals("steadied", englishDictionaryForms("steadied").first())
        listOf(
            "steadied" to "steady",
            "running" to "run",
            "classes" to "class",
            "wives" to "wife",
            "ice creams" to "ice cream",
        ).forEach { (word, lemma) -> assertTrue(lemma in englishDictionaryForms(word), word) }
    }

    @Test
    fun japaneseCandidatesCoverPoliteNegativePastAndAdjectiveForms() {
        listOf(
            "食べました" to "食べる",
            "書きません" to "書く",
            "行った" to "行く",
            "読んで" to "読む",
            "話さなかった" to "話す",
            "勉強しました" to "勉強する",
            "高くなかった" to "高い",
            "高くて" to "高い",
        ).forEach { (word, lemma) ->
            assertEquals(word, japaneseDictionaryForms(word).first())
            assertTrue(lemma in japaneseDictionaryForms(word), word)
        }
    }
}
