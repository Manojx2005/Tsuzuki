package eu.kanade.presentation.tsuzuki.novel

import eu.kanade.presentation.reader.settings.RegexReplacement
import kotlinx.serialization.json.Json

/** Why a rule can't be saved. */
internal enum class ReplaceRuleProblem { TitleEmpty, PatternEmpty, InvalidRegex }

/**
 * Pure helpers for the Tsuzuki replace-rules screen. They read and write the same JSON list as the stock section
 * (`ReaderPreferences.novelRegexReplacements`) and compile rules the way `RegexReplacementsProcessor` does, so the
 * test line shows what the reader will do.
 */
internal object ReplaceRulesLogic {

    fun decode(json: String): List<RegexReplacement> =
        try {
            Json.decodeFromString<List<RegexReplacement>>(json)
        } catch (e: Exception) {
            emptyList()
        }

    fun encode(rules: List<RegexReplacement>): String = Json.encodeToString(rules)

    fun toggle(rules: List<RegexReplacement>, id: String): List<RegexReplacement> =
        rules.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }

    /** Replaces the rule with [editingId], or appends when it is null (or no longer in the list). */
    fun upsert(rules: List<RegexReplacement>, rule: RegexReplacement, editingId: String?): List<RegexReplacement> =
        if (editingId != null && rules.any { it.id == editingId }) {
            rules.map { if (it.id == editingId) rule else it }
        } else {
            rules + rule
        }

    fun delete(rules: List<RegexReplacement>, id: String): List<RegexReplacement> = rules.filterNot { it.id == id }

    fun compile(pattern: String, isRegex: Boolean, matchWholeWord: Boolean, caseSensitive: Boolean): Result<Regex> =
        runCatching {
            val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            if (isRegex) {
                Regex(pattern, options)
            } else {
                val escaped = Regex.escape(pattern)
                val bounded = if (matchWholeWord) {
                    "(?<![\\p{L}\\p{N}_])(?:$escaped)(?![\\p{L}\\p{N}_])"
                } else {
                    escaped
                }
                Regex(bounded, options)
            }
        }

    fun validate(title: String, pattern: String, isRegex: Boolean): ReplaceRuleProblem? = when {
        title.isBlank() -> ReplaceRuleProblem.TitleEmpty
        pattern.isBlank() -> ReplaceRuleProblem.PatternEmpty
        isRegex && compile(pattern, true, false, false).isFailure -> ReplaceRuleProblem.InvalidRegex
        else -> null
    }

    /** Runs one rule over [input] with the reader's semantics. A blank pattern leaves the text unchanged. */
    fun test(
        input: String,
        pattern: String,
        replacement: String,
        isRegex: Boolean,
        matchWholeWord: Boolean,
        caseSensitive: Boolean,
    ): Result<String> {
        if (pattern.isBlank()) return Result.success(input)
        return compile(pattern, isRegex, matchWholeWord, caseSensitive).mapCatching { it.replace(input, replacement) }
    }

    /** Builds the rule to save; whole-word and case flags only apply to plain-text rules, as in the stock dialog. */
    fun build(
        existing: RegexReplacement?,
        title: String,
        pattern: String,
        replacement: String,
        isRegex: Boolean,
        matchWholeWord: Boolean,
        caseSensitive: Boolean,
    ): RegexReplacement = RegexReplacement(
        title = title.trim(),
        pattern = pattern,
        replacement = replacement,
        enabled = existing?.enabled ?: true,
        isRegex = isRegex,
        matchWholeWord = !isRegex && matchWholeWord,
        caseSensitive = !isRegex && caseSensitive,
        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
    )
}
