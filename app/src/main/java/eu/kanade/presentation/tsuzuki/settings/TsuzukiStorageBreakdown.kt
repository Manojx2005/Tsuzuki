package eu.kanade.presentation.tsuzuki.settings

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hippo.unifile.UniFile
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

/** The downloads folder can be a slow SAF tree; stop counting after this long and show a partial total. */
private const val DOWNLOADS_BUDGET_MS = 8_000L

private object StorageLog

/**
 * Storage card plus stat tiles for Settings, Data and storage in the Tsuzuki themes (board 08-tools "DataTools").
 * Everything is measured on the IO dispatcher when the screen opens, and each figure appears as soon as it is known
 * ("Calculating..." until then), so the main thread is never blocked.
 */
@Composable
fun TsuzukiStorageBreakdown(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var breakdown by remember { mutableStateOf(StorageBreakdown()) }
    LaunchedEffect(Unit) {
        withIOContext { measureStorage(context) { transform -> breakdown = transform(breakdown) } }
    }

    Column(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StorageCard(breakdown)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(breakdown.novels, stringResource(MR.strings.tsz_storage_novels), Modifier.weight(1f))
            StatTile(breakdown.manga, stringResource(MR.strings.tsz_storage_manga), Modifier.weight(1f))
            StatTile(breakdown.chapters, stringResource(MR.strings.tsz_storage_chapters), Modifier.weight(1f))
        }
    }
}

private suspend fun measureStorage(context: Context, update: ((StorageBreakdown) -> StorageBreakdown) -> Unit) {
    val job = currentCoroutineContext()
    val keepGoing = { job.isActive }

    fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return measureTree(
            dir,
            children = { it.listFiles()?.toList() },
            length = { it.length() },
            now = System::currentTimeMillis,
            deadline = Long.MAX_VALUE,
            keepGoing = keepGoing,
        ).bytes
    }

    runCatching {
        val dbDir = context.getDatabasePath("tachiyomi.db").parentFile
        val db = dbDir?.listFiles { f -> f.name.startsWith("tachiyomi.db") }?.sumOf { it.length() } ?: 0L
        update { it.copy(database = db) }
    }.onFailure { StorageLog.logcat(LogPriority.WARN, it) { "Storage: database size failed" } }
    runCatching {
        val covers = dirSize(context.getExternalFilesDir("covers") ?: File(context.filesDir, "covers"))
        update { it.copy(covers = covers) }
    }.onFailure { StorageLog.logcat(LogPriority.WARN, it) { "Storage: cover size failed" } }
    runCatching {
        val cache = dirSize(File(context.cacheDir, "chapter_disk_cache"))
        update { it.copy(cache = cache) }
    }.onFailure { StorageLog.logcat(LogPriority.WARN, it) { "Storage: cache size failed" } }
    runCatching {
        val (novels, manga) = Injekt.get<MangaRepository>().getFavoritesCountByType()
        val chapters = Injekt.get<ChapterRepository>().getFavoriteChaptersCount()
        update { it.copy(novels = novels, manga = manga, chapters = chapters) }
    }.onFailure { StorageLog.logcat(LogPriority.WARN, it) { "Storage: library counts failed" } }
    // Last, because it can be slow on a SAF folder.
    runCatching {
        val root: UniFile? = Injekt.get<StorageManager>().getDownloadsDirectory()
        val measured = if (root == null) {
            TreeMeasure(0L, 0, partial = false)
        } else {
            measureTree(
                root,
                children = { if (it.isDirectory) it.listFiles()?.toList().orEmpty() else null },
                length = { it.length() },
                now = System::currentTimeMillis,
                deadline = System.currentTimeMillis() + DOWNLOADS_BUDGET_MS,
                keepGoing = keepGoing,
            )
        }
        update { it.copy(downloads = measured.bytes, downloadsPartial = measured.partial) }
    }.onFailure {
        StorageLog.logcat(LogPriority.WARN, it) { "Storage: downloads size failed" }
        update { it.copy(downloads = 0L) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorageCard(b: StorageBreakdown) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(t.tileRadius)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val colours = listOf(t.accent, t.dotOn, muted, t.track)
    val labels = listOf(
        stringResource(MR.strings.tsz_storage_downloads),
        stringResource(MR.strings.tsz_storage_covers),
        stringResource(MR.strings.tsz_storage_database),
        stringResource(MR.strings.tsz_storage_cache),
    )
    val calculating = stringResource(MR.strings.tsz_storage_calculating)
    val plus = if (b.downloadsPartial) "+" else ""
    val total = if (b.isComplete) formatStorageSize(b.total) + plus else calculating
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = caseFor(stringResource(MR.strings.tsz_storage_title)),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = caseFor(total),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        SegmentBar(storageFractions(b.sizes), colours, t.track, t.isDot)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            b.sizes.forEachIndexed { i, size ->
                val value = size?.let { formatStorageSize(it) + if (i == 0) plus else "" } ?: calculating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(colours[i]))
                    Text(
                        text = caseFor("${labels[i]} $value"),
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentBar(fractions: List<Float>, colours: List<Color>, empty: Color, dot: Boolean) {
    val gap = 2.dp
    Canvas(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50))) {
        drawRect(empty)
        val g = gap.toPx()
        val usable = size.width - g * (fractions.count { it > 0f } - 1).coerceAtLeast(0)
        var x = 0f
        fractions.forEachIndexed { i, f ->
            if (f <= 0f) return@forEachIndexed
            val w = (usable * f).coerceAtLeast(if (dot) 2f else 4f)
            drawRect(colours[i], topLeft = Offset(x, 0f), size = Size(w, size.height))
            x += w + g
        }
    }
}

@Composable
private fun StatTile(value: Long?, label: String, modifier: Modifier) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(t.tileRadius)
    Column(
        modifier
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value?.let { formatCompactCount(it) } ?: "...",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = caseFor(label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun caseFor(text: String): String = if (TsuzukiTheme.tokens.isDot) text.uppercase() else text
