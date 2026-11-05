# Tsuzuki work plan (architect: Claude · labour: Codex)

Written 2026-11-05 by Claude as the architect. This is the **what and how**. Progress, claims and the
running log live in `docs/HANDOFF.md` (the only handoff file). Rules live in `AGENTS.md`. Design truth lives in
`docs/design/` (boards win over this plan if they disagree; ask the owner if a board is missing).

---

## 0. Operating model

### 0.1 Roles

| Role | Who | Does | Never does |
|---|---|---|---|
| Architect / reviewer | Claude (Opus) | Splits work into task cards, writes prompts, reviews every diff, resolves merge conflicts, makes judgement calls on upstream hooks, **owns the devices** (install, screenshots, data backup/restore), commits after review, updates `HANDOFF.md` | Large mechanical edits it can delegate |
| Implementer | Codex (`gpt-6.1-sol`, default) | Code changes, unit tests, build, per-file Spotless, in its assigned worktree, then a written report | Commit, push, touch devices, edit `AGENTS.md`/`HANDOFF.md`/`WORKPLAN.md` (it reports; Claude records) |
| Fast implementer | Codex `gpt-5.3-codex-spark` or `gpt-5.4-mini` | Mechanical moves, string additions in 8 locales, renames, import fixes, boilerplate tests | Design decisions, upstream conflict resolution |
| Researcher | Claude Explore agent (Haiku for greps/inventories, Sonnet for classification/analysis) | Read-only surveys, file maps, "where does X live" | Edits |
| Second reviewer | Claude `ecc:kotlin-reviewer` / `pr-review-toolkit:silent-failure-hunter` (Sonnet) | Independent review of risky diffs (coroutines, storage, DB, security) | Edits |

**How Codex is run (owner, 2026-11-05): the owner runs Codex in the Codex app.** Claude does not launch Codex
itself. Claude puts ready cards in `docs/HANDOFF.md` → "Codex queue" and tells the owner the one line to paste,
normally: `Read AGENTS.md and docs/HANDOFF.md, then do the next task in the Codex queue.`
Codex then claims the card in `HANDOFF.md` (status `codex-working`, its worktree path), does the labour in that
worktree, writes its report into the card's "Result" field, sets `review`, and stops **without committing**.
Claude reviews, device-tests, commits and moves the card to the log. This is the one place Codex edits
`HANDOFF.md`; it never edits `AGENTS.md` or this file.

### 0.2 Model and effort by task difficulty

| Tier | Typical work | Codex | Claude |
|---|---|---|---|
| T1 trivial | Strings, icon additions, doc tables, renames, moving files with `git mv` | `gpt-5.3-codex-spark`, effort low | Haiku |
| T2 routine | Restyle an upstream screen with existing building blocks, small hooks, unit tests for pure logic | `gpt-6.1-sol` medium (or `gpt-5.4-mini` high) | Sonnet |
| T3 hard | New screens with state, coroutine/storage changes, upstream ports with divergence, performance work | `gpt-6.1-sol` high | Sonnet + Opus review |
| T4 critical | DB migrations, anime engine/player, downloader, backup format, reader architecture, security | `gpt-6.1-sol` xhigh, split into small cards | Opus designs the card and reviews line by line; second reviewer agent |

Rule of thumb: if the card needs judgement about *what* to build, Claude writes it first; if it only needs
*typing*, use the cheapest tier that can compile it.

### 0.3 Parallelism rules (hard limits)

1. **One worktree per concurrent code task.** Create with
   `git -c core.longpaths=true worktree add -b tsuzuki/<task> .worktrees/<task> <base>` and copy `local.properties`.
   Never let two agents edit the same worktree.
2. **Devices are a single resource owned by Claude.** Codex never runs adb. Claude batches device checks.
3. **At most two Gradle builds at once** on this Windows machine (RAM/IO). Prefer one heavy Codex task plus
   read-only agents.
4. **Upstream merges and anything touching shared upstream hook files are serial on `main`**, done by Claude.
5. Tasks that touch the same files are never parallel. File-overlap check before launch:
   `git diff --name-only` of open branches.
6. Owner-run Codex threads follow the same limits: one card per thread, named explicitly when more than one
   thread is open, claimed in `HANDOFF.md` first; Claude marks cards that may run together `parallel-ok`.
7. Cost guard: parallel runs only when the tasks are independent *and* each is ≥ T2. T1 work is batched into one
   cheap run.

### 0.4 Task card format (Claude writes; Codex receives verbatim)

