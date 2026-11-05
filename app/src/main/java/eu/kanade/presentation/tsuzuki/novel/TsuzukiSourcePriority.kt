package eu.kanade.presentation.tsuzuki.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile
import eu.kanade.tachiyomi.jsplugin.source.JsSource
import eu.kanade.tachiyomi.source.custom.CustomNovelSource
import eu.kanade.tachiyomi.ui.library.duplicate.DuplicateDetectionViewModel.SourceType
import eu.kanade.tachiyomi.ui.library.duplicate.SourcePriorityItem
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** The priority range the duplicate finder's stock sliders use. */
private val PriorityRange = -5..5

/**
 * Source priority in the Tsuzuki themes (board 08-tools "SourcePriority").
 *
 * The board shows drag-to-reorder, but the app stores a number per type and per source (−5…5, higher wins; a source
 * at 0 uses its type's number), and the duplicate finder compares those numbers. Reordering would mean inventing
 * numbers behind the user's back and breaking ties, so this keeps the model and shows it the board's way: the type
 * order as pills (highest first, top one in the accent), then a ranked list with the number on the left in the accent
 * and a − / + stepper where the board has its drag handle. Rows are ranked once when the screen opens, so a row
 * doesn't jump away from the finger while its number changes. Left out: per-source book counts (not in this state).
 */
@Composable
fun TsuzukiSourcePriorityScreen(
    typePriorities: Map<SourceType, Int>,
    sourcePriorities: Map<Long, Int>,
    sources: List<SourcePriorityItem>,
    hasLocalSource: Boolean,
    onBack: () -> Unit,
    onTypePriority: (SourceType, Int) -> Unit,
    onSourcePriority: (Long, Int) -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val sourceManager = remember { Injekt.get<SourceManager>() }

    val typeLabels = mapOf(
        SourceType.KT to stringResource(MR.strings.duplicate_source_type_kt_extensions),
        SourceType.JS to stringResource(MR.strings.duplicate_source_type_js_extensions),
        SourceType.CUSTOM to stringResource(MR.strings.duplicate_source_type_custom_extensions),
        SourceType.LOCAL to stringResource(MR.strings.duplicate_source_type_local_source),
    )
    val types = typeLabels.keys.filter { it != SourceType.LOCAL || hasLocalSource }
    // Live order for the pills (they aren't touch targets); the stepper rows keep their opening order.
    val typeOrder = types.sortedByDescending { typePriorities[it] ?: 0 }
    val typeRows = remember(types) { types.sortedByDescending { typePriorities[it] ?: 0 } }

    val rows = remember(sources) {
        sources.map { item ->
            val source = sourceManager.get(item.id)
            val type = when {
                source == null -> SourceType.STUB
                source.isLocal() -> SourceType.LOCAL
                source is CustomNovelSource -> SourceType.CUSTOM
                source is JsSource -> SourceType.JS
                else -> SourceType.KT
            }
            SourceRow(item.id, source?.name ?: item.displayName, type)
        }.sortedWith(
            compareByDescending<SourceRow> {
                sourcePriorities[it.id]?.takeIf { p -> p != 0 } ?: typePriorities[it.type] ?: 0
            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
        )
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .tsuzukiBackground(t),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "header") {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                            .clip(CircleShape)
                            .then(tileFill(t, CircleShape))
                            .clickable(role = Role.Button, onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(MR.strings.tsz_a11y_back),
                            tint = Color.White,
                        )
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            t.case(stringResource(MR.strings.duplicate_source_priority)),
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, lineHeight = 36.sp),
                            color = Color.White,
                        )
                        Text(
                            t.case(stringResource(MR.strings.tsz_source_priority_subtitle)),
                            style = body(13.sp, FontWeight.Medium),
                            color = muted,
                        )
                    }
                }
            }
        }

        item(key = "type_label") { Label(stringResource(MR.strings.tsz_source_priority_by_type)) }
        item(key = "type_order") {
            LazyRow(
                Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .then(tileFill(t, RoundedCornerShape(28.dp))),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(typeOrder, key = { _, type -> type.name }) { index, type ->
                    OrderPill(
                        text = (if (index > 0) "› " else "") + typeLabels.getValue(type),
                        first = index == 0,
                    )
                }
            }
        }
        item(key = "type_gap") { Spacer(Modifier.height(8.dp)) }
        itemsIndexed(typeRows, key = { _, type -> "type_${type.name}" }) { index, type ->
            PriorityRow(
                value = typePriorities[type] ?: 0,
                inherited = false,
                title = typeLabels.getValue(type),
                subtitle = null,
                first = index == 0,
                last = index == typeRows.lastIndex,
                onChange = { onTypePriority(type, it) },
                onReset = null,
            )
        }

        item(key = "source_label") { Label(stringResource(MR.strings.tsz_source_priority_specific)) }
        if (rows.isEmpty()) {
            item(key = "empty") {
                Text(
                    t.case(stringResource(MR.strings.duplicate_no_sources_found)),
                    style = body(14.sp, FontWeight.Medium),
                    color = muted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
            val own = sourcePriorities[row.id] ?: 0
            val typeValue = typePriorities[row.type] ?: 0
            val shown = if (own != 0) own else typeValue
            PriorityRow(
                value = shown,
                inherited = own == 0,
                title = row.name,
                subtitle = typeLabels[row.type],
                first = index == 0,
                last = index == rows.lastIndex,
                // A source's own 0 means "use the type's number", so stepping steps over 0 when the type isn't 0;
                // tapping the number goes back to the type's number.
                onChange = { next -> onSourcePriority(row.id, storedSourcePriority(next, shown, typeValue)) },
                onReset = if (own != 0) ({ onSourcePriority(row.id, 0) }) else null,
            )
        }
        item(key = "footnote") {
            Text(
                t.case(stringResource(MR.strings.tsz_source_priority_footnote)),
                style = body(13.sp, FontWeight.Medium),
                color = muted,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .widthIn(max = 640.dp),
            )
        }
    }
}

