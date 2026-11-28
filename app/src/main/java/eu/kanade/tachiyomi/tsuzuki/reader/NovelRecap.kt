package eu.kanade.tachiyomi.tsuzuki.reader

import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.util.concurrent.TimeUnit

internal data class NovelRecap(val chapterId: Long, val paragraphs: List<String>, val resumePercent: Int = 0)

internal fun shouldShowNovelRecap(lastReadAt: Long?, now: Long): Boolean =
    lastReadAt != null && lastReadAt > 0L && now - lastReadAt >= TimeUnit.HOURS.toMillis(24)

private val paragraphBreak = Regex("\\n\\s*\\n+")
private val whitespace = Regex("[\\s\\u00A0]+")
private val noteStart = Regex(
    "^(?:a note from\\b|(?:author|translator|editor)(?:['’]s)?\\s+notes?\\b|(?:a/n|t/n|e/n)\\s*[:：])",
    RegexOption.IGNORE_CASE,
)

internal fun novelRecapParagraphs(content: String): List<String> {
    val normalized = normalizedProseParagraphs(content)
    val lastNote = normalized.indexOfLast { noteStart.containsMatchIn(it) }
    val beforeNote = if (lastNote > 0 && normalized.take(lastNote).count { it.length >= 24 } >= 2) {
        normalized.take(lastNote)
    } else {
        normalized
    }
    return recapExcerpt(beforeNote)
}

private fun recapExcerpt(paragraphs: List<String>): List<String> {
    val excerpt = paragraphs.filter { it.length >= 24 && !noteStart.containsMatchIn(it) }.takeLast(3)
    return if (excerpt.size >= 2) excerpt else emptyList()
}

private fun normalizedProseParagraphs(content: String): List<String> {
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
    return paragraphs.map { it.replace(whitespace, " ").trim() }
}

/**
 * The prose the reader has just passed when a chapter reopens at [progress] (0..1 of the chapter, the
 * same fraction both renderers restore): the last paragraphs that end before that point, with the same
 * filters as [novelRecapParagraphs]. Empty when fewer than two qualify, which means the position is in
 * the opening paragraphs and the caller falls back to the previous chapter.
 */
internal fun novelRecapParagraphsBefore(content: String, progress: Float): List<String> {
    val paragraphs = normalizedProseParagraphs(content)
    val total = paragraphs.sumOf { it.length }
    if (total <= 0 || progress <= 0f) return emptyList()
    val position = progress.coerceAtMost(1f) * total
    var end = 0
    val passed = ArrayList<String>()
    for (paragraph in paragraphs) {
        end += paragraph.length
        if (end > position) break
        passed += paragraph
    }
    return recapExcerpt(passed)
}

internal enum class NovelRecapMode { None, CardAboveText, ResumeOverlay }

/**
 * Reopening mid-chapter restores a position past the top, where a card above the text would be off
 * screen; that case shows the recap as an overlay instead. [entryProgress] is what the viewers restore.
 */
internal fun novelRecapMode(due: Boolean, entryProgress: Float): NovelRecapMode = when {
    !due -> NovelRecapMode.None
    entryProgress > 0f -> NovelRecapMode.ResumeOverlay
    else -> NovelRecapMode.CardAboveText
}

/**
 * The chapter read just before [chapterId] in reading order. Taken from the full sorted list, not the
 * reader's list: "skip read" / filters / "downloaded only" drop exactly the chapter just finished, which
 * would make the recap silently disappear (or recap the wrong chapter) for people using them.
 */
internal fun <T> previousChapterForRecap(sortedAscending: List<T>, chapterId: Long, idOf: (T) -> Long): T? {
    val index = sortedAscending.indexOfFirst { idOf(it) == chapterId }
    return sortedAscending.getOrNull(index - 1)?.takeIf { index > 0 }
}

/** The reader has moved on from the resume point: two percentage points of the chapter, or a paged turn. */
internal fun novelRecapScrolledAway(basePercent: Int, percent: Int, basePage: Int, page: Int): Boolean =
    kotlin.math.abs(percent - basePercent) >= 2 || page != basePage
