package eu.kanade.tachiyomi.tsuzuki.anime.backup

import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.history.anime.model.AnimeHistory
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.track.anime.model.AnimeTrack

/** The import field numbers are shared with Aniyomi. Tsuzuki writes only its own range. */
internal enum class AnimeBackupFormat(val entries: Int, val categories: Int, val sources: Int) {
    TSUZUKI(8000, 8001, 8002),
    ANIYOMI(501, 502, 503),
    LEGACY_ANIYOMI(3, 4, 103),
    ;

    // Aniyomi writes milliseconds for its episode position and duration fields.
    fun episodeSeconds(value: Long): Long = if (this == TSUZUKI) value else value / 1_000
}

@Serializable
internal data class AnimeBackupEntry(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(2) val url: String,
    @ProtoNumber(3) val title: String = "",
    @ProtoNumber(4) val artist: String? = null,
    @ProtoNumber(5) val author: String? = null,
    @ProtoNumber(6) val description: String? = null,
    @ProtoNumber(7) val genre: List<String> = emptyList(),
    @ProtoNumber(8) val status: Long = 0,
    @ProtoNumber(9) val thumbnail: String? = null,
    @ProtoNumber(13) val dateAdded: Long = 0,
    @ProtoNumber(16) val episodes: List<AnimeBackupEpisode> = emptyList(),
    @ProtoNumber(17) val categories: List<Long> = emptyList(),
    @ProtoNumber(18) val tracks: List<AnimeBackupTrack> = emptyList(),
    @ProtoNumber(100) val favorite: Boolean = true,
    @ProtoNumber(101) val episodeFlags: Long = 0,
    @ProtoNumber(103) val viewer: Long = 0,
    @ProtoNumber(104) val history: List<AnimeBackupHistory> = emptyList(),
    @ProtoNumber(105) val updateStrategy: AnimeUpdateStrategy = AnimeUpdateStrategy.ALWAYS_UPDATE,
    @ProtoNumber(106) val lastModifiedAt: Long = 0,
    @ProtoNumber(107) val favoriteModifiedAt: Long? = null,
    @ProtoNumber(109) val version: Long = 0,
    @ProtoNumber(112) val memo: ByteArray? = null,
    @ProtoNumber(500) val background: String? = null,
    @ProtoNumber(502) val parentId: Long? = null,
    @ProtoNumber(503) val oldId: Long? = null,
    @ProtoNumber(504) val seasonFlags: Long = 0,
    @ProtoNumber(505) val seasonNumber: Double = -1.0,
    @ProtoNumber(506) val seasonSourceOrder: Long = 0,
    @ProtoNumber(507) val fetchType: FetchType = FetchType.Episodes,
    @ProtoNumber(8100) val notes: String = "",
    @ProtoNumber(8101) val alternativeTitles: List<String> = emptyList(),
    @ProtoNumber(8102) val initialized: Boolean = false,
) {
    fun toAnime() = Anime(
        source = source,
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = genre.ifEmpty { null },
        alternativeTitles = alternativeTitles.ifEmpty { null },
        status = status,
        thumbnailUrl = thumbnail,
        backgroundUrl = background,
        favorite = favorite,
        initialized = initialized,
        viewer = viewer,
        episodeFlags = episodeFlags,
        dateAdded = dateAdded,
        updateStrategy = updateStrategy,
        fetchType = fetchType,
        seasonFlags = seasonFlags,
        seasonNumber = seasonNumber,
        seasonSourceOrder = seasonSourceOrder,
        notes = notes,
        lastModifiedAt = lastModifiedAt,
        favoriteModifiedAt = favoriteModifiedAt,
        version = version,
        memo = memo?.let(MemoColumnAdapter::decode) ?: JsonObject(emptyMap()),
    )
}

@Serializable
internal data class AnimeBackupEpisode(
    @ProtoNumber(1) val url: String,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val group: String? = null,
    @ProtoNumber(4) val seen: Boolean = false,
    @ProtoNumber(5) val bookmark: Boolean = false,
    @ProtoNumber(6) val resumeSeconds: Long = 0,
    @ProtoNumber(7) val dateFetch: Long = 0,
    @ProtoNumber(8) val dateUpload: Long = 0,
    @ProtoNumber(9) val legacyNumber: Float = 0f,
    @ProtoNumber(10) val sourceOrder: Long = 0,
    @ProtoNumber(11) val lastModifiedAt: Long = 0,
    @ProtoNumber(12) val version: Long = 0,
    @ProtoNumber(13) val memo: ByteArray? = null,
    @ProtoNumber(16) val durationSeconds: Long = 0,
    @ProtoNumber(501) val filler: Boolean = false,
    @ProtoNumber(502) val summary: String? = null,
    @ProtoNumber(503) val preview: String? = null,
    @ProtoNumber(8000) val preciseNumber: Double? = null,
) {
    fun toEpisode(animeId: Long, format: AnimeBackupFormat) = Episode(
        animeId = animeId,
        url = url,
        name = name,
        scanlator = group,
        seen = seen,
        bookmark = bookmark,
        fillermark = filler,
        lastSecondSeen = format.episodeSeconds(resumeSeconds).coerceAtLeast(0),
        totalSeconds = format.episodeSeconds(durationSeconds).coerceAtLeast(0),
        episodeNumber = (preciseNumber ?: legacyNumber.toDouble()).takeIf(Double::isFinite) ?: -1.0,
        sourceOrder = sourceOrder,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        summary = summary,
        previewUrl = preview,
        lastModifiedAt = lastModifiedAt,
        version = version,
        memo = memo?.let(MemoColumnAdapter::decode) ?: JsonObject(emptyMap()),
    )
}

