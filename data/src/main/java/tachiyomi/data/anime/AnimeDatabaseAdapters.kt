package tachiyomi.data.anime

import app.cash.sqldelight.ColumnAdapter
import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import tachiyomi.data.AlternativeTitlesColumnAdapter
import tachiyomi.data.Anime_history
import tachiyomi.data.Animes
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Episodes
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter

object AnimeDatabaseAdapters {
    val animes = Animes.Adapter(
        genreAdapter = StringListColumnAdapter,
        alternative_titlesAdapter = AlternativeTitlesColumnAdapter,
        update_strategyAdapter = object : ColumnAdapter<AnimeUpdateStrategy, Long> {
            override fun decode(databaseValue: Long): AnimeUpdateStrategy =
                AnimeUpdateStrategy.entries.firstOrNull { it.ordinal.toLong() == databaseValue }
                    ?: AnimeUpdateStrategy.ALWAYS_UPDATE

            override fun encode(value: AnimeUpdateStrategy): Long = value.ordinal.toLong()
        },
        fetch_typeAdapter = object : ColumnAdapter<FetchType, Long> {
            override fun decode(databaseValue: Long): FetchType =
                if (databaseValue == 0L) FetchType.Seasons else FetchType.Episodes

            override fun encode(value: FetchType): Long = value.ordinal.toLong()
        },
        memoAdapter = MemoColumnAdapter,
    )
    val episodes = Episodes.Adapter(memoAdapter = MemoColumnAdapter)
    val history = Anime_history.Adapter(last_seenAdapter = DateColumnAdapter)
}
