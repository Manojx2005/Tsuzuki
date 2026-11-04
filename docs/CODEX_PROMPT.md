# Codex hand-off: finish the Tsuzuki design and make the app match it completely

You are continuing work on **Tsuzuki**, an Android reader for light novels, web novels, manga and manhwa
(Kotlin, Jetpack Compose), a fork of Tsundoku (itself a Mihon fork). The owner wants the app to be a
**complete match of the design boards**: every board built, in both themes (Colour and Dot), with the features
the boards show actually working.

## START HERE: latest checkpoint (2026-11-04, Codex)

The owner authorized this handoff and local commits/merge, then asked to keep working across usage resets;
both tablet and phone may be used for reversible tests. No push. Hourly continuation heartbeat:
`continue-tsuzuki-work` (this chat). Keep the full priority list active; do not start new features yet.

Completed: main work in `cf5d4326c`, `5bb7e6555`, `f76fc2811`, `fa569add0`; all 28 picks merged cleanly in
`c54b2f809`. `de061e0ef` fixes interrupted tall-image splits found in the restart test. Changed-file Spotless,
debug build and all 630 app tests pass. Tablet Dot/Colour novel and manga reader checks pass, novel group
pause/resume verified, novel and manga queues survive force-stop and resume. The actual failed manga chapter
recovered without clearing its partial files; CBZ CRCs pass, 43 entries, none empty. Crash buffer empty.

Owner data restored and all 12 original DB/prefs/WorkManager hashes verified before restart. Fresh baseline:
Continue = Nano Machine chapter 336; preserve this instead of the old Mother of Learning baseline. Only the
two new test download folders removed; original directory set matches. Tablet left Dot Home with the fixed APK.
Phone connected, untouched. Evidence: `app/build/codex-merge-20261104/` and `app/build/codex-*-validation.log`.
Exports and outreach still untracked; anime worktree untouched. No push.

The old `../Tsuzuki-upstream` folder is fully removed (native PowerShell long paths resolved the deletion
failure). Next: continue optimized preview bug sweep and measured startup/frame-timing checks.
Owner approved copying the debug library/settings into preview on both devices, installing the tablet's Royal
Road and Asura Scans sources on the phone, and temporary EPUB/Royal Road mass-import tests with backup and
cleanup. Preserve any existing preview data before testing. Release keys, publishing and telemetry changes
remain deferred. Workflow not actionlint-checked. The owner also authorized designing uncovered/inconsistent
pages to match the shared Colour/Dot scheme (existing boards remain authoritative).

Afternoon progress: `b8b3938de` regenerates baseline/startup profiles on tablet optimized preview; the full
Home/Library/details/cached-reader/tabs collector and separate Home-ready startup collector pass. Profiles
retain legacy coverage and package successfully; preview/debug builds, 630 tests and 16 KB checks pass.
See `docs/PERFORMANCE.md`. Both devices now have separate preview copies of the four-book library; debug
data remains separate. Phone Royal Road JS plugin installed; Asura upgraded in place from 1.4.54 to tablet's
1.6.71, with old/new APKs saved locally. Phone manga now renders in paged and long-strip modes; swipe advances
pages and the original default mode was restored. Preview tests change only
their copied reader history; original debug baseline is preserved. ReaderSettings Japanese labels clipped
on the phone: fixed responsive labels plus 48 dp touch boxes, checked in Dot/Colour on both devices with
reversible value changes and screenshots. Build/tests/optimized preview pass. Evidence and live test helpers
are in `app/build/codex-preview-20261104/`. Startup/frame benchmark and splash draw-phase cleanup saved in
`b68b3fc2c`; measurements and limits are in `docs/PERFORMANCE.md` (no demonstrated overall smoothness gain).
Both reader progress labels also exclude invisible sizing text from accessibility; all eight device/theme/reader
checks pass, optimized/debug builds and 630 tests pass. Both preview copies are now Dot; return tablet to Dot
Home before leaving it. Remaining runtime sweep still includes downloads, TTS,
translation, alternate rendering modes, rotation/process death and measured smoothness.

## Previous handoff (2026-11-04 ~10:45, from Claude; commit/merge steps completed above)

