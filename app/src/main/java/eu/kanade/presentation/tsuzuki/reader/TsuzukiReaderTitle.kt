package eu.kanade.presentation.tsuzuki.reader

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme

/** Compact body-face reader chrome from the Reader and MangaPaged boards. */
@Composable
fun TsuzukiReaderTitle(title: String?, subtitle: String?, novel: Boolean = false) {
    val tokens = TsuzukiTheme.tokens
    if (tokens.style == TsuzukiStyle.NONE) {
        AppBarTitle(title = title, subtitle = subtitle)
        return
    }

    Column {
        title?.let {
            Text(
                text = if (tokens.isDot) it.uppercase() else it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (novel) FontWeight.ExtraBold else FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        subtitle?.let {
            Text(
                text = if (tokens.isDot) it.uppercase() else it,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (novel) 11.sp else 12.sp,
                    lineHeight = if (novel) 15.sp else 16.sp,
                    fontWeight = if (novel) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (novel) Modifier else Modifier.basicMarquee(repeatDelayMillis = 2_000),
            )
        }
    }
}
