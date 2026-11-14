package eu.kanade.tachiyomi.tsuzuki.reader

import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.util.concurrent.TimeUnit

internal data class NovelRecap(val chapterId: Long, val paragraphs: List<String>)

internal fun shouldShowNovelRecap(lastReadAt: Long?, now: Long): Boolean =
    lastReadAt != null && lastReadAt > 0L && now - lastReadAt >= TimeUnit.HOURS.toMillis(24)

private val paragraphBreak = Regex("\\n\\s*\\n+")
private val whitespace = Regex("[\\s\\u00A0]+")
private val noteStart = Regex(
    "^(?:a note from\\b|(?:author|translator|editor)(?:['’]s)?\\s+notes?\\b|(?:a/n|t/n|e/n)\\s*[:：])",
    RegexOption.IGNORE_CASE,
)

internal fun novelRecapParagraphs(content: String): List<String> {
    val body = Jsoup.parse(content).body()
    val html = body.children().isNotEmpty()
    body.select("script, style, nav, aside, [hidden], .author-note, .translator-note, .editor-note").remove()
    body.select("p, div, section, blockquote, li, h1, h2, h3, hr").forEach { block ->
        block.after(TextNode("\n\n"))
    }
    val text = body.wholeText()
    // Plain text with blank lines is hard-wrapped prose: the blank lines are the paragraph breaks.
    // Separator lines ("—", "***") inside such a block are not part of the sentence around them.
    val paragraphs = when {
        html -> text.split(paragraphBreak)
        paragraphBreak.containsMatchIn(text.trim()) -> text.split(paragraphBreak).map { block ->
            block.lines().filter { line -> line.any(Char::isLetterOrDigit) }.joinToString(" ")
        }
        else -> text.lines()
    }
    val normalized = paragraphs.map { it.replace(whitespace, " ").trim() }
    val lastNote = normalized.indexOfLast { noteStart.containsMatchIn(it) }
    val beforeNote = if (lastNote > 0 && normalized.take(lastNote).count { it.length >= 24 } >= 2) {
        normalized.take(lastNote)
    } else {
        normalized
    }
    val excerpt = beforeNote.filter { it.length >= 24 && !noteStart.containsMatchIn(it) }.takeLast(3)
    return if (excerpt.size >= 2) excerpt else emptyList()
}
