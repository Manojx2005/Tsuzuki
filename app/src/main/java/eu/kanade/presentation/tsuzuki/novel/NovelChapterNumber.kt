package eu.kanade.presentation.tsuzuki.novel

import eu.kanade.presentation.util.formatChapterNumber

private const val CHAPTER_NUMBER = "[0-9]+(?:[.,][0-9]+)?"

private val chapterHeading = Regex(
    """^\s*(?:(?<volume>vol(?:ume)?\.?\s*$CHAPTER_NUMBER)\s*[-:–—,]?\s*)?ch(?:apter)?\.?\s*(?<number>$CHAPTER_NUMBER)(?=$|[\s:：)\-–—]|\.(?:\s|$))""",
    RegexOption.IGNORE_CASE,
)
private val numberedHeading = Regex("""^\s*(?<number>$CHAPTER_NUMBER)(?:\s*[:：]|[.)]\s+)""")
private val japaneseChapterHeading = Regex("""^\s*第\s*(?<number>$CHAPTER_NUMBER)\s*[章話]""")

internal data class NovelChapterHeading(val number: Double, val title: String)

/** Some novel sources store a zero-based index or a number used to sort their chapter list. */
internal fun novelChapterHeading(title: String): NovelChapterHeading? {
    val explicitHeading = chapterHeading.find(title)
    val heading = explicitHeading ?: numberedHeading.find(title) ?: japaneseChapterHeading.find(title) ?: return null
    val number = heading.groups["number"]?.value?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it.isFinite() }
        ?: return null
    val name = title.substring(heading.range.last + 1).trimStart { it.isWhitespace() || it in ":：.)-–—" }
    val volume = explicitHeading?.groups?.get("volume")?.value
    return NovelChapterHeading(number, listOfNotNull(volume, name.takeIf(String::isNotBlank)).joinToString(" · "))
}

internal fun novelTitleChapterNumber(title: String): Double? = novelChapterHeading(title)?.number

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
