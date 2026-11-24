package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.compose.runtime.saveable.SaverScope
import eu.kanade.tachiyomi.animesource.model.SAnime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimeOpenedDetailsTest {
    private val scope = SaverScope { true }

    private fun save(value: AnimeOpenedDetails?): Any? = with(AnimeOpenedDetails.Saver) { scope.save(value) }

    @Test
    fun roundTripsTheOpenedAnimeThroughBundleTypes() {
        val opened = AnimeOpenedDetails("/anime/1", "Title", "https://example.org/c.jpg")
        val saved = save(opened)
        assertTrue(saved is ArrayList<*>)
        assertTrue((saved as List<*>).all { it == null || it is String })
        assertEquals(opened, AnimeOpenedDetails.Saver.restore(saved))
    }

    @Test
    fun keepsAMissingCover() {
        val opened = AnimeOpenedDetails("/anime/2", "No cover", null)
        assertEquals(opened, AnimeOpenedDetails.Saver.restore(save(opened)!!))
    }

    @Test
    fun savesNothingWhenNoDetailsWereOpened() {
        assertNull(save(null))
    }

    @Test
    fun ignoresUnreadableState() {
        assertNull(AnimeOpenedDetails.restore("not a list"))
        assertNull(AnimeOpenedDetails.restore(listOf(1L, "title")))
        assertNull(AnimeOpenedDetails.restore(emptyList<String>()))
    }

    @Test
    fun rebuildsAnUninitializedAnimeWithTheSameIdentity() {
        val tapped = SAnime.create().apply {
            url = "/anime/3"
            title = "Shown"
            thumbnail_url = "cover"
            description = "not saved"
            initialized = true
        }
        val rebuilt = AnimeOpenedDetails.of(tapped).toSAnime()
        assertEquals("/anime/3", rebuilt.url)
        assertEquals("Shown", rebuilt.title)
        assertEquals("cover", rebuilt.thumbnail_url)
        assertNull(rebuilt.description)
        assertFalse(rebuilt.initialized)
    }
}
