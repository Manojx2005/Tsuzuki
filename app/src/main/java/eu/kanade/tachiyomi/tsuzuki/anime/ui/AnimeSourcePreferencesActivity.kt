package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import androidx.preference.DialogPreference
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.forEach
import androidx.preference.getOnBindEditTextListener
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.anime.applyAnimeScreenTransition
import eu.kanade.presentation.tsuzuki.browse.ANIME_SOURCE_ID
import eu.kanade.presentation.tsuzuki.components.TsuzukiErrorState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.sourcePreferences
import eu.kanade.tachiyomi.data.preference.SharedPreferencesDataStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.widget.TachiyomiTextInputEditText.Companion.setIncognito
import kotlinx.coroutines.CancellationException
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** True when an anime source has settings of its own to show (AY2). */
internal fun hasAnimeSourceSettings(source: AnimeSource?): Boolean = source is ConfigurableAnimeSource

/**
 * An anime source's own settings (AY2; Aniyomi's `AnimeSourcePreferencesScreen`): the extension builds the list into
 * an androidx `PreferenceScreen` stored under `source_<id>`, as for manga sources. Leaving the page after a change
 * calls [AnimeSourceManager.extensionsChanged], so the next load creates fresh source instances (sources often read
 * their base URL or quality once) and an open catalogue reloads.
 */
class AnimeSourcePreferencesActivity : BaseActivity() {
    private var setupError by mutableStateOf<Throwable?>(null)
    internal var changed = false

    override fun finish() {
        super.finish()
        applyAnimeScreenTransition(opening = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyAnimeScreenTransition(opening = true)
        val sourceId = intent.getLongExtra(ANIME_SOURCE_ID, -1L)
        if (!TsuzukiAnime.enabled || sourceId < 0) {
            finish()
            return
        }
        changed = savedInstanceState?.getBoolean(CHANGED) == true
        // A fragment restored after a configuration change or process death has no source yet; add a fresh one.
        supportFragmentManager.fragments.forEach { supportFragmentManager.commitNow { remove(it) } }
        val sources = Injekt.get<AnimeSourceManager>()
        setContent {
            TachiyomiTheme {
                val tokens = TsuzukiTheme.tokens
                val source by produceState<AnimeSourceLookup>(AnimeSourceLookup.Loading) {
                    value = try {
                        sources.ensureLoaded()
                        val found = sources.get(sourceId)?.source
                        if (found is ConfigurableAnimeSource) {
                            AnimeSourceLookup.Ready(found)
                        } else {
                            AnimeSourceLookup.Failed(
                                found?.name,
                                IllegalStateException("No settings for anime source $sourceId"),
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AnimeLog.w("Could not load anime source $sourceId for its settings", e)
                        AnimeSourceLookup.Failed(null, e)
                    }
                }
                val title = when (val lookup = source) {
                    is AnimeSourceLookup.Ready -> lookup.source.name
                    is AnimeSourceLookup.Failed -> lookup.name
                    AnimeSourceLookup.Loading -> null
                } ?: stringResource(MR.strings.source_settings)
                Scaffold(
                    modifier = Modifier.tsuzukiBackground(tokens),
                    containerColor = Color.Transparent,
                    topBar = { AppBar(title = tsuzukiMenuLabel(title), navigateUp = ::finish) },
                ) { padding ->
                    val modifier = Modifier.fillMaxSize().padding(padding)
                    val error = setupError ?: (source as? AnimeSourceLookup.Failed)?.error
                    when {
                        error != null -> TsuzukiErrorState(
                            error = error,
                            whatFailed = stringResource(MR.strings.source_settings),
                            modifier = modifier,
                            onCancel = ::finish,
                        )
                        source is AnimeSourceLookup.Ready -> {
                            val containerId = remember { View.generateViewId() }
                            AndroidView(
                                modifier = modifier,
                                factory = { context -> FragmentContainerView(context).apply { id = containerId } },
                                update = {
                                    if (supportFragmentManager.findFragmentById(containerId) == null) {
                                        supportFragmentManager.commit {
                                            add(containerId, AnimeSourcePreferencesFragment.getInstance(sourceId))
                                        }
                                    }
                                },
                            )
                        }
                        else -> LoadingScreen(modifier)
                    }
                }
            }
        }
    }

    internal fun onSetupFailed(error: Throwable) {
        setupError = error
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(CHANGED, changed)
    }

    override fun onDestroy() {
        if (isFinishing && changed) AnimeSourceManager.extensionsChanged()
        super.onDestroy()
    }

    companion object {
        private const val CHANGED = "changed"

        fun intent(context: Context, sourceId: Long): Intent =
            Intent(context, AnimeSourcePreferencesActivity::class.java).putExtra(ANIME_SOURCE_ID, sourceId)
    }
}

private sealed interface AnimeSourceLookup {
    data object Loading : AnimeSourceLookup
    data class Ready(val source: ConfigurableAnimeSource) : AnimeSourceLookup
    data class Failed(val name: String?, val error: Throwable) : AnimeSourceLookup
}

/** The manga `SourcePreferencesFragment`'s styling, for an anime source (no manga-only "reverse chapters" row). */
class AnimeSourcePreferencesFragment : PreferenceFragmentCompat() {
    private var preferences: SharedPreferences? = null
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        (activity as? AnimeSourcePreferencesActivity)?.changed = true
    }

    override fun getContext(): Context? {
        val superCtx = super.getContext() ?: return null
        val tv = TypedValue()
        superCtx.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        return ContextThemeWrapper(superCtx, tv.resourceId)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val sourceId = requireArguments().getLong(SOURCE_ID)
        val screen = preferenceManager.createPreferenceScreen(requireContext())
        preferenceScreen = screen
        val source = Injekt.get<AnimeSourceManager>().get(sourceId)?.source as? ConfigurableAnimeSource ?: return
        val prefs = source.sourcePreferences()
        preferences = prefs
        preferenceManager.preferenceDataStore = SharedPreferencesDataStore(prefs)
        try {
            source.setupPreferenceScreen(screen)
        } catch (e: Exception) {
            AnimeLog.w("Anime source ${source.name.take(40)} could not build its settings", e)
            (activity as? AnimeSourcePreferencesActivity)?.onSetupFailed(e)
            return
        }
        screen.forEach { pref ->
            pref.isIconSpaceReserved = false
            pref.isSingleLineTitle = false
            if (pref is DialogPreference && pref.dialogTitle.isNullOrEmpty()) {
                pref.dialogTitle = pref.title
            }
            if (pref is EditTextPreference) {
                val setListener = pref.getOnBindEditTextListener()
                pref.setOnBindEditTextListener {
                    setListener?.onBindEditText(it)
                    it.setIncognito(lifecycleScope)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferences?.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onPause() {
        preferences?.unregisterOnSharedPreferenceChangeListener(listener)
        super.onPause()
    }

    companion object {
        private const val SOURCE_ID = "source_id"

        fun getInstance(sourceId: Long): AnimeSourcePreferencesFragment =
            AnimeSourcePreferencesFragment().apply { arguments = Bundle().apply { putLong(SOURCE_ID, sourceId) } }
    }
}
