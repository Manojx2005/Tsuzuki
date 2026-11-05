package eu.kanade.presentation.tsuzuki.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import eu.kanade.presentation.theme.tsuzuki.tsuzukiSharedCover
import eu.kanade.tachiyomi.ui.tsuzuki.home.ChapterStatus
import eu.kanade.tachiyomi.ui.tsuzuki.home.HomeBook
import eu.kanade.tachiyomi.ui.tsuzuki.home.HomeState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Everything Home can do; the tab wires these to navigation. */
data class HomeActions(
    val onResume: (HomeBook) -> Unit,
    val onOpenBook: (HomeBook) -> Unit,
    val onAllBooks: () -> Unit,
    val onUpdates: () -> Unit,
    val onHistory: () -> Unit,
    val onSearch: () -> Unit,
    val onBrowse: () -> Unit,
    val onImportBackup: () -> Unit,
    val onOpenEpub: () -> Unit,
    val onOpenChapter: (HomeBook, Long) -> Unit,
)

/**
 * Bento Home (design boards: Main, TabletPortraitLibrary, TabletLibrary, LibraryEmpty).
 *
 * One layout, two skins: everything reads [TsuzukiTheme.tokens].
 * Tiles without real data yet (streak, listen, source health) are left out on purpose.
 */
@Composable
fun TsuzukiHomeScreen(
    state: HomeState,
    actions: HomeActions,
    /** True when a bottom nav bar floats over the content (phones). */
    bottomBarOverlay: Boolean,
) {
    val t = TsuzukiTheme.tokens
    // Read / Watch switch (boards Main, TabletLibrary). Watch is a placeholder until anime lands.
    var watching by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .tsuzukiBackground(t),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().statusBarsPadding()) {
            val side = if (maxWidth < 600.dp) 16.dp else 28.dp
            MediaSwitch(
                watching = watching,
                onChange = { watching = it },
                modifier = Modifier.padding(start = side, end = side, top = 10.dp),
            )
        }
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(WindowInsets.statusBars),
        ) {
            val bottomPad = if (bottomBarOverlay) 110.dp else 24.dp
            when {
                watching -> WatchPlaceholder(onBack = { watching = false })
                state.loading -> Unit
                state.isEmpty -> EmptyHome(actions, compact = maxWidth < 600.dp, bottomPad = bottomPad)
                maxWidth < 600.dp -> PhoneHome(state, actions, bottomPad)
                maxWidth < 1000.dp -> TabletPortraitHome(state, actions)
                else -> TabletLandscapeHome(state, actions)
            }
        }
    }
}

