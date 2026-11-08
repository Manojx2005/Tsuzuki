package eu.kanade.presentation.tsuzuki.novel

import eu.kanade.presentation.util.formatChapterNumber

private const val CHAPTER_NUMBER = "[0-9]+(?:[.,][0-9]+)?"

private val chapterHeading = Regex(
    """^\s*(?:vol(?:ume)?\.?\s*$CHAPTER_NUMBER\s*[-:–—,]?\s*)?ch(?:apter)?\.?\s*($CHAPTER_NUMBER)(?=$|[\s:：)\-–—]|\.(?:\s|$))""",
    RegexOption.IGNORE_CASE,
)
private val numberedHeading = Regex("""^\s*($CHAPTER_NUMBER)(?:\s*[:：]|[.)]\s+)""")
private val japaneseChapterHeading = Regex("""^\s*第\s*($CHAPTER_NUMBER)\s*[章話]""")

/** Some novel sources store a zero-based index or a number used to sort their chapter list. */
internal fun novelTitleChapterNumber(title: String): Double? {
    val heading = chapterHeading.find(title) ?: numberedHeading.find(title) ?: japaneseChapterHeading.find(title)
    return heading?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it.isFinite() }
}

internal fun novelReaderChapterLabel(
    title: String,
    storedNumber: Double,
    showNumber: Boolean,
    showTitle: Boolean,
    numberPrefix: String,
): String? {
    val name = title.takeIf { showTitle && it.isNotBlank() }
    if (!showNumber) return name
    val sourceNumber = novelTitleChapterNumber(title)
    if (sourceNumber != null && name != null) return name
    val number = sourceNumber ?: storedNumber.takeIf { it.isFinite() && it >= 0 } ?: return name
    val label = "$numberPrefix ${formatChapterNumber(number)}"
    return if (name == null) label else "$label: $name"
}