@Serializable
internal data class AnimeBackupTrack(
    @ProtoNumber(1) val service: Long,
    @ProtoNumber(2) val libraryId: Long = 0,
    @ProtoNumber(3) val legacyMediaId: Long = 0,
    @ProtoNumber(4) val remoteUrl: String = "",
    @ProtoNumber(5) val title: String = "",
    @ProtoNumber(6) val legacyProgress: Float = 0f,
    @ProtoNumber(7) val episodeCount: Long = 0,
    @ProtoNumber(8) val legacyScore: Float = 0f,
    @ProtoNumber(9) val status: Long = 0,
    @ProtoNumber(10) val startDate: Long = 0,
    @ProtoNumber(11) val finishDate: Long = 0,
    @ProtoNumber(12) val isPrivate: Boolean = false,
    @ProtoNumber(100) val mediaId: Long = 0,
    @ProtoNumber(8000) val malId: Long? = null,
    @ProtoNumber(8001) val progress: Double? = null,
    @ProtoNumber(8002) val score: Double? = null,
) {
    fun toTrack(animeId: Long): AnimeTrack? {
        val remote = legacyMediaId.takeIf { it > 0 } ?: mediaId.takeIf { it > 0 } ?: return null
        val restoredProgress = (progress ?: legacyProgress.toDouble()).takeIf(Double::isFinite) ?: 0.0
        return AnimeTrack(
            animeId = animeId,
            syncId = service,
            remoteId = remote,
            title = title,
            remoteUrl = remoteUrl,
            libraryId = libraryId.takeIf { it > 0 },
            lastEpisodeSeen = restoredProgress.coerceAtLeast(0.0),
            totalEpisodes = episodeCount.coerceAtLeast(0),
            score = (score ?: legacyScore.toDouble()).takeIf(Double::isFinite) ?: 0.0,
            status = status,
            startDate = startDate,
            finishDate = finishDate,
            isPrivate = isPrivate,
            malId = malId?.takeIf { it > 0 } ?: remote.takeIf { service == 1L },
        )
    }
}

@Serializable
internal data class AnimeBackupHistory(
    @ProtoNumber(1) val episodeUrl: String,
    @ProtoNumber(2) val lastSeen: Long,
    @ProtoNumber(3) val watchSeconds: Long = 0,
)

@Serializable
internal data class AnimeBackupCategory(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val order: Long = 0,
    @ProtoNumber(3) val id: Long = 0,
    @ProtoNumber(100) val flags: Long = 0,
)

@Serializable
internal data class AnimeBackupSource(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val id: Long,
    @ProtoNumber(8000) val lang: String = "",
)

internal class AnimeBackupSummary {
    private val counts = mutableMapOf<AnimeBackupFormat, Int>()
    private val categories = mutableMapOf<AnimeBackupFormat, MutableList<AnimeBackupCategory>>()
    private val sources = mutableMapOf<AnimeBackupFormat, MutableList<AnimeBackupSource>>()

    val format: AnimeBackupFormat? get() = AnimeBackupFormat.entries.firstOrNull {
        counts.containsKey(it) || categories.containsKey(it) || sources.containsKey(it)
    }
    val count: Int get() = counts[format] ?: 0
    val shelves: List<AnimeBackupCategory> get() = categories[format].orEmpty()
    val sourceNames: List<AnimeBackupSource> get() = sources[format].orEmpty()

    fun record(field: Int, bytes: ByteArray, parser: ProtoBuf) {
        val shape = AnimeBackupFormat.entries.firstOrNull {
            field == it.entries || field == it.categories || field == it.sources
        } ?: return
        when (field) {
            shape.entries -> counts[shape] = (counts[shape] ?: 0) + 1
            shape.categories -> categories.getOrPut(shape, ::mutableListOf)
                .add(parser.decodeFromByteArray(AnimeBackupCategory.serializer(), bytes))
            shape.sources -> sources.getOrPut(shape, ::mutableListOf)
                .add(parser.decodeFromByteArray(AnimeBackupSource.serializer(), bytes))
        }
    }
}

internal fun AnimeCategory.toAnimeBackup() = AnimeBackupCategory(name, order, id, flags)

internal fun AnimeHistory.toAnimeBackup(episodeUrl: String) =
    AnimeBackupHistory(episodeUrl, lastSeen?.time ?: 0, timeWatched)

internal fun Episode.toAnimeBackup() = AnimeBackupEpisode(
    url = url,
    name = name,
    group = scanlator,
    seen = seen,
    bookmark = bookmark,
    resumeSeconds = lastSecondSeen,
    dateFetch = dateFetch,
    dateUpload = dateUpload,
    sourceOrder = sourceOrder,
    lastModifiedAt = lastModifiedAt,
    version = version,
    memo = MemoColumnAdapter.encode(memo),
    durationSeconds = totalSeconds,
    filler = fillermark,
    summary = summary,
    preview = previewUrl,
    preciseNumber = episodeNumber,
)

internal fun AnimeTrack.toAnimeBackup() = AnimeBackupTrack(
    service = syncId,
    libraryId = libraryId ?: 0,
    remoteUrl = remoteUrl,
    title = title,
    episodeCount = totalEpisodes,
    status = status,
    startDate = startDate,
    finishDate = finishDate,
    isPrivate = isPrivate,
    mediaId = remoteId,
    malId = malId,
    progress = lastEpisodeSeen,
    score = score,
)
