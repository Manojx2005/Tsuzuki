package eu.kanade.tachiyomi.tsuzuki.manhwa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.manga.model.Manga

class ManhwaClassifierTest {
    @Test
    fun recognizesExactFormatTagsAndLocalizedWebtoonTags() {
        listOf(" Manhwa ", "MANHUA", "webtoon", "long-strip", "웹툰", "条漫").forEach { tag ->
            assertTrue(isManhwa(listOf("Action", tag), "en", null, null), tag)
        }
        assertFalse(isManhwa(listOf("Webtoon adaptation"), "en", null, null))
    }

    @Test
    fun sourceLanguageIsWeakerThanPageEvidenceOrAnExplicitMangaTag() {
        assertTrue(isManhwa(null, "ko-KR", null, null))
        assertTrue(isManhwa(null, "zh_CN", null, null))
        assertFalse(isManhwa(null, "ko", false, null))
        assertFalse(isManhwa(listOf("Manga"), "zh", null, null))
        assertFalse(isManhwa(null, "ja", null, null))
        assertTrue(isManhwa(null, "ja", true, null))
    }

    @Test
    fun pageEvidenceRequiresMoreThanHalfOfDistinctChapterPages() {
        val evidence = ManhwaChapterEvidence()
        assertNull(evidence.record(0, 5, 100, 250))
        assertNull(evidence.record(0, 5, 100, 250))
        assertNull(evidence.record(1, 5, 100, 300))
        assertNull(evidence.record(2, 5, 100, 249))
        assertNull(evidence.record(3, 5, 0, 250))
        assertNull(evidence.record(5, 5, 100, 250))
        assertEquals(true, evidence.record(3, 5, 100, 250))
        assertNull(evidence.record(4, 5, 100, 300))
    }

    @Test
    fun ordinaryPagesCanDisproveTheLanguageHint() {
        val evidence = ManhwaChapterEvidence()
        assertNull(evidence.record(0, 2, 100, 200))
        assertEquals(false, evidence.record(1, 2, 100, 249))
    }

    @Test
    fun manualClassificationWinsAndCanBeCleared() {
        val preferences = InMemoryPreferenceStore()
        val store = ManhwaStore(preferences)
        val manga = Manga.create().copy(id = 12, genre = listOf("Webtoon"))
        store.recordPageEvidence(manga.id, true)
        store.setOverride(manga.id, false)
        assertFalse(ManhwaStore(preferences).isManhwa(manga, "ko"))
        store.setOverride(manga.id, null)
        assertTrue(store.isManhwa(manga, "en"))

        val ordinary = Manga.create().copy(id = 13, genre = listOf("Manga"))
        store.recordPageEvidence(ordinary.id, false)
        store.setOverride(ordinary.id, true)
        assertTrue(store.isManhwa(ordinary, "ja"))
        assertFalse(store.isManhwa(ordinary.copy(isNovel = true), "ja"))
        store.setOverride(ordinary.id, null)
        assertFalse(store.isManhwa(ordinary, "ja"))
    }

    @Test
    fun laterLongStripEvidenceSurvivesAnOrdinaryChapter() {
        val store = ManhwaStore(InMemoryPreferenceStore())
        val manga = Manga.create().copy(id = 14)
        store.recordPageEvidence(manga.id, false)
        assertFalse(store.isManhwa(manga, "ko"))
        store.recordPageEvidence(manga.id, true)
        store.recordPageEvidence(manga.id, false)
        assertTrue(store.isManhwa(manga, "en"))
        assertFalse(store.needsPageEvidence(manga.id))
    }
}
