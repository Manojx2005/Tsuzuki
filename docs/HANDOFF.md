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

## Current state (2026-11-05, Claude)

- `main` = `1f2b79255`: Tsundoku main merged (`c26dfc13a`, 5 commits) + icons for the new overflow scroll actions,
  on top of `8f7f12e3b` (reader transition download check off the main thread). Debug + preview builds and all
  651 unit tests pass (`app/build/claude-merge-validation.log`). New preview installed on both devices; cold
  launch to Dot Home on both, crash buffers empty. Nothing pushed (51 local commits ahead of `origin/main`).
- Worktrees: `.worktrees/onboarding` (`tsuzuki/onboarding`, Codex working on P0.5),
  `.worktrees/anime-spike` (`codex/anime-spike`, old base, 31 uncommitted paths — backed up, see WORKPLAN §6.0).
- Devices: tablet `R52Y90HA77L` and phone `RZCWA139X1X` both connected. Tablet preview app on Dot Home; preview
  Continue now = Nano Machine ch. 336 (test reading); ch. 335 restored to unread, ch. 336 at page 2. Owner's
  debug app (`.dev`) untouched. Phone not yet tested this session.
- Anime native artifact archived at `app/build/anime-native-41645408336/` (GitHub copy expires ~2026-11-17).
- Codex heartbeat automation `continue-tsuzuki-work` repointed to this file.

## Claude is working on (Codex: don't touch these)

- Nothing claimed right now. Next candidates (WORKPLAN 4B/4C): reader immersive + paged chrome, tablet layouts
  (TabletUpdates, TabletBrowse), CatalogueFilters device check, device passes listed in the log below.

## Codex queue

### Overnight batch N (2026-11-05 → 06:00 2026-11-06) — run in ONE Codex thread, in order

**How to run the batch (overrides the usual rules where they differ):**
- Do the cards **in order N1 → N8**, one at a time. Before each card re-read this file; skip any card not `ready`.
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

#### N1 Mihon fixes, group A (T3) — `ready`
Port from remote `mihon` (already fetched; `git show <sha>`): 1e471a414 (updates show finished chapter as still
downloading), 287cbbb98 (keep last update time on empty Updates), 8457fb91e (chapter refresh with non-ASCII
digits), c9e1b6e96 (invalid extension filters instead of crashing), 4c88f0264 (SearchViewModel race),
d3f10772f (local source in global search), e9234f205 (settings search matches group headings). Also check
07b9fb695, ae749bb4f, cdc7d69c9 (installer) — probably already in via `2c7bcae9a`; mark "already applied" if so.
Per commit: already applied / `git cherry-pick -x -n` / hand-port preserving Tsundoku novel paths and Tsuzuki
hooks / skip with reason. Add tests where the logic is pure. Log each in `docs/UPSTREAM.md` (new dated section).
Result: _pending_

#### N2 Mihon fixes, group B (T3) — `ready`
Same method for: b5856135c (webtoon scroll stutter), a42428284 (covers reloading when removing from library),
0864ce0f4 (redact sensitive headers in logcat), 5910a51eb (keep uninstalled source names across restores),
0c1be369b (ask to add to library before editing cover), 40f552981 (blank title duplicates), c0c090999 (Kitsu
novels in search), ee5939454 (tracker start date UTC shift), dd4e9f00f (confirm unfavourite on manga screen —
must also work with the Tsuzuki details screen; keep the dialog themed via existing Tsuzuki dialog colours).
Result: _pending_

#### N3 Novel reader immersive + paged chrome (T3) — `ready`
Boards `06-reader/ReaderImmersive`, `06-reader/ReaderPaged` (PNG both themes + HTML). Restyle the existing
immersive (bars hidden, text selection toolbar if a composable exists) and WebView paged-mode chrome (page
indicator / page footer / any paged-mode controls) in `presentation/reader/**` via dispatches; new code in
`presentation/tsuzuki/novel/`. Don't touch viewer internals (`ui/reader/viewer/text/**`). Real data only.
Result: _pending_

#### N4 Manga reader chrome (T3) — `ready`
Boards `09-manga/MangaReader`, `09-manga/MangaPaged`, `09-manga/MangaReaderSettings`. The manga reader top bar
already uses `TsuzukiReaderTitle` and the Tsuzuki slider; restyle the rest of the manga chrome in
`presentation/reader/appbars/{ReaderAppBars,ReaderBottomBar,ReaderTopBar}.kt`, page indicator, and the manga
reader settings dialog pages (reading mode / general / colour filter) via dispatches, new code in
`presentation/tsuzuki/manga/`. Keep every action. If N3 touched the same files, base on `tsuzuki/n3`.
Result: _pending_

#### N5 Manga details (T2) — `ready`
Board `09-manga/MangaDetails` (scanlators, auto webtoon). Details already has Tsuzuki header/tiles/resume; add
the board's manga-specific parts that have real data (scanlator filter entry → existing excluded-scanlators
dialog, reading-mode/webtoon indicator from the book's viewer flags) and restyle the scanlator dialog. Hooks in
`presentation/manga/**` only as dispatches.
Result: _pending_

#### N6 Vertical reader progress (T2) — `ready`
"Vertical progress presentation remains stock" (`docs/RUNTIME_QA.md`). The vertical chapter navigator
(`presentation/reader/components/ChapterNavigator.kt`, vertical variant) should use the Tsuzuki slider look
(Dot dots / Colour thin track, 48 dp touch area, same semantics) like the horizontal one already does
(`presentation/tsuzuki/reader/TsuzukiReaderSlider.kt`). If N3/N4 touched these files, base on that branch.
Result: _pending_

#### N7 Settings screens vs boards (T2) — `ready`
Boards `07-system/SettingsReader`, `08-tools/ReaderBars`, `08-tools/ReaderEngine`, `08-tools/SettingsTranslation`.
These are `PreferenceScreen`-based and already get Tsuzuki tiles. Compare each board with the current screen
(`SettingsReaderScreen.kt`, `SettingsNovelReaderScreen.kt`, `StatusBarElementsScreen.kt`,
`SettingsTranslationScreen.kt`) and fix the differences that are presentation-only (grouping/section labels,
value labels in accent, segmented choices as pills via existing preference widgets/Tsuzuki widgets). List the
rest as left out. Note: `SettingsNovelReaderScreen.kt` has an old upstream lint error — don't "fix" unrelated code.
Result: _pending_

#### N8 Time format setting (T2) — `ready`
Port Mihon #4118 (time format setting next to the existing Date format in Appearance; find the commit with
`git log mihon/main --grep "time format" -i`). Apply it where upstream applies it (relative times / timestamps),
keep Tsuzuki value labels (`tsuzukiValueLabel`) working, strings in 8 locales.
Result: _pending_

## Open questions for the owner

- Still parked: release v0.1 / signing / making the repo public; Firebase telemetry removal before release;
  Support us screen; in-app PIN lock; Import EPUB "off" switch.
- Upcoming (B6) now lists only the selected day's books (board behaviour) instead of every date with "Today /
  Tomorrow" headings; duplicate dialog (B2) taps select and Migrate is a separate button (board behaviour). Say
  if you prefer the stock behaviour.

## Owner decisions in force

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
