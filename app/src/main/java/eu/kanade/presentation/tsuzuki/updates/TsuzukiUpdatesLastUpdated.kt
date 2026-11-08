package eu.kanade.presentation.tsuzuki.updates

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.presentation.util.relativeTimeSpanString
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiUpdatesLastUpdated(lastUpdated: Long, modifier: Modifier = Modifier) {
    val caption = stringResource(MR.strings.updates_last_update_info, relativeTimeSpanString(lastUpdated))
    Text(
        text = tsuzukiCase(caption) ?: caption,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontStyle = FontStyle.Normal,
        color = TsuzukiTheme.tokens.mutedText,
    )
}
