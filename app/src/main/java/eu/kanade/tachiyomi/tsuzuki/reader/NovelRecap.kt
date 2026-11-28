package eu.kanade.tachiyomi.tsuzuki.reader

import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.util.concurrent.TimeUnit

/**
 * [previousChapter] names the chapter the paragraphs come from when it is not the one being opened; [lastReadAt]
 * is when the book was last read (epoch ms). Both are null when unknown and then left out of the label.
 */
internal data class NovelRecap(
    val chapterId: Long,
    val paragraphs: List<String>,
    val resumePercent: Int = 0,
    val previousChapter: String? = null,
    val lastReadAt: Long? = null,
)

/** The board's label: "Previously · Ch. 4 · 2 days ago", with whichever parts are known. */
internal fun novelRecapLabel(title: String, previousChapter: String?, relativeTime: String?): String =
    listOfNotNull(title, previousChapter?.trim()?.takeIf(String::isNotEmpty), relativeTime?.takeIf(String::isNotEmpty))
        .joinToString(" · ")

internal fun shouldShowNovelRecap(lastReadAt: Long?, now: Long): Boolean =
    lastReadAt != null && lastReadAt > 0L && now - lastReadAt >= TimeUnit.HOURS.toMillis(24)

private val paragraphBreak = Regex("\\n\\s*\\n+")
private val whitespace = Regex("[\\s\\u00A0]+")
private val noteStart = Regex(
    "^(?:a note from\\b|(?:author|translator|editor)(?:['’]s)?\\s+notes?\\b|(?:a/n|t/n|e/n)\\s*[:：])",
    RegexOption.IGNORE_CASE,
)

internal fun novelRecapParagraphs(content: String): List<String> {
    return novelRecapCompletedParagraphs(normalizedProseParagraphs(content))
}

/** Rendered plain text must not be parsed as HTML or estimated from a saved scroll percentage. */
internal fun novelRecapCompletedParagraphs(paragraphs: List<String>): List<String> {
    val normalized = paragraphs.map { it.replace(whitespace, " ").trim() }
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

internal enum class NovelRecapMode { None, CardAboveText, ResumeOverlay }

internal const val NOVEL_RECAP_FINISHED_PROGRESS = 0.99f

/**
 * Reopening mid-chapter restores a position past the top, where a card above the text would be off
 * screen; that case shows the recap as an overlay instead. [entryProgress] is what the viewers restore.
 */
internal fun novelRecapMode(due: Boolean, entryProgress: Float): NovelRecapMode = when {
    !due -> NovelRecapMode.None
    // A finished chapter reopens on its last screen: the closing paragraphs are already visible, so an
    // overlay would only repeat them on top of themselves.
    entryProgress >= NOVEL_RECAP_FINISHED_PROGRESS -> NovelRecapMode.None
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
