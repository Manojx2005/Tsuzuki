package eu.kanade.presentation.tsuzuki.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal fun LazyListScope.tsuzukiUpdatesLoading() {
    item(key = "updates_loading_label") {
        Text(
            tsuzukiMenuLabel(stringResource(MR.strings.tsz_q9_loading_updates)),
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    items(7, key = { "updates_loading_row_$it" }, contentType = { "updates_loading_row" }) { index ->
        val fill = TsuzukiTheme.tokens.tile
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp, 64.dp).background(fill, RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val titleWidth = if (index % 2 == 0) 0.68f else 0.58f
                Box(Modifier.fillMaxWidth(titleWidth).height(12.dp).background(fill, RoundedCornerShape(6.dp)))
                Box(Modifier.fillMaxWidth(0.44f).height(10.dp).background(fill, RoundedCornerShape(5.dp)))
            }
        }
    }
}
