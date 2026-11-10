package eu.kanade.tachiyomi.tsuzuki.anime

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import tachiyomi.domain.items.episode.model.Episode

internal const val ANIME_EXCLUDED_GROUPS = "tsuzuki:excluded-groups"

internal fun JsonObject.excludedAnimeGroups(): Set<String> =
    (get(ANIME_EXCLUDED_GROUPS) as? JsonArray).orEmpty().mapNotNullTo(mutableSetOf()) {
        (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content?.trim()?.takeIf(String::isNotEmpty)
    }

internal fun JsonObject.withExcludedAnimeGroups(groups: Set<String>): JsonObject = JsonObject(
    if (groups.isEmpty()) {
        this - ANIME_EXCLUDED_GROUPS
    } else {
        this + (ANIME_EXCLUDED_GROUPS to JsonArray(groups.sorted().map(::JsonPrimitive)))
    },
)

internal fun String?.isExcludedAnimeGroup(excluded: Set<String>): Boolean {
    val group = this?.trim()?.takeIf(String::isNotEmpty) ?: return false
    return excluded.any { it.equals(group, ignoreCase = true) }
}

internal fun List<Episode>.filterAnimeGroups(excluded: Set<String>, keepEpisodeId: Long? = null): List<Episode> =
    if (excluded.isEmpty()) this else filter { it.id == keepEpisodeId || !it.scanlator.isExcludedAnimeGroup(excluded) }
