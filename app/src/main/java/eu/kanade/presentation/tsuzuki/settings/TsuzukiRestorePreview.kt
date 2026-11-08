package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat

@Composable
internal fun TsuzukiRestorePreview(contents: BackupFileValidator.Contents) {
    val tokens = TsuzukiTheme.tokens
    val format = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
    val heading = stringResource(MR.strings.tsz_backup_contents)
    val counts = listOf(
        contents.books.toLong() to stringResource(MR.strings.tsz_backup_books),
        contents.shelves.toLong() to stringResource(MR.strings.tsz_backup_shelves),
        contents.historyEntries to stringResource(MR.strings.tsz_backup_history_entries),
    )
    TsuzukiTile(Modifier.fillMaxWidth(), padding = 16.dp) {
        Text(
            text = if (tokens.isDot) heading.uppercase() else heading,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = tokens.dotOn,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            counts.forEach { (count, label) ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(format.format(count), style = MaterialTheme.typography.titleLarge, color = tokens.dotOn)
                    Text(
                        text = if (tokens.isDot) label.uppercase() else label,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.mutedText,
                    )
                }
            }
        }
    }
}
