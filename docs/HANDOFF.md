# Tsuzuki handoff — shared by Claude and Codex

The **only** handoff file. Read order for any agent: `AGENTS.md` (rules, build, devices) → this file (state,
queue, log) → `docs/WORKPLAN.md` (the plan and how work is split) → `docs/ARCHITECTURE.md` (where code lives) →
design docs for UI work.

Old handoffs (`docs/CODEX_PROMPT.md`, `docs/CLAUDE_HANDOFF.md`, the AGENTS.md task queue) were folded in here on
2026-11-05. Their full text is in git: `git show 8f7f12e3b:docs/CODEX_PROMPT.md`,
`git show 8f7f12e3b:docs/CLAUDE_HANDOFF.md`, `git show 8f7f12e3b:AGENTS.md`.

## How to use this file

- **Owner → Codex app:** paste `Read AGENTS.md and docs/HANDOFF.md, then do the next task in the Codex queue.`
- **Codex:** take the first card with status `ready`. Set it to `codex-working` with your worktree path and the
  date. Work only in that worktree. When done, fill **Result** (files + one line each, decisions, left out and
  why, test counts, what Claude must check on a device), set `review`, stop. Don't commit, push, or use adb.
  If blocked, set `blocked` and write the question.
- **Claude:** reviews `review` cards (checklist in WORKPLAN §0.5), tests on devices, commits as the owner, moves
  the card to the log, writes the next cards. Claude also owns upstream merges and everything on devices.
- Statuses: `ready` · `codex-working` · `claude-working` · `review` · `blocked` · `done`.
- **Several Codex threads at once (owner may open more than one):** give each thread a card explicitly, e.g.
  `Read AGENTS.md and docs/HANDOFF.md, then do card P0.4 only.` A thread must re-read this file and claim its card
  (status `codex-working` + worktree path) *before* any other work, and must skip any card already claimed.
  Only cards marked `parallel-ok` may run beside another card; never run two cards that list the same files.
  At most two threads build with Gradle at the same time.
- Log entries: newest first, one short paragraph, with commit hashes and evidence paths.

---

## Current state (2026-11-06 evening, Claude — desktop machine)

- Back on the desktop: JDK 21 = Android Studio `jbr` (21.0.10); the Temurin path in older notes is the laptop's.
  Tablet `R52Y90HA77L` + S22 `RZCWA139X1X` connected; owner's phone `R5GL23ZYE1V` is not (it's on the laptop).
- `main` = docs batch Q (`f832df96a`) + this plan. Q1 is being reviewed by Claude now, then merged into main;
  `tsuzuki/q5` gets rebased onto it. Codex runs **Overnight batch Q** below in one thread.
- Old worktree `.worktrees/anime-spike` still exists (untouched).

## Claude is working on (Codex: don't touch these)

