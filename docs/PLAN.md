# LightNovelReader — Product & Engineering Plan

> Scope: light novels **and** manga / manhwa / webtoons in one app.

A light-novel / web-novel reader for Android, written entirely in Kotlin, built on
the Mihon codebase, with LNReader's 328 source plugins re-implemented as native
Kotlin extensions. Goal: Mihon-level smoothness and polish, plus reading features
Mihon (a manga app) never needed.

Screen designs: see the "LightNovelReader App Screens" design canvas (24 artboards:
phone, tablet, desktop).

---

## 1. Prior art (researched 2026-11-02)

| Project | Base | Novel sources | State |
|---|---|---|---|
| **Hayai** (HayaiApp/hayai) | TachiyomiJ2K fork (not Mihon) | Kotlin APKs + LNReader **JS** plugins | Active, nightly builds only, no stable release |
| **Yomiden** (SirJohnyD/yomiden) | Mihon fork | Planned QuickJS host for LNReader plugins | Spec only, nothing implemented |
| IReader, NovelDokusha, Shosetsu | Standalone apps | Own source systems | Not Mihon-based |

### Code we can reuse (licence-checked 2026-11-02)

| Project | Licence | Reuse | Notes |
|---|---|---|---|
| **Tsundoku** (tsundoku-otaku/tsundoku) | Apache-2.0 | **Fork base (recommended)** | Mihon-derived; already has novel reader, TTS, translation, EPUB/TXT/HTML, LNReader plugins (QuickJS), LNReader backup import. Released v0.3.4 (Oct 2026). |
| Mihon | Apache-2.0 | Upstream | Keep merging upstream fixes via Tsundoku. |
| IReader | Apache-2.0 | Borrow pieces | Compose + KMP, Piper TTS on desktop, translation engines, Legado import. |
| IReader-extensions | **MPL-2.0** | Copy Kotlin sources (~85) and the Madara multisrc | MPL is file-level: copied files stay MPL, the app stays Apache. |
| LNReader plugins | MIT | Port logic to Kotlin | Already planned. |
| NovelDokusha / WebnovelReader | **GPL-3.0** | Ideas only | Copying code would force the whole app to GPL-3. |
| Readium Kotlin toolkit | BSD-3 | EPUB parsing/export | |
| sherpa-onnx | Apache-2.0 | Offline neural TTS | |

**Cost impact:** forking Tsundoku instead of Mihon removes most of Phases 0–4
(novel data model, reader, downloads, local EPUB, LNReader import, JS plugin host).
Plan becomes: fork Tsundoku → reskin to the chosen design → keep the JS plugin host
as a fallback while porting sources to Kotlin (starting from IReader-extensions'
Kotlin sources + the 14 LNReader multisrc templates) → add the "beyond Mihon" features.

**Conclusion:** no shipped Mihon-based app runs LNReader's catalogue as **native
Kotlin** sources. That is the gap this project fills. Native Kotlin sources (no JS
runtime) give faster startup, lower memory, real stack traces, compile-time
checking, and simpler sandboxing.

---

## 2. Source catalogue (from `plugins.min.json`, LNReader v3.0.0)

- **328 plugins**. Index fields: `id, name, site, lang, version, url, iconUrl, customCSS?`
- **Languages:** English 187 · Russian 26 · Arabic 20 · Spanish 18 · French 17 ·
  Turkish 15 · Portuguese 11 · Indonesian 10 · Chinese 8 · Vietnamese 5 · other 11
- **Multisrc themes (161 sources share 14 templates):** madara 77 · lightnovelwp 36 ·
  readnovelfull 8 · fictioneer 8 · readwn 7 · mtlnovel 6 · hotnovelpub 5 ·
  webnovelworld 3 · rulate 3 · novelcool 2 · novelfire 2 · ranobes 2 · ifreedom 2
- **Standalone:** 167 hand-written plugins → individual Kotlin ports.

### Kotlin source API (`source-api` module)

