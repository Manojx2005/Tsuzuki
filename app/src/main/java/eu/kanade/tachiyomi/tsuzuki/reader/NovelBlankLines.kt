package eu.kanade.tachiyomi.tsuzuki.reader

import android.text.SpannableStringBuilder

/**
 * `Html.fromHtml(FROM_HTML_MODE_LEGACY)` ends every `<p>` with two line breaks, and `&nbsp;`-only paragraphs
 * become further empty lines. Each empty line costs a full line height and also receives the paragraph-spacing
 * span, so paragraphs ended up about three lines apart. Removing blank lines leaves the reader's
 * paragraph-spacing setting as the only gap between paragraphs. Only used when that setting is above 0: at 0 the
 * book's own blank lines are the only separation, so they stay.
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
        val blank = (lineStart until i).all { isBlankChar(text[it]) }
        if (blank && (!atEnd || lineStart < i)) ranges.add(lineStart to if (atEnd) i else i + 1)
        lineStart = i + 1
    }
    return ranges
}

/**
 * Whitespace plus the invisible format characters scraped chapters often carry (zero-width space / joiners, word
 * joiner, BOM, soft hyphen), which would otherwise keep an empty-looking line. An inline image (object replacement
 * character) is not blank.
 */
internal fun isBlankChar(c: Char): Boolean =
    c != OBJECT_REPLACEMENT && (c.isWhitespace() || c in INVISIBLE_CHARS)

private const val OBJECT_REPLACEMENT = '￼'

// Zero-width space, ZWNJ, ZWJ, word joiner, BOM, soft hyphen.
private const val INVISIBLE_CHARS = "​‌‍⁠﻿­"

/**
 * WebView chapters: drop paragraphs that hold nothing visible (`<p>&nbsp;</p>`, `<p><br></p>`, zero-width
 * characters) and squeeze runs of three or more `<br>` to two, so a chapter never shows a screen-tall blank gap.
 * Paragraph spacing then comes from the reader setting, as in the TextView renderer.
 */
internal fun collapseBlankHtml(html: String): String =
    html.replace(EMPTY_PARAGRAPH, "").replace(BR_RUN, "<br><br>")

private val EMPTY_PARAGRAPH = Regex(
    """<p\b[^>]*>(?:\s|&nbsp;|&#160;|&#xa0;|&#8203;|&#x200b;|[ ​‌‍⁠﻿]|<br\s*/?>)*</p>""",
    RegexOption.IGNORE_CASE,
)
private val BR_RUN = Regex("""(?:<br\s*/?>\s*){3,}""", RegexOption.IGNORE_CASE)
