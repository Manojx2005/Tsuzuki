package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R

/**
 * A6.8 (board `remaining-2026-11/LocalAnime.dc.html`, "Empty folder"): the V8.2 empty state plus the exact folder
 * layout to copy. File names are shown as they must be typed, so they are never uppercased.
 */
@Composable
internal fun LocalAnimeEmptyFolder() {
    val tokens = TsuzukiTheme.tokens
    val body = MaterialTheme.typography.bodyLarge
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("(・_・?)", style = body.copy(fontSize = 30.sp, fontStyle = FontStyle.Normal), color = tokens.mutedText)
        Text(
            tsuzukiMenuLabel(stringResource(R.string.tsz_local_anime_empty)),
            style = body.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = tokens.dotOn,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.tsz_local_anime_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = tokens.mutedText,
            textAlign = TextAlign.Center,
        )
        TsuzukiTile(Modifier.fillMaxWidth(), padding = 12.dp) {
            Text(
                LOCAL_ANIME_LAYOUT,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 18.sp),
                color = tokens.dotOn,
            )
        }
    }
}

private val LOCAL_ANIME_LAYOUT = """
    localanime/
      Sample Show/
        cover.jpg
        Sample Show - 01.mkv
        Sample Show - 01.ass
        Sample Show - 02.mkv
""".trimIndent()
