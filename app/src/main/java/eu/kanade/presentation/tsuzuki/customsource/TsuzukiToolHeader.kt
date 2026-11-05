package eu.kanade.presentation.tsuzuki.customsource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Back circle, display title and a muted subtitle (boards CustomSource / ElementSelector); [trailing] sits at the end. */
@Composable
internal fun TsuzukiToolHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = TsuzukiTheme.tokens
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .clip(CircleShape)
                .background(tokens.tile)
                .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, CircleShape) } ?: Modifier),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(MR.strings.action_webview_back),
                tint = tokens.dotOn,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = tsuzukiCase(title).orEmpty(),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                color = tokens.dotOn,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tsuzukiCase(subtitle).orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.dotOn.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}
