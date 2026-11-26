package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.ChapterType
import eu.kanade.tachiyomi.animesource.model.TimeStamp

/*
 * AY5: chapters for the seek bar and the skip button. The behaviour follows Aniyomi's
 * `utils/ChapterUtils.mergeChapters` (Apache-2.0, https://github.com/aniyomiorg/aniyomi): stamps from the source
 * win over the file's own chapters, AniSkip fills in only where the source said nothing. Written for Tsuzuki.
 */

/** A chapter mpv read from the file (`chapter-list`): its title (may be blank) and start in seconds. */
internal data class AnimeFileChapter(val title: String?, val start: Double)

/**
 * One piece of the merged chapter list: [start] until [end] (`Double.POSITIVE_INFINITY` while the duration is not
 * known). [name] is null when nothing named it; [skip] is set for an opening, ending or recap the skip button offers.
 */
internal data class AnimeChapter(
    val start: Double,
    val end: Double,
    val name: String?,
    val skip: AniSkipKind? = null,
)

/** Remnants of a file chapter shorter than this, left between two stamps, are dropped. */
private const val CHAPTER_MIN_REMNANT_SECONDS = 1.0

/** A stamp that starts this close to 0 starts the episode. */
private const val CHAPTER_START_SNAP_SECONDS = 1.0

internal fun ChapterType.skipKind(): AniSkipKind? = when (this) {
    ChapterType.Opening, ChapterType.MixedOp -> AniSkipKind.OPENING
    ChapterType.Ending -> AniSkipKind.ENDING
    ChapterType.Recap -> AniSkipKind.RECAP
    ChapterType.Other -> null
}

/** The setting "Don't use AniSkip when the video has chapters": should AniSkip be asked at all? */
internal fun shouldRequestAniSkip(
    mode: AniSkipMode,
    ignoreWithChapters: Boolean,
    fileChapters: List<AnimeFileChapter>,
    timestamps: List<TimeStamp>,
): Boolean = mode != AniSkipMode.OFF && !(ignoreWithChapters && (fileChapters.isNotEmpty() || timestamps.isNotEmpty()))

/**
 * Merge the file's chapters, the source's [timestamps] and AniSkip's [aniSkip] into one ordered, non-overlapping
 * list. [duration] ≤ 0 (or not finite) means unknown: nothing is clamped to it and the last chapter is open-ended.
 *
 * - Stamps and AniSkip segments outside the episode (non-finite, negative, starting at or after a known duration,
 *   ending at or before their start) are dropped; ends are clamped to a known duration.
 * - Source stamps win over AniSkip: an AniSkip segment that overlaps one is dropped. Where source stamps overlap
 *   each other the earlier one keeps its span and the later one starts after it.
 * - File chapters run to the next chapter (or the end) and keep their name around the stamps that cut into them:
 *   an AniSkip opening inside "Part A" gives "Part A", the opening, then "Part A" again.
 */