```kotlin
interface NovelSource {
    val id: Long            // stable hash of LNReader plugin id → backup compatibility
    val name: String
    val lang: String        // ISO code, mapped from the index's native-script names
    suspend fun getPopular(page: Int, filters: FilterList): NovelsPage
    suspend fun getLatest(page: Int): NovelsPage
    suspend fun search(query: String, page: Int, filters: FilterList): NovelsPage
    suspend fun getNovelDetails(novel: SNovel): SNovel
    suspend fun getChapterList(novel: SNovel): List<SChapter>
    suspend fun getChapterContent(chapter: SChapter): ChapterContent   // sanitized HTML
    fun getFilterList(): FilterList
}
interface ConfigurableNovelSource { fun setupPreferences(screen: PreferenceScreen) }
abstract class HttpNovelSource : NovelSource  // OkHttp, rate limit, CF interceptor, Jsoup helpers
```

Mapping from LNReader: `popularNovels` → `getPopular/getLatest`, `searchNovels` →
`search`, `parseNovel` → `getNovelDetails + getChapterList`, `parsePage` → paged
chapter lists, `parseChapter` → `getChapterContent`, `filters` → `FilterList`,
`pluginSettings` → `ConfigurableNovelSource`.

### Extension repo
- Separate Gradle project (or `extensions/` dir): `lib/` (date parsing, CF, crypto),
  `lib-multisrc/<theme>/`, `src/<lang>/<source>/`.
- Each source → signed APK; CI generates `index.min.json` + icons (Mihon format).
- **Generator script** reads `plugins.min.json` → creates source stubs, `build.gradle`,
  icons, version metadata. Logic is ported by hand.
- **Testing:** fixture tests (saved HTML, offline) for every source + nightly live
  smoke test (popular → details → chapters → content). Results publish a
  `health.json` that the app shows in Browse (see §4 Source health).

### Port order
1. 14 multisrc templates → 161 sources
2. Top ~95 English standalone sources (by popularity)
3. Remaining ~72 non-English standalone
4. Flag dead/blocked sites instead of porting blindly.

---

## 3. Design system — two themes, one layout

The whole app uses one **bento** layout (tiles of different sizes) with two
switchable themes. Theme is chosen in **Settings › Appearance** (and onboarding);
switching never moves anything, only the skin changes.

| Token | Colour | Dot |
|---|---|---|
| Background | the current book's cover colour (e.g. navy `#1C2B4A`) | `#000` + 12 px dot grid |
| Tile | `rgba(255,255,255,.08)`, no border | `#0D0D0D`, 1 px `#262626` border |
| Accent | book's second colour (e.g. gold `#E9B44C`) | `#FF2D2D` only |
| Display type | Instrument Serif italic | Doto (dot-matrix), uppercase |
| Body type | Manrope | Space Mono, uppercase labels |
| Progress | solid bar / solid ring | rows of dots / ring of dots, red = "you are here" |
| Covers | full colour | greyscale (red is the only colour) |
| Reading text | Literata on deep book tint | Literata on black |

- **Shared components:** Cover (generated art until real covers load), Icon, Nav (phone),
  Rail (tablet), Bar (progress, solid or dotted).
- **UX rules:** Resume is one tap from Home and from every novel page; primary actions
  sit in the bottom third (thumb reach); 44 px minimum targets; every gesture has a visible
  button; unread counts live on the book; source problems surface as a Home tile before
  they break a book; reader keeps Literata in both themes for long-form comfort.
- **Implementation:** one Compose `ColorScheme`-like token set per theme, provided via
  `CompositionLocal`; Colour theme derives its tokens from the cover palette
  (`androidx.palette`) of the book you're reading.

### Adaptive layouts (phones, foldables, tablets, ChromeOS/desktop)
Uses `WindowSizeClass` + Compose Material 3 Adaptive (`NavigationSuiteScaffold`,
`ListDetailPaneScaffold`).

