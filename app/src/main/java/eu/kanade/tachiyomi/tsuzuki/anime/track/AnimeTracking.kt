package eu.kanade.tachiyomi.tsuzuki.anime.track

import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository
import java.io.IOException

internal class AnimeTracking(
    private val trackers: TrackerManager,
    private val tracks: AnimeTrackRepository,
    preferences: PreferenceStore,
) {
    val autoUpdate = preferences.getBoolean("tsuzuki_anime_auto_update_track", true)
    val airing = AnimeAiringSchedule(preferences)
    private val writes = Mutex()

    val services: List<Tracker>
        get() = listOf(trackers.aniList, trackers.myAnimeList, trackers.kitsu)

    fun observe(animeId: Long) = tracks.subscribeByAnimeId(animeId)

    suspend fun canAutoUpdate(animeId: Long) =
        autoUpdate.get() && services.any { it.isLoggedIn } &&
            tracks.awaitByAnimeId(animeId).any { track -> services.any { it.id == track.syncId && it.isLoggedIn } }

    suspend fun search(trackerId: Long, query: String): List<AnimeTrackSearch> =
        checkNotNull(provider(trackerId)).search(query)

    suspend fun bind(animeId: Long, trackerId: Long, result: AnimeTrackSearch) = writes.withLock {
        val api = checkNotNull(provider(trackerId))
        val candidate = result.track(animeId, trackerId)
        val current = api.entry(candidate) ?: api.update(candidate, AnimeTrackEdit(status = AnimeTrackStatus.PLANNING))
        tracks.upsert(current)
        // A6.6: a new AniList binding shows its next airing time at once instead of after the next update.
        if (trackerId == trackers.aniList.id) {
            try {
                airing.put(animeId, trackers.aniList.anime.nextAiring(current.remoteId))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The next library update tries again.
            }
        }
    }

    suspend fun edit(selected: AnimeTrack, edit: AnimeTrackEdit) = writes.withLock {
        val api = checkNotNull(provider(selected.syncId))
        val current = checkNotNull(
            tracks.awaitByAnimeId(selected.animeId).firstOrNull {
                it.syncId == selected.syncId && it.remoteId == selected.remoteId
            },
        )
        val latest = if (current.syncId == TrackerManager.KITSU) checkNotNull(api.entry(current)) else current
        tracks.upsert(api.update(latest, edit))
    }

    suspend fun unlink(track: AnimeTrack) = writes.withLock {
        val current = tracks.awaitByAnimeId(track.animeId).firstOrNull { it.syncId == track.syncId }
        if (current?.remoteId == track.remoteId) tracks.delete(track.animeId, track.syncId)
    }

    suspend fun refresh(animeId: Long) = writes.withLock {
        for (local in tracks.awaitByAnimeId(animeId)) {
            val api = provider(local.syncId) ?: continue
            api.entry(local)?.let { tracks.upsert(it) }
        }
    }

    /**
     * A6.6: store AniList's next airing episode for [animeId] when it is bound to AniList (logged in); clears it
     * when the binding is gone or AniList has nothing scheduled. Errors keep the last stored value.
     */
    suspend fun refreshAiring(animeId: Long) {
        val track = tracks.awaitByAnimeId(animeId).firstOrNull { it.syncId == trackers.aniList.id }
        if (track == null || !trackers.aniList.isLoggedIn) {
            airing.put(animeId, null)
            return
        }
        airing.put(animeId, trackers.aniList.anime.nextAiring(track.remoteId))
    }

    suspend fun syncProgress(animeId: Long, number: Double): Boolean = writes.withLock {
        var retry = false
        for (local in tracks.awaitByAnimeId(animeId)) {
            val api = provider(local.syncId) ?: continue
            try {
                val remote = api.entry(local) ?: continue
                val progress = nextAnimeTrackProgress(remote, number)
                val updated = if (progress == null) remote else api.update(remote, AnimeTrackEdit(progress = progress))
                tracks.upsert(updated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                retry = true
            } catch (e: HttpException) {
                retry = retry || e.code == 429 || e.code in 500..599
            } catch (_: Exception) {
                // Provider errors may include account data. Do not log payloads or credentials.
            }
        }
        retry
    }

    private fun provider(id: Long): AnimeTrackerApi? = when {
        id == trackers.aniList.id && trackers.aniList.isLoggedIn -> trackers.aniList.anime
        id == trackers.myAnimeList.id && trackers.myAnimeList.isLoggedIn -> trackers.myAnimeList.anime
        id == trackers.kitsu.id && trackers.kitsu.isLoggedIn -> trackers.kitsu.anime
        else -> null
    }
}
