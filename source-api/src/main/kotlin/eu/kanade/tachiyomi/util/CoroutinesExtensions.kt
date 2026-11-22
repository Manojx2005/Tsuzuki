package eu.kanade.tachiyomi.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/*
 * AY6: the parallel helpers aniyomi extensions-lib 16/17 declares (`eu.kanade.tachiyomi.util.CoroutinesExtensionsKt`).
 * Extensions call them to load several hosters or pages at once; without them those extensions fail with
 * NoSuchMethodError. Same names and JVM signatures as the stubs; work runs on IO.
 */

/** Runs [f] on every element at once and returns the results in order. */
suspend fun <A, B> Iterable<A>.parallelMap(f: suspend (A) -> B): List<B> =
    withContext(Dispatchers.IO) { coroutineScope { map { async { f(it) } }.awaitAll() } }

/** [parallelMap] without the nulls. */
suspend fun <A, B> Iterable<A>.parallelMapNotNull(f: suspend (A) -> B?): List<B> = parallelMap(f).filterNotNull()

/** [parallelMap] then flattened. */
suspend fun <A, B> Iterable<A>.parallelFlatMap(f: suspend (A) -> Iterable<B>): List<B> = parallelMap(f).flatten()

/** [parallelFlatMap], where an element that throws contributes nothing instead of failing the whole call. */
suspend fun <A, B> Iterable<A>.parallelCatchingFlatMap(f: suspend (A) -> Iterable<B>): List<B> =
    parallelMap { item ->
        try {
            f(item)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            emptyList()
        }
    }.flatten()

fun <A, B> Iterable<A>.parallelMapBlocking(f: suspend (A) -> B): List<B> = runBlocking { parallelMap(f) }

fun <A, B> Iterable<A>.parallelMapNotNullBlocking(f: suspend (A) -> B?): List<B> =
    runBlocking { parallelMapNotNull(f) }

fun <A, B> Iterable<A>.parallelFlatMapBlocking(f: suspend (A) -> Iterable<B>): List<B> =
    runBlocking { parallelFlatMap(f) }

fun <A, B> Iterable<A>.parallelCatchingFlatMapBlocking(f: suspend (A) -> Iterable<B>): List<B> =
    runBlocking { parallelCatchingFlatMap(f) }