| Width class | Navigation | Library / Browse | Reader |
|---|---|---|---|
| Compact (< 600 dp, phones) | Bottom bar | Single pane, 3-col grid | Single column / single page |
| Medium (600–840 dp, foldables, small tablets) | Navigation rail | List-detail when landscape | Single page, wider margins (max 70 ch line length) |
| Expanded (> 840 dp, tablets) | Rail | **List + detail panes** (grid left, novel details right) | **Two-page spread** in paged mode; centered column in scroll mode |
| Large / desktop (> 1200 dp: ChromeOS, Samsung DeX, Android desktop windowing) | Permanent sidebar w/ pinned sources | **Three panes** (sidebar · grid · preview) | Two-page spread, optional side TOC |

Plus: keyboard shortcuts (`/` search, `Ctrl K`, arrows/space to turn pages,
`A` add to library), mouse hover states, right-click context menus, resizable
windows, foldable hinge awareness (no text across the hinge), stylus-friendly
highlighting.

---

## 4. Screens and features

### Main tabs (bottom bar / rail / sidebar)

**Library**
- "Continue reading" hero card (cover, chapter, %, time left, Resume)
- Category tabs with counts; per-category sort & display
- Grid (compact / comfortable / cover-only) or list; adjustable columns
- Badges: unread, downloaded, local, language, source down
- Filters: unread, downloaded, started, completed, tracked, bookmarked, source
- Sort: alphabetical, last read, last update, unread, chapter count, date added, word count
- Multi-select actions: categorize, mark read, download, delete, migrate, merge
- Search within library; global-search fallback
- Pull to refresh (update library)

**Updates**
- Grouped by date; new / saved / read states
- "N new chapters" banner with Get all
- Per-row download, mark read, bookmark; multi-select
- Update schedule summary (last / next check)
- Restrictions: Wi-Fi only, charging, skip completed/unstarted

**History**
- Today/yesterday/older groups with per-entry progress bar
- Reading summary tiles: time today, chapters, streak
- Resume, remove entry, clear all; search

**Browse**
- Tabs: **Sources · Extensions · Migrate**
- Sources: pinned, last used, by language; Latest shortcut; **health dot**
  (working / slow / down + latency)
- Extensions: language filter, updates pending (Update all), installed, available,
  trust/untrusted states, per-extension settings, multiple repos
- Global search across all/pinned sources with per-source status (results /
  searching / Cloudflare / error) and progress

**More**
- Downloaded only, Incognito toggles
- Download queue, Stats & goals, Source health & migration, Categories,
  Backup & sync, Settings, About

### Discovery & novel

**Source catalogue**: Popular / Latest / Completed; filter sheet (genres,
status, sort; source-defined); "Hide in library"; WebView button; infinite grid.

**Novel details**
- Cover, title, author, status, source, chapter count, **word count**
- Actions: Add to library (with category), Tracking, **Merge sources**, WebView,
  share, edit metadata (title, cover, description), download options
- Synopsis (expand), tags (tap → search)
- Chapter list: new / saved / read, read %, per-chapter **reading time estimate**,
  filter/sort, scanlator/volume grouping, range-select download
- Resume FAB

### Reading

**Reader (scroll)**: native Compose text renderer (not a WebView)
- Top bar: title, chapter, % + time left, bookmark, find-in-chapter, menu
- Bottom bar: chapter slider with prev/next, Contents, Listen, Appearance,
  Translate, Notes
- Seamless infinite scroll into next chapter (preloaded)
- Long-press selection → highlight (4 colours), note, copy, share quote,
  define, translate, search web, add to glossary
- Tap zones configurable; volume-key scrolling; auto-scroll with speed
- Progress saved per paragraph; resume exactly where left

**Reader (paged)**
- Pagination computed off-thread with `TextMeasurer`; slide / curl / none animations
- Header/footer (title, chapter, page x/y, time left); no text ever under footer
- Two-page spread on wide screens

