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
| Upstream integration checkpoint (2026-11-04) | main UI work committed and all 28 Mihon picks merged; debug build + 630 app tests pass; tablet Dot/Colour novel and manga reader checks, novel pause/resume and queue restart checks pass. Interrupted tall-image split recovery fixed and verified on the same failed chapter; owner data restored and hash-verified. Optimized preview/device bug sweep remains | merge `c54b2f809`, recovery `de061e0ef`; evidence `app/build/codex-merge-20261104/` |
| Runtime profiles (2026-11-04) | regenerated on tablet optimized preview: Home, mixed Library scrolling, details, cached novel reader, Updates, History and Browse/extensions; startup waits for populated Home. Both collectors pass; merged legacy coverage retained; preview/debug builds, 630 tests and profile/16 KB packaging checks pass. Four-book test library does not establish large-library performance | `baseline-profile/`, `app/src/main/baselineProfiles/`, commit `b8b3938de`, `docs/PERFORMANCE.md` |
| ReaderSettings phone fit (2026-11-04) | Japanese Size/Lines labels now fit: compact tiles put labels above controls; wide tiles remain inline. 40 dp circles have 48 dp touch targets and label +/- accessibility descriptions. Dot/Colour on phone and tablet: full-label widths and original values restored after +/- checks; preview/debug builds and 630 tests pass | `presentation/tsuzuki/reader/TsuzukiReaderSettingsSheet.kt`; evidence `app/build/codex-preview-20261104/` |
| Reader progress accessibility (2026-11-04) | Invisible width-reserving maximum labels excluded from semantics; actual manga current/total and novel percent/100% exposed. All eight reader/device/theme combinations checked in optimized preview; layout retained, preview/debug builds + 630 tests pass | `reader/components/ChapterNavigator.kt`, `reader/appbars/NovelReaderAppBars.kt` |
| Bar, tile, dot grid | done | `theme/tsuzuki/TsuzukiComponents.kt` |
| Nav, Rail | done | `theme/tsuzuki/TsuzukiNav.kt`, wired in `ui/home/HomeScreen.kt` |
| Main, TabletPortraitLibrary, TabletLibrary (with details pane), LibraryEmpty | done, verified with real books in Dot and Colour | `presentation/tsuzuki/home/`, `ui/tsuzuki/home/` |
| MotionSplash (startup) | done, verified on both devices; inherited grey night-splash resource fixed. Animated exit/drop reads now stay in layer/draw callbacks; tablet Dot/Colour recordings checked. Controlled cold-start/frame measurements recorded without claiming an overall smoothness gain; preview/debug builds + 630 tests pass | `presentation/tsuzuki/startup/`, `ui/main/MainActivity.kt`, `docs/PERFORMANCE.md` |
| MotionSheet | done: 300/200 ms sheet motion, Dot steps, Colour overshoot, 150 ms reduced-motion fade; recorded and checked on tablet in both themes with animations on/off; build/tests pass, test state restored | `theme/tsuzuki/TsuzukiModalBottomSheet.kt`, `TsuzukiAdaptiveSheet.kt` |
| History | done: rows, filter pills, date labels, Today / Chapters / Streak tiles (streak from `ReadingDays`) | `presentation/tsuzuki/history/` |
| Updates | done for novels: grouped book tiles with NEW / SAVED pills (seen with real data), New + Save all tiles, filter pills; Airing strip waits for anime | `presentation/updates/`, `presentation/tsuzuki/updates/` |
| LibraryList, LibraryMixed | done; LibraryMixed reviewed in live Vivaldi canvas: themed covers/badges, title, progress, categories; combined All / Novels / Manga pills, real counts and type badges; checked tablet Dot + Colour with real books and phone Colour empty state. Manhwa waits for reliable classification; counts omitted with DB pagination | `presentation/library/components/`, `presentation/tsuzuki/library/`, `ui/library/NovelsTab.kt` |
| More | done (no Source health tile yet) | `presentation/tsuzuki/more/` |
| Stats | done: goal ring, Week / Month / Year chart, chapters (words, speed, listen, anime left out: no data) | `ui/tsuzuki/stats/`, `tachiyomi/tsuzuki/ReadingDays.kt`, `ReadingChart.kt` |
| NovelDetails, TabletNovel | partly: display title, action tiles, Resume tile with progress, synopsis tile, chapter rows as tiles; ChapterFilter (with Mark previous read) + DownloadSheet done; estimates and Has notes left out (no data) | `presentation/manga/`, `presentation/tsuzuki/details/` |
| Reader, ReaderBars | partly: solid bars, Literata default reading face, book-tint background in Colour (via `BookTint`); ReaderTOC contents drawer and ReaderSettings sheet done (`presentation/tsuzuki/reader/`) | `presentation/reader/appbars/NovelReaderAppBars.kt`, `res/font/lnr_literata.ttf` |
| Browse, Extensions, Catalogue, GlobalSearch, ExtensionRepos, ExtensionDetail | partly: pill tab row, global-search tile and per-source result tiles, grouped source/extension tiles, pinned two-up tiles, extension language pills, repository tiles, extension details header, catalogue Popular/Latest segments and IN LIBRARY pills; health dots/tiles, search timings and catalogue stats wait for data | `presentation/tsuzuki/browse/` |
| UpdateSchedule | interval pills done: real supported intervals, selected accent; tablet Dot/Colour preference persistence, WorkManager scheduling/cancellation checked; original Off/data restored and hash-verified. Other board controls remain | `SettingsLibraryScreen.kt`, `Preference.kt`, `PreferenceItem.kt`, `tsuzuki/settings/TsuzukiSettingsPane.kt` |
| Settings | mostly: search tile, Colour/Dot theme cards (Settings header and Appearance, stock pickers folded under App theme), sections in one tile with neutral icons, Dot uppercase rows, theme background/dot grid behind settings screens, tablet two-pane accent outline, accent value rows incl. App language, display-face title (back circle on phones), bold section rows (chevron + summary on phones, title only in the tablet list), rail kept on tablet Settings with More selected, bold preference titles, Dot "Colour covers" switch. Not yet: Accent dot / Reduce motion rows (no preferences) | `presentation/more/settings/`, `presentation/tsuzuki/settings/` (`TsuzukiSettingsMain.kt`) |
| AppLanguage, NewUpdate, BookNotes (17-drafts) | done (NewUpdate not seen on a device) | `presentation/tsuzuki/settings/TsuzukiAppLanguage.kt`, `presentation/tsuzuki/more/TsuzukiNewUpdateScreen.kt`, `presentation/tsuzuki/details/TsuzukiBookNotes.kt` |
| SourcePriority | done, number model kept (steppers, ranked rows, type-order pills) | `presentation/tsuzuki/settings/TsuzukiSourcePriority.kt` |
| Menus, MenuPick (17-drafts) | partly: shared dropdown tile + border, accent radio, Dot uppercase/bold overflow labels. Not yet: item icons, groups, accent destructive row, screens using Material's menu directly | `theme/tsuzuki/TsuzukiMenu.kt`, `components/DropdownMenu.kt` |
| Typography weights | fixed: body text bold/semi-bold now renders (one typeface per weight) | `presentation/theme/LnrTypography.kt` |
| MotionTheme | done: 600 ms circular reveal from the picked card across Activity recreation; both directions, BACK during reveal, reduced motion and stock-theme fallback checked on tablet; build/tests pass | `theme/tsuzuki/TsuzukiThemeReveal.kt`, settings theme cards, `ui/main/MainActivity.kt` |
| MotionStack | done: 400 ms cover shuffle, Dot steps, Colour selected-cover palette morph, reduced-motion fade; phone layout checked on tablet at temporary density 500, restored afterward; build/tests pass | `presentation/tsuzuki/home/TsuzukiCoverStack.kt`, `ui/home/HomeScreen.kt` |
| MotionOpen, MotionReader | done after owner approval: 450 ms shared covers from library/Home to details and back; selected tab retained; reader is an approximate Activity scale/fade; build/tests and tablet Dot/Colour recordings pass | `presentation/util/Navigator.kt`, `ui/main/MainActivity.kt`, `ui/reader/ReaderActivity.kt` |
| Press feedback | done: remaining Home, pinned/base browse, library grid, reader controls and Stats tiles; shared source, scale first; held screenshots and Default smoke check on tablet | `theme/tsuzuki/TsuzukiMotion.kt` (`tsuzukiClickable`) |
| Translations | done: all 141 Tsuzuki keys in zh-CN, ko, es, pt-BR, fr and de; XML/coverage/placeholders and build/unit tests pass; device language unchanged, translated layouts not visually checked | `i18n/src/commonMain/moko-resources/` |
| Shared pill | done | `theme/tsuzuki/TsuzukiPill.kt` (`TsuzukiPill`, `TsuzukiPillRow`) |
| Anime (later boards) | Stage 1 in progress in isolated `codex/anime-spike`: source API and adapted loader compile, existing unit tests pass; no UI/player/device install; native dependencies need non-GPL build and 16 KB audit | `.worktrees/anime-spike/docs/ANIME_SPIKE.md` |
| About | done: app tile, version, What's new / Check updates (release builds), Source code, Licences, Built on credits with verified licences; Report a bug and privacy blurb left out | `presentation/tsuzuki/more/TsuzukiAboutScreen.kt` |
| Categories | mostly: shelf tiles with content type, default marker, global-update switch, rename/delete menu, wide New shelf action and theme background/Dot grid. Tablet Dot/Colour create-flow checked; Default retains stock FAB. Book counts and footnote left out | `presentation/tsuzuki/category/` |
| Draft boards (17-drafts) | AppLanguage, NewUpdate, BookNotes drawn as agent drafts; waiting for owner approval | `docs/design/boards/html/17-drafts/` |
| Everything else | not started | — |
