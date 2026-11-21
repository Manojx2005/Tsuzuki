package eu.kanade.tachiyomi.util

import kotlinx.serialization.json.Json
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/*
 * AY6: the JSON helpers aniyomi extensions-lib declares (`eu.kanade.tachiyomi.util.JsonExtensionsKt`). They are
 * inline, so extensions mostly carry their own copy; the class is here so the few calls that link to it resolve.
 */

/** Parses this string as [T] with the app's [Json]. */
inline fun <reified T> String.parseAs(): T = Injekt.get<Json>().decodeFromString(this)

/** Applies [transform] first (to clean the text), then parses it as [T]. */
inline fun <reified T> String.parseAs(transform: (String) -> String): T = transform(this).parseAs()

/** Parses the response body as [T]. */
inline fun <reified T> Response.parseAs(): T = use { it.body.string() }.parseAs()

/** Parses the response body as [T] after [transform]. */
inline fun <reified T> Response.parseAs(transform: (String) -> String): T =
    use { transform(it.body.string()) }.parseAs()