**Appearance sheet** (live preview behind sheet)
- Tabs: Appearance · Layout · Controls
- Themes: Paper, Sepia, Sage, Slate, Night, AMOLED, custom (bg/text/link colors)
- Fonts incl. user fonts; size, line spacing, paragraph spacing, margins,
  justify + hyphenation, indent, max line width
- Paged/scroll, **Focus reading** (bold word starts, dim non-active lines),
  keep screen on, brightness/warmth overlay, immersive mode, orientation lock
- Per-novel overrides ("use for this novel only")

**Listen (TTS)**
- Media3 `MediaSessionService`: lock-screen/notification/Bluetooth controls
- Engines: system TTS + **offline neural voices** (sherpa-onnx / Piper)
- Sentence highlighting synced in reader; paragraph skip; chapter skip
- Speed 0.5–3×, pitch; per-novel voice; pronunciation dictionary
- Sleep timer (minutes / end of chapter); auto-continue N chapters
- Works with screen off and across downloaded chapters

**Notebook (highlights, notes, glossary)**
- Highlights with notes, by chapter; bookmarks
- **Glossary / character tracker**, spoiler-safe up to current chapter
- Export Markdown / Obsidian / Anki CSV

**Tracking sheet**: NovelUpdates (new), AniList, MyAnimeList, Kitsu; status,
chapters, score, dates; auto-sync on chapter finish; auto-match on add.

### System

**Download queue**: grouped by novel; progress; errors with auto-retry/backoff
(HTTP 429 aware); reorder; pause/resume all; parallel per source; Wi-Fi only;
storage estimate.

**Stats & goals**: daily goal ring, streak, minutes chart (week/month/year),
words read, reading speed (wpm), chapters, TTS time, top genres, per-novel stats.

**Settings**: Appearance · Library · Reader · Listen · Translation & dictionary ·
Downloads · Tracking · Browse · Backup & sync · Security & privacy · Advanced
(network, DoH, user agent, logs), all searchable.

**Source health & migration**: working/slow/down summary (on-device telemetry +
nightly CI `health.json`); alert when a library source fails; one-tap
migration with **fuzzy matching**, chapter-number mapping, progress kept,
gap-filling from a second source.

**Onboarding**: welcome → languages → import (LNReader backup, Mihon-style
backup, Drive) → reading theme; permissions asked in context, not up front.

### Inherited from Tsundoku (audited 2026-11-02)

Tsundoku already ships these. Each one now has a design in both themes on the
canvas page **"Final · Power tools (from Tsundoku)"**, or in an existing board.

