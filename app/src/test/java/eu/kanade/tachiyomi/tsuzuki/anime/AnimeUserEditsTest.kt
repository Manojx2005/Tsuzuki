package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.model.SAnime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.entries.anime.model.ANIME_USER_EDITS
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUserEdits
import tachiyomi.domain.entries.anime.model.animeUserEdits
import tachiyomi.domain.entries.anime.model.editedAnimeTitle
import tachiyomi.domain.entries.anime.model.withAnimeUserEdits
import tachiyomi.domain.entries.anime.model.withUserEdits

class AnimeUserEditsTest {
    private val source = Anime(
        source = 1,
        url = "/show",
        title = "Source title",
        author = "Source author",
        artist = "Source artist",
        description = "Source description",
        genre = listOf("Action", "Drama"),
        status = SAnime.ONGOING.toLong(),
    )

    @Test
    fun allEditsApplyOverTheSourceValues() {
        val edits = AnimeUserEdits(
            title = "My title",
            author = "My author",
            artist = "My artist",
            description = "My description",
            genres = listOf("Comedy"),
            status = SAnime.COMPLETED.toLong(),
        )
        val shown = source.copy(memo = source.memo.withAnimeUserEdits(edits)).withUserEdits()

        assertEquals("My title", shown.title)
        assertEquals("My author", shown.author)
        assertEquals("My artist", shown.artist)
        assertEquals("My description", shown.description)
        assertEquals(listOf("Comedy"), shown.genre)
        assertEquals(SAnime.COMPLETED.toLong(), shown.status)
    }

    @Test
    fun partialEditsKeepTheOtherSourceFieldsAndBlankTextClearsAField() {
        val edits = AnimeUserEdits(title = "My title", artist = "")
        val shown = source.copy(memo = source.memo.withAnimeUserEdits(edits)).withUserEdits()

        assertEquals("My title", shown.title)
        assertEquals("Source author", shown.author)
        assertNull(shown.artist)
        assertEquals("Source description", shown.description)
        assertEquals(listOf("Action", "Drama"), shown.genre)
        assertEquals(SAnime.ONGOING.toLong(), shown.status)
    }

    @Test
    fun noEditsLeaveTheAnimeAsItIs() {
        assertSame(source, source.withUserEdits())
        assertEquals("Source title", source.memo.editedAnimeTitle(source.title))
    }

    @Test
    fun diffKeepsOnlyChangedFields() {
        val unchanged = AnimeUserEdits.diff(
            source,
            title = " Source title ",
            author = "Source author",
            artist = "Source artist",
            description = "Source description",
            genres = listOf("Action", "Drama"),
            status = SAnime.ONGOING.toLong(),
        )
        assertTrue(unchanged.isEmpty)

        val changed = AnimeUserEdits.diff(
            source,
            title = "",
            author = "Source author",
            artist = "New artist",
            description = "Source description",
            genres = listOf("Action"),
            status = SAnime.ONGOING.toLong(),
        )
        // A blank title is never an edit; only the artist and genres changed.
        assertEquals(AnimeUserEdits(artist = "New artist", genres = listOf("Action")), changed)
    }

    @Test
    fun resetRemovesTheKeyAndKeepsTheRestOfTheMemo() {
        val memo = JsonObject(mapOf("source-key" to JsonPrimitive(1)))
            .withAnimeUserEdits(AnimeUserEdits(title = "My title"))
        assertEquals("My title", memo.animeUserEdits().title)

        val reset = memo.withAnimeUserEdits(null)
        assertNull(reset[ANIME_USER_EDITS])
        assertEquals(JsonPrimitive(1), reset["source-key"])
        assertEquals(reset, memo.withAnimeUserEdits(AnimeUserEdits.NONE))
    }

    @Test
    fun editsRoundTripThroughTheMemo() {
        val edits = AnimeUserEdits(
            title = "T",
            author = "",
            description = "D",
            genres = emptyList(),
            status = 3,
        )
        assertEquals(edits, JsonObject(emptyMap()).withAnimeUserEdits(edits).animeUserEdits())
    }

    @Test
    fun refreshMergeKeepsTheStoredEditsAndAppKeys() {
        val stored = JsonObject(
            mapOf(
                "source-key" to JsonPrimitive("old"),
                ANIME_EXCLUDED_GROUPS to JsonArray(listOf(JsonPrimitive("Group"))),
            ),
        ).withAnimeUserEdits(AnimeUserEdits(title = "My title"))
        val incoming = JsonObject(
            mapOf(
                "source-key" to JsonPrimitive("new"),
                ANIME_USER_EDITS to JsonObject(mapOf("title" to JsonPrimitive("Source sent this"))),
            ),
        )

        val merged = mergeAnimeMemo(stored, incoming)

        assertEquals(JsonPrimitive("new"), merged["source-key"])
        assertEquals(setOf("Group"), merged.excludedAnimeGroups())
        assertEquals("My title", merged.animeUserEdits().title)
        // A memo without edits does not gain any from the source either.
        assertNull(mergeAnimeMemo(JsonObject(emptyMap()), incoming)[ANIME_USER_EDITS])
    }

    @Test
    fun theSourceNeverReceivesTheEdits() {
        val memo = JsonObject(emptyMap()).withAnimeUserEdits(AnimeUserEdits(title = "My title"))
        val sent = source.copy(memo = memo).toSourceAnime()

        assertEquals("Source title", sent.title)
        assertNull(sent.memo[ANIME_USER_EDITS])
    }

    @Test
    fun detailsShowTheEditsOverTheSourceDetails() {
        val memo = JsonObject(emptyMap()).withAnimeUserEdits(
            AnimeUserEdits(title = "My title", genres = listOf("Comedy", "Slice of life"), description = ""),
        )
        val display = source.toSourceAnime()
        val shown = display.withUserEdits(memo)

        assertEquals("My title", shown.title)
        assertEquals("Comedy, Slice of life", shown.genre)
        assertNull(shown.description)
        assertEquals("Source author", shown.author)
        // The source details stay as they were.
        assertEquals("Source title", display.title)
        assertSame(display, display.withUserEdits(null))
    }

    @Test
    fun fallbackEditsOnlyFillAMemoWithoutEdits() {
        val mine = JsonObject(emptyMap()).withAnimeUserEdits(AnimeUserEdits(title = "Mine"))
        val theirs = JsonObject(emptyMap()).withAnimeUserEdits(AnimeUserEdits(title = "Theirs"))

        assertEquals("Mine", mine.withFallbackUserEdits(theirs).animeUserEdits().title)
        assertEquals("Theirs", JsonObject(emptyMap()).withFallbackUserEdits(theirs).animeUserEdits().title)
    }
}
