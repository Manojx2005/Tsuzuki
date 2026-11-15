package eu.kanade.tachiyomi.tsuzuki.tts

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope

private val FIRST_SENTENCE = Regex("^(.{12,}?[.!?][\"'”’)\\]]*)\\s+(\\S.*)$", RegexOption.DOT_MATCHES_ALL)

/** Split [chunk] after its first sentence so the first audio is short, or null when it has no useful boundary. */
internal fun splitFirstSentence(chunk: String): Pair<String, String>? =
    FIRST_SENTENCE.matchEntire(chunk)?.let { it.groupValues[1] to it.groupValues[2] }

private class Piece(val offset: Int, val text: String, val first: Boolean, val last: Boolean)

/**
 * Synthesize just one piece ahead while the current audio is playing. The first chunk is played as two pieces (its
 * first sentence, then the rest) so the first audio does not wait for a whole 180-character chunk. A chunk that is not
 * [covered] is never synthesized: playback stops there and [onUncovered] names it so the caller can speak that chunk
 * with the phone voice and resume afterwards.
 */
internal suspend fun <Audio : Any> playNeuralChunks(
    chunks: List<String>,
    firstIndex: Int,
    synthesize: suspend (String) -> Audio,
    play: suspend (Audio) -> Unit,
    onStart: (Int) -> Unit,
    onDone: () -> Unit,
    onChunkDone: (Int) -> Unit = {},
    covered: suspend (String) -> Boolean = { true },
    onUncovered: (Int) -> Unit = {},
    onFailure: (Int, Throwable) -> Unit,
) {
    val pieces = buildList {
        chunks.forEachIndexed { offset, chunk ->
            val split = if (offset == 0) splitFirstSentence(chunk) else null
            if (split != null) {
                add(Piece(offset, split.first, first = true, last = false))
                add(Piece(offset, split.second, first = false, last = true))
            } else {
                add(Piece(offset, chunk, first = true, last = true))
            }
        }
    }
    var index = firstIndex
    var uncovered: Int? = null
    try {
        supervisorScope {
            // null audio means the chunk is not covered by the voice pack.
            fun fetch(piece: Piece) = async<Audio?> {
                if (piece.first && !covered(chunks[piece.offset])) null else synthesize(piece.text)
            }
            var pending = fetch(pieces.first())
            for (n in pieces.indices) {
                val piece = pieces[n]
                index = firstIndex + piece.offset
                val audio = pending.await()
                currentCoroutineContext().ensureActive()
                if (audio == null) {
                    uncovered = index
                    return@supervisorScope
                }
                if (n < pieces.lastIndex) pending = fetch(pieces[n + 1])
                if (piece.first) onStart(index)
                play(audio)
                if (piece.last) onChunkDone(index)
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        onFailure(index, error)
        return
    } catch (error: LinkageError) {
        onFailure(index, error)
        return
    }
    uncovered?.let {
        onUncovered(it)
        return
    }
    onDone()
}