| Tsundoku feature (code) | Design board |
|---|---|
| Custom sources from CSS selectors (`ui/customsource`, `CustomSourcesScreen`) | Make a source |
| Element selector wizard, 9 steps, auto-detect, full test (`ElementSelectorScreen`) | Site picker |
| Search inside downloaded chapters, regex / case / whole word (`ChapterSearchScreen`) | Search inside chapters |
| Duplicate detection: exact / contains / URL, bulk select, move, delete (`library/duplicate`) | Find duplicates |
| Source priority by type and by source (`SourcePriorityScreen`) | Source priority |
| EPUB import with novel/volume grouping (`ImportEpubScreen`) | Import EPUB |
| Mass import by URL, batches, pause, retry errors (`MassImportDialog`) | Mass import by URL |
| Batch EPUB export: translation mode, join volumes, CSS/JS, file name (`BatchExportEpubDialog`) | Export as EPUB |
| Library search syntax `title: author: tag: source: id: -x` and regex (`LibrarySearchSpec`), open random | Library power search + tools |
| Upcoming calendar (`mihon/feature/upcoming`) | Upcoming calendar |
| Font manager: system, imported TTF/OTF, Google Fonts (`FontManagerScreen`) | Font manager |
| Status bar elements and toolbar button placement (`StatusBarElementsScreen`, `AppbarItemConfig`) | Reader status bar + toolbar |
| Quotes saved from selected text (`reader/quote`, `QuotesSheet`) | Quotes sheet |
| Translation engines: OpenAI, DeepSeek, Gemini, DeepL, Google free, Ollama, LibreTranslate, NVIDIA NIM, SYSTRAN, HuggingFace, Custom HTTP; queue, chunking, contextual anchoring (`SettingsTranslationScreen`) | Translation engines |
| Novel reader engine: native text vs WebView, paged WebView, auto-load/mark-read thresholds, block media, raw HTML, source CSS, margins, TTS speed/pitch (`SettingsNovelReaderScreen`) | Reader engine settings |
| Library update schedule and restrictions, smart update, per-source throttle overrides, page-load delay, download ahead (`SettingsLibrary/NovelDownload/Download/Browse`) | Updates, downloads, throttling |
| LNReader backup import, storage location, DB stats and maintenance (normalise URLs, remove duplicate URLs, delete translations, clear DB), DoH, user agent, worker info (`SettingsData/Advanced`, `WorkerInfoScreen`) | Data & maintenance |
| Trackers: NovelUpdates (with list mapping), NovelList, RanobeDB, MangaBaka, MAL, AniList, Kitsu, MangaUpdates, Shikimori, Bangumi, Hikka (`SettingsTrackingScreen`) | Tracker accounts |
| Manga support: image reader (webtoon, L→R, R→L, vertical), combined or separate Novels/Manga library, "hide manga UI" | Page "Final · Manga & manhwa" (see §4b) |
| Novel notes (`MangaNotesScreen`), migration config/list, extension stores and filters, source preferences, backup create/restore, app lock, onboarding, about | Existing boards: Notebook, Migration, Extension repos, Extension detail, Backup, App lock, Onboarding, About |

Not designed (plain system UI is fine): crash screen, debug info, open-source
licences, deep-link handler, app-language picker, new-update dialog.

### 4b. Manga & manhwa (first-class, not an extra)

LightNovelReader reads light novels, manga and manhwa/webtoons. The Mihon base
already has the image readers (`ui/reader/viewer/pager`, `webtoon`), Mihon/Tachiyomi
extension repo support and manga trackers. Manga screens are on by default
(`hideMangaUi = false`, `joinedLibrary = true`). No repo is bundled; users add repos.

Designs (page **"Final · Manga & manhwa"**, both themes):

| Screen | What it shows | Code status |
|---|---|---|
| Onboarding: "What do you read?" | Novels / Manga / Manhwa, which sets the tabs and `hideMangaUi` | New step (replace checkbox in `ThemeStep`) |
| Library | One library with All / Novels / Manga / Manhwa chips and type badges | New type filter; badges new |
| Browse | Novels / Manga / All switch, manga sources, add Mihon repo | Restyle of existing |
| Manhwa details | Scanlator filter, page counts, "opens as webtoon" per series | Auto-webtoon is new |
| Paged reader | Right-to-left manga, tap-zone overlay, page slider | Restyle of existing pager |
| Webtoon reader | Long strip for manhwa | Restyle of existing webtoon viewer |
| Reader settings sheet | Reading mode, tap zones, scale, crop, two-page, split, colour filters, CBZ | Mostly existing settings |
| Tablet spread | Two pages side by side in landscape | Existing pager dual-page, restyled |

New logic to build:
1. **Content type** per entry (novel / manga / manhwa), from the source or tags
   (manhwa, webtoon, long strip), editable on the details screen.
2. **Auto reading mode**: manhwa opens in webtoon, manga in right-to-left;
   per-series override (Mihon already stores per-series viewer flags).
3. **Library type filter** and badges; History, Updates and Stats can also be split by type.

---

## 5. Features beyond Mihon

