package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.util.parallelCatchingFlatMapBlocking
import eu.kanade.tachiyomi.util.parallelFlatMapBlocking
import eu.kanade.tachiyomi.util.parallelMapBlocking
import eu.kanade.tachiyomi.util.parallelMapNotNullBlocking
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** AY6: the extensions-lib parallel helpers keep input order, and the catching one drops only failing items. */
class AnimeExtensionHelpersTest {
    @Test
    fun `results come back in input order`() {
        val out = listOf(30L, 10L, 20L).parallelMapBlocking {
            delay(it)
            it.toString()
        }
        assertEquals(listOf("30", "10", "20"), out)
    }

    @Test
    fun `not-null and flat variants`() {
        assertEquals(listOf(1, 3), listOf(1, 2, 3).parallelMapNotNullBlocking { it.takeIf { n -> n % 2 == 1 } })
        assertEquals(listOf(1, 1, 2, 2), listOf(1, 2).parallelFlatMapBlocking { listOf(it, it) })
    }

    @Test
    fun `a failing item is skipped by the catching variant`() {
        val out = listOf("a", "bad", "c").parallelCatchingFlatMapBlocking {
            check(it != "bad")
            listOf(it.uppercase())
        }
        assertEquals(listOf("A", "C"), out)
    }
}
