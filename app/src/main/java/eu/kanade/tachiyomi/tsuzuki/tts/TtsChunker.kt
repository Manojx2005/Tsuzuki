package eu.kanade.tachiyomi.tsuzuki.tts

import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.TtsTextUtils

/** A chapter cut into speakable chunks, with where each chunk sits in the source text. */
internal class ChunkPlan(
    val chunks: List<String>,
    val paragraphIndexes: List<Int>,
    val startOffsets: List<Int>,
    val paragraphs: List<String>,
    /** True when the chunks were cut for the neural worker (at most [NeuralTtsProtocol.CHUNK_LENGTH] characters). */
    val forNeural: Boolean,
)

internal object TtsChunker {
    /** Cut [text] for the neural worker, or for the phone engine whose limit is [phoneMaxLength]. */
    fun plan(text: String, forNeural: Boolean, phoneMaxLength: Int): ChunkPlan {
        val maxLength = if (forNeural) NeuralTtsProtocol.CHUNK_LENGTH else phoneMaxLength.takeIf { it > 0 } ?: 4000
        val paragraphs = TtsTextUtils.newlineParagraphBounds(text).map { (start, end) -> text.substring(start, end) }
        val paragraphIndexes = mutableListOf<Int>()

        val chunks = if (paragraphs.size > 1) {
            paragraphs.flatMapIndexed { paragraphIndex, para ->
                val c = if (para.length <= maxLength) {
                    listOf(para)
                } else {
                    TtsTextUtils.splitTextForTts(para, maxLength, allowLongWords = !forNeural)
                }
                repeat(c.size) { paragraphIndexes.add(paragraphIndex) }
                c
            }
        } else if (text.length <= maxLength) {
            paragraphIndexes.add(0)
            listOf(text)
        } else {
            val c = TtsTextUtils.splitTextForTts(text, maxLength, allowLongWords = !forNeural)
            repeat(c.size) { paragraphIndexes.add(0) }
            c
        }

        // Char offset per chunk so Spannable-based highlight can locate the right occurrence.
        val offsets = mutableListOf<Int>()
        var searchFrom = 0
        for (chunk in chunks) {
            val idx = text.indexOf(chunk, searchFrom)
            if (idx >= 0) {
                offsets.add(idx)
                searchFrom = idx + chunk.length
            } else {
                offsets.add(searchFrom)
            }
        }
        return ChunkPlan(chunks, paragraphIndexes, offsets, paragraphs.map { it.take(180) }, forNeural)
    }

    /** The chunk in [new] that contains the text [oldIndex] of [old] started at (clamped to the last chunk). */
    fun remapIndex(old: ChunkPlan, oldIndex: Int, new: ChunkPlan): Int {
        val offset = old.startOffsets.getOrElse(oldIndex) { 0 }
        return new.startOffsets.indexOfLast { it <= offset }.coerceAtLeast(0)
            .coerceAtMost((new.chunks.size - 1).coerceAtLeast(0))
    }
}
