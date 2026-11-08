package eu.kanade.tachiyomi.tsuzuki.manhwa

private val longStripTags = setOf(
    "manhwa", "manhua", "webtoon", "webtoons", "long strip", "longstrip",
    "웹툰", "条漫", "條漫", "ウェブトゥーン", "縦読み",
)

internal fun isManhwa(
    tags: List<String>?,
    language: String?,
    longStripPages: Boolean?,
    override: Boolean?,
): Boolean {
    if (override != null) return override
    val formats = tags.orEmpty().map { it.trim().lowercase().replace('-', ' ').replace('_', ' ') }
    if (formats.any { it in longStripTags }) return true
    if (longStripPages != null) return longStripPages
    if ("manga" in formats) return false
    // A source language is a fallback. It can describe a translation, so explicit evidence wins.
    val primaryLanguage = language?.lowercase()?.substringBefore('-')?.substringBefore('_')
    return primaryLanguage == "ko" || primaryLanguage == "zh"
}

internal class ManhwaChapterEvidence {
    private val measuredPages = mutableSetOf<Int>()
    private var longStripPages = 0
    private var classified = false

    @Synchronized
    fun record(index: Int, totalPages: Int, width: Int, height: Int): Boolean? {
        if (classified || index !in 0 until totalPages || width <= 0 || height <= 0) return null
        if (!measuredPages.add(index)) return null
        if (height.toLong() * 2 >= width.toLong() * 5) longStripPages++
        val verdict = when {
            longStripPages > totalPages / 2 -> true
            measuredPages.size - longStripPages > totalPages / 2 -> false
            else -> null
        }
        classified = verdict != null
        return verdict
    }
}