internal fun mergeAnimeChapters(
    fileChapters: List<AnimeFileChapter>,
    timestamps: List<TimeStamp>,
    aniSkip: List<AniSkipSegment>,
    duration: Double,
): List<AnimeChapter> {
    val known = duration.isFinite() && duration > 0
    val end = if (known) duration else Double.POSITIVE_INFINITY

    fun stamp(start: Double, stop: Double, name: String?, skip: AniSkipKind?): AnimeChapter? {
        if (!start.isFinite() || !stop.isFinite() || start < 0 || start >= end || stop <= start) return null
        val from = if (start < CHAPTER_START_SNAP_SECONDS) 0.0 else start
        return AnimeChapter(from, stop.coerceAtMost(end), name?.trim()?.takeIf(String::isNotEmpty), skip)
    }

    val overlays = mutableListOf<AnimeChapter>()
    fun addWithoutOverlap(candidates: List<AnimeChapter>) {
        for (candidate in candidates.sortedBy(AnimeChapter::start)) {
            var piece: AnimeChapter? = candidate
            for (taken in overlays) {
                val current = piece ?: break
                if (current.start < taken.end && current.end > taken.start) {
                    piece = when {
                        // Starts inside: keep the part after.
                        current.start >= taken.start -> current.copy(start = taken.end)
                            .takeIf { it.end - it.start >= CHAPTER_MIN_REMNANT_SECONDS }
                        // Ends inside (or surrounds): keep the part before.
                        else -> current.copy(end = taken.start)
                            .takeIf { it.end - it.start >= CHAPTER_MIN_REMNANT_SECONDS }
                    }
                }
            }
            piece?.let(overlays::add)
        }
    }
    addWithoutOverlap(timestamps.mapNotNull { stamp(it.start, it.end, it.name, it.type.skipKind()) })
    // AniSkip only fills in where the source said nothing: a segment touching a source stamp is dropped.
    val sourceStamps = overlays.toList()
    overlays += aniSkip.mapNotNull { stamp(it.start, it.end, null, it.kind) }
        .filter { segment -> sourceStamps.none { segment.start < it.end && segment.end > it.start } }
        .distinctBy(AnimeChapter::start)
        .sortedBy(AnimeChapter::start)
        .fold(emptyList<AnimeChapter>()) { kept, segment ->
            if (kept.lastOrNull()?.let { segment.start < it.end } == true) kept else kept + segment
        }
    overlays.sortBy(AnimeChapter::start)

    // The file's chapters as back-to-back pieces covering [0, end).
    val starts = fileChapters
        .filter { it.start.isFinite() && it.start >= 0 && it.start < end }
        .sortedBy(AnimeFileChapter::start)
        .distinctBy(AnimeFileChapter::start)
    val base = buildList {
        if (starts.isEmpty() || starts.first().start > 0) {
            add(AnimeChapter(0.0, starts.firstOrNull()?.start ?: end, null))
        }
        starts.forEachIndexed { index, chapter ->
            val stop = starts.getOrNull(index + 1)?.start ?: end
            add(AnimeChapter(chapter.start, stop, chapter.title?.trim()?.takeIf(String::isNotEmpty)))
        }
    }

    // Cut the stamps out of the file's chapters; what is left keeps the chapter's name.
    val remnants = base.flatMap { chapter ->
        var pieces = listOf(chapter)
        for (overlay in overlays) {
            pieces = pieces.flatMap { piece ->
                if (overlay.end <= piece.start || overlay.start >= piece.end) {
                    listOf(piece)
                } else {
                    listOfNotNull(
                        piece.copy(end = overlay.start).takeIf { it.end > it.start },
                        piece.copy(start = overlay.end).takeIf { it.end > it.start },
                    )
                }
            }
        }
        pieces
    }.filter { it.end - it.start >= CHAPTER_MIN_REMNANT_SECONDS || it.end == Double.POSITIVE_INFINITY }

    val merged = (overlays + remnants).sortedBy(AnimeChapter::start)
    // Nothing worth showing: one unnamed chapter over the whole episode.
    return if (merged.none { it.name != null || it.skip != null }) emptyList() else merged
}

/** The skippable pieces of a merged list, for the skip button and auto-skip. */
internal fun List<AnimeChapter>.skipSegments(): List<AniSkipSegment> =
    mapNotNull { chapter -> chapter.skip?.let { AniSkipSegment(it, chapter.start, chapter.end) } }

/** The chapter at [position], or null. */
internal fun List<AnimeChapter>.chapterAt(position: Double): AnimeChapter? =
    lastOrNull { position >= it.start && position < it.end }

/**
 * Where the seek bar draws a tick, as fractions of [duration]: every chapter start, but the start of the episode
 * only when a named chapter (or a skippable stamp) begins there.
 */
internal fun List<AnimeChapter>.tickFractions(duration: Double): List<Float> {
    if (!duration.isFinite() || duration <= 0) return emptyList()
    return filter { it.start > 0 || it.name != null || it.skip != null }
        .map { it.start }
        .filter { it < duration }
        .map { (it / duration).toFloat() }
        .distinct()
}