Owner's order: **(1) merge everything done so far → (2) quality-of-life and smoothness → (3) Mihon work → (4) new features.**
Owner chose: commit `main` in logical commits **authored as the owner (Manojx2005), no Co-Authored-By / "Generated
with" trailers**, then merge `upstream/mihon-picks`; **do not push**.

Done: commit `7c7b6bfe0` (docs, design boards PNG + HTML, AGENTS, UPSTREAM, this prompt, PRIVACY.md, scripts,
upstream-watch workflow). Build + all unit tests passed on the full working tree just before it.

Next steps, in order:
1. Commit the rest of `main` (still uncommitted), suggested split: (a) all `i18n/**/strings.xml`; (b) all app / core /
   presentation-core code and resources + `app/build.gradle.kts` + `.gitignore`; (c) `.github/workflows/tsuzuki-preview.yml`.
   Leave **uncommitted**: `docs/design/boards/exports/` (78 MB, reference only) and `docs/outreach/` (owner's Reddit draft).
   Run `:app:assembleDebug :app:testDebugUnitTest` before committing.
2. Merge `upstream/mihon-picks` (worktree `../Tsuzuki-upstream`; 28 commits; 18 Mihon fixes + per-source manga download
   speed-up + novel engine split). Keep Tsuzuki UI hooks on conflicts (expect `Downloader`, `MangaViewModel`, `MangaScreen`).
   Build, test, install on the tablet, smoke-test Home / details / novel + manga reader / a novel and a manga download
   (back up and restore owner data). Then `git worktree remove ../Tsuzuki-upstream`, update `docs/UPSTREAM.md`, re-check
   novel group pause/resume (order 8 note). Don't remove `.worktrees/anime-spike`.
3. Updates channel: `preview` build type now has the updater on (`app/build.gradle.kts`) and
   `.github/workflows/tsuzuki-preview.yml` publishes signed `rNNNN` releases. Owner must make the repo public, create a
   release keystore (`keytool -genkeypair -v -keystore tsuzuki-release.jks -alias tsuzuki -keyalg RSA -keysize 4096
   -validity 36500`), add `keystore.properties` locally (storeFile / storePassword / keyAlias / keyPassword) so local
   preview builds use the same key, and add Actions secrets SIGNING_KEY_BASE64, KEY_STORE_PASSWORD, KEY_ALIAS,
   KEY_PASSWORD. Workflow is not yet run or actionlint-checked. The optimised preview APK builds (R8 OK, baseline
   profile included) but has **not been installed or tested** on a device yet; preview installs beside `.dev`
   (app id `…tsuzuki.debug`), move the library with backup / restore (ask owner).
4. Then the PRIORITY list below (starting at item 4, bug sweep), then Part A/B, Part E, Part C/D.

## PRIORITY (owner, 2026-11-04): usable, stable, smooth and fast first; new features wait

The owner wants to use the app daily as soon as possible. Work in this order and **don't start Part C (new
features) or anime** until 1–6 are done:

1. **Run an optimised build, not debug.** Daily use should be the `preview` build type (R8 minify + resource
   shrink, debug-key signed, app id `…tsuzuki.debug`, installs beside the `.dev` build): `./gradlew :app:assemblePreview`.
   Debug builds are far slower in Compose. Fix any R8 breakage (missing keep rules for reflection, kotlinx
   serialization, QuickJS / LNReader plugins, extension loading, Glance widget) and check every main flow on it.
   Generate and ship a **baseline profile** (`baseline-profile` module) for startup, Home, Library scroll, details
   and reader open. Moving the owner's library: Backup in `.dev` → Restore in preview (ask first).
2. **Picks merge (order 9)** once the owner commits `main`: brings 18 Mihon fixes plus the faster per-source
   manga downloads.
3. **Take Mihon's low-divergence files whole** (network/Cloudflare interceptors, WebView, extension installer,
   download provider) to get the WebView / Cloudflare / install-crash fixes; re-add our 2–4 lines each.
