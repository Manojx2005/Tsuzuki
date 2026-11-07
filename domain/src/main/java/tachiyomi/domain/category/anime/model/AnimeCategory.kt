package tachiyomi.domain.category.anime.model

data class AnimeCategory(
    val id: Long,
    val name: String,
    val order: Long,
    val flags: Long,
) {
    val isSystem: Boolean get() = id == 0L
}
