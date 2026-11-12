package eu.kanade.presentation.tsuzuki.settings

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import eu.kanade.tachiyomi.tsuzuki.backup.BackupExport
import eu.kanade.tachiyomi.tsuzuki.backup.TsuzukiBackupPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

/**
 * Backup's manual actions in the Tsuzuki themes (board 07-system "Backup"): "Create backup" and "Restore backup" as
 * two tiles. Sync providers (Drive / WebDAV) from the board don't exist in the app and are left out.
 */
@Composable
fun TsuzukiBackupTiles(lastAutoBackup: Long, onCreate: () -> Unit, onRestore: () -> Unit) {
    val preferences = remember { TsuzukiBackupPreferences(Injekt.get()) }
    val lastExport by preferences.lastExport.collectAsState()
    // One root: the preference slot stacks sibling roots on top of each other.
    Column(Modifier.fillMaxWidth()) {
        BackupStatusTile(BackupExport.fromPreference(lastExport, lastAutoBackup))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BackupTile(
                Icons.Outlined.FileDownload,
                stringResource(MR.strings.pref_create_backup),
                Modifier.weight(1f).fillMaxHeight(),
                onCreate,
            )
            BackupTile(
                Icons.Outlined.FileUpload,
                stringResource(MR.strings.pref_restore_backup),
                Modifier.weight(1f).fillMaxHeight(),
                onRestore,
            )
        }
    }
}

@Composable
private fun BackupStatusTile(export: BackupExport?) {
    val context = LocalContext.current
    val tokens = TsuzukiTheme.tokens
    val title = stringResource(MR.strings.tsz_backup_last_export)
    val detail = if (export == null) {
        stringResource(MR.strings.relative_time_span_never)
    } else {
        val date = Date(export.timestamp)
        val dateTime = DateFormat.getMediumDateFormat(context).format(date) + " " +
            DateFormat.getTimeFormat(context).format(date)
        export.sizeBytes?.let { "$dateTime · ${Formatter.formatFileSize(context, it)}" } ?: dateTime
    }
    TsuzukiTile(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        padding = 16.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.FileDownload,
                contentDescription = null,
                tint = tokens.onAccent,
                modifier = Modifier.size(52.dp).background(tokens.accent, CircleShape).padding(14.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (tokens.isDot) title.uppercase() else title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.dotOn,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = tokens.mutedText)
            }
        }
    }
}

@Composable
private fun BackupTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(24.dp)
    val press = remember { MutableInteractionSource() }
    Column(
        modifier
            .tsuzukiPress(press)
            .heightIn(min = 112.dp)
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .clickable(interactionSource = press, indication = ripple(), role = Role.Button, onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = t.dotOn, modifier = Modifier.size(24.dp))
        Spacer(Modifier.weight(1f))
        Text(
            if (t.isDot) label.uppercase() else label,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
            color = t.dotOn,
        )
    }
}
