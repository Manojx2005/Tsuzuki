package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Download queue header in the Tsuzuki themes (board 07-system "Downloads"): a Queued tile with the chapter count
 * across novels and manga, a Wi-Fi only chip when that setting is on, then the Novels / Manga / Translations tabs as
 * pills with their counts. Left out (not measured): storage size, download speed.
 */
@Composable
fun TsuzukiDownloadQueueHeader(
    labels: List<String>,
    counts: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val wifiOnly by remember { Injekt.get<DownloadPreferences>().downloadOnlyOverWifi }.collectAsState()
    val shape = RoundedCornerShape(t.tileRadius)
    Column {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .clip(shape)
                    .background(t.tile)
                    .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(
                    stringResource(MR.strings.tsz_downloads_queued).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.Bold,
                    color = muted,
                )
                Text(
                    "${counts.take(2).sum()}",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 44.sp),
                    color = Color.White,
                )
            }
            if (wifiOnly) {
                Box(
                    Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(t.tile)
                        .then(t.tileBorder?.let { Modifier.border(1.dp, it, RoundedCornerShape(19.dp)) } ?: Modifier)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val label = stringResource(MR.strings.connected_to_wifi)
                    Text(
                        if (t.isDot) label.uppercase() else label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
            }
        }
        TsuzukiPillRow(
            labels = labels,
            selected = selected,
            onClick = onSelect,
            counts = counts,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