@Composable
private fun MediaSwitch(watching: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val t = TsuzukiTheme.tokens
    Row(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (t.isDot) {
                    Modifier.border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(22.dp))
                } else {
                    Modifier.background(Color.Black.copy(alpha = 0.25f))
                },
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(
            false to stringResource(MR.strings.tsz_read),
            true to stringResource(MR.strings.tsz_watch),
        ).forEach { (isWatch, label) ->
            val selected = watching == isWatch
            Text(
                text = t.case(label),
                style = bodyStyle(13.sp, FontWeight.ExtraBold),
                color = when {
                    selected && t.isDot -> Color.Black
                    selected -> t.background.orIfUnspecified(Color.Black)
                    else -> muted(t)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(19.dp))
                    .then(if (selected) Modifier.background(Color.White) else Modifier)
                    .selectable(selected = selected, role = Role.Tab) { onChange(isWatch) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

/** Anime is designed (canvas page "Final · Anime") but not built yet; this keeps its place. */
@Composable
private fun WatchPlaceholder(onBack: () -> Unit) {
    val t = TsuzukiTheme.tokens
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(t.case(stringResource(MR.strings.tsz_coming_soon)), style = labelStyle(t), color = t.accent)
        Text(t.case(stringResource(MR.strings.tsz_watch)), style = displayStyle(56.sp), color = Color.White)
        Text(
            t.case(
                stringResource(MR.strings.tsz_watch_body),
            ),
            style = bodyStyle(14.sp, FontWeight.Medium).copy(lineHeight = 21.sp),
            color = muted(t),
        )
        Spacer(Modifier.height(4.dp))
        listOf(
            Icons.Filled.PlayArrow to
                (stringResource(MR.strings.tsz_watch_f1) to stringResource(MR.strings.tsz_watch_f1_sub)),
            Icons.Outlined.Explore to
                (stringResource(MR.strings.tsz_watch_f2) to stringResource(MR.strings.tsz_watch_f2_sub)),
            Icons.Outlined.FileUpload to
                (stringResource(MR.strings.tsz_watch_f3) to stringResource(MR.strings.tsz_watch_f3_sub)),
        ).forEach { (icon, text) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .then(tileFill(t, hero = false))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(icon, contentDescription = null, tint = t.accent, modifier = Modifier.size(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(t.case(text.first), style = bodyStyle(15.sp, FontWeight.ExtraBold), color = Color.White)
                    Text(t.case(text.second), style = bodyStyle(12.sp, FontWeight.Medium), color = muted(t))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            t.case(stringResource(MR.strings.tsz_back_to_reading)),
            style = bodyStyle(15.sp, FontWeight.ExtraBold),
            color = t.onAccent,
            modifier = Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(t.accent)
                .tsuzukiClickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        )
    }
}

// ---------------------------------------------------------------- layouts

@Composable
private fun PhoneHome(state: HomeState, actions: HomeActions, bottomPad: Dp) {
    val t = TsuzukiTheme.tokens
    if (state.books.isEmpty()) return
    var stackIndex by rememberSaveable { mutableIntStateOf(0) }
    val stack = state.books.take(4)
    val current = stack[stackIndex.coerceIn(0, stack.lastIndex)]
    val paletteCover = LocalShelfPaletteCover.current
    LaunchedEffect(current.cover) { paletteCover(current.cover) }
    DisposableEffect(paletteCover) { onDispose { paletteCover(null) } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = bottomPad),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(t.case(todayKicker(state.libraryCount)), style = labelStyle(t), color = muted(t))
            IconCircle(onClick = actions.onSearch) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = stringResource(MR.strings.tsz_search),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Cover stack: tap to switch which book the Resume tile acts on.
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp).height(236.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TsuzukiCoverStack(stack, stackIndex.coerceIn(0, stack.lastIndex)) {
                stackIndex = (stackIndex.coerceIn(0, stack.lastIndex) + 1) % stack.size
            }
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                if (stack.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        stack.indices.forEach { k -> Pip(selected = k == stackIndex.coerceIn(0, stack.lastIndex)) }
                    }
                }
                Text(
                    t.case(current.title),
                    style = displayStyle(30.sp),
                    color = Color.White,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 14.dp),
                )
                current.chapterLabel?.let {
                    Text(
                        t.case(it),
                        style = bodyStyle(13.sp, FontWeight.SemiBold),
                        color = muted(t, 0.8f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (current.unread > 0) UnreadChip(current.unread, Modifier.padding(top = 10.dp))
                Spacer(Modifier.weight(1f))
                if (stack.size > 1) {
                    Text(
                        t.case(stringResource(MR.strings.tsz_tap_stack)),
                        style = bodyStyle(12.sp, FontWeight.SemiBold),
                        color = if (t.isDot) Color(0xFF5A5A5A) else Color.White.copy(alpha = 0.55f),
                    )
                }
            }
        }

        Column(
            Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResumeTile(current, state.fromHistory, onClick = { actions.onResume(current) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    stringResource(MR.strings.tsz_new_chapters),
                    state.newChapters.toString(),
                    null,
                    Modifier.weight(1f).height(100.dp),
                    accentValue =
                    t.isDot && state.newChapters > 0,
                    onClick = actions.onUpdates,
                )
                StatTile(
                    stringResource(MR.strings.tsz_today),
                    state.todayMinutes.toString(),
                    stringResource(MR.strings.tsz_min),
                    Modifier.weight(1f).height(100.dp),
                    onClick = actions.onHistory,
                )
            }
        }
        ShelfSection(
            state,
            actions,
            columns = 3,
            coverHeight = null,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun TabletPortraitHome(state: HomeState, actions: HomeActions) {
    val t = TsuzukiTheme.tokens
    val hero = state.hero ?: return
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header(state)
        HeroTile(
            hero,
            state.fromHistory,
            Modifier.fillMaxWidth().height(
                330.dp,
            ),
            coverWidth = 190.dp,
            titleSize = 44.sp,
            onClick = {
                actions.onResume(hero)
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                stringResource(MR.strings.tsz_new),
                state.newChapters.toString(),
                null,
                Modifier.weight(1f).height(130.dp),
                accentValue =
                state.newChapters > 0,
                bigSize = 50.sp,
                onClick = actions.onUpdates,
            )
            StatTile(
                stringResource(MR.strings.tsz_today),
                state.todayMinutes.toString(),
                stringResource(MR.strings.tsz_min),
                Modifier.weight(1f).height(130.dp),
                bigSize = 40.sp,
                onClick = actions.onHistory,
            )
            StatTile(
                stringResource(MR.strings.tsz_library),
                state.libraryCount.toString(),
                stringResource(MR.strings.tsz_books_unit),
                Modifier.weight(1f).height(130.dp),
                bigSize = 40.sp,
                onClick = actions.onAllBooks,
            )
        }
        ShelfSection(state, actions, columns = 4, coverHeight = null, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun TabletLandscapeHome(state: HomeState, actions: HomeActions) {
    val hero = state.hero ?: return
    // The details pane shows the selected book; tapping a shelf cover selects it.
    var selectedId by rememberSaveable { mutableLongStateOf(hero.mangaId) }
    val selected = state.books.firstOrNull { it.mangaId == selectedId } ?: hero
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header(state, titleSize = 52.sp)
            Row(Modifier.height(280.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // The details pane takes 360 dp here, so the hero gets more of the row than on the board.
                HeroTile(
                    hero,
                    state.fromHistory,
                    Modifier.weight(
                        2.2f,
                    ).fillMaxSize(),
                    coverWidth = 120.dp,
                    titleSize = 30.sp,
                    onClick = {
                        actions.onResume(hero)
                    },
                )
                Column(Modifier.weight(1.6f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.weight(150f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            stringResource(MR.strings.tsz_new),
                            state.newChapters.toString(),
                            null,
                            Modifier.weight(1f).fillMaxSize(),
                            accentValue =
                            state.newChapters > 0,
                            bigSize = 44.sp,
                            onClick = actions.onUpdates,
                        )
                        StatTile(
                            stringResource(MR.strings.tsz_library),
                            state.libraryCount.toString(),
                            stringResource(MR.strings.tsz_books_unit),
                            Modifier.weight(1f).fillMaxSize(),
                            bigSize = 44.sp,
                            onClick = actions.onAllBooks,
                        )
                    }
                    Row(Modifier.weight(120f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            stringResource(MR.strings.tsz_today),
                            state.todayMinutes.toString(),
                            stringResource(MR.strings.tsz_min),
                            Modifier.weight(1f).fillMaxSize(),
                            bigSize = 34.sp,
                            onClick = actions.onHistory,
                        )
                        StatTile(
                            stringResource(MR.strings.tsz_history),
                            state.books.size.toString(),
                            stringResource(MR.strings.tsz_recent),
                            Modifier.weight(1f).fillMaxSize(),
                            bigSize = 34.sp,
                            onClick = actions.onHistory,
                        )
                    }
                }
            }
            ShelfSection(
                state,
                actions,
                columns = 5,
                coverHeight = null,
                selectedId = selectedId,
                onSelect = { selectedId = it.mangaId },
            )
        }
        DetailsPane(selected, actions)
    }
}

/** Right-hand pane on wide screens (board TabletLibrary): the selected book and its latest chapters. */
@Composable
private fun DetailsPane(book: HomeBook, actions: HomeActions) {
    val t = TsuzukiTheme.tokens
    val canResume = book.chapterId != null
    Column(
        Modifier
            .width(360.dp)
            .fillMaxHeight()
            .then(
                if (t.isDot) {
                    Modifier
                        .background(Color(0xFF070707))
                        .drawBehind { drawLine(Color(0xFF262626), Offset.Zero, Offset(0f, size.height), 1.dp.toPx()) }
                } else {
                    Modifier.background(Color.Black.copy(alpha = 0.18f))
                },
            )
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            Cover(book, Modifier.width(112.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    t.case(book.title),
                    style = displayStyle(28.sp),
                    color = Color.White,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                book.meta?.let { Text(t.case(it), style = bodyStyle(12.sp, FontWeight.SemiBold), color = muted(t)) }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(t.accent)
                .tsuzukiClickable(role = Role.Button) {
                    if (canResume) actions.onResume(book) else actions.onOpenBook(book)
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = t.onAccent, modifier = Modifier.size(18.dp))
            Text(
                t.case(if (canResume) stringResource(MR.strings.tsz_resume) else stringResource(MR.strings.tsz_open)),
                style = bodyStyle(15.sp, FontWeight.ExtraBold),
                color = t.onAccent,
            )
        }
        Text(
            t.case(stringResource(MR.strings.tsz_chapters)),
            style = labelStyle(t),
            color = muted(t),
            modifier = Modifier.padding(top = 6.dp),
        )
        if (book.recentChapters.isEmpty()) {
            Text(
                t.case(stringResource(MR.strings.tsz_load_chapters)),
                style = bodyStyle(13.sp, FontWeight.Medium),
                color = muted(t),
            )
        }
        book.recentChapters.forEach { ch ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .then(tileFill(t, hero = false))
                    .tsuzukiClickable(role = Role.Button) { actions.onOpenChapter(book, ch.id) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(ch.number, style = numberStyle(t, 16.sp), color = Color.White, modifier = Modifier.width(40.dp))
                Text(
                    t.case(ch.name),
                    style = bodyStyle(13.sp, FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val (label, color) = when (ch.status) {
                    ChapterStatus.NEW -> stringResource(MR.strings.tsz_new) to t.accent
                    ChapterStatus.IN_PROGRESS -> stringResource(MR.strings.tsz_reading) to Color.White
                    ChapterStatus.READ -> stringResource(MR.strings.tsz_read_state) to muted(t)
                    ChapterStatus.UNREAD -> "" to muted(t)
                }
                if (label.isNotEmpty()) Text(t.case(label), style = bodyStyle(11.sp, FontWeight.Bold), color = color)
            }
        }
    }
}

@Composable
private fun EmptyHome(actions: HomeActions, compact: Boolean, bottomPad: Dp) {
    val t = TsuzukiTheme.tokens
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(
                start = if (compact) 12.dp else 28.dp,
                end = if (compact) 12.dp else 28.dp,
                top = 24.dp,
                bottom = bottomPad,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t.case(stringResource(MR.strings.tsz_welcome)), style = labelStyle(t), color = muted(t))
            Text(
                t.case(stringResource(MR.strings.tsz_empty_title)),
                style = displayStyle(if (compact) 52.sp else 64.sp),
                color = Color.White,
            )
            Text(
                t.case(stringResource(MR.strings.tsz_empty_body)),
                style = bodyStyle(14.sp, FontWeight.Medium).copy(lineHeight = 21.sp),
                color = muted(t),
            )
        }
        Spacer(Modifier.height(12.dp))
        // Primary action: accent tile.
        val browsePress = androidx.compose.runtime.remember {
            androidx.compose.foundation.interaction.MutableInteractionSource()
        }
        Row(
            Modifier
                .tsuzukiPress(browsePress)
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(t.accent)
                .clickable(
                    interactionSource = browsePress,
                    indication = androidx.compose.material3.ripple(),
                    role = Role.Button,
                    onClick = actions.onBrowse,
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Explore,
                    contentDescription = null,
                    tint = t.onAccent,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    t.case(stringResource(MR.strings.tsz_browse_sources)),
                    style = bodyStyle(18.sp, FontWeight.ExtraBold),
                    color = t.onAccent,
                )
                Text(
                    t.case(stringResource(MR.strings.tsz_browse_sources_sub)),
                    style = bodyStyle(13.sp, FontWeight.SemiBold),
                    color = t.onAccent.copy(alpha = 0.8f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(
                Icons.Outlined.FileUpload,
                stringResource(MR.strings.tsz_import_backup),
                stringResource(MR.strings.tsz_import_backup_sub),
                Modifier.weight(1f),
                actions.onImportBackup,
            )
            ActionTile(
                Icons.Outlined.FolderOpen,
                stringResource(MR.strings.tsz_open_epub),
                stringResource(MR.strings.tsz_open_epub_sub),
                Modifier.weight(1f),
                actions.onOpenEpub,
            )
        }
    }
}

// ---------------------------------------------------------------- pieces

@Composable
private fun Header(state: HomeState, titleSize: TextUnit = 60.sp) {
    val t = TsuzukiTheme.tokens
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(t.case(greeting()), style = displayStyle(titleSize), color = Color.White)
        Text(
            t.case(stringResource(MR.strings.tsz_books_count, state.libraryCount)),
            style = bodyStyle(13.sp, FontWeight.SemiBold),
            color = muted(t),
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

/** The big stringResource(MR.strings.tsz_continue) tile. Colour: accent fill with book-coloured text. Dot: dark tile, red play. */
@Composable
private fun HeroTile(
    book: HomeBook,
    canResume: Boolean,
    modifier: Modifier,
    coverWidth: Dp,
    titleSize: TextUnit,
    onClick: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val ink = if (t.isDot) Color.White else t.onAccent
    val heroPress = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    Row(
        Modifier.tsuzukiPress(heroPress).then(modifier)
            .clip(RoundedCornerShape(24.dp))
            .then(tileFill(t, hero = true))
            .clickable(
                interactionSource = heroPress,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClickLabel = stringResource(MR.strings.tsz_resume),
                onClick = onClick,
            )
            .padding(22.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Cover(book, Modifier.width(coverWidth).rotate(-3f))
        Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                t.case(
                    if (canResume) {
                        stringResource(
                            MR.strings.tsz_continue,
                        )
                    } else {
                        stringResource(MR.strings.tsz_start_reading)
                    },
                ),
                style = labelStyle(t),
                color = ink.copy(alpha = 0.75f),
            )
            Text(
                t.case(book.title),
                style = displayStyle(titleSize),
                color = ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            book.chapterLabel?.let {
                Text(t.case(it), style = bodyStyle(15.sp, FontWeight.SemiBold), color = ink.copy(alpha = 0.8f))
            }
            if (book.unread > 0) UnreadChip(book.unread, onHero = true)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    book.progress?.let {
                        TsuzukiProgressBar(
                            progress = it,
                            dots = 20,
                            fill = if (t.isDot) Color.Unspecified else t.onAccent,
                            track = if (t.isDot) Color.Unspecified else t.onAccent.copy(alpha = 0.18f),
                        )
                    }
                }
                PlayDisc(size = 60.dp)
            }
        }
    }
}

@Composable
private fun ResumeTile(book: HomeBook, canResume: Boolean, onClick: () -> Unit) {
    val t = TsuzukiTheme.tokens
    val ink = if (t.isDot) Color.White else t.onAccent
    val press = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        Modifier
            .tsuzukiPress(press)
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(tileFill(t, hero = true))
            .clickable(
                interactionSource = press,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 14.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PlayDisc(size = 52.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val pct = book.progress?.let { " · ${(it * 100).toInt()}%" }.orEmpty()
            Text(
                t.case(
                    (if (canResume) stringResource(MR.strings.tsz_resume) else stringResource(MR.strings.tsz_start)) +
                        pct,
                ),
                style = if (t.isDot) {
                    bodyStyle(
                        14.sp,
                        FontWeight.Bold,
                    ).copy(letterSpacing = 1.sp)
                } else {
                    bodyStyle(17.sp, FontWeight.ExtraBold)
                },
                color = ink,
            )
            book.progress?.let {
                TsuzukiProgressBar(
                    progress = it,
                    dots = 18,
                    fill = if (t.isDot) Color.Unspecified else t.onAccent,
                    track = if (t.isDot) Color.Unspecified else t.onAccent.copy(alpha = 0.18f),
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    unit: String?,
    modifier: Modifier,
    accentValue: Boolean = false,
    bigSize: TextUnit = 40.sp,
    onClick: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val press = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        Modifier.tsuzukiPress(press).then(modifier)
            .clip(RoundedCornerShape(24.dp))
            .then(tileFill(t, hero = false))
            .clickable(
                interactionSource = press,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(16.dp),
    ) {
        Text(t.case(label), style = labelStyle(t), color = muted(t))
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = numberStyle(t, bigSize), color = if (accentValue) t.accent else Color.White)
            unit?.let {
                Text(
                    t.case(it),
                    style = bodyStyle(13.sp, FontWeight.Bold),
                    color = muted(t),
                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val press = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        Modifier.tsuzukiPress(press).then(modifier)
            .height(132.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(tileFill(t, hero = false))
            .clickable(
                interactionSource = press,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.weight(1f))
        Text(t.case(title), style = bodyStyle(15.sp, FontWeight.ExtraBold), color = Color.White)
        Text(t.case(sub), style = bodyStyle(12.sp, FontWeight.Medium), color = muted(t))
    }
}

@Composable
private fun ShelfSection(
    state: HomeState,
    actions: HomeActions,
    columns: Int,
    @Suppress("UNUSED_PARAMETER") coverHeight: Dp?,
    modifier: Modifier = Modifier,
    selectedId: Long? = null,
    onSelect: ((HomeBook) -> Unit)? = null,
) {
    val t = TsuzukiTheme.tokens
    val shelf = state.shelf
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                t.case(stringResource(MR.strings.tsz_shelf, shelf.size)),
                style = labelStyle(t),
                color = muted(t),
            )
            Text(
                t.case(stringResource(MR.strings.tsz_all_books)),
                style = bodyStyle(13.sp, FontWeight.ExtraBold),
                color = t.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .tsuzukiClickable(role = Role.Button, onClick = actions.onAllBooks)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
            )
        }
        shelf.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { book ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (book.mangaId == selectedId) {
                                    Modifier.border(3.dp, t.accent, RoundedCornerShape(8.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .tsuzukiClickable(role = Role.Button) {
                                if (onSelect != null) onSelect(book) else actions.onOpenBook(book)
                            },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box {
                            Cover(
                                book,
                                Modifier.fillMaxWidth().then(
                                    if (onSelect == null) Modifier.tsuzukiSharedCover(book.mangaId) else Modifier,
                                ),
                            )
                            if (book.unread > 0) {
                                UnreadChip(book.unread, Modifier.align(Alignment.TopEnd).padding(6.dp), compact = true)
                            }
                        }
                        Text(
                            t.case(book.title),
                            style = bodyStyle(13.sp, FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        book.progress?.let { TsuzukiProgressBar(it, dots = 12, thickness = 4.dp) }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Cover(book: HomeBook, modifier: Modifier) {
    val t = TsuzukiTheme.tokens
    MangaCover.Book(
        data = book.cover,
        generatedTitle = book.title,
        modifier = modifier,
        contentDescription = book.title,
        shape = RoundedCornerShape(8.dp),
        colorFilter = eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter(),
    )
}

@Composable
private fun PlayDisc(size: Dp) {
    val t = TsuzukiTheme.tokens
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (t.isDot) t.accent else t.background.orIfUnspecified(Color.Black)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = if (t.isDot) Color.White else t.accent,
            modifier = Modifier.size(size * 0.42f),
        )
    }
}

@Composable
private fun UnreadChip(count: Int, modifier: Modifier = Modifier, onHero: Boolean = false, compact: Boolean = false) {
    val t = TsuzukiTheme.tokens
    val bg = when {
        t.isDot -> t.accent
        onHero -> t.onAccent
        else -> t.accent
    }
    val fg = when {
        t.isDot -> Color.White
        onHero -> t.accent
        else -> t.onAccent
    }
    Text(
        text = if (compact) count.toString() else t.case("+$count new"),
        style = bodyStyle(12.sp, FontWeight.ExtraBold),
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun Pip(selected: Boolean) {
    val t = TsuzukiTheme.tokens
    if (t.isDot) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(if (selected) t.accent else Color(0xFF3A3A3A)))
    } else {
        Box(
            Modifier
                .size(width = if (selected) 22.dp else 8.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) Color.White else Color.White.copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun IconCircle(onClick: () -> Unit, content: @Composable () -> Unit) {
    val t = TsuzukiTheme.tokens
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (t.isDot) {
                    Modifier.border(
                        BorderStroke(1.dp, Color(0xFF2A2A2A)),
                        CircleShape,
                    )
                } else {
                    Modifier.background(Color.Black.copy(alpha = 0.25f))
                },
            )
            .tsuzukiClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ---------------------------------------------------------------- style helpers

private fun tileFill(t: TsuzukiTokens, hero: Boolean): Modifier = when {
    t.isDot ->
        Modifier
            .background(t.tile)
            .border(1.dp, t.tileBorder ?: Color(0xFF262626), RoundedCornerShape(24.dp))
    hero -> Modifier.background(t.accent)
    else -> Modifier.background(t.tile)
}

private fun TsuzukiTokens.case(s: String) = if (isDot) s.uppercase() else s

private fun muted(t: TsuzukiTokens, alpha: Float = 0.72f) = if (t.isDot) {
    Color(
        0xFF8A8A8A,
    )
} else {
    Color.White.copy(alpha = alpha)
}

private fun Color.orIfUnspecified(other: Color) = if (this == Color.Unspecified) other else this

@Composable
private fun displayStyle(size: TextUnit): TextStyle =
    MaterialTheme.typography.displayMedium.copy(fontSize = size, lineHeight = size * 0.98f, letterSpacing = 0.sp)

@Composable
private fun bodyStyle(size: TextUnit, weight: FontWeight): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(fontSize = size, fontWeight = weight)

@Composable
private fun labelStyle(t: TsuzukiTokens): TextStyle = if (t.isDot) {
    MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.2.sp)
} else {
    MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
    )
}

/** Dot uses the dot-matrix display face for numbers; Colour uses heavy Manrope. */
@Composable
private fun numberStyle(t: TsuzukiTokens, size: TextUnit): TextStyle = if (t.isDot) {
    MaterialTheme.typography.displayLarge.copy(fontSize = size, lineHeight = size, letterSpacing = (-1).sp)
} else {
    MaterialTheme.typography.titleLarge.copy(
        fontSize = size,
        lineHeight = size,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1.5).sp,
    )
}

@Composable
private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> stringResource(MR.strings.tsz_greeting_morning)
    in 12..16 -> stringResource(MR.strings.tsz_greeting_afternoon)
    else -> stringResource(MR.strings.tsz_greeting_night)
}

@Composable
private fun todayKicker(books: Int): String {
    // Device locale, e.g. "Mon 2 Nov" or "11月3日(火)".
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMM")
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date()) + " · " +
        stringResource(MR.strings.tsz_books_count, books)
}
