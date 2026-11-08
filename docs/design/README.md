# Tsuzuki design reference

The source of truth is the **"Tsuzuki App Screens"** Claude Design canvas:
https://claude.ai/artifact/JupsRJRJ2GsXbLaXnmbjMC (slug `910b88a4-7738-4f64-bac1-fc80a77806ab`).
The complete 2026-11-03 snapshot is in [the offline index](boards/INDEX.md) and
[visual gallery](boards/gallery.html): all 16 pages, 139 artboards, and 99 individual Dot screens.
Look at the relevant PNG and per-board HTML before changing UI; use this file, `specs.md` and
`docs/PLAN.md` §3 for implementation rules. The snapshots need no canvas access. See
[capture and refresh instructions](boards/README.md). The owner's signed-in Vivaldi session can access the live canvas.

## Tokens (exact values from the boards)

| Token | Colour | Dot |
|---|---|---|
| Background | book cover colour, default navy `#1C2B4A` | `#000000` + dot grid: `#1C1C1C` dots, 1 px, every 12 dp |
| Tile | `rgba(255,255,255,.08)`, no border, radius 24 dp | `#0D0D0D`, 1 dp `#262626` border, radius 24 dp |
| Accent | book's second colour, default gold `#E9B44C` | `#FF2D2D` (the only colour) |
| Text on accent | the background colour | `#FFFFFF` |
| Muted text | `rgba(255,255,255,.72)` | `#8A8A8A` |
| Display | Instrument Serif *italic* (Japanese: Noto Serif JP 600) | Doto 900, UPPERCASE (Japanese: DotGothic16) |
| Body | Manrope 500–800 (Japanese: Zen Kaku Gothic New) | Space Mono, UPPERCASE labels, letter-spacing 1–1.2 (Japanese: M PLUS 1 Code) |
| Numbers | Manrope 800 | Doto 900 |
| Covers | full colour | `grayscale(1) contrast(1.2)` |
| Progress | solid bar, accent fill on `rgba(255,255,255,.18)` | dots: read `#FFF`, unread `#2E2E2E`, current `#FF2D2D`; current index = round(p × n), none at 100 % |
| Hero tile ("Continue") | accent fill, background-coloured text, play disc = background colour with accent icon | dark tile, white text, red play disc |

Code: `app/src/main/java/eu/kanade/presentation/theme/tsuzuki/` (`TsuzukiTokens`, `TsuzukiComponents`,
`TsuzukiNav`, `BookPalette`). Read tokens with `TsuzukiTheme.tokens`; never hardcode theme colours in screens.

### Nav (board `Nav`, phone)
Floating pill, 16 dp from the screen edges, 62 dp tall, radius 31. Colour: `rgba(0,0,0,.32)`, icon items 48 dp, the
selected item is a white pill with icon + label in the background colour. Dot: `#111` with a `#2A2A2A` border,
text-only items `HOME / NEW / PAST / FIND / •••` (Space Mono 12, bold, spacing 1). The selected item is a white
pill with black text. Updates with unread shows `NEW 14` in red.

### Rail (board `Rail`, tablet)
96 dp wide. Colour `rgba(0,0,0,.2)`; Dot `#070707` with a 1 dp `#222` right border. From the top: logo tile 48 dp
(accent, radius 16), search button 56 dp (radius 18), items (pill 56×32, selected = accent), settings at the bottom.

## Board index (canvas pages)

