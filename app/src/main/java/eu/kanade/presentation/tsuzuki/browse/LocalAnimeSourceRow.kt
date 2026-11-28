package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel

/**
 * A6.8 (board `remaining-2026-11/LocalAnime.dc.html`): the Local anime row under "On this device" in
 * Browse › Watch, outlined in the accent like the board; a tap opens its catalogue.
 */
@Composable
internal fun LocalAnimeSourceRow(name: String, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(tsuzukiGroupedTile(first = true, last = true))
            .border(1.5.dp, tokens.accent, shape)
            .heightIn(min = 60.dp)
            .tsuzukiClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.VideoLibrary,
            contentDescription = null,
            tint = tokens.accent,
            modifier = Modifier.size(28.dp),
        )
        Text(tsuzukiMenuLabel(name), style = MaterialTheme.typography.titleSmall, color = tokens.dotOn)
    }
}
