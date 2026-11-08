package eu.kanade.tachiyomi.tsuzuki.anime.track

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.util.system.workManager
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimeTrackProgressJob(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!TsuzukiAnime.enabled) return Result.success()
        val animeId = inputData.getLong(ANIME_ID, -1)
        val number = inputData.getDouble(EPISODE_NUMBER, 0.0)
        if (animeId < 0 || !number.isFinite() || number < 1.0) return Result.failure()
        val tracking = Injekt.get<AnimeTracking>()
        if (!tracking.autoUpdate.get()) return Result.success()
        return if (tracking.syncProgress(animeId, number) && runAttemptCount < 5) Result.retry() else Result.success()
    }

    companion object {
        private const val ANIME_ID = "anime_id"
        private const val EPISODE_NUMBER = "episode_number"

        suspend fun enqueue(context: Context, episode: Episode) {
            if (!TsuzukiAnime.enabled || !episode.episodeNumber.isFinite() || episode.episodeNumber < 1.0) return
            if (!Injekt.get<AnimeTracking>().canAutoUpdate(episode.animeId)) return
            val work = OneTimeWorkRequestBuilder<AnimeTrackProgressJob>()
                .setInputData(workDataOf(ANIME_ID to episode.animeId, EPISODE_NUMBER to episode.episodeNumber))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            val key = "tsuzuki-anime-track-" + episode.animeId
            context.workManager.enqueueUniqueWork(key, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
        }
    }
}