1. **Native Kotlin ports of all 328 LNReader sources** with live health status
2. **Source health dashboard + proactive migration** when a site dies
3. **Merge sources**: one novel, several sources, missing chapters filled automatically
4. **Offline neural TTS** with sentence sync, sleep timer and auto-continue
5. **Highlights, notes, glossary/character tracker** (spoiler-safe) with export
6. **On-device translation** (ML Kit) incl. bilingual side-by-side mode and
   **text replacement rules** (fix MTL names, regex cleanup per novel/source)
7. **Tap-to-define dictionary** (offline dictionaries, Yomitan format)
8. **Reading goals, streaks and deep stats** (wpm, words, time)
9. **Reading time estimates** per chapter and "time left" everywhere
10. **Smart downloads**: auto-fetch next N chapters on Wi-Fi/charging, auto-delete after read
11. **Cross-device sync** (Google Drive / WebDAV / self-hosted), not just backups
12. **LNReader backup import** and EPUB import/export (with cover & metadata)
13. **Focus reading** (bionic-style), dyslexia font, line focus, max line width
14. **First-class tablets, foldables and ChromeOS** with two-page spread and
    keyboard shortcuts
15. **Home-screen widgets**: continue reading, daily goal
16. **Duplicate detection** when adding a novel already in library from another source

---

## 6. Architecture (inherited from Mihon, adapted)

- Modules: `app`, `core`, `core-metadata`, `data`, `domain`, `source-api`,
  `source-local` (EPUB/TXT), `presentation-core`, `presentation-widget`, `i18n`,
  `macrobenchmark`, `reader-engine` (new), `tts` (new).
- Compose + Voyager navigation, SQLDelight DB, Injekt DI, coroutines/Flow,
  OkHttp + Coil, WorkManager.
- **reader-engine**: HTML → sanitized block model (Jsoup `Safelist`) →
  `AnnotatedString` per paragraph, cached in memory + disk; `LazyColumn` for scroll;
  `TextMeasurer` paginator for paged; images via Coil.
- **DB changes:** manga → novel tables; chapter: `word_count`, `last_paragraph`,
  `last_offset`; new tables: `highlights`, `notes`, `glossary_terms`,
  `replacement_rules`, `reading_sessions`, `source_health`, `merged_sources`.
- **Performance budget:** cold start < 600 ms on mid-range device, chapter
  open < 150 ms from cache, 0 janky frames during scroll; enforced with baseline
  profiles + macrobenchmarks in CI.

---

## 7. Roadmap

| Phase | Scope | Est. |
|---|---|---|
| 0 | Fork Mihon, rebrand (name, app id, icon), remove image-reader code, CI | 2 days |
| 1 | `source-api`, DB migration, LNReader-compatible source IDs, ISO lang mapping | 4 days |
| 2 | reader-engine: scroll mode, appearance sheet, progress | 2 weeks |
| 3 | Paged mode, two-page spread, adaptive layouts (rail, list-detail, desktop) | 1.5 weeks |
| 4 | Downloads (HTML), local EPUB/TXT, EPUB export, LNReader backup import | 1 week |
| 5 | Extension repo + generator + multisrc templates (161 sources) | 2 weeks |
| 6 | TTS (system → neural), notebook, glossary | 2 weeks |
| 7 | Standalone ports wave 1 (EN) + nightly health CI | 3–4 weeks |
| 8 | Tracking (NovelUpdates), stats & goals, smart downloads, migration/merge | 2 weeks |
| 9 | Translation, dictionary, sync, widgets, standalone ports wave 2 | 3 weeks |
| 10 | Polish, accessibility pass, translations, release | 1 week |

**v0.1 milestone** = Phases 0–3 + madara & lightnovelwp templates (113 sources).

---

## 8. Open decisions

1. Extensions as separate APKs (recommended, Mihon model) vs bundled
2. Extensions in this repo vs a second repo
3. Final app name and application ID (e.g. `io.github.manojx2005.lightnovelreader`)
4. License: Apache-2.0 (inherited) is fine; ported plugin logic credits LNReader (MIT)
