package eu.kanade.presentation.tsuzuki.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.LocalTsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.presentation.util.isTabletUi
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaYearMonth
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import mihon.feature.upcoming.UpcomingUIModel
import mihon.feature.upcoming.UpcomingViewModel
import tachiyomi.core.common.Constants
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.time.Clock

@Composable
internal fun TsuzukiUpcomingScreen(
    state: UpcomingViewModel.State,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickUpcoming: (Manga) -> Unit,
    onClickFilter: () -> Unit,
    hasActiveFilters: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    val navigator = LocalNavigator.currentOrThrow
    val uriHandler = LocalUriHandler.current
    val month = state.selectedYearMonth
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var pickedDay by rememberSaveable(month.toString()) { mutableIntStateOf(0) }
    val selectedDate = if (pickedDay in 1..month.numberOfDays) {
        LocalDate(month.year, month.month, pickedDay)
    } else {
        defaultUpcomingDate(month, today, state.events.keys)
    }
    val books = remember(state.items, state.headerIndexes, selectedDate) {
        upcomingBooksForDay(state.items, state.headerIndexes[selectedDate])
    }
    val calendar: @Composable () -> Unit = {
        UpcomingMonthTile(
            month = month,
            today = today,
            selected = selectedDate,
            datesWithUpdates = state.events.keys,
            onSelectMonth = setSelectedYearMonth,
            onSelectDate = { pickedDay = it.day },
        )
    }

    Scaffold(
        modifier = modifier.tsuzukiBackground(tokens),
        containerColor = Color.Transparent,
        topBar = {
            AppBar(
                title = stringResource(MR.strings.label_upcoming),
                subtitle = tsuzukiCase(stringResource(MR.strings.tsz_upcoming_subtitle)),
                backgroundColor = Color.Transparent,
                navigateUp = navigator::pop,
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_filter),
                                icon = Icons.Outlined.FilterList,
                                iconTint = if (hasActiveFilters) tokens.accent else tokens.dotOn,
                                onClick = onClickFilter,
                            ),
                            AppBar.Action(
                                title = stringResource(MR.strings.upcoming_guide),
                                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                iconTint = tokens.dotOn,
                                onClick = { uriHandler.openUri(Constants.URL_HELP_UPCOMING) },
                            ),
                        ),
                    )
                },
            )
        },
    ) { paddingValues ->
        if (isTabletUi()) {
            TwoPanelBox(
                modifier = Modifier.padding(paddingValues),
                startContent = {
                    Box(Modifier.verticalScroll(rememberScrollState())) { calendar() }
                },
                endContent = {
                    FastScrollLazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                        upcomingDayRows(selectedDate, books, onClickUpcoming)
                    }
                },
            )
        } else {
            FastScrollLazyColumn(contentPadding = paddingValues) {
                item(key = "upcoming-calendar", content = { calendar() })
                upcomingDayRows(selectedDate, books, onClickUpcoming)
                item(key = "upcoming-spacer") { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun UpcomingMonthTile(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    datesWithUpdates: Set<LocalDate>,
    onSelectMonth: (YearMonth) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val locale = Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val weekdays = remember(firstDay) {
        List(DAYS_PER_WEEK) { firstDay.plus(it.toLong()) }
    }
    val weeks = remember(month, firstDay) { upcomingMonthWeeks(month, firstDay) }
    val monthTitle = remember(month, locale) {
        DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month.toJavaYearMonth())
    }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale) }

    CompositionLocalProvider(LocalTsuzukiTokens provides tokens.copy(tileRadius = 24.dp)) {
        TsuzukiTile(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
            padding = 12.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tsuzukiCase(monthTitle).orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = tokens.dotOn,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onSelectMonth(month.minusMonth()) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(MR.strings.upcoming_calendar_prev),
                        tint = tokens.dotOn.copy(alpha = 0.72f),
                    )
                }
                IconButton(onClick = { onSelectMonth(month.plusMonth()) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(MR.strings.upcoming_calendar_next),
                        tint = tokens.dotOn.copy(alpha = 0.72f),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weekdays.forEach { weekday ->
                    Box(Modifier.weight(1f).heightIn(min = 26.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = tsuzukiCase(weekday.getDisplayName(TextStyle.NARROW, locale)).orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.dotOn.copy(alpha = 0.72f),
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                weeks.forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { date ->
                            if (date == null) {
                                Spacer(Modifier.weight(1f).height(40.dp))
                            } else {
                                UpcomingDayCell(
                                    date = date,
                                    dateLabel = dateFormatter.format(date.toJavaLocalDate()),
                                    isToday = date == today,
                                    isSelected = date == selected,
                                    hasUpdates = date in datesWithUpdates,
                                    onClick = { onSelectDate(date) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingDayCell(
    date: LocalDate,
    dateLabel: String,
    isToday: Boolean,
    isSelected: Boolean,
    hasUpdates: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(if (tokens.isDot) 4.dp else 12.dp)
    val ink = if (isSelected) tokens.onAccent else tokens.dotOn
    Column(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .then(if (isSelected) Modifier.background(tokens.accent) else Modifier)
            .then(if (isToday && !isSelected) Modifier.border(1.dp, tokens.track, shape) else Modifier)
            .tsuzukiClickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = dateLabel
                selected = isSelected
            }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(date.day.toString(), style = MaterialTheme.typography.labelLarge, color = ink)
        if (hasUpdates) {
            Box(
                Modifier.size(if (isSelected) 10.dp else 4.dp, 4.dp)
                    .background(if (isSelected) tokens.onAccent else tokens.accent, CircleShape),
            )
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun LazyListScope.upcomingDayRows(
    date: LocalDate,
    books: List<Manga>,
    onClickUpcoming: (Manga) -> Unit,
) {
    item(key = "upcoming-date") {
        val locale = Locale.getDefault()
        val heading = remember(date, locale) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date.toJavaLocalDate())
        }
        TsuzukiSectionLabel(heading)
    }
    if (books.isEmpty()) {
        item(key = "upcoming-empty") {
            Text(
                text = tsuzukiCase(stringResource(MR.strings.tsz_upcoming_no_updates)).orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = TsuzukiTheme.tokens.dotOn.copy(alpha = 0.72f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
    itemsIndexed(books, key = { _, book -> book.id }, contentType = { _, _ -> "upcoming_book" }) { index, book ->
        val tokens = TsuzukiTheme.tokens
        Column(
            tsuzukiGroupedTile(first = index == 0, last = index == books.lastIndex)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .tsuzukiClickable(role = Role.Button, onClick = { onClickUpcoming(book) })
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MangaCover.Book(data = book.asMangaCover(), modifier = Modifier.width(36.dp))
                Text(
                    text = tsuzukiCase(book.title).orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tokens.dotOn,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (index != books.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(tokens.tileBorder ?: tokens.track))
            }
        }
    }
}

private const val DAYS_PER_WEEK = 7

internal fun upcomingMonthWeeks(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate?>> {
    val firstDate = LocalDate(month.year, month.month, 1)
    val firstDateWeekday = firstDate.toJavaLocalDate().dayOfWeek.value
    val leadingDays = (firstDateWeekday - firstDayOfWeek.value + DAYS_PER_WEEK) % DAYS_PER_WEEK
    val occupiedDays = leadingDays + month.numberOfDays
    val cellCount = ((occupiedDays + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK) * DAYS_PER_WEEK
    return List(cellCount) { index ->
        val day = index - leadingDays + 1
        if (day in 1..month.numberOfDays) LocalDate(month.year, month.month, day) else null
    }.chunked(DAYS_PER_WEEK)
}

internal fun defaultUpcomingDate(month: YearMonth, today: LocalDate, forecastDates: Set<LocalDate>): LocalDate =
    forecastDates.filter { it.yearMonth == month }.minOrNull()
        ?: today.takeIf { it.yearMonth == month }
        ?: LocalDate(month.year, month.month, 1)

internal fun upcomingBooksForDay(items: List<UpcomingUIModel>, headerIndex: Int?): List<Manga> =
    if (headerIndex == null) {
        emptyList()
    } else {
        items.asSequence().drop(headerIndex + 1)
            .takeWhile { it is UpcomingUIModel.Item }
            .filterIsInstance<UpcomingUIModel.Item>()
            .map { it.manga }
            .toList()
    }
