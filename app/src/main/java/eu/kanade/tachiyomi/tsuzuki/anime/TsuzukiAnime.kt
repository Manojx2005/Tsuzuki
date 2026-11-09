package eu.kanade.tachiyomi.tsuzuki.anime

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadJob
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloads
import eu.kanade.tachiyomi.tsuzuki.anime.ui.supportsAnimePlayback
import eu.kanade.tachiyomi.tsuzuki.widget.setWatchingWidgetAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal object TsuzukiAnime {
    @Volatile
    private var gate: AnimeFeatureGate? = null

    val supported: Boolean get() = supportsAnimePlayback()

    val enabled: Boolean get() = gate?.enabled == true

    fun preference(store: PreferenceStore): Preference<Boolean> = store.getBoolean("tsuzuki_anime_enabled", false)

    fun initialize(context: Context, scope: CoroutineScope) {
        val app = context.applicationContext
        val availability = AnimeFeatureGate(supported, preference(Injekt.get()))
        gate = availability
        scope.launch {
            availability.collectChanges { enabled ->
                setWatchingWidgetAvailable(app, enabled)
                if (enabled) {
                    AnimeLibraryUpdateJob.setupTask(app)
                    scope.launch(Dispatchers.IO) { Injekt.get<AnimeDownloads>().restoreWhenEnabled() }
                } else {
                    AnimeLibraryUpdateJob.cancel(app)
                    AnimeDownloadJob.cancel(app)
                }
            }
        }
    }
}

internal class AnimeFeatureGate(private val supported: Boolean, private val preference: Preference<Boolean>) {
    var enabled by mutableStateOf(supported && preference.get())
        private set

    suspend fun collectChanges(onChange: suspend (Boolean) -> Unit) {
        preference.changes()
            .map { supported && it }
            .distinctUntilChanged()
            .collect {
                enabled = it
                onChange(it)
            }
    }
}