- **Brand:** Brand, Logo
- **Motion:** MotionSplash (startup), MotionSpec, MotionOpen (cover → novel shared element), MotionReader (resume → reader), MotionStack, MotionTheme (theme reveal), MotionSheet
- **Home & tabs (phone):** Main (home: stack + bento), LibraryList, LibraryDisplay, LibrarySearch, LibraryEmpty, Updates, History, More; DotMain = the same screens in Dot
- **Browse & sources:** Browse, Extensions, ExtensionDetail, ExtensionRepos, Catalogue, CatalogueFilters, GlobalSearch, WebView, SourceHealth, Migration, MigrationCompare; DotBrowse
- **Novel:** NovelDetails, ChapterFilter, DownloadSheet, AddCategory, EditNovel, Tracking, TrackerSearch, MergeSources, Duplicate; DotNovel
- **Reader & listen:** Reader, ReaderImmersive, ReaderPaged, ReaderSettings, ReaderTOC, ReaderFind, ChapterEnd, Dictionary, Translate, TTS, ListenMini, Notes, ReplaceRules; DotReader
- **System & settings:** Downloads, Stats, Categories, Settings, SettingsReader, SettingsAppearance, Backup, AppLock, About, Onboarding, Widgets; DotSystem
- **Power tools (from Tsundoku):** CustomSource, ElementSelector, ChapterSearch, DuplicateFinder, SourcePriority, ImportEpub, MassImport, ExportEpub, LibraryQuery, Upcoming, FontManager, ReaderBars, Quotes, SettingsTranslation, ReaderEngine, UpdateSchedule, DataTools, TrackerAccounts; DotTools
- **Manga & manhwa:** OnboardingContent, LibraryMixed, BrowseManga, MangaDetails, MangaPaged, MangaReader, MangaReaderSettings, TabletMangaSpread; DotManga
- **Anime (Aniyomi), designed 2026-11-03, not built:** AnimeHome (Read / Watch switch, continue watching, airing this week), AnimeLibrary (novels / manga / anime), AnimeDetails (seasons, filler + release-group tags, airing countdown), BrowseAnime (anime sources, torrents, extension stores), PlayerSettings (mpv, AniSkip, decoder, custom Lua buttons, mpv.conf, torrent), Player (landscape: controls, chapter-marked seek bar, AniSkip chip, gestures, lock), PlayerSheets (subtitles + delay, audio, quality/servers, speed, episodes, video filters); DotAnime
- **Anime folded into existing boards (2026-11-03, design only):** Main + TabletLibrary + TabletPortraitLibrary (Read / Watch switch), LibraryList + LibraryMixed + Browse + Extensions + GlobalSearch (Novels / Manga / Anime), History + Updates (episodes beside chapters), Downloads (episode group, video storage), Stats (read + watch), More + Settings (Player), OnboardingContent (anime option), Backup (Aniyomi import), TrackerAccounts (Simkl, anime counts), LibraryEmpty (copy). Also: Widgets (continue watching), TabletUpdates (chapters / episodes), TabletBrowse + BrowseManga (media switch), DesktopLibrary (Read / Watch), ExtensionRepos (anime store). App: Home has a Read / Watch switch; Watch is a placeholder until the anime stages.
- **Nav (2026-11-03):** the bar is solid (book colour darkened in Colour, `#111` in Dot) over a dock in the page colour that fades in above it; content never shows through. App: `TsuzukiNavBar` uses an opaque background.
- **Tablet landscape (1280×800):** TabletLibrary (home + details pane), TabletUpdates, TabletBrowse, TabletNovel, TabletReader, TabletReaderPanel, TabletTTS, TabletSettings; DotTablet
- **Tablet portrait & foldable (800×1280):** TabletPortraitLibrary, TabletPortraitReader, Foldable; DotPortrait
- **Desktop (1440×900):** DesktopLibrary, Desktop, DesktopReader; DotDesktop
- **Components:** Cover, Rail, Icon, Nav, Bar, Logo
- Archive (not to build): R2*, Dir*

## Implementation status