private data class SourceRow(val id: Long, val name: String, val type: SourceType)

@Composable
private fun Label(text: String) {
    val t = TsuzukiTheme.tokens
    Text(
        text.uppercase(),
        style = body(12.sp, FontWeight.ExtraBold).copy(letterSpacing = 1.4.sp),
        color = if (t.isDot) Color(0xFFB0B0B0) else Color.White.copy(alpha = 0.72f),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
    )
}

@Composable
private fun OrderPill(text: String, first: Boolean) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(if (t.isDot) 4.dp else 20.dp)
    Box(
        Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .then(
                when {
                    first -> Modifier.background(t.accent)
                    t.isDot -> Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), shape)
                    else -> Modifier.background(t.background)
                },
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            t.case(text),
            style = body(14.sp, FontWeight.ExtraBold),
            color = if (first) t.onAccent else Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun PriorityRow(
    value: Int,
    inherited: Boolean,
    title: String,
    subtitle: String?,
    first: Boolean,
    last: Boolean,
    onChange: (Int) -> Unit,
    onReset: (() -> Unit)?,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    Column(tsuzukiGroupedTile(first = first, last = last)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                signed(value),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (inherited) muted else t.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = onReset != null, role = Role.Button) { onReset?.invoke() }
                    .padding(vertical = 6.dp),
            )
            Column(Modifier.weight(1f).padding(start = 6.dp, end = 8.dp)) {
                Text(
                    t.case(title),
                    style = body(16.sp, FontWeight.ExtraBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    val line = if (inherited) {
                        "$subtitle · ${stringResource(MR.strings.tsz_source_priority_from_type)}"
                    } else {
                        subtitle
                    }
                    Text(
                        t.case(line),
                        style = body(13.sp, FontWeight.Medium),
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            StepButton(
                Icons.Outlined.Remove,
                stringResource(MR.strings.tsz_a11y_decrease),
                enabled =
                value > PriorityRange.first,
            ) { onChange(value - 1) }
            Spacer(Modifier.width(8.dp))
            StepButton(
                Icons.Outlined.Add,
                stringResource(MR.strings.tsz_a11y_increase),
                enabled =
                value < PriorityRange.last,
            ) { onChange(value + 1) }
        }
        if (!last) HorizontalDivider(color = Color.White.copy(alpha = if (t.isDot) 0.14f else 0.08f))
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .tsuzukiClickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White.copy(alpha = if (enabled) 1f else 0.38f))
    }
}

/**
 * The number to store when a source's stepper moves from [shown] to [next]. A source's own 0 means "use the type's
 * number", so when the type isn't 0 the stepper steps over 0 (1 → −1, −1 → 1) instead of silently snapping back to
 * the type. Clamped to the stock −5…5 range.
 */
internal fun storedSourcePriority(next: Int, shown: Int, typeValue: Int): Int {
    val stored = if (next == 0 && typeValue != 0) next + (next - shown) else next
    return stored.coerceIn(PriorityRange)
}

/** "+2", "0", "−1": the sign shows which way a number pushes the source. */
private fun signed(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}

private fun tileFill(t: TsuzukiTokens, shape: androidx.compose.ui.graphics.Shape): Modifier = Modifier
    .background(t.tile)
    .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)

private fun TsuzukiTokens.case(s: String) = if (isDot) s.uppercase() else s

@Composable
private fun body(size: TextUnit, weight: FontWeight) =
    MaterialTheme.typography.bodyMedium.copy(fontSize = size, fontWeight = weight)
