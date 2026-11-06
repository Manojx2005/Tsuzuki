package eu.kanade.tachiyomi.tsuzuki.reader

import android.text.SpannableStringBuilder

/**
 * `Html.fromHtml(FROM_HTML_MODE_LEGACY)` ends every `<p>` with two line breaks, and `&nbsp;`-only paragraphs
 * become further empty lines. Each empty line costs a full line height and also receives the paragraph-spacing
 * span, so paragraphs ended up about three lines apart. Removing blank lines leaves the reader's
 * paragraph-spacing setting as the only gap between paragraphs.
 */
internal fun SpannableStringBuilder.removeBlankLines(): SpannableStringBuilder {
    blankLineRanges(this).asReversed().forEach { (start, end) -> delete(start, end) }
    return this
}

/**
 * Ranges `[start, end)` of lines that hold only whitespace, each including its line break (a trailing blank
 * line without one is included too). Lines with an object-replacement character (inline image) are kept.
 */
internal fun blankLineRanges(text: CharSequence): List<Pair<Int, Int>> {
    val ranges = ArrayList<Pair<Int, Int>>()
    var lineStart = 0
    for (i in 0..text.length) {
        val atEnd = i == text.length
        if (!atEnd && text[i] != '\n') continue
        val blank = (lineStart until i).all { text[it].isWhitespace() && text[it] != '￼' }
        if (blank && (!atEnd || lineStart < i)) ranges.add(lineStart to if (atEnd) i else i + 1)
        lineStart = i + 1
    }
    return ranges
}
