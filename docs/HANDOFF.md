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

- Nothing right now. (Phone runtime checks P1.9 / P1.1 stopped by the owner on 2026-11-05; still open in WORKPLAN Phase 1.)

## Codex queue

### P0.3 Sort Tsuzuki-owned novel/manga code — `ready` (P0.4 and P0.5 are merged into main)
- Tier T1 · Codex `gpt-5.3-codex-spark`, effort low.
- Worktree: `git -c core.longpaths=true worktree add -b tsuzuki/media-packages .worktrees/media-packages main`, copy `local.properties`.
- `docs/ARCHITECTURE.md` §7 found only five media-specific Tsuzuki files; everything else is shared and stays.
  Move with `git mv`, change only the `package` line, imports and fully-qualified call sites:
  - → `app/src/main/java/eu/kanade/presentation/tsuzuki/novel/`: `reader/TsuzukiReaderSettingsSheet.kt`,
    `reader/TsuzukiReaderContents.kt`, `updates/TsuzukiUpdatesGroupTile.kt`, `settings/TsuzukiSourcePriority.kt`
    (package `eu.kanade.presentation.tsuzuki.novel`).
  - → `app/src/main/java/eu/kanade/presentation/tsuzuki/manga/`: `reader/ReaderDownloadStatus.kt`
    (package `eu.kanade.presentation.tsuzuki.manga`).
  - Callers to fix (FQN or import only, no other edits): `P/reader/settings/ReaderSettingsDialog.kt`,
    `A/ui/reader/ReaderActivity.kt`, `P/updates/UpdatesUiItem.kt`, `A/ui/library/duplicate/SourcePriorityScreen.kt`,
    `A/ui/reader/viewer/ReaderTransitionView.kt`, and `P/tsuzuki/reader/TsuzukiReaderHooks.kt` (now calls
    `TsuzukiReaderContents`; it stays in `reader/`); move the tests (`TsuzukiTocFilterTest`, `SourcePriorityStepTest`,
    `ReaderDownloadStatusTest`) to the matching test packages. Keep `internal` visibility working.
  - Update the paths in `docs/ARCHITECTURE.md` §7 is Claude's job; just list the moves in Result.
- Done when: `git diff -M --stat` shows five renames (+ tests) with only package/import lines changed, per-file
  Spotless clean, `./gradlew :app:assembleDebug :app:testDebugUnitTest` green.
Result: _pending_

## Open questions for the owner

1. **Onboarding content step** (`09-manga/OnboardingContent`, "What do you read & watch?" Novels / Manga /
   Manhwa / Anime, "we only show what you use"): there's no preference behind it yet. Build it as a new
   "media types I use" setting that hides tabs/filters for unused types (manhwa waits for classification, anime
   for stage 2)? Or skip it?
2. Anime spike WIP: commit the 31 uncommitted API-port paths on `codex/anime-spike` to preserve them, or keep
   only the patch backup? (Plan re-ports onto current main either way.)
3. Push: 49+ local commits are unpushed. Push `main` to `origin` now?
4. Still parked from before: release v0.1 / signing / making the repo public; Firebase telemetry removal before
   release; Support us screen; in-app PIN lock; Import EPUB "off" switch.

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