4. **Bug sweep on both devices** (tablet `R52Y90HA77L` and phone `RZCWA139X1X`), Dot and Colour, release-type build:
   Home, Library (combined + separate), search, details, chapter filter / download sheets, novel reader (TextView
   and WebView renderers, TTS, translation, contents drawer, progress and history saved), manga reader (paged +
   webtoon), downloads (novel + manga, pause / resume / kill-restart), updates, history, browse / global search /
   catalogue, extensions install / update, backup + restore, settings, theme switch, rotation, back gestures,
   process death (`adb shell am kill` in background). Fix every crash (`logcat -b crash`) and visible glitch.
5. **Smoothness / speed pass**, measured, not guessed: Home, Library grid scroll, details chapter list,
   reader open time, cold start (`adb shell am start -W`), with `dumpsys gfxinfo <pkg> framestats` jank counts
   before / after. Likely targets: stable keys and `contentType` on lazy lists, `remember` / `derivedStateOf` for
   heavy per-item work, cover sizes (Coil `size()`), avoid recomposing whole screens on palette / progress ticks,
   `ProvideBookPalette` animation cost, dot-grid background drawing, startup work on the main thread. Keep
   motion specs as designed; reduce cost, not the animations.
6. **Usability polish** that makes daily use nicer without new features: touch targets ≥ 48 dp, readable
   contrast in both themes, consistent back behaviour, no dead taps, loading / empty / error states on every
   screen, Japanese text fitting, phone layout of every Tsuzuki screen.
7. Then Part A / B design work, then Part E (merge-friendly splits), then Part C / D.

## Read first (in this order)

1. `AGENTS.md`: rules, build/test commands, device list, and the task queue. **Current state** at the top is
   the latest status (2026-11-04). Follow its Rules exactly: Tsuzuki code in `tsuzuki` packages, small
   dispatch hooks in upstream files, no re-indenting upstream code, add every upstream file you touch to the
   "already touched" list.
2. `docs/design/README.md`: tokens, nav/rail specs, and the **Implementation status** table.
3. `docs/design/boards/INDEX.md`: every board with PNGs (`png/<page>/` and `png/<page>/dot/`) and HTML.
   **Open the board PNG (both themes) before building any screen.** If a board isn't there, ask the owner.
4. `docs/design/specs.md`: sizes, colours, states and motion for the boards.

## How to work (non-negotiable)

- Build for **Dot first**, then check **Colour**. Stock themes (Default etc.) must stay unchanged.
- Use the existing building blocks: `TsuzukiTheme.tokens`, `TsuzukiPill` / `TsuzukiPillRow`, `TsuzukiSectionLabel`,
  `tsuzukiGroupedTile`, `TsuzukiTile`, `TsuzukiProgressBar`, `tsuzukiClickable` (press feedback),
  `TsuzukiMenuItem` / `TsuzukiMenuIcons` (menus), `tsuzukiImportCard` / `tsuzukiTileColor` / `tsuzukiDialogColor`
  (stock cards and dialogs), `TsuzukiViewStyle` (View-based lists), `isTsuzukiStyle()`, `tsuzukiCase()`.
  Fonts only via `MaterialTheme.typography` (never set `fontFamily`). Dot: uppercase, red is the only colour,
  covers greyscale unless the "Colour covers" switch is on.
- **Real data only.** If a board shows a number the app doesn't record, leave that element out and write it down.
- Every new string: add to `base`, `ja`, `zh-rCN`, `ko`, `es`, `pt-rBR`, `fr`, `de` (moko-resources), escape `'` as `\'`.
- Per task: `./gradlew :app:assembleDebug :app:testDebugUnitTest` with JDK 21
  (`JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`), Spotless **per file**
  (`./gradlew :app:spotlessApply -PspotlessIdeHook=<absolute path>`, run twice until "IS CLEAN"; the whole-project
  run touches unrelated files), install on the tablet `R52Y90HA77L`, screenshot, `logcat -b crash`.
- **Owner data on the tablet:** before any test that reads, downloads, imports or opens the reader, back up
  `databases/tachiyomi.db*` and all `shared_prefs/*` with `adb exec-out run-as … cat`, and afterwards restore with
  `adb push` + `run-as cp` and verify SHA-256 (never pipe binaries through stdin). Opening a reader changes history;
  browsing a catalogue adds non-library rows; downloads write to `/sdcard/Mihon/downloads/…` (delete test folders).
  Leave Mother of Learning as the Continue book (82% of chapter 1). Switch the tablet back to Dot when done.
