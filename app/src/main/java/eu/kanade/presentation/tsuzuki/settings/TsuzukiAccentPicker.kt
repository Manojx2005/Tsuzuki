package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiAccentSwatch
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/** Settings > Appearance: a row of swatch circles for the Colour theme's accent. The selected one has a ring. */
@Composable
fun TsuzukiAccentPicker(uiPreferences: UiPreferences) {
    val pref = uiPreferences.tsuzukiColourAccent
    val selected by pref.collectAsState()
    val current = TsuzukiAccentSwatch.fromArgb(selected)
    val tokens = TsuzukiTheme.tokens
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            stringResource(MR.strings.tsz_appearance_accent),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(MR.strings.tsz_appearance_accent_sub),
            color = muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        FlowRow(
            Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TsuzukiAccentSwatch.entries.forEach { swatch ->
                val isSelected = swatch == current
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .tsuzukiClickable(role = Role.RadioButton) { pref.set(swatch.argb) }
                        .semantics {
                            this.selected = isSelected
                            role = Role.RadioButton
                            contentDescription = swatch.name.lowercase().replaceFirstChar { it.uppercase() }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(if (isSelected) 40.dp else 32.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, tokens.dotOn, CircleShape).padding(5.dp)
                                } else {
                                    Modifier
                                },
                            )
                            .clip(CircleShape)
                            .background(swatch.color),
                    )
                }
            }
        }
    }
}
