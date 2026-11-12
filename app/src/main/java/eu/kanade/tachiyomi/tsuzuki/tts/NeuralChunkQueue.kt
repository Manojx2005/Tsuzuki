package eu.kanade.tachiyomi.tsuzuki.tts

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope

/** Synthesize just one chunk ahead while the current audio is playing. */
internal suspend fun <Audio> playNeuralChunks(
    chunks: List<String>,
    firstIndex: Int,
    synthesize: suspend (String) -> Audio,
    play: suspend (Audio) -> Unit,
    onStart: (Int) -> Unit,
    onDone: () -> Unit,
    onFailure: (Int, Throwable) -> Unit,
) {
    var index = firstIndex
    try {
        supervisorScope {
            var pending = async { synthesize(chunks.first()) }
            for (offset in chunks.indices) {
                val audio = pending.await()
                currentCoroutineContext().ensureActive()
                if (offset < chunks.lastIndex) pending = async { synthesize(chunks[offset + 1]) }
                onStart(index)
                play(audio)
                index++
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
    onDone()
}
