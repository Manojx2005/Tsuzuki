package eu.kanade.tachiyomi.tsuzuki.anime.player

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimeSubtitleTracksTest {
    @Test
    fun `extension labels name their language`() {
        assertEquals("en", normalizeAnimeLanguage("English (SRT)"))
        assertEquals("en", normalizeAnimeLanguage("[Eng] Full"))
        assertEquals("ja", normalizeAnimeLanguage("Japanese - Signs"))
        assertEquals("ja", normalizeAnimeLanguage("日本語 (SRT)"))
        assertEquals("es", normalizeAnimeLanguage("Español"))
        assertEquals("en", normalizeAnimeLanguage("en-US"))
    }

    @Test
    fun `words that only look like codes are not languages`() {
        assertNull(normalizeAnimeLanguage("Full"))
        assertNull(normalizeAnimeLanguage("No subs"))
        assertNull(normalizeAnimeLanguage("Signs & Songs"))
        assertNull(normalizeAnimeLanguage("CC"))
    }

    private fun sub(id: Int, title: String?, language: String?, forced: Boolean = false) =
        AnimePlayerTrack(id, AnimeTrackKind.SUBTITLE, language, title, "subrip", selected = false, forced = forced)

    @Test
    fun `a signs track is not the main subtitle when a full one matches`() {
        val tracks = listOf(
            sub(1, "English (Signs)", "en"),
            sub(2, "English", "en"),
            sub(3, "日本語", "ja"),
        )
        assertEquals(2, chooseAnimeTrack(AnimeTrackKind.SUBTITLE, tracks, null, listOf("en"), "")?.track?.id)
        assertEquals(3, chooseAnimeTrack(AnimeTrackKind.SUBTITLE, tracks, null, listOf("ja", "en"), "")?.track?.id)
    }

    @Test
    fun `a forced track loses to a full one in the same language`() {
        val tracks = listOf(sub(1, null, "en", forced = true), sub(2, null, "en"), sub(3, null, "ja"))
        assertEquals(2, chooseAnimeTrack(AnimeTrackKind.SUBTITLE, tracks, null, listOf("en"), "")?.track?.id)
    }

    @Test
    fun `external tracks carry the label as title and its code as language`() {
        assertArrayEquals(
            arrayOf("https://h/en.srt", "auto", "English (SRT)", "en"),
            externalTrackArgs("https://h/en.srt", "English (SRT)"),
        )
        assertArrayEquals(
            arrayOf("https://h/x.srt", "auto", "Commentary", "Commentary"),
            externalTrackArgs("https://h/x.srt", "Commentary"),
        )
    }

    @Test
    fun `fonts are copied for a new install and not again`() {
        assertTrue(AnimeSubtitleFonts.needsCopy(null, "100", allPresent = false))
        assertTrue(AnimeSubtitleFonts.needsCopy("100", "200", allPresent = true))
        assertTrue(AnimeSubtitleFonts.needsCopy("200", "200", allPresent = false))
        assertFalse(AnimeSubtitleFonts.needsCopy("200", "200", allPresent = true))
    }
}