- Don't commit. Don't touch release/signing/telemetry config (owner deferred order 11). Tick each task in
  `AGENTS.md` with a short note and update the status table in `docs/design/README.md`.

---

## Part A: boards not built yet (design work)

Grouped by board page. "Upstream screen" means the screen exists from Tsundoku/Mihon and only needs the board's
look; "new" means the feature itself is missing (see Part C).

### 03-home
- `LibraryDisplay`, `LibrarySearch`: done as part of Library; re-check against the PNGs (filter/sort/display sheet).
- `DotMain`: verify the 8 Dot screens side by side with the board.

### 04-browse
- `CatalogueFilters`: source filter sheet (upstream sheet; restyle as a Tsuzuki sheet with pills/switches).
- `WebView`: Cloudflare check screen (upstream `WebViewScreenContent`; menu icons done, rest stock).
- `SourceHealth`: **new feature** (no source health data exists; see C).
- `Migration`, `MigrationCompare`: upstream migration screens (`mihon/feature/migration/*`), not restyled.
- Catalogue leftovers: chapter count / rating under covers (no data), floating Filters button, source status line.
- Global search leftovers: "N results · 0.6 s" (time not measured), Novels / Manga / Anime pills, dotted source progress.
- Extension detail leftovers: health / speed / checked tiles (no data), "What's new in" (no changelog).

### 05-novel
- `NovelDetails`, `TabletNovel`: partly done. Left: reading-time / size estimates (no data), "Has notes" filter.
- `AddCategory` (add-to-library category dialog), `EditNovel` (edit info), `Tracking` (tracking sheet),
  `TrackerSearch` (match search), `Duplicate` (duplicate found dialog): upstream dialogs/sheets, not restyled.
- `MergeSources`: **new feature** (no merge support in code).

### 06-reader (novel reader)
- `ReaderImmersive` (immersive + text selection), `ReaderPaged` (paged mode chrome): not restyled.
- `ReaderFind`: **new feature** (no find-in-chapter in code).
- `ChapterEnd`: chapter transition + recap: transition is upstream; **recap is new**.
- `Dictionary`: **new feature** (no dictionary lookup).
- `Translate`: bilingual mode: translation exists (Tsundoku engines); bilingual layout needs checking/restyle.
- `TTS`, `ListenMini`: TTS exists upstream; full player and in-reader mini player not restyled; sleep timer to verify.
- `Notes`: notebook with highlights & glossary: **highlights and glossary are new** (only per-book notes exist,
  now styled as BookNotes).
- `ReplaceRules`: text replacement exists in reader settings (`NovelPage`, `ReaderPreferences`); build the board's screen.
- `Reader`: remaining controls per board (bars, bottom toolbar) beyond what's done.
- `DotReader`: verify all 13 in Dot.

### 07-system
- `SettingsReader`: reader settings screen, not restyled.
- `AppLock`: board shows an in-app PIN pad; app uses the system prompt. **Owner parked this as a future update; don't build.**
- `Onboarding` (4 steps): upstream onboarding, not restyled.
- `Widgets`: Reading widget done (`tsuzuki/widget/`). Left: Continue watching (anime), Listening widget (TTS state),
  notification styles shown on the board.
- `Backup`: left: sync providers (none exist; see C), "Synced N min ago" header, "Bring your library" tiles.
- `Downloads`: left: storage size and MB/s tiles (not measured); manga rows still system font (optional Compose
  rewrite of `DownloadAdapter` / `DownloadHolder`).
- `Categories`: left: per-shelf book count, footnote.
- `Settings`: left: Accent colour row and Reduce motion row (no preferences: see C).
- `SettingsAppearance`: re-check against board.

