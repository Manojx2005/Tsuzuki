package tachiyomi.domain.entries.anime.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/** Memo key of the user's own details for an anime (AP7-EDIT). Source refreshes never write it. */
const val ANIME_USER_EDITS = "tsuzuki:user-edits"

/**
 * The details the user changed by hand ("Edit info"). A null field is not edited and shows the source value; an empty
 * text or list is an edit that clears the field. The source columns of the anime row are never changed, so removing
 * the edits (Reset) shows the source data again.
 */
data class AnimeUserEdits(
    val title: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val genres: List<String>? = null,
    val status: Long? = null,
) {
    val isEmpty get() = this == NONE

    companion object {
        val NONE = AnimeUserEdits()

        /** Keeps only the fields that differ from the source [anime]'s values; a blank title is not an edit. */
        fun diff(
            anime: Anime,
            title: String,
            author: String,
            artist: String,
            description: String,
            genres: List<String>,
            status: Long,
        ): AnimeUserEdits {
            fun text(value: String, source: String?) = value.trim().takeIf { it != source.orEmpty().trim() }
            val tags = genres.map(String::trim).filter(String::isNotEmpty).distinct()
            return AnimeUserEdits(
                title = title.trim().takeIf { it.isNotEmpty() && it != anime.title.trim() },
                author = text(author, anime.author),
                artist = text(artist, anime.artist),
                description = description.trim().takeIf { it != anime.description.orEmpty().trim() },
                genres = tags.takeIf { it != anime.genre.orEmpty().map(String::trim).filter(String::isNotEmpty) },
                status = status.takeIf { it != anime.status },
            )
        }
    }
}

fun JsonObject.animeUserEdits(): AnimeUserEdits {
    val edits = get(ANIME_USER_EDITS) as? JsonObject ?: return AnimeUserEdits.NONE
    fun text(key: String) = (edits[key] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull
    return AnimeUserEdits(
        title = text("title")?.takeIf(String::isNotBlank),
        author = text("author"),
        artist = text("artist"),
        description = text("description"),
        genres = (edits["genres"] as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content
        },
        status = (edits["status"] as? JsonPrimitive)?.takeUnless(JsonPrimitive::isString)?.longOrNull,
    )
}

/** This memo with [edits] stored under [ANIME_USER_EDITS], or without the key when there is nothing edited. */
fun JsonObject.withAnimeUserEdits(edits: AnimeUserEdits?): JsonObject {
    if (edits == null || edits.isEmpty) return JsonObject(this - ANIME_USER_EDITS)
    val fields = buildMap<String, JsonElement> {
        edits.title?.let { put("title", JsonPrimitive(it)) }
        edits.author?.let { put("author", JsonPrimitive(it)) }
        edits.artist?.let { put("artist", JsonPrimitive(it)) }
        edits.description?.let { put("description", JsonPrimitive(it)) }
        edits.genres?.let { genres -> put("genres", JsonArray(genres.map(::JsonPrimitive))) }
        edits.status?.let { put("status", JsonPrimitive(it)) }
    }
    return JsonObject(this + (ANIME_USER_EDITS to JsonObject(fields)))
}

/** The title to show: the user's edited title, else [sourceTitle]. */
fun JsonObject.editedAnimeTitle(sourceTitle: String): String = animeUserEdits().title ?: sourceTitle

/**
 * The anime as shown, with the user's edits applied. Only for display: never pass the result to a write (saving it
 * would turn the edits into source data).
 */
fun Anime.withUserEdits(): Anime {
    val edits = memo.animeUserEdits()
    if (edits.isEmpty) return this
    return copy(
        title = edits.title ?: title,
        author = editedText(edits.author, author),
        artist = editedText(edits.artist, artist),
        description = editedText(edits.description, description),
        genre = edits.genres ?: genre,
        status = edits.status ?: status,
    )
}

/** [edit] when the user changed the field (blank meaning cleared), else the [source] value. */
fun editedText(edit: String?, source: String?): String? = if (edit == null) source else edit.ifBlank { null }
