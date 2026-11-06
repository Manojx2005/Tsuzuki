package eu.kanade.presentation.tsuzuki.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.reader.settings.ColorPickerDialog
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.tsuzuki.BOOK_PAGE_THEME
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

internal enum class NovelPageColour(
    val key: String,
    val label: StringResource,
    val fixedColours: Pair<Int, Int>? = null,
) {
    BOOK(BOOK_PAGE_THEME, MR.strings.tsz_rs_page_book),
    PAPER("paper", MR.strings.tsz_rs_page_paper, 0xFFFAFAF7.toInt() to 0xFF1F2024.toInt()),
    SEPIA("sepia", MR.strings.tsz_rs_page_sepia, 0xFFF4ECD8.toInt() to 0xFF5B4636.toInt()),
    SAGE("sage", MR.strings.tsz_rs_page_sage, 0xFFDDE5D6.toInt() to 0xFF25301F.toInt()),
    NIGHT("night", MR.strings.tsz_rs_page_night, 0xFF15140F.toInt() to 0xFFDED7C9.toInt()),
    BLACK("black", MR.strings.tsz_rs_page_black, 0xFF000000.toInt() to 0xFFCCCCCC.toInt()),
    CUSTOM("custom", MR.strings.tsz_rs_page_custom),
}

@Composable
internal fun TsuzukiPageColourPicker(
    preferences: ReaderPreferences,
    modifier: Modifier = Modifier,
    includeCustom: Boolean = true,
    columns: Int = 4,
) {
    val tokens = TsuzukiTheme.tokens
    val theme by preferences.novelTheme.collectAsState()
    val customInk by preferences.novelFontColor.collectAsState()
    val customPage by preferences.novelBackgroundColor.collectAsState()
    var editCustom by rememberSaveable { mutableStateOf<String?>(null) }
    val rowSize = if (LocalDensity.current.fontScale >= 1.5f) minOf(columns, 3) else columns
    val options = NovelPageColour.entries.filter { includeCustom || it != NovelPageColour.CUSTOM }
    Column(modifier.fillMaxWidth()) {
        TsuzukiSectionLabel(stringResource(MR.strings.tsz_rs_page_colour))
        Column(
            Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            options.chunked(rowSize).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.forEach { option ->
                        val selectedOption = theme == option.key
                        val label = stringResource(option.label)
                        val colours = option.fixedColours
                        val background = when (option) {
                            NovelPageColour.BOOK -> tokens.background
                            NovelPageColour.CUSTOM -> if (customPage == 0) tokens.tile else Color(customPage)
                            else -> Color(requireNotNull(colours).first)
                        }
                        val ink = when (option) {
                            NovelPageColour.BOOK -> tokens.dotOn
                            NovelPageColour.CUSTOM -> if (customInk == 0 ||
                                customPage == 0
                            ) {
                                tokens.dotOn
                            } else {
                                Color(customInk)
                            }
                            else -> Color(requireNotNull(colours).second)
                        }
                        Column(
                            Modifier.weight(1f).heightIn(min = 48.dp)
                                .semantics {
                                    selected = selectedOption
                                    contentDescription = label
                                }
                                .tsuzukiClickable(role = Role.RadioButton) { preferences.novelTheme.set(option.key) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                Modifier.size(if (columns == 6) 46.dp else 54.dp)
                                    .then(
                                        if (selectedOption) {
                                            Modifier.border(
                                                2.dp,
                                                tokens.dotOn,
                                                CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(background)
                                    .border(1.dp, tokens.track, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (option == NovelPageColour.CUSTOM) "+" else "Aa",
                                    color = ink,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Text(
                                tsuzukiMenuLabel(label),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    repeat(rowSize - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (includeCustom && theme == NovelPageColour.CUSTOM.key) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                TextButton(onClick = { editCustom = "ink" }) {
                    Text(tsuzukiMenuLabel(stringResource(MR.strings.tsz_rs_custom_ink)))
                }
                TextButton(onClick = { editCustom = "page" }) {
                    Text(tsuzukiMenuLabel(stringResource(MR.strings.tsz_rs_custom_page)))
                }
            }
        }
    }
    editCustom?.let { field ->
        val ink = field == "ink"
        ColorPickerDialog(
            title = tsuzukiMenuLabel(
                stringResource(if (ink) MR.strings.tsz_rs_custom_ink else MR.strings.tsz_rs_custom_page),
            ),
            initialColor = if (ink) {
                customInk.takeUnless { it == 0 } ?: 0xFF000000.toInt()
            } else {
                customPage.takeUnless { it == 0 } ?: 0xFFFFFFFF.toInt()
            },
            onDismiss = { editCustom = null },
            onConfirm = { color ->
                if (ink) preferences.novelFontColor.set(color) else preferences.novelBackgroundColor.set(color)
                editCustom = null
            },
        )
    }
}
