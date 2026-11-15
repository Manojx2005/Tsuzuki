package eu.kanade.tachiyomi.tsuzuki.anime.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import java.text.Collator
import java.util.Locale
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

@Composable
internal fun AnimeLanguageOrderDialog(
    kind: AnimeTrackKind,
    initial: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var order by remember(initial) { mutableStateOf(initial) }
    var choosing by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val off = stringResource(R.string.tsz_anime_player_track_off)
    val names = remember(locale, kind, off) {
        val collator = Collator.getInstance(locale)
        val languages = animeLanguageCodes.map { it to Locale.forLanguageTag(it).getDisplayLanguage(locale) }
            .sortedWith { a, b -> collator.compare(a.second, b.second) }.toMap()
        if (kind == AnimeTrackKind.SUBTITLE) {
            mapOf(AnimePlayerPreferences.SUBTITLES_OFF to off) + languages
        } else {
            languages
        }
    }
    fun move(from: Int, to: Int) {
        order = order.toMutableList().apply { add(to, removeAt(from)) }
    }
    fun back() {
        if (choosing) choosing = false else onDismiss()
    }
    AlertDialog(
        onDismissRequest = ::back,
        title = { Text(tsuzukiMenuLabel(stringResource(R.string.tsz_anime_player_preferred_languages))) },
        text = {
            Column {
                if (choosing) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tsuzukiMenuLabel(mokoStringResource(MR.strings.action_search))) },
                        singleLine = true,
                    )
                    val available = names.entries.filter { (code, label) ->
                        code !in order && (label.contains(search, ignoreCase = true) || code.contains(search, true))
                    }
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(available, key = { it.key }) { (code, label) ->
                            TextButton(onClick = {
                                order = order + code
                                choosing = false
                            }) { Text(tsuzukiMenuLabel(label)) }
                        }
                    }
                } else {
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        itemsIndexed(order, key = { _, code -> code }) { index, code ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    tsuzukiMenuLabel(names[code] ?: code),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                IconButton(onClick = { move(index, index - 1) }, enabled = index > 0) {
                                    Icon(
                                        Icons.Outlined.KeyboardArrowUp,
                                        mokoStringResource(MR.strings.tsz_query_move_up),
                                    )
                                }
                                IconButton(onClick = { move(index, index + 1) }, enabled = index < order.lastIndex) {
                                    Icon(
                                        Icons.Outlined.KeyboardArrowDown,
                                        mokoStringResource(MR.strings.tsz_query_move_down),
                                    )
                                }
                                IconButton(onClick = { order = order - code }) {
                                    Icon(Icons.Outlined.Close, mokoStringResource(MR.strings.action_remove))
                                }
                            }
                        }
                    }
                    TextButton(onClick = {
                        search = ""
                        choosing = true
                    }) {
                        Text(tsuzukiMenuLabel(mokoStringResource(MR.strings.action_add)))
                    }
                }
            }
        },
        confirmButton = {
            if (!choosing) {
                TextButton(onClick = { onSave(order) }) {
                    Text(tsuzukiMenuLabel(mokoStringResource(MR.strings.action_apply)))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = ::back) { Text(tsuzukiMenuLabel(mokoStringResource(MR.strings.action_cancel))) }
        },
    )
}
