package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.jsplugin.source.JsSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.rateLimitBaseUrl
import eu.kanade.tachiyomi.tsuzuki.sourcehealth.SOURCE_HEALTH_DAYS
import eu.kanade.tachiyomi.tsuzuki.sourcehealth.SourceHealth
import eu.kanade.tachiyomi.tsuzuki.sourcehealth.SourceHealthError
import eu.kanade.tachiyomi.tsuzuki.sourcehealth.SourceHealthFailure
import eu.kanade.tachiyomi.tsuzuki.sourcehealth.SourceHealthStatus
import eu.kanade.tachiyomi.tsuzuki.sourcehealth.SourceHealthStore
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrateMangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun sourceHealthAction(onClick: () -> Unit): AppBar.OverflowAction? =
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) {
        null
    } else {
        AppBar.OverflowAction(
            title = stringResource(MR.strings.tsz_source_health),
            icon = Icons.Outlined.MonitorHeart,
            onClick = onClick,
        )
    }

@Composable
internal fun SourceHealthDot(sourceId: Long) {
    if (LocalInspectionMode.current) return
    val store = remember { Injekt.get<SourceHealthStore>() }
    val health by remember(sourceId) { store.observe(sourceId) }.collectAsState(initial = SourceHealth())
    val status = health.status(LocalDate.now().toEpochDay())
    if (status == SourceHealthStatus.UNKNOWN) return
    val description = status.label()
    Box(
        Modifier.padding(end = 6.dp).size(8.dp).background(status.color(), CircleShape)
            .semantics { contentDescription = description },
    )
}

internal class TsuzukiSourceHealthScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val manager = remember { Injekt.get<SourceManager>() }
        val store = remember { Injekt.get<SourceHealthStore>() }
        val repository = remember { Injekt.get<SourceRepository>() }
        val sources by manager.sources.collectAsState(initial = emptyList())
        val counts by remember(repository) {
            repository.getSourcesWithFavoriteCount().map { rows -> rows.associate { it.first.id to it.second } }
        }.collectAsState(initial = emptyMap())
        val online = remember(sources) {
            sources.filter { it is HttpSource || it is JsSource }.sortedBy { it.name.lowercase() }
        }
        val ids = remember(online) { online.map { it.id } }
        val observed = remember(store, ids) {
            if (ids.isEmpty()) {
                flowOf(emptyMap<Long, SourceHealth>())
            } else {
                combine(ids.map { id -> store.observe(id).map { id to it } }) { it.toMap() }
            }
        }
        val records by observed.collectAsState(initial = emptyMap())
        val today = LocalDate.now().toEpochDay()
        val countsByStatus = online.groupingBy {
            records[it.id]?.status(today) ?: SourceHealthStatus.UNKNOWN
        }.eachCount()
        val down = online.firstOrNull { records[it.id]?.status(today) == SourceHealthStatus.DOWN }
        val tokens = TsuzukiTheme.tokens

        Scaffold(
            modifier = Modifier.tsuzukiBackground(tokens),
            containerColor = Color.Transparent,
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.tsz_source_health),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = padding,
            ) {
                item(key = "summary") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (status in listOf(
                            SourceHealthStatus.WORKING,
                            SourceHealthStatus.SLOW,
                            SourceHealthStatus.DOWN,
                        )) {
                            TsuzukiTile(Modifier.weight(1f).heightIn(min = 86.dp), padding = 12.dp) {
                                Text(
                                    tsuzukiMenuLabel(status.label()),
                                    color = tokens.mutedText,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(
                                    (countsByStatus[status] ?: 0).toString(),
                                    color = status.color(),
                                    style = MaterialTheme.typography.displaySmall,
                                )
                            }
                        }
                    }
                }
                if (down != null) {
                    item(key = "outage") {
                        val failure = records[down.id]?.lastError
                        val bookCount = counts[down.id]?.takeIf { it > 0 }
                        TsuzukiTile(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text(
                                tsuzukiMenuLabel(stringResource(MR.strings.tsz_source_health_outage, down.name)),
                                color = tokens.dotOn,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            failure?.let {
                                Text(
                                    tsuzukiMenuLabel(failureLabel(it)),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = tokens.mutedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (bookCount != null) {
                                    TsuzukiPill(
                                        text = stringResource(MR.strings.tsz_source_health_migrate, bookCount),
                                        selected = true,
                                        onClick = { navigator.push(MigrateMangaScreen(down.id)) },
                                        role = Role.Button,
                                    )
                                }
                                down.rateLimitBaseUrl()?.toHttpUrlOrNull()?.let { url ->
                                    TsuzukiPill(
                                        text = stringResource(MR.strings.action_open_in_web_view),
                                        selected = false,
                                        onClick = { navigator.push(WebViewScreen(url.toString(), down.name, down.id)) },
                                        role = Role.Button,
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "history") { TsuzukiSectionLabel(stringResource(MR.strings.tsz_source_health_history)) }
                itemsIndexed(online, key = { _, source -> source.id }) { index, source ->
                    SourceHealthRow(
                        title = source.name,
                        health = records[source.id] ?: SourceHealth(),
                        today = today,
                        modifier = tsuzukiGroupedTile(first = index == 0, last = index == online.lastIndex),
                    )
                }
                item(key = "footer") {
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.tsz_source_health_passive)),
                        color = tokens.mutedText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceHealthRow(title: String, health: SourceHealth, today: Long, modifier: Modifier) {
    val tokens = TsuzukiTheme.tokens
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale) }
    Row(
        modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(tsuzukiMenuLabel(title), color = tokens.dotOn, style = MaterialTheme.typography.titleSmall)
            val average = health.averageMillis(today)
            if (average != null) {
                HealthDetail(stringResource(MR.strings.tsz_source_health_average, average))
            }
            if (health.lastSuccessAt > 0L) {
                HealthDetail(
                    stringResource(MR.strings.tsz_source_health_success, relativeTimeSpanString(health.lastSuccessAt)),
                )
            }
            health.lastError?.let { HealthDetail(failureLabel(it)) }
            if (health.days.isEmpty()) HealthDetail(stringResource(MR.strings.tsz_source_health_unknown))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            health.recentDays(today).forEachIndexed { index, day ->
                val date = LocalDate.ofEpochDay(today - SOURCE_HEALTH_DAYS + 1 + index).format(formatter)
                val status = day?.status ?: SourceHealthStatus.UNKNOWN
                val description = "$date · ${status.label()}"
                Box(
                    Modifier.size(12.dp)
                        .background(status.color(), if (tokens.isDot) CircleShape else RoundedCornerShape(3.dp))
                        .semantics { contentDescription = description },
                )
            }
        }
    }
}

@Composable
private fun HealthDetail(text: String) {
    Text(tsuzukiMenuLabel(text), color = TsuzukiTheme.tokens.mutedText, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun failureLabel(failure: SourceHealthFailure): String {
    val reason = when (failure.kind) {
        SourceHealthError.HTTP -> failure.httpCode?.let { stringResource(MR.strings.tsz_source_health_http, it) }
            ?: stringResource(MR.strings.tsz_source_health_network)
        SourceHealthError.CLOUDFLARE -> stringResource(MR.strings.tsz_source_health_cloudflare)
        SourceHealthError.TIMEOUT -> stringResource(MR.strings.tsz_source_health_timeout)
        SourceHealthError.NETWORK -> stringResource(MR.strings.tsz_source_health_network)
    }
    return stringResource(MR.strings.tsz_source_health_failure, reason, relativeTimeSpanString(failure.at))
}

@Composable
private fun SourceHealthStatus.label(): String = stringResource(
    when (this) {
        SourceHealthStatus.WORKING -> MR.strings.tsz_source_health_working
        SourceHealthStatus.SLOW -> MR.strings.tsz_source_health_slow
        SourceHealthStatus.DOWN -> MR.strings.tsz_source_health_down
        SourceHealthStatus.UNKNOWN -> MR.strings.tsz_source_health_unknown
    },
)

@Composable
private fun SourceHealthStatus.color(): Color {
    val tokens = TsuzukiTheme.tokens
    return when (this) {
        SourceHealthStatus.WORKING -> tokens.sourceWorking
        SourceHealthStatus.SLOW -> tokens.sourceSlow
        SourceHealthStatus.DOWN -> tokens.sourceDown
        SourceHealthStatus.UNKNOWN -> tokens.track
    }
}