| Board | Status | Code |
|---|---|---|
| Onboarding | ready for review in the onboarding worktree. Dot and Colour share six steps, including storage and permissions. Debug build and 660 unit tests pass. Device checks pending | `presentation/tsuzuki/onboarding/` |
| Upstream integration checkpoint (2026-11-04) | main UI work committed and all 28 Mihon picks merged; debug build + 630 app tests pass; tablet Dot/Colour novel and manga reader checks, novel pause/resume and queue restart checks pass. Interrupted tall-image split recovery fixed and verified on the same failed chapter; owner data restored and hash-verified. Optimized preview/device bug sweep remains | merge `c54b2f809`, recovery `de061e0ef`; evidence `app/build/codex-merge-20261104/` |
| Runtime profiles (2026-11-04) | regenerated on tablet optimized preview: Home, mixed Library scrolling, details, cached novel reader, Updates, History and Browse/extensions; startup waits for populated Home. Both collectors pass; merged legacy coverage retained; preview/debug builds, 630 tests and profile/16 KB packaging checks pass. Four-book test library does not establish large-library performance | `baseline-profile/`, `app/src/main/baselineProfiles/`, commit `b8b3938de`, `docs/PERFORMANCE.md` |
| ReaderSettings phone fit (2026-11-04) | Japanese Size/Lines labels now fit: compact tiles put labels above controls; wide tiles remain inline. 40 dp circles have 48 dp touch targets and label +/- accessibility descriptions. Dot/Colour on phone and tablet: full-label widths and original values restored after +/- checks; preview/debug builds and 630 tests pass | `presentation/tsuzuki/reader/TsuzukiReaderSettingsSheet.kt`; evidence `app/build/codex-preview-20261104/` |
| Reader progress accessibility (2026-11-04) | Invisible width-reserving maximum labels excluded from semantics; actual manga current/total and novel percent/100% exposed. All eight reader/device/theme combinations checked in optimized preview; layout retained, preview/debug builds + 630 tests pass | `reader/components/ChapterNavigator.kt`, `reader/appbars/NovelReaderAppBars.kt` |
| Novel reader safe area (V7.4) | TextView and WebView share a reading viewport inside the system bars and display cutout. Page-coloured bands stay fixed while scrolling and while toggling the reader menu. User margins remain inside the viewport. Device review pending | `ReaderActivity.kt`, `tsuzuki/reader/NovelSafeInsets.kt` |
| Reader toolbar type (2026-11-04) | Reader/MangaPaged boards' compact body title restored instead of the global 30 sp display heading; 14 sp bold title, smaller muted chapter, Dot uppercase. Eight device/theme/reader combinations plus phone Default fallback checked. Preview/debug builds and 630 tests pass. Other reader chrome differences remain | `presentation/tsuzuki/reader/TsuzukiReaderTitle.kt`; two small top-bar dispatch hooks |
| Horizontal reader progress (2026-11-04) | Dot read/current/unread dots and Colour 6 dp accent track reuse the themed progress component; Material seeking/semantics retained, themed touch area 48 dp. Tap/drag/starting-position checks pass across both readers, devices and themes, including phone RTL / tablet LTR. Preview/debug builds and 630 tests pass | `presentation/tsuzuki/reader/TsuzukiReaderSlider.kt`; full remaining matrix in `../RUNTIME_QA.md` |
| WebView stability (2026-11-04) | Existing themed shell/menus retained; renderer death closes the page while keeping the app alive. Forced crash + fresh-page test passes on both optimized devices; ordinary preview opens tablet Colour Asura / phone Dot Royal Road and returns to reader. Preview/debug + 656 app/core tests pass | `presentation/webview/WebViewScreenContent.kt`, Mihon core network group; `../UPSTREAM.md` |
| WebView header readiness (2026-11-04) | Source/header work moved off UI thread; existing loading component used until headers ready, failures leave loading safely. Both activity and navigator paths checked on both optimized devices, real Asura/Royal Road pages and BACK; renderer regression still passes. 660 app/core tests pass | `ui/webview/WebViewActivity.kt`, `WebViewScreen.kt`, `WebViewViewModel.kt` |
| Extension APK safety (2026-11-04) | Existing UI retained; Mihon installer port validates repository signing keys and cancels downloads cleanly. Six regression tests; real-APK wrong-key rejection passes on both optimized test copies. Ordinary daily preview/Dot Home and packaging pass; live successful installation matrix remains | `extension/util/ExtensionInstaller.kt`, `ExtensionLoader.kt`; `../UPSTREAM.md`, `../RUNTIME_QA.md` |
| Download storage (2026-11-04) | Existing UI retained; older novel ZIP/folder names recognized, new write names stable, large SAF batch lookup shared. Seven new regressions; real old-ZIP lookup/cleanup passes on both optimized test copies. Ordinary preview/Dot Home/packaging pass; synchronous caller migration remains | `data/download/DownloadProvider.kt`; `../UPSTREAM.md`, `../RUNTIME_QA.md` |
| Novel renderer verification (2026-11-04) | Earlier captures confirmed native TextView; actual WebView now checked in Dot/Colour on both devices, including compact title/progress/touch/seek/drag. Tablet Colour settings and phone Colour TTS pass; saved native modes/positions restored, both Dot Home. Full renderer matrix remains partial | `../RUNTIME_QA.md`; local renderer audit, backups and runtime screenshots |
| Tablet novel line length (R17) | Both renderers cap the text at 600 dp within the available pane and preserve user side margins. Chromium verified WebView scrolling, paging, dock resizing and chapter-end layout. Android device review pending | `tsuzuki/reader/NovelReadingColumn.kt`, `NovelIllustrations.kt` |
| Bar, tile, dot grid | Q1 ready for review (862 tests green): dot grid uses cached repeating tile; device comparison pending | `theme/tsuzuki/TsuzukiComponents.kt` |
| Nav, Rail | Q1 ready for review (862 tests green): fixed cells and shared spring indicator, reduced snap; device check pending | `theme/tsuzuki/TsuzukiNav.kt`, wired in `ui/home/HomeScreen.kt` |
| Main, TabletPortraitLibrary, TabletLibrary (with details pane), LibraryEmpty | done, verified with real books in Dot and Colour | `presentation/tsuzuki/home/`, `ui/tsuzuki/home/` |
| Home Watch teaser (V3) | The Read / Watch switch opens a short message about anime in a later version. Back returns to Read. Device review pending | `presentation/tsuzuki/home/TsuzukiHomeScreen.kt` |
| MotionSplash (startup) | done, verified on both devices; inherited grey night-splash resource fixed. Animated exit/drop reads now stay in layer/draw callbacks; tablet Dot/Colour recordings checked. Controlled cold-start/frame measurements recorded without claiming an overall smoothness gain; preview/debug builds + 630 tests pass | `presentation/tsuzuki/startup/`, `ui/main/MainActivity.kt`, `docs/PERFORMANCE.md` |
| MotionSheet | done: 300/200 ms sheet motion, smooth Dot curves (Q1; device recheck pending), Colour overshoot, 150 ms reduced-motion fade; recorded and checked on tablet in both themes with animations on/off; build/tests pass, test state restored | `theme/tsuzuki/TsuzukiModalBottomSheet.kt`, `TsuzukiAdaptiveSheet.kt` |
| History | done: rows, filter pills, date labels, Today / Chapters / Streak tiles (streak from `ReadingDays`) | `presentation/tsuzuki/history/` |
| Updates | done for novels: grouped book tiles with NEW / SAVED pills (seen with real data), New + Save all tiles, filter pills; Airing strip waits for anime | `presentation/updates/`, `presentation/tsuzuki/updates/` |
| LibraryList, LibraryMixed | done; LibraryMixed reviewed in live Vivaldi canvas: themed covers/badges, title, progress, categories; combined All / Novels / Manga pills, real counts and type badges; checked tablet Dot + Colour with real books and phone Colour empty state. Manhwa waits for reliable classification; counts omitted with DB pagination | `presentation/library/components/`, `presentation/tsuzuki/library/`, `ui/library/NovelsTab.kt` |
| More | done (no Source health tile yet). The scroll viewport starts below the system status bar in Dot and Colour (V8 polish, device review pending) | `presentation/tsuzuki/more/` |
| Upcoming (R16) | Calendar rows have an opt-in bell for expected releases. Reminders use the existing chapter notification channel and follow the stored forecast. Device review pending. | \`presentation/tsuzuki/upcoming/\`, \`tachiyomi/tsuzuki/upcoming/\` |
| Stats | done: goal ring, Week / Month / Year chart, chapters (words, speed, listen, anime left out: no data) | `ui/tsuzuki/stats/`, `tachiyomi/tsuzuki/ReadingDays.kt`, `ReadingChart.kt` |
| NovelDetails, TabletNovel | partly: display title, action tiles, Resume tile with progress, synopsis tile, chapter rows as tiles; ChapterFilter (with Mark previous read) + DownloadSheet done; Has notes awaits data | `presentation/manga/`, `presentation/tsuzuki/details/` |
| Novel reading estimates (R11) | The Resume tile and reader chrome show chapter and book time left from cached or downloaded text and at least three usable reading sessions. Each estimate appears once its inputs are available. Device review pending | `TsuzukiReadingEstimates.kt`, `NovelReadingEstimates.kt` |
| Reader, ReaderBars | partly: solid bars, Literata default reading face, book-tint background in Colour (via `BookTint`); ReaderTOC contents drawer and ReaderSettings sheet done (`presentation/tsuzuki/reader/`) | `presentation/reader/appbars/NovelReaderAppBars.kt`, `res/font/lnr_literata.ttf` |
| Novel reader Previous (V7.5) | In TextView and WebView, Previous opens a completed chapter at its start. A chapter already in the scroll buffer becomes the active chapter when the renderer positions it. Device review pending | `tsuzuki/reader/NovelEntryProgress.kt`, `NovelViewer.kt`, `NovelWebViewViewer.kt` |
| Browse, Extensions, Catalogue, GlobalSearch, ExtensionRepos, ExtensionDetail | partly: pill tab row, global-search tile and per-source result tiles, grouped source/extension tiles, pinned two-up tiles, extension language pills, repository tiles, extension details header, catalogue Popular/Latest segments and IN LIBRARY pills; health dots/tiles, search timings and catalogue stats wait for data | `presentation/tsuzuki/browse/` |
| UpdateSchedule | Interval pills, device and smart-update switches, category summary and download toggle use existing preferences. Interval persistence and scheduling were previously checked on the tablet. The reworked group awaits device review | `SettingsLibraryScreen.kt`, `tsuzuki/settings/TsuzukiUpdateSchedule.kt` |
| TrackerAccounts | Novel accounts first, local linked counts and masked token entry. The reworked group awaits device review | `SettingsTrackingScreen.kt`, `tsuzuki/settings/TsuzukiTrackingPreferences.kt`, `tsuzuki/settings/TsuzukiTrackerRow.kt` |
| Settings | mostly: search tile, Colour/Dot theme cards (Settings header and Appearance, stock pickers folded under App theme), sections in one tile with neutral icons, Dot uppercase rows, theme background/dot grid behind settings screens, tablet two-pane accent outline, accent value rows incl. App language, display-face title (back circle on phones), bold section rows (chevron + summary on phones, title only in the tablet list), rail kept on tablet Settings with More selected, bold preference titles, Dot "Colour covers" switch. Not yet: Accent dot / Reduce motion rows (no preferences) | `presentation/more/settings/`, `presentation/tsuzuki/settings/` (`TsuzukiSettingsMain.kt`) |
| AppLanguage, NewUpdate, BookNotes (17-drafts) | done (NewUpdate not seen on a device) | `presentation/tsuzuki/settings/TsuzukiAppLanguage.kt`, `presentation/tsuzuki/more/TsuzukiNewUpdateScreen.kt`, `presentation/tsuzuki/details/TsuzukiBookNotes.kt` |
| SourcePriority | done, number model kept (steppers, ranked rows, type-order pills) | `presentation/tsuzuki/settings/TsuzukiSourcePriority.kt` |
| Menus, MenuPick (17-drafts) | partly: shared dropdown tile + border, accent radio, Dot uppercase/bold overflow labels. Not yet: item icons, groups, accent destructive row, screens using Material's menu directly | `theme/tsuzuki/TsuzukiMenu.kt`, `components/DropdownMenu.kt` |
| Typography weights | fixed: body text bold/semi-bold now renders (one typeface per weight) | `presentation/theme/LnrTypography.kt` |
| MotionTheme | done: 600 ms circular reveal from the picked card across Activity recreation; both directions, BACK during reveal, reduced motion and stock-theme fallback checked on tablet; build/tests pass | `theme/tsuzuki/TsuzukiThemeReveal.kt`, settings theme cards, `ui/main/MainActivity.kt` |
| MotionStack | 400 ms cover shuffle by tap or horizontal swipe, smooth Dot curves and deferred depth, Colour selected-cover palette morph, reduced-motion fade. Phone layout previously checked on tablet at temporary density 500 and restored. Swipe device review pending | `presentation/tsuzuki/home/TsuzukiCoverStack.kt`, `presentation/tsuzuki/home/TsuzukiHomeScreen.kt` |
| MotionOpen, MotionReader | done after owner approval: 450 ms shared covers from library/Home to details and back; selected tab retained; reader is an approximate Activity scale/fade; build/tests and tablet Dot/Colour recordings pass | `presentation/util/Navigator.kt`, `ui/main/MainActivity.kt`, `ui/reader/ReaderActivity.kt` |
| Press feedback | done: remaining Home, pinned/base browse, library grid, reader controls and Stats tiles; shared source, scale first; held screenshots and Default smoke check on tablet | `theme/tsuzuki/TsuzukiMotion.kt` (`tsuzukiClickable`) |
| Translations | R7 checked Japanese Home, phone nav, Browse media labels, More, reader controls and History at 360 dp and 100–200% text size in Dot and Colour. The updated strings are present in all eight Tsuzuki locales. Device review pending | `i18n/src/commonMain/moko-resources/`, `i18n-novel/src/commonMain/moko-resources/` |
| v0.1 polish (V7.6) | Phone and tablet Updates share an upright caption with Dot casing. Themed list rows prioritise their label over the selected value. The docked Listen panel uses localised voice names, and the reader uses shorter renderer labels. Device review pending. | `presentation/tsuzuki/updates/`, `presentation/tsuzuki/settings/`, `presentation/tsuzuki/reader/` |
| Japanese resource coverage (V7.3) | Japanese covers the translatable strings in the shared and novel modules, including downloads and translation. Browse and Appearance settings, global and in-reader novel settings, and Font Manager use resources for their labels. Device review pending | `i18n/src/commonMain/moko-resources/ja/strings.xml`, `i18n-novel/src/commonMain/moko-resources/ja/strings.xml`, `scripts/check-japanese-strings.py` |
| Shared pill | done | `theme/tsuzuki/TsuzukiPill.kt` (`TsuzukiPill`, `TsuzukiPillRow`) |
| Anime (later boards) | Stage 1 in progress in isolated `codex/anime-spike`: source API and adapted loader compile, existing unit tests pass; no UI/player/device install; native dependencies need non-GPL build and 16 KB audit | `.worktrees/anime-spike/docs/ANIME_SPIKE.md` |
| About | done: app tile, version, What's new / Check updates (release builds), Source code, Licences, Built on credits with verified licences; Report a bug and privacy blurb left out | `presentation/tsuzuki/more/TsuzukiAboutScreen.kt` |
| Categories | mostly: shelf tiles with content type, default marker, global-update switch, rename/delete menu, wide New shelf action and theme background/Dot grid. Tablet Dot/Colour create-flow checked; Default retains stock FAB. Book counts and footnote left out | `presentation/tsuzuki/category/` |
| Draft boards (17-drafts) | AppLanguage, NewUpdate, BookNotes drawn as agent drafts; waiting for owner approval | `docs/design/boards/html/17-drafts/` |
| Chapter-end transition (V7.7) | With infinite scroll off, the themed Next chapter button shows a pressed state and holds the outgoing page until the next chapter can draw. TextView and scrolling WebView use the existing reader transition. Device review pending. | `presentation/tsuzuki/reader/NovelChapterEndTransition.kt`, `assets/novel-reader/chapter-end-card.js` |
| Batch Q (2026-11-07) | done: Q1 motion, Q2 reader prev/next + chapter-end card, Q3 save by chapter number, Q4 one Default shelf, Q5 HOME · LIBRARY · UPDATES · BROWSE · MORE, Q6 details header, Q7 Browse two-level tabs, Q12 only Colour + Dot. Device-checked tablet + S22 (preview). Q8 overflow, Q9 selection/undo/loading/advanced, Q11 reader settings also done (2026-11-07). Open: Q13 next-chapter prefetch, Q14 polish | boards `boards/fixes-2026-11/` |
| Colour typography (R6) | Instrument Serif Italic confirmed in 360 dp and tablet Home previews. The preview matrix uses the board palette and the app's content colours. Device checks for live cover palettes and full tablet panes are pending | `presentation/theme/LnrTypography.kt`, `presentation/tsuzuki/preview/TsuzukiNarrowPreviews.kt` |
| Everything else | not started | — |