### 08-tools
- `CustomSource`, `ElementSelector` (site picker): upstream screens exist, not restyled.
- `ChapterSearch` (search inside chapters): upstream action exists, not restyled.
- `DuplicateFinder`: upstream `DuplicateDetectionScreen`, not restyled (only its Source priority sub-screen is done).
- `ExportEpub`: upstream export, not restyled.
- `LibraryQuery`: library power search + tools: **new** (no query builder).
- `Upcoming`: upstream `mihon/feature/upcoming`, not restyled.
- `ReaderBars`, `ReaderEngine`, `SettingsTranslation`: settings screens, not restyled.
- `Quotes`: quotes sheet exists upstream, not restyled.
- `UpdateSchedule`: interval pills done; other controls on the board left.
- `DataTools`: clean-up group moved into Data and storage; left: storage breakdown (chapters / covers / database
  sizes) and novels / chapters / database stat tiles (not measured: see C).
- `MassImport`: full page, link-count pills, red unsupported links done. Left: board's back circle / title row and
  action bar; batch cards never seen on a device (need a real import, ask owner for a test source).
- `ImportEpub`: left: optional "off" switch for auto-grouping (owner to decide).
- `DotTools`: verify all 18 in Dot.

### 09-manga
- `OnboardingContent` ("what do you read?"), `BrowseManga`, `MangaDetails` (scanlators, auto webtoon),
  `MangaPaged` (RTL paged reader), `MangaReader` (webtoon), `MangaReaderSettings`, `TabletMangaSpread`, `DotManga`:
  manga reader and details are upstream Mihon UI, not restyled. Manhwa classification isn't reliable yet
  (LibraryMixed omits manhwa).

### 10-anime
- All boards (`AnimeHome`, `AnimeLibrary`, `AnimeDetails`, `BrowseAnime`, `PlayerSettings`, `Player`,
  `PlayerSheets`, `AnimeTracking`, `DotAnime`): **feature doesn't exist**. Stage 1 spike is in
  `.worktrees/anime-spike` (branch `codex/anime-spike`); see AGENTS "Later: anime" and `docs/CLAUDE_HANDOFF.md`.
  Home's Watch tab is a placeholder.

### 11-tablet / 12-portrait / 13-desktop
- Done: `TabletLibrary`, `TabletPortraitLibrary`, `TabletSettings` (rail kept, list + detail).
- Not built: `TabletBrowse` (sources + catalogue two-pane), `TabletReader` (two-page spread),
  `TabletTTS` (text + player), `TabletUpdates` (list + chapter preview), `TabletReaderPanel` (reader + notes side
  panel), `TabletPortraitReader`, `Foldable` (unfolded details), `DotTablet` / `DotPortrait` checks.
- Desktop boards (`DesktopLibrary`, `Desktop`, `DesktopReader`, `DotDesktop`): Android-only app today; treat
  as large-window layouts or ask the owner.

### 14-components / 01-brand
- `Cover` (generated cover art for books without covers): not built.
- `Icon`, `Logo`: check the launcher icon and in-app logo against the boards.

---

## Part B: built but never seen on a device (verify, fix if off)

- `NewUpdate` (debug builds hide the update check: test with a release-type build or a preview hook).
- Widget picker preview (`res/layout/tsz_reading_widget_preview.xml`).
- Menus not opened on the tablet: manga reader top bar, extension details, web view, migration config,
  download queue Cancel all.
- Mass import batch cards; download queue active novel card plain-tile colour.
- Translated layouts (zh-CN, ko, es, pt-BR, fr, de) never seen visually.
- Phone layout (S22 `RZCWA139X1X`): Settings phone layout, Home stack, sheets: phone was disconnected all session.

---

## Part C: features the design shows that the app doesn't have (for a complete match)

Build only with real data; each needs storage and logic, not just UI. Ask the owner which to prioritise.

1. **Anime** (Aniyomi, Apache-2.0): sources/extensions, library, details, episodes, player (mpv), AniSkip,
   tracking, downloads, Watch tab, Continue-watching widget, Airing today strip in Updates. Native builds must be non-GPL.