```
ID / title / tier / model+effort
Worktree + base commit + branch
Goal (one paragraph, the user-visible outcome)
Board(s) to open first (PNG both themes + HTML path) — or "no UI"
Files you may edit / files you must not edit
Steps (numbered, concrete)
Real-data rules / what to leave out
Strings (keys, 8 locales)
Tests to add
Verify: exact commands (JDK 21, per-file Spotless twice, assembleDebug, testDebugUnitTest[, assemblePreview])
Report: files changed + why, decisions, leftovers, what Claude must check on device
```

### 0.5 Claude's review checklist (every Codex result)

1. `git diff --stat` matches the card's allowed files; no stray formatter edits in upstream files
   (restore with `git show HEAD:<file> > <file>`).
2. Upstream files: only a dispatch/slot hook, no re-indent; file added to AGENTS "already touched" list.
3. Theme rules: tokens only, typography only, Dot uppercase + red only, stock themes untouched.
4. Real data only; no sample numbers.
5. Coroutines: no blocking on main, cancellation rethrown, no swallowed errors without a log.
6. Strings present in all 8 locales; placeholders identical.
7. Build + tests green locally (rerun if the report is older than the last change).
8. Device check on tablet `R52Y90HA77L` **and** phone `RZCWA139X1X`, Dot then Colour, screenshots read,
   `logcat -b crash` empty, data restored, tablet left on Dot Home.
9. Commit as the owner (no co-author trailers), message says what and why; update `HANDOFF.md`, design status
   table, `RUNTIME_QA.md` as relevant.

### 0.6 Device and data protocol (summary; details in `AGENTS.md` and `HANDOFF.md`)

- Daily app = optimized **preview** build (`…tsuzuki.debug`, not debuggable: no `run-as`). Original debug app
  (`…tsuzuki.dev`) holds the owner's untouched data; don't use it for tests.
- Before a preview test that changes data: export an in-app backup (Settings → Data and storage) and copy it to
  `app/build/<evidence>/`. After: put reading positions, read flags, theme and preferences back by UI, or
  restore the backup.
- Reading in long-strip mode past a chapter end marks it read: undo with Mark as unread.
- Git Bash rewrites `/data/...` paths: prefix adb shell calls with `MSYS_NO_PATHCONV=1` or call adb from Python.
- Never tap system pickers/permission dialogs the owner may be using; ask.

---

## Phase 0 — in progress this session (2026-11-05)

| ID | Task | Tier / agent | Status |
|---|---|---|---|
| P0.1 | Merge 5 new Tsundoku commits (`c26dfc13a`) + overflow icons for the new scroll actions | T2 / Claude | merged; build running |
| P0.2 | `docs/ARCHITECTURE.md`: novel-only / manga-only / shared map with origin (Mihon/Tsundoku/Tsuzuki) | Sonnet research → Claude writes | research running |
| P0.3 | Sort Tsuzuki-owned code: move clearly novel-only / manga-only files under `presentation/tsuzuki/novel/` and `presentation/tsuzuki/manga/`; shared files stay put (no `shared/` dump). One `git mv` commit, imports fixed, AGENTS paths updated | T1 / Codex spark, after P0.2 | todo |
| P0.4 | Reader hook consolidation: our ~44 lines in `ReaderActivity` + 2 in `ReaderViewModel` go behind one Tsuzuki helper (`presentation/tsuzuki/reader/TsuzukiReaderHooks.kt` or similar) called from minimal hooks; Tsundoku's own novel branches stay where they are | T3 / Codex sol high, Claude reviews | todo |
| P0.5 | Onboarding redesign (welcome → languages → look → storage → permissions → import) | T3 / Codex sol high in `.worktrees/onboarding` | running |
| P0.6 | Single shared handoff (`docs/HANDOFF.md`), delete `CODEX_PROMPT.md` / `CLAUDE_HANDOFF.md`, repoint Codex heartbeat | T1 / Claude | in progress |

**Decision recorded (owner, 2026-11-05):** no full novel/manga split (no separate tables, libraries or
activities). Reason: Tsundoku actively changes the novel code (17 commits in 60 days); moving it into Tsuzuki
files would turn every Tsundoku fix into a hand port. Separate only Tsuzuki-owned code and our hooks. This
supersedes old "Part E step 3" (`NovelReaderActivity` split). Revisit only if Tsundoku goes inactive or Mihon's
Compose reader rewrite becomes a priority.

