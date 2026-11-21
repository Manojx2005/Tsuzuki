package eu.kanade.tachiyomi.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * AY6: everything an anime extension can link against (the extensions-lib stubs, dumped by
 * `scripts/anime-abi/dump_api.py` into `expected-lib<N>.txt`) must exist in the app with the same JVM descriptor
 * and at least the same visibility. A miss is a `NoSuchMethodError` / `NoClassDefFoundError` at runtime.
 * Lines in [NOT_OFFERED] are deliberately absent (reason beside each); `android.*` is the platform's.
 */
class AnimeExtensionAbiTest {
    private fun expected(lib: String): List<String> =
        File("../scripts/anime-abi/expected-lib$lib.txt").readLines().filter(String::isNotBlank)

    @Test
    fun `lib 14 API is offered`() = assertNoMisses("14")

    @Test
    fun `lib 16 API is offered`() = assertNoMisses("16")

    @Test
    fun `lib 17 API is offered`() = assertNoMisses("17")

    @Test
    fun `a broken expectation is reported with every miss`() {
        val misses = abiMisses(
            listOf(
                "eu.kanade.tachiyomi.animesource.model.SAnime",
                "eu.kanade.tachiyomi.animesource.model.SAnime#noSuchMember ()V",
                "eu.kanade.tachiyomi.NoSuchClass",
            ),
        )
        assertEquals(2, misses.size, misses.joinToString("\n"))
    }

    private fun assertNoMisses(lib: String) {
        val misses = abiMisses(expected(lib))
        assertTrue(misses.isEmpty()) { "lib $lib: ${misses.size} missing\n" + misses.joinToString("\n") }
    }

    companion object {
        /** Stub members the app deliberately doesn't offer, with the reason. */
        val NOT_OFFERED: Map<String, String> = mapOf(
            "eu.kanade.tachiyomi.torrentutils" to "lib 17 torrent API; Tsuzuki ships no torrent support in v0.2",
            "androidx.preference.DialogPreference#<init> ()V" to "stub-only constructor; extensions never build it",
            "androidx.preference.Preference#<init> ()V" to "stub-only constructor; extensions never build it",
            "androidx.preference.PreferenceScreen#<init> ()V" to "stub-only constructor; the app passes the screen",
            "eu.kanade.tachiyomi.network.NetworkHelper#<init> (Landroid/content/Context;)V" to
                "the app constructs NetworkHelper; extensions get it from Injekt",
            "eu.kanade.tachiyomi.animesource.model.Video#component3 ()Ljava/lang/String;" to LIB14_VIDEO,
            "eu.kanade.tachiyomi.animesource.model.Video#component4 ()Lokhttp3/Headers;" to LIB14_VIDEO,
            "eu.kanade.tachiyomi.animesource.model.Video#component5 ()Ljava/util/List;" to LIB14_VIDEO,
            "eu.kanade.tachiyomi.animesource.model.Video#component6 ()Ljava/util/List;" to LIB14_VIDEO,
            "eu.kanade.tachiyomi.animesource.model.Video#copy (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Lokhttp3/Headers;Ljava/util/List;Ljava/util/List;)Leu/kanade/tachiyomi/animesource/model/Video;" to
                LIB14_VIDEO,
            "eu.kanade.tachiyomi.animesource.model.Video#copy\$default (Leu/kanade/tachiyomi/animesource/model/Video;" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lokhttp3/Headers;Ljava/util/List;Ljava/util/List;" +
                "ILjava/lang/Object;)Leu/kanade/tachiyomi/animesource/model/Video;" to LIB14_VIDEO,
        )

        private const val LIB14_VIDEO = "lib 14 Video data-class members: Kotlin can't declare componentN with the " +
            "lib-16 component's name and another return type, and an old copy() overload would make every copy() call " +
            "ambiguous; lib-14 constructors are bridged (AY1)"

        fun abiMisses(lines: List<String>): List<String> = lines
            .filterNot { it.startsWith("android.") }
            .filterNot { line ->
                NOT_OFFERED.keys.any { line == it || line.startsWith("$it#") || line.startsWith("$it.") }
            }
            .mapNotNull { line -> line.takeUnless { offered(it) } }

        private fun offered(line: String): Boolean {
            val className = line.substringBefore('#')
            val type = try {
                Class.forName(className, false, AnimeExtensionAbiTest::class.java.classLoader)
            } catch (_: Throwable) {
                return false
            }
            if ('#' !in line) return true
            val (name, descriptor) = line.substringAfter('#').split(' ', limit = 2)
            return members(type).any { (memberName, memberDescriptor, modifiers) ->
                memberName == name && memberDescriptor == descriptor &&
                    (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers))
            }
        }

        private fun members(type: Class<*>): List<Triple<String, String, Int>> {
            val out = mutableListOf<Triple<String, String, Int>>()
            var current: Class<*>? = type
            val seen = mutableSetOf<Class<*>>()
            val queue = ArrayDeque<Class<*>>().apply { add(type) }
            while (queue.isNotEmpty()) {
                current = queue.removeFirst()
                if (!seen.add(current)) continue
                runCatching { current.declaredMethods }.getOrDefault(emptyArray()).forEach { out += it.entry() }
                runCatching { current.declaredFields }.getOrDefault(emptyArray()).forEach { out += it.entry() }
                if (current == type) {
                    runCatching { current.declaredConstructors }.getOrDefault(emptyArray()).forEach { out += it.entry() }
                }
                current.superclass?.let(queue::add)
                current.interfaces.forEach(queue::add)
            }
            return out
        }

        private fun Method.entry() =
            Triple(name, "(${parameterTypes.joinToString("") { it.descriptor() }})${returnType.descriptor()}", modifiers)

        private fun Constructor<*>.entry() =
            Triple("<init>", "(${parameterTypes.joinToString("") { it.descriptor() }})V", modifiers)

        private fun Field.entry() = Triple(name, type.descriptor(), modifiers)

        private fun Class<*>.descriptor(): String = when {
            isArray -> "[" + componentType.descriptor()
            this == Void.TYPE -> "V"
            this == java.lang.Boolean.TYPE -> "Z"
            this == java.lang.Byte.TYPE -> "B"
            this == java.lang.Character.TYPE -> "C"
            this == java.lang.Short.TYPE -> "S"
            this == java.lang.Integer.TYPE -> "I"
            this == java.lang.Long.TYPE -> "J"
            this == java.lang.Float.TYPE -> "F"
            this == java.lang.Double.TYPE -> "D"
            else -> "L" + name.replace('.', '/') + ";"
        }
    }
}