2. **Source health**: per-source status / speed / last check (Browse dots, Extension detail tiles, More tile).
3. **Find in chapter** (ReaderFind).
4. **Dictionary lookup** (Dictionary sheet).
5. **Highlights and glossary notebook** (Notes board; today only per-book notes).
6. **Chapter recap** at chapter end.
7. **Merge sources** for one book.
8. **Library power search / query builder** (LibraryQuery).
9. **Backup sync providers** (Drive / WebDAV) and "synced N min ago".
10. **Reading estimates**: words, reading speed, time left, size per book/chapter (NovelDetails, Stats words/speed).
11. **Listen stats** and **TTS state** for the Listening widget and Stats.
12. **Storage measurement**: chapters / covers / database sizes (DataTools, Downloads storage tile) and download speed.
13. **Accent colour choice** and **Reduce motion** settings (Settings board rows).
14. **In-app PIN lock** (AppLock): owner parked as future; don't build now.
15. **Generated covers** for books without art (Cover component).
16. **Manhwa classification** (separate from manga) for LibraryMixed / OnboardingContent.
17. **Per-shelf book counts** in Categories, and extension source counts per repository.
18. **Search timing** ("N results · 0.6 s") in Global search.
19. **Support us** screen: owner decision pending (keep Tsundoku links, use owner's, or remove).

## Blocked on the owner (don't start)

- Order 9 merge/build/device checks completed in `c54b2f809`; only old worktree-folder cleanup remains.
- Order 11: release v0.1, signing, making the repo public, and removing/replacing Tsundoku's Firebase config
  (`app/google-services.json`, `-Pinclude-telemetry` in `.github/workflows/release.yml`); `PRIVACY.md` says no telemetry.
- AppLock PIN, Support us screen, Import EPUB "off" switch: owner decisions.

## Suggested order

1. Part B verifications (cheap, catches regressions).
2. Restyle-only boards in Part A with upstream screens behind them, smallest first: CatalogueFilters, AddCategory,
   EditNovel, Duplicate, Tracking/TrackerSearch, Quotes, SettingsReader, ReaderBars, ReaderEngine,
   SettingsTranslation, ExportEpub, ChapterSearch, Upcoming, DuplicateFinder, CustomSource/ElementSelector,
   Migration/MigrationCompare, Onboarding, ReplaceRules, TTS/ListenMini, ReaderImmersive/ReaderPaged.
3. Tablet layouts: TabletUpdates, TabletBrowse, TabletReaderPanel, TabletReader, TabletTTS, Foldable.
4. Manga boards (09-manga).
5. Part C features, in the owner's order; anime last (largest).

---

## Part D: Mihon features Tsuzuki doesn't have (checked 2026-11-04 against `mihon/main` CHANGELOG)

Tsuzuki has every core Mihon feature (library, categories, updates + smart-update restrictions, history, browse,
extensions + repos + trust, global search, migration, downloads, backup + auto-backup, all trackers except Comick,
local source, app lock, incognito, upcoming, stats, notes, excluded scanlators, Shizuku / private installer, deep
links, reader modes incl. webtoon / dual page / crop / colour filters, updates widget). What's missing comes from
Mihon v0.21.x, which Tsundoku hasn't merged. Most of it rests on Mihon refactors Tsundoku didn't take (Compose
reader, new DB schema, `Instant` dates, Material Symbols), so expect hand ports, and coordinate with order 9
(`upstream/mihon-picks`). Verify each item in code before starting; a few are marked "verify".

Features:
1. **High quality reader renderer** (Mihon #3388): better image scaling, page transition animations, dual page view,
   display-cutout handling (Settings → Advanced). Not present.
2. **Reader rewrite**: Compose-drawn reader, viewer out of the ViewModel, shared page chrome (#4193). Not taken.
3. **Time format setting** (#4118): only Date format exists.
4. **Comick tracker** (#4056): not in `data/track/`.
5. **`id:` prefix search** for AniList, Bangumi, Kitsu, MangaUpdates, Shikimori, Hikka (#3776): only MAL and MangaBaka have it.
6. **Kitsu user rating system** (#3818) and **tracker refresh buttons** in Settings (#3828): verify, likely missing.
7. **Allowed content warnings** (Safe / Mixed / 18+) replacing the NSFW toggle (#3951): Tsuzuki still has the NSFW
   toggle; the repo index model already carries `contentWarning`.
8. **Keep loading installed extensions regardless of content warnings** (#3952), **show failed-to-load extensions
   with reason and stack trace** (#3953), **apply trust / repo / warning changes without restart** (#3954),
   **repo name in extension details and list, one entry per signing key** (#3955), **updates for unloaded
   extensions** (2d86a70), **update badge right after launch** (4f33486), **background extension loading** (#3788): verify each.
9. **Repo signing-key enforcement** for installs/updates (0938411): partly (signing data present); verify.
10. **Option to turn off the hash suffix** in downloaded chapter folder names (#3966): not present.
11. **Skip an app update version** (fc20410) and **download app updates from the update screen** (#3707): not present.
12. **Data-migration screen after an app update** (fd4f73c) and the **database schema rework** merging duplicate
    entries / chapters (#3805, plus migrations 6dffca159, a999e49f2, 9dbb7148c, ea2b73ad7): not taken; needs
    Tsundoku migration numbering.
13. **Restore keeps existing details, furthest progress and earliest date added** (c67a33f), **faster / lighter
    backups** (771ba33), **system document picker for restore** (#3948): verify.
14. **Wavy progress ring while a chapter downloads** (e562a9b): not present.
15. **Confirm before unfavouriting** from the manga screen (#4073 / dd4e9f00f): skipped pick; verify UI.
16. **Download the next chapter of a source while the current one finishes** (730e43fd1 and follow-ups): hand-ported
    on `upstream/mihon-picks`, not yet in `main` (order 9).
17. **Library update order: sources with the most entries first** (b8070cd), **online chapter cache kept for a day**,
    **AVIF / JPEG XL / HEIF / JPEG 2000 decoder** (#3786): verify; the app has some AVIF/JXL handling.
18. **Comick progress formatting, Cloudflare `cf-mitigated` detection** (#3842), **expired AniList credentials
    message** (#3888), **MangaUpdates scores in search** (#3795): verify.
19. **Material Symbols icons** (#3873): not taken (cosmetic; Tsuzuki has its own design anyway).

Bug fixes: `docs/UPSTREAM.md` → "Skipped for conflicts" lists ~35 Mihon fix commits not yet in Tsuzuki
(download correctness, WebView / Cloudflare robustness, tablet FAB under taskbar, library range-selection crash,
and more). Hand-port them on `upstream/mihon-picks`, most valuable first, and log each in `docs/UPSTREAM.md`.

---

## Part E: make Mihon / Tsundoku merges easy (after the priority list; one step per session)

Goal: Mihon's own files go back to (nearly) Mihon's version with a few small hooks; novel and Tsuzuki logic
lives in separate files. Then upstream fixes cherry-pick cleanly. Each step on its own branch, build + unit tests,
novel **and** manga checked on the tablet with owner data backed up and restored, logged in `docs/UPSTREAM.md`.

1. (Priority 2) Picks merge, order 9.
2. (Priority 3) Whole-file sync from Mihon for low-divergence code: Cloudflare / WebView interceptors,
   `NetworkHelper`, WebView screens, extension `Installer`, `DownloadProvider`. Keep a list of our re-applied lines.
3. **Reader split**: `NovelReaderActivity` + `NovelReaderViewModel` take every novel branch out of
   `ReaderActivity` (≈1,500 Tsundoku lines) and `ReaderViewModel` (≈935); `ReaderActivity.newIntent` routes by
   book type so callers don't change; shared history / progress / `ReadingDays` / incognito / transitions behind
   small interfaces. Done when both readers work (novel: TextView + WebView renderers, TTS, translation, quotes,
   contents drawer, progress / history; manga: paged + webtoon) and the two manga files diff only by hooks.
   This unlocks Mihon's reader rewrite and high-quality renderer (Part D 1–2).
4. **Library split**: novel / combined-library logic out of `LibraryViewModel` (≈1,680 lines changed).
5. **Book details split**: novel logic out of `MangaViewModel` (≈811) and `MangaScreen` (≈199).
6. **Browse, updates, downloads, settings items**: `BrowseSourceViewModel` (≈720), `UpdatesViewModel` (≈234),
   `Downloader` (finish the engine split), `SettingsItems` (≈301).
7. Re-take the ~35 skipped Mihon fixes (`docs/UPSTREAM.md`); most should now be plain cherry-picks.

Estimated 5–6 sessions in total; the reader split alone may take a full session.
