# Tsuzuki design reference

The source of truth is the **"Tsuzuki App Screens"** Claude Design canvas:
https://claude.ai/artifact/JupsRJRJ2GsXbLaXnmbjMC (slug `910b88a4-7738-4f64-bac1-fc80a77806ab`).
Each board is a file `project/<Name>.dc.html`. Agents without access to the canvas: use this file plus
`docs/PLAN.md` §3, and ask the owner for a screenshot or an export of a board when the spec below is not enough.
Exported boards go in `docs/design/boards/<Name>.dc.html`.

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
| Bar, tile, dot grid | done | `theme/tsuzuki/TsuzukiComponents.kt` |
| Nav, Rail | done | `theme/tsuzuki/TsuzukiNav.kt`, wired in `ui/home/HomeScreen.kt` |
| Main, TabletPortraitLibrary, TabletLibrary (with details pane), LibraryEmpty | done, verified with real books in Dot and Colour | `presentation/tsuzuki/home/`, `ui/tsuzuki/home/` |
| MotionSplash (startup) | done, verified on both devices | `presentation/tsuzuki/startup/`, `ui/main/MainActivity.kt` |
| History (rows) | done | `presentation/tsuzuki/history/` |
| Updates | partly: row tiles (seen with data), New count + Save all tiles, filter pills; Airing strip waits for anime | `presentation/updates/`, `presentation/tsuzuki/updates/` |
| LibraryList | partly: greyscale covers, accent badges, display title, progress under covers (both grids), category pills; LibraryMixed not started | `presentation/library/components/` (`LibraryTabs`, `LibraryBadges`, grids) |
| More | done (no Source health tile yet) | `presentation/tsuzuki/more/` |
| NovelDetails, TabletNovel | partly: display title, action tiles, Resume tile with progress, synopsis tile, chapter rows as tiles; filter/download sheets not started | `presentation/manga/`, `presentation/tsuzuki/details/` |
| Reader, ReaderBars | partly: solid bars, Literata default reading face; book-tint background not started | `presentation/reader/appbars/NovelReaderAppBars.kt`, `res/font/lnr_literata.ttf` |
| Browse, Extensions | partly: pill tab row (all tabbed screens), global-search tile, sources and extensions grouped in tiles; pinned two-up tiles; health dots not started | `presentation/components/TabbedScreen.kt`, `presentation/browse/`, `presentation/tsuzuki/browse/` |
| Settings | partly: search tile, Colour/Dot theme cards, sections in one tile, every preference group as a tile; tablet two-pane list still stock | `presentation/more/settings/`, `presentation/tsuzuki/settings/` |
| Shared pill | done | `theme/tsuzuki/TsuzukiPill.kt` (`TsuzukiPill`, `TsuzukiPillRow`) |
| Everything else | not started | — |
