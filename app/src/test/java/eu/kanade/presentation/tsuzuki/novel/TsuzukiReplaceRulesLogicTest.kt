package eu.kanade.presentation.tsuzuki.novel

import eu.kanade.presentation.reader.settings.RegexReplacement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TsuzukiReplaceRulesLogicTest {

    private fun rule(id: String, enabled: Boolean = true) =
        RegexReplacement(title = id, pattern = "a", replacement = "b", enabled = enabled, id = id)

    @Test
    fun `json round trip keeps every field`() {
        val rules = listOf(
            RegexReplacement(
                title = "t",
                pattern = "p",
                replacement = "r",
                enabled = false,
                isRegex = false,
                matchWholeWord = true,
                caseSensitive = true,
                id = "x",
            ),
        )
        assertEquals(rules, ReplaceRulesLogic.decode(ReplaceRulesLogic.encode(rules)))
    }

    @Test
    fun `bad or empty json decodes to an empty list`() {
        assertTrue(ReplaceRulesLogic.decode("not json").isEmpty())
        assertTrue(ReplaceRulesLogic.decode("[]").isEmpty())
    }

    @Test
    fun `toggle flips only the matching rule`() {
        val result = ReplaceRulesLogic.toggle(listOf(rule("1"), rule("2")), "2")
        assertEquals(listOf(true, false), result.map { it.enabled })
    }

    @Test
    fun `upsert replaces when editing and appends otherwise`() {
        val rules = listOf(rule("1"), rule("2"))
        val edited = rule("2").copy(replacement = "z")
        assertEquals("z", ReplaceRulesLogic.upsert(rules, edited, "2")[1].replacement)
        assertEquals(3, ReplaceRulesLogic.upsert(rules, rule("3"), null).size)
        assertEquals(3, ReplaceRulesLogic.upsert(rules, rule("3"), "gone").size)
    }

    @Test
    fun `delete removes by id`() {
        assertEquals(listOf("1"), ReplaceRulesLogic.delete(listOf(rule("1"), rule("2")), "2").map { it.id })
    }

    @Test
    fun `validation reports the first problem`() {
        assertEquals(ReplaceRuleProblem.TitleEmpty, ReplaceRulesLogic.validate(" ", "a", true))
        assertEquals(ReplaceRuleProblem.PatternEmpty, ReplaceRulesLogic.validate("t", "", true))
        assertEquals(ReplaceRuleProblem.InvalidRegex, ReplaceRulesLogic.validate("t", "(", true))
        assertNull(ReplaceRulesLogic.validate("t", "(", false))
        assertNull(ReplaceRulesLogic.validate("t", "a+", true))
    }

    @Test
    fun `test replaces like the reader`() {
        assertEquals(
            "Lin Feng stood",
            ReplaceRulesLogic.test("Ling Feng stood", "ling feng", "Lin Feng", false, false, false).getOrThrow(),
        )
        assertEquals(
            "ling feng",
            ReplaceRulesLogic.test("ling feng", "Ling Feng", "x", false, false, true).getOrThrow(),
        )
        assertEquals(
            "gate.",
            ReplaceRulesLogic.test("gate(TL note: x).", "\\(TL note:.*?\\)", "", true, false, false).getOrThrow(),
        )
    }

    @Test
    fun `whole word does not touch longer words`() {
        assertEquals("x cats", ReplaceRulesLogic.test("cat cats", "cat", "x", false, true, false).getOrThrow())
    }

    @Test
    fun `invalid regex gives a failure and blank pattern is a no-op`() {
        assertTrue(ReplaceRulesLogic.test("a", "(", "", true, false, false).isFailure)
        assertEquals("a", ReplaceRulesLogic.test("a", "", "b", true, false, false).getOrThrow())
    }

    @Test
    fun `build drops text-only flags for regex rules and keeps id and enabled`() {
        val existing = rule("keep", enabled = false)
        val built = ReplaceRulesLogic.build(existing, " T ", "p", "r", true, true, true)
        assertEquals("T", built.title)
        assertEquals("keep", built.id)
        assertEquals(false, built.enabled)
        assertEquals(false, built.matchWholeWord)
        assertEquals(false, built.caseSensitive)
    }
}