**P0.3 card details.** Input: the ARCHITECTURE map's Tsuzuki section. Move only files classified novel-only or
manga-only *and* not imported by more than one upstream hook file in the other media path. Keep package
declarations and imports consistent; upstream files whose imports change get only the import-line edit. Done
when assembleDebug + all unit tests pass and `git diff -M --stat` shows renames with ≤ 3 changed lines each.

**P0.4 card details.** Start from `git diff $(git merge-base HEAD tsundoku/main) HEAD -- app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt`.
Every Tsuzuki addition (reader transition animation selection, `ReadingDays` recording, contents drawer relaunch,
Tsuzuki chrome) moves into Tsuzuki functions; the upstream files keep one-line calls. Behaviour identical.
Claude verifies on both devices: novel TextView + WebView open/close animation, progress/history saved,
`ReadingDays` minutes increase, contents drawer jump, manga paged + long strip.

---

## Phase 1 — stability on both devices (priority 1)

Source of truth for what's verified: `docs/RUNTIME_QA.md`. Each card = Codex fixes, Claude finds/verifies.
Claude runs the device matrix; Codex receives one bug per card with logcat + screenshot + repro.

| ID | Flow | What to exercise (tablet + phone, Dot + Colour, preview build) | Tier |
|---|---|---|---|
| P1.1 | Rotation + process death | `adb shell am kill` in background on Home, Library, details, novel reader (TextView, WebView), manga reader (paged, strip), chapter filter/download sheets, reader settings sheet, queued downloads | Claude test, T3 fixes |
| P1.2 | Downloads in preview | novel + manga together, per-group pause/resume, force-stop restore, interrupted split image retry, delete after reading | Claude test, T3 fixes |
| P1.3 | Backup round trip | export → restore into a scratch install? No: restore into preview after export, verify books, categories, history, prefs; LNReader import | Claude test |
| P1.4 | EPUB + mass import | owner-approved temporary imports, queue, retry, cleanup | Claude test, T2 fixes |
| P1.5 | Translation | configured provider; safe failure without credentials; never use paid keys silently | Claude test, T3 |
| P1.6 | TTS | other device/theme combos, background playback, headset buttons (new Tsundoku fix 175ec298d), sleep timer | Claude test, T3 |
| P1.7 | Extensions | real install/update/uninstall of one approved source through system and private installer; repo key mismatch | Claude test (owner approval per install) |
| P1.8 | Widget in preview | place Reading widget for preview build (owner places it), verify data, tap | Claude |
| P1.9 | Reader transitions on phone | Dot long strip transition card (tablet passed 2026-11-05) | Claude |
| P1.10 | Separate-library mode, search, empty/error states | both devices | Claude test, T2 fixes |
| P1.11 | Storage async migration | move the synchronous `DownloadProvider`/`DownloadManager` callers (reader loaders, cache, EPUB export, translation) to suspend APIs together, keeping signatures for Tsundoku merges via thin wrappers | T4 / Codex xhigh in a worktree, Claude + kotlin-reviewer review |

## Phase 2 — performance (measured, not guessed)

Reference: `docs/PERFORMANCE.md`. Every claim needs before/after numbers on the preview build.