- Q1 review + merge into `main`, then rebasing `tsuzuki/q5` onto it (Codex: skip Q5 until it says `ready`).
- Overnight: every ~2 h Claude reviews cards that reached `review`, builds (only when Codex isn't building),
  device-checks on the tablet + S22, merges good cards into `main` locally and pushes. Codex never waits on this.

## Codex queue

### Overnight batch Q — run rules (2026-11-06 night; overrides the batch Q "How to run" and prompt below)

**Prompt the owner pastes into ONE Codex thread:**

```text
You are working on Tsuzuki (Android, Kotlin, Jetpack Compose) overnight, alone. Read AGENTS.md, then
docs/HANDOFF.md section "Overnight batch Q — run rules", and follow it exactly: do the cards in the order of its
table, one at a time, until the table is finished or you run out of usage. Before each card re-read
docs/HANDOFF.md (Claude edits it during the night) and skip any card that is not `ready`. Never stop at a
blocked card: record it and move on to the next one.
```

**Rules (every card):**
1. **Re-read this file** (root checkout `docs/HANDOFF.md`) before each card. Take the next `ready` row of the
   table. Set its card heading status to `codex-working (.worktrees/<id>, 2026-11-06)` — in the **root
   checkout's** `docs/HANDOFF.md`, never in the worktree copy. Only status + Result are yours to edit here.
2. **Worktree from the current `main`** (Claude merges reviewed cards into main during the night, so main
   moves): `git -c core.longpaths=true worktree add -b tsuzuki/<id> .worktrees/<id> main`, copy
   `local.properties`. Then for every card in the row's **Needs** column that is not yet in main
   (`git merge-base --is-ancestor tsuzuki/<need> main` fails), run `git merge --no-edit tsuzuki/<need>` in your
   worktree. A merge conflict there → `git merge --abort`, set the card `blocked` ("needs Claude to merge
   <need> first"), go to the next row.
3. **Before UI work** open the card's boards: `docs/design/boards/fixes-2026-11/README.md` (board → card),
   `png/<Board>.png` + `png/Colour<Board>.png`, numbers in `<Board>.dc.html`, bug shots in `evidence/`. Dot
   first, then Colour. Extra-wave cards name their boards in `docs/design/boards/INDEX.md`.
4. **Build:** JDK 21 exactly as AGENTS.md "Build" says (on this machine `/c/Program Files/Android/Android
   Studio/jbr`). At most **one** Gradle build of yours at a time. Follow AGENTS.md Rules + WORKPLAN appendix
   "standard rules for restyle cards": tokens + `MaterialTheme.typography` only, Dot uppercase + red, real data
   only, strings in all 8 locales, small dispatch hooks in upstream files (list every upstream file you touch
   in Result — Claude adds it to AGENTS.md), new code in `tsuzuki` packages, tests for logic you add.
5. **Done =** per-file `spotlessApply -PspotlessIdeHook=<abs path>` twice on every file you touched (never the
   whole project), `./gradlew :app:assembleDebug :app:testDebugUnitTest` green.
6. **Commit on the card's own branch** (owner approved 2026-11-06 for tonight):
   `git add -A -- . ':!docs/HANDOFF.md' ':!AGENTS.md' ':!docs/WORKPLAN.md'` then
   `git commit -m "<id>: <what and why>"`. Owner is the author; **no Co-Authored-By / Generated-with
   trailers**. Never commit to or merge into `main`, never push, never rebase another card's branch, never use
   adb or devices.
7. **Result** (under the card, root checkout): branch + commit hash, files + one line each, upstream files
   touched, decisions, what you left out and why, test count (`N tests, 0 failures`), exactly what Claude must
   check on the phone/tablet. Then set the card `review` and go straight on to the next row.
8. **Stuck > 45 min or build can't go green** → `blocked` + the exact question/error in Result, commit
   nothing, `git -C .worktrees/<id> reset --hard && git -C .worktrees/<id> clean -fd`, next row.
9. Never touch: release/signing/telemetry files, `app/google-services.json`, `.github/`, other cards'
   worktrees, `.worktrees/anime-spike`, DB schema (`*.sq` / `*.sqm` migrations). Q4's app-start migration is
   data-only (no schema change) and is the one allowed data migration.
10. AGENTS.md / WORKPLAN changes a card asks for (Q12's theme rule): write the exact new text into Result;
    Claude applies it.

**Order (one thread, top to bottom):**

| # | Card | Needs (merge first if not in main) | Why here |
|---|---|---|---|
| 1 | Q2 Novel reader next chapter | — | Only broken-function bug; doesn't overlap Q1, so it starts before Q1 is merged |
| 2 | Q5 Navigation Library tab | Q1 | Continue Codex's own work on `tsuzuki/q5` — **not** a new branch: Claude rebases it onto main and sets it `ready`; use `git -c core.longpaths=true worktree add .worktrees/q5 tsuzuki/q5`. If not `ready` yet, skip and come back after Q4 |
| 3 | Q3 Save chapters number range | — | Details download sheet + chapter-list range select |
| 4 | Q6 Book details header | Q3 | Same details screen files as Q3 |
| 5 | Q4 Two Default shelves | — | Import fix + one-time data migration (Claude runs it on a device only after a backup) |
| 6 | Q7 Browse tabs + source rows | — | Browse header only |
| 7 | Q12 Remove other themes | — | Must land before Q11 (one page-colour list) |
| 8 | Q11 Reader settings redesign | Q12 | Uses Q12's page-colour list; also touches `NovelPage.kt` |
| 9 | Q9 Accessibility + QoL | Q3, Q5 | Range select (Q3) and nav labels (Q5) |
| 10 | Q8 Overflow + large-font sweep | Q2, Q3, Q5, Q6, Q7, Q11 | Touches every screen — last of batch Q |
| 11 | X1 Reader hook consolidation | Q2 | Extra wave |
| 12 | X2 Tablet Updates two-pane | Q5 | Extra wave |
| 13 | X3 Restyle check: ReplaceRules + SettingsTranslation | Q11 | Extra wave |
| 14 | X4 Restyle check: DuplicateFinder + Migration | — | Extra wave |

Q1 is Claude's (review/merge tonight). Q10 was done by Claude (AGENTS.md Build + device table).

### Batch Q — polish, layout overflow, smooth Dot motion, Mihon-style QoL (written 2026-11-06 by Claude)

**Why:** device pass on the owner's phone (Galaxy `R5GL23ZYE1V`, SM-S948Q, 1080×2340 @ 450 dpi ≈ **384×832 dp**,
120 Hz, release build `io.github.manojx2005.tsuzuki`, Dot theme). Evidence screenshots:
`docs/design/boards/fixes-2026-11/evidence/`. Boards for this batch: **`docs/design/boards/fixes-2026-11/README.md`**
(maps board → card). Look at `png/<Board>.png` (Dot) and `png/Colour<Board>.png` first; `<Board>.dc.html` is the
source with exact sizes. Same boards on the owner's canvas https://claude.ai/artifact/Qs9pMeZhphy77fbPiE9cTH (pages
**Dot** and **Colour**). In the app Colour labels are sentence case, not the uppercase drawn on most Colour boards.
Build Dot first, then match Colour. Stock themes go away in Q12; until then leave them unchanged.

**Prompt the owner pastes into Codex (one thread per card; change the card id):**

```text
You are working on Tsuzuki (Android, Kotlin, Jetpack Compose). Read AGENTS.md, then docs/HANDOFF.md, then do
card Q1 only (batch Q). Claim it first: set its status to `codex-working` with your worktree path and today's date.
Before changing UI, open the card's boards in docs/design/boards/fixes-2026-11/ (README.md says which; look at
png/<Board>.png and png/Colour<Board>.png, measurements in <Board>.dc.html) and the bug screenshots in evidence/.
Work only in your worktree (.worktrees/q1, branch tsuzuki/q1 from main). Build with
JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot". Follow AGENTS.md Rules: tokens and
MaterialTheme.typography only, Dot uppercase + red, real data only, strings in all 8 locales, small dispatch
hooks in upstream files, new code in tsuzuki packages. Write tests for the logic you add.
Done means: per-file spotlessApply twice on the files you touched,
./gradlew :app:assembleDebug :app:testDebugUnitTest green, then fill the card's Result (files + one line each,
what you left out and why, test counts, exactly what Claude must check on the phone) and set it to `review`.
Don't commit, push or use adb. If you are stuck for more than 45 minutes, set `blocked` with the question and stop.
```

**How to run:** one card per Codex thread, in order Q1 → Q12 unless a card says `parallel-ok`. Q5 builds on
Q1 (sliding nav indicator): start it after Q1 is merged. Worktree per card:
`git -c core.longpaths=true worktree add -b tsuzuki/<id> .worktrees/<id> main`, copy `local.properties`.
Follow `AGENTS.md` Rules and WORKPLAN "Appendix — standard rules for restyle cards" (early-return dispatch in upstream
files, tokens/typography only, Dot uppercase + red, real data only, strings in all 8 locales, per-file Spotless twice,
`./gradlew :app:assembleDebug :app:testDebugUnitTest` green). **Build JDK:** Android Studio now bundles JDK 25 — use
`JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot"`. Don't commit, push or use adb; fill
**Result** (files + one line each, what you left out and why, test counts, what Claude must check on the phone),
set `review`, stop. Stuck > 45 min → `blocked` with the question.

**Mihon rule of thumb for every card (owner: "don't overwhelm users"):** show the 3–5 actions people use every
time; put the rest in an overflow menu or a "More" sheet; never let a row scroll sideways to hide an action; hide
tiles/labels that have no data (no `N/A`); one primary action per screen; long text uses the body face in sentence
case, Doto only for short headings and numbers.

#### Q1 Smooth motion in Dot (T3) — `claude-working` (Claude reviewing `tsuzuki/q1` @ 1d8fe9074, then merging into main; Codex's Result is in that branch's HANDOFF.md)
Owner: "a lot of animations are laggy and not smooth on Dot." Measured: frames are fine (gfxinfo 1.2 % janky,
p50 5 ms at 120 Hz) — the lag is **designed in**:
1. `theme/tsuzuki/TsuzukiMotion.kt` `stepped()` quantises Dot motion into `DOT_STEPS = 8` jumps (≈ 13–27 visible
   fps). Used by `tsuzukiTween()` (all Dot callers), `TsuzukiModalBottomSheet.kt:41,47` and
   `TsuzukiOnboardingScreen.kt:107`. The owner already rejected this for the reader transition on 2026-11-06
   (`TsuzukiReaderTransition.kt:18`). **Use the smooth curve in Dot everywhere**; keep `stepped()` only for
   genuinely discrete fills (progress dots lighting one by one), and make those step on the dot count, not 8.
   Delete the now-unused `res/anim/tsuzuki_reader_*_dot.xml` if nothing references them.
2. Nav pill (`TsuzukiNav.kt` `NavBarItem`): `animateContentSize()` on every item + background toggled instantly →
   the white pill pops while the whole row re-lays out. Replace with ONE indicator behind fixed-width items that
   slides with `graphicsLayer { translationX }` (spring, no layout per frame). Same for the tablet rail.
3. Dot grid (`TsuzukiComponents.kt` `tsuzukiBackground`): ~2,000 `drawCircle` calls per draw, 22 call sites.
   Draw one 12 dp tile into an `ImageBitmap` once (`drawWithCache`) and fill with an `ImageShader`
   (`TileMode.Repeated`).
4. Audit remaining Dot-only animations (`TsuzukiStartup.kt`, `TsuzukiSharedCover.kt`, `TsuzukiCoverStack.kt`,
   `TsuzukiAdaptiveSheet.kt`, `TsuzukiSegments.kt`, `TsuzukiReaderContents.kt`): animate only
   transform/alpha in `graphicsLayer`, read animated values in draw/layout lambdas, not composition.
Keep Reduce motion behaviour. Tests: `stepped` / motion-spec selection unit tests updated.
Device check (Claude): record `dumpsys gfxinfo framestats` + screen recording for sheet open/close, tab switch,
home stack swipe, book open, before/after.

#### Q2 Novel reader: next chapter always reachable (T3) — `ready`
Owner: "the continue button on the novel reader doesn't go to the next chapter." Found on the phone
(`evidence/reader-menu-overflow.png`): the bottom bar (`presentation/reader/appbars/NovelReaderAppBars.kt` ~l.504)
is a `horizontalScroll` row with `SpaceEvenly`; with 9–10 enabled items it is wider than 384 dp, **Settings is
clipped and Next chapter is off-screen** (uiautomator: Settings ends at x=1057/1080, no Next node). Board
`fixes-2026-11/Main.dc.html`:
- Row 1: fixed **‹ Previous** (48 dp, tile) | progress dots + `19%` / page | fixed **› Next** (48 dp, accent). Never
  scroll. Disabled state when there is no prev/next chapter.
- Row 2: the first **4** enabled actions from the existing configurable bottom-bar list (`BottomBarItem` order) with
  short labels + **MORE** (a Tsuzuki menu/sheet holding the rest: auto-scroll, rotation, scroll to top, edit, TTS
  paragraph controls). PREV/NEXT_CHAPTER items are taken out of row 2 because row 1 owns them.
- Top bar: title and chapter both `maxLines = 1` + ellipsis (now "SWITC" is hard-cut).
- End of chapter (infinite scroll off): replace the hardcoded light-blue `android.widget.Button` in
  `NovelViewer.addNextChapterButton()` (`#ADD8E6`, black stroke) with a themed end card — board
  `ChapterEnd.dc.html` (next chapter number + title from source, downloaded state, NEXT CHAPTER 56 dp, Mark read,
  Contents). The card sits on the reading page, so it takes the **page colours** (ink text, ink-filled button with
  page-coloured label, tile = ink at ~8 % over the page) in Colour, for every page colour incl. Night/Black — never
  the app tile/accent (white/gold on cream is unreadable; see `ColourChapterEnd.dc.html`, its `pageColour` switch).
  Do it through a hook/factory in `presentation/tsuzuki/reader/` so `NovelViewer` keeps a one-line call.
  WebView renderer: same card via its existing JS bridge / `onChapterNavigate` path.
- **Verify the action itself:** write tests around `ReaderActivity.loadNextChapterInternal` /
  `ReaderViewModel.loadNextChapter` for (a) infinite scroll on — `viewerChapters.nextChapter` must follow the
  *visible* chapter (`novelVisibleChapter`), not the first-loaded one; (b) a tap while `isLoadingAdjacentChapter`
  (pending direction replay); (c) WebView paged. If (a) is wrong, fix it.
Stock themes: keep the stock row but add the same fixed prev/next (it's a bug there too) only if the diff stays a
small slot; otherwise Tsuzuki-only and say so.

#### Q3 Save chapters: choose a range by chapter number (T3) — `ready`
Owner: "I can't choose which chapter I want to download properly with entering numbers." Found
(`evidence/save-chapters-slider.png`, `presentation/tsuzuki/details/TsuzukiDownloadSheet.kt`): CUSTOM is a
`RangeSlider` over **list positions** (`rangeStart/rangeEnd` are indices); on 1,229 chapters a 25-chapter range puts
both thumbs on top of each other; no text entry. Board `SaveChapters.dc.html`:
- CUSTOM shows **From chapter / To chapter** numeric fields (`KeyboardType.Number`, IME Done), −/+ steppers (long-press
  repeats), shortcuts **FROM WHERE I AM** (first unread after last read) and **TO LAST**. Values are **chapter
  numbers** (`chapterNumber`, fallback to list order for chapters without a number); decimals (12.5) allowed.
- Inline validation (from > to, out of range, nothing to download) in the accent colour; the CTA reads
  `SAVE CH. 33–57` and is disabled when invalid. Show `N CHAPTERS · M ALREADY SAVED · SKIPPED`.
- Keep the slider out (remove it) — Mihon uses a number dialog for custom amounts; the slider is unusable on long books.
- Move the two app-wide switches (Only on Wi-Fi, Delete after reading) out of the per-book sheet into a
  `DOWNLOAD SETTINGS ›` row that opens the download settings screen.
- Explain/fix the count mismatch: header shows `1432 chapters`, sheet shows `ALL 1229`, `UNREAD 1198`. Label what
  ALL means (not yet saved?) or make it match; same numbers on details and sheet.
- Pure helper `chaptersInNumberRange(chapters, from, to)` + tests (decimals, missing numbers, reversed input,
  duplicates across scanlators).
Also confirm Mihon-style range selection still works in the chapter list (long-press one chapter, long-press
another → selects everything between → download from the action bar); if not wired in the Tsuzuki list, wire it.

#### Q4 Two "Default" shelves in the library (T3) — `ready`
Found (`evidence/library-two-defaults.png`): shelf pills `DEFAULT · DEFAULT · LOCAL`. The first is the system
category (id 0, manga); the second is a **real category named "Default"** holding the 4 novels, created by the
LNReader backup import (`LNReaderBackupImporter.kt` ~l.204 maps every LN category, including LNReader's own
"Default", to a new `BackupCategory`; `CategoriesRestorer` then creates it).
- Import: map LNReader's default category (its id 1 / name "Default", case-insensitive, also the localized
  `default_category` string) to the system category instead of creating one.
- One-time fix for existing installs (app-start migration in `tsuzuki/`, guarded by a preference flag): if a
  non-system category's trimmed name equals "Default" (or the localized default name) **and** it has content type
  novel or all, move its memberships to the system category and delete it. Unit/SQLite test with the in-memory fixture
  (memberships preserved, other categories untouched, runs once). Note it in the HANDOFF log; Claude will run it on
  the phone only after an in-app backup.
- Display: if two pills would still render the same label, never show duplicates (append the content type).
- Pills show their book count (N12 already has the query).

#### Q5 Navigation: Library tab + real labels (T3) — `blocked` (waiting for Claude: rebase `tsuzuki/q5` @ 4345fc994 onto main after Q1 merges, then Claude sets `ready`; Codex continues on that branch) (owner approved 2026-11-06)
Mihon's five tabs are Library · Updates · History · Browse · More; in Tsuzuki the library is only reachable from
Home's "ALL BOOKS →" (no nav item) and Dot labels are cryptic (`NEW / PAST / FIND / •••`). Board `Library.dc.html` /
`Browse.dc.html`: **HOME · LIBRARY · UPDATES · BROWSE · MORE** (History moves into More + Home's recent row).
Uses Q1's sliding indicator. Tablet rail gets the same items. TalkBack labels unchanged words.
- History stays reachable: a HISTORY tile/row in More (top group) and Home's recent row opens it; deep links and
  shortcuts that targeted the History tab still land on History. Library tab opens the existing library screen
  (the one behind Home's ALL BOOKS →), keeps its state across tab switches, and Home's ALL BOOKS → selects the tab.
- Badges: Updates keeps its unread badge. Back from any tab returns to Home (as now).
- Tests: nav item list per layout, History routing. Device check (Claude): phone pill + tablet rail, Dot + Colour.

#### Q6 Book details header (T2) — `ready`
`evidence/details-header.png`, board `Details.dc.html`:
- Toolbar: back · download · filter · overflow. Move scroll-to-top / next-unread / scroll-to-bottom into the overflow
  (Tsundoku added them; keep the actions, Mihon shows only download/filter/more).
- Hide tiles without data: the hourglass `N/A` tile; chapter rows show no `N/A` date line when the date is unknown.
- One Resume: the big Resume tile stays; the floating `Resume` FAB over the chapter list only appears after the tile
  scrolls off screen (it currently overlaps chapter 2 while the tile is visible).
- Genre chips wrap to max two lines (+N chip) instead of being clipped at the card edge ("Myster…").
- Dot case consistency: tile labels uppercase via tokens ("In library", "Tracking", "WebView" are mixed case);
  summary/author stay sentence case in the body face. Summary expand chevron must not overlap text.

#### Q7 Browse tabs + source rows (T2) — `ready` `parallel-ok`
`evidence/tabs-updates-history-browse-more.png` (3rd panel), board `Browse.dc.html`: the 4 pills
(NOVEL SOURCES · MANGA SOURCES · NOVEL EXT… · …) run off screen. Use media pills (NOVELS | MANGA) + a 2-item
segmented control (SOURCES | EXTENSIONS) — the existing pager keeps its 4 pages, only the header changes. Source rows:
name gets the full width (no "LightNovelP…" at 384 dp), LATEST becomes a compact outline chip, pin stays 48 dp.

#### Q8 Overflow and large-font sweep (T3) — `ready`
Every Tsuzuki screen at **360 dp width** and **font scale 1.0 / 1.3 / 2.0**, Dot + Colour, EN + JA. Known on the
phone now: More "DATA AND STORA…", reader title hard-cut, genre chips, Browse pills, source names, Home hero title
(`LORD OF / THE / MYSTERIES` 3 lines pushes the stack), shelf pills. Add `@Preview` groups
(`widthDp = 360, fontScale = 1.3f/2f`) for each Tsuzuki screen and fix: no clipped text without ellipsis, rows that
wrap instead of scroll, min 48 dp targets, no fixed heights around text. List every fix in Result.

#### Q9 Accessibility + quality of life, Mihon parity (T3) — `ready`
- Nav and tab labels are spoken as real words (already true for content descriptions — keep it when Q5 lands).
- Contrast: check every muted/disabled colour on tile/background (e.g. `#8A8A8A` on `#0D0D0D`, dim read-chapter rows
  at `alpha .55`) ≥ 4.5:1 for text; fix tokens, not screens.
- Undo instead of confirm where Mihon does (remove from library, delete downloads, mark read): snackbar with UNDO.
- Long-press anywhere a list has selection (library, chapters, updates, history) and a bottom action bar with
  labelled actions; range select by long-press (see Q3).
- Settings: hide rarely used Tsundoku power options behind an "Advanced" group in each settings screen (no deletes).
- Updates: show a skeleton/last-update list instead of a bare spinner while 1,871 rows load (took > 2.5 s on the phone);
  profile the load and move mapping off the main thread if needed.
Each item is optional if it turns out already done — say so in Result.

#### Q11 Reader settings redesign: quick sheet + All reader settings (T4) — `ready`
Owner: "redesign the reader settings … the all settings panel too." Boards (Dot + Colour pages of the canvas;
offline `fixes-2026-11/ReaderQuick`, `ReaderAll`, `ReaderAllPage` and their `Colour*` twins). Found on the phone:
- Quick sheet (`presentation/tsuzuki/novel/TsuzukiReaderSettingsSheet.kt`): 6 page-colour circles overflow 384 dp
  (BLACK clipped) and have no visible names; tabs LOOK/LAYOUT/CONTROLS; size `13` and lines `1.1` without units.
- All settings (`presentation/reader/settings/NovelPage.kt`, 2,044 lines, Tsundoku's stock sheet opened from
  "ALL SETTINGS"): five **unlabelled icon tabs**, labels wrap mid-word ("Rende / ring / mode"), line height shows
  `11` here vs `1.1` in the quick sheet, four separate margin steppers, ~30 options, and **three overlapping colour
  lists** (Theme App/Light/Dark/Sepia/Black/Grey/Custom + Font colour ×9 + Background colour ×9) that fight the
  quick sheet's Book/Paper/Sepia/Sage/Night/Black.
Do:
1. Quick sheet → board `ReaderQuick`: tabs **TEXT · PAGE · CONTROLS**; page colours in one 6-column grid with names
   under each swatch (fits 360 dp); TEXT SIZE / LINE SPACING with the same unit formatting as the full screen
   (one formatter, e.g. `1.1×`); RESET (this reader's defaults) + `ALL READER SETTINGS ›`.
2. All reader settings → a **Tsuzuki full-screen page** (not a sheet) in `presentation/tsuzuki/novel/`, opened from
   the quick sheet in Tsuzuki themes; stock themes keep `NovelPage`. Labelled tabs
   **TEXT · PAGE · CONTROLS · LISTEN · MORE** mapping the existing tabs (Reading/Appearance/Controls/TTS/Advanced).
   Labels sit above controls (never beside a wrapping label); every row min 56 dp; rare options fold into a
   **FINE-TUNE** expander (paragraph indent/spacing, individual top/bottom margins, highlight style…).
   PAGE tab: one page-colour list (Q12) + CUSTOM swatch that opens the existing text/background colour pickers;
   margins as SIDE MARGINS + TOP / BOTTOM (writes left+right / top+bottom; separate values still editable in
   FINE-TUNE). Live preview strip on TEXT. Every control writes the **same `ReaderPreferences` keys** — no new prefs
   except what Q12 needs; no option is deleted, only regrouped.
3. Same treatment for the manga reader settings dialog tabs (`ReaderSettingsDialog.kt`, N4's Tsuzuki pages): labelled
   tabs, no mid-word wraps. Small.
Tests: pure mapping of tab → preference groups, unit formatter, margin linking. Strings in 8 locales.

#### Q12 Remove the themes we didn't make (T3) — `ready`
Owner: "remove themes that we did not make" (2026-11-06). **This overrides the AGENTS.md rule "Legacy themes must
keep working"** — update that rule in the same card. Board `Themes.dc.html`.
- **App themes:** Settings › Appearance offers only **Colour** and **Dot** (+ light/dark/system mode, Colour accent
  swatches, Dot covers greyscale/in colour). Hide DEFAULT, MONET, CATPPUCCIN, GREEN_APPLE, LAVENDER,
  MIDNIGHT_DUSK, NORD, STRAWBERRY_DAIQUIRI, TAKO, TEALTURQUOISE, TIDAL_WAVE, YINYANG, YOTSUBA, TOKYONIGHT,
  MONOCHROME and the hidden DARK_BLUE/HOT_PINK/BLUE from `AppThemePreferenceWidget.kt` / `AppThemesList`. On
  start, a stored legacy value maps to **COLOUR** (one-time migration, also when restoring an old backup). Keep the
  enum entries and colour-scheme files for now (Tsundoku merges + backups stay readable) — just unreachable; list
  them for a later deletion card. Remove the stock-UI branches only where trivial; don't rip out `TsuzukiStyle.NONE`.
- **Novel reader themes (Tsundoku's):** `NovelPage.kt` `novelThemes` (app/light/dark/sepia/black/grey/custom) and
  the Font colour / Background colour lists disappear from Tsuzuki UI; the only list is ours (Book/Paper/Sepia/Sage/
  Night/Black + Custom). Map stored `pref_novel_theme`: light→paper, dark→night, grey→night, sepia→sepia,
  black→black, app→book, custom stays custom (font/background colour prefs kept for Custom).
- Tests: theme migration (every legacy value → COLOUR; COLOUR/DOT untouched), novel theme mapping, backup restore of
  a legacy theme value. Device check (Claude): Appearance shows two cards; switching works; reader colours intact.

#### Q10 Docs: JDK 21 path (T1) — `done` (Claude 2026-11-06: AGENTS.md Build picks jbr 21 or Temurin 21; owner phone in device table)
`AGENTS.md` Build section still says JDK 21 is Android Studio's `jbr`; Android Studio 2026.2 ships JDK 25. Point to
Temurin 21 (`C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot`) and note the owner's phone serial
`R5GL23ZYE1V` (release app `io.github.manojx2005.tsuzuki`) in the device table. Docs only.


### Extra wave X (owner approved 2026-11-06; only after the batch Q rows above)

General: each X card first checks whether the work already exists (grep `presentation/tsuzuki/`, the status
table in `docs/design/README.md`). If it is already done, set `done` with a one-line Result saying where, and
move on.

#### X1 Reader hook consolidation (T3) — `ready`
WORKPLAN P0.4. Start from `git diff $(git merge-base HEAD tsundoku/main) HEAD -- app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt`
(includes Q2's additions once merged). Move every Tsuzuki addition (reader transition animation selection,
`ReadingDays` recording, contents drawer relaunch, Tsuzuki chrome, Q2's next-chapter hooks) into
`presentation/tsuzuki/reader/TsuzukiReaderHooks.kt` (or a few small files there); the upstream files keep
one-line calls. **Behaviour identical**: no logic changes, no re-indent of Tsundoku code; Tsundoku's own novel
branches stay where they are. Tests for any pure helper you extract. Result lists each moved block
(upstream line → new function) so Claude can verify on devices: novel TextView + WebView open/close animation,
progress/history saved, `ReadingDays` minutes increase, contents drawer jump, manga paged + long strip.
Result: _pending_

#### X2 Tablet Updates: list + chapter preview (T3) — `ready`
Board `docs/design/boards/png/11-tablet/TabletUpdates.png` (+ `dot/`), HTML `html/11-tablet/TabletUpdates.html`.
On windows ≥ 900 dp wide (same breakpoint helper as N9 Tablet Browse / N25 side panel — reuse it), Updates
becomes two panes: the existing Tsuzuki updates list on the left, the selected chapter's book card on the right
(cover, title, chapter number/title, date, downloaded state, actions READ / DOWNLOAD / OPEN BOOK). Phone layout
unchanged. Selection survives rotation (`rememberSaveable` chapter id). Real data only: no excerpt unless it is
cheap and real — otherwise leave it out and say so. Upstream `UpdatesScreen.kt` keeps one dispatch.
Tests: pane/selection logic.
Result: _pending_

#### X3 Restyle check: ReplaceRules + SettingsTranslation (T2) — `ready`
Boards `png/06-reader/ReplaceRules.png`, `png/08-tools/SettingsTranslation.png` (+ `dot/`, HTML alongside).
Find the screens (Tsundoku text replacement rules, translation engine settings). If they are stock in Tsuzuki
themes, restyle with `PreferenceScreen` / `tsuzukiGroupedTile` / `TsuzukiPill` per the boards (dispatch at the
top of the upstream composable). Never show an API key in clear text; add no network calls. Leave out any board
stat without backing data.
Result: _pending_

#### X4 Restyle check: DuplicateFinder + Migration (T2) — `ready`
Boards `png/08-tools/DuplicateFinder.png`, `png/04-browse/Migration.png`, `png/04-browse/MigrationCompare.png`
(+ `dot/`). `presentation/tsuzuki/migration/` exists: compare it to the boards first and fix only what differs
(list the differences in Result). DuplicateFinder: restyle if stock. No change to migration logic.
Result: _pending_

### Overnight batch N (2026-11-05 → 06:00 2026-11-06) — run in ONE Codex thread, in order

**How to run the batch (overrides the usual rules where they differ):**
- Do the cards **in order N1 → N32** (wave 1 N1–N8, wave 2 N9–N20, wave 3 N21–N32), one at a time. Before each card re-read this file; skip any card not `ready`.
- Worktree per card from the current `main`:
  `git -c core.longpaths=true worktree add -b tsuzuki/<id> .worktrees/<id> main` (lower-case id), copy
  `local.properties` from the repo root. If a card touches files an **earlier card of this batch** already changed,
  base it on that card's branch instead (`... .worktrees/<id> tsuzuki/<earlier-id>`) and say so in Result.
- **Overnight exception: commit your finished work on the card's own branch** (`git add -A app i18n` + a clear
  message; owner is the author, no co-author trailers). Never commit to `main`, never merge, never push.
- Follow `AGENTS.md` Rules and `docs/WORKPLAN.md` → "Appendix — standard rules for restyle cards" (early-return
  dispatch in upstream files, tokens/typography only, Dot uppercase + red, real data only, 8 locales, tests,
  per-file Spotless twice, `./gradlew :app:assembleDebug :app:testDebugUnitTest` green). No adb/devices.
- Card status: `codex-working` when you start, then `review` with **Result** filled (branch + commit hash, files,
  what you left out and why, test counts, what Claude must check on a device). If stuck > ~45 min or a build
  can't be made green, set `blocked` with the reason, `git checkout` the card's worktree clean, and move on.
- At most one Gradle build at a time. Don't touch devices, `main`, release/signing/telemetry files.

#### N1 Mihon fixes, group A (T3) — `done` (merged overnight)
Port from remote `mihon` (already fetched; `git show <sha>`): 1e471a414 (updates show finished chapter as still
downloading), 287cbbb98 (keep last update time on empty Updates), 8457fb91e (chapter refresh with non-ASCII
digits), c9e1b6e96 (invalid extension filters instead of crashing), 4c88f0264 (SearchViewModel race),
d3f10772f (local source in global search), e9234f205 (settings search matches group headings). Also check
07b9fb695, ae749bb4f, cdc7d69c9 (installer) — probably already in via `2c7bcae9a`; mark "already applied" if so.
Per commit: already applied / `git cherry-pick -x -n` / hand-port preserving Tsundoku novel paths and Tsuzuki
hooks / skip with reason. Add tests where the logic is pure. Log each in `docs/UPSTREAM.md` (new dated section).
Result: `tsuzuki/n1` at `96e8b11b0` (base `main`). Per-file Spotless twice, debug assembly and all 730 app unit tests passed. Logs: `.worktrees/n1/app/build/n1-verification.log` and `.log.build`.

- `UpdatesViewModel.kt` ignores stale events for downloads that left the queue. `TsuzukiUpdatesTwoPane.kt` keeps the last update timestamp when the tablet list is empty. The phone already does this.
- `SourceFilterDialog.kt` treats unknown tri-state values as disabled. The stock and Tsuzuki select widgets already handle invalid indices safely.
- `SearchViewModel.kt` atomically updates source results and includes enabled local manga and novel sources regardless of language filters.
- `SettingsSearchScreen.kt` matches group headings. `PreferenceScreen.kt` scrolls to and highlights the group in both layouts.
- `Installer.kt` synchronizes queue access and cancels only the matching entry. `ExtensionLoader.kt` stages replacements so copy or rename failure preserves the installed APK.
- `docs/UPSTREAM.md` records all ten dispositions. Skipped the chapter formatter fix because Tsundoku parses the numeric regex match directly. The three installer patches still needed ports after the earlier installer sync.

Device review: Finish a chapter download from Updates, clear Updates on tablet, search a local manga and novel with the Other language disabled, open an invalid source filter, and jump to a setting group from search. In an approved extension test, cancel one of two installs and verify a failed replacement leaves the original installed.

#### N2 Mihon fixes, group B (T3) — `done` (merged overnight)
Same method for: b5856135c (webtoon scroll stutter), a42428284 (covers reloading when removing from library),
0864ce0f4 (redact sensitive headers in logcat), 5910a51eb (keep uninstalled source names across restores),
0c1be369b (ask to add to library before editing cover), 40f552981 (blank title duplicates), c0c090999 (Kitsu
novels in search), ee5939454 (tracker start date UTC shift), dd4e9f00f (confirm unfavourite on manga screen —
must also work with the Tsuzuki details screen; keep the dialog themed via existing Tsuzuki dialog colours).
Result: `tsuzuki/n2` at `ae1c4a430`, based on `tsuzuki/n1` for the shared upstream log. Per-file Spotless twice, debug assembly and all 731 app unit tests passed. SQLite checks passed for blank, whitespace-only, exact and alternate title cases. Validated both redaction strings in all eight locales. Logs: `.worktrees/n2/app/build/overnight-verification.log` and `.build`.

- `MangaCoverFetcher.kt` and `MangaCoverKeyer.kt` use custom covers only for favourites. `MangaExtensions.kt`, `MangaViewModel.kt` and `BrowseSourceViewModel.kt` remove cached covers without bumping the cover key.
- `NetworkHelper.kt` and `NetworkPreferences.kt` redact Auth and Authorization by default. `SettingsAdvancedScreen.kt` adds the switch with two strings across eight `strings.xml` files.
- `BackupFileValidator.kt`, `BackupSource.kt` and `BackupRestorer.kt` preserve meaningful missing-source names and novel classification. `BackupSourceNameTest.kt` covers names and legacy placeholders.
- `MangaScreenConstants.kt`, `MangaCoverDialog.kt`, `MangaCoverViewModel.kt` and `MangaScreen.kt` reuse the existing add-to-library warning for cover editing.
- `mangas.sq` guards primary-title matching for blanks while preserving alternate title matches. `AddTracks.kt` keeps the history instant for the tracker start date.
- `MangaScreen.kt` and `MangaViewModel.kt` confirm removal through the shared details callback. `BrowseSourceDialogs.kt` applies Tsuzuki dialog colours.
- `docs/UPSTREAM.md` records all nine dispositions. Kitsu already filters out novels. Skipped Mihon's webtoon strip renderer because Tsundoku uses a different native decoder API with crop and display-profile hooks.

Device review: Check remove/cancel and cover editing on stock and Tsuzuki details, confirm ordinary covers stay stable when removing a favourite, round-trip a backup with an uninstalled novel source, check first-read tracker dates across a date boundary, and verify redaction with synthetic headers.

#### N3 Novel reader immersive + paged chrome (T3) — `done` (merged overnight)
Boards `06-reader/ReaderImmersive`, `06-reader/ReaderPaged` (PNG both themes + HTML). Restyle the existing
immersive (bars hidden, text selection toolbar if a composable exists) and WebView paged-mode chrome (page
indicator / page footer / any paged-mode controls) in `presentation/reader/**` via dispatches; new code in
`presentation/tsuzuki/novel/`. Don't touch viewer internals (`ui/reader/viewer/text/**`). Real data only.
Result: `tsuzuki/n3` at `6446abef0`, from current `main` (`159eafd73`). Per-file Spotless twice, debug assembly and all 731 app unit tests passed. Logs: `.worktrees/n3/app/build/overnight-verification.log` and `.build`.

- `NovelStatusBar.kt` dispatches to the themed wrapper while reusing the existing configurable clock, battery, chapter, progress, and collapse row. The Dot page label uses display typography and the themed toggle has a larger touch target.
- `TsuzukiNovelStatusBar.kt` draws real scroll or paged progress using the board's 26 dp insets and 3/4 dp track. Paged Colour uses the reader foreground for contrast against its configurable paper colour.
- `TsuzukiComponents.kt` lets this track spread dots across its width. `NovelPageNumberTest.kt` verifies the page bounds and single-page chapters.
- Left out the estimated minutes, menu hint, and separate running title header. Status-bar content and placement follow existing user preferences. The existing menu already provides the seek control and book title. Text selection is Android action mode in both renderers, outside this card's viewer boundary. No new strings.

Device review: In Dot and Colour open a novel, enable the status bar, then hide the menu. Check the footer in both continuous and WebView paged mode, rotate, change pages and chapters, move the status bar to the top, collapse and expand it, and check text selection still works. Smoke-test the stock status bar.

#### N4 Manga reader chrome (T3) — `done` (merged overnight)
Boards `09-manga/MangaReader`, `09-manga/MangaPaged`, `09-manga/MangaReaderSettings`. The manga reader top bar
already uses `TsuzukiReaderTitle` and the Tsuzuki slider; restyle the rest of the manga chrome in
`presentation/reader/appbars/{ReaderAppBars,ReaderBottomBar,ReaderTopBar}.kt`, page indicator, and the manga
reader settings dialog pages (reading mode / general / colour filter) via dispatches, new code in
`presentation/tsuzuki/manga/`. Keep every action. If N3 touched the same files, base on `tsuzuki/n3`.
Result: `tsuzuki/n4` at `26f678af0`, based on current `main` (`56fc27d2f`). N3 changed separate files. Per-file Spotless twice, debug assembly and all 731 app unit tests passed. Logs: `.worktrees/n4/app/build/overnight-verification.log` and `.build`.

- `ReaderAppBars.kt`, `ReaderTopBar.kt`, `ReaderBottomBar.kt` and `TsuzukiMangaReaderChrome.kt` render the themed header and combine the seek and action rows in a floating footer. Header actions, chapter navigation, mode, rotation, crop, and settings retain their callbacks.
- `ChapterNavigator.kt` styles the horizontal navigator in the footer and retains RTL seeking. `ReaderPageIndicator.kt` uses the themed counter when menus are hidden.
- `ReaderSettingsDialog.kt` supplies themed controls to the three manga settings pages. `ReadingModePage.kt` shows all six series modes as tiles. `GeneralSettingsPage.kt` and `ColorFilterPage.kt` share the controls in `TsuzukiMangaReaderSettings.kt`, preserving toggles, choices, sliders, and live preview.
- Reused all labels. Left the manga mode shortcuts in the existing mode dialog and tap-zone controls in settings. Page-level sharing remains in the existing page action menu. No series-type or scanlator label was added to the reader header.

Device review: On phone and tablet in Dot and Colour, open RTL, LTR and Webtoon readers. Check seeking, chapter changes, all header/footer actions, the hidden-menu indicator, switching settings tabs and live filter preview. Check the stock reader and novel settings for regressions.

#### N5 Manga details (T2) — `done` (merged overnight)
Board `09-manga/MangaDetails` (scanlators, auto webtoon). Details already has Tsuzuki header/tiles/resume; add
the board's manga-specific parts that have real data (scanlator filter entry → existing excluded-scanlators
dialog, reading-mode/webtoon indicator from the book's viewer flags) and restyle the scanlator dialog. Hooks in
`presentation/manga/**` only as dispatches.
Result: `tsuzuki/n5` at `bebf5b200`, based on `tsuzuki/n2` for the shared details screen. Per-file Spotless twice, debug assembly and all 731 app unit tests passed. Logs: `.worktrees/n5/app/build/overnight-verification.log` and `.build`.

- `presentation/manga/MangaScreen.kt` adds the mode tile and a direct scanlator entry to phone and tablet details without changing chapter list indices. `ui/manga/MangaScreen.kt` passes a callback to the existing dialog state.
- `ScanlatorFilterDialog.kt` dispatches Tsuzuki themes to the adaptive sheet. `TsuzukiMangaDetails.kt` renders the mode, exclusion count, and scanlator rows with the existing select all, reset, cancel and apply actions.
- The mode tile uses series flags and the configured reader default. It omits the board's art-detection claim and remains read-only. The chapter header uses a single filter entry to open the multi-select dialog. Reused existing localized strings.

Device review: On phone and tablet in Dot and Colour, open manga details with several scanlators. Check applying and clearing exclusions, cancel, the count, and the change in the chapter list. Verify Webtoon, an explicit paged mode and Default, then check stock and novel details.

#### N6 Vertical reader progress (T2) — `done` (merged overnight)
"Vertical progress presentation remains stock" (`docs/RUNTIME_QA.md`). The vertical chapter navigator
(`presentation/reader/components/ChapterNavigator.kt`, vertical variant) should use the Tsuzuki slider look
(Dot dots / Colour thin track, 48 dp touch area, same semantics) like the horizontal one already does
(`presentation/tsuzuki/reader/TsuzukiReaderSlider.kt`). If N3/N4 touched these files, base on that branch.
Result: `tsuzuki/n6` at `c26770738`, based on `tsuzuki/n4` for the shared chapter navigator. Per-file Spotless twice, debug assembly and all 731 app unit tests passed. Logs: `.worktrees/n6/app/build/overnight-verification.log` and `.build`.

- `ChapterNavigator.kt` connects the existing Material vertical slider to the themed track and reserves a 48 dp touch width. Chapter buttons, labels, seeking callbacks and accessibility stay on the existing path.
- `TsuzukiReaderSlider.kt` measures and rotates the existing Dot/Colour progress artwork for top-to-bottom movement independent of left/right placement. Stock themes still use the Material track and thumb. No new strings.

Device review: Enable the vertical navigator on the left and right in Dot and Colour. Tap and drag near both ends, check label and track alignment, use an accessibility increment, and verify horizontal/stock sliders still work.

#### N7 Settings screens vs boards (T2) — `done` (merged overnight)
Boards `07-system/SettingsReader`, `08-tools/ReaderBars`, `08-tools/ReaderEngine`, `08-tools/SettingsTranslation`.
These are `PreferenceScreen`-based and already get Tsuzuki tiles. Compare each board with the current screen
(`SettingsReaderScreen.kt`, `SettingsNovelReaderScreen.kt`, `StatusBarElementsScreen.kt`,
`SettingsTranslationScreen.kt`) and fix the differences that are presentation-only (grouping/section labels,
value labels in accent, segmented choices as pills via existing preference widgets/Tsuzuki widgets). List the
rest as left out. Note: `SettingsNovelReaderScreen.kt` has an old upstream lint error — don't "fix" unrelated code.
Result: `tsuzuki/n7` at `903b72b27`, based on current `main` (`e5ed33e07`). Per-file Spotless twice, debug assembly and all 732 app unit tests passed. Logs: `.worktrees/n7/app/build/overnight-verification.log` and `.build`.

- `SettingsReaderScreen.kt` groups the leading reader controls in a Reading mode tile. Existing list rows continue to show their selected values in accent.
- `Preference.kt`, `PreferenceItem.kt` and `TsuzukiSettingsPane.kt` support wrapping and segmented choices. The three novel reader choices are mapped by preference key in `TsuzukiReaderStatusBarSettings.kt` so `SettingsNovelReaderScreen.kt` and its pre-existing lint issue stay outside this diff.
- `StatusBarElementsScreen.kt` reuses its ordering and persistence with the themed header, grouped rows and switches in `TsuzukiReaderStatusBarSettings.kt`.
- `SettingsTranslationScreen.kt` uses wrapping pills for the real installed engine options when translation is enabled. Existing config and test actions stay in their own groups. Reused localized resources.
- Left out the status bar preview with sample chapter data, toolbar button placement controls, new reader thresholds and estimates, combined novel font/size, and inline translation credentials. Existing controls and preferences keep their behavior.

Device review: In Dot and Colour check reader settings, novel renderer and bar position/size, drag/reorder/reset of status-bar items, and translation engine selection without invoking a paid test. Check a legacy theme for stock controls.

#### N8 Time format setting (T2) — `done` (merged overnight)
Port Mihon #4118 (time format setting next to the existing Date format in Appearance; find the commit with
`git log mihon/main --grep "time format" -i`). Apply it where upstream applies it (relative times / timestamps),
keep Tsuzuki value labels (`tsuzukiValueLabel`) working, strings in 8 locales.
Result: `tsuzuki/n8` at `b22c05a3a`, based on `tsuzuki/n2` for the eight shared locale files. Hand-port of Mihon `970b12f1b` (#4118). Per-file Spotless twice, debug assembly and all 734 app unit tests passed. Logs: `.worktrees/n8/app/build/overnight-verification.log` and `.build`.

- `UiPreferences.kt` and `TimeFormat.kt` add Follow language (default), Follow system, 24-hour and 12-hour with locale-aware formatting.
- `SettingsAppearanceScreen.kt` adds Time format beside Date format with live examples and the existing accent value label. The five new keys are present in all eight requested `strings.xml` files.
- `TwentyFourHourSystem.kt` observes the system format on resume. `HistoryScreen.kt`, `HistoryItem.kt` and `TsuzukiHistoryItem.kt` share the selected formatter. `DateText.kt` and `TrackInfoDialog.kt` use the app locale for dates.
- `AboutScreen.kt`, `DebugInfoScreen.kt`, `WorkerInfoScreen.kt` and `DateExtensions.kt` apply the setting to absolute timestamps. The Tsuzuki About card has no visible build-time field. The novel status-bar clock is outside the upstream patch.
- `TimeFormatTest.kt` adds three tests for mode selection, locale defaults, and the combined timestamp.

Device review: In Appearance switch between all four formats in both Tsuzuki themes and a stock theme. Check History and Debug info, then change Android's 12/24-hour setting while the app is backgrounded and verify Follow system refreshes after resume.

#### Wave 2 — N9 to N20 (same batch rules; continue here after N8, in order)

#### N9 Tablet Browse two-pane (T3) — `done` (merged overnight)
Board `11-tablet/TabletBrowse`. On tablet UI (`isTabletUi()`, as D1/TsuzukiUpdatesTwoPane uses) in Tsuzuki themes,
Browse shows the sources list left and the selected source's catalogue right. Prefer hosting the existing catalogue
content composable in the pane over re-implementing it; if the Voyager screen model makes that unsafe, build a
read-only preview pane (popular list via the source's existing paging) with an "Open catalogue" action, and say so.
Phones unchanged. Don't touch `.worktrees/anime-a12` files.
Result: `tsuzuki/n9` at `cd94f45f8`, based on `tsuzuki/n8` for shared locale files. Per-file Spotless twice, debug assembly and all 734 app unit tests passed. Logs: `.worktrees/n9/app/build/overnight-verification.log` and `.build`.

- `SourcesScreen.kt` dispatches to the split pane only in Tsuzuki tablet UI for both media tabs. `TsuzukiBrowse.kt` highlights the selected pinned source. The same source list retains its pin, filter and latest actions.
- `TsuzukiTabletBrowse.kt` shows the selected source's real titles, covers, and language, with loading, retry, and empty states. The read-only preview uses page one of the existing popular catalogue API on IO and avoids the full pager's shared page tracker and navigation state. **Open catalogue** opens the full list with its existing controls and paging. Phones and stock themes keep their current routes.
- Two new labels in all eight `strings.xml` files. Left out sample counts, source health and filters in the preview. The first page loads when the user selects a source.

Device review: In portrait and landscape tablet Dot and Colour, select a novel source, switch sources while loading, use retry, and open the full catalogue. Repeat in manga Browse, test pin and latest actions, and verify a phone and a legacy theme still open the catalogue directly.

#### N10 Download queue manga rows in Compose (T3) — `done` (merged overnight)
`docs/design/boards` `07-system/Downloads`. The manga queue is View-based (`ui/download/DownloadAdapter`,
`DownloadHolder`, `DownloadHeaderHolder`, styled by `tsuzuki/TsuzukiViewStyle.kt`). In Tsuzuki themes render it in
Compose (source group header + rows with cover, title, chapter, progress via `TsuzukiProgressBar`, menu actions
move to top/bottom/cancel as stock, drag reorder if feasible with a maintained library already in the project —
else keep stock reorder via menu and say so). Stock themes keep the RecyclerView.
Result: `tsuzuki/n10` at `a63179f8b`, based on current `main` (`a567c86e5`). Per-file Spotless twice, debug assembly and all 737 app unit tests passed. Logs: `.worktrees/n10/app/build/overnight-verification.log` and `.build`.

- `DownloadQueueScreen.kt` dispatches the Manga tab to Compose in Tsuzuki themes. Legacy themes keep the RecyclerView. `TsuzukiMangaDownloadQueue.kt` renders source headers, real covers/titles, chapter rows, live progress/status, and the chapter and series menus. Error details can still be copied.
- `DownloadQueueViewModel.kt` loads covers on IO and sorts the existing groups without relying on the adapter. Reorder merges with the current queue so other media, new entries, and completed chapters are handled safely. `ReorderDownloadSubsetTest.kt` covers mixed queues and stale/duplicate drag entries.
- Drag reorder uses the existing Reorderable library within each source. Use the series menu or the global sort for larger moves. The Active and Errors views keep cancel actions and disable reorder. No new strings or sample storage/speed numbers.

Device review: Queue manga from multiple sources alongside novels. Check Dot/Colour progress, covers, drag within a source, chapter and series moves, toolbar sort, filters, error copy, and cancellation. Verify the stock RecyclerView and novel/translation tabs.

#### N11 Generated covers for books without art (T3) — `done` (merged overnight)
Board `14-components/Cover`. When a book has no cover URL / cover fails to load, draw a generated cover (title in
the display face, deterministic colours from the title hash; Dot = black/grey with one red mark; Colour = palette
like the board) instead of the empty placeholder, in `MangaCover` via a dispatch. Unit-test the deterministic
colour/seed logic.
Result: `tsuzuki/n11` at `a699ba338`, based on `tsuzuki/n9` for the tablet browse cover and its N1 ancestor for the updates pane. The shared `MangaCover` dispatch draws deterministic display-face covers for missing or failed art, with six motifs, the board’s ten Colour palettes and one red marker on greyscale Dot art. Passed real title metadata through browse, library, home, history and updates. URL-only callers without a supplied title use the existing placeholder, and no author is fabricated. Two deterministic tests cover palette/motif and the minimum signed hash. Both Spotless passes on 19 files, debug assemble and all 736 app unit tests passed. Device review: verify title sizing and clipping in grids, missing and broken URLs, and custom covers on books with no remote URL in Dot and Colour.

#### N12 Per-shelf book counts in Categories (T2) — `done` (merged overnight)
Board `07-system/Categories` shows a count per shelf. Add a read-only count (books per category, respecting the
category content type) to the Categories screen state and show it in `TsuzukiCategoryListItem`. Real data only.
Result: `tsuzuki/n12` at `47c2be4f2`, based on main `5f9c354df`. Categories now observes one grouped SQL query for live shelf counts. It counts saved library entries that match each shelf’s manga/novel/all type, including zero for empty shelves. The Tsuzuki row adds a localized numeric count alongside existing content type and default indicators. The automatic system shelf remains outside the editable shelf list. The in-memory SQLite fixture checked mixed and incompatible memberships, empty shelves and removal from the library. Both per-file Spotless passes, debug assemble and all 737 app unit tests passed. Device review: check count placement in both themes and updates after moving a book between typed shelves.

#### N13 Source counts per extension repository (T2) — `done` (Claude agent `13cfa2edc`, merged `57431ab55`)
Board `04-browse/ExtensionRepos` shows how many sources each repo provides. Compute from the available-extension
lists already loaded (manga + novel) grouped by repo, show in `TsuzukiExtensionStoreItem`. Hide when unknown.
Result: _pending_

#### N14 Global search result count + time (T2) — `done` (Claude agent `ec7f81ad0`, merged `6b7fa0b3f`)
Board `04-browse/GlobalSearch`: "N results · 0.6 s". Measure elapsed time per search in the global search
view models (manga + novel) and show results count + seconds in the Tsuzuki header/result tiles. Real measurements.
Result: _pending_

#### N15 Storage breakdown in Data and storage (T3) — `done` (Claude agent `ba3718e28`, merged `6bc2f7cdb`; tablet: 1.7 GB, 4 novels / 1 manga / 4,158 chapters)
Board `08-tools/DataTools` storage breakdown + stat tiles. Measure on IO: downloaded chapters size (download dir),
cover cache size, database size, plus counts (novels, chapters). Show in the Data and storage screen in Tsuzuki
themes; refresh on open; never block the main thread. Unit-test any formatting/aggregation helpers.
Result: _pending_

#### N16 Accent colour + Reduce motion settings (T3) — `done` (Claude agent `772656afa`, merged `2174782d3`)
Board `07-system/Settings` rows. Add `UiPreferences` for (a) Colour theme accent choice (the board's swatches; Dot
stays red) feeding `TsuzukiTokens`/`ProvideBookPalette` default accent, and (b) Reduce motion (app-level, OR-ed with
the system animator scale inside `rememberReduceMotion()`). Rows in Appearance (Tsuzuki themes only).
Result: _pending_

#### N17 Mihon: skip an update version + download from update screen (T2/T3) — `done` (Claude agent `5c66704ba`, merged `62896ccd9`; needs a public release to device-check)
Port Mihon fc20410 (skip this version) and #3707 (download the app update from the update screen) onto
`data/updater/**` and the Tsuzuki `TsuzukiNewUpdateScreen`. Find exact commits with `git log mihon/main --grep`.
Result: _pending_

#### N18 Mihon: failed-to-load extensions with reason (T3) — `done` (Claude agent `ef061ce14`, merged `3419ae572`; #3954 skipped, see UPSTREAM.md)
Port Mihon #3953 (list extensions that failed to load, with reason and stack trace) and, if cheap, #3954 (apply
trust/repo changes without restart). Keep Tsundoku's novel-extension loader paths. Restyle the new rows with
existing Tsuzuki extension tiles.
Result: _pending_

#### N19 Mihon: content warnings instead of the NSFW toggle (T3) — `done` (Claude agent `3911523fb`; no DB/backup change; old show_nsfw value seeds the new default)
Port Mihon #3951 (+ #3952 keep loading installed extensions regardless of warnings) if it applies cleanly to
Tsundoku's extension model (the repo index already carries `contentWarning`); if it needs a DB/backup change, stop
and mark `blocked` with the plan.
Result: _pending_

#### N20 Storage async migration (T4, last) — `done` (Claude agent `19d2b76a4`; all blocking storage probes moved to IO, sync wrappers kept for Downloader/cache; readers verified on tablet)
WORKPLAN P1.11: move synchronous `DownloadProvider`/`DownloadManager` storage callers (reader loaders, download
cache, EPUB export, translation) to the existing suspend APIs together, keeping thin synchronous wrappers only
where Tsundoku merges need the old signatures. Write tests for each moved path. If the change grows past ~600
lines or touches the novel viewer internals, stop at a clean intermediate commit and mark `blocked` with what's left.
Result: _pending_

#### N22b Anime: make the debug player render (T4) — `done` (Claude agent `755f1aa62`; tablet plays a local 720p H.264/AAC file with mediacodec hwdec + audiotrack; the old Google/Blender sample URLs return 403/404)
Tablet test 2026-11-06 05:51: `AnimeDebugPlayerActivity` (debug build) launches, libmpv loads (no
UnsatisfiedLinkError, no crash), but the SurfaceView stays black for both https and http Big Buck Bunny URLs and
mpv prints nothing. Steps: (1) wire mpv log messages (`MPVLib` event/log observer → logcat, `msg-level=all=v`);
(2) compare option order/values with Aniyomi's `PlayerActivity`/`MPVView` at the pinned revision (vo/gpu-context,
`android-surface-size`, hwdec, attach surface before/after `init`, `wid`); (3) for https provide a CA bundle
(`tls-ca-file`) like Aniyomi; (4) verify audio track via `ao=audiotrack`. Launch:
`adb shell am start -n io.github.manojx2005.tsuzuki.dev/eu.kanade.tachiyomi.tsuzuki.anime.debug.AnimeDebugPlayerActivity --es url <url>`.
Done when Big Buck Bunny plays with sound on the tablet.
Result: _pending_

#### Wave 3 — N21 to N32 (same batch rules; continue here after N20, in order)

#### N21 Anime A1.3: vendor the native player libraries (T3) — `done` (Claude `654e0dff6`; checksum tamper test passes; not an :app dependency yet)
WORKPLAN §6.1 A1.3. New Android library module `anime-player` with `src/main/jniLibs/arm64-v8a/*.so` and headers
copied from the archived artifact `app/build/anime-native-41645408336/anime-native-arm64-41645408336/` (verify every
file against its `SHA256SUMS.txt`; add a Gradle check task that re-verifies), licence texts from its `licenses/` into
`docs/licenses/` (LGPL FFmpeg/mpv notices + source offer note), module NOT yet a dependency of `:app` (no APK change).
Result: _pending_

#### N22 Anime A1.4: MPV wrapper + debug player (T4) — `done` (Claude agent `d71870d01`, merged `e49c11127`; see N22b)
Base on `tsuzuki/n21`. Port the MIT `MPVLib` wrapper (aniyomi-mpv-lib, pinned revision in docs/ANIME_SPIKE.md) into
`anime-player`, plus a debug-build-only `AnimeDebugPlayerActivity` (surface, play/pause, seek, back) that plays a URL
passed by intent. Wire `anime-player` into `:app` only for the debug build type (or behind a flag that is off in
preview/release). Re-run a 16 KB alignment check on the debug APK's anime libs and record sizes. No user-visible UI.
Result: _pending_

#### N23 Anime A2.1: schema design doc (T3, doc only) — `done` (`ec00d9a0b`, docs/ANIME_SCHEMA.md; owner questions Q1–Q8 inside)
Write `docs/ANIME_SCHEMA.md`: tables `animes`, `episodes`, `anime_history`, `anime_categories`, `anime_tracks` (Aniyomi
Apache-2.0 schema as reference), SQLDelight migration numbering after the current latest migration in
`data/src/main/sqldelight/tachiyomi/migrations/`, backup proto additions, how library/Home/Updates/History will read
them, rollback/upgrade test plan. No code changes.
Result: _pending_

#### N24 Neural TTS licence research (T3, doc only) — `done` (`52c7dae92`: GO-WITH-CONDITIONS)
WORKPLAN Phase 5 "Neural TTS voices". Determine from sherpa-onnx sources/build scripts whether its Android JNI
library links espeak-ng (GPL-3.0) for Kokoro/VITS, whether an espeak-free build option or lexicon-only voice packs
exist, and their licences/sizes. Write `docs/research/neural-tts.md` with a clear go/no-go and sources (URLs,
file paths, commit hashes). No code.
Result: _pending_

#### N25 Tablet reader side panel (T3) — `done` (Claude agent `502896e0e`; docked on windows ≥ 900 dp, reader_container end padding; not device-checked)
Board `11-tablet/TabletReaderPanel`. On tablet UI in Tsuzuki themes, the novel reader can show a side panel (contents
/ quotes / notes for this book) beside the text instead of the drawer, toggled from the existing contents action.
Reuse `TsuzukiReaderContents`, quotes data and book notes; don't touch viewer internals.
Result: done (`TsuzukiReaderPanel.kt`, hook in `TsuzukiReaderHooks.kt`; docked on tablet UI at >= 900 dp by padding `reader_container`; Contents / Quotes / Notes tabs; no Edit action since the notes screen is not reachable from the reader; not device-checked).

#### N26 Find in chapter (T3) — `done` (Claude agent `a9192df60`; WebView renderer only, hidden in TextView mode)
Board `06-reader/ReaderFind`. In the novel reader, a find bar that searches the current chapter text (query, n of m,
next/prev). Implement for the WebView renderer via its existing JS bridge / `findAllAsync`; for the TextView
renderer only if it can be done through a public hook of `NovelViewer` without changing its internals; otherwise
hide the action there and say so.
Result: _pending_

#### N27 Notebook: quotes + notes for a book (T3) — `done` (Claude agent `ecd4993c1`; details overflow, novels)
Board `06-reader/Notes`. A Tsuzuki "Notebook" screen per book combining existing quotes (highlights) and the
book note, reachable from details overflow and the reader. Real data only (no glossary — not stored).
Result: _pending_

#### N28 Listening widget + TTS state (T3) — `done` (Claude agent `0d7523a9c`; widget not yet placed/checked on a device)
Board `07-system/Widgets` (Listening). Expose TTS playback state from `TtsPlaybackService` (book, chapter, playing)
in a small observable store, and add a Glance "Listening" widget next to `TsuzukiReadingWidget` (play/pause via
the existing media session). Widget picker preview with neutral content.
Result: _pending_

#### N29 Library query builder (T3) — `done` (Claude agent `aa9d395a1`; saved filter/sort presets as pills; library overflow → Saved queries)
Board `08-tools/LibraryQuery`. A Tsuzuki screen that combines existing library filters/sorts (status, unread,
downloaded, source, category, tracker, content type) into saved "queries" shown as pills on the library header.
Store queries in a preference (JSON). Real filters only.
Result: _pending_

#### N30 Performance audit of Tsuzuki lazy lists (T2) — `done` (Claude agent `422bd3ec0`; ~24 key/contentType fixes, docs/research/compose-perf-n30.md)
Every Tsuzuki-owned `LazyColumn/LazyRow/LazyGrid` (presentation/tsuzuki/**, theme/tsuzuki/**): stable `key`s,
`contentType`, no heavy work in item composition (move to `remember`/view model), Coil image sizes for covers.
List each fix in Result. Behaviour unchanged; tests green.
Result: _pending_

#### N31 Accessibility audit of Tsuzuki UI (T2) — `done` (Claude agent `f986b6aeb`; 54 fixes, docs/research/a11y-n31.md)
All Tsuzuki composables: 48 dp touch targets, content descriptions for icon-only buttons (localised, 8 locales),
`Role`s/toggleable semantics, no hardcoded English. List each fix in Result.
Result: _pending_

#### N32 Unit tests for Tsuzuki logic (T2) — `done` (Claude agent `3c71b8dc1`; 56 new cases)
Add JVM tests for untested pure logic in `ui/tsuzuki/**`, `tachiyomi/tsuzuki/**` and `presentation/tsuzuki/**`
(e.g. home view-model mapping, updates summary, palette/tint math, value labels). Extract tiny pure helpers only
where needed (no behaviour change).
Result: _pending_

## Open questions for the owner


- Anime schema (docs/ANIME_SCHEMA.md Q1–Q8): share `categories` with content_type 3? Aniyomi-compatible backups (proto 500/501)? season columns? watch time in the reading streak? table naming; discard the spike's 36.sqm.
- Still parked: release v0.1 / signing / making the repo public; Firebase telemetry removal before release;
  Support us screen; in-app PIN lock; Import EPUB "off" switch.
- Upcoming (B6) now lists only the selected day's books (board behaviour) instead of every date with "Today /
  Tomorrow" headings; duplicate dialog (B2) taps select and Migrate is a separate button (board behaviour). Say
  if you prefer the stock behaviour.

## Owner decisions in force

- Nav (2026-11-06): HOME · LIBRARY · UPDATES · BROWSE · MORE with plain-word labels; History moves into More and
  Home's recent row (card Q5). Only Colour and Dot themes ship (card Q12). Reader settings follow the batch Q boards.

- Priority: usable, stable, smooth, fast first; then design completion; then Mihon features; then new features;
  anime last (but plan it now). (2026-11-04)
- No full novel/manga split; separate only Tsuzuki-owned code and hooks (2026-11-05, see WORKPLAN Phase 0).
- Owner runs Codex in the Codex app; Claude writes cards here and reviews (2026-11-05).
- Commits authored by the owner only, no co-author or "Generated with" trailers. Commit after review; **no push**
  unless asked.
- Both devices may be used for reversible testing; preview build is the daily/test app; owner's `.dev` data stays
  untouched. Temporary EPUB / Royal Road imports allowed with cleanup.
- Pushed screens follow the Settings boards, not the book palette. Dot "Colour covers" switch exists (default off).
- Anime native builds via GitHub Actions only; must stay non-GPL (LGPL dynamic libs OK with licences).
- Owner 2026-11-05 (before leaving): push `main` after review — allowed; build the media-types onboarding step
  (novels + manga now); commit the anime spike WIP on its branch; Codex is out of usage, Claude continues alone.
- AppLock PIN, Support us: parked. Release/signing/telemetry: owner-gated (WORKPLAN Phase 7).

## Device and data notes (things that bit us)

- Preview app id `io.github.manojx2005.tsuzuki.debug`, not debuggable → no `run-as`; back up with the in-app
  backup export. Debug app `…tsuzuki.dev` is debuggable (binary restore: `adb push` to staging + `run-as cp`,
  verify SHA-256; never pipe binaries through stdin).
- Git Bash mangles `/data/local/tmp` → use `MSYS_NO_PATHCONV=1` or call adb from Python.
- Long strip: the transition card only appears with "Always show chapter transition" on (it is on in the preview
  copy) or while the neighbour chapter loads; reading to a chapter end marks it read.
- Update check logs HTTP 404 while the repo is private (expected).
- `spotlessApply` per file only (`-PspotlessIdeHook=<absolute path>`), twice.
- Long Windows paths: use `git -c core.longpaths=true` for worktrees.

## Log (newest first)

- **2026-11-06 evening Claude — overnight plan.** Back on the desktop (JDK 21 = AS jbr). Owner approved for
  tonight: Codex commits on card branches; Claude reviews + merges Q1 now, then reviews/merges cards about every
  2 h, device-tests on tablet + S22 (in-app backup first; Q4 migration allowed after a backup), pushes main +
  card branches, deletes the superseded draft branches; extra wave X1–X4 after batch Q. Q10 done by Claude.

- **2026-11-06 Claude — owner switching computers; everything pushed.** Codex finished Q1 (`review`) and started Q5
  (paused) in uncommitted worktrees; each was committed as a `wip(...)` commit on its own branch and pushed:
  `tsuzuki/q1` (final Q1), `tsuzuki/q5` (Q1 + Q5 so far), and Codex's partial pieces `tsuzuki/q1-motion`,
  `q1-nav` (older drafts, superseded by q1), `q1-nav-fix`, `q1-review` (already in q1), `q5-more`, `q5-settings`
  (already in q5). Nothing merged into main. **Resume on the new computer:** `git fetch`, then
  `git -c core.longpaths=true worktree add .worktrees/q1 tsuzuki/q1` (same for q5), copy `local.properties`;
  Claude reviews `tsuzuki/q1`, Codex continues Q5 from `tsuzuki/q5`. The draft branches can be deleted after review.

- **2026-11-06 15:20 Claude — new machine set up + device pass → batch Q.** Installed Android Studio 2026.2.2,
  Temurin JDK 21, SDK 37.0/36, build-tools 37/36, NDK 29.0.14206865, platform-tools; `:app:compileDebugKotlin`
  green in 5 m 38 s. Owner's phone `R5GL23ZYE1V` (release build, Dot): reader bottom bar hides Next chapter
  off-screen, save-chapters custom range is an index slider, library shows two DEFAULT shelves (LNReader import),
  details/Browse/More text clipped, Dot motion stepped by design (frames fine: 1.2 % janky at 120 Hz). Boards for
  the fixes: `docs/design/boards/fixes-2026-11/` + canvas https://claude.ai/artifact/Qs9pMeZhphy77fbPiE9cTH.
  Cards Q1–Q12 written (Q11 reader settings, Q12 remove non-Tsuzuki themes, both added on owner request; Colour boards added); Owner approved Q5 (nav) the same day.

- **2026-11-06 06:30 Claude — batch N complete.** N20 merged (blocking storage lookups → IO: chapter content
  reader, DownloadPageLoader, `isChapterOnDisk` for skip-cache probes; 5 off-thread tests; novel + manga readers
  open fine on the tablet). N22b merged: mpv logging (`TsuzukiMpv` tag) + Aniyomi init/surface sequence + CA
  bundle → **anime stage 1 playback works** (debug build, local test video, hardware decode, audio). Build 846
  tests green, pushed, preview on both devices, tablet on Dot Home. Next anime step: A1.5 (install one real anime
  extension in the debug build and play an episode from it) — needs the owner to pick an extension/repo.
- **2026-11-06 06:00 Claude — morning review.** Batch N status: N1–N19, N21–N32 done (N22 player loads but renders
  black → follow-up **N22b**); **N20** (storage async migration, T4) not started; **N22b** ready. Codex did N1–N12
  (00:00–01:37) and then stayed at its usage limit; Claude agents did N13–N19, N21, N23–N32. Final `main` =
  `aab2aff6a`: debug + preview build, **841 unit tests green**, pushed; preview installed on tablet + phone, tablet on
  Dot Home, both crash buffers empty, extensions still load. Merged worktrees removed (only `anime-spike` left).
  Owner decisions waiting: anime schema questions (docs/ANIME_SCHEMA.md Q1–Q8); B2/B6 behaviour changes (duplicate
  dialog select-then-migrate, Upcoming per-day list); repo now carries ~30 MB of anime `.so` files in
  `anime-player/` (fine without LFS, but say if you'd rather use releases/LFS).
- **2026-11-06 02:30–03:55 Claude — Codex paused at its limit since 01:37; Claude took over cards.** Merged N13
  (repo source counts), N14 (global search count + time), N15 (storage breakdown; tablet shows real 1.7 GB /
  4 novels / 1 manga / 4,158 chapters), N16 (accent swatches + Reduce motion; Dot Appearance checked), N17 (skip
  update version + in-screen download; needs a public release to see), N18 (failed-to-load extensions; extensions
  and sources still load after cold start), N24 research (`docs/research/neural-tts.md`: GO-WITH-CONDITIONS,
  sherpa-onnx >= 2.0.1 has no espeak-ng). Build 766 tests green, pushed, installed on both devices. Claude hit its
  5-hour limit at ~03:55 (resets 04:30); N21/N23 agents stopped mid-way and are back to `ready`. Remaining: N19-N23,
  N25-N32. The 06:00 Claude review is still scheduled.
- **2026-11-06 00:00–02:30 Claude — overnight batch N1–N12 reviewed and merged as they landed.** N1/N2 Mihon fix
  groups A/B (webtoon stutter b5856135c skipped: needs Mihon's pixel-buffer reader pipeline), N3 novel status bar
  (Tsundoku `NovelStatusBar.kt` body moved into a `themed`-flag content function: watch on Tsundoku merges), N4
  manga reader chrome + settings (`ReaderTopBar.kt` actions extracted to a lambda, `ReadingModePage.kt` import
  aliases: watch on Mihon merges), N5 manga details mode tile + scanlator sheet, N6 vertical Tsuzuki slider, N7
  reader/translation settings (`StatusBarElementsScreen.kt` 77-line change: watch), N8 Mihon time format setting,
  N9 tablet Browse preview pane (Claude: only on windows ≥ 900 dp — portrait squeezed source names), N10 Compose
  manga download queue (safer subset reorder + tests), N11 generated covers (title added to `MangaCover`), N12
  per-shelf counts (read-only query). Each merge built green (now 739 tests), pushed, installed on both devices;
  tablet checks: manga reader chrome, manga details mode tile, Browse portrait list — no crashes. Also: Dot book
  opening motion made smooth (`0e6fd1732`), NanoHTTPD licence added. N13 claimed by Codex ~01:40 but no worktree
  yet; Codex session idle since 01:37 (likely usage limit) — heartbeat will resume it.
- **2026-11-06 00:xx Claude — anime A1.2 merged (`bb18e65a7`).** 26 animesource API files, NanoHTTPD 2.3.1
  (BSD-3, credit still to add), keep rule, unregistered loader, native builder + licences, ANIME_SPIKE.md. Spike UI/
  schema/player left for A1.3+/A2. Build 730 tests green. E2 range selection checked on tablet (4 selected, no crash).
- **2026-11-05 Claude — E1 + E2 Mihon fixes merged (`8cafe3b13`, `32f4570c3`).** E1: 88d4071cc (disk index
  can't overwrite a startup rescan, atomic index file), f28b2d887 (cancel queued downloads of chapters the source
  removed), f82a7a68f (fresh error notifications); f0f1e4de5 + a5d584eeb already in via the DownloadProvider sync,
  Mihon's `DownloadProviderTest` added. E2: c279291e2 (tablet FAB above the taskbar), a6ee4bc59 (library range
  selection across categories; pure helper + 6 tests). Build 730 tests green; preview on both devices; pushed.
  Overnight Codex batch N1–N8 queued; Claude review scheduled 06:00 2026-11-06.
- **2026-11-05 Claude — D1 tablet Updates + device passes.** D1 (`2e0451bb4` merge): on tablet UI (sw ≥ 720 dp)
  Updates shows list + preview; tablet portrait Dot checked (row select switches preview without opening the
  reader). Added a confirm dialog before a group preview downloads > 25 chapters (Shadow Slave group had 1,911;
  `93f36e6cd`). Device-checked: C4 replacement rules section + add dialog (tablet Dot, cancelled), B1 filter sheet
  (Filter label was thin on red → bold, `onAccent`). Builds 708 tests green. Still unchecked on device: C1, C3,
  C5 (see entry below), B2–B6 screens.
- **2026-11-05 Claude — C3, C4, C5 merged (Sonnet agents, reviewed by Claude).** C3 custom source setup +
  site-picker chrome (WebView untouched); C4 replacement rules (same JSON pref, live test line, regex validation,
  accent delete); C5 TTS mini player (accent play, 48 dp targets; no timer/speed/progress — no data). Build 706
  tests green, preview installed on both devices. **Not yet seen on a device:** C1 migration list (needs a real
  migration run), C3 setup + picker (Browse → custom source), C4 (novel reader → All settings → Advanced), C5 (needs
  "TTS controls visible" on, which is off in the preview copy). Do these in the next device pass.
- **2026-11-05 Claude — P0.6 + C1 + C2 merged and pushed.** P0.6 media-types onboarding step (`b0c041128`,
  `10dec7ac3`): Light novels fixed on, Manga toggles `hideMangaUi`; mixed-library pills hide in novel-only mode;
  phone Dot check found the stripe art spilling over the card (fixed with clipToBounds) and a wrapping Japanese
  title (shortened). C1 migration list (`4e742d0bd`): old/new tiles side by side, all row actions; not device-run
  (needs a real migration). C2 duplicate finder (`e0b70b185` merge): pills, group tiles, bottom action bar;
  tablet Dot idle + empty states checked, no crashes. Build 692 tests green. `main` pushed to origin (CI only;
  preview release workflow is manual). Restored the restyle card rules into WORKPLAN (they had been dropped from
  this file when the Codex queue was emptied).
- **2026-11-05 Claude — Codex P0.3 + B1–B6 reviewed and merged.** P0.3 (`ae1ecc6bd`, merge `5f2ff2a03`): five
  media-specific files moved to `tsuzuki/novel/` and `tsuzuki/manga/`, package/import lines only. B1–B6
  (merges `b2f863a4f`…`757642986`): catalogue filters, change-category + duplicate dialogs, edit info,
  tracking + tracker search, quote tiles + chapter search, EPUB export + Upcoming — each behind an early-return
  dispatch. Three Sonnet reviewers: all merge-with-fixes, no blockers; fixes applied (category selection uses
  plain `remember`, duplicate summary restored, tracker search list keys, EPUB filename labels use the stock
  `epub_include_*` keys, explicit translation-mode list, preview remember keys, pill index guard, muted match
  suffix). Known exception: quotes use Literata via a direct `FontFamily` (commented; no shared reader-face helper).
  `strings.xml` conflicts unioned. Onboarding device pass (tablet): welcome, languages (real list, current
  selection), look (Colour ↔ Dot recreates and stays on the step), storage, permissions, import tiles, backup
  handoff — all OK; tablet back on Dot Home, no crashes. Known pre-existing quirk: re-opening onboarding from
  Settings on the tablet two-pane layout and using a backup tile pushes a second Settings into the detail pane
  (stock onboarding does the same). Anime spike WIP committed on `codex/anime-spike` (`ad6442e1f`).
- **2026-11-05 Claude — reviewed Codex P0.4 + P0.5, merged (`af8155b6c`, `c9e648a3b`).** P0.4 (`ff407d59c`):
  transitions, contents drawer and ReadingDays moved to `presentation/tsuzuki/reader/TsuzukiReaderHooks.kt`;
  ReaderActivity/ReaderViewModel keep one-line calls; behaviour identical on review. P0.5 (`b4532cf5b`): six-step
  Tsuzuki onboarding (welcome → languages → look → storage → permissions → import), stock flow for legacy
  themes, 19 strings × 8 locales, 9 tests. Claude change in review: `StorageStep.kt` hook made purely additive
  (early-return block with its own picker/effect) instead of restructuring upstream lines. Device check: see
  next entry when done.
- **2026-11-05 Claude:** merged Tsundoku main (`c26dfc13a`, clean: TTS headset buttons, scroll actions into the
  overflow, faster title marquee, source memo fix, Based on Mihon link — our Tsuzuki About already credits Mihon
  under "Built on"). Added icons to the three new overflow scroll actions. Archived anime native artifact (LGPLv3
  FFmpeg, 16 KB aligned) and backed up anime spike WIP. Wrote WORKPLAN.md, this file; deleted old handoffs.
- **2026-11-05 Claude:** `8f7f12e3b` reader transition next-chapter download check runs on IO (Codex wrote it on
  2026-11-04; Claude finished): tablet Dot long strip 335→336 card rendered and recycled, no errors, crash buffer
  empty; data restored as noted above. Phone check open (P1.9).
- **2026-11-04 Codex (summary of the day):** WebView/Cloudflare robustness port (`2510f09f6`), source headers
  off the UI thread (`5e68ec54a`), Mihon installer sync with signing-key check (`2c7bcae9a`), legacy download
  names + shared batch lookup (`7636bd698`), novel renderer coverage corrected (`1f85f4fb3`), reader progress
  bars/typography/accessibility (`02b963e81`, `9b105a87a`, `74f39d534`, `2c24c5027`), profiles + splash cost
  (`b8b3938de`, `b68b3fc2c`), picks merge + split-image recovery (`c54b2f809`, `de061e0ef`). Evidence under
  `app/build/codex-*`; QA matrix in `docs/RUNTIME_QA.md`; measurements in `docs/PERFORMANCE.md`.
- **2026-11-03/04 Claude + Codex:** orders 1–10, 12–14 of the old queue (Tsuzuki UI across Home, Library,
  details, reader chrome, settings, browse, motion, translations, widget, menus). Details in git history of
  `AGENTS.md` (see top of this file) and `docs/design/README.md` status table.
