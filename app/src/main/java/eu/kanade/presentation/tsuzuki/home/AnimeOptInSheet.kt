package eu.kanade.presentation.tsuzuki.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiAdaptiveSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
internal fun AnimeOptInSheet(supported: Boolean, onEnable: () -> Unit, onDismiss: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiAdaptiveSheet(onDismissRequest = onDismiss) { close ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_media)),
                color = tokens.dotOn,
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = androidStringResource(
                    if (supported) R.string.tsz_anime_beta_summary else R.string.tsz_anime_player_unsupported_device,
                ),
                color = tokens.mutedText,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { close(onDismiss) },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.tile, contentColor = tokens.dotOn),
                ) {
                    Text(tsuzukiMenuLabel(stringResource(MR.strings.action_not_now)))
                }
                if (supported) {
                    Button(
                        onClick = { close(onEnable) },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accent,
                            contentColor = tokens.onAccent,
                        ),
                    ) {
                        Text(tsuzukiMenuLabel(androidStringResource(R.string.tsz_anime_turn_on)))
                    }
                }
            }
        }
    }
}
