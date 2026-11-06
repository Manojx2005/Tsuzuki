package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

internal data class AdvancedPreferences(
    val primary: List<Preference>,
    val advanced: List<Preference>,
) {
    fun containsHighlight(highlight: String?): Boolean = highlight != null && advanced.any {
        it.title == highlight || (
            it is Preference.PreferenceGroup && it.preferenceItems.any { item -> item.title == highlight }
            )
    }
}

internal fun splitAdvancedPreferences(
    items: List<Preference>,
    groupTitles: Set<String>,
    itemTitles: Set<String>,
): AdvancedPreferences {
    val primary = mutableListOf<Preference>()
    val advanced = mutableListOf<Preference>()
    items.forEach { item ->
        when {
            item is Preference.PreferenceGroup && item.title in groupTitles -> {
                if (item.enabled) advanced += item
            }
            item is Preference.PreferenceGroup -> {
                val (rare, common) = item.preferenceItems.partition { it.title in itemTitles }
                if (common.isNotEmpty()) primary += item.copy(preferenceItems = common)
                if (item.enabled && rare.isNotEmpty()) advanced += item.copy(preferenceItems = rare)
            }
            item.title in itemTitles -> advanced += item
            else -> primary += item
        }
    }
    return AdvancedPreferences(primary, advanced)
}

@Composable
internal fun tsuzukiAdvancedPreferences(items: List<Preference>, settingsTitle: StringResource?): AdvancedPreferences {
    if (!isTsuzukiStyle()) return AdvancedPreferences(items, emptyList())
    val groupTitles = when (settingsTitle) {
        MR.strings.pref_category_library -> setOf(stringResource(MR.strings.pref_behavior))
        MR.strings.pref_category_reader -> setOf("E-Ink", stringResource(MR.strings.pref_reader_actions))
        MR.strings.pref_category_downloads -> setOf(stringResource(MR.strings.pref_category_delete_chapters))
        TDMR.strings.pref_category_novel_downloads -> setOf(
            stringResource(TDMR.strings.pref_novel_request_throttling_category),
            stringResource(TDMR.strings.pref_novel_mass_import_category),
            "Per-Extension Overrides",
        )
        TDMR.strings.pref_category_novel -> setOf(
            "Formatting",
            "Content",
            stringResource(TDMR.strings.pref_novel_auto_scroll),
        )
        MR.strings.label_data_storage -> setOf(stringResource(MR.strings.tsz_data_cleanup))
        else -> emptySet()
    }
    val itemTitles = when (settingsTitle) {
        MR.strings.pref_category_library -> setOf(
            stringResource(MR.strings.pref_library_update_refresh_metadata),
            stringResource(TDMR.strings.skip_recently_updated_title),
        )
        MR.strings.browse -> setOf("Page load delay", "Skip cover loading", "Confirm back after pages")
        MR.strings.pref_category_tracking -> setOf(
            "Minimum chapters before tracking (Manga)",
            "Minimum chapters before tracking (Novels)",
        )
        else -> emptySet()
    }
    return if (groupTitles.isEmpty() && itemTitles.isEmpty()) {
        AdvancedPreferences(items, emptyList())
    } else {
        splitAdvancedPreferences(items, groupTitles, itemTitles)
    }
}

@Composable
internal fun advancedPreferenceToggle(expanded: Boolean, onClick: () -> Unit): Preference.PreferenceGroup =
    Preference.PreferenceGroup(
        title = stringResource(MR.strings.pref_category_advanced),
        preferenceItems = listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.tsz_q9_advanced_options),
                widget = { Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null) },
                onClick = onClick,
            ),
        ),
    )