| ID | Task | Tier |
|---|---|---|
| P2.1 | Phone cold-start repeated measurements (`am start -W`, 10 runs, compiled vs not) | Claude |
| P2.2 | Large-library test: synthetic 500-book library in a scratch preview install (never the owner's) via backup import; Library grid and Home jank (`dumpsys gfxinfo framestats`) | T3 Codex builds the generator script, Claude runs |
| P2.3 | Fix top 3 jank sources found (stable keys, `contentType`, Coil sizes, palette recomposition, dot-grid drawing) | T3 Codex |
| P2.4 | Re-collect baseline profile after P2.3 (`-PtsuzukiProfileConnected=true`, see PERFORMANCE.md) | Claude |

## Phase 3 — upstream (keep Tsundoku and Mihon flowing)

| ID | Task | Tier |
|---|---|---|
| P3.1 | Weekly: `scripts/upstream-report.sh`, merge `tsundoku/main` (Claude, serial on main) | T2 Claude |
| P3.2 | Mihon skipped fixes (~35, `docs/UPSTREAM.md` "Skipped for conflicts"): one card per fix group, most valuable first (downloads, WebView, tablet FAB, range-selection crash) | T3 Codex per card |
| P3.3 | Mihon features (from old Part D, verify each first): time format setting; Comick tracker; `id:` search for more trackers; content warnings replacing NSFW toggle; failed-extension list with reason; repo name per extension; option to drop hash suffix in chapter folder names; skip an app update version; wavy download ring; restore keeps furthest progress | T2–T3 per item |
| P3.4 | Mihon high-quality renderer / Compose reader: **not now** (depends on reader decision above) | — |

## Phase 4 — design completion (boards → app)

Open the board PNG (Colour + Dot) and HTML before each card. Dot first, then Colour, stock themes unchanged.
Restyle-only cards are T2 (Codex sol medium); new-state cards are T3.

**4A. Built but never seen on a device (Claude verifies first, cheap):** NewUpdate screen (needs a release-type
build once the repo is public), widget picker preview, menus not opened (manga reader top bar, extension details,
web view, migration config, download queue Cancel all), mass import batch cards, translated layouts (switch one
device language only with owner OK), phone Settings layout.

**4B. Restyle upstream screens (smallest first):** CatalogueFilters sheet · AddCategory dialog · EditNovel ·
Duplicate dialog · Tracking + TrackerSearch · Quotes · SettingsReader · ReaderBars · ReaderEngine ·
SettingsTranslation · ExportEpub · ChapterSearch · Upcoming · DuplicateFinder · CustomSource + ElementSelector ·
Migration + MigrationCompare · ReplaceRules · TTS + ListenMini · ReaderImmersive + ReaderPaged · vertical reader
progress (still stock) · download queue manga rows in Compose (optional).
Board paths: `docs/design/boards/INDEX.md`. One card each; batch two tiny dialogs per card at most.

**4C. Tablet layouts:** TabletUpdates (list + preview) · TabletBrowse (sources + catalogue) · TabletReaderPanel
(reader + notes) · TabletReader (two-page spread) · TabletTTS · Foldable. T3 each.

**4D. Manga boards (09-manga):** MangaDetails (scanlators, auto webtoon), MangaPaged, MangaReader,
MangaReaderSettings, TabletMangaSpread, BrowseManga, DotManga checks. T2–T3.

**4E. Onboarding content step (09-manga/OnboardingContent):** blocked on owner decision (see HANDOFF open
questions): needs a "media types I use" preference and manhwa classification; anime option appears only after
anime stage 2.

**4F. Brand:** Cover component (generated covers for books without art), launcher icon / logo check.

## Phase 5 — new features the boards show (owner orders these)

Each needs storage + logic, so T3/T4 with a design card from Claude first:
source health (status/speed/last check) · find in chapter · dictionary lookup · highlights + glossary notebook ·
chapter recap · merge sources for one book · library query builder · backup sync (Drive/WebDAV) · reading
estimates (words, speed, time left) · listen stats + TTS state (Listening widget) · storage measurement
(chapters/covers/DB sizes, download speed) · accent colour + reduce-motion settings · generated covers · manhwa
classification · per-shelf counts · search timing. Parked by owner: in-app PIN lock, Support us screen.

**Neural TTS voices (owner idea 2026-11-05, sherpa-onnx + Kokoro). Research done 2026-11-06 → GO-WITH-CONDITIONS, see `docs/research/neural-tts.md`: sherpa-onnx ≥ v2.0.1 no longer contains espeak-ng (PR #4055); use lexicon voices (Kokoro int8 multi-lang v1.1), re-zip without `espeak-ng-data/`, keep system TTS default, and run a listening test on real novel text first (out-of-lexicon words are dropped).** Original note: Optional second engine behind
`TtsController` (system TTS stays default). Gate first (T3 research card, before any code): sherpa-onnx's
Kokoro/VITS English phonemiser uses **espeak-ng (GPL-3.0)** — confirm whether it is compiled into the Android
JNI library and whether an espeak-free build or lexicon-only voice pack exists; if not, don't ship (Apache-2.0,
no GPL). If cleared: Kotlin API (the owner's draft was Flutter/Dart and doesn't apply), model download screen
into app storage (not in the APK; zip on our own release), synthesis on a background thread one ~150–200-char
sentence chunk ahead of playback, reuse `TtsTextUtils` chunking, voice picker (real speaker IDs from the pack),
16 KB alignment check of the new native libs, licence texts in About.

---

## Phase 6 — anime integration (Aniyomi, Apache-2.0) — full plan

### 6.0 Facts and assets (as of 2026-11-05)

- Spike branch `codex/anime-spike` (`.worktrees/anime-spike`), based on old main `33bda2171`. Committed:
  native builder only (`75a1b7c09`, `5c351934b`). **Uncommitted API-port work (31 paths)** backed up to
  `app/build/anime-spike-wip-20261105/` (`tracked.patch`, `untracked.tar`, lists). Contents: 26 Apache-2.0
  `animesource` API files in `source-api`, PreferenceScreen typealias, NanoHTTPD 2.3.1, R8 keep rule, anime
  extension loader + Installed/Untrusted models under `tsuzuki/anime/extension` (private dir `files/anime-exts`,
  reusing host trust/signature checks). `docs/ANIME_SPIKE.md` in the spike has pinned upstream revisions and
  licence findings.
- Native player libs: Actions run `41645408336` succeeded; artifact archived (expires on GitHub ~2026-11-17) at
  `app/build/anime-native-41645408336/` with `SHA256SUMS.txt`. FFmpeg licence file says **LGPL v3 or later**
  (GPL/version3-only/nonfree/postproc off; mpv `-Dgpl=false`); all `.so` 16 KB-aligned per `audit.json`.
  Libraries: libmpv, libplayer (wrapper JNI), libav*, libsw*, libxml2, libc++_shared; headers in `include/mpv`.
- Licence posture: app Apache-2.0; LGPL libraries must stay dynamically linked `.so` with their licence texts
  and source offer (record in `docs/licenses/` + About). No GPL code. Aniyomi app code copied must be Apache-2.0
  files only (check headers).

### 6.1 Stage 1 — play one episode (T4)

| Card | Work | Agent |
|---|---|---|
| A1.1 | Owner decision recorded: commit the spike WIP on `codex/anime-spike` (preserve) — or keep as patch. Then create `tsuzuki/anime` branch from current main in `.worktrees/anime` | Claude |
| A1.2 | Re-port `animesource` API + loader from the patch onto current main (not a rebase of the dirty tree); compile `:source-api` and `:app` | Codex sol high |
| A1.3 | Vendor native libs: new module `anime-player` (Android library) with `jniLibs/arm64-v8a` from the archived artifact, licences, a Gradle check that SHA-256 matches `SHA256SUMS.txt`; ABI split keeps them out of armeabi/x86 APKs or ships arm64-only for anime | Codex sol high |
| A1.4 | Kotlin `MPVLib` wrapper (MIT, from aniyomi-mpv-lib at the pinned revision) + minimal `PlayerActivity` (surface, play/pause, seek, back) using Tsuzuki theme tokens; 16 KB re-audit of the final APK | Codex sol xhigh |
| A1.5 | Debug-only entry (developer screen) to install one owner-approved anime extension APK into the private dir, list its sources, fetch one episode list and play the first video | Codex sol high |
| A1.6 | Device test both phones/tablet: playback, rotation, background/foreground, audio focus, 16 KB, crash log; APK size delta recorded | Claude |
Done when: one real episode plays on both devices from a real extension in a preview build; nothing visible to
normal users yet (Watch tab still placeholder).

### 6.2 Stage 2 — data model and library (T4)

| Card | Work |
|---|---|
| A2.1 | Schema design by Claude: separate `animes`, `episodes`, `anime_history`, `anime_categories`, `anime_tracks` tables (Aniyomi-style), **new SQLDelight migration numbered after Tsundoku's latest**; never alter `mangas`. Write the migration plan + rollback test in this file before code |
| A2.2 | Codex: tables, mappers, repositories, domain models/interactors under `tachiyomi.domain.anime` / `data.anime` |
| A2.3 | Codex: anime source manager registration (installed anime extensions → sources), browse anime catalogue (popular/latest/search) |
| A2.4 | Codex: add to library, Watch tab in Home becomes real (Continue watching, shelf), Library gets Novels / Manga / Anime switch per board `LibraryMixed` |
| A2.5 | Migration test: upgrade the owner's backup copy in a scratch install; verify novels/manga untouched |

### 6.3 Stage 3 — features (T3–T4)

Details/episodes screen · episode progress + history · Updates with episodes (and "Airing today" strip) ·
downloads as a **third engine** beside `NovelDownloadEngine` (video segments/HLS, pause/resume, Wi-Fi only) ·
backups (anime section in proto, Aniyomi backup import) · trackers (AniList/MAL anime lists, Simkl) · AniSkip ·
player settings (decoder, subtitles, audio track, speed, gestures) · Continue-watching widget · Stats watch time.
One card each; Claude writes the design card from the matching board first.

### 6.4 Stage 4 — restyle to anime boards (T2–T3)

Boards (`10-anime`): AnimeHome, AnimeLibrary, AnimeDetails, BrowseAnime, Player, PlayerSettings, PlayerSheets,
AnimeTracking, DotAnime; plus anime parts folded into Main, Browse, Extensions, GlobalSearch, History, Updates,
Downloads, Stats, More, Settings (Player), Backup, TrackerAccounts, Widgets, ExtensionRepos (anime store).

---

## Phase 7 — release v0.1 (owner-gated; don't start alone)

Owner: make repo public (or name a releases repo), create keystore + Actions secrets. Then Claude/Codex: remove
or replace Tsundoku's Firebase config (`app/google-services.json`, `-Pinclude-telemetry` in `release.yml`)
because `PRIVACY.md` promises no telemetry; adapt `release.yml` + signing block to Tsuzuki; `CHANGELOG.md`;
bump `versionName`; tag; verify the in-app updater sees the release (the 404 in logs today is the private repo).

---

## Appendix — card skeleton (Claude writes it into the HANDOFF Codex queue; Codex app executes)

```
--effort <low|medium|high|xhigh>   [--model gpt-5.3-codex-spark for T1]
Work ONLY inside <absolute worktree path> (branch <name>). Do not commit, push, or use adb/devices.
Read AGENTS.md (Rules) first and follow it exactly.
# Task <ID>: <title>
<card body per 0.4>
Verify:
  cd <worktree>; export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
  ./gradlew :app:spotlessApply -PspotlessIdeHook=<abs path>   # each edited Kotlin file, twice, until IS CLEAN
  ./gradlew :app:assembleDebug :app:testDebugUnitTest
Report: files + one line each, decisions, left out + why, test counts, device checks for Claude.
```

## Appendix — standard rules for restyle cards (B, C, … batches)

Every restyle card says "follow the standard restyle rules"; these are they.

- Tier T2 Â· Codex `gpt-6.1-sol` effort medium. All B cards are `parallel-ok` with each other and with P0.3, but
  run **at most two at a time** (Gradle). One card per thread; claim it here first.
- Worktree per card: `git -c core.longpaths=true worktree add -b tsuzuki/<card-id> .worktrees/<card-id> main`
  (lower-case id, e.g. `b1`), copy `local.properties` from the repo root into it, work only there.
- Before coding open the board PNGs **both themes**: `docs/design/boards/png/<page>/<Board>.png` and
  `.../png/<page>/dot/<Board>.png` (if a Dot one exists), plus `docs/design/boards/html/<page>/<Board>.html`
  (numbers are in `renderVals()` at the bottom of the last script) and the matching section of
  `docs/design/specs.md`. Build **Dot first**, then Colour.
- Pattern: new Tsuzuki composable(s) under `app/src/main/java/eu/kanade/presentation/tsuzuki/<area>/`; in the
  upstream file add **only** an early-return dispatch at the top of the composable
  (`if (TsuzukiTheme.tokens.style != TsuzukiStyle.NONE) { Tsuzukiâ€¦(same params); return }`), no re-indent, no
  other upstream edits. Stock themes must look exactly as before. Reuse `TsuzukiTile`, `TsuzukiPill`/`TsuzukiPillRow`,
  `TsuzukiSectionLabel`, `tsuzukiGroupedTile`, `TsuzukiModalBottomSheet`/`TsuzukiAdaptiveSheet`, `tsuzukiClickable`,
  `tsuzukiDialogColor`, `TsuzukiMenuItem`, `tsuzukiCase()`; colours only from `TsuzukiTheme.tokens`; fonts only
  from `MaterialTheme.typography`; Dot = uppercase + red only + greyscale covers.
- Real data only: if the board shows something the app doesn't have, leave it out and list it in Result.
- Strings: reuse existing ones first. New ones as `tsz_<area>_*` in all 8 locales (base, ja, zh-rCN, ko, es,
  pt-rBR, fr, de), appended just before `</resources>` under a comment `<!-- <card-id> -->`, apostrophes escaped.
- Unit-test any pure logic you add (JUnit5 + kotest, under `app/src/test/java/eu/kanade/presentation/tsuzuki/`).
- Verify: per-file `./gradlew :app:spotlessApply -PspotlessIdeHook=<abs path>` twice until IS CLEAN for each
  edited file; `./gradlew :app:assembleDebug :app:testDebugUnitTest`. Don't commit, push or use adb.
- Result: files + one line each, what you left out and why, test counts, what Claude should check on devices
  (which screen, how to reach it). Then status `review`.
