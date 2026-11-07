package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.core.common.preference.Preference as StoredPreference

@Composable
internal fun tsuzukiLibraryUpdateGroup(
    stock: Preference.PreferenceGroup,
    library: LibraryPreferences,
): Preference.PreferenceGroup {
    val context = LocalContext.current
    val downloads = remember { Injekt.get<DownloadPreferences>() }
    val deviceKey = library.autoUpdateDeviceRestrictions.key()
    val smartUpdateKey = library.autoUpdateMangaRestrictions.key()
    val metadataKey = library.autoUpdateMetadata.key()
    val categoriesTitle = stringResource(MR.strings.categories)

    val items = stock.preferenceItems.toMutableList()
    val deviceIndex = items.indexOfFirst {
        it is Preference.PreferenceItem.MultiSelectListPreference<*> && it.preference.key() == deviceKey
    }
    val smartIndex = items.indexOfFirst {
        it is Preference.PreferenceItem.MultiSelectListPreference<*> && it.preference.key() == smartUpdateKey
    }
    if (deviceIndex >= 0 && smartIndex > deviceIndex) {
        items.add(deviceIndex + 1, items.removeAt(smartIndex))
    }
    val categoriesIndex = items.indexOfFirst {
        it is Preference.PreferenceItem.TextPreference &&
            it.title == categoriesTitle
    }
    val metadataIndex = items.indexOfFirst {
        it is Preference.PreferenceItem.SwitchPreference && it.preference.key() == metadataKey
    }
    if (categoriesIndex >= 0 && metadataIndex > categoriesIndex) {
        items.add(categoriesIndex, items.removeAt(metadataIndex))
    }

    val themedItems = buildList {
        for (item in items) {
            when (item) {
                is Preference.PreferenceItem.MultiSelectListPreference<*> -> when (item.preference.key()) {
                    deviceKey -> addAll(
                        tsuzukiDeviceRestrictionRows(library, scheduled = item.enabled) {
                            LibraryUpdateJob.setupTask(context)
                        },
                    )
                    smartUpdateKey -> addAll(tsuzukiMangaRestrictionRows(library))
                    else -> add(item)
                }
                is Preference.PreferenceItem.TextPreference ->
                    add(if (item.title == categoriesTitle) item.withCategorySummary() else item)
                else -> add(item)
            }
        }
        add(
            Preference.PreferenceItem.SwitchPreference(
                preference = downloads.downloadNewChapters,
                title = stringResource(MR.strings.pref_download_new),
            ),
        )
    }
    return stock.copy(preferenceItems = themedItems)
}

/** The stock device flags, exposed as individual board switches. */
@Composable
private fun tsuzukiDeviceRestrictionRows(
    library: LibraryPreferences,
    scheduled: Boolean,
    onDeviceRestrictionsChanged: () -> Unit,
): List<Preference.PreferenceItem.TextPreference> {
    val device = library.autoUpdateDeviceRestrictions
    return listOf(
        flagRow(
            device,
            DEVICE_ONLY_ON_WIFI,
            stringResource(MR.strings.connected_to_wifi),
            scheduled,
            onDeviceRestrictionsChanged,
        ),
        flagRow(device, DEVICE_CHARGING, stringResource(MR.strings.charging), scheduled, onDeviceRestrictionsChanged),
        flagRow(
            device,
            DEVICE_NETWORK_NOT_METERED,
            stringResource(MR.strings.network_not_metered),
            scheduled,
            onDeviceRestrictionsChanged,
        ),
    )
}

@Composable
private fun tsuzukiMangaRestrictionRows(library: LibraryPreferences): List<Preference.PreferenceItem.TextPreference> {
    val rules = library.autoUpdateMangaRestrictions
    return listOf(
        flagRow(rules, MANGA_NON_COMPLETED, stringResource(MR.strings.pref_update_only_non_completed)),
        flagRow(
            rules,
            MANGA_OUTSIDE_RELEASE_PERIOD,
            stringResource(MR.strings.pref_library_update_smart_update),
            subtitle = stringResource(MR.strings.pref_update_only_in_release_period),
        ),
        flagRow(rules, MANGA_HAS_UNREAD, stringResource(MR.strings.pref_update_only_completely_read)),
        flagRow(rules, MANGA_NON_READ, stringResource(MR.strings.pref_update_only_started)),
    )
}

@Composable
private fun flagRow(
    preference: StoredPreference<Set<String>>,
    key: String,
    title: String,
    enabled: Boolean = true,
    onChanged: () -> Unit = {},
    subtitle: String? = null,
): Preference.PreferenceItem.TextPreference {
    val flags by preference.collectAsState()
    return Preference.PreferenceItem.TextPreference(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        widget = { Switch(checked = key in flags, enabled = enabled, onCheckedChange = null) },
        onClick = {
            val current = preference.get()
            preference.set(if (key in current) current - key else current + key)
            onChanged()
        },
    )
}

private fun Preference.PreferenceItem.TextPreference.withCategorySummary(): Preference.PreferenceItem.TextPreference {
    val selection = subtitle.orEmpty()
    return copy(
        subtitle = null,
        widget = {
            val tokens = TsuzukiTheme.tokens
            Text(
                text = if (tokens.isDot) selection.uppercase() else selection,
                modifier = Modifier.widthIn(max = 144.dp),
                style = MaterialTheme.typography.labelLarge,
                color = tokens.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}
