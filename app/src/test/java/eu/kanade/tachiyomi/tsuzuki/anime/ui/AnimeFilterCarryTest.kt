package eu.kanade.tachiyomi.tsuzuki.anime.ui

import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** Filters an extension would define (public with no-arg constructors so a second class loader can build them). */
class CarryGenre(values: Array<String> = arrayOf("Any", "Action", "Drama")) :
    AnimeFilter.Select<String>("Genre", values)
class CarryDubbed : AnimeFilter.CheckBox("Dubbed")
class CarryYear : AnimeFilter.Text("Year")
class CarrySort(values: Array<String> = arrayOf("Name", "Date")) : AnimeFilter.Sort("Sort", values)
class CarryTags : AnimeFilter.Group<AnimeFilter.TriState>("Tags", listOf(CarryTag("Isekai"), CarryTag("Mecha")))
class CarryTag(name: String) : AnimeFilter.TriState(name)

/** Loads the Carry* classes itself, like a reloaded extension; everything else (source-api) from the parent. */
private class ReloadedExtensionLoader(parent: ClassLoader) : ClassLoader(parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (!name.substringAfterLast('.').startsWith("Carry")) return super.loadClass(name, resolve)
        synchronized(this) {
            findLoadedClass(name)?.let { return it }
            val bytes = parent.getResourceAsStream(name.replace('.', '/') + ".class")!!.use { it.readBytes() }
            return defineClass(name, bytes, 0, bytes.size)
        }
    }
}

class AnimeFilterCarryTest {
    private fun reloadedFilters(): AnimeFilterList {
        val loader = ReloadedExtensionLoader(javaClass.classLoader!!)
        fun make(simple: String) =
            loader.loadClass(
                "${javaClass.packageName}.$simple",
            ).getDeclaredConstructor().newInstance() as AnimeFilter<*>
        return AnimeFilterList(
            make("CarryGenre"),
            make("CarryDubbed"),
            make("CarryYear"),
            make("CarrySort"),
            make("CarryTags"),
        )
    }

    private fun applied() = AnimeFilterList(
        CarryGenre().apply { state = 2 },
        CarryDubbed().apply { state = true },
        CarryYear().apply { state = "2024" },
        CarrySort().apply { state = AnimeFilter.Sort.Selection(1, ascending = false) },
        CarryTags().apply { state[1].state = AnimeFilter.TriState.STATE_EXCLUDE },
    )

    @Test
    fun `filtered search keeps its values on the reloaded source, with the new instance's objects`() {
        val fresh = reloadedFilters()
        // The reloaded classes really are different classes with the same name.
        assertNotSame(CarryGenre::class.java, fresh[0].javaClass)
        assertEquals(CarryGenre::class.java.name, fresh[0].javaClass.name)

        val carried = carryAnimeFilters(applied()) { fresh }

        assertSame(fresh, carried)
        carried.forEach { assertSame(fresh[0].javaClass.classLoader, it.javaClass.classLoader) }
        assertEquals(2, carried[0].state)
        assertEquals(true, carried[1].state)
        assertEquals("2024", carried[2].state)
        assertEquals(AnimeFilter.Sort.Selection(1, ascending = false), carried[3].state)
        val tags = (carried[4] as AnimeFilter.Group<*>).state.map { (it as AnimeFilter<*>).state }
        assertEquals(listOf(AnimeFilter.TriState.STATE_IGNORE, AnimeFilter.TriState.STATE_EXCLUDE), tags)
    }

    @Test
    fun `no filters stay no filters`() {
        var asked = false
        val carried = carryAnimeFilters(AnimeFilterList()) {
            asked = true
            reloadedFilters()
        }
        assertEquals(0, carried.size)
        assertEquals(false, asked)
    }

    @Test
    fun `reordered filters and choices keep the selected labels and sort direction`() {
        val sort = CarrySort(arrayOf("Date", "Name"))
        val genre = CarryGenre(arrayOf("Drama", "Any", "Action")).apply { state = 1 }
        val year = CarryYear()
        restoreAnimeFilterStates(applied(), AnimeFilterList(sort, genre, year))
        assertEquals(0, genre.state)
        assertEquals(AnimeFilter.Sort.Selection(0, ascending = false), sort.state)
        assertEquals("2024", year.state)
    }

    @Test
    fun `removed or ambiguous choices keep the fresh default`() {
        val removed = CarryGenre(arrayOf("Any", "Action"))
        val ambiguous = CarryGenre(arrayOf("Any", "Drama", "Drama"))
        for (draft in listOf(removed, ambiguous)) {
            restoreAnimeFilterStates(applied(), AnimeFilterList(draft))
            assertEquals(0, draft.state)
        }
    }

    @Test
    fun `values that no longer fit keep the source default`() {
        // The new version of the source dropped a genre and renamed the year filter.
        class FewerGenres : AnimeFilter.Select<String>("Genre", arrayOf("Any", "Action"))
        class Season : AnimeFilter.Text("Season")
        val old = AnimeFilterList(
            object : AnimeFilter.Select<String>("Genre", arrayOf("Any", "Action", "Drama"), 2) {},
            CarryYear().apply { state = "2024" },
        )
        val fresh = AnimeFilterList(FewerGenres(), Season())
        restoreAnimeFilterStates(old, fresh)
        assertEquals(0, fresh[0].state) // class name differs, and index 2 is out of range anyway
        assertEquals("", fresh[1].state)

        val sameShape = AnimeFilterList(CarrySort())
        restoreAnimeFilterStates(
            AnimeFilterList(CarrySort().apply { state = AnimeFilter.Sort.Selection(5, true) }),
            sameShape,
        )
        assertNull(sameShape[0].state)
    }
}
