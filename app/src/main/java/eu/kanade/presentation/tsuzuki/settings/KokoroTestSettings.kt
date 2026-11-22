package eu.kanade.presentation.tsuzuki.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.tsuzuki.tts.KokoroPack
import eu.kanade.tachiyomi.tsuzuki.tts.KokoroPackImporter
import eu.kanade.tachiyomi.tsuzuki.tts.neuralTtsDebugPreference
import eu.kanade.tachiyomi.tsuzuki.tts.neuralTtsSupported
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.core.common.preference.Preference as CorePreference

/**
 * Kokoro natural voice (arm64 only): the switch, and import/remove of the voice pack zip. Import and remove need an
 * Activity (document picker), so they are offered only when [interactive]; settings search lists just the switch.
 */
@Composable
internal fun kokoroTestPreferences(interactive: Boolean): List<Preference> {
    if (!neuralTtsSupported()) return emptyList()
    val context = LocalContext.current
    val switch = remember { neuralTtsDebugPreference() }
    val switchOn by switch.collectAsState()
    var revision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    val installed by produceState(false, interactive, revision) {
        if (interactive) value = KokoroPack(context).isInstalled()
    }
    val scope = rememberCoroutineScope()
    val installedMessage = stringResource(MR.strings.tsz_kokoro_test_imported)
    val removedMessage = stringResource(MR.strings.tsz_kokoro_test_removed)
    val failedMessage = stringResource(MR.strings.tsz_kokoro_test_import_failed)
    if (downloading && interactive) {
        val packUrl = remember { kokoroPackUrlPreference().get() }
        KokoroDownloadDialog(
            url = packUrl,
            onDismiss = { downloading = false },
            onInstalled = {
                downloading = false
                revision++
                context.toast(installedMessage)
            },
        )
    }
    val picker = if (interactive) {
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            busy = true
            scope.launch {
                val message = try {
                    // A slow document provider must not block the UI thread.
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(uri) ?: error("Could not open the file")
                        stream.use { KokoroPackImporter(KokoroPack(context)).import(it) }
                    }
                    installedMessage
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    "$failedMessage ${error.message ?: error.javaClass.simpleName}"
                } finally {
                    busy = false
                    revision++
                }
                context.toast(message)
            }
        }
    } else {
        null
    }
    val items = mutableListOf<Preference.PreferenceItem<out Any, out Any>>(
        Preference.PreferenceItem.SwitchPreference(
            preference = switch,
            title = stringResource(MR.strings.tsz_kokoro_test_title),
            subtitle = stringResource(
                if (installed) MR.strings.tsz_kokoro_test_ready else MR.strings.tsz_kokoro_test_missing,
            ),
            // The switch can always be turned off, even when the pack is missing or was removed.
            enabled = installed || switchOn,
        ),
    )
    if (picker != null) {
        items += Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.tsz_kokoro_download),
            subtitle = stringResource(MR.strings.tsz_kokoro_download_summary),
            enabled = !busy,
            onClick = { downloading = true },
        )
        items += Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.tsz_kokoro_test_import),
            subtitle = stringResource(
                if (busy) MR.strings.tsz_kokoro_test_importing else MR.strings.tsz_kokoro_test_import_summary,
            ),
            enabled = !busy,
            onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream")) },
        )
        if (installed) {
            items += Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.tsz_kokoro_test_remove),
                enabled = !busy,
                onClick = {
                    scope.launch {
                        switch.set(false)
                        KokoroPack(context).remove()
                        revision++
                        context.toast(removedMessage)
                    }
                },
            )
        }
    }
    return listOf(
        Preference.PreferenceGroup(
            title = stringResource(MR.strings.tsz_kokoro_test_title),
            preferenceItems = items,
        ),
    )
}

/**
 * Where the voice pack downloads from: the `voices-v1` release asset.
 * A tester can point it at a local server: `adb reverse` + editing this preference with `run-as`.
 */
internal fun kokoroPackUrlPreference() = Injekt.get<PreferenceStore>().getString(
    CorePreference.appStateKey("tsz_kokoro_pack_url"),
    "https://github.com/Manojx2005/Tsuzuki/releases/download/voices-v1/kokoro-cmu-v1.zip",
)
