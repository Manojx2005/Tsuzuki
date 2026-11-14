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
  why, test counts, what Claude must check on a device), commit on your card branch
  `tsuzuki/<card>` (owner as author, no trailers; message `<card>: <what>`), put the hash in Result, set `review`,
  stop. Never commit to `main` or another card's branch; don't push or use adb.
  If blocked, set `blocked` and write the question.
- **Claude:** reviews `review` cards (checklist in WORKPLAN §0.5), tests on devices, merges into `main` as the owner, moves
  the card to the log, writes the next cards. Claude also owns upstream merges and everything on devices.
  **From 2026-11-08 (owner): the Codex review lane does this job** (Batch T): it reviews, uses the tablet and
  S22, and merges into `main` locally. Implementer threads still never touch `main` or adb.
  **From 2026-11-12 (owner): anime-lane implementers (Batch AY and the A6 cards) test their own cards with adb**
  on the tablet and the S22, one thread at a time (device lock line in "Batch AY"). App-lane implementers still
  don't use adb.
- Statuses: `ready` · `codex-working` · `claude-working` · `review` · `blocked` · `done`.
- **Several Codex threads at once (owner may open more than one):** give each thread a card explicitly, e.g.
  `Read AGENTS.md and docs/HANDOFF.md, then do card P0.4 only.` A thread must re-read this file and claim its card
  (status `codex-working` + worktree path) *before* any other work, and must skip any card already claimed.
  Only cards marked `parallel-ok` may run beside another card; never run two cards that list the same files.
  At most two threads build with Gradle at the same time.
- Log entries: newest first, one short paragraph, with commit hashes and evidence paths.

---

## Plan 0.1.1 — reading first, anime later (owner 2026-11-12)

Owner 2026-11-13 (this chat): review RV2 then RV4, then implement the app queue; another chat reviews cards implemented here. Use the tablet only for now; S22 device checks are deferred. Hourly resume reminder disabled after owner stop on 2026-11-13.

Owner decisions 2026-11-12: (1) the next release is **0.1.1 with no anime**; (2) **the anime lane is paused**
(A6.4 may be committed as it is; no AY card starts until 0.1.1 is tagged); (3) **Codex does the reviews**
(review lane, Batch T rules); (4) Claude drew the missing boards (`docs/design/boards/fixes-2026-11b/`); they
are **approved by the owner (2026-11-12)**.

| Step | What | Who | Done when |
|---|---|---|---|
| 1 | **RV2** finish: completed: fixes committed and accepted cards reviewed | Codex review lane | R15, S1, S2, S3, R13 each have a verdict; passed ones are in `main` |
| 2 | **RV4**: V7.7-F, R12-F, S3-F, R14, CHAPNUM, TTS-LANGUAGE, MAIN-REPOS | Codex review lane | same |
| 3 | Fix cards from RV2/RV4: **S2-F**, **RP1**, **STR1**, plus any `-F` card a review adds | Codex app lane (one thread) | each is `review`, then **RV5** |
| 4 | Boards 2026-11b approved | Owner | **done 2026-11-12** |
| 5 | **ERR1** errors and long waits say what happened (after step 4) | Codex app lane | `review`, in RV5 |
| 6 | **RV5**: everything from steps 3 and 5 | Codex review lane | merged |
| 6b | **Neural TTS** (owner 2026-11-12: ship it in 0.1.1): NT1 review S4 → NT2 licence + speed + owner listening test (**go / no-go**) → NT3 engine in release builds + Voices page (board 5, approved 2026-11-13) → NT4 voice pack published by the owner | review lane, app lane, owner | merged, or a recorded no-go |
| 7 | **PERF1** speed budgets measured on `main` | Codex review lane (needs devices) | numbers in `docs/PERFORMANCE.md` |
| 8 | **REL-0.1.1**: changelog, version, preview APK on the tablet and S22 | Codex review lane prepares; **owner tags and publishes** | owner has the release candidate |
| 9 | Owner uses the release candidate for a week; notes become cards | Owner | — |
| 10 | Anime resumes: RV3 (chain review), then AY-PLAY, AY9, AY0 … | Codex | after the 0.1.1 tag |

Not in 0.1.1: anime (all of it stays behind its switch, unmerged), R13 dictionary packs if RV2 does not pass them (they can ship in 0.1.2).

**Prompts for the owner to paste:**
- Review lane: `Read AGENTS.md and docs/HANDOFF.md. You are the review lane: follow "Plan 0.1.1", start with RV2, then RV4.`
- App lane (one thread): `Read AGENTS.md and docs/HANDOFF.md. You are the app lane: follow "Plan 0.1.1", take S2-F, then RP1, then STR1, then ERR1, then NT2 and NT3 when they are ready.`
- Don't start an anime thread.

Owner 2026-11-14 (this chat): Resume ERR1, then investigate a neural TTS binary that satisfies the literal NT1 native-string rule. The owner reports the tablet connected by USB and the S22 available over wireless debugging. Independent review may use both; this app implementer still does not run device QA. This is permission to investigate, not a waiver of NT1, lexicon licensing or the owner listening go/no-go.

## Current state (2026-11-14, Codex review lane — Case B completed)

RV5, PERF1 and REL-0.1.1 candidate preparation are complete. Accepted review fixes:
`0a96ace3d`, `bf5055dcd`, `8570ff5d7`; RV5 close `88bcf6e4e`, PERF1 `5bd037c50`.
Release branch includes those fixes, versionCode 25 / versionName 0.1.1. Preview + Release
builds and 1,085 tests pass; 16 KB checks pass. Exact Y4 upgrade comparison retained
500 books, 50,000 chapters, 995 history entries, 30 shelves and exported settings.
Both test devices are on optimized candidate scratch Dot Home with 500 synthetic books;
final crash buffers empty, devices remain awake. Evidence: `app/build/review-evidence/rel-011/`.
PERF-START and PERF-DETAILS are ready follow-ups for measured budget misses; no tuning done.

**Stop at the owner publishing handoff.** Local artifacts use the Android Debug signing
certificate; official CI signing and publishing remain with the owner, along with the owner-phone
install and stale `v0.1.1` tag decision. No tag or push. Neural voice/anime remain blocked.
Daily apps were untouched: auto review rejected relaunching them, so scratch candidates
were used for the final handback. Local fixture server/reverse stopped and test Wi-Fi/files restored.

## Current state (2026-11-14 evening, Claude — took over from Codex, which is out of usage)

Owner 2026-11-14: Claude orchestrates and cheaper agents (Sonnet/Haiku subagents) do the work, until Claude's usage runs out. Nothing is pushed; `main` is still `0b1c8c889` until RV5 closes.

**For Codex, if it resumes before Claude finishes — read this block first.**
- Branch map (all local, nothing pushed, `main` untouched at `0b1c8c889`):
  `review/rv5` (`.worktrees/rv5`, tip `86e640358`) = S2-F + RP1 + STR1 + two review fixes + TTS-LANGUAGE →
  `review/rv5-err1` (`.worktrees/rv5e`, tip `28bd88a33`) = that + ERR1 with its review fixes; **1,084 tests, 0 failures** →
  `tsuzuki/rel-0.1.1` (`.worktrees/rel-011`, tip `6c0dea4a7`) = that + 0.1.1 changelog + versionCode 25 / versionName 0.1.1 (not built).
  When the device passes are clean: fast-forward `review/rv5` to `review/rv5-err1`, then `main` to it (Batch T step 7), then REL.
- **Do not take:** RV5, ERR1, TTS-LANGUAGE, NT1-BINARY review, REL-0.1.1 steps 1–2 (all done or in Claude's hands).
- **Still open, in order:** (1) device pass A–H on `review/rv5` (running; verdicts for S2-F, RP1, TTS-LANGUAGE come from
  it); (2) ERR1 device script with the APK already built at
  `.worktrees/rv5e/app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` (package `…tsuzuki.rv5debugscratch`);
  (3) close RV5: verdicts in each card, log entry, status table in `docs/design/README.md`; (4) PERF1; (5) REL-0.1.1
  steps 3–5 on `tsuzuki/rel-0.1.1`.
- Scratch builds: `./gradlew -I app/build/review-evidence/rv5/scratch.init.gradle :app:assembleDebug` (init script
  sets the application id; no `build.gradle.kts` edit). Use `--no-parallel --max-workers=2 -Pkotlin.daemon.jvmargs=-Xmx6g`.
- **Owner must decide:** neural voice is out of 0.1.1 unless the lexicon licence is cleared (below and
  `docs/research/neural-tts-clean-lexicon.md`); a stale upstream **local tag `v0.1.1`** (2016 commit `0210fd882`)
  exists — `git tag -d v0.1.1` before tagging the release (the remote only has `v0.1.0`).

**What Codex does next (Claude, 2026-11-14 evening).**
*Case A — Claude is still working (the Device lock line in "Batch AY" names Claude):* app lane only, no adb, no
`main`, don't touch `.worktrees/rv5`, `rv5e` or `rel-011`. Take these in order, each on its own branch from
`review/rv5-err1` unless it says otherwise:
1. **NT1-BINARY-F** (branch `tsuzuki/nt1-binary`, same worktree; T2): add `-ffile-prefix-map` so the `.so` has no
   absolute build paths or user name; bundle the full BSD-3 text for kissfft; make `prepare-source.py` fail on
   extra files in `.native/src`; add a UTF-16 pass to `scripts/audit-neural-runtime.py`. Rebuild, re-run the 13
   tests, record the new hashes. Still no model, no app integration, no S4 merge.
2. **STR1-F** (T1): the CSV export dialog title shows 移行するデータを選択 ("select data to migrate"); give it
   its own `tsz_` string ("select columns to export") in 8 locales. One-line upstream edit.
3. **ERR1-F2** (T2), only what can be settled by reading code and unit tests: "Loading from the source" must not
   be the label for a downloaded chapter/page or for the WebView translation wait (use a neutral "Opening" label
   there; 8 locales); in themed mode the pager/webtoon loading tile should keep showing the real page progress
   percentage that `ReaderProgressIndicator` already receives. Leave the overlay-vs-chapter-end-card question to
   the device pass.
4. **R12-F** leftover (T2): add the missing decode warning RV4 noted; the live-source trust check stays deferred.
Set each to `review` with a Result as usual; Claude (or a different Codex chat) reviews them.
*Case B — Claude has stopped (owner says so, or the Device lock still names Claude with no log entry for a
day):* a **review-lane chat that did not implement S2-F, RP1, STR1, ERR1 or NT1-BINARY** takes over the "Still
open, in order" list below from item 1, following Batch T. Take the Device lock first. The app-lane chat that
wrote those cards must not give their verdicts.
*Either case, do not start:* NT2 steps 2–4, NT3, NT4 (blocked on the lexicon licence and the owner), any anime
card, tagging or pushing.

- **RV5 (`review/rv5`, `.worktrees/rv5`) is Claude's now.** On top of Codex's S2-F + RP1 + STR1 merges:
  `03cd237dd` review: RP1 Listen dock shows the *playing* chapter (was the viewport chapter; `ReaderListenLayout.kt`,
  new pure helpers in `NovelTtsPlayback.kt`); `498430910` review: native autoplay no longer replays the last
  chapter (hand-off anchored on the playing chapter id, not the viewport index; WebView never had the bug);
  `86e640358` merge **TTS-LANGUAGE** (`9fb91e4e3`, finished from the dirty worktree: script check first, classifier
  only for Latin/Han text of 24+ chars, engine without the detected language resets to the device locale).
  `TtsController.kt` conflict resolved keeping both the listening-run tracking and the language job.
  **1,044 tests, 0 failures.** Not yet device-verified: a device pass (checks A–H, evidence files `c-*` in
  `.worktrees/rv5/app/build/review-evidence/rv5/`) was started on the tablet, then the S22.
- **STR1 device check: pass** on tablet and S22 (Dot, Japanese): every Data and storage row and the CSV export
  dialog are Japanese, nothing clipped, no crash (`str1-*.png`). Not seen on a device: the LNReader options dialog
  (it opens only after a file is picked; no file was picked). Nit: the CSV export dialog title is
  移行するデータを選択 ("select data to migrate"), a reused migration string.
- **ERR1: independent review = pass with small fixes** (no blocker). Fixes applied on `tsuzuki/err1` as
  `5f5f49781` (1,051 tests), merged on `review/rv5-err1` as `5a7f1816a`, plus `1ac0b4524` (a late result after a
  per-source retry is ignored), `47e8438d0` (AGENTS touch list), `28bd88a33` (ktlint: parentheses in
  `ErrorCopy.kt`). What the fixes were: titles over-redacted ("Chapter #12" → "Chapter [redacted]");
  global-search Retry navigated instead of retrying (new per-source retry in `SearchViewModel`); `errorCopy`
  could throw `CancellationException` inside composition (non-throwing UI variant); bare HTTP 403 → challenge copy;
  redaction gaps (JSON-quoted secrets, file paths, `user:pass@host`); `NoChaptersException` lost its specific
  message; uncached `errorCopy` in download rows; hidden ComposeView left per pager retry; one vacuous test;
  `NovelViewer.showLoadingIndicator` re-indent. Still to check on a device: whether the opaque WebView overlay
  covers the chapter-end card during a chapter switch; "Loading from the source" shown for downloaded chapters;
  page-progress percentage dropped in themed mode. New upstream file for the AGENTS list besides the six in
  ERR1's Result: `presentation/browse/BrowseSourceScreen.kt`. Then: merge into `review/rv5`, build, device script.
- **NT1-BINARY: independent artifact review = pass.** AAR and .so hashes/sizes match; 0 `espeak`/`piper` bytes
  (ASCII and UTF-16) in the .so; 3 PT_LOAD at 0x4000; DT_NEEDED system libs only; 18 `Java_*` exports (10 TTS,
  8 VersionInfo); 13 Python tests pass. Notes: the patch removes code paths, not GPL code — official 2.0.1 has no
  espeak-ng/piper source, the gate's hits were deprecation text and identifiers; clean strings depend on
  `--gc-sections` (the audit script must stay in release checks); `classes.jar` is the unmodified official one and
  contains "eSpeak" inside `OfflineSpeaker*` class names (jar is not scanned by the gate); ONNX Runtime is a
  third-party prebuilt pinned by hash only; the .so embeds 25 absolute build paths with the Windows user name
  (add `-ffile-prefix-map` before shipping); kissfft's full BSD-3 text is missing; arm64 only (other ABIs must
  fall back to the phone voice; not implemented); never loaded a model. Not merged; S4 stays excluded.
- **NT2 step 1 (lexicon licence): NOT SETTLED → by the Plan 0.1.1 rule, 0.1.1 ships without the neural voice
  unless the owner clears it.** `lexicon-us-en.txt` / `lexicon-gb-en.txt` are a mechanical re-encoding of misaki's
  `{us,gb}_{gold,silver}.json` (sherpa-onnx `scripts/kokoro/v1.0/generate_lexicon_en.py`, `run.sh`). The misaki
  maintainer says (hexgrad/misaki#51) silver = output of espeak-ng 1.52 (GPL-3.0) over dwyl/english-words, gold =
  unnamed "local datasets". No data-specific licence exists; only the repo-level Apache-2.0. Issues
  hexgrad/misaki#107 and #113 ask exactly this and have no maintainer answer. Options: wait for / ask the
  maintainer; legal advice on espeak output; or build our own lexicon from a source with an explicit licence
  (e.g. CMUdict, licence not yet checked). `lexicon-zh.txt` (pypinyin + misaki zh) is unverified too.
- **REL-0.1.1 prep:** changelog draft (22 items for `main` + 5 pending lines) is in `docs/CHANGELOG-0.1.1-draft.md` (uncommitted),
  not yet moved into `CHANGELOG.md`. Its dictionary line promises packs that are not published (R13): reword or publish first.

## Current state (2026-11-14, Codex app lane)

- ERR1 code `ad3e6a871` / handoff `dd945ec72` is ready for independent review: Debug APK + 1,039 tests pass. S2-F, RP1 and STR1 are also review; RV5 is ready for another chat.
- NT1-BINARY `d1e360ff4` + `d580d500e` provides a 7.86 MB arm64 Kokoro-only source-built candidate satisfying the literal/native alignment checks; 13 audit/source tests pass. It has not been integrated or device-tested. NT1 remains pending independent review; NT2's licence/performance/listening gates remain.
- Main stays `0b1c8c889`; no merge or push. Tablet USB and S22 wireless are available per owner. This app lane performed no device QA or installs; device lock stays free. Hourly resume automation stays disabled.

## Current state (2026-11-13, Codex)

- RV2 base `424618de6`: RV2 accepted R15, S3/S3-F and R13; 1,006 tests green. S1 deferred pending explicit extension-trust approval; S2 failed and S2-F remains ready. RV2 evidence archived in `app/build/review-evidence/rv2/`; disposable app removed and the daily tablet Preview restored.
- RV4 accepted V7.7-F, CHAPNUM and MAIN-REPOS after the repository Add fix. Accepted suite: 1,011 tests green, Debug/Preview built, tablet scratch QA complete. This chat now switches to app work S2-F → RP1 → STR1 → ERR1; another chat reviews those cards. S22 deferred by owner; hourly resume reminder stays active.
- TTS-LANGUAGE still has uncommitted implementer work and was skipped. R12-F/R14/NT1 are deferred with explicit evidence/gates below. NT2 requires NT1 merged and NT3 requires the owner listening go; neither gate is waived. Anime remains paused.

## Current state (2026-11-12, Claude)

- **Owner bug 2026-11-12: extension videos don't play.** Reproduced by Claude on the tablet; card **AY-PLAY** (PARITY §7) is first in the anime lane, then **AY9** video previews (PARITY §8).
- **Anime lane: read `docs/ANIME_ANIYOMI_PARITY.md`, then "Batch AY" in the Codex queue.** Claude compared the
  anime code on `tsuzuki/a6.3` with Aniyomi (`97414446b`) and found 15 gaps; the biggest are stream loading (one
  failing hoster fails the episode, the source's own video order and default headers are ignored), no anime
  source settings screen, and subtitle fonts. New order: A6.4 → AY0 → AY1 → AY2 → AY3 → AY4 → AY6 → A6.5 → AY5 → A6.8 →
  AY7 → A6.6 → A6.7 → AY8. A6.9 is replaced by AY0 + AY1 + AY4.
- Unchanged since 2026-11-10: `main` has no anime chain yet (39 commits wait on `tsuzuki/a6.3`); RV2 is open with
  14 findings and no merge verdict; RV3 (anime chain review) has not started; A6.4 has uncommitted work in
  `.worktrees/a6.4`; `main` has uncommitted changes (no default repos in shipped builds, this file).
- S22 is on wireless debugging (owner 2026-11-12). On 2026-11-12 `adb devices` showed only the tablet, so the
  review lane needs the IP:port from the owner (`adb connect`), and must confirm the device with
  `getprop ro.serialno` before installing.

## Earlier state (2026-11-08, Claude)

- **Claude is out of usage (owner, 2026-11-08). Codex now reviews too:** open a third Codex thread with the
  review lane prompt in "Batch T". Waiting for review: app lane V7.6, V7.7, R12, R13, R15, R16, R17, S1, S2, S3
  (RV1, RV2) and the anime chain up to R-A5 on `tsuzuki/r-a5` (RV3). Ten new anime cards (A4.3a, A6.1–A6.9)
  are in the anime lane table.
- **v0.1.0 is released** (2026-11-08, https://github.com/Manojx2005/Tsuzuki/releases/tag/v0.1.0). **The freeze is
  lifted**: `main` takes reviewed cards again, anime included (still behind `TsuzukiAnime.enabled` until A4.3).
- **Codex runs two lanes** (prompts in "Batch S" at the top of the Codex queue): lane 1 = anime chain (A3.5 →
  R-A5 → A3.6 → ... → A4.2 → A5.x), lane 2 = app (V7.6 → V7.7 → R12 → ... → S4). Claude reviews,
  device-checks, merges.
- Claude next: merge `review/post-v01` (R11, HOME-SWIPE, R8, R10) after device checks; merge the anime chain into
  `main` once R-A5 is done; U1 after 0.1.1 is tagged.

## Earlier state (2026-11-07 evening, Claude)

- **Goal: v0.1 release (no anime).** Plan and every card: "v0.1 release — every card" in the Codex queue below.
- V1 merged (`3fa896b3e`): Y2, Y6, R1–R5, R9 are in main. Codex: V1.1 → R6 → R7 → V3–V6, then post-v0.1 cards.
- Claude's next session: V1 leftovers, review everything Codex finished, V2 upstream merge + freeze, V7–V12.

## Earlier state (2026-11-06 evening, Claude — desktop machine)

- Back on the desktop: JDK 21 = Android Studio `jbr` (21.0.10); the Temurin path in older notes is the laptop's.
  Tablet `R52Y90HA77L` + S22 `RZCWA139X1X` connected; owner's phone `R5GL23ZYE1V` is not (it's on the laptop).
- `main` = docs batch Q (`f832df96a`) + this plan. Q1 is being reviewed by Claude now, then merged into main;
  `tsuzuki/q5` gets rebased onto it. Codex runs **Overnight batch Q** below in one thread.
- Anime spike worktree + local branch deleted (owner, 2026-11-06); `origin/codex/anime-spike` kept as backup.

## Claude is working on (Codex: don't touch these)

- Owner 2026-11-07: Codex works on the anime lane only; Claude fixes owner-reported bugs.
- Overnight: every ~2 h Claude reviews cards that reached `review`, builds (only when Codex isn't building),
  device-checks on the tablet + S22 **with the preview build** (`.debug`; the owner's `.dev` data is never used for
  card builds; backup before data-changing cards Q4/Q12; A2.2 only in a temporary `.scratch` install), merges good
  cards into `main` locally and pushes main. Codex never waits on this.

## Codex queue

#### TTS-LANGUAGE Select the automatic voice from chapter text (T3) — `done` (Codex RV5, 2026-11-14)

Owner report from RV2. English text spoken on a Japanese device used the Japanese voice. Detect the language of the text sent to speech in both novel renderers and ask the engine for that language. Keep an explicitly selected voice or legacy voice locale. Check a Japanese device with English and Japanese text and confirm the choice follows the chapter on handoff.

Review (Codex RV5, 2026-11-14): Pass. Retained explicit voice/legacy locale behavior and cancellation across handoff. In Japanese-system scratch apps, Native English selected `en-US` (S22 process trace 14:57:27), WebView Japanese selected `ja-JP` (tablet trace 14:59:22). Earlier explicit English playback and automatic/restore menu evidence inspected. Script/classifier/fallback tests included in the 1,085 passing combined tests. No neural runtime or model integrated.

### Batch T — Codex reviews Codex (owner 2026-11-08; Claude is out of usage) — read this first

Owner decisions (2026-11-08): **a Codex review thread does Claude's review job**: it may merge passed cards into
`main` locally (owner as sole author, no trailers, **no push**) and may use adb on the tablet `R52Y90HA77L` and
the S22 `RZCWA139X1X` (**never** the owner's phone `R5GL23ZYE1V`). The implementer lanes (Batch S lane 1 and 2)
are unchanged: they still commit only on `tsuzuki/<card>` and never touch `main` or adb.

**Review lane prompt:** `Read AGENTS.md and docs/HANDOFF.md. You are the review lane: do the first RV card that is
ready, then the next.` A reviewer never reviews a card it implemented in the same thread.

**How a review card runs (every RV card):**
1. Claim it (`codex-working`, worktree `.worktrees/<rv>`, date). Worktree from current `main` on branch
   `review/<rv>`; copy `local.properties`.
2. For each card in the RV card's list, in order: `git merge --no-ff tsuzuki/<card>`. Conflicts in
   `docs/HANDOFF.md`: keep `main`'s text and copy in only that card's status line + Result. Conflicts in string
   files: union (keep both keys; duplicates → keep one). Any other conflict you can't resolve in a few lines →
   `git merge --abort`, the card fails review ("needs a rebase on main"), go on.
3. Checklist per card (WORKPLAN §0.5 items 1–7): `git diff --stat main...tsuzuki/<card>` stays inside the card's
   files; upstream files have only a small hook (no re-indent, no stray Spotless; restore with
   `git show main:<file> > <file>`), and each new upstream file is added to the AGENTS.md "already touched" list;
   tokens + `MaterialTheme.typography` only; Dot uppercase + red only; real data only; no blocking on main,
   `CancellationException` rethrown, no swallowed errors without a log; strings in all 8 locales with the same
   placeholders. Read the whole diff, not only the Result.
4. Build the merged branch once: `:app:assembleDebug :app:assemblePreview :app:testDebugUnitTest`. Gradle limit:
   at most two builds at once across all threads; on a daemon OOM, wait and rerun.
5. Devices (AGENTS.md "Test on devices"): install the **preview** APK on the tablet and the S22; Dot first, then
   Colour; follow each card's "Claude device check" list; save screenshots to
   `app/build/review-evidence/<rv>/<card>-<device>-<what>.png` and look at them; `logcat -d -b crash` must be
   empty. Before a card that changes data (backup, migrations, library classification), export an in-app backup
   first and restore it after. Anime is hidden in preview: check anime with the **debug** APK on the S22 only, after
   an in-app backup of the `.dev` app (restore if anything changed). If the screen isn't what you expect (owner
   using the device, system picker, permission dialog), don't tap through it: skip that check and write it down.
   Leave the tablet on Dot Home; `svc power stayon usb` on both.
6. Verdict per card, written in that card's section as `Review (Codex RV, date):` with pass/fail, findings as
   `file:line — what`, test counts and evidence paths.
   - **Pass:** small fixes (stray formatting, a missing locale string, a missing AGENTS.md line, an upstream
     re-indent) go in a separate commit on the review branch, `review: <card> <fix>`. Card → `done`.
   - **Fail:** `git revert -m 1` that merge on the review branch (or never merge it); card → `blocked` with the
     findings; add a fix card `<card>-F` (`ready`, same lane, T2/T3) right under it, listing exactly what to change.
7. When every card in the list has a verdict: `git merge --ff-only review/<rv>` from the main worktree (if `main`
   moved, merge `main` into the review branch, rebuild, then fast-forward). Add one log entry (newest first:
   cards merged, cards failed, test counts, evidence folder), set the RV card `done`, update the status table in
   `docs/design/README.md`. No push. Delete the review worktree, keep the branches.



#### RV1 Review: app lane batch 1 (T3) — `done` (2026-11-08)
Cards, in order: **V7.6, V7.7, R12, R16, R17** (all `review`; each branch is 2 commits off `main`). R17 is
tablet-only: check line length on the tablet in portrait and landscape and that the S22 is unchanged.
Review: V7.6, R16 and R17 passed. V7.7 and R12 were reverted, with fix cards below. The full candidate built Debug and Preview and passed 981 tests. After the reverts, Preview built and 977 tests passed with no failures or skips. Both devices launched the accepted Preview. The tablet stayed on Dot Home. The S22 crash buffer contained a UI automation process failure during a dump, with no Tsuzuki crash. Evidence is in `app/build/review-evidence/rv1/`.

#### RV2 Review: app lane batch 2 (T3) — `done` (2026-11-13; R15, S3/S3-F, R13 accepted; S1 deferred, S2 failed)
**Final RV2 evidence:** accepted 1,006 tests and tablet results are recorded in the RV2 log and archived at `app/build/review-evidence/rv2/`. S2-F must revert exclusion `591429e49` to restore S2 before fixing it; merging its already-merged ancestor branch alone will not restore the excluded code. S1 remains deferred pending explicit extension-trust approval.

Cards, in order: **R15, S1, S2, S3, R13**. R15 changes backups and S1 classifies the library: back up first. R13
is T4 (offline dictionary packs): read every storage and download path line by line; the pack itself is not in the
repo, so check the "no pack installed" state and that nothing downloads without a tap.
**QA findings (Codex RV2, 2026-11-10)**

- **RV2-01 · R15 · Japanese backup actions truncate to the same label.** On the S22 in Colour, open Settings → Data and storage and find the backup actions. The earlier candidate showed `バックアップ…` on both tiles. `TsuzukiBackupTiles.kt` constrained these labels to a single ellipsized line. The wrapping change already in the RV2 worktree showed full Create and Restore labels in both Dot and Colour on the S22. See `.worktrees/rv2-batch-t/app/build/review-evidence/rv2/r15-phone-and-boards.png`, `s22-rv2-backup-labels-dot.png` and `s22-rv2-backup-labels-colour.png`. The status-card overlap is a separate issue (RV2-09).
- **RV2-02 · R15 · Scheduled retention can delete a manually saved backup (pre-existing).** Save a manual export with its default filename in the automatic backup directory, then let it age out of the retention window. `BackupCreator.getFilename()` uses the same legacy filename format that `TsuzukiBackupName.kt` accepts for automatic rotation. An executable check against the Preview classes put the manual filename in the deletion list at Keep 1. This check used synthetic filenames and did not delete any backup. See `qa-edge-cases.txt` in the RV2 evidence folder.
- **RV2-03 · S3 · Hard-wrapped plain-text paragraphs are split in the recap.** With two prose paragraphs separated by a blank line and two lines per paragraph, `NovelRecap.kt` returns only the last three lines as individual paragraphs. It drops the first line even though it belongs to the first of the two paragraphs. Reproduced against the Preview classes. See `qa-edge-cases.txt`.
- **RV2-04 · R13 · Incomplete SQLite schema passes pack inspection.** A schema-1 English database with valid metadata and one `term` row passes all of `DictionaryPacks.inspect()`'s queries while missing `exception`, `entry` and `sense`. Given matching manifest bytes, size and SHA-256, installation can replace a working pack with this file. The next English lookup hits a missing table, and `readPack()` reduces the error to an empty result. Reproduced the inspection/lookup SQL against an in-memory fixture. See `qa-dictionary-schema.txt`.
- **RV2-05 · R13 · Common Japanese adjective connective does not resolve to its headword.** A lookup for `高くて` should try `高い`. `DictionaryTerms.kt` currently generates the original form and `高くる`. In a pack with `高い` and no exact surface entry, lookup can miss the adjective or fall back to a shorter unrelated match. Reproduced the candidate generation against the Preview classes. See `qa-edge-cases.txt`.
- **RV2-06 · R15 · Restore offers to import an unrelated gzip file.** On the Tab S11 in Dot, opening `Tsuzuki-RV2-unrelated.tachibk` (gzip containing one unknown protobuf field and no backup records) showed 0 books, 0 shelves, 0 history entries and an enabled Restore button. `BackupFileValidator.kt` returns valid contents when it has recognized zero backup fields. Tested in the separate RV2 Preview package and stopped before restore. See `tablet-rv2-unrelated-restore.png` and the synthetic fixture in the RV2 evidence folder.
- **RV2-07 · S3 · A local novel's previous chapter is missed when there is no recap cache.** On the Tab S11, restore the synthetic local novel with a chapter 1 history entry 48 hours old and both chapter files already in `localnovels`. Open chapter 2 in Dot TextView before opening chapter 1. Chapter 2 starts at its own first sentence with no recap. `NovelRecapSession.prepare()` checks `LocalNovelSource.getPageList()` for a non-null `Page.text`. That source returns a URL-only `Page`, whose `text` defaults to null. The normal local reader calls `fetchPageText()` to open the local file. See `Tsuzuki-RV2-recap.tachibk`, both chapter fixture files, and `tablet-rv2-recap-cold-local-clean.png`.
- **RV2-08 · Reader · The toolbar covers the first highlighted line during TextView read-along.** On the tablet in Dot, open the short local chapter at its start, show reader controls and tap Listen → from the beginning. The highlight appears, but the title bar covers most of the text on the first line. With controls hidden, the line is readable. See `tablet-rv2-listening-playing.png` and `tablet-rv2-recap-cold-local-clean.png`. The same playback capture shows the duplicate `CH. 2: CHAPTER 2: RETURN` label tracked by CHAPNUM and the yellow default TTS highlight previously logged in `docs/RUNTIME_QA.md`.
- **RV2-09 · R15 · Last backup is painted behind the Create and Restore tiles.** On the S22 in Dot or Colour, open More → Data and storage → Backup and restore. The status card is underneath the actions. Dot exposes its border and part of the heading through the gap. Colour's translucent tiles allow the status text and icons to collide with the action labels. `TsuzukiBackupTiles.kt` emits `BackupStatusTile` and the action row as separate roots in a single custom preference item. See `s22-rv2-backup-labels-dot.png` and `s22-rv2-backup-labels-colour.png`.
- **RV2-10 · Data and storage · LNReader backup description is English in Japanese (pre-existing).** Both S22 theme captures show the Japanese row title with an English subtitle. `SettingsDataScreen.kt` hardcodes the fallback subtitle when no import is in progress. The literal is also in the main baseline. See `s22-rv2-backup-labels-dot.png` and `s22-rv2-backup-labels-colour.png`.
- **RV2-11 · Reader · WebView selection has low contrast in Dot.** Long-press a word in the tablet's Dot WebView reader. The selected glyphs become black against a dark blue selection fill. Sampling the captured word gives `#000000` on `#14485C`, about 2.11:1 contrast. See `tablet-rv2-webview-selection.png`.
- **RV2-12 · S3 · A recap changes the resumed text in paged WebView.** A Chromium fixture ran the candidate's recap script, paging script and multi-column CSS at 390 × 650. The same chapter and saved 60% position showed paragraphs 22–24 without a recap and paragraphs 21–23 with one. The card changed the layout from 14 to 15 pages. `paged-reader.js` derives the landing page from the total page count, which includes the inserted recap. The Dismiss button called the bridge once, and repeating the restore after dismissal returned to paragraphs 22–24. This isolates the layout change in Chromium. An Android paged-reader retest is pending. See `check-paged-recap.cjs` and `qa-paged-recap.txt` in the RV2 evidence folder.
- **RV2-13 · Reader · The compact Listen controls have no sleep timer (pre-existing).** Start speech in the tablet's 753 dp portrait reader. The compact dock has hide, previous paragraph, play/pause and next paragraph, with no timer action. The Dot and Colour ListenMini phone boards both show the timer. The only production control that calls `setSleepTimer()` is `TsuzukiReaderListenPanel`, which is mounted for a supported fold or a tablet window at least 900 dp wide. The main baseline has the same restriction. See `tablet-rv2-listening-pause-resume-comparison.png` and `docs/design/boards/png/06-reader/dot/ListenMini.png`.
- **RV2-14 · S2 · One autoplay run can count more chapter completions than the novel contains.** In a fresh tablet scratch install, restore `Tsuzuki-RV2-listening.tachibk` and its two local chapters. Start TextView Listen from chapter 1, pause and resume once, then leave auto-next and infinite scroll on. The later reader capture still showed chapter 1, and Stats reported 5 listened chapters. The fixture contains exactly 2 chapters and neither was explicitly replayed. The capture proves excess completions. The exact handoff sequence still needs a trace. See `tablet-rv2-listening-pause-resume-comparison.png`, `tablet-rv2-listening-followup-after-handoff.png` and `tablet-rv2-listening-followup-stats.png`.

**Verification so far**

The offline JDK 21 `:app:testDebugUnitTest` rerun passed 1,007 tests with zero failures, errors or skips. Both Python dictionary converter tests and both changed reader scripts' JavaScript syntax checks passed. RV2's earlier Debug and Preview builds passed. The tablet opened the already-installed A4.3a Preview to Dot Home without a new crash. RV2 ran on the Tab S11 and wireless S22 in a separate ARM64 Preview package (`io.github.manojx2005.tsuzuki.rv2scratch`). On the tablet, the valid synthetic backup preview showed 3 books, 1 shelf and 1 history entry. Restore populated the scratch Library, and Manhwa showed only the fixture tagged Webtoon, with the Manhwa badge. TextView and WebView selections both opened the no-pack dictionary sheet with system and web fallbacks. TTS finished the synthetic chapter and Stats recorded 1 chapter and 0 whole listening minutes. Replaying it in incognito left the listening count at 1. The S22 displayed both complete Japanese backup action labels in Dot and Colour. Create saved a 532-byte scratch backup and the Restore picker loaded its preview without applying it. The status text with the actual date and size still overlapped (`s22-rv2-backup-after-export.png`). Neither device logged a scratch-process crash. Evidence is in `.worktrees/rv2-batch-t/app/build/review-evidence/rv2/`.

Both temporary installs and fixture folders were removed. The tablet returned to its existing Preview on Dot Home. R15, S3 and R13 are held for the findings above. Their follow-up cards are R15-F, S3-F and R13-F. S1 still needs a live Keiyoushi title and the manual override check. S2 still needs background playback, timer, pause/resume and chapter handoff on device. Positive recap rendering, dismissal and paged resume also remain open. RV2 has no merge verdict yet.

#### RV2 fix plan (Claude, 2026-11-10) — read before taking R15-F, R13-F or S3-F

Claude read the code behind the RV2 findings in `.worktrees/rv2-batch-t` (branch `review/rv2`, all five cards
merged). Causes below are from reading the code. **Rows 1–7 are applied in the worktree but uncommitted and never built** (11 files, see "RV2 — how to finish"). Work on `review/rv2`
(one `review: <card> <fix>` commit per card), so RV2 can close without three more branches. Whoever takes a row
writes their name on it first. Each fix needs a unit test that fails before it.

| # | Finding | Cause (file) | Change | Owner |
|---|---|---|---|---|
| 1 | RV2-09 status card behind the tiles | `TsuzukiBackupTiles.kt`: status tile and action `Row` are two roots inside one `CustomPreference` slot, so they overlap | Wrap both in one `Column`. Keep the uncommitted label-wrap edit (RV2-01) | Claude |
| 2 | RV2-06 unrelated gzip accepted | `BackupFileValidator.validate`: unknown fields are skipped and nothing checks that any field was recognised | Count fields 1, 2, 101, 104, 105, 106; zero recognised → `IOException(invalid_backup_file_unknown)`. Settings-only backups stay valid | Claude |
| 3 | RV2-02 retention deletes manual exports | `TsuzukiBackupName.backupFileRegex` accepts the short `HH-mm` name that `BackupCreator.getFilename()` gives manual exports | Rotate only the scheduler's `HH-mm-ss-SSS` names. Old short-named automatic backups are then never rotated (safe side); update `TsuzukiBackupNameTest` | Claude |
| 4 | RV2-03 hard-wrapped text split per line | `NovelRecap.novelRecapParagraphs`: plain text uses `text.lines()` | Split on blank lines when the text has any; fall back to lines only when it has none | Claude |
| 5 | RV2-07 local novel has no recap without cache | `NovelRecapSession.prepare` reads `Page.text` from `LocalNovelSource.getPageList()`, which returns URL-only pages | Call `fetchPageText(page)` on the local source | Claude |
| 6 | RV2-05 `高くて` misses `高い` | `DictionaryTerms.japaneseEndings` has no `くて` rule; the generic `て` → `る` gives `高くる` | Add `"くて" to listOf("い")` before `"て"`; test in `DictionaryTermsTest` | Claude |
| 7 | RV2-04 incomplete pack passes inspection | `DictionaryPacks.inspect` queries only `metadata` and `term` | In `inspect`, run the real lookup join (`term` → `entry` → `sense`, with `ordinal`, `part`, `gloss`, `headword`, `reading`) and require one row; for English also `SELECT form, lemma FROM exception LIMIT 0`. A missing table or column throws `SQLiteException`, which `readPack` already turns into "invalid", so the working pack is kept | Claude |
| 8 | RV2-12 recap shifts the paged WebView resume | `paged-reader.js` derives the landing page from the total page count, which includes the recap card | Restore by anchor/offset of the chapter body, not by page fraction of the whole document; re-run `check-paged-recap.cjs`, then Android | Codex (S3-F) |
| 9 | RV2-14 listened chapters over-counted | Not traced. Suspect the chapter-finished callback firing per hand-off attempt with infinite scroll + auto-next | Trace `TtsController` hand-off with the RV2 fixture; count a chapter once per (chapter id, playback run) | Codex (new card S2-F) |
| 10 | RV2-08 toolbar covers the highlighted line | Not traced; TextView read-along scroll ignores the top bar height | Offset the scroll target by the visible top bar | Codex (reader polish card) |
| 11 | RV2-10 English LNReader strings | `SettingsDataScreen.kt` L269, L307, L385–387 hardcode English (Tsundoku's code) | New `tsz_` strings in 8 locales; smallest possible upstream edit | Codex (T1) |
| 12 | RV2-11 Dot WebView selection contrast | Not traced; `NovelWebViewStyler` sets no `::selection` colours for Dot | Token-based `::selection` background/text | Codex (reader polish card) |
| 13 | RV2-13 compact Listen dock has no sleep timer | `TsuzukiReaderListenPanel` (the only caller of `setSleepTimer()`) mounts only ≥ 900 dp or on a fold | Add the timer action to the compact dock per board `06-reader/dot/ListenMini` | Codex (reader polish card) |

After rows 1–7: `:app:testDebugUnitTest` and `:app:assemblePreview` on `review/rv2`, then the scratch-package device
checks from the RV2 card (S22 Dot + Colour backup screen; unrelated-gzip restore; local-novel recap on the tablet).
Still open for the RV2 verdict after that: S1 with a live Keiyoushi title and manual override; S2 background
playback, timer and hand-off (blocked by row 9); recap rendering and dismissal in both renderers (blocked by row 8
for paged WebView). S1 can merge alone if its device check passes; S2 and S3 wait for rows 8–9.

**Also uncommitted on `main` (Claude, owner request 2026-11-10): no default repos in shipped builds.**
`tsuzuki/DefaultSourceRepos.kt` seeds only when `BuildConfig.DEBUG` (release, preview and foss add nothing; debug
keeps seeding — owner confirmed). Repos an earlier build added are left alone. New test in
`DefaultSourceReposTest`; README step 4, CHANGELOG (Unreleased) and the AGENTS.md Sources rule updated. Not yet
compiled or tested. Device check needed: a fresh preview install shows an empty Browse whose empty state leads to
"add repository" on the novel and manga tabs. Review-lane scratch installs now start with no repos.

#### RV2 — how to finish (Claude, 2026-11-12; review lane)
The worktree `.worktrees/rv2-batch-t` (branch `review/rv2`, R15 + S1 + S2 + S3 + R13 merged) holds Claude's
uncommitted fixes for fix-plan rows 1–7: `TsuzukiBackupTiles.kt`, `BackupFileValidator.kt`, `TsuzukiBackupName.kt`,
`DictionaryPacks.kt`, `DictionaryTerms.kt`, `NovelRecap.kt`, `NovelRecapSession.kt` and four test files. They were
never compiled. Don't discard them.
1. Read the diff (`git diff`) against the fix-plan table. Each row needs a test that fails without the fix; rows
   2 (unrelated gzip), 5 (local-novel recap) and 7 (incomplete pack) have **no test yet** — write them
   (`BackupFileValidatorTest`, `NovelRecapSessionTest`, `DictionaryPacksTest`; follow the existing tests' style).
2. `git merge --no-ff tsuzuki/s3-f` (row 8, paged recap resume). Conflict in `NovelRecap*.kt` → keep both changes.
3. `./gradlew :app:testDebugUnitTest :app:assemblePreview`. Fix what your review finds; one commit per card:
   `review: R15 <fix>`, `review: S3 <fix>`, `review: R13 <fix>`.
4. Devices, in the scratch package `io.github.manojx2005.tsuzuki.rv2scratch` as before (never the owner's
   `.debug` or `.dev` data): re-run RV2-01, -02, -03, -05, -06, -07, -09 and -12; each must now pass. Then the
   checks RV2 never reached: S1 with a live Keiyoushi long-strip title + manual override; S2 background playback,
   timer, pause/resume; recap shown and dismissed in TextView, WebView scroll and WebView paged.
5. Verdicts. R15, S3, R13 pass if their findings are gone. **S2 fails on RV2-14** (over-counted chapters) unless
   you can show it is fixed: revert its merge, card S2-F below. S1 passes or fails on its device check alone.
   Findings RV2-08, -10, -11, -13 are pre-existing: they don't fail a card; they are RP1 and STR1 below.
6. Close RV2 as in Batch T step 7 (fast-forward `main`, log entry, status table, no push).
The S22 is on wireless adb: get the IP:port from the owner if `adb devices` doesn't show it, and confirm
`getprop ro.serialno` = `RZCWA139X1X` before installing.

#### RV4 Review: fix cards + owner reports (T3) — `done` (2026-11-13; V7.7-F, CHAPNUM, MAIN-REPOS accepted; other gates deferred)
**Final validation 2026-11-13:** Candidate 1,026 tests; accepted Debug + optimized scratch Preview built, **1,011 tests, 0 failures/errors/skips**, scoped Spotless twice. Tablet-only checks per owner; crash buffer empty; both scratch apps removed and daily Preview returned to Dot Home. Evidence archived and size-verified (225 files) in `app/build/review-evidence/rv4/`.

Cards, in order: **V7.7-F, R12-F, R14, CHAPNUM, TTS-LANGUAGE, NT1 (= S4), MAIN-REPOS**. (S3-F is merged in RV2.)
- V7.7-F and R12-F replace the cards RV1 reverted: merge `tsuzuki/v7.7` then `tsuzuki/v7.7-f`, `tsuzuki/r12` then
  `tsuzuki/r12-f`, and re-run the RV1 checks that failed (WebView chapter-end tap; source health with a native
  Keiyoushi source).
- R14 is T4 (merge sources for one book): back up first; read every DB write line by line.
- CHAPNUM and TTS-LANGUAGE are `codex-working` with commits on their branches. If the worktree has uncommitted
  work, the implementer thread is not finished: skip the card and say so. TTS-LANGUAGE device check: Japanese
  device, an English chapter is read with an English voice, a Japanese chapter with a Japanese voice, an
  explicitly chosen voice is kept.
- **MAIN-REPOS**: the uncommitted change in the **main checkout** (`DefaultSourceRepos.kt`, its test, README,
  CHANGELOG, AGENTS.md: no default repositories in shipped builds, owner 2026-11-10). Review it like a card: run
  `DefaultSourceReposTest`, fresh preview install shows an empty Browse whose empty state leads to "add
  repository" on the novel and manga tabs, debug still seeds. Then commit it on `main` as
  `Ship no default repositories outside debug builds`. `docs/HANDOFF.md`, `docs/ANIME_ANIYOMI_PARITY.md` and
  `docs/design/boards/fixes-2026-11b/` are also uncommitted on `main`: commit them as `docs: plan 0.1.1, boards 2026-11b`.
Review (Codex RV4, 2026-11-13): Accepted V7.7-F and CHAPNUM. MAIN-REPOS committed on main as `7f697fef9`; owner plan/boards as `3e610bd08`. Review fix `7f51d31da` restores the manga Add Repository action in Tsuzuki themes: fresh Preview has no repositories on either tab, both add flows open; Debug still seeds LNReader/Keiyoushi; 7 seed tests pass. R12-F deferred for native live-source trust approval (and missing decode warning), R14 deferred for two-source runtime checks, S4/NT1 deferred for literal native-string gate clarification; exclusions `89e1ac9ac`, `451168815`, `911a2222f`. TTS-LANGUAGE skipped because its worktree remains dirty. S3-F already passed RV2. Native bottom-card overlap is pre-existing and added to RP1. No push.

#### Neural TTS in 0.1.1 (owner 2026-11-12) — read before NT1–NT4
The owner wants the offline natural voice (Kokoro through sherpa-onnx, S4) **shipped in 0.1.1**. It is optional:
system TTS stays the default and the fallback, the voice pack is a download and never part of the APK, and the
conditions in `docs/research/neural-tts.md` stay hard rules (sherpa-onnx ≥ 2.0.1, no espeak-ng anywhere, lexicon
voices only, licences in About). **Three things can stop it, and each has a rule:**
- The licence of `lexicon-us-en.txt` / `lexicon-gb-en.txt` is UNVERIFIED in the research. We may not publish our
  own pack until NT2 settles it. Not settled → 0.1.1 ships without the neural voice (the code stays, the entry
  stays debug-only) and nobody decides otherwise but the owner.
- Speed has never been measured on a phone. NT2 measures it. Slower than real time at 1.5× on the S22 → same rule.
- Nobody has heard it. The owner's listening test (NT2 step 4) is the go / no-go.

#### NT1 Review S4 (review lane) — `blocked` (NT1-BINARY candidate awaits independent review, 2026-11-14)
Review `tsuzuki/s4` as a T4 card: the private process, the AAR's SHA-256 against the research doc, no
`espeak`/`piper` string in any packaged `.so` (`strings | grep -i -E 'espeak|piper'`), 16 KB alignment, licence
files present, system TTS untouched when the opt-in is off. Device: opt-in with no pack → system fallback.
Don't download a model. Pass → merge (still debug-only).
Review (Codex RV4, 2026-11-13): See S4 review below. Pinned AAR contains literal espeak/piper matches; owner clarification pending. This gate is not waived.

#### NT1-BINARY Investigate a runtime satisfying the literal native-string gate (T3) — `review` (.worktrees/nt1-binary, 2026-11-14; d1e360ff4 + d580d500e; independent review required)

Inspect current official sherpa-onnx Android artifacts and source build options for a lexicon-only binary with no literal `espeak`/`piper` substring in packaged native libraries. Keep the raw gate unchanged, distinguish deprecated references/Speaker false matches from actual linked GPL code, and document reproducible source/dependency/string/16 KB checks for any candidate. Do not merge S4, publish a pack, download a model or waive NT2/owner listening gates. Another chat reviews any implementation produced here.
Result (Codex app lane, 2026-11-14): **review**, implementation `d1e360ff4` plus patch-whitespace rule `d580d500e` on `tsuzuki/nt1-binary`. Investigation produced a source-built candidate; **not an NT1 approval or app dependency change**.

- Audited all three official v2.0.1 Android AAR variants, SHA-256-verified against release metadata. Normal: 12/16 libraries fail the literal string gate; static-runtime: 4/5; RKNN: 6/10 have string matches and two additional libraries fail 16 KB alignment. The raw rule still rejects deprecated references and CreateSpeaker substring matches; no gate was relaxed.
- `scripts/neural-tts/kokoro-only-v2.0.1.patch` is a seven-file source profile: TTS/version JNI only, Kokoro factory/config only, a required lexicon, no legacy phonemizer directories, no external token/phoneme input routes, no unused legacy metadata reads. Removed APIs/paths are unavailable rather than renamed. Modified version/tag identity and Apache modification notices are retained. ONNX Runtime and C++ are statically linked; normal stripping removes debug information, with no post-link string edits.
- `scripts/build-kokoro-runtime.ps1` + preparation/packaging helpers pin source commit/archive and ten dependency hashes, preserve unknown edits, and rebuild under `.native/`. A cached **offline recipe run passes**. Official Java classes are byte-identical and **all 10 TTS/GeneratedAudio JNI exports match**. Candidate assets include modification/runtime/dependency notices. There are no app Gradle/source changes or voice models.
- Candidate `.worktrees/nt1-binary/.native/output/sherpa-onnx-2.0.1-tsuzuki-kokoro-arm64.aar`: **7,857,879 bytes**, SHA-256 `b2cee72b9131cf5190753c966e23e0c0e65ef7a0e9313e434d074d4aa3b5fa47`. JNI: **20,903,096 bytes**, SHA-256 `9aa11f94d53ad3bac95ae84e6349e3ff0486f63bba39154e10dbde5e6532ff6f`. Zero literal matches/runtime markers, three 0x4000-aligned PT_LOAD segments, DT_NEEDED only Android system libraries. Independent NDK LLVM strings/readelf agree; identical packaging inputs reproduce the AAR hash. Cross-path/toolchain native byte reproducibility is not established.
- **13 Python tests pass** (10 artifact checks, 3 source-integrity checks). Evidence `.worktrees/nt1-binary/app/build/nt1-binary-evidence/`; committed summary `docs/research/neural-tts-native-audit-20261114.json`; full recipe/limits `scripts/neural-tts/README.md`. No model downloaded, no synthesis or device test, no S4 exclusion revert, no app install, no merge/push.
- Independent reviewer: inspect patch/dependencies/notices; rebuild and compare the TTS API, raw string scan, ELF dependencies/alignment. Integrate only on a disposable review branch with S4 (excluded by `911a2222f`); this AAR is **arm64-only**, so unsupported ABIs must keep Android TTS. Verify worker/no-pack fallback and APK zipalign separately. Successful artifact checks do not settle the lexicon licence, performance, quality or owner listening decision. NT1/NT2/NT3/NT4 gates remain.


#### NT2 Neural TTS: licence, speed and the listening test (T3) — `ready` (needs NT1 merged; app lane; **gate for NT3/NT4**)
1. Lexicon licence: find the origin and licence of the two English lexicon files in
   `kokoro-int8-multi-lang-v1_1` (sherpa-onnx `tts-models` release notes, the k2-fsa scripts that generate
   them, the upstream dictionary they derive from). Write the finding with links into
   `docs/research/neural-tts.md`. Allowed outcome: a licence that permits redistribution (then add its text to
   `docs/licenses/neural-tts/`). Otherwise `blocked` with the facts; do not guess.
2. If step 1 passes, build the test pack with `scripts/package-kokoro-test-pack.py` (downloading the upstream
   archive from the official sherpa-onnx release is allowed for this card; verify its SHA-256 against the
   release page and record it).
3. Measure on the S22 and the tablet with the debug build (review lane runs it if you have no device access):
   real-time factor for a 180-character chunk at 1×, 1.5× and 2×, time to first audio after tapping Listen, peak
   memory of the worker process, battery over 30 minutes of listening compared with system TTS. Table in the
   research doc. Budget: first audio ≤ 2 s, no gap between chunks at 1.5×, worker ≤ 400 MB.
4. Leave the pack side-loaded on the S22 debug build and tell the owner how to listen (which chapter, which
   toggle). **Owner go / no-go** is written under "Owner decisions in force".
Result: _pending_

#### NT3 Neural TTS for everyone: engine in release builds + Voices page (T4) — `blocked` (needs NT2 go; board 5 `Voices` approved by the owner 2026-11-13)
Board 5 in `docs/design/boards/fixes-2026-11b/`. Move the engine from the debug source set to main:
1. Packaging: the sherpa-onnx AAR for **arm64-v8a only** in release/preview/foss (other ABIs show only the phone
   voice tile, no download). Record the APK size before and after per ABI in Result; more than +20 MB on arm64 →
   stop and ask the owner. Keep rules for the JNI classes; 16 KB check; licences in About (V5's screen).
2. Pack manager `tsuzuki/tts/pack/`: manifest (URL, size, SHA-256, version) fetched from the Tsuzuki release
   asset NT4 publishes; download on Wi-Fi by default with pause/resume/cancel, free-space check (2× pack size),
   SHA-256 verified before install, atomic swap, delete, survives process death (WorkManager). Nothing downloads
   without a tap. Follow R13's pack code where it does the same job (and RV2's findings about validating before
   replacing).
3. Voices page per board 5: engine tiles, download states, voice rows with a one-sentence sample, "dialogue in
   a second voice" (quoted speech uses the second speaker; off by default), delete. Same entry from the Listen
   panel. 8 locales.
4. Routing: the neural voice is used only when the chapter's detected language (TTS-LANGUAGE's detector) is one
   the pack covers; otherwise the phone voice, with one line in the Listen panel saying which voice is speaking.
   Any synthesis failure falls back to the phone voice for that chunk, as S4 does.
5. Parity: sleep timer, read-along highlight, pause/resume, paragraph seek, chapter hand-off, listening stats
   (S2/S2-F) and the listening widget behave the same on both engines; one test per item with a fake engine.
6. Pronunciation: the per-book replace rules (existing ReplaceRules screen) are applied before synthesis on both
   engines; pauses: 250 ms after a paragraph, 600 ms at a scene-break line; footnote markers and URLs skipped.
Device script: fresh install → Voices → download on Wi-Fi, pause, resume, kill the app mid-download, finish →
pick a voice, hear the sample → Listen on an English chapter (neural) and a Japanese chapter (phone voice) →
airplane mode still works → delete the pack → Listen falls back. Dot and Colour, phone and tablet.
Result: _pending_

#### NT4 Publish the voice pack (review lane prepares; owner uploads) — `blocked` (needs NT2 go)
Build the final pack zip (no `espeak-ng-data/`, licence files inside), its SHA-256 and the manifest JSON, and
write the exact upload steps for the owner: a GitHub release on the Tsuzuki repo tagged `voices-v1` with the zip
and the manifest as assets. **Only the owner creates the release.** After the upload, verify the public URL and
hash from a device and put the URL in NT3's manifest constant.
Result: _pending_

#### S2-F Count a listened chapter once (T3) — `done` (Codex RV5, 2026-11-14)
RV2-14: one autoplay run counted 5 chapters for a 2-chapter book. Trace `TtsController`'s chapter hand-off with
infinite scroll + auto-next on (log each "chapter finished" with chapter id and a playback-run id). Count a
chapter at most once per (chapter id, playback run); a run ends on stop, on a manual chapter change, or when the
reader closes. Tests: hand-off fired twice → one count; replay after stop → second count; incognito → none.
Device script in Result (review lane runs it): the RV2 fixture `Tsuzuki-RV2-listening.tachibk`, expected Stats = 2.
Result (Codex app lane, 2026-11-13): `2f7b56a4c`; S2 restored on this branch by `252ff29b8` (do not separately revert its main exclusion when merging S2-F).

- `ListeningSession.kt` associates queues with chapter IDs and a playback-run ID. Repeated queues for a completed chapter count once until the run ends; real speaking duration still accumulates. Debug completion logs include chapter, run, queue and whether the completion was counted.
- `TtsController.kt` ignores duplicate/interrupted completion callbacks before scheduling auto-next. Pause/resume and seek preserve the run; Stop/manual navigation and viewer destruction end it. Incognito callbacks still hand off but do not write listening data. Unknown chapter IDs retain playback/time behavior without inventing a chapter count.
- `NovelWebViewViewer.kt` preserves the run for its explicit autoplay full-document handoff; user-driven navigation retains the normal Stop path.
- Tests cover repeated same-chapter queues, duplicate callback acknowledgement, two distinct chapters, pause/seek, replay after Stop, stale callbacks, incognito, mid-queue privacy changes, unknown chapter IDs, and the controller's stop/close/sleep-timer paths. Debug build + **1,024 unit tests, 0 failures/errors/skips**, two scoped Spotless passes; five restored labels present once in all eight locales. Evidence `.worktrees/s2-f/app/build/s2-f-evidence/`.
- No adb used in this app card. The exact device event sequence behind RV2-14 remains for independent review; the code fix and deterministic regression do not claim a runtime pass.

Independent reviewer device script (tablet for now; S22 deferred by owner):
1. Use a disposable install, restore `app/build/review-evidence/rv2/Tsuzuki-RV2-listening.tachibk`, and copy its `listen-1.txt` / `listen-2.txt` into the dedicated SAF folder's `localnovels/RV2 Listening Test/`. Enable infinite scroll and auto-next; choose an installed English system voice explicitly while TTS-LANGUAGE is pending.
2. Start chapter 1, pause/resume once, let both chapters finish. Capture `TTS chapter finished` logs: the run ID must stay the same across queue and chapter handoffs; only two distinct chapter IDs may have `counted=true`. Stats listened chapters must be **2**, in both renderers. Any automatic stop/restart that changes the run unexpectedly needs a follow-up trace.
3. Stop and explicitly replay one chapter: it starts a new run and adds one completion. Repeat in incognito: totals stay unchanged. Check timer stop and manual chapter navigation end the previous run. Restore/remove only the disposable app and leave daily data alone.


Review (Codex RV5, 2026-11-14): Pass. Reviewed restoration and queue/run accounting, retaining both timer constructor parameters and test sets. Prior tablet Native/WebView and S22 replay/incognito evidence plus this takeover’s S22 Native pause/resume trace show two distinct completions in one run, followed by Stop after the final chapter; no last-chapter replay. Combined suite: 1,085 tests, zero failures/errors/skips. Evidence: `app/build/review-evidence/rv5/` (`c-tablet-A-native-trace.txt`, `c-tablet-E-webview-trace.txt`, S22 Stats/replay/incognito captures and `s22-rv5-caseb-*`).

#### RP1 Reader polish from RV2 (T3) — `done` (Codex RV5, 2026-11-14)
1. RV2-08: during TextView read-along the top bar covers the highlighted line. Scroll target = line top −
   (visible top bar height + 16 dp). No board needed.
2. RV2-11: Dot WebView selection is black on dark blue (2.1:1). Set `::selection` and the TextView highlight
   colour from tokens (board 3, "Selection" row). The yellow TTS highlight becomes the token colours in board 4.
3. RV2-13: the compact Listen dock has no sleep timer. Add the timer chip per board 4 (`TabletListenDock`) and
   the phone board `06-reader/ListenMini`; it calls the same `setSleepTimer()` as `TsuzukiReaderListenPanel`.
4. RV4-01: the Native chapter-end card can remain under visible bottom reader bars at maximum scroll. Reserve the measured visible bottom chrome height plus 16 dp in scroll content so Next, Mark read and Contents stay tappable; remove the extra space when chrome hides. Keep story progress independent of the card. Existing Q2 ChapterEnd / TabletReader boards apply. Evidence `app/build/review-evidence/rv4/tablet-rv4-preview-native-secondary-actual-bounds-20261113.png`; hiding chrome exposes the card and Contents opens correctly. Card vertical layout is unchanged by V7.7-F, so this is a pre-existing polish issue.
Tests: scroll-offset maths; timer chip label formatting; selection CSS contains the token colours.
Result (Codex app lane, 2026-11-13): **review**, code `54810e270` on `tsuzuki/rp1`. Native read-along uses measured top chrome plus 16 dp; chapter-end scroll content reserves measured visible bottom chrome plus 16 dp and releases it when hidden. Shared token feedback drives Native selection/ink and both renderers' read-along; WebView selection CSS overrides source CSS. New board-based phone/tablet Listen dock shows the existing sleep choices and a live countdown from the controller deadline; actual chapter/paragraph/speed only. Thin AppBars slot preserves legacy UI. Three strings translated in all 8 locales. Debug APK and **1,017 tests** pass (zero failures/errors/skips), scoped Spotless twice; evidence `.worktrees/rp1/app/build/rp1-evidence/`. No adb or self-review.

Independent reviewer: check Dot/Colour Native highlighted line with bars opening during speech, selection fill/ink/handles in Native and WebView, final card Next/Mark read/Contents with bottom bars shown/hidden and unchanged story progress. Check timer Off/Chapter/15 minutes, actual decreasing countdown and pause/resume, phone dock fit and tablet controls. Read from here remains available in reader actions. S22 deferred by owner. On merge with S2-F, retain both ListeningSession and elapsed-clock constructor arguments and both timer-test additions; neither card subsumes the other.

Review (Codex RV5, 2026-11-14): Pass with review fixes `03cd237dd`, `498430910` and `0a96ace3d`. The dock follows the playing chapter; Native autoplay stops at the final chapter. The first line now has actual top clearance (the previous clamped scroll target still clipped it at chapter start), and measured floating Listen height is included in bottom clearance. Dot/Colour, tablet/S22, Native/WebView selection/read-along and timer evidence inspected. S22 Native card Next/Mark read/Contents exercised above both dock and bars; story progress remains 100%. Countdown visibly decreases (15:00 → 14:57; Colour 14:55). 1,085 tests pass. Evidence `app/build/review-evidence/rv5/`, notably `s22-rv5-caseb-first-line-fixed.png`, `s22-rv5-caseb-card-clears-dock.png`, `s22-rv5-caseb-preview-colour-readalong.png` and prior `c-tablet-F-*`.

#### STR1 Translate the LNReader import strings (T1) — `done` (Codex RV5, 2026-11-14)
RV2-10: `SettingsDataScreen.kt` L269, L307, L385–387 hardcode English. New `tsz_` strings in all 8 locales;
smallest possible edit to the upstream file. Check the same screen for any other literal.
Result (Codex app lane, 2026-11-13): `77ede839d` localizes the LNReader chooser, started status, import preference and entire options dialog. The same-screen audit also found CSV export labels; those now use resources too. 18 new `tsz_` keys in all 8 locales, existing Categories/Downloaded chapters reused. Upstream edit is 21 line replacements; no layout changes. `:app:compileDebugKotlin` and two scoped Spotless passes succeed. XML audit confirms 18 unique keys per locale and no remaining UI string literals in this screen. Evidence `.worktrees/str1/app/build/str1-evidence/`. No new tests for this copy-only change; no adb. Reviewer: Japanese Data screen chooser title, temporary import-started status, import-options dialog and CSV options, then another app language. No actual owner backup import required.

Review (Codex RV5, 2026-11-14): Pass with `bf5055dcd`: CSV now has its own “Select columns to export” title in all 8 locales. Japanese LNReader options/chooser/data evidence inspected; Japanese and English CSV dialog labels verified on S22. Eight-locale key uniqueness and XML audit pass; 1,085 combined tests pass. Evidence `app/build/review-evidence/rv5/` (`tablet-rv5-str1-japanese-options.png`, `s22-rv5-caseb-str1-ja-csv.png`, `s22-rv5-caseb-str1-en-csv.png`). No owner backup imported.

#### ERR1 Errors and long waits say what happened (T3) — `done` (Codex RV5, 2026-11-14)
Boards 1 `ErrorState` and 2 `LoadingStages`. One composable pair in `presentation/tsuzuki/components/`
(`TsuzukiErrorState`, `TsuzukiLoadingStages`) and one pure mapper `tsuzuki/error/ErrorCopy.kt`: Throwable →
(what failed, why in one sentence, technical line, primary action). Cases: no network, timeout, HTTP 403/503 with
a Cloudflare/DDoS-Guard challenge → "Open in WebView", HTTP 404, HTTP 5xx, parse error ("the site changed; update
the extension"), extension missing/untrusted, storage full/permission, anything else → the exception's message,
never "Unknown error". Technical line: status · message · host, no query string. Use it in: novel and manga
catalogue + global search, book details refresh, chapter open in both novel renderers and the manga reader,
downloads list rows. Dispatch from the existing upstream empty/error composables with a small hook
(`presentation/components/EmptyScreen` pattern), no re-indent. LoadingStages appears only after 2 s, and turns
into ErrorState after 30 s on one step. 8 locales. Tests: the mapper table; redaction; 2 s / 30 s thresholds.
Device script: airplane mode on a catalogue; a Cloudflare-protected source; a removed extension's book.
Result (Codex app lane, 2026-11-14): **review**, code `ad3e6a871` on `tsuzuki/err1`. Independent review required; no merge or push.

- `tsuzuki/error/ErrorCopy.kt` classifies wrapped network/timeouts, 403/503 browser challenges, 404/server/parse failures, unavailable or untrusted extensions, full storage and denied access. Display and clipboard diagnostics remove URL paths/queries, credentials, auth/cookie headers and stack frames; cause chains and message lengths are bounded. Normal cancellation propagates; the reader's timeout is classified as a timeout.
- `presentation/tsuzuki/components/TsuzukiErrorState.kt` implements approved boards 1/2 using shared tiles and pills, typography and tokens. Two-second delayed stages, per-step seconds and 30-second timeout; retry/new-operation/new-step resets the clock. The shared tile keeps its original positional API, with an additional overload for these boards' 24 dp corners.
- `TsuzukiOperationState.kt` adapts catalogue/global-search/details, with working extension/storage routes where hosted navigation is available. Details keeps cached data and shows the real refresh failure. Global-search Retry opens the same source with the current search query. Only observable steps appear; the adapters do not invent source/parse/layout completions.
- `TsuzukiReaderOperation.kt` adapts Native, WebView and manga holders. WebView loading/error UI sits outside the document so it cannot be spoken or counted as story progress. A full WebView failure stops speech/autoscroll; real document readiness removes the overlay. Native actual inline load failures reuse the card; WebView inline diagnostics reuse the mapper and token colours. Existing retry/WebView handlers remain wired. Search clocks reset on query change; manga clocks reset on reuse/hide.
- Novel/manga download rows show concise mapped causes and copy redacted diagnostics. **23 new keys in all 8 locales**, with identical placeholders. Debug APK and **1,039 tests, 0 failures/errors/skips**, including **28 mapper/redaction/threshold tests**, pass. Two scoped Spotless passes succeeded; unrelated upstream formatter edits restored, then final APK/tests rerun. Evidence `.worktrees/err1/app/build/err1-evidence/` (`build-tests-final.log`, `tests-final.txt`, `locale-audit.json`, formatter logs). All edited Kotlin uses UTF-8/LF.
- Independent device review: tablet + S22, Dot then Colour. Offline catalogue: cause and Retry; browser-challenge source: WebView then Retry; removed extension's cached book: Extensions route. Slow request: spinner before 2 s, real stage/seconds after, timeout at 30 s, retry/leave/reopen and new search reset it. Check Native/WebView chapter failure/retry and inline next-chapter failures; ensure error text is never spoken and story position is retained. Manga paged/strip: failed page retry, WebView, recycling/scroll back. Download rows: localized cause and copied diagnostic has no query/token. Check portrait/landscape and larger font sizing. No app-lane runtime verdict is claimed.
- Reviewer: add newly touched upstream adapters to AGENTS' touch list when merging: `presentation/browse/MigrateSearchScreen.kt`, `ui/reader/viewer/ReaderProgressIndicator.kt`, `viewer/pager/PagerPageHolder.kt`, `viewer/webtoon/WebtoonPageHolder.kt`, `viewer/text/textview/NovelTextViewInlineFeedback.kt`, `viewer/text/webview/NovelWebViewInlineFeedback.kt`. Other hooks are in already-listed files. Watch the Native loading-view expression formatting; no upstream body was wrapped/reindented as a new dispatch.

Review (Codex RV5, 2026-11-14): Pass with the prior independent review fixes and `8570ff5d7`. Reader waits now use neutral Opening copy, manga progress reaches the themed loading tile, and WebView keeps its chapter-end snapshot while target pages are being prepared (including repeated loaded-state callbacks). Controlled localhost fixture verified stages at 4s, timeout, HTTP 403 → WebView, HTTP 500 → reader error → Retry recovery. Tablet missing-source cached details → Extensions; S22 Native missing local chapter → Retry recovery after restoring the synthetic file. A synthetic failed download showed its Japanese cause, Copy action and successful Retry. Manga paged offline error and Retry exercised; DNS failure persisted briefly online, then images rendered in webtoon mode. Webtoon render/recycling smoke checked; a separate forced webtoon error, live external browser challenge, inline-append failure and full large-font/landscape matrix were not established by this sample. These remain broader runtime QA coverage, not claims of this verdict. Debug, optimized Preview and 1,085 tests pass; Preview 16 KB APK alignment passes, SHA-256 `6f519bc92772f28e31f7ca5db8796dac2be497feaf48d6b85554c61a34dcea96`. Evidence `app/build/review-evidence/rv5/`. Both final crash buffers empty; daily Preview/Debug and owner phone untouched.

#### RV5 Review app fixes for 0.1.1 (review lane) — `done` (Codex independent Case B takeover, 2026-11-14)

Plan 0.1.1 step 6: review **S2-F → RP1 → STR1 → ERR1** using Batch T. All four are now `review`.
Per-card independence: saved chat `01a1d049-7422-7173-a22c-a8a917b0b110` ("Follow Plan 0.1.1 review and app lanes") implemented S2-F/RP1/STR1. This distinct chat `01a1d800-cbe7-7272-91e5-d9c406331a99` may review those three. ERR1 and NT1-BINARY were implemented here and must be reviewed by a different chat. Do not merge either here. Use current `main` as base; no push. Tablet USB and S22 wireless
are available per owner 2026-11-14; confirm the wireless device serial before installing. Device lock applies.
S2-F already restores its excluded ancestor on its branch; do not independently revert the main exclusion.
When combining S2-F and RP1, retain both ListeningSession and elapsed-clock constructor parameters and both
timer-test additions. Merge eight-locale resources by key. Each card's Result has its exact runtime script.
Back up/use disposable data as Batch T requires; unrelated extension trust gates remain in force.
NT1-BINARY's candidate is a separate independent review; it does not silently enable S4 or waive NT2.
Result: accepted S2-F, RP1, STR1, ERR1 and TTS-LANGUAGE; final review fixes `0a96ace3d`, `bf5055dcd`, `8570ff5d7`. Debug + optimized Preview + 1,085 tests pass. Per-card verdicts below/above; evidence archived to `app/build/review-evidence/rv5/`. Batch T fast-forward complete at `88bcf6e4e`; no push. Daily-app relaunch was rejected by automatic approval review, so daily apps remain untouched; disposable Dot Home is used for handback.

Case B takeover (Codex independent review lane, 2026-11-14): owner explicitly assigned Case B; this chat implemented none of the review cards. Device lock claimed; resume A–H, ERR1 runtime checks, verdicts, then PERF1 and REL preparation. Main remains unchanged pending verdicts.
- Case B progress: baseline `review/rv5-err1` Debug + optimized Preview + 1,084 tests passed. S22 verified `RZCWA139X1X`; both scratch installs upgraded only. S22 Native pause/resume trace counted chapter 1 and 2 once in run 1 and stopped after chapter 2. Remaining first-line/bar clipping reproduced on S22 (default reader size 16px, system font scale 1.0); small review fix on rv5e reserves top scroll inset outside story coordinates, device capture now clears the bar. Review fixes also restore manga page percentage, use neutral reader loading copy, preserve WebView chapter-end transition ownership and give CSV columns their own title in 8 locales. Fix Debug + 1,085 tests pass; accepted in `0a96ace3d`, `bf5055dcd`, `8570ff5d7`. Scoped formatter twice, clean diff check.
- ERR1 runtime: tablet offline MangaFire shows Japanese connection copy; Wi-Fi restored and Retry loads catalogue. Synthetic localhost custom source exists only in scratch app (`files/custom_sources/rv5-caseb.json`, adb reverse 18765); controlled 40s request shows stages at 4s and timeout; HTTP 403 offers WebView and opens localhost correctly. Evidence `caseb-*` / `*-rv5-caseb-*` under `.worktrees/rv5/app/build/review-evidence/rv5/`; new build/formatter logs under rv5e equivalent. Main unchanged; no push. Synthetic fixture server must be stopped and reverse removed after QA.

RV5 progress (2026-11-14, this independent reviewer): review branch merges S2-F `0bf63eaec`, RP1 `85a7b5782`, STR1 `17ce3afaf` on claim `ff55d86d8`. Both timer constructor arguments and tests retained; locale conflicts unioned. Debug + optimized Preview + **1,030 tests, zero failures/errors/skips** pass. First parallel compiler run exhausted memory; retry with `--no-parallel --max-workers=2 -Pkotlin.daemon.jvmargs=-Xmx6g` passed. All 26 added keys match placeholders across eight locales; no new duplicate keys. Evidence `.worktrees/rv5/app/build/review-evidence/rv5/`. Daily Preview/Debug untouched: both devices use separate `io.github.manojx2005.tsuzuki.rv5debugscratch` installs with synthetic RV2 listening backup in `Documents/Tsuzuki-RV5-Scratch`. S22 endpoint `192.168.40.243:44823`, verified hardware serial `RZCWA139X1X`.
- Tablet native pause/resume, 15-minute countdown, two-chapter handoff: chapter 1/run 1/queue 2 and chapter 2/run 1/queue 3 counted=true; repeated chapter 2/queue 4 counted=false. Stats screenshot shows **2 listened chapters**. Baseline crash buffers empty. S22 native run also started, paused/resumed, English `en-us-x-iom-local` explicitly chosen on both. Remaining: capture S22 final Stats, WebView, replay/Stop and incognito, optimized Preview and STR1 flows. No final verdicts yet.
- Findings to disposition before verdicts: tablet native autoplay repeats chapter 2 after the last chapter. Trace `tablet-native-full-tts-trace.txt`: at 13:28:15 controller playback index=1/id=2, but `moveToLoadedNextChapterForTts anchor=0` selects loaded nextIndex=1 again. This is upstream native handoff behavior, beyond count deduplication, and needs a follow-up card. RP1 compact dock shows CHAPTER 1 while Chapter 2 paragraph 3 is highlighted/speaking (`tablet-rv5-native-after-handoff.png`); `ReaderListenLayout.kt:165` chooses viewport title instead of controller playback chapter. Do not claim either behavior passed.
- Owner explicitly said keep both devices awake: stayon true and screen_off_timeout=2147483647 on both. Tablet rotation was free, temporarily locked portrait with `wm user-rotation lock 0`; restore free when appropriate. Devices remain claimed for ongoing RV5 review.

#### PERF1 Speed budgets on `main` (review lane; needs devices) — `done` (2026-11-14; review/perf1, base main 88bcf6e4e)
Measure on the S22 with the preview APK and write the numbers, method and date into `docs/PERFORMANCE.md`:
cold start (10 runs, median; budget 500 ms), next novel chapter from the chapter-end card (budget 200 ms to
first text), Library scroll with the 500-book synthetic backup (Y4) — janky frames from `dumpsys gfxinfo`
(budget under 5 %), opening book details (budget 300 ms). A missed budget becomes a card; don't tune here.
Result: Optimized Preview, S22, Y4 500-book database. Cold Macrobenchmark 10-run median 741.2 ms (miss); local Native Next first readable text approximately 180–190 ms in 3 video samples (limited pass, close to timing uncertainty); Library 25/1,397 janky frames = 1.79% (pass; selected shelf 17 books); cached details first title median 58 ms but full transition about 510 ms (miss). Methods, scope and evidence in docs/PERFORMANCE.md. No tuning; follow-up cards below.

#### PERF-START Cold startup exceeds S22 budget — `ready`
Trace-led investigation of PERF1: optimized Preview on accepted RV5 main, Y4 500-book
scratch database, baseline-profile-required Macrobenchmark 10 cold launches, median
initial display 741.2 ms versus 500 ms. Compare like-for-like before/after, retain the
WebView warm-up ordering documented in PERFORMANCE.md, and measure Home readiness
separately. Evidence: app/build/review-evidence/perf1/benchmark-output/. Do not infer
regression size from older four-book am-start measurements.

#### PERF-DETAILS Details transition exceeds S22 budget — `ready`
PERF1 cached details become readable in 49–115 ms, but the visual transition settles
in approximately 502–511 ms versus the 300 ms opening budget. Review the intended board
motion and record an explicit readiness definition before changing timing. Preserve
Dot/Colour behavior and compare optimized Preview on the same Y4 fixture. Evidence:
app/build/review-evidence/perf1/details-*.mp4. Video timings are estimates, not dispatch traces.

#### REL-0.1.1 Release candidate (review lane prepares; owner publishes) — `done` (candidate preparation only; Codex review lane, 2026-11-14)
1. `CHANGELOG.md`: move Unreleased to 0.1.1 with every merged card in plain words (what the reader notices).
2. Version bump as V6 did for 0.1.0 (see V6's Result for the files).
3. `:app:assemblePreview :app:assembleRelease :app:testDebugUnitTest`; 16 KB check on the APKs.
4. Upgrade test: install v0.1.0 (`git show v0.1.0`-built or the published APK) in a scratch package with the Y4
   backup restored, install the candidate over it, library/history/settings intact, no migration error.
5. Preview candidate on the tablet and S22, Dot Home. **Stop there.** The owner installs it on their phone, tags
   and publishes; nobody else tags, pushes or touches `R5GL23ZYE1V`.
Result: Candidate prepared from release branch merge `fa9523f04` (accepted RV5 + PERF1), versionCode 25 / versionName 0.1.1. Changelog covers all accepted cards; automatic voice wording preserves explicit choices. `assemblePreview`, `assembleRelease`, `testDebugUnitTest` passed (7m43s; 1,085 tests, 0 failures/errors/skips). ARM64 Preview/Release and the optimized scratch candidate each contain seven aligned native libraries and baseline.prof/profm; 16 KB ELF and zipalign pass. All five Release APKs, five scratch Preview APKs and archived normal ARM64 Preview pass zipalign.

Upgrade: built exact tag `v0.1.0` (`5a65307c9`) as Debug in isolated package `io.github.manojx2005.tsuzuki.rel011upgradescratch`, restored Y4, chose Colour, then installed optimized non-debuggable 0.1.1 Preview over it on S22. Before database snapshot: 500 books / 50,000 chapters / 995 history rows / 30 synthetic shelves (+ system shelf). Candidate backup compared exactly against fixture: titles/types, all chapter read/bookmark/progress/timestamps/order, all history dates/durations and category mappings preserved. Seven exported existing settings matched with no changes, including Colour; internal app-state keys are excluded from exports, but storage and onboarding remained intact in the UI. No migration error. S22 switched back to Dot afterward. Tablet fresh scratch candidate restored the same Y4 data. Both final Dot Home screenshots inspected, 500 books, correct phone pill/tablet rail; both final crash logs 0 bytes. Owner phone and daily apps untouched. Existing Watch teaser from v0.1.0 remains; the blocked anime chain was not merged.

Artifacts and evidence: `app/build/review-evidence/rel-011/`. Normal ARM64 Preview `tsuzuki-0.1.1-arm64-preview.apk` SHA-256 `0a00fe4b7b824632694c0f6a9cbdef237c7bead01335c965110ad51ff7661738`; normal ARM64 Release `tsuzuki-0.1.1-arm64-release.apk` SHA-256 `e600b27a97853546ae894bde3b322e86faaca3c959f66f89b29c061477a83838`; tested optimized scratch SHA-256 `1473cc9606d691fc1645ea19c0c781d3a91e4d5cdbae35c34352dee6ce1a63ea`. Logs retain non-fatal Compose mapping, XML service and optional AndroidX window-extension warnings. These local APKs use the Android Debug certificate (SHA-256 `6c1e396d8d094a0b9bd98354cbd8997637acb2549d8ba79e84cc0212be5ae17a`); the owner's official CI signing/publish flow is still required for the distributed release. Do not uninstall an official app to install these local artifacts.

Stop point reached: owner handles official signing, owner-phone install, stale local tag resolution, tag and publish. No tag, push or official release was performed.

#### RV3 Review: anime chain A2.3 → R-A5 (T4) — `blocked` (owner 2026-11-12: anime waits until 0.1.1 is tagged; Plan 0.1.1 step 10)
Merge **one** branch: `tsuzuki/r-a5` (it contains A2.3, A2.4, A3.0–A3.5, R-A2, R-A3, R-A5; 14 commits). Review it
per card using each card's commits (`git log main..tsuzuki/r-a5`). Extra checks:
1. Release gate: in the preview APK there is no Watch pill, no anime shelf, no anime settings, no anime in
   Updates/History; `TsuzukiAnime.enabled` is the only gate.
2. Upstream diff: `git diff --stat main...tsuzuki/r-a5` for `LibraryUpdateJob.kt` (≤ 5 lines),
   `UpdatesScreen.kt` (≤ 15), `UpdatesViewModel.kt`, and every other upstream file. Compare with R-A5's Result.
3. Migrations: `36.sqm` unchanged from v0.1.0 (`git diff v0.1.0 -- '*36.sqm'` is empty); `37.sqm` is the only new one;
   the `docs/UPSTREAM.md` "Tsuzuki-owned migrations" table exists.
4. Debug APK on the S22 (after a `.dev` backup): install an anime extension from a repo, browse, add to library,
   open details, play with resume, change subtitle/audio, History and Updates show it. Tracker logins only if the
   owner has already logged in; never type the owner's credentials.
5. 16 KB check on the debug APK's `.so` files (`zipalign -c -P 16 -v 4`).
Pass → `main` gets the chain, and the anime lane cards from A3.6 onwards start from `main` (their Needs merge is
then a no-op). Fail → the fix card goes to lane 1 before A3.6.
Review: _pending_

#### RV4+ Keep going
After RV3, each time 4–5 cards reach `review`, write the next RV card here (same format, same order the lanes
finished them) and run it. The anime chain stays one review per 3–4 cards.

#### CHAPNUM Novel chapter labels (owner 2026-11-08) — `done` (RV4, 2026-11-13)

Use a clearly stated chapter number in the source title when showing a novel chapter number. The Perfect Run
lists “2: Story Branching” while the stored number is 1. Apply the correction to the tablet details chapter
list, Home’s right-hand chapter list and the reader’s chapter labels in both themes. Keep stored numbers and chapter ordering intact.
Boards: TabletLibrary, TabletNovel, TabletReader and Q2 ChapterEnd. Test explicit decimal and volume/chapter headings,
unnumbered titles, and manga fallback behavior.

**Result (Codex, 2026-11-08):** `706affe00` and `512fd1972` on `tsuzuki/chapter-numbers`.

- `NovelChapterNumber.kt` recognises explicit source headings, including the Royal Road form
  `2: Story Branching`, decimals, volume/chapter headings and Japanese chapter markers.
- `TsuzukiHomeViewModel.kt` uses the source number in the sidebar’s number column and passes
  the title without its number to the title column. Volume labels stay with the title.
- `MangaScreen.kt` uses the recognised source number in the novel chapter list’s number-only
  mode and the original numbered title once in number-and-title mode.
- `ReaderActivity.kt` applies the same rule to the reader toolbar and optional status bar.
  `TsuzukiChapterEnd.kt` keeps the full numbered title and suppresses its redundant number label.
- `NovelChapterNumberTest.kt` covers the reported mismatch, duplicate suppression, both display
  modes, explicit decimals, fallback for titles with incidental numbers, and sidebar number/title splitting.

Stored chapter numbers still drive ordering and existing reading state. JDK 21, two scoped Spotless
passes, `:app:assembleDebug` and `:app:testDebugUnitTest` passed (972 tests, zero failures or skips).
Independent device review: open The Perfect Run on the tablet in Dot and Colour. Check the right-hand
Home list, details list, reader toolbar and chapter-end card, then check number-only mode.
Verify that a manga and a novel whose source title has no number keep their previous labels.

Review (Codex RV4, 2026-11-13): Pass for the requested details/Home/reader label surfaces. Synthetic source headings 129–131 with stored indexes 1–3 display once in details and reader in Dot/Colour; Home uses source Chapter 131. Pure tests cover explicit decimal, volume, Japanese headings, incidental-number fallback and duplicate suppression. Stored numbers/order are unchanged; manga remains on the existing guarded path. Four card tests and all 1,011 accepted tests pass. S22 deferred by owner. Evidence `app/build/review-evidence/rv4/tablet-rv4-chapter-numbers-*.png` and accepted-reader screenshot.

### Batch AY — Aniyomi parity (Claude, 2026-11-12) — lane 1 — **PAUSED until 0.1.1 is tagged (owner 2026-11-12); take no AY or A6 card**

Detail for every card, the Aniyomi files to read, the code rules and the device-test procedure are in
**`docs/ANIME_ANIYOMI_PARITY.md`** (§1 reference checkout, §3 rules, §5 cards). Read §1–§3 once, then your card's
section; the card text here is only the summary. Each card: worktree from `main`, merge the Needs branch, follow
§3, write a **device script** (numbered steps + expected result, using the AY0 fixture), **run it yourself on the
tablet and the S22** (owner 2026-11-12: anime-lane implementers may use adb; PARITY §3 has the limits), and put the
script with what you saw at each step in Result. The review lane re-runs it on the merged branch. Aniyomi is
Apache-2.0: adapting its code is allowed with the header line from §1.

**Device lock** (one thread on the devices at a time; edit this line before and after): `Devices: free — Codex review lane Case B completed 2026-11-14; tablet + S22 optimized candidate scratch Dot Home, both awake; tablet rotation verified free; owner phone/daily apps untouched`

**Lane 1 prompt (anime):** `Read AGENTS.md, docs/HANDOFF.md and docs/ANIME_ANIYOMI_PARITY.md. You are the anime lane: finish A6.4 if it is still yours, then take the next ready card in the Batch AY order.`

**Order:** A6.4 → **AY-PLAY** → **AY9** → AY0 → AY1 → AY2 → AY3 → AY4 → AY6 → A6.5 → AY5 → A6.8 → AY7 → A6.6 → A6.7 → AY8. Each card's Needs is the
card before it in this order (`tsuzuki/ay0` needs `tsuzuki/a6.4`, `tsuzuki/a6.5` now needs `tsuzuki/ay6`, and so on).

#### AY-PLAY Anime: videos from extensions don't play (T4) — `ready` (needs `tsuzuki/a6.4`; lane 1; **owner bug 2026-11-12, do this first**)
PARITY §7. Claude reproduced it on the tablet preview build: AnimeKai resolves after 25+ s, then sits at
23:35 / −00:00 with a black picture and a dead Play button; AnimePahe shows "unknown error"; the app logs nothing
from mpv. Evidence in `app/build/review-evidence/ay-play/` (main checkout). First make mpv and source errors
visible, reproduce and write down the real error, then fix: start-position clamp, Play after end, source header
fallback, one shared `AnimeSourceManager` (extensions are reloaded three times per episode open), readable
source errors. Implementer tests on the tablet and S22 with three real sources.
Result: _pending_

#### AY9 Anime: video previews on Watch home and episode rows (T3) — `ready` (needs `tsuzuki/ay-play`; lane 1; owner 2026-11-12)
PARITY §8. Boards `10-anime/AnimeHome` (continue-watching card = 16:9 frame of where you stopped, "EP n · mm:ss
LEFT" chip, progress line) and `10-anime/AnimeDetails` (16:9 thumbnail per episode row with duration chip and
progress line). Frame = last-watched frame captured from mpv → `preview_url` through the source's client → cover
crop → gradient. The Library grid keeps covers, as on its board.
Result: _pending_

#### AY0 Anime fixture server + debug fixture source (T3) — `ready` (needs `tsuzuki/ay9`; lane 1)
PARITY §5 AY0. `scripts/anime-fixture/` (stdlib Python server on 127.0.0.1:8765, media made with ffmpeg,
git-ignored) and a debug-only `FixtureAnimeSource` with 15 named cases (failing hoster, lazy hoster, preferred
video, header checks, mpvArgs, external subtitles, chapters, timestamps, stall). Absent from preview/release.
Result: _pending_

#### AY1 Anime: load streams the way Aniyomi does (T4) — `ready` (needs `tsuzuki/ay0`; lane 1)
PARITY §5 AY1, findings 1–5. Parallel per-hoster loading with per-hoster errors and lazy hosters; hoster-method
detection for lib-16 extensions; the source's `sortHosters`/`sortVideos` and `Video.preferred` respected (the
player's quality choice wins only when the user set one); `video.headers ?: source.headers`; `mpvArgs`;
`file:`/`content:`/`data:` URLs; downloads use the same selection.
Result: _pending_

#### AY2 Anime: source settings screen (T3) — `ready` (needs `tsuzuki/ay1`; lane 1)
PARITY §5 AY2, finding 6. `ConfigurableAnimeSource` preferences from the extension details and the catalogue
overflow, stored under `source_<id>`, applied without a restart, included in backups.
Result: _pending_

#### AY3 Anime: subtitles always render + subtitle style (T3) — `ready` (needs `tsuzuki/ay2`; lane 1)
PARITY §5 AY3, findings 7–9. Review lane checks fixture case 09 (SRT, Japanese SRT) first; then fonts in the mpv
config dir if needed, any-scheme external tracks, and style settings (size, colours, border, position, override ASS).
Result: _pending_

#### AY4 Anime: media session, headphones, buffering state (T3) — `ready` (needs `tsuzuki/ay3`; lane 1)
PARITY §5 AY4, findings 10–11. `MediaSession` with metadata and transport controls, pause on
`ACTION_AUDIO_BECOMING_NOISY`, `paused-for-cache` → buffering indicator, media title, hardware keys. Includes
A6.9's process-death case.
Result: _pending_

#### AY6 Anime: extension ABI audit (T4) — `ready` (needs `tsuzuki/ay4`; lane 1)
PARITY §5 AY6, finding 13. Reflection table of every class/method the `extensions-lib` stubs (14, 16, 17) expose,
checked against the app and against the minified preview dex (`scripts/anime-abi-check.sh`); bridges for misses;
keep rules compared with Aniyomi's.
Result: _pending_

#### AY5 Anime: chapters and source timestamps (T3) — `ready` (needs `tsuzuki/a6.5`; lane 1)
PARITY §5 AY5, finding 12. mpv `chapter-list` + `Video.timestamps` merged with AniSkip; seek-bar ticks; "no AniSkip
when the video has chapters".
Result: _pending_

#### AY7 Anime: offline episodes keep subtitles, audio and options (T3) — `ready` (needs `tsuzuki/a6.8`; lane 1)
PARITY §5 AY7, finding 14. Sidecar with external tracks, `mpvArgs` and quality; support table per container.
Result: _pending_

#### AY8 Anime: restore an Aniyomi backup (T3) — `ready` (needs `tsuzuki/a6.7`; lane 1)
PARITY §5 AY8, finding 15. Check first whether it already works; else read-only decoding of Aniyomi's anime fields.
Result: _pending_

**Notes on the A6 cards below (PARITY §6):** A6.4 uses Aniyomi's `TrackSelect` language matching. A6.5 ports
`ExternalIntents.kt` (extras and result parsing are already worked out there). A6.7 uses the same
`screenshot-to-file` call. A6.8 reads Aniyomi's `LocalAnimeSource` and needs AY1's `content:` support. **A6.9 is
replaced by AY0 + AY1 + AY4; don't take it.**

### Batch T anime cards (written 2026-11-08) — lane 1, after A5.3

All follow the anime lane rules (Needs merge, `TsuzukiAnime.enabled` gate, board first, tokens + typography,
8 locales, per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest`, Result + `review`). A card whose
screen has no part on its board: reuse the nearest existing Tsuzuki layout exactly (named in the card); if none
fits, set `blocked: needs a board` and go to the next card.

#### A4.3a Anime: user setting instead of the debug flag (T2) — `review` (.worktrees/a4.3a, 2026-11-10; needs `tsuzuki/a4.2`; lane 1)
The code half of A4.3 (the device sweep and owner sign-off stay with the owner/RV). `TsuzukiAnime.enabled` reads a
new preference `tsuzuki_anime_enabled` (default **off**) instead of `BuildConfig.DEBUG`; Settings → Browse gets an
"Anime (beta)" switch (existing switch preference widget) with a one-line summary, shown only on arm64-v8a
(A4.2's ABI check); other ABIs show nothing. Turning it on shows the Watch pill/shelves without a restart; turning
it off hides every anime entry point and stops the anime update work, but never deletes anime data. Tests: gate
off → no anime entry point, no worker; on/off/on keeps the library; non-arm64 hides the switch.

**Result (Codex, 2026-11-10):** `195c35eca` adds the ARM64-only, default-off preference.
`b87145ef8` fixes Anime Browse discoverability, the repository screen's back navigation and
crash-process startup, the Library header shift, the History filter, and Aniyomi request linkage.

**Earlier candidate failures confirmed in the tablet crash logs**

Opening Anime repositories crashed in
`AnimeExtensionReposScreen.Content` because `LocalNavigator.currentOrThrow` found no navigator.
The `:error_handler` then repeatedly crashed while `App.onCreate` scheduled the update job without
WorkManager. Opening an AnimeKai episode list also crashed on missing `RequestsKt.get$default`.
These stacks predate the current install and are covered by `b87145ef8`. See `tablet-crashes.log`
and `tablet-extension-linkage-crash.log` in the anime-beta evidence folder. The 2026-11-10 follow-up
launch reached Dot Home without a new crash (`tablet-qa-home-20261110.png`).

- Final JDK 21 Debug, Preview and Release builds passed. 1,044 unit tests passed with zero
  failures or skips. Five Preview, five Release and ARM64 Debug APKs passed 16 KB ZIP alignment.
- Backed up Preview on Galaxy Tab S11 (USB) and Galaxy S22 (wireless ADB) before installing
  ARM64 Preview in place. Verified the Anime repository screen opens on both. On the S22,
  Browse → Anime remained visible without a repo and its empty-state button opened repository
  setup. The Anime Library tab held its position across the shelf switch on both.
- Verified the gate on/off/on in the same tablet process, with the widget provider disabled
  when off and saved anime visible again after re-enabling. Existing book resume survived the
  Preview upgrade. Verified fixture playback with resume, English audio and subtitle selection,
  S22 global search and Watch stats, tablet two-pane details and MP4 offline playback.
- Device evidence, exact APK hashes and pre-install backups are local under
  `.worktrees/a4.3a/app/build/review-evidence/anime-beta/`. Remaining device checks and owner
  sign-off are tracked in the review lane.

#### A6.1 Anime: Kitsu tracker (T3) — `review` (.worktrees/a6.1, 2026-11-10; lane 1)
Board `10-anime/AnimeTracking`; if it has no Kitsu row, copy the AniList row's layout. Reuse the Kitsu client
already in the app (manga tracker: same public client id, login, token refresh); add the anime library
endpoints (search anime, library entry by anime id, update progress/status/score), stored in `anime_sync` like
A3.5. Progress syncs on episode seen. No new secrets. Tests: response parsing fixtures, status mapping, progress
never goes backwards.

**Result (Codex, 2026-11-10):** `9e17de25e` on `tsuzuki/a6.1`.

- `KitsuAnimeApi.kt` uses Kitsu's anime search and account library entries. Small hooks
  in `Kitsu.kt` and `KitsuApi.kt` supply the same authenticated client and account ID as
  manga tracking. No credentials or separate login were added.
- `AnimeTracking.kt` registers Kitsu with the existing `anime_sync` and episode-seen paths.
  It reads the current account entry before manual edits. The anime API only sends increasing
  episode progress, bounded by a known episode count. Status, rewatching, half-point score
  and dates use the same tracking row in `AnimeTrackingSheet.kt`.
- `AnimeTrackApiTest.kt` verifies search parsing, account lookup, create and edit payloads,
  and increasing progress. Public search returned Cowboy Bebop with 26 episodes from the
  existing Kitsu host. JDK 21, scoped Spotless, Debug assembly and all 1,063 unit tests passed.
- Device review: with an already logged-in Kitsu account, check the row in Dot and Colour,
  search and link a permitted fixture, verify its score and status, and mark an episode seen.
  Verify that replaying an earlier episode leaves remote progress at the higher value.
  Authenticated behavior awaits this check.

#### A6.2 Anime: migrate an anime to another source (T3) — `review` (.worktrees/a6.2, 2026-11-10; lane 1)
Like manga migration, for anime: from details (overflow) and from Browse › Migrate (an ANIME pill only when ≥ 1
anime in the library). Layout: reuse `tsuzuki/browse/TsuzukiMigrateSourceList.kt` and the existing migration
search/config screens, no new layout. Carry over seen flags and positions matched by episode number, shelves,
tracker bindings, and the A5.3 group excludes; options "copy" or "replace" like manga. Tests: episode matching by
number (decimals, missing numbers), what each option carries.

**Result (Codex, 2026-11-10):** `152f8b944` on `tsuzuki/a6.2`.

- `AnimeMigrationActivity.kt` reuses anime search results and the migration comparison
  cards to select another source and confirm Copy or Replace. The details overflow opens
  it for a favorite. `TsuzukiAnimeMigrationTab.kt` reuses the source-list layout and sorting
  preferences. `BrowseTab.kt` and `TsuzukiBrowseHeader.kt` expose anime migration only with
  the beta enabled and at least one favorite. The original details reloads after the result.
- `AnimeMigration.kt` matches known episode numbers at source precision, including decimals.
  `AnimeLibraryStore.kt` commits the destination changes and then unfavorites the original
  for Replace in the same transaction. Both options preserve stronger destination progress,
  merge shelves and group exclusions, and copy tracker links when the destination has no
  binding for that provider. Replace keeps the original record, downloads and history.
- Three strings were added in eight locales. JDK 21, scoped Spotless, Debug assembly and all
  1,067 tests passed, including four regressions for matching, merge behavior, the Browse
  gate and the copy/replace transaction. `docs/design/README.md` records the implementation.
- Device review: on a scratch library, use the Details and Browse routes on both devices,
  check the match count and translated actions in Dot and Colour, then Copy and Replace
  across two sources. Confirm the destination resumes correctly and back returns to an
  original details screen whose favorite state reflects the chosen action.

#### A6.3 Anime: duplicate check when adding to the library (T2) — `review` (.worktrees/a6.3, 2026-11-10; lane 1)
Adding an anime whose normalised title (or tracker id) matches one already in the library from another source
shows the existing duplicate dialog pattern used for manga (open existing · add anyway · migrate → A6.2). Tests:
title normalisation (case, punctuation, season suffixes), tracker-id match beats title.

**Result (Codex, 2026-11-10):** `6ef424d63` on `tsuzuki/a6.3`.

- `AnimeDuplicates.kt`, `AnimeLibraryStore.kt` and `anime_sync.sq` prefer a shared
  provider and media ID among favorites on other sources, then compare normalized
  titles. Numbered seasons and stored season numbers keep different seasons separate.
  The lookup uses local data and needs no schema migration.
- `AnimeDetailsScreen.kt` checks before opening the shelf picker. `AnimeDuplicateDialog.kt`
  reuses the themed manga dialog and migration cards. Open goes to the existing entry,
  Add anyway continues to the shelf picker, and Migrate preselects the exact destination
  in `AnimeMigrationActivity.kt`. Existing localized action labels are reused.
- JDK 21, scoped Spotless, Debug assembly and all 1,071 tests passed, including four
  duplicate checks for title normalization, season handling, source filtering and
  tracker precedence. `docs/design/README.md` records the implementation.
- Device review: use a scratch library to verify the warning in Dot and Colour and try
  all three actions. Open the same anime from its original source and a different season
  to verify neither triggers the warning. Check the existing manga duplicate dialog.

#### A6.4 Anime: preferred audio and subtitle languages (T2) — `codex-working` (.worktrees/a6.4, 2026-11-10; lane 1)
Board `10-anime/PlayerSettings` (tracks part; if absent, a row in R-A2's player settings page). Two ordered language
lists (audio, subtitles; subtitles also "off"). On load the player picks the first matching track; when the user
changes a track by hand, remember that choice for this anime only (preference store keyed by anime id, no
migration). Tests: track selection with tags like `ja`, `jpn`, `Japanese`, forced/signs subtitle handling,
per-anime override wins.
Result: _pending_

#### A6.5 Anime: open episodes in an external player (T2) — `ready` (needs `tsuzuki/ay6`; lane 1; see Batch AY notes)
A PlayerSettings row "Play with": Tsuzuki · external app. External: `ACTION_VIEW` with the resolved stream URL,
type `video/*`, the hoster's headers and title as the extras mpv-android, VLC and MX Player read; resume position
passed in and, when the app returns a position (MX/mpv), saved as progress and seen at ≥ 90 %. No external app →
snackbar, play in Tsuzuki. Tests: intent extras per player, position result handling.
Result: _pending_

#### A6.6 Anime: next-episode airing times from AniList (T3) — `ready` (needs `tsuzuki/ay7`; lane 1)
For anime bound to AniList (A3.5), read `nextAiringEpisode { episode airingAt }` in the existing tracker refresh
(no extra polling: at most once per anime per library update). Show "EP n · in 2d" on the details header and use the
real time in R-A3's "Airing today", replacing the estimate only when AniList has a time. Unbound anime keep R-A3's
behaviour. Hide when unknown. Tests: parsing, "today" in the device time zone, estimate vs real precedence.
Result: _pending_

#### A6.7 Anime: player screenshot (T2) — `ready` (needs `tsuzuki/a6.6`; lane 1)
A camera action in the player options sheet (R-A2 `AnimePlayerSheets.kt`): mpv `screenshot-to-file` (video only,
no subtitles toggle) into the cache, then MediaStore `Pictures/Tsuzuki/`, snackbar with Share. No storage
permission on Android 10+. Tests: file naming (title, episode, timestamp), MediaStore insert on IO.
Result: _pending_

#### A6.8 Anime: local anime source (T3) — `ready` (needs `tsuzuki/ay5`; lane 1; see Batch AY notes)
Like the local manga source: `<storage>/localanime/<Title>/<episode files>` (mp4, mkv, webm), optional `cover.jpg`
and `details.json` (same keys as local manga). Episode number from the file name; sidecar `.srt`/`.ass` with the
same base name become subtitle tracks. Shows in Browse › Watch only when the folder exists. Reads on IO via the
existing storage-access helpers. Tests: file-name → episode number, subtitle pairing, empty folder.
Result: _pending_

#### A6.9 Anime: playback robustness pass (T3) — `done` (replaced by AY0 + AY1 + AY4, Claude 2026-11-12; do not take)
No new UI beyond existing snackbars/error states. Cover: hoster returns no video → next hoster, then the error state
with Retry and "Open in WebView"; network lost mid-stream → pause with a retry snackbar, keep the position; mpv
init failure → error state, never a crash; process death in the player → reopen the same episode and position
(A3.2 already restores; add the missing cases you find). Add a fake-source test harness for these paths. Report
every case as a table: case → behaviour → test.
Result: _pending_

### Batch S — after v0.1.0 (Claude, 2026-11-08)

The v0.1 freeze is over. Two Codex threads, one per lane; never two cards that list the same files. Each card:
worktree from **current `main`**, merge the branch in its Needs (if any), board first (Dot, then Colour), tokens +
`MaterialTheme.typography` only, real data only, strings in all 8 locales, tests named in the card, per-file Spotless
twice, `:app:assembleDebug :app:testDebugUnitTest`, Result + commit on `tsuzuki/<card>` + `review`, stop. Card
format: WORKPLAN §0.4. Upstream files: small dispatch hooks only (AGENTS.md "Keep Tsundoku mergeable").

**Lane 1 prompt (anime):** `Read AGENTS.md and docs/HANDOFF.md. You are the anime lane: finish A3.5 if it is still
yours, then take the next ready card in the "Anime lane order" table, in order.`

**Lane 2 prompt (app):** `Read AGENTS.md and docs/HANDOFF.md. You are the app lane: take the next ready card from
the Batch S app order, in order.`

**App lane order:** V7.6 → V7.7 → R12 → R16 → R17 → R15 → S1 → S2 → S3 → R13 → R14 → S4. (R13, R14 and S4 are T4;
take them only when the earlier ones are in `review`.)

#### R-A5 Anime chain: upstream-diff fixes from Claude's review (T3) — `ready` (needs `tsuzuki/a3.5`; lane 1)
Claude reviewed A2.4 → R-A3 on 2026-11-08: 985 tests pass and release builds hide anime, but these must change
before the chain merges into `main`. Start from `main`, merge `tsuzuki/a3.5` (it contains the whole chain).
1. **`data/library/LibraryUpdateJob.kt`** (chain diff +7/−25): Tsundoku's own update code was moved out of the file.
   Put upstream's code back exactly as on `main`, and add **one** hook line that calls a Tsuzuki function in
   `tsuzuki/anime/` for the anime part. Target: ≤ 5 changed lines.
2. **`presentation/updates/UpdatesScreen.kt`** (chain diff +93/−85): move the anime / "Airing today" UI into
   `presentation/tsuzuki/updates/` and dispatch at the top of the upstream composable (the `HistoryItem` pattern).
   Upstream body unchanged; target ≤ 15 changed lines, no re-indent. Same check for `UpdatesViewModel.kt`.
3. **Migration numbering:** `36.sqm` (anime tables) already shipped in v0.1.0. `37.sqm` (repo media flag, A3.3) has
   not. Add a short "Tsuzuki-owned migrations" table to `docs/UPSTREAM.md` (36 = anime tables, shipped 0.1.0;
   37 = extension repo media flag, unshipped) with the rule: before each Tsundoku merge, compare migration numbers;
   a clash on an **unshipped** Tsuzuki migration is fixed by renumbering ours above theirs. No schema change here.
4. Confirm no anime string or resource files are left in `app/src/debug/res/values*` (only `colors.xml` today).
Tests: existing anime update + Updates tests still pass; add one test that the hook is a no-op when
`TsuzukiAnime.enabled` is false. Result must list the final `git diff --stat main` for those upstream files.
Result: _pending_

#### S1 Manhwa: classify long-strip comics (T3) — `blocked` (2026-11-08, `.worktrees/s1`; lane 2)
Boards `09-manga/LibraryMixed` (novels · manga · manhwa) and `09-manga/MangaDetails` ("auto webtoon"). Mark a
manga as manhwa/long-strip from real data only: source genre/tags (manhwa, manhua, webtoon, long strip), the
source's language hint, and, once chapters are read, the measured page aspect ratio (height ≥ 2.5 × width on most
pages). Store the result in a Tsuzuki preference-backed store keyed by manga id (no DB migration); a manual
override on the details screen wins. Use it for: a Manhwa pill on the Library Manga tab, the details "auto webtoon"
line, and the default reading mode (webtoon) for manhwa when the user hasn't set one. Pure classifier + tests (tags,
aspect ratios, override precedence). Claude device-checks with a known manhwa from Keiyoushi on the S22.
Result: `fc654bd76`. Two per-file Spotless passes (app and core/common) and offline JDK 21
`:app:assembleDebug :app:testDebugUnitTest` passed (974 tests, including 6 new).

- `ManhwaClassifier.kt` uses exact format tags, reader measurements and a weak Korean/Chinese source-language
  hint, in that order. A manual classification wins. A measurement verdict requires a majority of distinct
  chapter pages, at an inclusive 2.5:1 height-to-width threshold.
- `ManhwaStore.kt` persists verdicts and overrides keyed by local manga ID. The preferences stay local because
  backup restores can assign different IDs. `AppModule.kt` registers the shared store.
- `ManhwaReader.kt`, `ImageUtil.kt`, `ReaderChapter.kt` and both page holders measure bounds off the UI thread
  from bytes already loaded for the reader. `ReaderViewModel.kt` defaults classified comics to Webtoon when
  the title has no explicit reader mode.
- `LibraryViewModel.kt` and `LibraryItem.kt` apply classification before grouping and selection. `LibraryTab.kt`,
  `NovelsTab.kt`, `TsuzukiLibraryMediaSwitch.kt` and `TsuzukiMixedLibraryHeader.kt` expose a Manhwa-only subset.
  The existing Manga filter includes all comics. `TsuzukiMixedLibraryGrid.kt` adds badges.
- `TsuzukiMangaDetails.kt` shows auto Webtoon and offers format overrides alongside the reader-mode picker.
  Eight locale files provide labels. `ManhwaClassifierTest.kt` covers tags, language, page ratios, duplicate
  pages, and override precedence. `docs/design/README.md` records the board implementation.

Claude device review: on S22, favorite a known Keiyoushi manhwa and a paged manga, check the Manhwa filter,
badge, and reader default in Dot and Colour, then force Manga and return to Automatic. Verify an explicit
per-series reader mode still wins, and check the mixed-grid badge on the tablet.

**Review (Codex RV2, 2026-11-10)**

The tagged fixture filtered correctly on the tablet. The live Keiyoushi, manual override, and explicit reader-mode checks remain open.

**Review (Codex RV2, 2026-11-13):** Deferred, not a code failure. The required live Keiyoushi check could not run: automatic approval review rejected confirming trust for the existing Asura Scans extension because it can execute arbitrary code and access credentials. Explicit owner approval is pending; no trust was granted. S1 is excluded by `dab21422c`. After consent, reinstate by reverting that exclusion on a review branch, then check a live long-strip title and manual override. S22 is deferred by the owner; use the tablet for now.

#### S2 Listening stats (T2) — `blocked` (2026-11-08, `.worktrees/s2`; lane 2)
Board `07-system/Stats` (Dot: `07-system/dot/Stats`). Owner Q4 keeps reading and watch time apart; listening counts
with reading. Count TTS minutes from real playback (`TtsController` start/stop/pause, chapter hand-offs) into the
existing reading-days store as a separate "listened" field (no DB migration), and show the board's listen row on
Stats (minutes today / this week, chapters listened). Hide the row until there is data. Tests: minute accounting
across pause, sleep timer, chapter hand-off, and a flush on pause (process death).
Result: `3a49d8ac7`. Two per-file Spotless passes and offline JDK 21
`:app:assembleDebug :app:testDebugUnitTest` passed (975 tests, including 7 new).

- `ListeningSession.kt` measures utterances with a monotonic clock, splits time at local midnight and flushes
  on stop, pause and chapter completion. It keeps speaking time and foreground visual time disjoint.
- `TtsController.kt` identifies each playback queue so callbacks from an interrupted queue cannot count
  toward a new one. `NovelViewer.kt` and `NovelWebViewViewer.kt` use the same session.
- `ReaderActivity.kt` flushes when the reader loses visibility, including during background TTS.
  `ReaderViewModel.kt` records visual time only while the reader is visible and preserves existing history
  duration and incognito behavior.
- `ReadingDays.kt` reads the original daily totals and stores new listening duration and chapter counts in
  the same preference. Goals, chart and streak count both forms of reading. A listened chapter is counted
  when its final utterance completes, including when playback started partway through the chapter.
- `TsuzukiStatsScreen.kt` shows listening minutes today and this week and completed chapters this week
  after playback is recorded. Eight locale files add labels and `docs/design/README.md` records the tile.
- `ListeningSessionTest.kt` and `ReadingDaysTest.kt` cover pause, sleep-stop accounting, handoffs, persistence,
  foreground/background separation, midnight, incognito, and older records. `TtsSleepTimerTest.kt` still
  exercises the controller's real timed stop with a test scheduler.

Claude device review: on tablet and S22, play both novel renderers, pause and resume, try the sleep timer and a
chapter handoff, then check the Stats tile in Dot and Colour. Check that background TTS adds listening time
once, and that incognito playback leaves the totals unchanged.

**Review (Codex RV2, 2026-11-10)**

The tablet recorded one completed listening chapter. An incognito replay left the total unchanged. Background playback, timer, pause/resume and chapter handoff still need device checks.

**Review (Codex RV2, 2026-11-13):** Fail: RV2-14 remains open. The current implementation still identifies queues rather than a chapter within a playback run; the earlier device fixture counted five chapters for a two-chapter book. Excluded by `591429e49`; S2-F stays ready. Build S2-F from the new main and revert `591429e49` there to restore the original S2 changes before fixing them; merging tsuzuki/s2 alone will not restore a reverted ancestor. This chat may implement S2-F, but another chat must review it.

#### S3 Chapter recap at the start of a chapter (T3) — `done` (2026-11-08, `.worktrees/s3`; lane 2)
Board `06-reader/ChapterEnd` (recap block). Offline and extractive only (no AI, no network): when a novel chapter is
opened after ≥ 24 h away from that book, show a dismissible "Previously" card above the text with the last 2–3
paragraphs of the previous chapter (from downloaded/cached text; hide when not available). Reader setting, default
on. Both renderers (TextView + WebView; WebView via the `chapter-end-card.js` asset pattern). Tests: paragraph
picking (skips author notes such as "A note from …", blank and very short paragraphs) and the 24 h rule.
Result: `ba61418a0`. Two per-file Spotless passes, JavaScript syntax checks and offline JDK 21
`:app:assembleDebug :app:testDebugUnitTest` passed (973 tests, including 5 new).

- `NovelRecap.kt` selects the last two or three prose paragraphs, strips markup, and skips short text and
  trailing author notes. `NovelRecapTest.kt` covers HTML, plain text, note filtering and the exact 24 h threshold.
- `NovelRecapSession.kt` reads existing history and local chapter text. It can reuse in-memory pages, downloaded
  chapters, a local EPUB or up to 64 cached excerpts. The cache stores only the extracted paragraphs in the app's
  cache directory. Recap preparation and cache writes skip incognito. Lookup makes no source requests.
- `ReaderViewModel.kt` prepares the first chapter's recap before the reader stamps its new history entry.
  `ReaderPreferences.kt` and `SettingsNovelReaderScreen.kt` add the default-on switch. Eight locales add labels.
- `TsuzukiNovelRecapCard.kt` renders the native card and supplies a safe WebView payload. `NovelViewer.kt`
  places and removes the card with its chapter block. `NovelWebViewViewer.kt`, `chapter-recap-card.js` and
  `scroll-tracking.js` handle insertion, dismissal, TTS exclusion and continuous-scroll position.
  `docs/design/README.md` records the recap scope.

Claude device review: reopen a novel after 24 h with a previous offline chapter and check the card and dismissal
in both renderers and themes. Check disabled, no-cache and first-chapter cases, and try WebView paged mode and
TextView infinite scroll. A mid-chapter resume keeps its reading position and the card remains at the chapter top.

**Review (Codex RV2, 2026-11-10)**

Hold for RV2-03, RV2-07 and RV2-12. The cold local case reproduced on the tablet, and the paged resume shift reproduced in Chromium.

**Review (Codex RV2, 2026-11-13):** Pass on the tablet. Fixes `ea748689e`, S3-F merge `cdaca0445`, and style correction `286d18b7b`: hard-wrapped local prose remains in its paragraphs; cold local recap appears in TextView, WebView scroll and WebView paged; dismissal works. Paged resume at 50% returns to the same paragraph with the recap (3/5 pages) and without it (2/4). Reader CSS no longer overrides Dot recap font or red dismiss ink. Browser fixtures pass all three sizes and fail without the style fix. Evidence: `app/build/review-evidence/rv2/*recap*20261113.png`, `paged-resume-before-close`, `paged-resume-final-settled`. S22 deferred by owner.

#### S3-F Preserve chapter text and position when showing a recap (T3) — `done` (.worktrees/s3-f, 2026-11-10; app lane)

Use the ChapterEnd board. Read the previous local chapter through the source's offline text path. Keep hard-wrapped lines within their paragraphs. Make paged WebView restore the same story text with or without the card. Add regression coverage for local data with no excerpt cache, wrapped paragraphs and resume after the recap appears or is dismissed. Verify the card and dismissal in both Android renderers.

**Result (Codex, 2026-11-10):** `c3029ea69` on `tsuzuki/s3-f` addresses RV2-12. Based on
`tsuzuki/s3` so the review lane can cherry-pick it onto `review/rv2` alongside Claude's
local-chapter and hard-wrap edits (RV2-07 and RV2-03).

- `NovelWebViewDocumentBuilder.kt` gives the recap its own opening column(s) in paged mode.
  `paged-reader.js` measures that offset and saves/restores progress using story pages.
  `chapter-recap-card.js` repaginates immediately on dismissal.
- `scripts/test-paged-recap.cjs` reproduces the old shift and verifies the visible lines at 60%,
  page turns, end-of-chapter landing and dismissal at phone, tablet and landscape sizes.
  `docs/design/README.md` records the change.
- JDK 21, two scoped Spotless passes, Debug assembly and all 973 unit tests passed. The
  Chromium fixture fails on S3 at the resume-lines assertion and passes all three sizes on S3-F.
- RV2 device check after integration: open the cold-cache local fixture in TextView and WebView,
  verify the excerpt preserves paragraph boundaries, and dismiss it. In paged WebView reopen
  from the same saved mid-chapter progress with the recap eligible and dismissed. Compare the
  visible story lines. Also open at 0%, turn through the recap and dismiss it at the start.
  Check Dot and Colour on the S22 and tablet. The local-source path still needs the Android retest.

#### S4 Neural TTS — step 1: engine + listening test, debug only (T4) — `blocked` (RV4 deferred, 2026-11-13)
Research is done: `docs/research/neural-tts.md` (GO-WITH-CONDITIONS). The conditions are hard rules: sherpa-onnx
≥ v2.0.1 **without espeak-ng** (GPL); **lexicon voices only** (Kokoro int8 multi-lang v1.1), the pack re-zipped
**without `espeak-ng-data/`**; system TTS stays the default engine; 16 KB alignment check on the new `.so` files;
licence texts in `docs/licenses/`. This card: a second engine behind `TtsController` (system engine unchanged),
synthesis on a background thread one ~150–200-char chunk ahead (reuse `TtsTextUtils` chunking), and a
**debug-only** developer entry that loads a voice pack from app files and plays a chapter. No download UI and no
voice picker yet (no board; Claude draws them after the owner's listening test). Codex never downloads the model;
Claude side-loads it for the test. Tests: chunk queueing, stop/pause/seek, fallback to system TTS on load failure.
**Result (Codex, 2026-11-09):** `tsuzuki/s4` at `b68d437e1`.

- `TtsController.kt` and `TtsTextUtils.kt` let the Debug opt-in use the existing Listen, highlight, stop, pause, resume and seek path with a 180-character limit. Android TTS is the default and receives the interrupted chunk on failure.
- `NeuralTtsClient.kt`, `NeuralChunkQueue.kt` and `KokoroTtsService.kt` send chapter chunks to a private Debug process, prefetch one chunk on a worker, and play its PCM with AudioTrack. `App.kt` bypasses database and app setup in the worker process. `DebugInfoScreen.kt` adds the opt-in in eight locales.
- `app/build.gradle.kts` packages the official Sherpa ONNX 2.0.1 AAR and licence assets in Debug only. The published SHA-256 matches the working copy and committed blob. `.gitattributes` prevents Git newline conversion. `docs/licenses/neural-tts/` contains runtime and model licences and ONNX notices.
- `scripts/package-kokoro-test-pack.py` creates an allowlisted zip from an owner-supplied archive. No model was fetched. `docs/research/neural-tts.md` gives the side-load path and listening steps.

Verification: JDK 21, two scoped Spotless passes, `:app:assembleDebug :app:testDebugUnitTest` (981 tests, 0 failures, errors or skips). Queue tests cover one-ahead synthesis, stop/restart/seek, initial and prefetch failures and surrogate-safe chunking. The pack fixture excludes eSpeak data. All five Debug APKs pass `zipalign -c -P 16 -v 4`, and the packaged Sherpa/ONNX ELF load segments pass 16 KB alignment. The AAR scan found no checked eSpeak API or phontab markers. The Gradle audit confirms the AAR and service are absent from Preview and Release runtime dependencies and merged manifests.

Device review: with a backed-up S22 Debug install, first try the opt-in with no pack and confirm system fallback. Side-load the owner-supplied pack, reopen the reader, and try Listen in both renderers. Check pause/resume, paragraph seek, chapter advance, sleep timer and proper names. The prototype uses US English speaker 0 and the existing speech-rate preference. Real synthesis, sound quality and device timing await the listening test.

Review (Codex RV4, 2026-11-13): Deferred by NT1 gate. Exact pinned AAR SHA-256 matches research; all 16 native libraries have 0x4000 PT_LOAD alignment, Debug/Preview zipalign passes, Debug notices present and Preview has neither neural libraries nor service. Tablet opt-in default-off verified; opt-in with no pack logs missing model in private worker and falls back to Android TTS (highlight/Pause). No model downloaded. Literal espeak/piper grep has legacy token-conversion names, warning text/config names, and Speaker false matches; requested clarification rather than waiving the explicit rule. Excluded by `911a2222f`; revert exclusion to resume. Evidence `app/build/review-evidence/rv4/s4-native-audit-20261113.json` and TTS logs/screenshots after archive.

#### A5.1 Anime: picture-in-picture (T3) — `review` (.worktrees/a5.1, 2026-11-10; lane 1; v0.3)
No board needed (system PiP window). The player enters PiP on Home/recents while playing (setting, default on),
keeps playing, offers play/pause and ±10 s remote actions, returns to the full player on tap, and pauses when the
PiP window is closed. Respect `TsuzukiAnime.enabled`. Tests for the action set and state restore.

**Result (Codex, 2026-11-10):** `098e584f0` on `tsuzuki/a5.1`, based on the committed A4.3a branch.

- `AndroidManifest.xml`, `AnimePlayerActivity.kt` and `AnimePictureInPicture.kt` enable the
  system window on supported devices. Android 12+ uses auto-entry, and older versions use the
  user-leave callback. The private actions offer play/pause and 10-second seeks. A new playback
  request reuses the activity's task and releases the previous player before opening the new one.
- `AnimePlayerModel.kt` holds playback while the window remains visible and pauses on stop.
  `AnimePlayerContent.kt` keeps the video surface mounted and hides the full controls in PiP.
  The existing episode, position and pause state still drive recreation.
- `AnimePlayerSettings.kt`, `AnimePlayerSettingsScreen.kt` and eight locale files add the
  default-on switch to the Video tile. A switch turned off while pinned prevents later automatic
  entry and keeps the current window's controls available.
- JDK 21, scoped Spotless and `:app:assembleDebug :app:testDebugUnitTest` passed. All 1,049
  tests passed, including five new preference and action-policy tests. The merged Debug manifest
  declares PiP on the private player activity. `docs/design/README.md` records the change.
- Device review: on an ARM64 device with Anime enabled, play a fixture and try both Home and
  Recents. Check play/pause, both seeks, expand to the same position, close while playing, and
  start another episode while pinned. Repeat with the switch off and with a paused video. Verify
  the switch fits in Dot and Colour, and check that turning Anime off closes an active player.

#### A5.2 Anime: seasons grouping (T3) — `review` (.worktrees/a5.2, 2026-11-10; lane 1; v0.3)
Owner Q3 kept the four season columns and ignored them in v1. Board `10-anime/AnimeDetails` (seasons row). When a
source or an Aniyomi import fills `parent_id` / `season_number`, show seasons as a row on the parent's details and
group child entries under the parent in the anime library (setting "Group seasons", default on). No schema change
(the columns exist). Tests for grouping and orphaned children.

**Result (Codex, 2026-11-10):** `b9766a725` on `tsuzuki/a5.2`.

- `AnimeLibraryStore.kt` reads season lists through the existing v16/v17 source APIs and
  persists child links and order while retaining favorites. `animes.sq`, `AnimeRepository.kt`
  and `AnimeRepositoryImpl.kt` expose the existing relationships in season order, without
  a schema migration. Aniyomi imports use the same relationships.
- `AnimeDetailsScreen.kt` shows season pills using real numbers or source titles, with the
  current season selected among siblings. `TsuzukiAnimeLibrary.kt`, `AnimeSeasons.kt` and
  `AnimeShelvesScreen.kt` apply the default-on grouping switch to the current shelf. Search
  still shows individual titles. Orphans, cross-source children and malformed cycles stay visible.
- Eight locale files provide the labels. `AnimeDatabaseTest.kt` exercises discovery from a
  source details response, ordering and favorite retention. `AnimeSeasonsTest.kt` covers
  grouping and the preference. JDK 21, scoped Spotless, Debug assembly and all 1,054 unit
  tests passed. `docs/design/README.md` records the implementation.
- Device review: open a series with source-linked or imported seasons in Dot and Colour.
  Tap the season pills, check the selected sibling and its own episodes, and compare the
  library with the switch on and off. Check an orphan, a season on another shelf and search.

#### A5.3 Anime: fansub / group filter (T2) — `review` (.worktrees/a5.3, 2026-11-10; lane 1; v0.3)
Owner Q5 left this out of v1. Board `10-anime/AnimeDetails` (groups). A per-anime exclude list of fansub groups
(`episodes.scanlator`), like manga's excluded scanlators: filter sheet on details; excluded groups hidden from the
episode list, Updates and next-episode logic. If this needs a new table, stop and set `blocked` (Claude writes the
migration). Tests for filtering and next-episode.

**Result (Codex, 2026-11-10):** `ea92e39eb` on `tsuzuki/a5.3`.

- `AnimeEpisodeGroups.kt` and `AnimeLibraryStore.kt` save per-anime exclusions in reserved
  metadata, protect that key from source writes, and keep unlabelled releases visible.
  `episodes.sq` adds read columns for both Updates lists. No migration or episode deletion.
- `AnimeDetailsScreen.kt`, `AnimeDetailsScreenModel.kt` and `AnimeEpisodeTiles.kt` add
  the group control and list to the adaptive episode filter sheet. Eight locales supply
  the label. `AnimePlayerModel.kt` applies exclusions to the episode picker and navigation
  and observes changes during playback.
- `AnimeEpisodeGroupsTest.kt` and `AnimeDatabaseTest.kt` cover matching, an excluded
  current episode, filter persistence and both Updates lists. JDK 21, scoped Spotless,
  Debug assembly and all 1,058 unit tests passed. `docs/design/README.md` records the change.
- Device review: on S22 and tablet, open an anime with multiple release groups, exclude one,
  and verify Details, Updates and Airing, then try Next/autoplay through the excluded release.
  Clear the filter and check that the same releases return. Check Dot and Colour and that
  a source with no group labels does not show the group control.

#### A5.4 Anime: Simkl tracker (T3) — `blocked` (owner: register a Simkl API client; then needs `tsuzuki/a5.3`)
Board `10-anime/AnimeTracking` (Simkl row). Needs a Simkl client id/secret registered by the owner and stored as
build secrets, never in the repo. Then: OAuth login, search/bind, episode progress sync on seen, stored in
`anime_sync` like AniList/MAL (A3.5). Torrent sources and torrent player settings stay out (owner has not asked).
Result: _pending_

#### U1 In-app updater end-to-end (Claude) — `blocked` until 0.1.1 is tagged
Install v0.1.0 from the release on the tablet (release app id), tag 0.1.1, then confirm the daily check and Settings
› About › Check for updates both find 0.1.1, the notification opens `NewUpdateScreen`, Download installs in place,
and Skip this version hides it. Record in V8/V12.

### Overnight batch Q — run rules (2026-11-06 night; overrides the batch Q "How to run" and prompt below)

**Prompt the owner pastes into ONE Codex thread:**

```text
You are working on Tsuzuki (Android, Kotlin, Jetpack Compose) overnight, alone. Read AGENTS.md, then
docs/HANDOFF.md section "Overnight batch Q — run rules", and follow it exactly: do the cards in the order of its
table, one at a time, until the table is finished or you run out of usage. Before each card re-read
docs/HANDOFF.md (Claude edits it during the night) and skip any card that is not `ready`. Never stop at a
blocked card: record it and move on to the next one.
```

**Design first:** every UI card in this table has a board (batch Q: `fixes-2026-11/README.md`; anime and
X/Y: named in the card or `docs/design/boards/INDEX.md`). If a card's screen has **no** board, don't invent one:
set it `blocked` ("needs a board") and move on — Claude draws it.

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
| 2 | Q5 Navigation Library tab | — (Q1 merged) | Continue on the existing `.worktrees/q5` (`tsuzuki/q5`, rebased by Claude); commit there on top of the wip commit |
| 3 | Q3 Save chapters number range | — | Details download sheet + chapter-list range select |
| 4 | Q6 Book details header | Q3 | Same details screen files as Q3 |
| 5 | Q4 Two Default shelves | — | Import fix + one-time data migration (Claude runs it on a device only after a backup) |
| 6 | Q7 Browse tabs + source rows | — | Browse header only |
| 7 | Q12 Remove other themes | — | Must land before Q11 (one page-colour list) |
| 8 | Q11 Reader settings redesign | Q12 | Uses Q12's page-colour list; also touches `NovelPage.kt` |
| 9 | Q9 Accessibility + QoL | Q3, Q5 | Range select (Q3) and nav labels (Q5) |
| 10 | Q8 Overflow + large-font sweep | Q2, Q3, Q5, Q6, Q7, Q11 | Touches every screen — last of batch Q |
| 11 | X1 Reader hook consolidation | Q2 | Extra wave |
| 12 | X3 Restyle check: ReplaceRules + SettingsTranslation | Q11 | Extra wave |
| 13 | X4 Restyle check: DuplicateFinder + Migration | — | Extra wave |
| 13a | Q13 Novel next chapter: instant, never a blank page | — | Owner report 2026-11-07 (lag before next chapter) |
| 13b | Q14 Design fixes from Claude's 2026-11-07 device pass | — | Owner asked for a bug pass of everything built overnight |
| 14 | A1.5 Anime: debug extension install → play | — | Anime lane (owner asked tonight); only new files + a debug entry |
| 15 | A2.2 Anime tables + data layer (migration 36) | — | T4; schema exception for this card only (rule 9) |
| 16 | A2.3 Anime sources + browse catalogue | A1.5, A2.2 | |
| 17 | A2.4 Anime library + Watch tab + anime shelves | A2.3 | |
| 18 | Y1 Mihon skipped fixes, top group | — | Wave Y |
| 19 | Y2 Tablet manga two-page spread | — | Wave Y |
| 20 | Y3 Manga details + Browse manga boards | Q6 | Wave Y |
| 21 | Y4 Synthetic 500-book backup generator | — | Wave Y, no app code |
| 22 | Y5 Restyle checks: Tracking, Quotes, ExportEpub, Upcoming, EditNovel | Q6 | Wave Y |
| 23 | Y6 Tablet TTS panel + foldable | Q11 | Wave Y |

X2 (tablet Updates two-pane) was dropped: Claude saw it already built on the tablet 2026-11-06.

**Optional second Codex thread (anime lane):** the anime rows (14–17) touch only new anime files, a debug entry
and the new migration, so the owner may run them in a **second** thread at the same time. Prompt: same as above
but "do only rows 14–17 of the table, in order". **If a second thread is open, thread 1 skips rows 14–17
entirely** (claimed or not). Both threads: one Gradle build each at most (with two threads, Claude's overnight
reviews mostly wait for the morning because of the two-build limit).

Q1 is Claude's (review/merge tonight). Q10 was done by Claude (AGENTS.md Build + device table).

### v0.1 release — every card (written 2026-11-07 by Claude; owner: v0.1 ships without anime)

**Critical path:** V0 (owner, start now) · V1 → R6 + R7 → V2 upstream merge → **freeze** → V3–V6 (Codex) →
V7 sweep + V8 never-seen screens → fixes → V9 perf → V10 signing check → V11 release candidate → V12 tag + publish.
**Freeze (from V2 until V12):** `main` takes only v0.1 cards and fixes found by V7/V8/V11. Everything else (anime lane,
R8/R10 reworks, R11–R17) keeps committing on its card branch and merges after the tag.
**Codex lane 2 order from now (updated 2026-11-08):** V7.6 (new, v0.1) → V7.5 → V7.4 → V7.3 → (done: V1.1, V2b, R6, R7, V3) → (V4–V6: Claude) → R8 rework →
R10 rework → R11 (resume `.worktrees/r11`) → R12 → … → R17. V3–V6 start from current `main` now (they don't wait
for V2; Claude reconciles them during V2). Everything after V6 is post-v0.1: commit on the card branch, `review`, done.
Lane 1 (anime) is unchanged and is v0.2; no anime entry point may appear in a release build.
**Not in v0.1 (v0.2 or later):** anime (A2.3 → R-A4 and stages 3–4), R8/R10 reworks unless they land before the freeze,
R11–R17, P1.11 storage async migration, Phase 5 features without a card.

| # | Card | Who | Needs | Tier |
|---|---|---|---|---|
| V0 | Owner tasks: repo public, secrets, signing key, permissions | owner | — | — |
| V1 | Review batch: device-check + merge Y2, Y6, R1–R5, R9 | Claude | — | — |
| — | R6 Colour parity, R7 Japanese layout (existing cards) | Codex | R3 | T2 |
| V2 | Tsundoku + Mihon upstream merge (C5), then freeze | Claude | V1, R6, R7 | — |
| V3 | Watch teaser check (switch stays, owner 2026-11-07) | Codex | V2 | T1 |
| V4 | Telemetry out + Tsuzuki `release.yml` | Codex | V2 | T2 |
| V5 | About, licences, privacy, links | Codex | V2 | T2 |
| V6 | CHANGELOG 0.1.0 + version | Codex | V2 | T1 |
| V7 | Release-build runtime sweep (RUNTIME_QA gaps) | Claude | V3–V6 | — |
| V8 | Built-but-never-seen screens (WORKPLAN 4A) | Claude | V3–V6 | — |
| V9 | Cold start + baseline profile re-collect | Claude | V7 fixes | — |
| V10 | Signing continuity check | Claude + owner | V0, V4 | — |
| V11 | Release candidate on all three devices | Claude + owner | V9, V10 | — |
| V12 | Tag v0.1.0, publish, verify updater | Claude + owner | V11 | — |

#### V0 Owner tasks (owner) — `done` (item 2 done by Claude at the owner's request, 2026-11-07)
**Item 2 result:** release keystore `C:/Users/Manoj/Documents/Tsuzuki-signing/tsuzuki-release.jks` (PKCS12, RSA 4096,
alias `tsuzuki`, `CN=Tsuzuki`, cert SHA-256 `FF:CD:C7:43:A6:69:78:AE:24:CD:76:0B:44:78:32:71:A3:CC:18:16:84:4C:6A:78:6C:C9:EF:71:8E:A1:F6:C7`);
owner confirmed 2026-11-07: folder backed up and password moved to a password manager.
Secrets `SIGNING_KEY_BASE64`, `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` set on `Manojx2005/Tsuzuki`. Verified
by building `assembleRelease -Penable-updater` through the CI env-var signing path: APK signed `CN=Tsuzuki`, same
fingerprint. **Consequences:** the owner's phone (debug-signed install) needs backup → uninstall → install → restore at
V11; CI preview builds now use this key too, so the `.debug` preview installs on the tablet/S22 (debug key) must be
reinstalled once (test data only).
1. Make `Manojx2005/Tsuzuki` public, or name a separate public releases repo (then `GITHUB_REPO` in
   `AppUpdateChecker.kt` changes in V4). The updater 404s until the repo it reads is public.
2. **Not done yet (Claude checked 2026-11-07: no secrets, no releases).** Create a release keystore and keep it safe
   (losing it means users can never update in place). In Git Bash, outside the repo:
   `keytool -genkeypair -v -keystore tsuzuki-release.jks -alias tsuzuki -keyalg RSA -keysize 4096 -validity 36500`
   then `base64 -w0 tsuzuki-release.jks > key.b64` and add the secrets (repo → Settings → Secrets → Actions, or
   `gh secret set NAME -R Manojx2005/Tsuzuki`): `SIGNING_KEY_BASE64` (contents of key.b64), `KEY_STORE_PASSWORD`,
   `KEY_ALIAS` (`tsuzuki`), `KEY_PASSWORD`. Delete key.b64 afterwards. Both workflows use these names.
3. Tell Claude which key signed the release app on your phone (`io.github.manojx2005.tsuzuki`): the same keystore
   as the secrets, or a different one. V10 checks it; a different key means v0.1 can't install over your data.
4. Answer the permission questions (V3 Watch switch, device use, phone upgrade, test installs, language switch,
   pushes). Answers are recorded under "Owner permissions for v0.1" below.

#### V1 Review batch (Claude) — `done` except leftovers below (merged into main as `3fa896b3e` on 2026-11-07)
Merge Y2, Y6, R1, R2, R3, R4, R5, R9 into a `review/night5` branch, resolve overlaps, build, unit tests, install on
the tablet + S22. Per card: its "Claude device checks" list. Plus C4: restore R1's synthetic 500-book backup into a
**scratch** install and record `gfxinfo framestats` for Home, Library (each tab), Updates, History, Browse; and R2:
`adb shell am kill` on every screen in R2's Result. Merge into `main` only what passes; failures go back as fix cards.

**V1 progress (Claude, 2026-11-07):** `review/night5` = main + R3, R1, R2, R4, R5, R9, Y2, Y6 (string conflicts: both
sides kept; R2 + Y6 combined in `TsuzukiReaderHooks.kt`). `assembleDebug` + 936 unit tests green; `assemblePreview`
installed on tablet + S22, both launch, crash buffers empty.
- Pass: **Y6** (tablet landscape docks the LISTEN panel; playback survives rotation; portrait < 900 dp keeps the
  floating pill by design). **R4** (Library menu: icons, groups, red destructive row). **R5** (tablet Settings two-pane).
- Found: **V1.1** (selected tab lost after process death; not from this batch).
- S22: **R3** pass (word-boundary wraps on Library, Updates, Home); Library/Updates phone layouts fine.
- Merged into main as `3fa896b3e` with these **leftovers for Claude's next session** (code already in main):
- Pending: **Y2** (needs a *paged* manga: the only manga, Nano Machine, opens as a webtoon), **R1/C4** scratch 500-book
  frame timing, **R2** in-Library state (after V1.1), **R3/R9** on the S22, R9 widgets placed.
- Polish notes for V8 (existing, not regressions): Dot System/Light/Dark segment not uppercased and in body font;
  Settings TIME FORMAT label wraps while its value truncates; "GRAYSCALE" vs British "colour"; Listen panel voice
  label shows a raw locale ("en-US-language"); Updates "Library last updated: Never" is italic body text, not
  uppercased in Dot.

#### V1.1 Selected tab lost after process death (T3) — `done` (merged via `review/night6`, 2026-11-07) (found by Claude in V1, 2026-11-07; release-blocking)
Repro (preview build `.debug`, tablet, Dot): Library → MANGA tab → Home key → `adb shell am kill
io.github.manojx2005.tsuzuki.debug` → open the app from **recents** (not the launcher). Expected: Library, MANGA
selected. Actual: Home. No crash. Evidence: `r2-before.png` / `r2-after2.png` in the repo root (untracked, local).
`HomeScreen.kt` keeps `retainedTsuzukiTab` / `retainedLibraryKey` in `rememberSaveable` and feeds
`TsuzukiNavigation.restore(...)` into `TabNavigator(tab = initialTab)`, so find where the restored value is lost
(is `savedInstanceState` non-null in `MainActivity.onCreate`? is `isTsuzuki` false on the first frame so
`initialTab` is computed before the theme resolves? does the startup overlay or Voyager's root `Navigator` rebuild
Home?). Fix with the smallest upstream change; add a test for `TsuzukiNavigation.restore`. Then R2's in-Library
state (shelf, search, selection) can be checked; Claude re-tests both on the tablet + S22. Not caused by the V1
batch (none of its cards touch `HomeScreen.kt` or `MainActivity.kt`), but R2's Result claimed the root tab already
survived process death.

**Result (Codex, 2026-11-07):** `afee7c2e6` on `tsuzuki/v1.1` (`.worktrees/v1.1`).
- Upstream `presentation/util/Navigator.kt` keys the outer animation by Voyager's saved screen key. The previous default keyed by the screen object, whose identity can change on deserialization and change the saveable composition path under Home.
- `TsuzukiNavigationTest.kt` checks that a serialized Manga tab resolves to the separate Library tab or the combined Library according to the active setting. Per-file Spotless passed twice. `assembleDebug` and `testDebugUnitTest` passed with 934 tests and zero failures.
- Claude device check: on the tablet and S22, background Library → Manga, kill the background process, then return from Recents and verify both selections. Check another root tab and the R2 in-Library shelf, search and selection state. Android process recreation still needs this device check.

#### HOME-SWIPE Swipe the Home cover stack (T2) — `done` (merged via `review/post-v01`, 2026-11-08)
On Home layouts that show the MotionStack cover stack, let a horizontal swipe select the next or previous book. Keep the tap action and existing shuffle and reduced-motion behavior. Use the MotionStack and phone Home boards in Dot and Colour.

**Result (Codex, 2026-11-07):** `a3a8a4220` on `tsuzuki/home-swipe` (`.worktrees/home-swipe`).
- `TsuzukiCoverStack.kt` uses a horizontal drag detector. A left swipe advances and a right swipe goes back, with wraparound. Taps advance through the same callback. The existing shuffle plays in reverse for the previous book.
- `TsuzukiHomeScreen.kt` applies the selected index to the title, progress, Resume action and cover tint. The hint lists both inputs in all eight locales. `docs/design/README.md` records the MotionStack status.
- Per-file Spotless passed twice. All eight string files parsed. `assembleDebug` and `testDebugUnitTest` passed with 933 tests and zero failures.
- Claude device check: use compact Home with at least three books in Dot and Colour. Swipe left and right through both ends, tap, scroll vertically starting on the stack, and check reduced motion and Resume after a swipe. Tablet layouts that use a single hero have no stack.

**V1.1 device check (Claude, 2026-11-07):** tablet preview from `review/night6`: Library → MANGA → Home → `am kill` →
Recents returns to Library with MANGA selected (before the fix: Home). No crash. 934 tests green.
Still to check on devices in V7/V8: R2 in-Library state (shelf/search/selection) now that V1.1 is in, V2b tracker
restore + long tab badges, R6 Colour headings, R7 Japanese labels, V3 Watch teaser.

#### V2 Upstream merge + freeze (Claude) — Tsundoku part `done` 2026-11-07 (1 commit, f118703a2 TTS paragraph split, merged; 933 tests green; S22 preview from main `3da3254c7`: TTS plays, highlights the spoken paragraph, progress advances, no crash); Mihon part → V2b (Codex); **freeze started 2026-11-07** (V2b, R6, R7, V1.1 merged): `main` takes only v0.1 work until the tag
`scripts/upstream-report.sh`; merge `tsundoku/main`; pick the Mihon fixes listed by the report (log in
`docs/UPSTREAM.md`). Build, tests, quick device smoke. Then announce the freeze in this file.

#### V2b Mihon release picks (T3) — `done` (merged via `review/night6`, 2026-11-07) (written by Claude 2026-11-07, owner: leave the rest to Codex)
Claude ran `scripts/upstream-report.sh mihon`: 224 commits not taken, most already logged as picked or skipped in
`docs/UPSTREAM.md`. For v0.1 take only these, one `git cherry-pick -x` each on `tsuzuki/v2b` (skip any already in
main or logged in UPSTREAM.md; if one conflicts with a Tsuzuki hook, resolve keeping the Tsuzuki behaviour, or skip
it and say why):
- `35bb84705` reader: GPU texture leak when a decoded page is no longer usable (#3872)
- `a82baac3e` tracking: Kitsu score-type crash from login (#3887)
- `1e054ea14` tracking: inverted MAL OAuth expiry check (#3927)
- `40f552981` duplicates: a blank title matched every library entry (#3308)
- `4b48a84ec` backup: keep a stored track's remote entry when restoring
- `6d4ed6cb2` extensions: backport class loader before Android 17
- `b19c8abeb` tab badges stay visible when the label is too long (the final version; not 7a5632c01/e8977b4a6)
- `0864ce0f4` redact sensitive headers in logcat (#4156): privacy, wanted for v0.1 even though it is flagged T/D
Log each pick (or skip + reason) in `docs/UPSTREAM.md`. Build + unit tests. Result: list, test count, what Claude
should check on a device (tracker login only if the owner approves; reader paging; Library duplicates; tab badges).

**Result (Codex, 2026-11-07):** `4e214de47` on `tsuzuki/v2b` (`.worktrees/v2b`).
- `ed17f1112` adapts Mihon `4b48a84ec` to Tsundoku's upstream `MangaRestorer.kt`. A stored tracker entry keeps its linked remote IDs and metadata, and a backup can advance its progress.
- `2c7bac0b2` cherry-picks Mihon `b19c8abeb` into upstream `Tabs.kt` in presentation-core. Badges get measured before the text.
- `docs/UPSTREAM.md` records all eight decisions. N2 already covered blank-title matching and header redaction. The current MAL expiry check and extension class loader cover those picks. The GPU viewer and Kitsu score-type code are absent in this fork.
- Per-file Spotless passed twice. `assembleDebug` and `testDebugUnitTest` passed with 933 tests and zero failures.
- Claude device check: in a scratch install restore a tracked book over the same tracker with different remote IDs and higher chapter progress. Verify the old link and higher progress. Check long Library tab labels with counts, then smoke-test manga paging and duplicate lookup. Tracker login requires owner approval.

#### V3 Watch teaser check (T1) — `done` (merged via `review/night6`, 2026-11-07) (owner 2026-11-07: keep the Read / Watch switch as a teaser)
`presentation/tsuzuki/home/TsuzukiHomeScreen.kt` `WatchPlaceholder`: the copy must say anime is coming in a later
version, with no sample numbers, fake titles or controls that look usable; nothing on it may open anime code in a
release build (only Back). `TsuzukiLibraryMediaSwitch.kt`, Updates tiles and Stats: no anime option in release.
Fix only what fails; list the checks in Result. Claude checks: preview build, Home → Watch, both themes, both devices.

**Result (Codex, 2026-11-07):** `69aa2ad3e` on `tsuzuki/v3` (`.worktrees/v3`).
- `TsuzukiHomeScreen.kt` keeps the switch and shows a short later-version message with a Back to reading action. Android Back also returns to Read. The feature tiles and their strings were removed in all eight locales. `docs/design/README.md` records the teaser.
- The Library media switch, Updates tiles and Stats have reading options only. The debug control APK declares both anime activities. The four ABI release APKs and the universal release APK contain neither activity nor the mpv/player/FFmpeg libraries.
- Per-file Spotless passed twice and the eight teaser strings parsed. `assembleDebug`, `testDebugUnitTest` (933 tests, zero failures) and `assembleRelease` passed.
- Claude device check: in the preview build, open Home → Watch in Dot and Colour on the S22 and tablet. Check both back paths and verify that Library, Updates and Stats only offer reading features.

#### V4 Telemetry out + Tsuzuki release workflow (T2) — `done` (Claude 2026-11-07; see Result)
`PRIVACY.md` promises no telemetry. Keep `Config.includeTelemetry` and the Firebase code paths for Tsundoku merges,
but the Tsuzuki release must never pass `-Pinclude-telemetry`.
- Rewrite `.github/workflows/release.yml` for Tsuzuki: run on tag `v*` pushes, gate
  `github.repository == 'Manojx2005/Tsuzuki'`, JDK 21, `./gradlew assembleRelease -Penable-updater` (no telemetry),
  sign with the same secret names as `tsuzuki-preview.yml`, APK names `tsuzuki-<tag>.apk` (universal) and
  `tsuzuki-arm64-v8a-<tag>.apk` (+ the other ABIs the build makes), SHA-256 file, release notes = the matching
  `CHANGELOG.md` section. Copy the signing step style from `tsuzuki-preview.yml`. Remove Tsundoku-only jobs.
- Delete `app/google-services.json` (owner approved 2026-11-07) after confirming release builds without it; note it
  in `docs/UPSTREAM.md` (a Tsundoku merge that restores it gets resolved by deleting again).
- Add a Gradle check or unit test that release builds have `BuildConfig.TELEMETRY_INCLUDED == false`.
- Don't run the workflow and don't create tags. Claude checks: `assembleRelease -Penable-updater` locally, then the
  APK has no `com.google.firebase` classes.

**Result (Claude, 2026-11-07):**
- `.github/workflows/release.yml` rewritten for Tsuzuki: tag `v*` on `Manojx2005/Tsuzuki`, checks the tag equals
  `versionName`, `assembleRelease -Penable-updater` (never `-Pinclude-telemetry`), signs through Gradle with the same
  secrets as `tsuzuki-preview.yml`, fails if any `Lcom/google/firebase/` class is in the APK, renames to
  `tsuzuki[-<abi>]-<tag>.apk`, notes = the CHANGELOG section + SHA-256 list (checksums in the notes, not as an asset:
  the updater treats an asset without an ABI in its name as the universal APK), published `--latest`.
- `app/google-services.json` deleted (owner-approved). The telemetry module already compiles its no-op source set
  and no Firebase dependency without the flag; local `assembleRelease -Penable-updater`: 0 Firebase classes,
  `io.github.manojx2005.tsuzuki` 0.1.0 (24), label Tsuzuki.
- **Updater channel bug fixed (found while doing V4):** preview (`rNNNN`) and stable (`vX.Y.Z`) releases share one
  repo and both read `/releases/latest`, and the preview workflow published `--latest`. A stable 0.1.0 install seeing
  `r3000` would compare `3000 > 0` and offer the preview APK; a preview install seeing `v0.1.0` would hit
  `"0.1.0".toInt()`. Now: `tsuzuki-preview.yml` publishes `--prerelease`; `ReleaseServiceImpl` (data) makes preview
  builds read `/releases?per_page=30` and take the newest `r…` tag; `GetApplicationRelease.isNewVersion` (domain)
  ignores a tag from the other channel. 2 new tests in `GetApplicationReleaseTest` (5/5 pass). Note: preview builds
  installed before this change read `/releases/latest` and won't see new (pre-release) previews; reinstall them once.
- **Signing (feeds V0 / V10):** the repo has no releases and no Actions secrets yet (`gh release list` / `gh secret
  list` empty), and this machine has no `keystore.properties`, so local builds (preview and release) are signed with
  the per-machine **Android debug key** (`CN=Android Debug`, SHA-256 `6c1e396d…e17a`). Claude's recommendation: the
  owner creates a real release keystore and the four secrets (commands in V0), and the owner's phone does a one-time
  backup → uninstall → install → restore at V11 (its current install is almost certainly debug-signed on the laptop).

#### V5 About, licences, privacy, links (T2) — `done` (Claude 2026-11-07; see Result; device look at the About credits in V8)
About screen: app name, version, "Based on Tsundoku and Mihon" credit with links, GitHub link = the V0 releases
repo, open-source licences list includes the bundled fonts (`docs/licenses/`), QuickJS host and the LNReader plugin
index terms. `PRIVACY.md`: accurate for v0.1 (no telemetry; network calls: sources, trackers, translation provider if
configured, updater). `TsuzukiLinks.kt`: no link points at tsundoku-otaku. `NOTICE`/licence files present for
Apache-2.0. List each check in Result. Claude checks: About + licences on both devices, both themes.

**Result (Claude, 2026-11-07):** `PRIVACY.md` is already accurate for v0.1 (sources, repos, trackers,
translation, fonts, updates; no analytics, now enforced by V4). About (`TsuzukiAboutScreen.kt`) already credits Mihon,
Tsundoku, LNReader plugins and Keiyoushi with licences, links source code and privacy policy to
`Manojx2005/Tsuzuki`, and the Licences screen lists Gradle dependencies. Added: credits for the bundled fonts
(Doto, Instrument Serif, Space Mono, Manrope, Literata; DotGothic16, M PLUS 1 Code, Noto Serif JP, Zen Kaku Gothic
New: OFL-1.1, texts in `docs/licenses/`). No link points at tsundoku-otaku except the stock-theme Mihon credit. No
`NOTICE` file needed (Mihon/Tsundoku ship none). V8: look at the About credits rows (long names may wrap).

#### V6 CHANGELOG 0.1.0 + version (T1) — `done` (Claude 2026-11-07; see Result)
Move Tsundoku's changelog to `docs/CHANGELOG-tsundoku.md` (header: "Inherited from Tsundoku; Tsuzuki's own
history is in CHANGELOG.md"). New `CHANGELOG.md`: same Keep-a-Changelog format, `## [0.1.0]` written for users
from this file's log (Dot + Colour themes, Home, library shelves, novel reader + TTS, manga reader, tablet layouts,
downloads, backups, widgets, Japanese UI); no card IDs, no sample numbers. Leave `versionName = "0.1.0"`;
`versionCode` stays 24 unless Claude reports in V10 that the phone's install is ≥ 24 (then bump above it).

**Result (Claude, 2026-11-07):** Tsundoku's changelog moved to `docs/CHANGELOG-tsundoku.md`.
New `CHANGELOG.md` with `## [0.1.0]` (Added / Improved / Fixed / Other), written for users; `release.yml` publishes
this section as the release notes. `versionName` stays `0.1.0`, `versionCode` 24 (V10 checks the phone's install).
`docs/UPSTREAM.md`: keep ours for `CHANGELOG.md` conflicts; delete `google-services.json` again if a merge restores it.
**Before tagging (V11):** confirm the changelog lines that are not device-verified yet: process-death state (needs
V1.1), the foldable hinge layout (no foldable here; drop the clause if untested), and the two-page spread (Y2).

#### V7 Release-build runtime sweep (Claude) — `ready` after V3–V6
On an optimised (preview or release-type) build, tablet + S22, Dot + Colour, close every gap in
`docs/RUNTIME_QA.md`: P1.1 rotation + process death (readers, sheets, queued work), P1.2 downloads (novel + manga,
pause/resume, force-stop), P1.3 backup round trip, P1.4 EPUB + mass import (owner-approved temporary files), P1.5
translation safe failure without credentials, P1.6 TTS remaining combos + background + headset, P1.7 one
owner-approved extension install/update/uninstall, P1.8 widget, P1.10 separate-library mode, search, empty and
error states. Each bug → a `V7.n` fix card for Codex (logcat + screenshot + repro). Update RUNTIME_QA.

**V7 progress (Claude, 2026-11-07, preview from `review/night6`):**
- **R2 pass** (tablet): Library shelf + search text and applied search survive `am kill` → Recents. Library search
  filters on submit (Enter), not live; that is Tsundoku's existing draft/submitted split, not a regression.
- **V3 pass** (S22): Watch page says anime is coming in a later version; only a Back to reading button.
- **R6 pass** (S22, Colour): italic serif headings over Manrope body, palette follows the current book; Home + Library.
- **R7 mostly pass** (S22, app language Default = Japanese): dates, counts, tabs, tiles, badges localized. **Bug →
  V7.1:** the pill-nav label ライブラリ wraps mid-word ("ライブラ / リ") on the phone, selected and unselected.
- **Downloads (P1.2) pass** (S22): Updates → Save all queued 6 novel chapters; the tile turned "saved" and the queue
  emptied with no errors.
- **V7.2 fixed by Claude:** More tiles in Japanese cut off ダウンロードキュー / データとストレージ with "…"; tile titles now
  auto-size down to 75 % before ellipsizing (`TsuzukiMoreScreen.kt`). S22: both labels whole.
- **Light mode bug fixed (owner report 2026-11-07):** picking Light (or System on a light phone) mixed a light Material
  scheme into the dark-only Dot/Colour designs (white Settings pane, invisible labels, dark status icons on black).
  Owner: "just remove it". Dot and Colour now always use night mode and Appearance no longer shows the switch.
  `TsuzukiThemeModeTest` 2/2; tablet with Light stored renders correctly.
- **Novel reader process death pass** (tablet, TextView): scrolled to 92 %, `am kill`, Recents → same paragraph, 92 %.
- **Translation (P1.5) pass:** defaults are on-device ML Kit (no key, nothing paid) with target `en`; toggling
  Translate on an English chapter changes nothing and doesn't crash. Polish note: no hint when source = target.
- **Manga reader smoke pass** (tablet, webtoon): Nano Machine ch. 1 loads from the source, long-strip scroll to 11/24,
  page counter updates, no crash.
- Still open in V7: backup round trip (needs the system file picker; owner can do it on the release build), EPUB and
  mass import, one extension install/update/uninstall, widget placement, separate-library mode, Y2 spread (needs a
  paged manga), R1 500-book frame timing (scratch install).
- **Downloads slow (owner report 2026-11-07):** novel pacing defaults were 3 s + 0-1 s jitter per request, 1 permit,
  1 book at a time (Tsundoku). Now 1 s + 0-0.5 s, 2 permits, 2 books at once; per-source pacing and a source's
  declared minimum are unchanged. Devices that ever saved their own values keep them (Settings → Novel downloads).
- **Browse → Migrate bugged (owner report 2026-11-07):** Migrate is the pager's third page but the header had two
  segments, so nothing was selected and the indicator collapsed; the list was stock-styled. Now Migrate shows as a
  selected third segment while open, and the list uses the Sources rows (`TsuzukiMigrateSourceList`). Tablet: header,
  rows, tap → source's books, Sources segment back; `BrowseHeaderSectionsTest` 2/2.
  **Follow-up (owner 2026-11-07):** Migrate is now always the third segment (no overflow entry), and the Browse pager
  holds only the chosen media's three pages, so swipes move Sources ↔ Extensions ↔ Migrate and never cross Novels /
  Manga; the pills switch media and keep the section. Tablet: 4 swipes stop at Novels Migrate; Manga keeps Migrate;
  3 back-swipes stop at Manga Sources. `BrowseHeaderSectionsTest` 4/4.
- **App update notification (owner 2026-11-07):** `TsuzukiAppUpdateJob` checks GitHub daily (network required, release
  and preview builds with the updater only) and notifies once per new, unskipped version; tapping opens the existing
  update screen. Tablet: job scheduled (WorkManager job "TsuzukiAppUpdateJob"), simulated tap opens UPDATE READY with
  Download / Not now / Open on GitHub / Skip; 946 tests green. First real end-to-end check needs v0.1.0 → a later tag.
- V8 polish note: when More scrolls, tiles slide under the transparent status bar and the clock overlaps their text.
  `done` (merged via `review/night7`, 2026-11-08), was `review` (`tsuzuki/v8-more-insets` at `afd41c61f`). `TsuzukiMoreScreen.kt` applies the status-bar inset outside
  the scroll viewport so tiles stay below the clock and the themed background fills the inset. `docs/design/README.md`
  records the change. Per-file Spotless passed twice, the debug APK built, and 954 tests passed (0 failures or skips).
  Claude: scroll More to both ends on the S22 and tablet in Dot then Colour. Check that no tile crosses the clock
  and that the first title and final tile remain fully visible when scrolled to their respective ends.
  **Claude 2026-11-08:** S22 Colour, scrolled mid-list: tiles clip below the clock, last tile fully visible. Pass (in `review/night7`).
- Note for testers: after closing the preview app, a launcher tap opened the owner's `.dev` app by mistake (no
  changes made there). Always start the test app with `monkey -p io.github.manojx2005.tsuzuki.debug`.
- Owner 2026-11-07: Claude tests on tablet + S22 only; the owner checks the release build on their own phone
  (replaces the phone parts of V10/V11).
- Owner's phone `R5GL23ZYE1V` was connected but not visible to adb on this PC (not even "unauthorized"): needs USB
  debugging allowed for this computer before V10/V11.

#### V7.1 Japanese nav label wraps mid-word (T2) — `done` (Claude 2026-11-07: a label without spaces stays on one line and auto-sizes down to 75 %; labels with spaces still wrap between words; `TsuzukiNavLabelTest` 3/3; S22 Japanese: ライブラリ on one line, no crash)
S22 (1080 px, 384 dp wide), preview, Colour and Dot, app language Japanese: the bottom pill nav shows ライブラリ as
"ライブラ" / "リ" on two lines, selected and unselected (evidence: `r7-preview-home.png`, `r7-preview-lib.png` in the
repo root, untracked). Fix in the Tsuzuki pill-nav label composable (`theme/tsuzuki/TsuzukiNav.kt` or wherever the
pill item text is drawn): keep each label on one line (`maxLines = 1`, `softWrap = false`) and shrink to fit the
item width (auto-size between the current size and ~80 %, or measure and step down) instead of wrapping; never
break inside a word. Check the other locales' longest labels (de, fr, es, pt-BR) at 360 dp and with font scale
1.3. Test: a pure function for the label size choice. Claude re-checks on the S22.

#### V7.3 Japanese for the rest of the app (T2) — `done` (merged via `review/night7`, 2026-11-08)
Claude added 87 strings (main-file Settings strings + novel-module headings, commit `5a523840d`). Still missing in
Japanese: ~120 in `i18n/.../base/strings.xml` and ~860 in `i18n-novel/.../base/strings.xml` (only 6 of 882 novel
strings had Japanese). Add them to the matching `ja/strings.xml`, novel-reader and settings screens first. Rules: keep
every placeholder exactly (`%s`, `%d`, `%1$s`, `{TEXT}`…; write a check like Claude's `ja_settings.py`), escape `&`
and `'`, product names stay Latin (Tsuzuki, Dot, Colour, LNReader, Mihon, Shizuku), titles as short noun phrases,
descriptions in polite です/ます, match the existing ja file's terms (小説, マンガ, チャプター, ライブラリ, 本棚).
Claude checks the S22 in Japanese (Settings, novel reader settings, Downloads, Translation).

**Result:** `tsuzuki/v7.3` at `9d066cb97`. Added Japanese for the 129 missing translatable shared strings and
862 missing novel strings. Moved literal labels in Browse, Appearance, Font Manager, global novel settings,
and the in-reader settings into resources so they follow the app language.

- `i18n` and `i18n-novel` base and Japanese `strings.xml` files cover the existing and newly resource-backed text.
- Upstream `SettingsBrowseScreen.kt`, `SettingsAppearanceScreen.kt`, `FontManagerScreen.kt`,
  `SettingsNovelReaderScreen.kt`, and `NovelPage.kt` use the localized settings labels.
- `scripts/check-japanese-strings.py` verifies all 2,473 translatable keys for Japanese coverage and checks
  placeholders and markup. Per-file Spotless passed twice, debug built, and 954 tests passed without failures
  or skips. `docs/design/README.md` records status.
- Claude: check S22 in Japanese, including the named Settings pages, novel reader controls in both renderers,
  Downloads and Translation. Check wrapping at large font scale and confirm the labels also render in English.

**Claude review 2026-11-08 (`review/night7` = main + V7.5 + V7.4 + V7.3 + V8-more-insets, clean merges, not in main yet):** code reviewed, 959 unit tests green, `assemblePreview` installed on the tablet + S22, crash buffers empty. S22 Japanese: Settings main and Novel reader settings fully Japanese. Pass. New keys exist in English + Japanese only (other 6 locales fall back to English). Leftover truncated `ネイティブ（TextVie…` → V7.6 item 5. Merge into main waits for the owner (freeze: v0.1 fixes allowed).

#### V7.4 Novel reader: keep scrolling text out of the notch / gesture area (T3) — `done` (merged via `review/night7`, 2026-11-08)
Claude's `84d722dce` adds `novelSafeInsets()` to the top/bottom margins, so a chapter starts below the notch. While
scrolling, text still passes under the camera cutout and the gesture bar because the margin is content padding. Like
LNReader, reserve the safe area on the scroll viewport in both renderers: TextView (scroll container padding +
`clipToPadding = true`, padded band painted with the page colour) and WebView (fixed top/bottom bands or viewport
padding via CSS env(safe-area-inset-*) / JS-provided px). The user's margins stay content padding inside that.
Paged mode: page height excludes the safe area. Reader menu shown/hidden must not shift text. Test both renderers,
Dot + Colour, S22 (punch-hole) and tablet; screenshots at chapter start and mid-chapter.

**Result:** `tsuzuki/v7.4` at `f99094214`. Both renderers use the existing clipped native viewport, so scroll range
and WebView page height use the same physical safe area. Insets ignore system-bar visibility, and the reserve for
the app's optional reading status bar sits on its selected edge. The viewport bands use the page background.

- Upstream `ReaderActivity.kt` combines window insets and reading status bar height, anchors that bar in the safe
  area, and paints the viewport background. `NovelSafeInsets.kt` supplies the stable insets and bar reserve.
- Upstream `NovelViewer.kt` and `NovelWebViewStyler.kt` use the reader's selected margins as content spacing.
- `NovelSafeInsetsTest.kt` checks top and bottom placement and preserves rotated side cutouts. Per-file Spotless
  passed twice, the debug APK built, and all 954 app unit tests passed without failures or skips.
- Claude: S22 and tablet, Dot and Colour, TextView scroll and WebView scroll/paged. Take screenshots at chapter
  start and during scroll. Toggle the menu mid-chapter and check the same text stays in place. Check both reading
  status bar edges, page background continuity, rotation, and a manga fullscreen/cutout smoke check.

**Claude review 2026-11-08 (`review/night7` = main + V7.5 + V7.4 + V7.3 + V8-more-insets, clean merges, not in main yet):** code reviewed, 959 unit tests green, `assemblePreview` installed on the tablet + S22, crash buffers empty. S22: first line starts below the punch-hole; opening the menu mid-chapter leaves the text in place. Pass. Not yet run: WebView paged, reading status bar at the top edge, rotation. Merge into main waits for the owner (freeze: v0.1 fixes allowed).

#### V7.5 Novel reader: can't scroll after Previous chapter (T3) — `done` (merged via `review/night7`, 2026-11-08)
Owner: reading chapter 41 (Lord of the Mysteries, release build), tapped back to the previous chapter (40, already
read) and could not scroll any more. Reproduce on tablet + S22 in both renderers (TextView, WebView), infinite scroll
on and off: open chapter N > 1, tap ‹ Previous (bottom bar and chapter-end card), try to scroll up/down, tap, open
the menu. Suspects: the previous chapter loads at its saved 100 % position and the scroll restore or Q13's
next-chapter warm-up leaves the container locked/consuming gestures, or the TTS mini-controls overlay intercepts.
Fix the cause, add a regression test for the position/lock logic. Claude device-checks.

**Result:** `tsuzuki/v7.5` at `50319ea66` (includes `3f865b77e`). Both renderers restored a saved 100% position on Previous. Previous now
opens a completed chapter at its start. A chapter already loaded by infinite scroll becomes active and is positioned
by its renderer when it receives the chapter. Direct opens and full loads with valid partial progress still restore
that position. Each Previous request is consumed once so a subsequent renderer switch can restore normal progress.

- `NovelEntryProgress.kt` shares the entry rule, with three regressions in `NovelEntryProgressTest.kt`.
- Upstream `ReaderViewModel.kt` and `ViewerChapters.kt` carry a distinct request for each Previous tap.
  `NovelChapterNavigationTest.kt` checks repeat navigation to the same target and renderer redispatch.
- Upstream `NovelViewer.kt` cancels stale restores and positions a buffered chapter at its visible text block.
  `NovelWebViewViewer.kt`
  cancels the superseded content job and reuses a buffered chapter only when its document is ready.
- `TsuzukiAdjacentChapterHooks.kt` hands the backward jump to the renderer. `docs/design/README.md` records status.
- Per-file Spotless passed twice. Debug APK built and all 959 app unit tests passed, 0 failures or skips.
- Claude: on S22 and tablet, use Previous from chapter 41 to already-read chapter 40. Check upward/downward scroll,
  menu taps, and progress after moving backward twice. Scroll into chapter 41 again and tap Previous again while
  40 remains loaded. Cover TextView and WebView with infinite scroll on/off, then
  the WebView paged edge and any available Previous action at chapter end. The original touch symptom awaits device
  reproduction and verification. The code change addresses the confirmed end-position restore and buffered jump paths.

**Claude review 2026-11-08 (`review/night7` = main + V7.5 + V7.4 + V7.3 + V8-more-insets, clean merges, not in main yet):** code reviewed, 959 unit tests green, `assemblePreview` installed on the tablet + S22, crash buffers empty. S22 TextView, Colour: Ch. 108 → ‹ Previous → Ch. 107 opens at 0 % and scrolls; ‹ again → Ch. 106 scrolls down and up. Pass. Not yet run: WebView renderer, infinite scroll off, tablet. Merge into main waits for the owner (freeze: v0.1 fixes allowed).

#### V7.7 Smoother chapter-end Next chapter (T2) — `blocked` (2026-11-08, `.worktrees/v7.7`) (owner 2026-11-08: 0.1.1; v0.1.0 is tagged)
Owner: RC4's fix works but the tap → next chapter switch should feel smoother. Board: `06-reader/ChapterEnd`. Text reader (`NovelViewer`) with
infinite scroll **off** (now non-default). Today the tap clears the page and the new chapter pops in at the top.
Look at the chapter-end card board, then: show press feedback at once, keep the card until the next chapter's
content is ready (no blank frame), and bring the new chapter in with the reader's existing transition
(`TsuzukiReaderTransition.kt`) rather than a hard cut. Don't regress the touch fix in
`tsuzuki/reader/NovelChapterEndTouch.kt` (fling into the end, then tap: must load on the first tap, no menu flash).
Check the WebView reader's card (`chapter-end-card.js`) for the same feel. Device check on the tablet + S22.

**Result (Codex, 2026-11-08):** `tsuzuki/v7.7` at `16f713ff8` (base `c0a0015a4`).

- `TsuzukiChapterEnd.kt` shows a pressed state and disables repeat taps while the chapter-end request is pending.
- `NovelChapterEndTransition.kt` keeps an in-memory copy of the drawn viewport above the reader and applies the existing enter and outgoing animations after the destination can draw. It releases the frame on errors, superseded loads and viewer teardown. If the window cannot be copied, navigation proceeds without the frame transition.
- `NovelViewer.kt` reveals after the next text layout and progress restore. The RC4 touch handling still owns the first tap after a fling.
- `chapter-end-card.js` and `NovelWebViewViewer.kt` share the pending feedback and wait for visual readiness before revealing the next page. The WebView keeps its old document while fetching and processing the next one.
- `ReaderActivity.kt` validates the outgoing chapter before forwarding the card request to the existing chapter loader.
- `docs/design/README.md` records the implementation status.

JDK 21, two per-file Spotless passes, `:app:assembleDebug` and `:app:testDebugUnitTest` passed
(968 tests, 0 skipped). A local JavaScript smoke test covered duplicate activation,
the chapter ID on all three actions and a fresh button after card reinjection.

Claude device review on the S22 and Tab S11 in Dot and Colour. Turn infinite scroll off, fling to
the chapter end and tap Next once. Check immediate feedback, no menu flash, and the reveal for
both cached and slow chapters in TextView and scrolling WebView. Check reduced motion, a failed
load and retry, and paged and infinite-scroll navigation.

**Review (Codex RV1, 2026-11-08):** Fail. Native Dot advances on the first tap and holds the outgoing frame. In scrolling WebView Colour, one Next chapter tap loads the right chapter but reveals the hidden reader chrome. The frame sheet shows the controls entering during the handoff. `NovelWebViewViewer.kt:302` handles the same tap as a native navigation gesture while the JS button handles it. Evidence: `v77-native-dot-frames.png` and `v77-web-colour-frames.png`. Reverted in `dc4b7d563`.

#### V7.7-F Keep the WebView chapter-end tap in the card (T3) — `done` (RV4, 2026-11-13)

Reapply V7.7 by reverting `dc4b7d563` on a fix branch from the reviewed main. A tap on an interactive
chapter-end control should reach the DOM action once and should preserve reader chrome visibility and scroll
position. Keep ordinary text taps and paged gestures working. Verify on the tablet and S22 with both renderers,
including a fling to the card and a slow or failed chapter load.

**Result (Codex, 2026-11-09):** `tsuzuki/v7.7-f` at `06224334e` (transition restored in `f8111f792`).

- `NovelWebViewViewer.kt` checks the DOM target at single-tap release before WebView handles the event. The native tap zone runs on confirmation only for a non-card hit. Paged drag, fling, and the native TextView tap path remain separate.
- `chapter-end-card.js` marks the card so the DOM keeps its own Next, Mark read and Contents clicks, including disabled Next while a request is pending.
- The revert-of-revert restores `NovelChapterEndTransition.kt`, `TsuzukiChapterEnd.kt`, and the existing `ReaderActivity.kt` / `NovelViewer.kt` handoff. `docs/design/README.md` records the current status.

Verification: JDK 21, two per-file Spotless passes, `:app:assembleDebug :app:testDebugUnitTest` (977 tests, 0 failures, errors or skips), and a Node DOM smoke check covering the three buttons, pending Next, nested targets, ordinary content and card reinjection.

Device review: on Tab S11 and S22, Dot then Colour, fling to the card and tap Next with chrome hidden and shown in TextView and scrolling WebView. Check one action, stable chrome and outgoing scroll position, plus slow/failed loads and retry. Check Mark read, Contents, ordinary text taps and paged swipes.

Review (Codex RV4, 2026-11-13): Pass for the chapter-end tap/transition change. Tablet Dot/Colour Native and scrolling WebView Next navigate once; the recorded hidden-chrome WebView tap keeps chrome hidden, and a normal text tap still shows it. Native missing local chapter displays Retry and recovers after the fixture is restored. Final accepted Preview smoke opens Chapter 131 at 0%. Native Contents works with chrome hidden; visible bottom bars overlap the card (pre-existing unchanged layout), tracked as RV4-01 in RP1, so no claim that covered-button taps passed. Candidate 1,026 tests; accepted 1,011 tests, both Debug/Preview builds and scoped Spotless twice pass. S22 deferred by owner. Evidence `app/build/review-evidence/rv4/`, including transition MP4s/contact sheets and accepted APKs.

#### V7.6 v0.1 polish from the V1 notes (T2) — `done` (2026-11-08, `.worktrees/v7.6`) (Claude 2026-11-08; v0.1 fix, allowed into main during the freeze)
Small, visible polish Claude found in V1 that is still open. Worktree from `main` (`tsuzuki/v7.6`), no Needs.
Boards: the screen's board in `docs/design/boards/INDEX.md` (Settings, Updates, TTS/ListenMini). Dot first, then Colour.
1. **Updates "Library last updated: Never/…" line** (`UpdatesUiItem.kt:68`, `TsuzukiUpdatesTwoPane.kt:260`): in the
   Tsuzuki themes it is italic body text. Use the board's caption style through `MaterialTheme.typography`, upright,
   and uppercase in Dot (`tsuzukiCase()`). Stock themes unchanged (dispatch, no re-indent).
2. **Settings TIME FORMAT row:** the label wraps while its value truncates on the S22 (360–384 dp). Give the label
   priority (value takes the remaining width, ellipsis on the value only), in the Tsuzuki preference row, not upstream.
3. **Listen panel voice label** shows a raw locale like `en-US-language`. Show the voice's display name
   (`Voice.locale.getDisplayName(appLocale)` + a short quality/network suffix only if the engine gives one); fall back to
   the engine's label. Same fix in the tablet LISTEN panel (Y6) and the mini player if they share the label.
4. **"Grayscale" vs British "colour":** the app's English uses British spelling (Colour theme). Change English
   `pref_grayscale` and any Tsuzuki-only English strings using "gray"/"color" in user-visible text to "greyscale"/
   "colour". Only `base/strings.xml` English values. Keys unchanged, other locales untouched; don't edit upstream
   strings that Tsundoku also ships unless they are Tsuzuki-only (list each changed key in Result).
5. **Novel reader settings, render-mode segment** (Japanese, S22): the left option shows `ネイティブ（TextVie…`. Use a
   shorter Japanese label (e.g. `ネイティブ`) or let the segment text wrap to two lines; check the other locales.
Tests: a pure function for the voice label (raw locale → display name; unknown → engine label). Verify per WORKPLAN §0.4
plus `:app:assemblePreview`. Claude checks on the S22 + tablet in Dot and Colour, English and Japanese.

**Result (Codex, 2026-11-08):** `tsuzuki/v7.6` at `2f482f4de` (base `c0a0015a4`).

- `UpdatesUiItem.kt`, `TsuzukiUpdatesTwoPane.kt`, `TsuzukiUpdatesLastUpdated.kt` share an upright, muted caption with Dot casing.
- `ListPreferenceWidget.kt` and `TsuzukiSettingsPane.kt` give the title first claim on width in themed rows that show a selected value. The value uses the remaining width with ellipsis.
- `TtsController.kt`, `TtsVoiceLabel.kt`, `ReaderListenLayout.kt` and `TsuzukiReaderListenPanel.kt` use the engine's voice locale and connection requirement for the docked Listen label. The engine label is the fallback. The phone mini player is controls-only.
- `TtsVoiceLabelTest.kt` covers app-locale display and unknown-locale fallback. `i18n` adds `tsz_tts_voice_offline` and `tsz_tts_voice_online` in all eight locales. `pref_grayscale` is the only existing English key changed in `i18n`.
- `i18n-novel` shortens `novel_render_default` in English and Japanese and `pref_novel_native_renderer` in Japanese. The six other shipped locales use the English `novel_render_default` fallback.
- `GuidesStep.kt` sends onboarding and empty Novel/Manga library links to the Tsuzuki README's Download section. Verified HTTP 200 and the published `#download` anchor.
- `docs/design/README.md` records the implementation status.

JDK 21, two per-file Spotless passes, `:app:assembleDebug`, `:app:testDebugUnitTest` (970 passed, 0 skipped)
and `:app:assemblePreview` passed. The serial build used `--no-parallel --max-workers=2` and a 6 GB Kotlin
daemon heap after the combined build exhausted the compiler heap.

Claude device review on the S22 and Tab S11 in Dot and Colour, English and Japanese. Verify the Updates
caption, Time format row and picker, the active voice label on tablet, the native renderer segment and
the Getting started guide from the empty library. R-A5 awaits the A3.5 commit.

**Review (Codex RV1, 2026-11-08):** Pass. S22 Time format kept its title on one line in Japanese and English, with only the English Dot value ellipsised. The Japanese Native chip fits. The Updates caption is upright on both displays, and tablet Listen shows the localised offline voice name. Earshot measured the 13.71 s local capture at −19.6 LUFS (AudioSet Speech 0.72; 92.55% digital silence). The unrelated GuidesStep URL change was dropped in `a8cfedf0f`. Evidence: `v76-*` in the RV1 evidence folder.

#### V8 Built-but-never-seen screens (Claude) — `ready` after V3–V6
WORKPLAN 4A: widget picker preview, unopened menus (manga reader top bar, extension details, web view, migration
config, download queue Cancel all), mass import batch cards, phone Settings layout. NewUpdate screen moves to V12
(needs a public release). One English pass on the S22 if the owner approved it in V0. Bugs → `V8.n`.

#### V9 Cold start + baseline profile (Claude) — cold start `done` 2026-11-07 (S22 median 352.5 ms over 10, see PERFORMANCE.md); baseline profile re-collect still to do
P2.1: phone cold start, `am start -W`, 10 runs, compiled vs not, before/after numbers in `docs/PERFORMANCE.md`.
P2.4: re-collect the baseline profile on the frozen main (`-PtsuzukiProfileConnected=true`), commit it.

#### V10 Signing continuity (Claude + owner) — `ready` after V0, V4
Pull the phone's installed release APK (`pm path io.github.manojx2005.tsuzuki`) and compare its certificate SHA-256
(`apksigner verify --print-certs`) with an APK signed by the release key; record the phone's installed `versionCode`.
Same cert and version code below the new one: in-place upgrade works. Different cert: **stop**, owner decides
(sign v0.1 with the phone's key, or backup → uninstall → install → restore on the phone).

#### V11 Release candidate (Claude + owner) — RC2 built 2026-11-07, owner testing on their phone
**RC6 (current, 2026-11-08):** RC4 + Dot as the default theme (onboarding in Dot) + infinite scroll on by
default. RC5 (Dot only, run 42431102954) was cancelled before it finished.
**RC4 (superseded, 2026-11-08):** commit `8be1ba942` (RC3 + chapter-end Next chapter fix), Actions run 42429219360,
0.1.0 (24), cert `ffcdc743…f6c7`. Installed on tablet + S22 (release app id): launches, crash buffers empty. Files:
`C:/Users/Manoj/Desktop/Tsuzuki-v0.1.0-rc4/tsuzuki-rc-8be1ba942/`. Tag only after the owner's OK.
**RC3 (superseded, 2026-11-08):** commit `000cb1ce6` (main + V7.3–V7.5 + V8 More insets), Actions run 42423861828,
0.1.0 (24), cert `ffcdc743…f6c7`. Installed on tablet + S22 (release app id): launches, Japanese onboarding renders,
crash buffers empty; reading not yet checked in this build (fresh install stops at the system folder picker). Owner
is testing it on their phone (debug-signed install → backup, uninstall, install, restore). Files:
`C:/Users/Manoj/Desktop/Tsuzuki-v0.1.0-rc3/tsuzuki-rc-000cb1ce6/`. V7.6 polish → 0.1.1. Tag only after the owner's OK.
**RC2 (superseded):** commit `2b9911b76` (adds the app update notification), Actions run 42375424770; same checks as RC1
pass (0.1.0 (24), cert `ffcdc743…`, 0 Firebase classes, `TsuzukiAppUpdateJob` kept by R8). Files + checklist:
`C:/Users/Manoj/Desktop/Tsuzuki-v0.1.0-rc2/tsuzuki-rc-2b9911b76/`. RC1 below is superseded.
**RC1:** commit `0025beca7`, built and signed by GitHub Actions (`release.yml` manual run 42373139062, no publish).
Verified: `io.github.manojx2005.tsuzuki` 0.1.0 (24), cert `CN=Tsuzuki` SHA-256 `ffcdc743…f6c7`, 0 Firebase classes,
baseline profile packaged. APKs + SHA256SUMS + owner checklist in `C:/Users/Manoj/Desktop/Tsuzuki-v0.1.0-rc1/`.
Local signed builds no longer work (the key password left this PC, owner 2026-11-07): build RCs with
`gh workflow run release.yml` and `gh run download`.
Build `assembleRelease -Penable-updater` locally with the release key. Tablet + S22: install the release app id,
smoke test Home, Library, both readers, TTS, downloads, backup + restore, Browse, extensions, settings, widgets.
Owner's phone (as approved in V0): fresh backup copied to the PC first, then upgrade in place over the real data;
check library, progress, shelves, history and prefs survived; crash buffer empty on all three.

#### V12 Tag + publish (Claude + owner) — `done` 2026-11-08
**Published:** https://github.com/Manojx2005/Tsuzuki/releases/tag/v0.1.0 — tag `v0.1.0` → `5a65307c9` (RC6 code +
changelog), run 42432617196. Not draft/pre-release; `/releases/latest` returns v0.1.0; 5 APKs with SHA-256 lines in the
notes; arm64 APK verified: cert `ffcdc743…f6c7`, 0.1.0 (24). Tachiyomi's 2016 local `v0.1.0` tag (`bba727ec8`, never on
origin) was deleted first. Still open: the in-app updater end-to-end check (needs a later tag), lifting the freeze.
Owner OK → `git tag v0.1.0` on frozen main → push **only that tag** (`git push origin v0.1.0`; never `--tags`: the
repo holds Tsundoku's `v0.8.4`–`v0.9.2`, which would look newer to the updater). Watch the workflow, check the
release assets and SHA-256 file. On the tablet, install a build reporting an older version and confirm the in-app
updater finds v0.1.0 and the NewUpdate screen renders (V8's deferred item). Then lift the freeze, merge the waiting
branches, and start v0.2 (anime).

#### Owner permissions for v0.1 (answers recorded 2026-11-07)
- **Repo:** `Manojx2005/Tsuzuki` is public (owner; Claude checked the API returns 200 without auth). Releases go there;
  `GITHUB_REPO` stays.
- **Signing:** owner: "do what is best". V10 signs v0.1 with whichever key signed the phone's installed release, so it
  upgrades in place; if that key isn't the Actions secret, Claude asks the owner to update the secrets before V12.
  Item 2 (secrets exist) is checked in V10.
- **Watch switch:** keep as a teaser (V3 rewritten).
- **Devices:** tablet + S22 free to use (any build type, `am kill`, scratch installs with the synthetic backup, widgets,
  temporary EPUB/mass-import files cleaned up after); one extension install/update/uninstall on tablet/S22; S22 to
  English once and back; owner's phone upgraded in place at V11 after a backup is copied to the PC.
- **Push:** Claude pushes `main` to origin after each verified merge, and pushes only the `v0.1.0` tag at V12 (never
  `--tags`).
- **Firebase:** delete `app/google-services.json` in V4.
- **Publish:** Claude stops after V11 and asks the owner before the V12 tag push.

### Batch R — the long list (written 2026-11-07 by Claude)

Same run rules as "Overnight batch Q — run rules" above (worktree per card from current `main`, Needs merged
first, commit on the card branch, Result + `review`, blocked → next row). Boards: `docs/design/boards/INDEX.md`
(open `png/<page>/<Board>.png` + `dot/` + `html/`) unless the card names another. Priority order from the owner:
stable → smooth/fast → design complete → Mihon parity → new features; anime has its own lane.

**Owner 2026-11-07: v0.1 ships without anime (anime = v0.2); checklist in WORKPLAN Phase 7.** Lane 2 cards R6 and R7 come first.
Anime lane rule until v0.2: no anime entry point may appear in a release build (gate with `BuildConfig.BUILD_TYPE == "debug"`, like the anime debug screen).

**Lane 1 — anime (Codex thread 1, in order):** superseded 2026-11-08 by the table "Anime lane order from 2026-11-08" in the Anime lane A section: A2.4 → A3.0 → A3.1 → A3.2 → R-A2 → A3.3 → A3.4 → R-A3 → A3.5 → A3.6 → R-A1 → R-A4 → A3.7 → A3.8 → A3.9 → A4.1 → A4.2.
**Lane 2 — app (Codex thread 2, in order):** R1 → R2 → … → R16 → R17. Lane 2 never touches anime files, and lane 1
never touches R-card files; both threads: one Gradle build at a time each.
**Lane 3 — Claude:** reviews, device checks and the C-list at the end of this section.

| # | Card | Lane | Needs | Tier |
|---|---|---|---|---|
| 1 | R1 Large-library performance pass | 2 | — | T3 |
| 2 | R2 Process death + rotation audit | 2 | — | T3 |
| 3 | R3 Mid-word wraps + tile labels sweep | 2 | — | T2 |
| 4 | R4 Menus: icons, groups, destructive row | 2 | — | T2 |
| 5 | R5 Tablet Library + Tablet Settings vs boards | 2 | — | T3 |
| 6 | R6 Colour theme parity pass | 2 | R3 | T2 |
| 7 | R7 Japanese layout pass | 2 | R3 | T2 |
| 8 | R8 Downloads screen + queue vs board | 2 | — | T2 |
| 9 | R9 Widgets: Continue reading + Listening vs board | 2 | — | T3 |
| 10 | R10 Tracker accounts + update schedule vs boards | 2 | — | T2 |
| 11 | R11 Reading estimates (time left, speed) | 2 | — | T3 |
| 12 | R12 Source health | 2 | — | T3 |
| 13 | R13 Dictionary lookup (system-provided) | 2 | — | T3 |
| 14 | R14 Merge sources for one book | 2 | — | T4 |
| 15 | R15 Backup: scheduled export + restore polish (Backup board) | 2 | — | T3 |
| 16 | R16 Upcoming: notify on expected release (optional) | 2 | — | T2 |
| 16b | R17 Tablet reading line length (cap ~600 dp, centred) | 2 | — | T3 |
| 17 | R-A1 Anime backups: Aniyomi import + our 8000-range fields | 1 | A2.4 | T4 |
| 18 | R-A2 Player settings + sheets (subtitles, audio, speed, decoder) | 1 | A2.4 | T3 |
| 19 | R-A3 Anime in Updates + "Airing today" strip | 1 | A2.4 | T3 |
| 20 | R-A4 Anime episode downloads (third engine) | 1 | R-A2 | T4 |

#### R1 Large-library performance pass (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
Use Y4's `scripts/generate_synthetic_backup.py` (500 books, 30 shelves, 50 k chapters). Codex: no devices — profile
on the JVM side only: add Macrobenchmark-free unit/perf tests for the hot paths (library grouping/sorting/filtering,
shelf counts, Updates mapping, Home stack queries) with the synthetic data shape, find anything O(n²) or done on
the main thread, fix the top issues (stable keys/contentType already done in N30 — check for regressions),
document numbers before/after in Result. Claude then restores the backup into a **scratch** install and records
`gfxinfo framestats` for Home, Library (each tab), Updates, History, Browse.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r1` at `.worktrees/r1` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (924 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- `TsuzukiHomeViewModel.kt` separates badge emissions from stack loads and skips sorting fallback books when the stack is full. In the JVM flow test, 20 badge changes after first load cause 0 extra stack builds (previous pipeline: 20). With a full nine-book stack that avoids up to 180 chapter-list queries. `TsuzukiHomeLogicTest.kt` guards the load count.
- Upstream `UpdatesViewModel.kt` moves media filtering into the IO mapping flow, keeps the downloaded filter applied, discards stale media-filter results, and maps date headers in one pass. At 50,000 rows, date conversion calls fall from 100,000 to 50,000. `TsuzukiUpdatesTwoPaneTest.kt` checks row order, dates and grouping on that shape.
- `LibraryGrouping.kt` exposes the existing linear category grouping for JVM tests. Upstream `LibraryViewModel.kt` calls it; grouping, sorting and filters already run on IO. `LibraryGroupingTest.kt` verifies ordering of shelves, 500 books across 30 shelves, title filtering and shelf counts. N30 keys and content types remain in place.
- Ran Y4's generator. Fixture: `.worktrees/r1/build/r1-synthetic-500.tachibk`. JVM numbers are operation counts, not device frame timings. Claude: restore into a scratch install, compare Home stack and badge updates, Library shelves, Updates media and downloaded filters, History and Browse using `gfxinfo framestats` on phone/tablet.

#### R2 Process death + rotation audit (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
WORKPLAN P1.1. For every Tsuzuki screen and sheet with user state (Home stack index, Library tab/shelf/filter/
selection, details sheets incl. Q3 save range, reader quick sheet tab, All reader settings tab, Browse media/
section, search queries, Q9 selection, Updates two-pane selection), make sure state survives rotation and
process death (`rememberSaveable` / ScreenModel saved state, no Parcelable leaks). List each screen + what was
missing in Result. Tests for any saver you write. Claude verifies with `adb shell am kill` on both devices.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r2` at `.worktrees/r2` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (923 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- Home stack index and selected tablet book were already saveable. Browse's media/section pager already saved its page. Upstream `BrowseTab.kt` now keeps both extension search drafts.
- Upstream `LibraryViewModel.kt`, `LibraryTab.kt`, and `NovelsTab.kt` retain the shelf index, draft and submitted search, filters sheet, and selected IDs. The root Library tab and persistent filter values already use saved state or preferences.
- Upstream `MangaViewModel.kt` and `MangaScreen.kt` retain chapter selection, chapter filter/tracking/cover sheets, and the scanlator filter sheet. Upstream `MangaToolbar.kt` retains the Save Chapters sheet, whose mode and range already use saveable state.
- Upstream `ReaderViewModel.kt` and `ReaderActivity.kt` retain the settings dialog and wait for the correct renderer before showing it. Quick and All settings tabs were already saveable. `TsuzukiReaderHooks.kt` retains Contents visibility. Reader page/chapter progress already uses the reader's saved state.
- Upstream `GlobalSearchScreen.kt`, `NovelGlobalSearchScreen.kt`, `GlobalSearchViewModel.kt`, `NovelGlobalSearchViewModel.kt`, and `SearchViewModel.kt` restore the draft and source filter and rerun the submitted query.
- Upstream `UpdatesTab.kt` and `UpdatesViewModel.kt` retain selected chapter IDs, media filter and loaded page limit. Upstream `UpdatesScreen.kt` holds the two-pane preview key outside the tablet-only branch and passes it to `TsuzukiUpdatesTwoPane.kt`.
- `TsuzukiSavedScreenState.kt` and `TsuzukiSavedScreenStateTest.kt` limit each saved selection to 8,192 IDs. Larger selections restore empty to avoid a partial bulk operation. Transient confirmations, in-flight work and reader find results are recreated on demand. R1 and R2 both change UpdatesViewModel; preserve both when merging.
- Claude device review: background and kill, reopen, and rotate on the phone and tablet with each named state active. Verify a custom chapter range, unsent draft vs submitted search, Q9 selection, both reader renderers, and a tablet preview selected beyond the first Updates page. JVM verification only.

#### R3 Mid-word wraps + tile labels sweep (T2) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
Found 2026-11-07: details action tiles wrap mid-word with 4 tiles ("TRACKIN / G", "IN / LIBRARY") on 360 dp.
Grep every Tsuzuki tile/pill/button label for `maxLines = 2` without word-safe handling; for equal-width tiles use
one line + ellipsis or a shorter label (8 locales), never break inside a word (`LineBreak.Simple` /
`Hyphens.None`, or `softWrap` off for single words). Add the details 4-tile case to `TsuzukiNarrowPreviews`.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r3` at `.worktrees/r3` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (921 tests, 0 failures). R6 and R7 need the card-branch commit. Automatic approval rejected card commits under AGENTS.md.
- Upstream `MangaInfoHeader.kt` makes themed details action tiles single-line with ellipses and gives the labels more room by reducing the button's inner horizontal padding. Stock treatment and the full accessible labels are preserved.
- `TsuzukiPill.kt`, `TsuzukiSegments.kt`, and `TsuzukiNav.kt` apply single-line ellipses to shared control labels. `TsuzukiBrowse.kt`, `TsuzukiPageColours.kt`, `TsuzukiMoreScreen.kt`, `TsuzukiAboutScreen.kt`, and `TsuzukiBackupTiles.kt` apply the same to their action labels. Source names, book titles, and other body text keep their multiline treatment.
- `TsuzukiNarrowPreviews.kt` adds the real four-action details row, using the existing translated labels. The preview compiled. Claude: inspect at 360 dp in Dot and Colour, EN and JA, including 130% and 200% font sizes. On the phone open a library book with an expected update and a WebView action to check all four buttons and TalkBack labels.

#### R4 Menus: icons, groups, destructive row (T2) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
Board `17-drafts` Menus / MenuPick (status table: "Not yet: item icons, groups, accent destructive row, screens
using Material's menu directly"). Extend the shared Tsuzuki dropdown (`components/DropdownMenu.kt` hook +
`theme/tsuzuki/TsuzukiMenu.kt`) with optional leading icons, group dividers/labels and a destructive (accent) row;
then switch the remaining Material `DropdownMenu` call sites in Tsuzuki themes to it (list them).
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r4` at `.worktrees/r4` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (921 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- `TsuzukiMenu.kt` supports icon rows, optional group captions and dividers, and accent destructive text/icons. Upstream `AppBar.kt` uses the shared row and groups overflow actions. Upstream `DropdownMenu.kt` styles anchored popups while preserving their stock position and sizing, and accepts icons for nested rows.
- Upstream `LibraryToolbar.kt` separates library operations and imports, and keeps clear-category last. Upstream `BrowseSourceToolbar.kt` labels the display-mode picker and styles the radio labels. Upstream `DownloadQueueScreen.kt` labels the sort picker and adds sort icons to nested rows.
- The remaining direct Material dropdown containers now use the shared anchored popup in upstream `ImportEpubDialog.kt`, `ImportEpubScreen.kt`, `MassImportDialog.kt`, `SettingsAdvancedScreen.kt`, `SettingsNovelDownloadScreen.kt`, `SettingsTrackingScreen.kt`, `DuplicateDetectionScreen.kt`, `MigrationListScreenContent.kt`, and `MigrateMangaScreen.kt`. Their row actions and custom content remain in their owners.
- Claude device review: compare Library overflow groups, Catalogue display mode, Downloads sorting and cancel row, then an import category and tracker list picker in Dot and Colour. Check edge anchoring and nested menu focus.

#### R5 Tablet Library + Tablet Settings vs boards (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
Boards `11-tablet/TabletLibrary`, `TabletSettings` (+ dot). Compare with the current tablet layouts (rail + Library
grid; Settings two-pane) and fix only differences (list them). Real data only.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r5` at `.worktrees/r5` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (921 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- `TsuzukiHomeScreen.kt` shows six covers on the TabletLibrary reading shelf when its content has 670 dp, with five on narrower landscape panes. The named board is the Home reading grid with a details pane. The separate Library tab still uses the user's configured grid settings. The rail and portrait Home layout already matched the intended structure.
- `TsuzukiSettingsMain.kt` caps the section list at the board's 360 dp, gives rows the 52 dp minimum and fills the selected row's tile within the accent outline. The existing shared rail and appearance pane remain the entry points.
- Kept the available real-data tiles and settings. Device review: verify Dot and Colour on tablet portrait and landscape. In landscape confirm the six-cover row and details selection, then check Settings list width, selected outline/fill and preference navigation. Check the separate Library grid and rail at a large font scale.

#### R6 Colour theme parity pass (T2) — `done` (merged via `review/night6`, 2026-11-07) (R3 is in main)
Every Tsuzuki screen in **Colour** at 360 dp and tablet: sentence case, Instrument Serif headings, book-palette
accents, no Dot-only artefacts (uppercase, grey `#8A8A8A`, red). Use the Q8 narrow previews in Colour; fix
differences; list per screen.
**Result (Codex, 2026-11-07):** `c004ea917` on `tsuzuki/r6` (`.worktrees/r6`).
- Upstream `LnrTypography.kt` selects the italic Instrument Serif face in the paired Android typeface. Headings previously rendered upright even though Compose requested italic. The Noto Serif JP fallback stays in the family.
- `TsuzukiNarrowPreviews.kt` sets the board's night palette and mirrors the app's content locals. Added tablet portrait and landscape variants for Home and the Library, Settings and Updates samples. `docs/design/README.md` records the coverage.
- The headless 360 dp matrix rendered Home, Library, Browse, Details, More, novel and manga reader chrome, reader settings, Settings, Updates and History samples in both themes. Colour showed italic display text, sentence-case controls and gold default accents. Small uppercase section labels and cover media badges match their Colour boards. Tablet Home showed italic headings in both orientations. Evidence: `.worktrees/r6/app/build/r6-preview-after/`.
- Per-file Spotless passed twice. `assembleDebug` and `testDebugUnitTest` passed with 933 tests and zero failures.
- Claude device check: compare live cover accents and full Library, Settings and Updates panes on the tablet. The night preview uses the board palette. The initial day-mode sample showed low contrast where light Material ink met dark Tsuzuki tokens, so also check System, Light and Dark on the device. Full runtime coverage for screens outside the preview matrix is pending.

#### R7 Japanese layout pass (T2) — `done` (merged via `review/night6`, 2026-11-07) (R3 is in main)
Devices are Japanese. Status table: translated layouts never visually checked. Run the narrow previews with the
`ja` locale; fix truncation, mixed fonts (Japanese must use the partner font via `MaterialTheme.typography`) and
any hard-coded English in Tsuzuki screens (move to strings, 8 locales).
**Result (Codex, 2026-11-07):** `e1502c742` on `tsuzuki/r7` (`.worktrees/r7`).
- `TsuzukiNav.kt` wraps phone labels inside the existing five cells, up to three lines at large font scales. The selected pill grows with them. `TsuzukiMoreScreen.kt` allows two-line toggle labels.
- `TsuzukiHomeScreen.kt` formats the date using the app locale and localizes the new-chapter count. `TsuzukiHistoryItem.kt` uses the existing chapter-number translation and allows its timestamp line to wrap.
- `i18n/` adds the count in eight locales. `i18n-novel/` supplies translations for the existing Manga, Novels, Translate and three reading-status labels. `docs/design/README.md` records the Japanese preview coverage.
- Reviewed 360 dp Japanese previews at 100%, 130% and 200% in Dot and Colour with the R6 preview harness. Evidence: `.worktrees/r7/app/build/r7-preview/`. Long book titles and Details action tiles retain their designed ellipses. English shelf names in the Library preview are fixtures. Other upstream novel strings still use their existing fallback.
- Per-file Spotless passed twice and XML checks covered seven keys in eight locales. `assembleDebug` and `testDebugUnitTest` passed with 933 tests and zero failures.
- Claude device check: select Japanese on the S22 and tablet. At standard and large text scales, check the phone navigation, More toggles, Browse media choices, Home date/new chip, History timestamp and reader controls. Check the tablet rail and remaining runtime settings.

#### R8 Downloads screen + queue vs board (T2) — `done` (merged via `review/post-v01`, 2026-11-08)
**Claude review 2026-11-07 — rework:** the queue UI is good, but upstream `Downloader.kt` (+98/−48) rewrites the scheduler for every download: `collectLatest` → `collect` with `joinAll()` inside the collector (a cancelled job stuck in blocking IO now stalls all scheduling), the per-run job map became a class field, and `distinctUntilChanged` was dropped. Keep Mihon's `collectLatest` loop. Put source pause and targeted retry in `tsuzuki/download/` (e.g. filter paused sources inside `activeMangaDownloads`, and have retry reset statuses + bump a flow the existing `combine` already watches). Target: `Downloader.kt` under ~20 changed lines; drop the `joinAll` wait unless a test shows a real write race.
Board `Downloads` (+ `DownloadSheet`). Compare the Tsuzuki download queue (N10 Compose rows) with the board: group
headers, per-group pause/resume, progress, error rows with retry; fix differences.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r8` at `.worktrees/r8` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (923 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- `TsuzukiMangaDownloadQueue.kt` gives each manga source header a pause/resume button, shows paused status and provides a retry on failed rows beside the live progress and error reason. Upstream `DownloadQueueScreen.kt` wires the actions and adds a Retry button to the novel error section. Upstream `DownloadQueueViewModel.kt` snapshots novel statuses so a failure or retry updates the card even without a membership change.
- `DownloadQueueScheduling.kt` and `DownloadQueueSchedulingTest.kt` select one runnable manga download per available source and verify paused-source ordering and targeted worker startup. Upstream `Downloader.kt` re-evaluates when a group changes or a failure is retried, waits for old attempts to finish writing before restarting, and leaves paused groups out of scheduling. Upstream `DownloadManager.kt` exposes the controls. Upstream `DownloadJob.kt` distinguishes a targeted restart from global Start, so the former leaves other failed items alone.
- Manga groups retain source grouping, novel groups retain book grouping, and source pauses are session-scoped like existing novel pauses. The board's video rows, sample storage and retry countdown were not added. R4 and R8 both edit `DownloadQueueScreen.kt`; preserve both when merging.
- Claude device review: on Dot and Colour queue from two manga sources and two novels, pause one source and one novel while in flight, confirm others advance, resume, then trigger two errors and retry one. Check retry while the source or whole queue is paused, reordering, and removing items during progress.

**Rework result (Codex, 2026-11-07):** `32dff0c4d` on top of `917f06958` (`.worktrees/r8`).
- Upstream `Downloader.kt` restores `collectLatest`, distinct active selections and the per-run job map. Cancelled sources release the scheduler without a group join. Queue completion signals let a waiting source resume once the previous attempt exits.
- `DownloadQueueScheduling.kt` owns the paused-source and retry state, filters source queues and wakes the existing scheduler when the queue list itself stays the same. `DownloadQueueSchedulingTest.kt` adds checks for targeted retry while paused and resuming a source as its cancelled attempt exits.
- The cumulative `Downloader.kt` change is +36/−20 against the base, above the approximate 20-line target. The extra hooks keep retry and rapid resume from starting a second attempt while the same download is still finishing.
- Per-file Spotless passed twice. `assembleDebug` and `testDebugUnitTest` passed with 925 tests and zero failures. Repeat the device checklist above, including a rapid pause/resume while another source continues downloading.

#### R9 Widgets vs board (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
Board `Widgets`. Continue-reading widget and N28 Listening widget: match sizes/styles, Dot + Colour (follow app
theme), real data only, tap targets open the right screen. Claude places them on the tablet and checks.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r9` at `.worktrees/r9` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (923 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- `TsuzukiReadingWidget.kt` and `tsz_reading_widget_info.xml` give Continue a 4×2 target, keep live stats when resized taller, and render chapter-count progress with Dot dots or a Colour bar. Continue uses the existing resume query, opens Details if the book has no resume target, and the taller New tile opens Updates. `TsuzukiReadingWidgetTest.kt` covers missing and inconsistent progress data.
- `TsuzukiListeningWidget.kt` fits the 4×1 target and opens the active reader chapter from live TTS IDs. Its transport still sends the existing service action.
- `TsuzukiWidgetTheme.kt` reuses theme tokens and watches app theme, accent and cover-colour changes. Upstream `App.kt` starts that watcher in the main process. Widget typography uses Glance system faces. Time estimates, anime content, and the launcher preview artwork are outside this diff.
- Claude device review: place both widgets on the tablet, resize Reading short/tall and Listening to one row, toggle Dot/Colour and cover colour, test an in-progress chapter, a completed book, the taller New tile, and Listening chapter/transport with TTS paused, playing and stopped. Verify the two widgets' distinct reader destinations when placed together.

#### R10 Tracker accounts + update schedule vs boards (T2) — `done` (merged via `review/post-v01`, 2026-11-08; Claude restored the upstream token dialog)
**Claude review 2026-11-07 — rework:** `SettingsLibraryScreen.kt` (+72/−41) and `SettingsTrackingScreen.kt` (+45/−24) wrap upstream preference rows in `if (!themed)` and re-indent them, which AGENTS.md rules out. Instead, put one dispatch at the top of each group builder (`if (isTsuzukiStyle()) return tsuzukiLibraryUpdateGroup(...)`) that builds the whole themed group in `presentation/tsuzuki/settings/`, and restore the upstream list byte-for-byte. Also check that the themed group keeps the smart-update manga restrictions (the current themed branch replaces them with Download new chapters).
Boards `TrackerAccounts`, `UpdateSchedule`. Settings › Tracking and Library update schedule screens: compare and
fix; never show tokens; no new network calls.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r10` at `.worktrees/r10` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (921 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- Novel tracker accounts come first and show locally linked library entries. Token entry is masked and opens empty. No additional network calls. The Update Schedule uses the existing interval, included shelves, download option, device and library restrictions, and scheduler. Detailed source throttling stays on its existing screen.
- Claude device review: compare Dot and Colour, change the schedule and restrictions, and check account state and link counts after login and logout. Coordinate the `SettingsTrackingScreen.kt` overlap with R4.

**Rework result (Codex, 2026-11-07):** `4b41d9712` on top of `7154179d3` (`.worktrees/r10`).
- Upstream `SettingsLibraryScreen.kt` and `SettingsTrackingScreen.kt` dispatch the themed groups at their entry points. Both stock preference bodies match the pre-R10 version byte for byte. The earlier empty, masked novel token entry stays in place.
- `TsuzukiUpdateSchedule.kt` reuses the interval and category dialog and exposes all four existing smart-update restrictions: skip unread, unstarted, completed and outside the release period. Download new chapters is a separate switch.
- `TsuzukiTrackingPreferences.kt` moves novel-only accounts to the top using tracker IDs and attaches local linked counts. `docs/design/README.md` records both groups.
- Per-file Spotless passed twice. `assembleDebug` and `testDebugUnitTest` passed with 921 tests and zero failures. Check all four smart-update flags independently of the download switch during the device pass above.

#### R11 Reading estimates (T3) — `done` (merged via `review/post-v01`, 2026-11-08)
Phase 5 "reading estimates (words, speed, time left)". Details Resume tile / reader chrome show **time left in
chapter** and **in book** only when backed by data: words from downloaded/cached chapter text (count once, cache in
a Tsuzuki table-free store e.g. prefs/JSON keyed by chapter id — no schema change), reading speed from
`ReadingDays` minutes ÷ words read (rolling median, needs ≥ 3 sessions). Hide when unknown. Pure helpers + tests.
Board: NovelDetails / TabletNovel estimate rows (status table says "estimates left out").
**Result:** `tsuzuki/r11` at `d6c8f5fae`, fast-forwarded to current main before applying the saved work. The Resume tile
and reader show chapter/book time left when local word counts and three valid reading samples are available.

- `NovelReadingEstimates.kt` caches counts and takes the median of up to nine usable samples.
  `TsuzukiReadingEstimates.kt` refreshes from real chapter, download and cache changes, subtracts each chapter's
  saved progress, and suppresses estimates in incognito mode.
- Upstream `ReaderActivity.kt`, `ReaderViewModel.kt`, `NovelViewer.kt`, `NovelWebViewViewer.kt`, and
  `TtsController.kt` supply reading windows, word counts and TTS/seek boundaries. The timer can start before
  initial novel loading finishes. Playback invalidates mixed sessions.
- Upstream `NovelReaderAppBars.kt` and `presentation/manga/MangaScreen.kt` use the estimate slots in
  `TsuzukiReaderTitle.kt` and `TsuzukiResumeTile.kt`. `PreferenceModule.kt` registers the local store.
- Eight locales preserve their placeholders. Per-file Spotless and the debug build passed. All 959 unit tests
  passed without failures or skips, including five estimate tests. The design status is updated. Pairwise
  merge checks with V7.3, V7.4 and V7.5 were clean.
- Claude: use a locally available novel on S22 and tablet in Dot and Colour. Check both renderers, three
  qualifying visual sessions including a cold entry, a mix of visual and TTS, a seek, incognito, and a
  missing chapter becoming available after download. Verify the Resume tile and reader use real progress.

#### R12 Source health (T3) — `blocked` (2026-11-08, `.worktrees/r12`)
Board `SourceHealth`. Per installed source: last successful request time, average response time, last error
(HTTP code / Cloudflare / timeout), measured passively from real Browse/Updates traffic via an OkHttp interceptor
hook in `tsuzuki/` (no extra pings; optional manual "Check now" that does one Popular request). Store in a small
Tsuzuki store (no DB migration). Show on the Browse source rows (status dot) and a Source health screen.
**Result (Codex, 2026-11-08):** `tsuzuki/r12` at `b77043aee` (base `c0a0015a4`).

- `SourceHealthRequest.kt`, `HttpSource.kt`, `JsSource.kt`, `PluginRuntime.kt` and `JSLibraryProvider.kt` carry the source ID in an in-process request tag. The standard HTTP catalogue, details and chapter-list paths are covered. The LNReader runtime tags its plugin fetches, including reader fetches. A native extension that bypasses these request paths can remain unmeasured.
- `NetworkHelper.kt`, `CloudflareInterceptor.kt`, `AppModule.kt` and `SourceHealthInterceptor.kt` measure tagged network responses after pacing, classify failures, skip cache-only responses and ignore cancelled calls. No background probes or manual checks are scheduled.
- `SourceHealthStore.kt` persists seven daily buckets per source in app-state preferences. The latest failed observation marks a source Down, a successful-response average of at least 1.5 seconds marks it Slow, and dates without observations stay Unmeasured. Last success and last error survive the rolling history.
- `TsuzukiSourceHealthScreen.kt`, `TsuzukiSourceRow.kt`, `TsuzukiTokens.kt` and the Browse tab hooks show health dots, seven-day history and a real outage card with migration and WebView actions when available.
- The eight locale files add 16 health strings with matching format arguments. `docs/design/README.md` records the implementation status.

JDK 21, two targeted Spotless passes, `:app:assembleDebug` and `:app:testDebugUnitTest` passed
(972 passed, 0 failed). `SourceHealthStoreTest.kt` checks source isolation, persistence, recovery
and expiry, request tags, and failure classification.

Claude device review on the S22 and Tab S11 in Dot and Colour. Open a real LNReader and Keiyoushi
catalogue, then check the source row and Browse overflow screen. Verify the same source updates
from library traffic, a never-used source stays unmeasured, and recovery keeps its last error while
updating its status. Check outage actions only when a real failure is present.

**Review (Codex RV1, 2026-11-08):** Fail. Real Royal Road requests set a healthy dot, last success and latency. An Archive of Our Own request recorded HTTP 418 and showed the WebView outage action. On the S22, Asura Scans loaded both Popular and Latest but remained unmeasured. `HttpSource.kt:127` tags the legacy request builders. Modern Keiyoushi suspend methods can issue OkHttp requests directly, outside those builders. Evidence: `r12-health-observed-dot-tablet.png`, `r12-health-colour-tablet.png`, `r12-manga-latest-dot-s22.png` and `r12-health-manga-dot-s22.png`. Reverted in `1d6554c94`.

#### R12-F Measure current Keiyoushi sources (T3) — `blocked` (RV4 deferred, 2026-11-13)

Reapply R12 by reverting `1d6554c94` on a fix branch from the reviewed main. Pass source identity through
native suspend catalogue and update paths that bypass HttpSource request builders. Keep cache-only requests,
cancellations and non-source calls out of the measurements. No probes or per-host source guesses. Verify one
real Keiyoushi source and one LNReader plugin on device, including unmeasured and observed states.

**Result (Codex, 2026-11-09):** `tsuzuki/r12-f` at `8246bccd7` (R12 restored in `57794a62f`).

- `SourceHealthRequest.kt` scopes the native source ID to the caller coroutine. `NetworkHelper.kt` attaches it to the OkHttp call at start, preserving explicit request tags and leaving HTTP headers alone. `SourceHealthInterceptor.kt` retains the same store and excludes cache-only, synthetic, cancelled and untagged calls.
- `source/tsuzuki/SourceHealth.kt` scopes native HTTP sources. `SourcePagingSource.kt`, `TsuzukiTabletBrowse.kt`, `SearchViewModel.kt`, `SmartSourceSearchEngine.kt` and `DeeplinkResolver.kt` use it for catalogue and search calls. `UpdateMangaFromRemote.kt`, `MassImport.kt` and `MassImportJob.kt` use it for native update calls. LNReader keeps its explicit request tags.
- Calls using the shared client or clients derived from it keep the tag factory. Calls made through an independent client or one that replaces the factory have no ambient measurement. No polling or source-host inference was added.
- `SourceHealthInterceptorTest.kt` covers concurrent sources on one host across dispatchers, explicit-tag precedence and cache/cancellation filtering. `docs/design/README.md` records the new coverage.

Verification: JDK 21, two scoped Spotless passes per fix file and `:app:assembleDebug :app:testDebugUnitTest` passed (984 tests, 0 failures, errors or skips).

Device review: on Tab S11 and S22, open Popular, Latest and Search for a live modern Keiyoushi source such as Asura Scans, then refresh one of its library entries. Check its observed status, latency and last success. Repeat with a live LNReader plugin in Dot and Colour, and verify an unused source remains unmeasured.

Review (Codex RV4, 2026-11-13): Deferred: the required live native Keiyoushi check cannot run until the owner explicitly approves trusting the untrusted extension. Automatic approval review rejected that trust action for arbitrary extension code/credential risk; it was not bypassed. Candidate unit tests passed, but this does not prove the live native path. Also fix `SourceHealthStore.kt` decode fallback before acceptance: malformed saved JSON is swallowed without the warning log required by the checklist. Excluded by `89e1ac9ac`; later review must revert that exclusion before testing. No code-fix card added for an authorization gate.

#### R13 Dictionary lookup — offline packs (T4) — `done` (2026-11-08, `.worktrees/r13`)
Owner 2026-11-07: **offline dictionary**. Board `Dictionary`. Long-press a word in the novel reader (TextView +
WebView) → Tsuzuki sheet: headword, reading (JA), part of speech, senses, "Add to notebook" (N27 quote), and the
device's own handlers (`ACTION_PROCESS_TEXT`, web search) as a fallback row.
- **Data as optional downloadable packs, not in the APK** (same pattern as the neural-TTS plan): Settings › Reader ›
  Dictionaries lists packs with size, licence and Download/Delete; stored in app files.
- Packs: **English = Princeton WordNet 3.x** (WordNet licence, permissive, attribution) and **Japanese = JMdict**
  (EDRDG licence, CC BY-SA 4.0, attribution; data file kept separate from app code). Convert each to a compact
  SQLite (FTS/prefix index) with a script in `scripts/` (no network in tests; Codex writes the converter + a tiny
  fixture; Claude/owner run the real conversion and host the pack on our own GitHub release — Codex never
  downloads). Licence texts in `docs/licenses/` and About.
- Lookup: lemmatize English (simple suffix rules + WordNet exception lists) and Japanese (deinflection rules
  table, Yomichan-style, own implementation — no GPL code). Pure helpers + tests (edges of selection, punctuation,
  inflected forms).
- If no pack is installed, the sheet shows the system/web fallback and a "Get offline dictionary" link.
Result: `e9e315cfb`. Two per-file Spotless passes and offline JDK 21
`:app:assembleDebug :app:testDebugUnitTest` passed (971 tests, including 3 new). Two Python converter
fixture tests passed.

- `DictionaryTerms.kt` normalizes selections and generates independent English and Japanese lookup forms.
  `DictionaryTermsTest.kt` covers punctuation, width, phrases and common inflections.
- `DictionaryPacks.kt` uses indexed, read-only SQLite for lookup and WordNet exception forms. It validates size,
  SHA-256, schema and version before replacing a pack. `AppModule.kt` registers the shared pack service.
- `ReaderDictionary.kt` shows definitions, readings, parts of speech, device handlers and a web-search action.
  `NovelViewer.kt`, `NovelWebViewViewer.kt` and `ReaderActivity.kt` connect selection actions and the notebook.
- `DictionaryPacksScreen.kt` and `SettingsNovelReaderScreen.kt` manage optional downloads, updates and deletion.
  Eight locales add labels. `TsuzukiAboutScreen.kt` and the two `docs/licenses/` notices record data attribution.
- `build_dictionary_pack.py`, `jmdict-tiny.xml` and `test_build_dictionary_pack.py` implement and exercise the
  converter with synthetic data. `docs/DICTIONARY_PACKS.md` gives release filenames and publishing steps.
  The full source licence supplied to the converter is embedded in its pack and readable from Dictionaries.
- `docs/design/README.md` records the implemented board scope. The board's pronunciation, word frequency and
  flashcards are outside this card. Selection opens the sheet through the Dictionary menu action.

Claude device review: generate a fixture pack or publish owner-built packs to the `dictionaries-v1` release.
Check Download, Update, Delete, offline lookup, notebook and the fallbacks on S22 in both renderers and themes.
The real packs have not been built or published in this worktree.

**Review (Codex RV2, 2026-11-10)**

Hold for RV2-04 and RV2-05. The no-pack sheet passed in both tablet renderers. A synthetic SQLite fixture passed inspection while failing lookup.

**Review (Codex RV2, 2026-11-13):** Pass for the scope available without published packs. `bb82bd9f1` validates the actual lookup schema and English exceptions before replacement and resolves 高くて to 高い. Real SQLite tests cover missing tables, missing exception table, missing ordinal, valid schema, and a malformed update with matching checksum/size that preserves the old file and cleans staging. Tablet Dictionaries shows both packs unpublished; no pack downloaded. Earlier no-pack selection-sheet checks in both tablet renderers remain valid; no pack install/update/delete was claimed on a device. S22 deferred by owner.

#### R13-F Reject unusable dictionary packs before replacement (T4) — `done` (needs `tsuzuki/r13`; app lane)

Validate the lookup tables and required columns for each language before replacing an installed pack. Keep a working pack after a rejected update. Cover the malformed schema with a local fixture and add the missing Japanese adjective connective form. Verify the no-pack fallback in both renderers and fixture-backed lookup. Keep real packs unpublished during this check.

#### R14 Merge sources for one book (T4) — `blocked` (RV4 deferred, 2026-11-13)
Board `MergeSources`. Start the Result with a short design note, then build it: link 2+ library entries of the same book from different sources into one "merged" view (chapter
list unified by chapter number, preferred source per chapter, read state synced across). Must not change the DB
schema in this card: store links in a Tsuzuki store; reading progress sync via existing interactors. Mihon has no
equivalent — keep it Tsuzuki-only. If it can't be done without schema changes, set `blocked` with the proposed
schema for Claude.
**Result (Codex, 2026-11-09):** `tsuzuki/r14` at `0ed0a6f18`. Design: link saved novel editions by source ID and source URL in local preferences. Project a single list from the existing chapters, using source priority for each recognised number and keeping unnumbered chapters separate.

- `MergeNovelSourcesScreen.kt` renders the Dot and Colour board, provides a saved-edition picker, drag and accessible reorder actions, removal, real coverage, and a combined list that opens each selected chapter through the normal reader.
- `MergedNovelSources.kt` validates distinct sources and rejects stale edits. It selects the first unlocked copy by priority, mirrors initial read flags on linking, and writes corresponding read/unread updates in one chapter transaction. New matching copies inherit read flags during refresh. Read progress offsets stay per edition, with explicit mark-unread resetting them on matching copies.
- `MangaScreen.kt` and `MangaToolbar.kt` add the novel-details overflow action. `DomainModule.kt`, `SetReadStatus.kt`, `SyncChaptersWithSource.kt` and `ReaderViewModel.kt` provide the data and read-state hooks.
- `MergedNovelSourcesTest.kt` covers priority, gaps, decimals, unnumbered rows, shared read state and late-arriving chapters. Seven new strings are present in all eight locales. `docs/design/README.md` records the implementation.

Links are local app state. The combined list routes to the preferred edition per row. Next/Previous inside the reader follow the open edition.

Verification: JDK 21, two clean per-file Spotless passes, `:app:assembleDebug :app:testDebugUnitTest` (981 tests, 0 failures, errors or skips), eight-locale placeholder check.

Device review: back up first. On Tab S11 and S22 in Dot and Colour, link two disposable saved editions with overlapping, missing and unnumbered chapters. Reorder and remove an edition, reopen the screen and check persistence, open a fallback row in the reader, mark a matching chapter read/unread in details, finish one in the reader and refresh for a late-arriving copy. Compare the read flags in both editions.

Review (Codex RV4, 2026-11-13): Deferred: line-by-line storage/read-flag review and four pure tests passed; tablet single-edition and empty picker states checked. A real second novel edition from a distinct trusted source was unavailable, so fallback reader opening, linked read/unread, reorder/removal and persistence are not device-proven. Excluded by `451168815`; revert that exclusion in the later review. No claim of a full data-safety pass.

#### R15 Backup: scheduled export + restore polish (T3) — `done` (2026-11-08, `.worktrees/r15`)
Board `Backup`. Data and storage › Backup: last backup time + size, scheduled frequency (existing pref), keep-N
rotation, restore preview (what's inside before restoring: counts of books/shelves/history from the file header),
clear errors. No cloud sync in this card (owner decision needed for Drive/WebDAV).
Result: `5e18bf6b4`. Two per-file Spotless passes and `:app:assembleDebug :app:testDebugUnitTest` passed
with JDK 21 offline (977 tests, including 9 new). Dot and Colour Backup boards checked offline.

- `SettingsDataScreen.kt` keeps the existing frequency preference and adds retention choices.
- `TsuzukiBackupTiles.kt` shows the last successful export time and compressed size. Older records show the
  saved automatic backup timestamp when it exists.
- `RestoreBackupScreen.kt` shows validation progress, readable errors and the contents tile before confirmation.
- `TsuzukiRestorePreview.kt` displays file-wide book, shelf and history counts independently of restore options.
- `BackupFileValidator.kt` computes those counts during the existing streaming decode.
- `BackupProtoReader.kt` reports an unavailable stream and rejects invalid tags or lengths.
- `BackupCreateJob.kt` surfaces export errors, including scheduled write failures.
- `BackupCreator.kt` measures the compressed export and validates it before publication and rotation.
- `AutomaticBackupFiles.kt` stages scheduled exports, supports providers without rename, and prunes after success.
- `TsuzukiBackupName.kt` sorts both current and legacy automatic names chronologically.
- `TsuzukiBackupPreferences.kt` stores export metadata and the retention count (2, 4, 8 or 12, default 4).
- `BackupRestorePreviewTest.kt` covers real fixture counts, truncation and an unavailable provider stream.
- `BackupRotationTest.kt` covers retention, naming, size, and successful or failed publication without rename.
- Eight shared locale files add labels and errors. `docs/design/README.md` records the implemented board scope.

Claude device review: on the tablet and S22, use a disposable backup directory to verify schedule and retention,
an older and a new file, valid and corrupt restore previews, and the last backup tile in Dot and Colour. Check that
a failed scheduled write keeps existing copies. Drive and WebDAV are outside this card.

**Review (Codex RV2, 2026-11-10)**

Hold for RV2-02, RV2-06 and RV2-09. The wrapped labels passed on S22 in both themes. The status card still overlaps after a successful export, and the unrelated gzip fixture reached an enabled Restore action.

**Review (Codex RV2, 2026-11-13):** Pass on the tablet. `005713576`: status and actions have one root, full Japanese labels wrap in both themes, unknown-only gzip is rejected with Restore disabled, and settings-only backup remains valid. Valid fixture counts and restore passed; actual manual export showed time and 784-byte size above the actions in Dot and Colour. Rotation tests preserve short manual/ambiguous legacy names and cover failed writes. Published backup survives status/rotation failure or cancellation. Evidence: `backup-after-export-{dot,colour}-20261113`, `unrelated-parse-rejected-20261113`, `settings-only-restore-preview-20261113`. S22 deferred by owner.

#### R15-F Separate backup actions, validate restores and protect manual exports (T3) — `done` (needs `tsuzuki/r15`; app lane)

Use the Backup board and the RV2 screenshots. Keep the wrapped labels and place the Last backup card above the action row. Reject the unrelated gzip fixture while accepting valid exports, including settings-only backups. Rotate files known to be scheduler-owned and preserve manual exports with indistinguishable legacy names. Verify both themes on the S22 and tablet, and exercise retention and failed writes with synthetic files in a disposable directory.

#### R16 Upcoming: notify on expected release (T2) — `done` (2026-11-08, `.worktrees/r16`)
Optional per-book toggle on Upcoming: a notification on the expected date from the existing interval estimate;
off by default; uses the existing notification channel; no new background polling.
**Result (Codex, 2026-11-08):** `tsuzuki/r16` at `6954716eb` (base `c0a0015a4`).

- `TsuzukiUpcomingScreen.kt` adds a trailing per-book bell with an accessible checked state, separate from the book navigation target. It requests notification permission only on opt-in and explains how to enable chapter notifications when Android blocks them.
- `UpcomingReleaseReminders.kt` stores opt-ins locally and follows changes from the existing Upcoming query. It enqueues one-time work per forecast and cancels superseded work it is tracking. Each worker rechecks the opt-in, eligible book, current estimate, local date and delivery marker before using the existing New chapters channel. Tapping the notification opens the book.
- `App.kt` starts the observer in the main process, and `GetUpcomingManga.kt` shares its eligible-status rule with the worker’s single-book lookup.
- Three resource keys ship in all eight locales. `docs/design/README.md` records the implementation status.

JDK 21, two per-file Spotless passes, `:app:assembleDebug` and `:app:testDebugUnitTest` passed
(971 passed, 0 failed). `UpcomingReleaseRemindersTest.kt` covers default-off persistence, changed
and stale estimates, Tokyo date boundaries and same-day deduplication.

Claude device review on the S22 and Tab S11 in Dot and Colour. Verify opt-in, opt-out, permission
denial and tapping through to the book with a real scheduled estimate. Test an estimate change
while the app process is running, then verify a pending reminder survives process exit. WorkManager
delivery is inexact. A reminder that runs after the expected date expires.

**Review (Codex RV1, 2026-11-08):** Pass. A real 11 November forecast on the S22 showed the bell off by default. Opt-in added a one-time JobScheduler entry approximately 2 days and 1 hour out. Opt-out removed it and restored the bell. The same forecast row rendered in Colour. The fresh tablet library showed the empty calendar state. Actual delivery, notification deep link and permission denial were not exercised. Evidence: `r16-*` in the RV1 evidence folder.

#### R17 Tablet reading line length (T3) — `done` (2026-11-08, `.worktrees/r17`)
Owner 2026-11-07: **cap the novel text column at ~600 dp, centred**, on wide windows (board `11-tablet/TabletReader`:
~512 dp column with ~64 dp margins). Phones unchanged; the user's own side margins remain the minimum. TextView:
measure the real content width (not `screenWidthDp` — the N25 side panel can be docked) and centre the chunk views
(e.g. `maxWidth` + centred gravity in the chapter block container) without changing Tsundoku's padding logic.
WebView: CSS `max-width` on the content wrapper with auto side margins, keeping the user margins as padding; check
paged mode (`paged-reader.js` column math) and the chapter-end card. Hook through `tsuzuki/reader/`; tests for the
width math. Claude checks tablet portrait/landscape, docked panel, both renderers, paged + scroll.
Result: `4642ed752` on `tsuzuki/r17`.

- `tsuzuki/reader/NovelReadingColumn.kt` and `NovelViewer.kt` measure the actual pane and split the excess width
  around the chapter blocks. Chunks retain their existing padding, including asymmetric margins. The cap applies
  in the 600 dp smallest-width class and above, which keeps landscape phones on their existing layout.
- `NovelIllustrations.kt`, `NovelImageGetter.kt` and `NovelTextRenderer.kt` size embedded images to the measured
  column and refit loaded spans when it resizes. Image-bearing chunks remeasure their span metrics during reflow.
- `NovelWebViewStyler.kt` and `NovelWebViewViewer.kt` apply the same cap using the WebView's viewport width.
  Scrolling uses auto margins and user padding. Paged gutters sit outside the clipped body, so the existing
  pager derives the column width and page stride from `body.clientWidth`.
- Scoped Spotless and `:app:assembleDebug :app:testDebugUnitTest` passed with 972 tests, including four new
  width tests. Headless Chromium checked phone portrait and landscape, tablet portrait and landscape, the
  exact threshold, asymmetric margins, dock resize, page clipping and stride, and the scrolling chapter-end
  card. The 550 px docked pane yielded 518 px of text with the default margins.
- Claude device review remains for Dot and Colour on the tablet in portrait and landscape, with the panel
  docked and undocked. Check TextView reflow on a chapter with an illustration, WebView paging and scrolling,
  the chapter-end card in scrolling mode, and a phone comparison.

**Review (Codex RV1, 2026-11-08):** Pass. Native tablet text spans measured 1343 px including two 16 dp margins at 340 dpi, in portrait and landscape. The dock re-centred the text inside the remaining pane. Colour WebView scrolling and paged layouts showed the same centred column, including a turn to page 2 of 16. The S22 native reader continued to occupy the full phone-width view. An illustration reflow fixture was unavailable. Evidence: `r17-*` and `v76-listen-dot-tablet.png` in the RV1 evidence folder.

#### R-A1 Anime backups (T4) — `review` (`tsuzuki/r-a1`, `81bb813bd`)
ANIME_SCHEMA §4 + §9 Q2 (import only): read Aniyomi backups (fields 500–503) into the anime tables; write anime
into our own backups in the 8000 range; restore round-trip test with a synthetic file; never change existing
manga/novel backup fields. Claude: scratch-install restore test.
Result (Codex 2026-11-09): `tsuzuki/r-a1` at `81bb813bd`, based on A3.6.

- `AnimeBackupWire.kt` reads modern and legacy Aniyomi layouts and writes Tsuzuki anime fields 8000–8002. It
  converts Aniyomi's millisecond episode position and duration to seconds. The existing manga and novel field
  numbers are retained. The SQLDelight change adds a paged query.
- `AnimeBackupStore.kt` maps titles by source and URL, episodes within each title, shelves by name, and season
  parents when both entries are in the file. Restore keeps local shelf memberships, the furthest episode position,
  and the greater watch time. Repeating a restore does not double-count history.
- The debug create and restore dialogs have an Anime option, enabled by default. Validation checks source and
  tracker availability. Backups carry source names and tracker IDs. Install the relevant extensions and sign in
  separately to play or sync.
- JDK 21, scoped Spotless clean on pass 2, `:app:assembleDebug :app:testDebugUnitTest` passed all 1,022 tests.
  The synthetic gzip fixtures cover both Aniyomi layouts, the old reader's manga/novel fields, season remapping,
  and restore into existing progress. The Anime option reuses the label in all eight required locales.
- Scratch-install review on S22 and Tab S11 in Dot and Colour: create and restore a mixed Tsuzuki backup, import
  a sample Aniyomi file, and check Anime off, shelf placement, episode resume and missing-source messaging.

#### R-A2 Player settings + sheets (T3) — `review`
Boards `10-anime/PlayerSettings`, `PlayerSheets`. Subtitles (track pick, size), audio track, speed, decoder
(hw/sw), gestures (seek/brightness/volume), skip ±85 s; persist per user; Dot + Colour.

Result (Codex 2026-11-08): `tsuzuki/r-a2` at `11ba0aba4`, includes A3.2.

- `AnimePlayerSettingsScreen.kt` adds a gated Player page for autoplay, speed, subtitle size, decoder and
  gesture toggles. `AnimePlayerSheets.kt` adds the landscape options panel for subtitles, audio, quality,
  speed, episodes and video. It shows tracks reported by mpv, including HTTP(S) tracks supplied by the source.
- `AnimePlayerSettings.kt` stores preferences and matches track metadata across files with different native
  IDs. The player applies subtitle size/delay, speed and decoder immediately. At non-default speeds,
  the progress tracker stores elapsed watch seconds separately from position in the video.
- Player controls include ±85-second seeks. Double-tap seeks and vertical brightness/media-volume gestures
  respect individual toggles and the controls lock. Upstream hook: `SettingsMainScreen.kt` adds the gated
  Player entry. The volume permission is declared in the debug manifest.
- JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest` green, 972 tests, zero failures.
  Verified placeholders in all eight required locales and the debug-only permission in the merged manifests.
- Claude device check on S22 and Tab S11 in Dot and Colour: use a show with multiple tracks, switch audio
  and subtitles, adjust subtitle size/delay and speed, switch decoder, and verify preferences after reopening
  or changing episodes. Test ±85 seconds and each swipe/tap toggle, including while locked.

#### R-A3 Anime in Updates + "Airing today" (T3) — `review` (tsuzuki/r-a3, 029238bc8)
New episodes from library anime appear in Updates under the media pills (ANIME pill only when ≥ 1 anime in
library); "Airing today" strip from source upload dates (real data only). Anime library update job (separate
worker, respects existing update restrictions).
Result (Codex 2026-11-08): `tsuzuki/r-a3` at `029238bc8`, includes A3.4.

- `TsuzukiAnimeUpdates.kt` and `TsuzukiAnimeLibrary.kt` show Airing today only for a saved title with a
  source upload timestamp in the local day. The Anime media filter lists newly discovered episodes and
  opens the selected episode. The book filters retain their chapter feeds. Added strings in all eight locales.
- `AnimeLibraryStore.kt` shares episode refresh with `AnimeDetailsScreenModel.kt` and excludes baseline
  imports from Updates. Existing episode data retains progress and its new-episode marker across refreshes.
  `animes.sq` and `episodes.sq` use existing columns and add queries only.
- `AnimeLibraryUpdateJob.kt` is a separate WorkManager job. It shares the library schedule, device and
  title restrictions, metadata preference, and throttle. The Anime filter refresh action enqueues it manually.
- Upstream hooks: `App.kt`, `LibraryUpdateJob.kt`, `UpdatesTab.kt`, `UpdatesScreen.kt` and
  `TsuzukiUpdatesTwoPane.kt`. JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest`
  green. 985 tests, zero failures, errors or skips. No schema migration.
- Claude device check on S22 and Tab S11 in Dot and Colour: favorite one show, check the Anime pill and
  baseline exclusion, introduce a later episode through an approved extension, refresh and play the row.
  Verify Airing today appears only for a real upload date, and Wi-Fi and charging restrictions defer the job.

#### R-A4 Anime episode downloads (T4) — `review` (.worktrees/r-a4, 2026-11-09)
WORKPLAN 6.3 third engine: queue in its own store, files under `<downloads>/<source>/<anime>/<episode>/`,
HLS/segments, pause/resume, Wi-Fi only pref, shown in the Downloads screen as an Anime group. Design note in
Result first; Claude reviews before merge (T4).

**Result (Codex, 2026-11-09):** `bc88b73e6` on `tsuzuki/r-a4`. Design: one atomically persisted
anime-only queue and a serial foreground WorkManager job. Video files use app-scoped Downloads
under `anime/<sourceId>/<animeId>/<episodeId>/` so the existing native player can open local HLS
segments by relative path. Manga and novel downloads keep their existing storage and scheduler.

- `AnimeDownloads.kt` and `AnimeDownloadJob.kt` persist queue order, pause and Wi-Fi policy. An
  episode can be retried or removed, and the worker can reopen the queue after process death.
- `AnimeVideoTransfer.kt` and `OfflineHls.kt` stage HTTP video and finite HLS. Direct files resume
  with a matching strong ETag and byte range. HLS reuses completed assets by URL hash and publishes
  a local playlist last. Supported fixtures cover MPEG-TS, fragmented MP4, byte ranges, and AES-128.
- `TsuzukiAnimeDownloadQueue.kt` and `DownloadQueueScreen.kt` add a gated Anime tab with grouped
  progress, downloaded bytes, pause/resume and Wi-Fi-only. `AnimeDetailsScreen.kt` and
  `AnimeEpisodeTiles.kt` queue one or several episodes and show their status. `AnimePlayerModel.kt`
  opens saved files through the existing episode playback and progress path.
- Live streams, DRM, separate HLS audio renditions and extension-provided external tracks are
  outside this implementation. App-scoped video storage is separate from the configurable
  manga/novel folder and is removed on uninstall.

Verification: JDK 21, two clean scoped Spotless passes, `:app:assembleDebug :app:testDebugUnitTest`
(1,030 tests, zero failures, errors or skips), and matching keys in all eight locales. Desktop
FFmpeg decoded synthetic MPEG-TS, fragmented MP4 and AES-128 HLS using only file/crypto protocols.

Device review: back up the S22 Debug install first. In Dot and Colour, queue a direct file and an
HLS episode. Pause and resume each, toggle Wi-Fi-only, interrupt and reopen the app, retry or cancel
a failed item, then disable networking and play the saved episodes. Confirm the existing resume
position survives, remove a saved episode, and check that the Anime tab is absent in Preview.

**Claude list (lane 3):** C1 review + device-check every card above as it lands · C2 A1.5 device test (needs a
non-owner debug install: build debug with a temporary `.scratch` suffix, install one yuzono extension, play an
episode, uninstall) · C3 Q1 frame timing + recording on the owner's phone when it is connected · C4 restore R1's
500-book backup into a scratch install and record jank · C5 Tsundoku + Mihon upstream merge (`scripts/upstream-
report.sh`) · C6 N28/R9 widgets placed and checked · C8 release v0.1
prep when the owner says go (signing, telemetry removal, changelog).

**Prompts for the owner:**
- Thread 1 (anime): `Read AGENTS.md and docs/HANDOFF.md, then work through "Batch R" lane 1 in order (A2.3, A2.4, Y2 rework, Y6 rework, R-A1…R-A4) under the batch Q run rules.`
- Thread 2 (app): `Read AGENTS.md and docs/HANDOFF.md, then work through "Batch R" lane 2 in order (R1…R16) under the batch Q run rules. Never touch anime files.`

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

#### Q1 Smooth motion in Dot (T3) — `done` (Claude merged `tsuzuki/q1` @ 1d8fe9074 into main 2026-11-06; 862 tests green; tablet Dot rail + S22 Colour pill indicator slide checked, crash buffers empty; owner-phone gfxinfo/recording still to do on the laptop. Codex's full Result: `git show 1d8fe9074:docs/HANDOFF.md`)
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

#### Q2 Novel reader: next chapter always reachable (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`)
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

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q2` at `50bfacf8e`.

- `NovelReaderAppBars.kt` dispatches to fixed chapter buttons, progress and the first four configured actions with an overflow for the rest. `TsuzukiNovelChrome.kt` owns those controls. `TsuzukiReaderTitle.kt` ellipsizes the novel subtitle.
- `TsuzukiReaderSlider.kt` and `TsuzukiComponents.kt` reuse the seek track and enable dots in both novel chrome themes.
- `ReaderActivity.kt` connects chapter buttons and the chapter-end Contents action. `ReaderViewModel.kt` resolves adjacent chapters from the visible chapter in infinite scroll, supports the existing read-progress path and checks download status on IO. `NovelChapterNavigation.kt` owns destination selection and queued taps.
- `NovelViewer.kt` mounts the Compose chapter-end card and scrolls to cached chapters on explicit navigation. `TsuzukiChapterEnd.kt` builds the native card and WebView payload with real next-chapter metadata and page colours.
- `NovelWebViewViewer.kt` connects the card to existing next navigation, handles the read and Contents actions, and scrolls to cached chapters. `NovelWebViewStyler.kt` injects `chapter-end-card.js` for continuous non-infinite reading. The JS uses page ink and background in Colour and the shared Dot palette in Dot.
- Eight `i18n` `strings.xml` files add localized reader labels. `NovelChapterNavigationTest.kt` covers visible-chapter routing, the paged path and one-time replay of the last queued direction.

Upstream files touched: `NovelReaderAppBars.kt`, `ReaderActivity.kt`, `ReaderViewModel.kt`, `NovelViewer.kt`, `NovelWebViewViewer.kt`, `NovelWebViewStyler.kt`, and the eight `i18n` resources.

Decisions and gaps: the fixed chrome is for Tsuzuki themes. Legacy toolbar configuration and its paged WebView page-turn arrows retain their existing behavior. Paged WebView uses the fixed chapter buttons and its existing edge gesture, with the in-page card in continuous mode only. The WebView card uses system font families, while the Compose card uses the Tsuzuki type ramp.

Verification: JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest` green (865 tests, 0 failures), JavaScript syntax and DOM/bridge smoke test green.

Claude device check: S22 and Tab S11, Dot then Colour. Open a novel with more than four enabled actions. Check the ellipses, both fixed arrows, overflow, and Settings at large font sizes. In TextView and WebView, scroll across appended chapters and then navigate in both directions, including back to a cached chapter and a second tap while loading. In paged WebView, verify that the fixed buttons cross chapters and tap/swipe still turns pages. With infinite scroll off, compare the card in Sepia, Night and Black, check the real downloaded marker and exercise Next chapter, Mark read and Contents. Check first and final chapter boundary states.

#### Q3 Save chapters: choose a range by chapter number (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`)
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

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q3` at `800f92ce4`.

- `TsuzukiDownloadSheet.kt` replaces the slider and global switches with preset pills, decimal From/To fields, repeating steppers, shortcuts, inline validation, a range CTA, and a link to Download settings. The sheet shows full-book totals alongside the actual eligible and already-saved counts.
- `TsuzukiDownloadSelection.kt` resolves inclusive source numbers, falls back to 1-based reading order for unnumbered chapters, and separates the selected range from eligible downloads. It also supplies the visible long-press range helper.
- `MangaViewModel.kt` applies the visible range by chapter ID against the full chapter list, so selection in a filtered list preserves hidden chapters and full-book counts.
- `TsuzukiDownloadSelectionTest.kt` covers decimal boundaries, duplicate scanlator numbers, unnumbered chapters, invalid input, and visible selection boundaries. Eight locale `strings.xml` files supply matching labels and format placeholders.

Upstream files touched: `MangaViewModel.kt` and the eight `i18n` resources. The input uses a decimal IME with Done so fractional numbers are available. Next 10 and Bookmarked stay in the sheet’s overflow.

Verification: per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (865 tests, 0 failures). Q3 locale keys and placeholders match in all eight locales.

Claude device check: Dot then Colour on S22 and Tab S11. Open Save chapters on a long book. Enter a decimal range, hold both steppers, exercise both shortcuts and check reversed, out-of-range and all-saved input. Compare totals with details, inspect the queued IDs and skipped count, and check the Download settings link and IME Done. Long-press two separated visible chapters with a filter active, download the selected range, clear the filter and confirm that the full list is available.

#### Q4 Two "Default" shelves in the library (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`)
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

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q4` at `e8f02e97b`.

- `LNReaderBackupImporter.kt` uses `LNReaderShelves.kt` to map LNReader’s Default ID and localized names to the system shelf, deduplicate Default memberships and preserve distinct category order. The importer’s existing dangling KDoc now attaches to its class.
- `DefaultShelfMigration.kt` moves matching novel and all-content memberships to category 0 in one database transaction, preserves other memberships and sets its one-time preference after success. `App.kt` calls it during background startup. A configured default pointing to a removed shelf follows the system shelf.
- `TsuzukiShelfPills.kt` disambiguates duplicate names with content type and, if needed, shelf ID. `LibraryTabs.kt` uses that implementation. `LibraryContent.kt`, `LibraryTab.kt`, `NovelsTab.kt` and `LibraryViewModel.kt` pass counts to the pills. The resident library uses its filtered group counts, while experimental pagination shows full shelf totals from the database.
- `DefaultShelfMigrationTest.kt` covers membership preservation, one-time execution, failure retry, LNReader mapping and label collisions.

Upstream files touched: `LNReaderBackupImporter.kt`, `App.kt`, `LibraryTabs.kt`, `LibraryContent.kt`, `LibraryTab.kt`, `NovelsTab.kt` and `LibraryViewModel.kt`. The migration uses existing SQLDelight queries. Shelf suffixes reuse translated strings.

Verification: per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (866 tests, 0 failures). An in-memory SQLite fixture using the checked-in table definitions and insert/delete statements verified category 0, existing memberships and foreign keys.

Claude device check: export an in-app backup first and use the preview test data. Start with a duplicate Default shelf containing novels, including a book also in Local. Verify a single system Default pill with the combined real count, Local and other shelves intact, and no extra memberships after a second start. Import an LNReader fixture with Default plus a custom shelf and verify the same mapping. Test a manga-only shelf sharing the Default label, then check Dot and Colour on phone and tablet.

#### Q5 Navigation: Library tab + real labels (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`) (Claude rebased onto main 2026-11-06: `tsuzuki/q5` @ 6612e1f1b, worktree **already exists** at `.worktrees/q5` — use it, don't create a new one. Code is nearly done; what's left: build + tests green, check the items in Codex's earlier Result (`git show 4345fc994:docs/HANDOFF.md`, card Q5), commit, fill Result) (owner approved 2026-11-06)
Mihon's five tabs are Library · Updates · History · Browse · More; in Tsuzuki the library is only reachable from
Home's "ALL BOOKS →" (no nav item) and Dot labels are cryptic (`NEW / PAST / FIND / •••`). Board `Library.dc.html` /
`Browse.dc.html`: **HOME · LIBRARY · UPDATES · BROWSE · MORE** (History moves into More + Home's recent row).
Uses Q1's sliding indicator. Tablet rail gets the same items. TalkBack labels unchanged words.
- History stays reachable: a HISTORY tile/row in More (top group) and Home's recent row opens it; deep links and
  shortcuts that targeted the History tab still land on History. Library tab opens the existing library screen
  (the one behind Home's ALL BOOKS →), keeps its state across tab switches, and Home's ALL BOOKS → selects the tab.
- Badges: Updates keeps its unread badge. Back from any tab returns to Home (as now).
- Tests: nav item list per layout, History routing. Device check (Claude): phone pill + tablet rail, Dot + Colour.

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q5` at `b3165f66b`, on top of Claude’s rebased `6612e1f1b`.

- `TsuzukiNavigation.kt` supplies the five destinations and resolves hidden History, shortcuts and retained Library media. `HomeScreen.kt` wires that policy to the phone bar and tablet rail, keeps the Updates badge and preserves Back to Home.
- `TsuzukiHomeTab.kt` sends All Books through the retained Library route. The Recent row continues to open History.
- `MoreTab.kt` opens History from the tile dispatched through `MoreScreen.kt`. `TsuzukiMoreScreen.kt` places that tile below the top toggles.
- `TsuzukiSettingsMain.kt` presents the same five destinations in the tablet Settings rail. `TsuzukiNav.kt` documents the Dot label role.
- `TsuzukiNavigationTest.kt` covers shared order, History and Library routing, retained and restored media, and shortcut destinations.

Upstream files touched: `HomeScreen.kt`, `MoreTab.kt` and `MoreScreen.kt`. Labels reuse existing translations. The Library route uses existing Library tabs and saveable state.

Verification: per-file Spotless twice, JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (869 tests, 0 failures).

Claude device check: Dot then Colour on S22 and Tab S11. Verify the full Home, Library, Updates, Browse and More labels and the sliding indicator, including rapid reversals and reduce motion. In Library, change shelf and filters, open a book, switch tabs and return. In separate mode, select Manga, switch to Home and use All Books to reopen Manga. Open History from More, Home's recent row and the launcher shortcut. Verify the Updates badge, Back to Home, long localized labels at increased font scale, and leaving the tablet Settings rail for Library.

#### Q6 Book details header (T2) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`)
`evidence/details-header.png`, board `Details.dc.html`:
- Toolbar: back · download · filter · overflow. Move scroll-to-top / next-unread / scroll-to-bottom into the overflow
  (Tsundoku added them; keep the actions, Mihon shows only download/filter/more).
- Hide tiles without data: the hourglass `N/A` tile; chapter rows show no `N/A` date line when the date is unknown.
- One Resume: the big Resume tile stays; the floating `Resume` FAB over the chapter list only appears after the tile
  scrolls off screen (it currently overlaps chapter 2 while the tile is visible).
- Genre chips wrap to max two lines (+N chip) instead of being clipped at the card edge ("Myster…").
- Dot case consistency: tile labels uppercase via tokens ("In library", "Tracking", "WebView" are mixed case);
  summary/author stay sentence case in the body face. Summary expand chevron must not overlap text.

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q6` at `105f67354`, based on the Q3 commit `800f92ce4`.

- `MangaToolbar.kt` puts scroll-to-top, next unread and scroll-to-bottom in the Tsuzuki overflow while keeping Download and Filter in the toolbar.
- `MangaInfoHeader.kt` hides the empty update tile, uses themed action labels and supplies a summary layout with a separate chevron. The summary and author keep their source casing.
- `TsuzukiDetailsDescription.kt` measures genres for two rows, reserves a `+N` pill when needed, and keeps search and copy available for every tag. `TsuzukiGenreRowsTest.kt` covers fitting, overflow and wide chips.
- `MangaScreen.kt` omits unknown chapter dates, gates the floating Resume action on tile visibility in both layouts, and accounts for the themed header items in chapter scroll targets. `TsuzukiResumeTile.kt` uses the board’s neutral tile and accent play disc.

Upstream files touched: `MangaToolbar.kt`, `MangaInfoHeader.kt` and `MangaScreen.kt`. Existing translated labels and actual chapter and genre data are reused.

Verification: per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (868 tests, 0 failures).

Claude device check: Dot then Colour on S22 and Tab S11. Open a title with an unknown update date and chapter dates, verify the three primary toolbar actions and exercise all overflow jumps. Scroll the Resume tile fully out and back into view, including the tablet’s independent metadata pane. Open a book with many genres at larger font scale, use the `+N` pill and search/copy an overflowed genre. Expand the summary and check that its control clears the text.

#### Q7 Browse tabs + source rows (T2) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`) `parallel-ok`
`evidence/tabs-updates-history-browse-more.png` (3rd panel), board `Browse.dc.html`: the 4 pills
(NOVEL SOURCES · MANGA SOURCES · NOVEL EXT… · …) run off screen. Use media pills (NOVELS | MANGA) + a 2-item
segmented control (SOURCES | EXTENSIONS) — the existing pager keeps its 4 pages, only the header changes. Source rows:
name gets the full width (no "LightNovelP…" at 384 dp), LATEST becomes a compact outline chip, pin stays 48 dp.

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q7` at `1b7a84e78`.

- `TsuzukiBrowseHeader.kt` presents the media pills and Sources/Extensions segments over the existing pager, with the active media’s extension update count.
- `BrowseTab.kt` uses a typed page map for header, toolbar, search and shortcut routing. The live pager has four source/extension pages plus two migration pages. Each migration page stays available from its media’s overflow and from the existing pager.
- `TabbedScreen.kt` exposes a header slot and guards the current page while the tab count changes.
- `SourcesScreen.kt` dispatches themed rows to `TsuzukiSourceRow.kt`. The source name can wrap and has its own line, Latest has a compact outline, the pin target is 48 dp, and a preset remains available on its supporting row. Ordinary language sections use their existing language headings.
- `TsuzukiBrowsePagesTest.kt` covers the existing page order, hidden manga mode and the extension shortcut destination.

Upstream files touched: `BrowseTab.kt`, `TabbedScreen.kt` and `SourcesScreen.kt`. Existing localized labels are reused.

Verification: per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (864 tests, 0 failures).

Claude device check: Dot then Colour on S22 and Tab S11. Exercise both media pills, both segments, swipes, update counts, the extension shortcut and both migration routes. Switch to hidden manga mode and repeat the novel flow. Check long source names, Latest, preset, pin and long-press pin actions at phone width and in the tablet’s two-pane layout.

#### Q8 Overflow and large-font sweep (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
**From Claude's 2026-11-07 device pass (include these):** reader action row needs short labels (SCROLL TO TOP / START AUTO-SCROLL ellipsize now; board `Main` uses one word each); More tiles `DATA AND STOR…`, `DOWNLOADED ON…`; details `131 chapters` heading not uppercased in Dot.
Every Tsuzuki screen at **360 dp width** and **font scale 1.0 / 1.3 / 2.0**, Dot + Colour, EN + JA. Known on the
phone now: More "DATA AND STORA…", reader title hard-cut, genre chips, Browse pills, source names, Home hero title
(`LORD OF / THE / MYSTERIES` 3 lines pushes the stack), shelf pills. Add `@Preview` groups
(`widthDp = 360, fontScale = 1.3f/2f`) for each Tsuzuki screen and fix: no clipped text without ellipsis, rows that
wrap instead of scroll, min 48 dp targets, no fixed heights around text. List every fix in Result.

**Result (Codex, 2026-11-07):** `tsuzuki/q8` @ `e6a34735b`. **893 tests, 0 failures.** Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21.
- `TsuzukiPill.kt` / `TsuzukiSegments.kt` — 48 dp controls, wrapping rows and labels, shelf/tab overflow with active choice retained, measured indicator height and a wrapped high-font-scale layout.
- `TsuzukiNav.kt` — taller bottom cells and two-line ellipsis for large text while retaining the sliding indicator and spoken labels.
- `TsuzukiHomeScreen.kt` — two-line ellipsized title, independent cover stack height, stacked hero and stat tiles at large font scales, a growing Resume row, and 48 dp Read/Watch controls.
- `TsuzukiMoreScreen.kt` — growing tile heights and multiline labels/subtitles. Phone tiles stack at large font scales.
- `TsuzukiBrowse.kt` / `TsuzukiSourceRow.kt` — multiline search and pinned labels, source names with ellipsis, wrapping metadata, and Latest under the title at large font scales.
- `TsuzukiNovelChrome.kt` / `NovelReaderAppBars.kt` — short labels for Top, Scroll/Stop, Rotate and paragraph actions, with full TalkBack/overflow descriptions. Fewer primary actions at larger scales, and progress labels can wrap. Q2 already ellipsizes novel titles and keeps fixed Previous and Next controls.
- `TsuzukiPageColours.kt` / `TsuzukiReaderSettingsSheet.kt` — quick swatches stay six across at the standard scale and wrap to three with large type. Font tiles grow and wrap. Full settings already has wrapping choices from Q11.
- `ChapterHeader.kt` — Dot chapter count uses uppercase, with a taller target. Existing Q6 genre rows fit two measured lines and keep extra tags in the overflow.
- `TsuzukiHistoryStats.kt` / `TsuzukiUpdatesTiles.kt` / `TsuzukiSettingsMain.kt` — growing stat tiles and wrapping units; settings titles and subtitles allow two lines.
- `TsuzukiNarrowPreviews.kt` — 360 dp English/Japanese previews at 1.0, 1.3 and 2.0 for Dot and Colour. Covers Home, Library/nav, Browse, Details, More, novel and manga reader chrome, reader settings, Settings, Updates and History. Previews use real composables with sample data. Separate previews for secondary screens were left out.
- `TsuzukiNarrowLayoutTest.kt` — checks shelf overflow and reader action capacity. Eight locale files add short reader labels. Upstream files touched: `ChapterHeader.kt` and `NovelReaderAppBars.kt`.
- Claude device checks: at 360 dp across the preview matrix, check More's Data and Storage and Downloaded only, Home long title and cover shuffle, Library selected shelf from More, Browse Latest and long source names, Details chapter count and genre overflow, reader short action labels and More, quick sheet swatches and font rows, and History/Updates stat tiles. Confirm long labels wrap or ellipsize and all actions stay reachable. Actual visual previews and device checks remain pending.

#### Q9 Accessibility + quality of life, Mihon parity (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
Boards (fixes-2026-11): `Q9Selection`, `Q9Undo`, `Q9UpdatesLoading`, `Q9SettingsAdvanced` + `Colour*` twins (static `.html` + `png/`).
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

**Result (Codex, 2026-11-07):** `tsuzuki/q9` @ `11efb24f4`. **892 tests, 0 failures.** Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21.
- `TsuzukiSelectionBar.kt`, `TsuzukiSelectionActions.kt`, `TsuzukiSelectedCover.kt` — labeled, font-scale-aware selection actions with overflow, localized counts, and checked grid covers. Existing Q3 chapter, Library and Updates long-press ranges remain in use.
- `TsuzukiHistorySelection.kt`, `TsuzukiHistoryItem.kt` — History range selection over visible rows, accent selection, and bulk removal of the selected entries.
- `TsuzukiUndoSnackbar.kt` — six-second Undo window and progress bar, with explicit dismissal under TalkBack. Library removal updates favorites immediately and restores them on Undo. Mark read and file deletion are staged until the window closes because they also delete files or translations and may notify trackers. Existing advanced destructive Library operations remain in the explicit Delete menu.
- `TsuzukiUpdatesLoading.kt` — static first-load skeleton with a localized loading label and no invented count. `UpdatesViewModel.kt` already maps fetched rows on IO. Grouped mode now publishes the first page while it prefetches. The 100 ms board target still needs a device trace.
- `TsuzukiAdvancedPreferences.kt` — search expands and scrolls to rare options. Folded groups/options: Library behavior, update metadata and recent-update interval; Manga Reader E-Ink and actions; Novel Reader formatting, content and auto-scroll; Downloads deletion rules; Novel Downloads throttling, mass import and per-extension overrides; Browse delay, cover skipping and back confirmation; Tracking chapter thresholds; Data cleanup. Search and reset still see the original preference tree.
- `TsuzukiTokens.kt`, `TsuzukiNav.kt`, `TsuzukiHomeScreen.kt` — shared muted text ink for read labels, nav and the low-contrast Home label. Q5 already supplies spoken nav labels. `TsuzukiSelectionTest.kt` covers action capacity, History ranges, search reveal and 4.5:1 contrast on the default Dot and Colour surfaces.
- `i18n/src/commonMain/moko-resources/{base,ja,zh-rCN,ko,es,pt-rBR,fr,de}/strings.xml` — new selection, Undo, loading and settings copy.
- Upstream files: `AppBar.kt`, `LibraryToolbar.kt`, `MangaToolbar.kt` (selection count); `CommonMangaItem.kt`, `MangaBottomActionMenu.kt`, `MangaChapterListItem.kt`, `UpdatesUiItem.kt` (selection/contrast hooks); `HistoryScreen.kt`, `HistoryItem.kt`, `HistoryTab.kt` (selection wiring); `PreferenceScreen.kt`, `PreferenceScaffold.kt` (Advanced render hook); `LibraryTab.kt`, `NovelsTab.kt`, `LibraryViewModel.kt` (library undo); both `MangaScreen.kt` files and `MangaViewModel.kt` (chapter undo); `UpdatesScreen.kt`, `UpdatesTab.kt`, `UpdatesViewModel.kt` (loading, actions and undo).
- Claude device checks: on 360–384 dp phone and tablet in Dot and Colour, long-press two entries in Library, chapter list, Updates and History, then check the range, count, cover check, Back and all More actions. At 200% font scale confirm all actions remain reachable. For disposable entries, remove from Library and undo, and cancel then repeat mark-read and delete-download through expiry. Enable TalkBack and verify announcement, persistence and dismiss behavior. Open Settings Search to a folded option and verify it expands and scrolls. Capture first-frame timing on the large Updates dataset, with grouped mode and Reduce motion both on and off.

#### Q11 Reader settings redesign: quick sheet + All reader settings (T4) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
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

**Result (Codex, 2026-11-07):** `tsuzuki/q11` @ `34ff66af1`. Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21. **891 tests, 0 failures**.
- `TsuzukiReaderSettingsSheet.kt` / `TsuzukiPageColours.kt` — Text, Page and Controls tabs, six named swatches in the quick row, shared reader controls and reset/full-settings actions.
- `TsuzukiAllReaderSettings.kt` — full-screen dialog with five labeled tabs, live text preview, linked side/vertical margins, custom colours and Fine-tune sections for paragraph, page and highlight controls.
- `TsuzukiReaderSettingsLogic.kt` / `TsuzukiReaderSettingsLogicTest.kt` — tab-to-group mapping, locale-aware spacing, linked margin midpoint and reset to existing defaults, with three tests.
- `i18n/src/commonMain/moko-resources/{base,ja,zh-rCN,ko,es,pt-rBR,fr,de}/strings.xml` — translated new settings labels, sample text and reset prompt.
- Upstream files: `ReaderSettingsDialog.kt` dispatches the Tsuzuki page and gives the manga sheet compact labeled tabs; `NovelPage.kt` exposes the existing settings groups and highlight controls; `presentation-core/.../SettingsItems.kt` gives the Tsuzuki page stacked labels, unit displays and decimal entry while retaining the default layout for other callers.
- Reset is confirmed, novel-only and preserves authored CSS/JS snippets and replacement rules. When the two individual margins differ, both are shown and the first linked edit starts from the rounded midpoint. The preview uses sample text and the selected font, spacing and palette. Site-supplied WebView fonts and authored CSS are left to the actual chapter. Existing Tsundoku English-only advanced/TTS labels were carried forward.
- Claude device checks: at 360–384 dp in Dot and Colour, check six named swatches, both quick metric values and the full-screen open/back path. Test decimal entry in a comma locale, Text preview with custom font and Book/Night page colours, Page custom ink/background, independently set margins then relink, status bar position, native/WebView switching, highlight Fine-tune, and Reset after adding a disposable replacement rule. On tablet verify insets, landscape scrolling and all five tabs, then check the three labeled manga tabs in both themes.

#### Q12 Remove the themes we didn't make (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`)
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

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q12` at `6deadf25d`.

- `TsuzukiThemePreferences.kt`, `UiPreferences.kt`, `ReaderPreferences.kt` and `App.kt` normalize stored themes and write canonical values at startup. `PreferenceRestorer.kt` applies the same mapping to incoming app backup preferences.
- `AppThemePreferenceWidget.kt` shows Colour and Dot only. `SettingsAppearanceScreen.kt`, `TsuzukiSettingsHeader.kt` and `TsuzukiAccentPicker.kt` expose the two cards, system/light/dark, Colour accent swatches and Dot cover colour.
- `TsuzukiPageColours.kt` supplies the seven page colours and the Custom editors to `NovelPage.kt`, `SettingsNovelReaderScreen.kt` and `TsuzukiReaderSettingsSheet.kt`. The pre-existing overlength subtitle in `SettingsNovelReaderScreen.kt` was wrapped for per-file Spotless.
- `ThemeUtils.kt`, `NovelViewer.kt`, `NovelWebViewStyler.kt` and `NovelWebViewViewer.kt` share palette resolution for TextView and WebView. The stored foreground and background overrides apply in Custom.
- `TsuzukiThemePreferencesTest.kt` covers every legacy enum value, the novel aliases and backup preference normalization. Eight locale `strings.xml` files provide the new labels.

Upstream files touched: `UiPreferences.kt`, `ReaderPreferences.kt`, `App.kt`, `PreferenceRestorer.kt`, `AppThemePreferenceWidget.kt`, `SettingsAppearanceScreen.kt`, `SettingsNovelReaderScreen.kt`, `NovelPage.kt`, `ThemeUtils.kt`, `NovelViewer.kt`, `NovelWebViewStyler.kt`, `NovelWebViewViewer.kt` and the eight `i18n` resources.

Retained for backup and upstream compatibility: DEFAULT, MONET, CATPPUCCIN, GREEN_APPLE, LAVENDER, MIDNIGHT_DUSK, NORD, STRAWBERRY_DAIQUIRI, TAKO, TEALTURQUOISE, TIDAL_WAVE, YINYANG, YOTSUBA, TOKYONIGHT, MONOCHROME, DARK_BLUE, HOT_PINK and BLUE enum entries and colour schemes. Deletion belongs to a later card.

Exact AGENTS.md replacement for the legacy theme bullet:

> - **App themes.** Colour and Dot are the only selectable app themes. Stored and restored legacy app themes resolve to Colour. Keep the legacy `AppTheme` entries, colour schemes and `TsuzukiStyle.NONE` for backup compatibility and Tsundoku merges. The shared novel page choices are Book, Paper, Sepia, Sage, Night, Black and Custom. The saved custom text and background colours apply in Custom.

Verification: per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (866 tests, 0 failures). All eight locales include the new labels.

Claude device check: export a backup first and use preview test data. Restore settings from a backup with a legacy app theme and novel `light`, `dark`, `grey` or `app`, and verify canonical choices. Check two theme cards, switching, system/light/dark, all eight accent swatches and both Dot cover states on phone and tablet. Open TextView and WebView and compare Book, Paper, Night, Black and a saved Custom pair. Switch away from Custom and return to verify the saved colours.

#### Q10 Docs: JDK 21 path (T1) — `done` (Claude 2026-11-06: AGENTS.md Build picks jbr 21 or Temurin 21; owner phone in device table)
`AGENTS.md` Build section still says JDK 21 is Android Studio's `jbr`; Android Studio 2026.2 ships JDK 25. Point to
Temurin 21 (`C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot`) and note the owner's phone serial
`R5GL23ZYE1V` (release app `io.github.manojx2005.tsuzuki`) in the device table. Docs only.


### Extra wave X (owner approved 2026-11-06; only after the batch Q rows above)

General: each X card first checks whether the work already exists (grep `presentation/tsuzuki/`, the status
table in `docs/design/README.md`). If it is already done, set `done` with a one-line Result saying where, and
move on.

#### X1 Reader hook consolidation (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
WORKPLAN P0.4. Start from `git diff $(git merge-base HEAD tsundoku/main) HEAD -- app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt`
(includes Q2's additions once merged). Move every Tsuzuki addition (reader transition animation selection,
`ReadingDays` recording, contents drawer relaunch, Tsuzuki chrome, Q2's next-chapter hooks) into
`presentation/tsuzuki/reader/TsuzukiReaderHooks.kt` (or a few small files there); the upstream files keep
one-line calls. **Behaviour identical**: no logic changes, no re-indent of Tsundoku code; Tsundoku's own novel
branches stay where they are. Tests for any pure helper you extract. Result lists each moved block
(upstream line → new function) so Claude can verify on devices: novel TextView + WebView open/close animation,
progress/history saved, `ReadingDays` minutes increase, contents drawer jump, manga paged + long strip.
Result: _pending_

**Result (Codex, 2026-11-07):** `tsuzuki/x1` @ `c54d8fba3`. **890 tests, 0 failures.** Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21.
- `ReaderActivity.kt` pre-card lines 167, 936–946, 1238–1242 and 1626–1628 → `TsuzukiReaderHooks.kt`, `TsuzukiReaderChromeRequests.bind/openContents` and `TsuzukiReaderChrome.Overlay`. The activity owns the request lifetime, with the drawer and find state still scoped to the novel composition. The chrome binding provides contents, find and adjacent-button availability.
- `ReaderActivity.kt` pre-card lines 320–327 and 1734–1779 → `TsuzukiAdjacentChapterHooks.kt`, `TsuzukiAdjacentNavigation.observe/navigate`. Preserves the last pending direction, paged reset before load, WebView direction notification on success, and then page reset. Tsundoku manual versus TTS entry points keep their existing TTS behavior.
- `ReaderViewModel.kt` pre-card lines 873–881 and 1112–1125 → `isTsuzukiChapterOnDisk`, `markTsuzukiChapterRead`, and `State.resolveTsuzukiAdjacentChapter`. The last still checks for a viewer chapter before evaluating the lazy chapter list. The transition choice and ReadingDays write were already delegated through `applyTsuzukiReaderTransition` and `recordTsuzukiReadingTime`. The N20 disk helper and Q2 visible-chapter reset remain one-line hooks.
- `TsuzukiReaderHooksTest.kt` — adds lazy adjacency and chapter-end mark-read coverage. Existing transition, contents and Q2 pending/paged navigation tests still pass. Upstream touched: `ReaderActivity.kt` and `ReaderViewModel.kt` only.
- Claude device checks: on tablet and phone, open and close a novel in TextView and WebView, save progress, verify History and ReadingDays minutes, open Contents from the chrome and the end card, and jump to another chapter. Check find in WebView, infinite-scroll visible-chapter navigation, a second tap during a pending load, paged WebView edges, TTS handoff, and manga paged and long strip.

#### X2 Tablet Updates: list + chapter preview (T3) — `done` (already built: Claude saw list + preview pane on the tablet 2026-11-06; card kept for reference)
Board `docs/design/boards/png/11-tablet/TabletUpdates.png` (+ `dot/`), HTML `html/11-tablet/TabletUpdates.html`.
On windows ≥ 900 dp wide (same breakpoint helper as N9 Tablet Browse / N25 side panel — reuse it), Updates
becomes two panes: the existing Tsuzuki updates list on the left, the selected chapter's book card on the right
(cover, title, chapter number/title, date, downloaded state, actions READ / DOWNLOAD / OPEN BOOK). Phone layout
unchanged. Selection survives rotation (`rememberSaveable` chapter id). Real data only: no excerpt unless it is
cheap and real — otherwise leave it out and say so. Upstream `UpdatesScreen.kt` keeps one dispatch.
Tests: pane/selection logic.
Result: _pending_

#### X3 Restyle check: ReplaceRules + SettingsTranslation (T2) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
Boards `png/06-reader/ReplaceRules.png`, `png/08-tools/SettingsTranslation.png` (+ `dot/`, HTML alongside).
Find the screens (Tsundoku text replacement rules, translation engine settings). If they are stock in Tsuzuki
themes, restyle with `PreferenceScreen` / `tsuzukiGroupedTile` / `TsuzukiPill` per the boards (dispatch at the
top of the upstream composable). Never show an API key in clear text; add no network calls. Leave out any board
stat without backing data.

**Result (Codex, 2026-11-07):** `tsuzuki/x3` @ `09c787c50`. **891 tests, 0 failures.** Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21.
- Replacement Rules already dispatches to `TsuzukiReplaceRulesSection` from Q11 More. Verified its themed rule tiles, toggles, live test input, regex validation and add/delete actions against both boards. Chapter excerpts, match counts and per-book/per-source scopes have no backing data here and were left out.
- Translation already uses themed `PreferenceScreen` groups and wrapped engine pills. `SettingsTranslationScreen.kt` now routes nine API keys and Custom HTTP headers through a sensitive preference and displays localized pass/fail for the existing Test action. Provider responses and exception messages stay out of the UI. The existing Test request still runs only when explicitly tapped.
- `TsuzukiSensitivePreference.kt` — fixed mask on configured rows, blank replacement editor with fully obscured typing and a secure window. The saved value is never prefilled or copied into dialog state. Custom headers are replaced as a whole because they can contain credentials.
- Upstream `Preference.kt`, `PreferenceItem.kt` and `EditTextPreferenceWidget.kt` carry the sensitive flag and dispatch. `SettingsTranslationScreen.kt` opts in credential fields and uses the safe test copy. The eight locale files add two test status labels. No new pure logic warranted a unit test.
- Claude device checks: in Dot and Colour on phone and tablet, open Q11 More → Replacement Rules, toggle a rule, enter valid and invalid regex in a disposable draft and compare the live test result. In Translation switch engines, open a disposable saved credential and Custom HTTP headers, verify blank/masked entry, rotation and cancel, and check that Test shows only pass/fail or not configured. Use a fixture for any provider error that echoes credentials.

#### X4 Restyle check: DuplicateFinder + Migration (T2) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
Boards `png/08-tools/DuplicateFinder.png`, `png/04-browse/Migration.png`, `png/04-browse/MigrationCompare.png`
(+ `dot/`). `presentation/tsuzuki/migration/` exists: compare it to the boards first and fix only what differs
(list the differences in Result). DuplicateFinder: restyle if stock. No change to migration logic.

**Result (Codex, 2026-11-07):** `tsuzuki/x4` @ `7ce6588b0`. **888 tests, 0 failures.** Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21.
- Duplicate Finder already dispatches to `TsuzukiDuplicateFinderScreen`. Verified its matching modes, keep-one pills, grouped tiles, selection counts and actions against Dot and Colour. No second layout was needed.
- `TsuzukiMigrationSetup.kt` adds the selected-book summary and a full-width Find matches action. It reads up to five real book titles/covers off the main thread and uses the actual selected book/source counts.
- `MigrationConfigScreen.kt` supplies the setup summary and footer, themed header/rows and Dot source casing. `MigrationConfigScreenSheet.kt` uses themed flag pills, switches and the primary button while retaining the existing options sheet and the same flag preference callbacks. These two are the upstream files touched.
- `TsuzukiMigrationList.kt` keeps OLD/NEW cards with an accent outline, adds available author metadata, omits unknown latest-chapter filler and uses a Back affordance routed to the existing exit flow. Skip and Migrate remain prominent on successful matches. Copy and Search manually move to More and still use the existing callbacks.
- The eight locale files add setup count labels. The board's confidence score, candidate list, retained highlights/download estimates, and per-book chapter progress were left out because those values are unavailable to these UI routes. The setup retains the reorderable source list and the separate options sheet. Migration flags, matching, transfer and skip logic stay in their existing implementation.
- Claude device checks: in Dot and Colour, open Duplicate Finder and exercise selection without deleting real entries. Select one and several disposable books for Migration, check setup book names and source count, source order, flags and the primary footer. Run a disposable search to see successful/unmatched rows, ensure Back still opens the exit dialog, and check Skip, Migrate, Copy and Search manually in their expected positions.

### Anime lane A (owner approved 2026-11-06) — read `docs/ANIME_SCHEMA.md` §9 decisions first

Common rules: new code under `app/.../tsuzuki/anime/`, `domain/.../anime/`, `data/.../anime/` (packages per
ANIME_SCHEMA §7); Apache-2.0 only — Aniyomi files may be ported only if their header is Apache-2.0, note each
ported file + upstream revision in Result; nothing visible to normal users in release builds until A2.4 (Watch
tab already exists as a placeholder). No GPL. Never download or install anything yourself — no network in tests.

#### A1.5 Anime: debug extension install → list sources → play (T3) — `done` (Claude reviewed, merged into main 2026-11-07 via `review/night4` → `c7c805cf2`)
Board: `docs/design/boards/fixes-2026-11/AnimeDebug.html` / `png/AnimeDebug.png` (Dot only).
Goal: from a **debug-only** developer screen, add an Aniyomi-format extension repo URL, list its extensions, install
one into the private dir (`files/anime-exts`, via the loader ported in `9b2a924de` with the host's trust/signature
check), list the installed sources, fetch Popular for one source, open one entry's episode list, resolve the video
list of the first episode and play it in the N22 debug player (`anime-player` module, debug only).
- Repo index: Aniyomi repos publish `index.min.json` (+ `apk/` and `icon/`); parse it with a pure parser
  (`AnimeRepoIndexParser`) + tests from a checked-in fixture file. Accept a GitHub repo URL and map it to the raw
  `repo` branch URL the way the manga repo code does (look at `DefaultSourceRepos.kt` / manga repo handling).
- Owner-approved test repos (Claude tests with these, Codex only writes code): `https://github.com/yuzono/anime-extensions`,
  `https://github.com/Secozzi/aniyomi-extensions`. Store added repos in a debug preference, not the DB (Q6 later).
- Network calls go through the existing `NetworkHelper` client; all work off the main thread; errors shown as text
  on the screen (not swallowed).
- Entry point: a row in the existing debug/developer settings screen (find where N22's debug player is launched).
- Tests: index parser, URL mapping, install-state model. Result: what Claude must tap to play an episode.

**Result (Codex, 2026-11-07):** `tsuzuki/a1.5` @ `03038f852`. **893 tests, 0 failures.** JDK 21, per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- `AnimeRepoIndexParser.kt` validates JSON entries and maps GitHub URLs to their repo branch, including Yūzōnō's separate anime-repo alias. `AnimeRepoIndexParserTest.kt` and the synthetic `anime-repo-index.json` cover parsing, URL mapping, unsafe paths and the four install states.
- `AnimeDebugRepository.kt` keeps repo URLs in a debug preference, fetches via NetworkHelper, checks the APK package/version/signature and installs via the existing private loader. Existing packages are reused. Untrusted packages require a separate fingerprint confirmation before they load. No third-party source code was copied.
- `AnimeDebugActivity.kt` adds the five-step Dot flow through Popular, episodes and playable video choices. The debug manifest registers it privately. Upstream hook `DebugInfoScreen.kt` adds the row only to the debug build. Eight locale files add the new interface labels.
- `AnimeDebugPlayback.kt` and the N22 `AnimeDebugPlayerActivity.kt` hook hand the selected video to mpv in memory and support Referer, User-Agent and Origin. This debug path omits extension-specific mpv arguments, cookies, alternate tracks and local servers. Only the JSON index is in scope. Secozzi's current advertised protobuf URL has not been exercised.
- Claude device check on the scratch arm64 debug install: Settings > Developer/Debug info > Anime test. Enter the approved Yūzōnō GitHub URL, tap Load, Install one extension, then Trust after checking the package, version and fingerprint. Tap Popular on its source, Episodes on a title, Load first episode videos, then Play on a video. Confirm playback and seek, Back returns to the flow, and reopening lists the saved repo and trusted source. Repeat repo loading with the approved Secozzi URL and record any live index error. No repo/APK was fetched or installed by Codex.

#### A2.2 Anime tables + data layer — migration 36 (T4) — `done` (Claude reviewed, merged into main 2026-11-07 via `review/night4` → `c7c805cf2`)
Implement ANIME_SCHEMA §2–§3 and §7 **with the §9 decisions** (separate `anime_categories`; `anime_sync`; season
columns kept; no fansub exclusion; no `extension_store` column).
- `ls data/src/main/sqldelight/tachiyomi/migrations | sort -n | tail -1` → use latest + 1 (35 → `36.sqm`). One
  file, additive only: never alter `mangas`, `chapters`, `history`, `categories`, `mangas_categories`.
  `.sq` files for `animes`, `episodes`, `anime_history` (with the `ifnull` guard trigger), `anime_categories`,
  `animes_categories`, `anime_sync`, `anime_sources`. Seed `anime_categories` row `_id 0` in the migration.
- Domain models + repositories + interactors (get/insert/update anime, episodes by anime, history upsert, anime
  categories CRUD + reorder, set anime categories, tracks), Injekt registration next to the manga ones.
- **Tests (must all pass):** (1) in-memory SQLite: create the DB at version 35 with fixture rows in
  mangas/chapters/history/categories, run the migration, assert every old row unchanged and new tables empty
  except `anime_categories` `_id 0`; (2) fresh-install schema equals migrated schema (compare `sqlite_master`);
  (3) repository round-trips; (4) history trigger with NULL `last_seen`. Write the rollback note (ANIME_SCHEMA §3.4)
  into Result.
- Backup proto: **not in this card** (A2.5 later).
- Claude: line-by-line review + kotlin-reviewer agent, then scratch-install upgrade test on the tablet (a second
  install of an older build, never the owner's apps), then merge (owner allowed if all pass).

**Result (Codex, 2026-11-07):** `tsuzuki/a2.2` @ `6323ef6bb`. **910 tests, 0 failures.** JDK 21, two per-file Spotless passes and `:app:assembleDebug :app:testDebugUnitTest` green. No device actions.
- `36.sqm` creates the seven anime tables, matching the new `animes.sq`, `episodes.sq`, `anime_history.sq`, `anime_categories.sq`, `animes_categories.sq`, `anime_sync.sq` and `anime_sources.sq`. It seeds only the protected anime default shelf. Episode counters and guarded history timestamps are maintained separately from books. The four season columns are stored, and the source/URL index and category links are unique.
- `AnimeDatabaseAdapters.kt` registers enum, memo, list and date columns. `AnimeRepositoryImpl.kt` handles identity and library queries. `EpisodeRepositoryImpl.kt` handles episode reads, inserts and progress. `AnimeHistoryRepositoryImpl.kt` handles accumulated watch seconds and one latest watch per anime. `AnimeCategoryRepositoryImpl.kt` handles shelves, order and transactional links. `AnimeTrackRepositoryImpl.kt` upserts stable tracker identities. `AnimeSourceRepositoryImpl.kt` persists stub source metadata.
- Domain `Anime.kt`, `Episode.kt`, `AnimeHistory.kt`, `AnimeCategory.kt`, `AnimeTrack.kt`, `StubAnimeSource.kt` and `LibraryAnime.kt` define separate anime records. Their repository contracts and `AnimeInteractors.kt`, `EpisodeInteractors.kt`, `AnimeHistoryInteractors.kt`, `AnimeCategoryInteractors.kt`, `AnimeTracks.kt` and `GetLibraryAnime.kt` expose the A2 operations.
- Upstream `AppModule.kt` wires the adapters and `DomainModule.kt` registers the repositories and interactors. `app/build.gradle.kts` and `gradle/libs.versions.toml` add a test-only SQLDelight JDBC driver. `AnimeDatabaseTest.kt` tests unchanged book fixtures, fresh/upgrade schema equality, repository round-trips, cascades and NULL history. Since the repo has no pre-anime schema snapshot, the tests reconstruct schema version 36 by removing only the seven new tables before applying migration 36 to reach version 37. A separate in-memory SQLite run compared all 80 schema entries.
- No backup proto, season UI, fansub exclusion or extension-store schema changes. Watch time remains in anime history. No Aniyomi implementation files were ported.
- Claude review gate: review migration line by line. In a scratch tablet profile only, copy and hash the old build's 12-file DB/WAL/SHM, prefs and WorkManager backup, install the migration build, compare all old book rows and schema, confirm only anime shelf 0 is seeded and run integrity/foreign-key checks. Verify the old novel/manga Home, Library, History and Updates still open. Also check fresh startup. For rollback, reinstall the pre-migration APK, restore the 12 backed-up files and verify hashes. SQLite does not support reopening the upgraded DB with the older app. Never run the upgrade or rollback drill on the owner's live data.

#### A2.3 Anime sources + browse catalogue (T3) — `review` (`tsuzuki/a2.3`)
**Claude 2026-11-07:** A1.5 + A2.2 are being merged into main by Claude (string conflicts resolved there); when the A2.2 card says `done`, start A2.3 from main (no prerequisite merge needed).
Register installed anime extensions as sources in an `AnimeSourceManager` (stub via `anime_sources` when the
extension is gone). Browse → a WATCH/ANIME media pill alongside novels/manga only when ≥ 1 anime extension is
installed (stays hidden otherwise; release users see nothing new). Catalogue screen: Popular / Latest / Search +
filters using the existing Tsuzuki browse grid components, cover → anime details stub (title, cover, description,
episode list from the source, PLAY first/next episode in the player). Boards: `docs/design/boards/png/10-anime/`
(BrowseAnime, AnimeDetails; Dot + Colour). Tests: source registration + stub fallback.

**Result:** `tsuzuki/a2.3` at `e5ab544`, based on current main with A1.5 and A2.2. Debug Browse shows the Watch
pill when trusted anime sources are installed. The catalogue supports Popular, Latest where supported, and
filtered Search. Its details stub shows source metadata and episodes, with first/next playback in the existing
debug player. Catalogue search and filters are retained when returning from details.

- `AnimeSourceManager.kt` registers installed sources and persists source stubs for use after uninstall.
- `TsuzukiAnimeBrowse.kt`, `TsuzukiBrowseHeader.kt`, and upstream `BrowseTab.kt` connect the debug Browse pages.
- `AnimeCatalogueActivity.kt`, `AnimeDetailsScreen.kt`, `AnimeFilterDialog.kt`, debug manifest and eight debug
  resource files implement the catalogue and details. Anime library and watch progress remain in A2.4.
- Source registration/uninstall tests and Browse gate/order tests pass. Per-file Spotless passed twice, debug
  built, and 957 app unit tests passed without failures or skips. The catalogue activity is registered in the
  debug manifest only. `docs/design/README.md` records status.
- Claude: on a debug device check the Watch pill before install, after trusting an extension, and after uninstall.
  Open its Popular/Latest/Search, apply a filter, open a result and return, then play the first and next episodes.
  Verify both Dot and Colour and spot-check both novel and manga Browse.

#### A2.4 Anime library + Watch tab + anime shelves (T3/T4) — `review`
**From the A2.2 review (fix in A2.4):** `AnimeCategoryRepositoryImpl.setForAnime` must not insert a link row for the default shelf `0` when other shelves are chosen; `AnimeRepositoryImpl.insert` returns the existing id for a duplicate (source, url) without updating fields — call `update` after it when adding to the library.
Boards: `fixes-2026-11/AnimeShelves` + `ColourAnimeShelves` (shelf management), `png/10-anime/AnimeLibrary`, `AnimeHome` (with Q5's nav).
Add to library from anime details (writes `animes.favorite`, anime categories picker using `anime_categories`).
Home's Watch tab becomes real: Continue watching (from `anime_history`), shelf row; Library gets the anime media
switch (board `LibraryMixed`, `AnimeLibrary`, `AnimeHome`) showing anime shelves only. Anime shelf management
screen reusing the Tsuzuki category components with the anime repository. Episode progress saved from the
player (position, seen at ≥ 85 %). Watch minutes recorded separately (not in `ReadingDays`). Boards in
`png/10-anime/`. Tests: progress threshold, continue-watching ordering, shelf CRUD.

**Claude 2026-11-08:** the earlier "Needs A2.3 first" result is superseded: `tsuzuki/a2.3` (`e5ab544`) now has the catalogue, details stub and player hooks. Start from `main`, merge `tsuzuki/a2.3` into the worktree, then do this card. Episode progress here is saved from the existing player; A3.2 replaces the player later.

Result (Codex 2026-11-08): `tsuzuki/a2.4` at `c52c5cffc`, includes A2.3.

- `AnimeLibraryStore.kt` saves anime, favorite/shelf assignments, episode state and watch history transactionally.
  The existing source/URL row is reused and updated. The read-only membership query and category repository
  keep Default implicit and prevent renaming it. No schema migration.
- `TsuzukiAnimeLibrary.kt` adds the anime library, shelf tabs and Watch content. `AnimeShelvesScreen.kt` manages
  anime shelves separately. Continue Watching picks the most recently played eligible favorite and advances
  to the next unseen episode in episode order. Airing and updates stay with their respective later cards.
- `AnimeDetailsScreen.kt` and `AnimeCatalogueActivity.kt` connect favorite actions, shelf selection and exact
  episode resume. `AnimeDebugPlayback.kt`, `AnimeDebugPlayerActivity.kt` and `AnimePlaybackProgress.kt` restore
  position, mark seen at 85%, and count played seconds separately from reading days. Incognito skips progress.
- Upstream hooks: `SettingsLibraryScreen.kt` adds the debug anime shelf entry, `NovelsTab.kt` adds the anime
  selector and `HomeScreen.kt` restores that selection. `AppModule.kt` registers the store.
- JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest` green, 960 tests and zero failures.
  The merged manifests contain the catalogue in debug and omit it in preview/release.
- Claude device check on Tab S11 and S22: in Dot and Colour, create/reorder/rename/delete a custom anime shelf,
  verify Default and book shelves, add/remove a favorite, play and resume from Watch, then finish past 85% and
  verify the next episode. Verify preview/release show the Read/Watch teaser with no anime library or Browse pill.

#### Anime lane order from 2026-11-08 (Claude) — detailed plan in WORKPLAN §6.3–6.5

(2026-11-08: the v0.1 freeze is over; Claude merges the chain into `main` after R-A5.) **Don't wait for main**: each
card starts a worktree from `main` and merges the card branch in its **Needs** column (that branch already contains the earlier chain). Gate every entry point with
`TsuzukiAnime.enabled` (A3.0; until then `BuildConfig.DEBUG`). Release/preview builds must show nothing anime.
Every card: board first (Dot then Colour), tokens + typography only, real data only, strings in all 8 locales,
tests named in the card, per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest`, Result + `review`.
Card format and verify commands: WORKPLAN §0.4 and the appendix skeleton.

| # | Card | Needs (merge this branch) | Tier | Effort |
|---|---|---|---|---|
| 1 | A2.4 Library + Watch tab + shelves | `tsuzuki/a2.3` | T3/T4 | high |
| 2 | A3.0 Anime out of the debug source set, one flag | `tsuzuki/a2.4` | T2 | medium |
| 3 | A3.1 Anime details + episodes | `tsuzuki/a3.0` | T3 | high |
| 4 | A3.2 Production player | `tsuzuki/a3.1` | T4 | xhigh |
| 5 | R-A2 Player settings + sheets | `tsuzuki/a3.2` | T3 | high |
| 6 | A3.3 Anime extension store | `tsuzuki/r-a2` | T4 | high |
| 7 | A3.4 Anime history | `tsuzuki/a3.3` | T2 | medium |
| 8 | R-A3 Updates + Airing today | `tsuzuki/a3.4` | T3 | high |
| 9 | A3.5 Anime trackers (AniList, MAL) | `tsuzuki/r-a3` | T4 | high |
| 9b | R-A5 Anime chain upstream-diff fixes | `tsuzuki/a3.5` | T3 | medium |
| 10 | A3.6 AniSkip | `tsuzuki/r-a5` | T3 | medium |
| 11 | R-A1 Anime backups | `tsuzuki/a3.6` | T4 | high |
| 12 | R-A4 Episode downloads | `tsuzuki/r-a1` | T4 | xhigh |
| 13 | A3.7 Watch-time stats | `tsuzuki/r-a4` | T2 | medium |
| 14 | A3.8 Anime global search | `tsuzuki/a3.7` | T2 | medium |
| 15 | A3.9 Continue-watching widget | `tsuzuki/a3.8` | T3 | medium |
| 16 | A4.1 Anime restyle parity pass | `tsuzuki/a3.9` | T2 | medium |
| 17 | A4.2 Release packaging for anime | `tsuzuki/a4.1` | T3 | high |
| 17b | A4.3a Anime user setting instead of the debug flag (Batch T) | `tsuzuki/a4.2` | T2 | medium |
| — | A4.3 / A4.4 device sweep, owner sign-off, migration upgrade test | review lane + owner | — | — |
| 18 | A5.1 Picture-in-picture | `tsuzuki/a4.3a` | T3 | medium |
| 19 | A5.2 Seasons grouping | `tsuzuki/a5.1` | T3 | high |
| 20 | A5.3 Fansub / group filter | `tsuzuki/a5.2` | T2 | medium |
| 21 | A5.4 Simkl tracker | `tsuzuki/a5.3` | T3 | high (blocked on owner) |
| 22 | A6.1 Kitsu tracker | `tsuzuki/a5.3` | T3 | high |
| 23 | A6.2 Migrate anime to another source | `tsuzuki/a6.1` | T3 | high |
| 24 | A6.3 Duplicate check on add | `tsuzuki/a6.2` | T2 | medium |
| 25 | A6.4 Preferred audio/subtitle languages | `tsuzuki/a6.3` | T2 | medium |
| 26 | A6.5 External player | `tsuzuki/a6.4` | T2 | medium |
| 27 | A6.6 Airing times from AniList | `tsuzuki/a6.5` | T3 | medium |
| 28 | A6.7 Player screenshot | `tsuzuki/a6.6` | T2 | low |
| 29 | A6.8 Local anime source | `tsuzuki/a6.7` | T3 | high |
| 30 | A6.9 Playback robustness pass | `tsuzuki/a6.8` | T3 | high |

**From 2026-11-12 rows 26–30 are superseded by the Batch AY order** (A6.4 → AY0 → AY1 → AY2 → AY3 → AY4 → AY6 → A6.5 → AY5 → A6.8 → AY7 → A6.6 → A6.7 → AY8; A6.9 dropped).

A linear chain keeps merges simple. A merge conflict in the Needs step → `git merge --abort`, set `blocked`
("needs Claude to merge"), next card. If the Needs branch is not `review`/`done` yet, the card waits (skip it).
Branch names for R-A cards: `tsuzuki/r-a2`, etc.

#### A3.0 Anime out of the debug source set, one flag (T2) — `review`
Goal: the anime catalogue, details and player stop being debug-only code so later cards build real features, while
normal users still see nothing. No UI change.
Files: move `app/src/debug/.../tsuzuki/anime/debug/{AnimeCatalogueActivity,AnimeDetailsScreen,AnimeFilterDialog,
AnimeDebugPlayerActivity,AnimeDebugPlayback}.kt` (and what they need) to `app/src/main/.../tsuzuki/anime/ui/`,
renamed without `Debug`; their manifest entries go to the main manifest, `exported=false`. Keep `AnimeDebugActivity`
(repo/APK install test screen) in `src/debug`. Add `tsuzuki/anime/TsuzukiAnime.kt` with `val enabled: Boolean`
(= `BuildConfig.DEBUG` for now; A4.3 turns it into a setting) and route every anime entry point through it: Browse
Watch pill, Home Watch tab content, Library anime switch, anime shelves (the Read/Watch switch stays a teaser, V3).
`anime-player` stays `debugImplementation`: put the player behind a small `AnimePlayerLauncher` interface in main,
the mpv implementation in `src/debug` (bound through a debug-only Injekt module), and a release no-op that reports
"unavailable". A4.2 changes this.
Tests: `TsuzukiAnimeGateTest` (flag false → Browse/Home/Library expose no anime destinations); a check that the
release merged manifest has no exported anime activity. `:app:assemblePreview` must also build.
Report: moved files, the gate points, preview APK size before/after (must stay within ±0.2 MB).

Result (Codex 2026-11-08): `tsuzuki/a3.0` at `55d5a5aee`, includes A2.4.

- Moved `AnimeCatalogueActivity.kt`, `AnimeDetailsScreen.kt`, `AnimeFilterDialog.kt`, playback request handling,
  and the player activity to `tsuzuki/anime/ui/` in main. `AnimeStreamResolver.kt` shares the extension API
  adapter with the debug installer. `AnimePlayerLauncher.kt` separates playback from the native backend.
  Debug injects mpv. Other variants inject an unavailable launcher with localized feedback.
- `TsuzukiAnime.enabled` gates the Browse Watch pill, real Home Watch content, the Library switch and saved
  routes, anime shelf settings, and both activities. The Watch teaser stays visible in preview and release.
  `AnimeDebugActivity` stays debug-only. Both shared activities are private, so playback starts from an
  in-app episode selection with a one-use token.
- Upstream hooks: `App.kt` imports the variant binding, `BrowseTab.kt`, `NovelsTab.kt` and
  `SettingsLibraryScreen.kt` read the flag, `app/build.gradle.kts` adds the fallback Kotlin source set,
  and the main/debug manifests declare the activities.
- JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest :app:assemblePreview` green.
  962 tests, zero failures. All seven anime activity declarations across the three merged manifests have
  `exported=false`. The five preview APKs contain neither mpv nor player native libraries.
- Preview APK size from A2.4 to A3.0: arm64 37,424,059 → 37,446,959 bytes and universal 77,257,922 →
  77,280,822 bytes. Every preview APK grew by 22,900 bytes, within the 0.2 MB budget.
- Claude device check on S22 and Tab S11: open a debug catalogue, filter it, play and resume a saved episode,
  then verify the preview Watch teaser and that Browse, Library and Settings omit anime destinations.

#### A3.1 Anime details + episodes (T3) — `review`
Board: `docs/design/boards/png/10-anime/AnimeDetails.png` + `dot/AnimeDetails.png` + `html/10-anime/AnimeDetails.html`.
Goal: replace the A2.3 details stub with the real anime details page, modelled on Tsuzuki's book details (Q6 header,
`TsuzukiTile`, `TsuzukiPillRow`) but for episodes.
Steps: (1) `AnimeDetailsScreenModel` loads the anime from `animes` (or the source when not in the library), refreshes
details + episodes from the source on open and on pull-to-refresh, and upserts `episodes` (new episodes get
`date_fetch`; removed ones are kept if seen or downloaded, Mihon-style). (2) Header: cover, title, studio/author,
status, source, genre pills, synopsis expand. (3) Episode list: number, name, date, group, seen state, progress bar
from `last_second_seen/total_seconds`, bookmark, filler mark only if the source sets it. Sort (number/date, asc/desc)
and filters (unseen, bookmarked), saved per anime. (4) Actions: PLAY EP n / RESUME EP n (first unseen or last in
progress), mark seen/unseen (single, multi-select, "previous seen"), bookmark, add/remove library (A2.4 picker), open
in WebView, share. (5) Season columns ignored (owner Q3).
Real data: no fillers, groups or ratings unless the source returns them. Tests: episode upsert/removal rule,
resume-target choice, sort/filter persistence. Claude checks: a real extension's anime, refresh, mark seen, resume.

Result (Codex 2026-11-08): `tsuzuki/a3.1` at `9ef03a1e9`, includes A3.0.

- `AnimeDetailsScreenModel.kt` loads stored metadata and refreshes source details and episodes on open or pull.
  Cached episodes remain usable when refresh fails. `AnimeLibraryStore.kt` updates source fields while preserving
  episode IDs and user state. Removed rows with seen state, bookmarks, progress or a supplied offline ID are
  retained. R-A4 will supply offline IDs from the anime downloader. The SQLDelight changes add queries only.
- `AnimeDetailsHeader.kt`, `AnimeDetailsScreen.kt`, and `AnimeEpisodeTiles.kt` implement the themed header,
  synopsis, genres, library and shelf actions, WebView and share, episode art where supplied, group/date/filler
  metadata, progress, and single or multiple episode watch/bookmark actions. The resume tile picks the latest
  in-progress episode or the first unseen episode. Season columns and later-card download/track actions are omitted.
- `AnimeEpisodeDisplay.kt` persists sort and filters per anime in reserved high bits of the episode flags.
  Source and repository changes are confined to anime files. No schema or migration changes.
- Per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest` green, 965 tests and zero failures.
  The database tests exercise refresh/removal, preserving user state, recent resume, and per-anime filter persistence.
- Claude device check on S22 and Tab S11 in Dot and Colour: open a real extension's title, pull to refresh,
  check details and thumbnail data, mark seen/unseen and bookmark singly and in a selection, reopen to verify
  saved sort and filters, then start and resume a partial episode. Try WebView and share from the overflow.

#### A3.2 Production player (T4) — `review`
Board: `png/10-anime/Player.png` + `dot/Player.png` + `html/10-anime/Player.html` (landscape 844×390).
Goal: one player that plays an episode from a real source the way Aniyomi users expect.
Steps: (1) Resolve videos through the anime source API in `source-api` (hosters → videos), pick the preferred quality
(pref, default highest), pass request headers + referer to mpv, fall back to the next video on load error with a
snackbar. (2) Controls overlay per board: title/episode, back, seek bar with buffered range, play/pause, ±10 s
double-tap, prev/next episode, lock, orientation lock, tap to hide (3 s). (3) Resume from
`episodes.last_second_seen`; save position every 10 s, on pause and on exit; mark seen at ≥ 85 % (reuse A2.4's
function); write `anime_history` and watch minutes. (4) Audio focus (pause on loss, duck on transient), pause in
background, keep screen on while playing, immersive mode, rotation without restarting playback (mpv in a retained
holder), process-death restore to the same episode + position. (5) Auto-play next episode pref (default on) with a
5 s countdown card at the end.
Files: `tsuzuki/anime/player/*` in main; the `anime-player` module only through `AnimePlayerLauncher` (A3.0). No GPL
code. Tests: video choice/fallback order, save cadence, seen threshold, next-episode choice. Claude checks: both
devices, rotation, background/foreground, audio focus with a notification sound, crash buffer, 16 KB.

Result (Codex 2026-11-08): `tsuzuki/a3.2` at `d41c05204`, includes A3.1.

- `AnimePlayerModel.kt` holds the native session across rotations and resolves each episode's stream queue.
  The player prefers the highest known resolution by default, remembers a chosen quality, and tries the
  remaining streams on failure. `MpvAnimePlayerLauncher.kt` passes headers through mpv and stays in debug.
- `AnimePlayerContent.kt` uses the Dot and Colour themes for the landscape overlay, buffered timeline, seeking,
  episode navigation, lock, orientation lock, stream fallback snackbar and five-second autoplay countdown.
  R-A2 owns the track, speed, decoder and gesture settings and sheets.
- The private player intent carries an episode ID so Android can restore the title and position after process
  death. The native surface reattaches without loading the current video again. Playback pauses in the
  background, observes media audio focus and saves progress at ten-second intervals and on pause/exit.
  The A2.4 85% threshold and separate watch history still apply. Incognito skips history writes.
- Changes stay in the anime packages and resources. Per-file Spotless twice,
  `:app:assembleDebug :app:testDebugUnitTest :app:compilePreviewKotlin` green, 969 tests and zero failures.
  The arm64 debug APK passed `zipalign -c -P 16 4` and all 11 packaged arm64 player libraries passed 16 KB ELF checks.
- Claude device check on S22 and Tab S11: use a real source with multiple streams, seek and reopen mid-episode,
  switch quality, rotate and background/foreground, test audio focus with a notification and another media app,
  restore an episode after process death, and check the countdown, controls lock and crash buffer in Dot and Colour.

#### A3.3 Anime extension store (T4) — `review`
Boards: `png/10-anime/BrowseAnime.png` (extension stores part) + `dot/BrowseAnime.png`; ExtensionRepos
(`docs/design/boards/INDEX.md`). If a screen's anime part is missing from its board: `blocked` ("needs a board").
Goal: install anime extensions from a repo without the debug screen.
Steps: (1) Owner decision Q6: a small new SQLDelight migration after 36 adds a media flag to the extension-repos table
(`novel | manga | anime`, existing rows keep their type). (2) Adding a repo accepts Aniyomi-format anime repos
(`index.min.json`), shown with an ANIME tag. (3) Extensions gets an ANIME group (only when ≥ 1 anime repo):
available / installed / updates; install through the existing installer path into the private anime dir
(`files/anime-exts`, A1.5 loader), reuse the signature trust prompt, uninstall, update. (4) `AnimeSourceManager`
reloads on change. No network in tests. Tests: repo parse (fixture JSON), the media-flag migration from 36,
install-state mapping. Claude checks with one extension from an owner-approved test repo: install, update,
uninstall → stub.

Result (Codex 2026-11-08): `tsuzuki/a3.3` at `127d60577`, includes R-A2.

- Migration `37.sqm` adds the media flag and classifies existing stores as manga or novel. The existing
  reader queries and backup path see the same book-store rows. Anime stores use separate queries.
- `AnimeExtensionReposScreen.kt` accepts Aniyomi-format HTTPS indexes and shows the anime format tag.
  `AnimeExtensionsTab.kt` shows available, installed and update groups when an anime repo is configured.
  Existing installed anime sources remain accessible if the last anime repo is removed.
- `AnimeExtensionInstaller.kt` shares validation and installation with the debug tool. The private installer
  validates the archive package, version and signature, stages updates in the private directory and restores
  the previous APK if loading the replacement fails. The shared trust prompt shows the anime signature.
  Install, trust and removal refresh the anime sources and retain source stubs for saved shows.
- Upstream hooks: `SettingsBrowseScreen.kt` opens the anime repository page, `BrowseTab.kt` attaches the
  anime Extensions group, and `ExtensionsScreen.kt` exposes the existing trust prompt with optional details.
- JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest :app:compilePreviewKotlin` green.
  976 tests, zero failures. The repo fixture, install-state mapping, Browse gate and schema upgrade run offline.
- Claude device check on S22 and Tab S11: add an owner-approved test repo, install and verify the signature
  prompt, open a source, update from a higher version, uninstall, and open a saved show to confirm its stub.
  Verify novel and manga repo listings remain available after upgrading an existing installation.

#### A3.4 Anime history (T2) — `review`
Board: History (`INDEX.md`, anime part; `10-anime/AnimeHome` continue-watching style). History gets an ANIME pill
beside novels/manga only when `anime_history` has rows. Row: cover, title, "EP n · 12:34 left", time; tap resumes in
the player; remove one / remove all for an anime; search. A separate repository query, no change to book history.
Tests: pill visibility, grouping by day, resume target.

Result (Codex 2026-11-08): `tsuzuki/a3.4` at `f399c7eeb`, includes A3.3.

- `TsuzukiAnimeHistory.kt` shows a dedicated anime filter when watch history has visible entries. It groups
  episodes by local day and filters by anime or episode name. Rows show the artwork, episode, time remaining
  or watched status, and the timestamp in Dot and Colour.
- Selecting a partial entry plays that episode. Selecting a finished entry starts the next unseen episode
  when present, otherwise it replays the selected episode. History removal hides one episode or all episodes
  of a show and keeps cumulative watch seconds and episode progress.
- Anime history uses separate SQLDelight queries. `HistoryScreen.kt` adds the pill and dispatches to the
  anime list. No database migration.
- JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest` green, 980 tests and zero failures.
  Tests cover the gate, local-day grouping, search, resume, and history removal with watch-time preservation.
- Claude device check on S22 and Tab S11: play two episodes, open History and choose Anime, verify date groups
  and search, resume the partial entry, remove one record, then remove the rest for a show. Check Dot and Colour.

#### A3.5 Anime trackers: AniList + MyAnimeList (T4) — `codex-working` (.worktrees/a3.5, 2026-11-08)
Board: `png/10-anime/AnimeTracking.png` + `dot/` + html. Goal: the existing AniList and MAL logins also track anime.
Steps: anime search/bind/update for AniList (GraphQL `type: ANIME`, progress = episodes) and MAL (`/anime`,
`num_watched_episodes`), stored in `anime_sync`; a tracking sheet on anime details reusing the Tsuzuki tracking UI;
on episode seen, raise progress only if the tracker's count is lower (Mihon rule); status, score, dates. Simkl left
out (no client exists). Never log tokens. Tests: request bodies against fixtures, the progress-only-increases rule,
`anime_sync` mapping. Claude checks with a real account only if the owner logs in.

Result (Codex 2026-11-08, in progress): implementation in .worktrees/a3.5 on tsuzuki/a3.5.
Anime tracking uses existing AniList and MyAnimeList clients, stores links in anime_sync, and adds
an optional background progress sync and a tracking sheet. Migration 38 stores the MAL ID returned
by AniList for A3.6. Debug Kotlin compiled before the final formatting pass. Offline build and tests
are running; see spotless-a3.5.log and build-a3.5.log in the worktree.

#### A3.6 AniSkip (T3) — `review` (.worktrees/a3.6, 2026-11-09; needs `tsuzuki/r-a5`)
Boards: `Player` (skip button) + `PlayerSettings` (AniSkip part). Call the AniSkip API
(`api.aniskip.com/v2/skip-times/<malId>/<ep>`) with the MAL id from an A3.5 binding (an AniList binding gives
`idMal`); no id → no button. A skip intro/ending/recap button for the segment's length; prefs: off / button /
auto-skip, intro offset. Cache per episode in memory. Tests: response parsing fixture, segment-at-position logic,
no id → disabled.

**Result (Codex, 2026-11-09):** `tsuzuki/a3.6` at `f4b6a0822` after clean Needs merge `90e708281`.

- `AniSkip.kt` looks up opening, ending, and recap intervals with the stored tracker MAL ID and a whole episode number. It validates and clips returned intervals, and caches successful or empty results in memory by MAL ID, episode and rounded playback duration.
- `AnimePlayerModel.kt` loads metadata only with AniSkip enabled and an eligible episode. Button mode seeks to the active segment's end. Auto mode honours the opening delay and skips each segment at most once per playback. Existing seek handling accounts for watch progress.
- `AnimePlayerContent.kt` adds the timeline markers and an active-segment pill that stays available with the chrome hidden. `AnimePlayerSettings.kt` and `AnimePlayerSettingsScreen.kt` provide Off by default, Show button, Auto-skip, and a 0–30 second opening delay. Eight labels are translated in all eight locales.
- `AppModule.kt` and `AnimePlayerActivity.kt` wire the lookup into the existing anime gate. `docs/design/README.md` records the status.

Verification: JDK 21, two per-file Spotless passes, `:app:assembleDebug :app:testDebugUnitTest` (1,019 tests, 0 failures, errors or skips), and locale placeholder validation. `AniSkipTest.kt` covers the response fixture, interval boundaries, opening delay, tracker IDs, no-ID network suppression, cache reuse and saved settings.

Device review: on the backed-up S22 Debug install, use an episode with a linked MAL or AniList binding and confirmed provider intervals. Check the pill in Dot and Colour with chrome shown and hidden, marker alignment, manual and automatic seeks, delay, replay into a skipped interval, and no button for an unlinked episode. Live provider playback awaits review.

RV3 checklist note: the R-A5 prerequisite already contains both unshipped migrations `37.sqm` and `38.sqm`. The latter provides tracker MAL IDs and is documented in `docs/UPSTREAM.md`. A3.6 adds zero schema changes.

#### A3.7 Watch-time stats (T2) — `review` (.worktrees/a3.7, 2026-11-09)
Board: Stats (`INDEX.md`, anime part). Owner Q4: watch minutes stay separate from the reading streak and goal. Stats
gets a WATCH section only when there is watch data: minutes this week/month/all time, episodes seen, anime completed.
Data comes from the watch-minute store A2.4/A3.2 write. Tests: aggregation by day/week, empty state hidden.

**Result (Codex, 2026-11-09):** `4d7a7a28a` on `tsuzuki/a3.7`.

- `anime_watch_days.sq` and migration `39.sqm` keep local-day watch seconds per episode. New tracked
  playback updates the day and the existing lifetime history in the same transaction. Existing and
  restored episode history still contributes to lifetime totals. Its older seconds lack a reliable
  day breakdown, so they are excluded from week and month.
- `AnimeHistoryRepositoryImpl.kt` totals the calendar week (Monday start), calendar month, lifetime
  history and actual watched episodes. An anime counts as completed if its source status is
  Completed and all its known episodes are watched.
- `TsuzukiWatchStats.kt` adds a gated Watch section only when there is watch data. Duration units,
  counts and five labels are localized. The reading chart, goal and streak use their existing data.

Verification: JDK 21, two clean per-file Spotless passes, `:app:assembleDebug :app:testDebugUnitTest`
(1,032 tests, zero failures, errors or skips), and matching keys in all eight locales. The database
tests cover a week crossing a month boundary, repeat checkpoints, empty data, episode state, history
clearing and migration from schema 39 without losing lifetime totals.

Device review: on a backed-up S22 Debug install, check Stats in Dot and Colour before and after
watching an episode. Compare week, month, lifetime and episode counts, check a finished anime and
confirm the reading goal did not advance. Preview should have no Watch section.

#### A3.8 Anime global search (T2) — `review` (.worktrees/a3.8, 2026-11-09)
Board: GlobalSearch (`INDEX.md`, anime part). An ANIME pill in global search when ≥ 1 anime source; searches
pinned/all anime sources in parallel (same limits as manga); results open A3.1. Tests: pill gate, source selection.

**Result (Codex, 2026-11-09):** `c65233360` on `tsuzuki/a3.8`.

- `TsuzukiAnimeGlobalSearch.kt` adds the Anime pill when the gate is enabled and at least one
  trusted anime source is installed. It searches all or pinned anime sources with at most five
  concurrent requests and keeps per-source results and elapsed time. Switching search inputs
  cancels the previous anime request.
- `TsuzukiAnimeBrowse.kt` adds persistent anime source pins. Selecting a result uses
  `AnimeLibraryStore.kt` to reuse or cache the entry, then opens the existing Anime Details
  route. Selecting the source opens its catalogue with the same query.
- The manga and novel search routes expose Anime alongside their existing media. They retain
  their separate result lists and extension-specific search behavior. The upstream UI changes
  are small slots in `GlobalSearchScreen.kt` and `GlobalSearchToolbar.kt`. Existing localized
  labels cover all eight required locales.

Verification: JDK 21, two clean scoped Spotless passes, and
`:app:assembleDebug :app:testDebugUnitTest` (1,036 tests, zero failures, errors or skips).
Fixtures cover the pill gate, pin selection and order, request concurrency and source failure
isolation, duplicate results, and preservation of saved details and extension metadata.

Device review: on a backed-up S22 Debug install, check Dot then Colour. With no trusted anime
extension, check the pill is absent. Install a fixture extension, pin and unpin its source in
Browse, try All, Pinned and With results, then open a title and a source catalogue. Check that
manga and novel searches still work and that Preview omits the Anime pill.

#### A3.9 Continue-watching widget (T3) — `review` (.worktrees/a3.9, 2026-11-09)
Board: Widgets (`INDEX.md`, anime part; R9's widget code is the pattern). Glance widget: the last 1–4 anime in
progress; tap → resume in the player (or details if the source is gone). Hidden from the widget picker when
`TsuzukiAnime.enabled` is false. Tests: item selection and order.

**Result (Codex, 2026-11-09):** `484ae89bd` on `tsuzuki/a3.9`.

- `TsuzukiWatchingWidget.kt` shows the newest in-progress episode per anime, with one to four
  titles depending on widget height. It reuses the Reading widget's cover and progress styling,
  and shows remaining time only when the episode duration is known.
- `AnimePlayerModel.kt` refreshes the widget after playback progress is saved on leaving the
  player. `TsuzukiWidgetTheme.kt` refreshes it on theme changes.
- Widget taps open the existing episode resume route. If the source is removed,
  `AnimeCatalogueActivity.kt` shows cached title details and the source message.
- The provider is registered in the Debug manifest while the anime gate is Debug-only. The
  user setting card will need to move provider registration behind the runtime gate.

Verification: JDK 21, two clean per-file Spotless passes, `:app:assembleDebug :app:testDebugUnitTest`
(1,038 tests, zero failures, errors or skips), and matching keys in all eight locales. Selection
tests cover newest first, duplicate anime, watched and untouched episodes, unknown durations,
and one to four rows. The merged Debug manifest includes the provider and Preview omits it.

Device review: on the backed-up S22 Debug build, add the widget and resize it in Dot and Colour.
Watch part of an episode, leave the player, and check progress and remaining time. Tap two
distinct rows and verify each resumes its own episode. Uninstall the disposable fixture source
and check the cached detail fallback. Check the Preview widget picker separately.

#### A4.1 Anime restyle parity pass (T2) — `review` (.worktrees/a4.1, 2026-11-09)
Follow the standard restyle rules (WORKPLAN appendix) for every screen on `png/10-anime/` (+ `dot/`, + `DotAnime.png`)
and `fixes-2026-11/AnimeShelves` / `ColourAnimeShelves`: spacing, type, case, pills, empty states; tablet layouts
(rail, two-pane details at ≥ 840 dp). Report a table: board → screen → what changed. No new features.

**Result (Codex, 2026-11-09):** `12ed776cd` on `tsuzuki/a4.1`. Scoped Spotless was clean,
`:app:assembleDebug` passed, and all 1,038 Debug unit tests passed.

| Board | Screen | Change |
|---|---|---|
| AnimeHome | Watch home | Landscape artwork, title and episode progress with remaining time when available. |
| AnimeLibrary | Watch shelf | Three columns on compact screens, responsive columns on wider screens, and an empty state for the selected shelf. |
| AnimeDetails | Anime detail | Overview and episode list side by side at 840 dp, with Favorite, Track, Save and External actions. Save uses existing episode selection. |
| BrowseAnime | Browse → Anime | Pinned and remaining source groups, with the existing Latest and pin actions. |
| PlayerSettings | Player → Settings | Icon cards for player preferences, with AniSkip and Autoplay highlighted. |
| Player | Episode player | Audio and subtitle pills reflect selected track languages, with scrolling on narrow layouts. |
| PlayerSheets | Player sheets | Bounded panel width on large displays, with responsive controls and clear selection states. |
| AnimeTracking | Tracking sheet | Tracker identity and status, with progress from the linked record. |
| AnimeShelves / ColourAnimeShelves | Manage anime shelves | Grouped rows, reorder handles, item counts, and a wider Add action. |

Watch, Library and Browse use the existing adaptive app navigation. The standalone detail route
keeps its own navigation. Board samples such as the New badge and unbacked schedule information
were omitted. Player sheets use the actions already supported by the player.

**Device review:** Compare the arm64 Debug APK in Dot and Colour on the S22 and tablet. Check the
tablet rail on Watch and Browse, and rotate to reach 840 dp for the detail split. Use a title with
artwork, multiple episodes and a linked tracker. Check Save eligibility, shelf reordering, and
selected tracks and player sheets in landscape and with larger text.

#### A4.2 Release packaging for anime (T3) — `review` (.worktrees/a4.2, 2026-11-10)
Goal: anime can ship in a release build (still behind `TsuzukiAnime.enabled`). Steps: `anime-player` becomes
`implementation` with native libs only in the arm64-v8a split (other ABI splits get the no-op launcher and a "not
supported on this device" message); R8 keep rules for `is.xyz.mpv.MPVLib` JNI and the `animesource` API extensions
call; the checksum task still runs; About → licences lists the LGPL libraries with the source offer from
`docs/licenses/anime-native/README.md`. Verify `assembleRelease` (unsigned is fine) and `assemblePreview`; report each
APK's size before/after and the 16 KB check (`zipalign -c -P 16 -v 4`). Tests: launcher selection by ABI.

**Result (Codex, 2026-11-10):** `70ddd61c4` on `tsuzuki/a4.2`. Debug, Release and Preview
assembled, all 1,040 Debug unit tests passed, and scoped Spotless was clean. The first Preview
baseline ran out of disk space. Its retry from the unchanged A4.1 commit passed.

- `app/build.gradle.kts` includes the anime player in all build types. The JNI payload is in
  `lib/arm64-v8a` in the ARM64 and universal APKs. The other splits contain none of the ten
  player-specific libraries. The TLS trust bundle and audio permission come from the player module.
- `AnimePlayerModule.kt` selects mpv for a 64-bit ARM64 process and the no-op launcher otherwise.
  `AnimePlayerModuleTest.kt` checks both selections, including a 32-bit process on ARM64.
  The unsupported-device message is translated in all eight locales. This card leaves the
  Debug feature gate in place for A4.3a.
- R8 keeps the mpv JNI bridge and the anime extension API. Both optimized mappings retain their
  names, and `:anime-player:verifyNativeChecksums` ran in all three builds.
- About → Open source licences includes FFmpeg, mpv, FriBidi and libplacebo. The APK bundles the
  LGPL text and links to the pinned corresponding-source and build notes.

**APK sizes (bytes):** all ten APKs below passed `zipalign -c -P 16 -v 4`. Debug ARM64 passed too.
Logs and raw CSVs are under `.worktrees/a4.2/app/build/`.

| ABI | Release before | Release after | Preview before | Preview after |
|---|---:|---:|---:|---:|
| arm64-v8a | 37,748,915 | 69,267,749 | 37,755,031 | 69,273,861 |
| armeabi-v7a | 34,733,237 | 34,908,676 | 34,739,353 | 34,914,788 |
| x86 | 39,070,813 | 39,246,252 | 39,076,929 | 39,252,364 |
| x86_64 | 40,094,582 | 40,270,021 | 40,100,698 | 40,276,133 |
| Universal | 77,582,778 | 109,101,612 | 77,588,894 | 109,107,724 |

**Device review:** Check the licence sheet and, after A4.3a, play on ARM64 Preview to exercise
the optimized JNI and extension API. Other ABIs were verified by unit tests and APK inspection.

### Wave Y (owner approved 2026-11-06; after the anime lane)

Same general rule as wave X: check first whether it already exists; if done, set `done` with where.

#### Y1 Mihon skipped fixes, top group (T3) — `done`
`docs/UPSTREAM.md` "Skipped for conflicts": take the most valuable group (downloads, tablet FAB, range-selection
crash, WebView) that is still missing. Port each fix with the smallest diff, cite the Mihon commit hash per fix in
Result, skip any that conflicts with Tsuzuki hooks and say why. Tests where Mihon had them.

**Result (Codex, 2026-11-07):** Already on main. `7edc18c05` covers Mihon `88d4071cc` (startup cache rescan), `f28b2d887` (cancel removed-chapter queue entries), and `f82a7a68f` (fresh notification builders, with Tsuzuki's per-chapter error suppression retained). `DownloadProvider.kt` and its existing `DownloadProviderTest.kt` cover `f0f1e4de5` and `a5d584eeb`. The speed picks `730e43fd1`, `f02bf5ba7`, `9a6dea804`, and `fb8e640d6` are recorded in UPSTREAM's order 8 hand-port log. `2135d3852` already ports `c279291e2` (tablet FAB) and `a6ee4bc59` (range selection). The WebView group `649fc83ff`, `5a5ff22c3`, `5f63c983d`, `7ae858e60` is documented in the 2026-11-04 WebView entry. The remaining `ba16158b1` requires Mihon's subscriber gate and download-state refactor and removes row progress used by Tsuzuki. Kept that documented skip. No new code or build required for this audit.

#### Y2 Tablet manga two-page spread (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
**Claude review 2026-11-07 — rework on `tsuzuki/y2` (keep your commit, add a fix commit):** the pairing logic is
good, but `PagerViewer.kt` (+99/−30) rewrites Mihon's `onPageChange` forward/preload block and long-tap handling for
every pager reader, and pulls `UiPreferences` via Injekt into the viewer. That changes behaviour for all manga
reading and makes every Mihon/Tsundoku merge of `PagerViewer` conflict. Move the spread logic behind a small
Tsuzuki helper (e.g. `tsuzuki/reader/MangaSpreadController` owning `updateSpreadLayout`, page-shape bookkeeping and
the `MangaSpread` → `ReaderPage` mapping); `PagerViewer` keeps the original `onPageChange` block untouched when the
spread is inactive and gets ≤ ~15 lines of one-line calls. Pass "themed" in from the activity instead of reading
`UiPreferences` in the viewer. Same for `PagerViewerAdapter` (keep its upstream code paths intact when inactive).
Board `png/11-tablet/TabletReader.png` / `TabletMangaSpread` (+ `dot/`). Paged manga reader on landscape windows
≥ 900 dp: optional two-page spread (setting in the manga reader settings, default off), first page single,
wide pages shown alone, RTL order follows reading direction. Hook in the viewer with a small Tsuzuki helper.
Tests: page-pairing logic (odd counts, wide pages, RTL).

**Result (Codex, 2026-11-07):** `tsuzuki/y2` @ `80173711f`. **910 tests, 0 failures.** JDK 21, per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- `MangaSpread.kt` groups adjacent portrait pages in reading order, with first, wide and unpaired pages kept single and no cross-chapter pairs. `MangaSpreadHolder.kt` reuses two paged image holders and places the earlier page on the right for RTL. `MangaSpreadTest.kt` covers first page, odd tails, wide/unknown pages, chapter boundaries and RTL.
- Upstream `ReaderPreferences.kt`, `ReadingModePage.kt` and `PagerConfig.kt` wire the default-off setting. `PagerPageHolder.kt`, `PagerViewerAdapter.kt` and `PagerViewer.kt` classify loaded images, apply pairs when idle and retain the selected original page through layout changes. Long press uses the touched side.
- Upstream `ReaderActivity.kt`, `ReaderViewModel.kt` and `ReaderPageIndicator.kt` pass the original-page range to `TsuzukiMangaReaderChrome.kt`. Resume, seek and completion use real page indices. The eight locale files add the toggle label.
- Pairing starts once both dimensions are known. The mode applies to Dot and Colour on landscape paged manga at 900 dp or wider. In this mode, the old rotate/split treatment is suspended so wide scans stay intact. The board's spread is wired into the existing reader chrome.
- Claude device checks: on the tablet in both themes, confirm default off, enable in Reading mode and open a chapter with portrait pages, an odd tail and a wide scan. Check first/solo pages, RTL and LTR visual order, long-press each half, scrubbing/resume into either member, turn across chapters and completion when the last page is paired. Rotate or resize below 900 dp, return to landscape, and switch off while on a pair. On the phone verify the setting leaves single pages. Device verification pending.

**Rework Result (Codex, 2026-11-07):** Verified working diff in `.worktrees/y2` (`tsuzuki/y2`, base `80173711f`). Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` passed (910 tests, 0 failures). Automatic approval rejected the card-branch commit because AGENTS.md says Codex never commits. Claude can review and commit the working diff.
- `MangaSpreadController.kt` owns layout, image shape tracking, pair mapping, touch-side lookup and holder dispatch.
- Upstream `ReaderActivity.kt` supplies the app theme gate. Upstream `PagerViewer.kt` retains its ordinary page-change block and delegates spread cases. Upstream `PagerViewerAdapter.kt` retains the ordinary grouping and lookup paths when the mode is off.
- Device review: repeat the checks above, including landscape to portrait while on a spread and stock long tap, splitting and chapter preloading with the setting off.


#### Y3 Manga details + Browse manga boards (T2/T3) — `done` (Claude reviewed, merged into main 2026-11-07 via `review/night4` → `c7c805cf2`) (needs Q6)
Boards `png/09-manga/MangaDetails.png`, `BrowseManga.png` (+ `dot/`): scanlator filter entry, auto-webtoon hint,
manga-specific tiles; compare with what N5 built and fix only differences (list them).

**Result (Codex, 2026-11-07):** `tsuzuki/y3` @ `8dc76777d`. **909 tests, 0 failures.** JDK 21, per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- `TsuzukiMangaDetails.kt` adds the effective Webtoon hint and a tappable reading-mode tile that opens the existing themed grid. It uses the per-series flag or global default, without making an image-detection claim. The scanlator filter entry, count, and multi-select exclusions from N5 already match the requested behavior.
- Upstream details hooks in `presentation/manga/MangaScreen.kt`, `ui/manga/MangaScreen.kt`, and `MangaViewModel.kt` pass the choice to the existing `SetMangaViewerFlags` interactor. Novel and stock details still use their existing routes.
- `TsuzukiBrowseManga.kt` places Local manga alongside pinned sources without changing pin state and adds a manga repo tile. `TsuzukiBrowse.kt` renders the real local formats, while upstream `SourcesScreen.kt` exposes language under manga source names. `SourcesTab.kt` opens the existing repo manager and `BrowseTab.kt` switches to the existing Manga Extensions tab with its real update count.
- `MangaSourceTilesTest.kt` covers local promotion, ordering and an absent local source. Eight locale files add the labels and help copy. The board's fabricated genres and source totals are omitted, and the separate anime selector remains in the anime lane. The repo button opens the manager where the existing Add action lives.
- Claude device checks: in Dot and Colour on phone and tablet, use an entry with several scanlators, open/apply/cancel the exclusions, and switch the entry between Webtoon, RTL and Default from the tile. Open a chapter to confirm the saved mode. In Browse Manga, verify Local opens the local catalogue, real pins remain removable, remote rows show language and supported Latest actions, and both repo and extension shortcuts open the intended screens. Repeat Browse Novels to check its layout. Device verification pending.

#### Y4 Synthetic 500-book backup generator (T2) — `done` (Claude reviewed, merged into main 2026-11-07 via `review/night4` → `c7c805cf2`)
WORKPLAN P2.2: a script in `scripts/` (Kotlin script or Python, no app code) that writes a valid Tsuzuki
`.tachibk` with 500 books (novels + manga mix), 30 shelves, 50 k chapters, history, using the backup proto in
the repo. Fake titles clearly marked `[synthetic]`; no real source data. Claude restores it only into a scratch
install. Test: the script output parses with the app's backup decoder (unit test that decodes a small generated file).

**Result (Codex, 2026-11-07):** `tsuzuki/y4` @ `6b25f922b`. **907 tests, 0 failures.** JDK 21, per-file Spotless hooks twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- `scripts/generate_synthetic_backup.py` writes the existing gzip/protobuf format with the Python standard library. Titles and stub names are marked `[synthetic]`, all identifiers are fabricated, and output requires a new file. Counts and the reference timestamp can be overridden for reproducible smaller fixtures.
- `synthetic-library-small.tachibk` is the deterministic fixture with four books, two shelves and 20 chapters. `SyntheticBackupTest.kt` decodes it through the app's `BackupDecoder` and checks media mix, source and shelf links, original chapter numbering, resume progress and history. No production app files changed.
- The default run wrote `.worktrees/y4/build/synthetic-library.tachibk` (generated, ignored). Independent wire counts confirmed 250 novels, 250 manga, 30 shelves, 50,000 chapters, 995 history records and two stub sources. Recreate from the card checkout with `python scripts/generate_synthetic_backup.py build/synthetic-library.tachibk` using a new destination if the file already exists.
- Claude scratch check: restore into an empty scratch preview profile, select both manga and novels plus history, confirm totals, shelf names, History and Continue entries, and exercise the Home and Library grids. The generator omits covers and real source content, so it measures the restored library data and layout. No restore or device action performed.

#### Y5 Restyle checks: Tracking, Quotes, ExportEpub, Upcoming, EditNovel (T2) — `done` (Claude reviewed, merged into main 2026-11-07 via `review/night4` → `c7c805cf2`) (needs Q6)
Boards in `docs/design/boards/INDEX.md` (05-novel, 08-tools). For each: Tsuzuki version exists? compare to board,
fix differences; stock → restyle with a dispatch hook. List per screen in Result.

**Result (Codex, 2026-11-07):** `tsuzuki/y5` @ `0a13db39f`. **908 tests, 0 failures.** JDK 21, per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- Tracking: `TsuzukiTrackInfoDialogHome.kt` adds the chapter auto-sync hint when a tracker is linked and the real auto-update preference is enabled. Existing cards, metric edits, add/search, private and date controls already cover the board's backed actions.
- Quotes: `QuotesSheet.kt` uses themed heading typography and localized action descriptions. New `TsuzukiQuotesExport.kt` adds the actual count and saves the current novel's quotes to a UTF-8 text file through the document picker. `QuoteTileMetadataTest.kt` checks order, Unicode and chapter attribution. The existing quote tile covers per-quote share/delete. No cross-novel selector was added because this sheet is backed by the current novel.
- Export EPUB: upstream `BatchExportEpubDialog.kt` dispatches Dot/Colour to the form shared with single-book export in `TsuzukiExportEpubDialog.kt`. It shows the real selected count and titles and keeps existing options/defaults and the EPUB/ZIP output contract. `EpubExportContainerTest.kt` checks the filename follows the actual container. Multi-novel output stays one ZIP and the form omits unsupported separate-file and compression choices.
- Upcoming: `TsuzukiUpcomingScreen.kt` already has the board's month controls, actual marked dates and selected-day cover list. Verified by comparison. No predicted chapter number was invented.
- Edit Novel: `TsuzukiEditMangaDialog.kt` already has the cover picker/reset, themed fields, status pills and Save. Verified by comparison. The board's manual book palette has no saved per-book setting in this editor. Eight locale files add the three new labels.
- Claude device checks: in Dot and Colour, open Tracking with auto-sync on/off and exercise linked/unlinked trackers, then export several disposable quotes and inspect UTF-8 text and chapter order. Open one- and multi-selection EPUB forms and verify cancel, picker MIME and filename for original, Both and split volumes. Check Upcoming month/day/filter interactions and Edit Info cover, status and Save on a disposable book. Device verification pending.

#### Y6 Tablet TTS panel + foldable (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
**Claude review 2026-11-07 — rework on `tsuzuki/y6`:** X1 just moved Tsuzuki code out of `ReaderActivity`; Y6 adds
~50 lines of listen-panel composition back into `ReaderActivity.setContent` (fold detection, listening state,
the whole `TsuzukiReaderListenPanel` lambda). Move that into one Tsuzuki composable / hook in
`presentation/tsuzuki/reader/` (e.g. `rememberTsuzukiListenPanel(state, viewer, readerPreferences, actions)` returning
`fold`, `listenContent`, `ttsSessionActive`, `ttsPaused`), with the activity passing the existing TTS functions as
lambdas. `ReaderActivity` should end up with a few one-line calls, as after X1. `TtsController.kt` (+40) is fine if
it only exposes state; keep it read-only.
Boards `png/11-tablet/TabletTTS.png`, `Foldable.png` (+ `dot/`). Tablet novel reader with TTS active: the N25
side panel shows the listen controls (play/pause, speed, voice, sleep timer, paragraph list) using existing
`TtsController` state; foldable half-open posture keeps text on top, controls on the bottom half. Tests: panel
state mapping.

**Result (Codex, 2026-11-07):** `tsuzuki/y6` @ `b0bc2463e`. **910 tests, 0 failures.** JDK 21, per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- `ReaderListenLayout.kt` reads Jetpack WindowManager posture and maps the existing controller's paragraph position to panel progress. `TsuzukiReaderPanel.kt` and `TsuzukiReaderHooks.kt` show Listen in the N25 dock, preserve Contents/Quotes/Notes, reserve reader space and move the panel below a horizontal half-open hinge. The measured hinge gap stays clear. Flat and vertical folds retain the normal reader layout.
- `TsuzukiReaderListenPanel.kt` shows the real cover, playing chapter when available, current voice, speed, paragraph progress, play/pause and paragraph transport. It lists the controller's actual spoken paragraphs for direct seek. Voice/speed selection reuses reader preferences and reapplies settings at the active paragraph when speaking.
- `TtsSleepTimer.kt` defines Off, end of chapter, and 15/30/60-minute choices. Upstream `TtsController.kt` owns the one-shot timer and short paragraph previews. A timed expiry stops playback and the chapter-end choice suppresses auto-advance. Timer state is scoped to the existing playback controller and clears on stop or destruction.
- Upstream `NovelViewer.kt` and `NovelWebViewViewer.kt` expose their existing controller internally. `ReaderActivity.kt` and `NovelReaderAppBars.kt` pass the active controls through the reader panel and reserve its measured space for the reader chrome. `app/build.gradle.kts` and `gradle/libs.versions.toml` declare AndroidX WindowManager 1.5.0. Eight locale files add timer and paragraph labels.
- `TsuzukiReaderPanelTest.kt` covers fold selection, hinge gap and clamped paragraph progress. `TtsSleepTimerTest.kt` tests replacing a timer and stopping once at expiry. No sample voice, offline badge or fake chapter data was added. The card's horizontal half-open instruction takes precedence over the board's vertical hinge illustration.
- Claude device checks: in Dot and Colour, start TTS on the tablet in each novel renderer, verify text and dock coexist, then pause/resume, change speed/voice and seek to a visible paragraph. Verify the close action keeps playback running and Listen reopens the panel. Check Contents/Quotes/Notes, status and menu bars with the dock open. Try timer replacement and end-of-chapter with auto-advance enabled. On a half-open foldable or emulator, verify text stays above the physical hinge, controls below, and flattening restores the ordinary layout without losing playback. Device verification pending.

**Rework Result (Codex, 2026-11-07):** Verified working diff in `.worktrees/y6` (`tsuzuki/y6`, base `b0bc2463e`). Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` passed (910 tests, 0 failures). Card-branch commits require owner or Claude after automatic approval rejected the Y2 commit under AGENTS.md.
- `ReaderListenLayout.kt` owns renderer selection, fold detection, dock eligibility, listening state and panel composition. Upstream `ReaderActivity.kt` binds the hook and passes the existing pause, resume and paragraph actions. The rework keeps the layout and controller behavior from the original card.
- Device review: repeat the checks above, including phone mini-controls, both novel renderers and a horizontal half-open hinge.


#### Q13 Novel next chapter: instant, never a blank page (T3) — `done` (Claude `54717865f`: warm-up past 75 %, Next ~0.1 s instead of ~1.2 s blank on the S22; also fixed the stale 100 % on chapter entry)
Owner 2026-11-07: "sometimes the novel reader lags before going to the next chapter." Measured on the S22
(preview, Royal Road, TextView, infinite scroll off): tapping › Next shows a **blank black page for ~1.2 s**
(`app/build/night3-evidence/next.mp4`, `next-strip.png`) while the chapter text is fetched from the network.
`NovelViewer` already prefetches the next chapter, but only for infinite scroll and TTS
(`preFetchNextChapterForTts`, `prepareNextChapterForInfiniteScroll`); the button path loads on demand.
- Prefetch the next chapter's text (network or disk) when the reader passes ~75 % of the current chapter or the
  chapter-end card appears, for TextView and WebView, infinite scroll off as well as on. Reuse the existing
  prefetch helpers; one in-flight prefetch at a time; cancel on chapter change; never download (no files).
- Next uses the prefetched text when present (no network wait). If it isn't ready, keep the current page visible
  with a small themed loading state (Tsuzuki progress dots in the bottom bar / end card) instead of a blank page,
  and show a retry message on failure (no silent swallow).
- Respect data saver / Wi-Fi-only download prefs if they exist for reading; no prefetch while TTS already does it.
- Tests: prefetch trigger threshold + cancellation logic as pure helpers. Device check (Claude): Next is instant
  after scrolling past 75 %, loading dots when tapping early, airplane mode → retry message.
Hooks in `NovelViewer.kt` / `NovelWebViewViewer.kt` stay small (call into `tsuzuki/reader/`).
Result: _pending_

#### Q14 Design fixes from Claude's 2026-11-07 device pass (T2) — `done` (Claude `aa9d5544a`: items 1–5 fixed and device-checked; item 6 tablet margins deferred → owner question)
Boards: `fixes-2026-11/` (ReaderQuick, ReaderAll, Q9Selection) and `INDEX.md` SettingsTranslation. Evidence:
`app/build/night3-evidence/` (`reader-quick.png`, `reader-all.png`, `q9-selection.png`, `x3-translation.png`).
Fix each, Dot then Colour, at 360 dp and font scale 1.3:
1. Quick reader sheet: the **LITERATA** font chip text touches both chip edges (chip too narrow) — size chips to
   content or shrink the label; no clipping.
2. All reader settings: the five tabs wrap (**MORE** drops to a second line) — one row (equal-width segment or
   horizontally fitted pills per board `ReaderAll`); title "ALL READER SETTINGS" wraps to two lines — one line
   with ellipsis or a shorter title ("READER SETTINGS").
3. All reader settings › Fine-tune: **paragraph spacing shows `5`** (it is 0.5 em ×10) and indent `0` without a unit
   — show `0.5 em` / `0 em` with the shared unit formatter (Q11's `1.6×` style).
4. Library selection (board `Q9Selection`): selected covers get the **accent outline + check badge**, unselected
   covers dim to ~55 % (now: dark red tinted card, no dimming); action labels white (now grey); `MARK AS READ`
   wraps — use the short label `MARK READ` (8 locales) and one line.
5. Settings › Translation (Dot): slider rows ("Translation chunk size", "Delay between requests", "Request
   timeout", "Max parallel translations") are not uppercased like the other rows; the GENERAL tile's bottom
   corners are square (the slider row breaks the grouped-tile rounding).
6. Tablet novel reader: left text margin is only ~6 dp (looks cramped at the edge) — check the default margins for
   wide windows against board `TabletNovel` and use the board's side margin when the user hasn't changed it.
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

- 2026-11-13: board 5 `Voices` (`docs/design/boards/fixes-2026-11b/`) is approved as drawn.
- 2026-11-12: the offline natural voice (neural TTS) ships **in 0.1.1** if NT2 passes (lexicon licence, speed, the owner's listening test); otherwise 0.1.1 ships without it.
- 2026-11-12: next release is 0.1.1 without anime; the anime lane is paused until its tag; Codex does the reviews; boards 2026-11b (`docs/design/boards/fixes-2026-11b/`) are approved as drawn; anime-lane implementers may use adb when the lane resumes.

- 2026-11-08: **v0.1 freeze lifted** after the v0.1.0 release. Codex: two lanes (anime, app), cards in Batch S.

- 2026-11-08: **Novel infinite scroll is on by default** (`ReaderPreferences.novelInfiniteScroll`; a saved
  choice is kept). Smoother chapter-end Next chapter → V7.7 in 0.1.1.
- 2026-11-08: **Dot is the default app theme** for new installs, so onboarding starts in Dot (`UiPreferences`
  default; a stored Colour choice and legacy themes → Colour are unchanged).

- 2026-11-07: run Batch R as **two Codex threads in parallel** (lane 1 anime, lane 2 app); tablet novel text
  **capped at ~600 dp, centred** (R17); **no cloud sync** for now (local backups only, R15); dictionary =
  **offline downloadable packs** (WordNet EN, JMdict JA; R13), not bundled in the APK.

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

- **2026-11-14 Codex independent review lane — RV5 Case B completed.** Accepted S2-F, RP1, STR1, ERR1 and TTS-LANGUAGE; review fixes `0a96ace3d` (first-line/dock clearance), `bf5055dcd` (CSV title), `8570ff5d7` (reader loading/progress/held chapter-end state). Debug + optimized Preview + 1,085 tests pass; 16 KB APK alignment pass, both crash buffers empty. Tablet/S22 scratch-only Dot/Colour QA; archive `app/build/review-evidence/rv5/`. External/CDN and unforced matrix limits are explicit in ERR1 verdict. Main fast-forward next; no push.

- **2026-11-14 — NT1-BINARY candidate ready for independent review:** `d1e360ff4` + `d580d500e`; all official Android variants fail the unchanged literal gate, while an isolated arm64 Kokoro-only source profile passes raw strings, 16 KB ELF alignment and TTS JNI API checks. Candidate 7,857,879 B, `b2cee72b9131cf5190753c966e23e0c0e65ef7a0e9313e434d074d4aa3b5fa47`; 13 Python tests and offline recipe pass. Evidence `.worktrees/nt1-binary/app/build/nt1-binary-evidence/`. No model, app integration, install, merge or push. RV5 is now ready for an independent chat.


- **2026-11-14 — ERR1 implementation ready for independent review:** `ad3e6a871` adds actionable mapped errors, redacted diagnostics and delayed loading stages in catalogue/search/details/readers/download rows. Debug APK and 1,039 tests pass, 23 keys verified in eight locales; evidence `.worktrees/err1/app/build/err1-evidence/`. No device verdict or merge. Owner reports USB tablet + wireless S22 available and asks to investigate a binary satisfying NT1; gates remain in force.


- **2026-11-13 RV4 (Codex review lane).** Accepted V7.7-F and CHAPNUM; MAIN-REPOS owner policy committed `7f697fef9`, plan/boards `3e610bd08`; Add Repository review fix `7f51d31da`. Accepted Debug/Preview builds and **1,011 tests** pass; tablet Dot/Colour reader, mismatch-label and fresh-repository checks; crash buffer empty. R12-F/R14/S4 deferred and excluded (`89e1ac9ac`/`451168815`/`911a2222f`), TTS-LANGUAGE skipped dirty. No model downloaded or extension trusted. Evidence 225 files archived `app/build/review-evidence/rv4/`; scratch apps removed, daily tablet on Dot Home; S22 deferred. RV4-01 Native bottom-card overlap added to RP1. No push.

- **2026-11-13 RV2 (Codex review lane).** Accepted R15, S3/S3-F and R13 after fixes `005713576`, `ea748689e`, `bb82bd9f1`, `286d18b7b` plus the dictionary update regression. Candidate Debug and scratch Preview built; final suite **1,006 tests, 0 failures/errors/skips**, per-file Spotless twice; browser recap 3/3 sizes; converter 2/2. Tablet scratch QA passed backup export/restore and invalid/settings-only inputs, cold recap in all three reader modes, paged resume, and no-pack management; crash buffer empty. S2 excluded (`591429e49`) for the documented over-count; S1 excluded (`dab21422c`) pending explicit trust approval for a live source. S22 deferred by owner. Evidence archived at `app/build/review-evidence/rv2/`; daily Preview data untouched. No push. Next RV4, then this chat implements app cards for a different reviewer.

- **2026-11-10 A6.3 (Codex).** The anime duplicate check is in review (`6ef424d63`). Debug
  assembled and all 1,071 unit tests passed. Android dialog and migration checks remain.

- **2026-11-10 A6.2 (Codex).** Anime source migration is in review (`152f8b944`). Debug
  assembled and all 1,067 unit tests passed. Android navigation and two-source checks remain.

- **2026-11-10 A6.1 (Codex).** Kitsu anime tracking is in review (`9e17de25e`). Debug assembled,
  all 1,063 unit tests passed, and unauthenticated anime search worked against Kitsu. Authenticated
  verification remains in the device review lane.

- **2026-11-10 A5.3 (Codex).** Subtitle-group exclusions are in review (`ea92e39eb`). Debug
  assembled and all 1,058 unit tests passed. Android filter-sheet and playback checks remain.

- **2026-11-10 A5.2 (Codex).** Season pills and library grouping are in review (`b9766a725`).
  Debug assembled and all 1,054 unit tests passed. Source and import fixtures still need device review.

- **2026-11-10 A5.1 (Codex).** Anime PiP is in review (`098e584f0`). Debug assembled and
  all 1,049 unit tests passed. Home, Recents and window-close behavior await Android review.

- **2026-11-10 S3-F (Codex).** Paged WebView resume fix ready for RV2 (`c3029ea69`). Debug
  assembled, 973 unit tests passed, and Chromium verified the same story lines at three viewport
  sizes. Android verification stays with RV2 alongside Claude's local-source and hard-wrap changes.

- **2026-11-10 RV2 paged recap (Codex QA).** The browser fixture reproduced RV2-12. It also verified that the recap renders and Dismiss calls the bridge once. Synced the five implementation results from the RV2 worktree and queued follow-ups for R15, S3 and R13. S1 and S2 remain in device review.

- **2026-11-10 RV2 (Codex QA).** Logged RV2-01 through RV2-11 with reproduction steps and evidence. Tested the backup flows, Manhwa filter, recap, listening stats and no-pack dictionary across the isolated Preview installs on Tab S11 and wireless S22. All 1,007 Debug unit tests passed. Removed both scratch installs and left RV2 in review.

- **2026-11-10 A4.2 (Codex).** Release packaging (`70ddd61c4`) is in review. Debug, Release and Preview built, 1,040 tests passed, and all ten optimized APKs passed 16 KB ZIP alignment.

- **2026-11-09 A4.1 (Codex).** Anime restyle parity implementation (`12ed776cd`) is in review. Debug assembly and 1,038 tests passed. Dot and Colour device checks are pending.

- **2026-11-08 RV1 (Codex review).** Passed V7.6, R16 and R17. V7.7’s WebView tap exposed reader controls and R12 missed native Asura Scans traffic, so both were reverted and queued for fixes. Accepted Preview assembled and 977 unit tests passed. Dot tablet and Colour S22 launched the accepted Preview. Screenshots, frames and the local Earshot report are in `app/build/review-evidence/rv1/`.

- **2026-11-08 Claude — Batch T: Codex reviews Codex.** Owner: Claude is out of usage; a Codex review thread
  may merge into `main` locally (no push) and use the tablet + S22. Wrote RV1 (V7.6, V7.7, R12, R16, R17), RV2
  (R15, S1, S2, S3, R13), RV3 (anime chain on `tsuzuki/r-a5`) and anime cards A4.3a, A6.1–A6.9; A5.1 now
  needs A4.3a. AGENTS.md and WORKPLAN §0.3 updated for the review lane.

- **2026-11-08 10:35 Claude — post-v0.1 merges: R11, HOME-SWIPE, R8, R10 into `main` via `review/post-v01`.**
  String files union-merged. R10 had removed the tracker token dialog's show/hide toggle and its pre-filled saved
  token (not in the card); Claude restored upstream's dialog, leaving only the theme dispatch in
  `SettingsTrackingScreen.kt`. `assembleDebug` + **968 tests, 0 failures**; `assemblePreview` installed on the S22
  (tablet disconnected): Home cover stack swipes to the next book (HOME-SWIPE); "next 5 chapters" of Mother of
  Learning downloaded through R8's scheduler (5 complete zips, no `_tmp` left; R8's pause/resume-while-in-flight
  checklist still to do on the tablet); Library settings show the four smart-update switches separately (R10);
  crash buffer empty. Also: signing key alias moved out of secrets (logs no longer show `***`).

- **2026-11-08 09:10 Claude — RC3 bug: chapter-end NEXT CHAPTER did nothing (owner screen recording).**
  Text reader (`NovelViewer`) only. After a fling into the chapter end, `NestedScrollView` still had an
  unsettled overscroll stretch, so each new touch "caught" it and the scroll view took the gesture: the
  card's Compose buttons never clicked, and the tap fell through to the menu toggle. Fix: finish the edge
  effects on ACTION_DOWN before the intercept check, and skip the menu toggle for taps that start on the
  card (`tsuzuki/reader/NovelChapterEndTouch.kt`; 17-line hook in `NovelViewer.kt`). Tablet `.dev`,
  Mother of Learning: before the fix, fling-to-end then tap → 0/2 loads; after → 3/3 loads, no menu flash,
  text taps still toggle the menu, crash buffer empty. Uncommitted on `main`; needs an RC4 before tagging.

- **2026-11-07 11:35 Claude — Codex review: A1.5, A2.2, Y3, Y4, Y5 merged (`c7c805cf2`); Y2, Y6 sent back.**
  `review/night4` = main + a1.5, a2.2, y3, y4, y5 (string files union-merged). **921 tests, 0 failures.**
  A2.2 gate met: line-by-line read of `36.sqm` (additive only, `ifnull` guard, seeded anime shelf `_id 0`),
  kotlin-reviewer agent "safe to merge" (low items moved to A2.4), and a **scratch upgrade test** on the tablet:
  main build installed as `.scratch` (temporary uncommitted suffix), today's preview backup restored (4 novels,
  1 manga, updates list), then the A2.2 build installed over it → library counts and the Updates list identical,
  no crash, no SQLite errors; `.scratch` uninstalled, scratch worktrees removed. Merged build installed as preview on
  tablet + S22: Y3 webtoon tile + Browse › Manga (Local manga pinned, Extensions · 7) fine, no crashes. A1.5 is
  debug-build-only (no non-owner debug install available) → merged on review + tests, device test pending. Y2
  (PagerViewer rewrite) and Y6 (listen panel inlined in ReaderActivity) back to `ready` with rework notes. A2.3 /
  A2.4 unblocked. Follow-up: details action tiles wrap mid-word at 4 tiles ("TRACKIN / G").

- **2026-11-07 08:40 Claude — Q13 + Q14 done (owner: Codex on anime, Claude fixes).** `54717865f` Q13: new
  `tsuzuki/reader/NovelNextChapterWarmup.kt` called from `ReaderViewModel.updateNovelProgressPercent` (infinite
  scroll off): past 75 % it prepares the next chapter and loads its text; S22 recording: Next ~0.1 s (was ~1.2 s
  blank). Found + fixed: entering a chapter whose text is already loaded kept the previous chapter's 100 % in the
  bar (`NovelViewer.restoreProgress` now reports the start/restored progress). `aa9d5544a` Q14: font tiles keep the
  face's own name (Literata fits), All reader settings tabs in one row (labelSmall), one-line title, paragraph
  indent/spacing in em (`formatReaderEm`), mixed Library grid selection per board (ring + check, others dimmed,
  no tint — the All view uses `TsuzukiMixedLibraryGrid`, which Q9 missed), selection-bar labels white, one line
  (`MARK READ`), slider titles in Dot case (`PreferenceItem.kt`), grouped tiles round first/last *shown* rows
  (`PreferenceScreen.kt`; hidden disabled rows left Translation › General with square corners). 906 tests, 0
  failures; checked on S22 + tablet, no crashes. Q14 item 6 (wider tablet margins / max line length) deferred:
  needs owner decision + layout work in TextView and WebView.

- **2026-11-07 07:25 Claude — cut letters at the page edge fixed (`9ed457af5`).** Owner screenshot (Lord of the
  Mysteries ch. 40, Sepia, Literata, justify): the "f" of "patrol of" lost its tip. TextView clips text to its
  padded area; overhanging glyphs at the end of justified lines were cut. Fix: transparent zero-offset shadow
  (`tsuzuki/reader/NovelGlyphOverhang.kt`, one call in `NovelViewer.applyTextViewStyles`), which widens the clip.
  Tested by adding Lord of the Mysteries from **AllNovelFull** on the tablet (TextView, Sepia, justify): italic
  "off!" at the right edge now whole. Same test showed that the earlier blank-line removal left **no** paragraph
  gap when paragraph spacing is 0 (tablet's setting, `<br><br>` source) → blank lines are now removed only when
  spacing > 0. Tablet preview library now has LotM (AllNovelFull) opened at ch. 1 (test data, not favourited).

- **2026-11-07 06:52 Claude — morning report, overnight batch Q.** **Done + merged into main (pushed):**
  Q1 `7aad35150`; Q2, Q3, Q4, Q5, Q6, Q7, Q12 via `review/night1` `52ac6a536`; Q8, Q9, Q11, X1, X3, X4 via
  `review/night2` `6af172281`; Q10 docs; Claude fixes Browse pills `277130bd3`, paragraph gaps `8582f16a5`, Q2
  labels `86c2ee50d`. X2 dropped (already built). **In progress:** A1.5 anime test screen (Codex). **Queued
  next:** Q13 next-chapter prefetch, Q14 design fixes, then A2.2–A2.4, Y1–Y6. **Nothing blocked.** **Checked on
  devices:** preview build only (never `.dev`), backups first (`app/build/night1-evidence/`), tablet
  `R52Y90HA77L` (Dot rail, Colour Browse, Appearance, translation settings, reader) and S22 `RZCWA139X1X` (Dot:
  nav, Library, Browse + recording, details, save sheet, reader menu/quick/full settings, paragraphs, next-chapter
  timing, selection); crash buffers empty after every install. Owner phone `R5GL23ZYE1V` not connected (Q1
  gfxinfo/recording still owed there). Deleted origin drafts q1-motion, q1-nav, q1-nav-fix, q1-review, q5-more,
  q5-settings (all files covered by q1/q5 in main). **Open questions for the owner:** where do letters look cut
  at the page edge (device/book/renderer, screenshot)? Q4 duplicate-Default migration still unproven on real data
  (run on your phone after an in-app backup). Gradle user-file memory cap applies to all your Gradle projects.

- **2026-11-07 06:20–06:55 Claude — owner awake: memory fix, review/night2 merged, three reader/Browse fixes.**
  Builds were being killed because `~/.gradle/gradle.properties` set `org.gradle.jvmargs` without `-Xmx` (it
  overrides the project's `-Xmx4g`), so the Gradle and Kotlin daemons each took ~8 GB (15.4 GB together). Owner
  approved: user file now `-Xmx4g` + `kotlin.daemon.jvmargs=-Xmx3g` (backup `gradle.properties.bak-20261107`),
  daemons stopped (free RAM 3.6 → 19 GB; new daemons confirmed at 4 g / 3 g). Fixes on main: **Browse pills
  flicker** (header followed `currentPage` while the pager slid Novel sources → Manga sources → Novel extensions;
  now follows `targetPage` and skips jump, `277130bd3`, `TsuzukiPagerHeader.kt`, test); **paragraph gaps** (legacy
  `Html.fromHtml` adds an empty line per paragraph that also got the spacing span; blank lines removed after
  parsing, `8582f16a5`, `tsuzuki/reader/NovelBlankLines.kt`, one-line hook in `NovelTextRenderer.kt`, 4 tests).
  `review/night2` (Q8, Q9, Q11, X1, X3, X4 + fixes) built: **901 tests, 0 failures**; preview on tablet + S22,
  checked Browse (pills steady, recorded), reader paragraphs, reader row labels TOP/TRANSLATE/SCROLL/LISTEN/MORE,
  quick sheet, All reader settings, Q9 selection bar, `131 CHAPTERS`, X3 translation (API key hidden), crash
  buffers empty → main fast-forwarded to `6af172281`. Design bugs found → **Q14**; next-chapter lag measured
  (~1.2 s blank page, no prefetch on the button path) → **Q13**. "Letters half cut at the page edges" not
  reproduced on S22 (margins 48/55 px) or tablet (left margin ~6 dp, tight but not cut) — asked owner where.

- **2026-11-07 05:47 Claude — overnight tick 3: Q8, Q9, Q11, X1, X3, X4 merged on `review/night2`, not on main.**
  All six merged onto main in `.worktrees/rv2` (string files: union; real conflict Q8 × Q9 in `TsuzukiNav.kt`
  and `TsuzukiHomeScreen.kt` resolved by keeping Q8's narrow/large-font layout with Q9's `t.mutedText` contrast
  colour). Diff scan clean (only hex is the `TsuzukiTokens.mutedText` definition; no schema/telemetry files). X1
  read: faithful move of ReaderActivity/ViewModel blocks into `presentation/tsuzuki/reader/`. **Build not run:**
  the preview build + tests (`--max-workers=2`) were stopped by the system for low memory a second time while
  Codex works on A1.5. **Morning, with Codex idle:** build `review/night2` (`:app:assemblePreview
  :app:testDebugUnitTest`), device-check Q11 quick sheet + full settings, Q9 selection/undo/Updates loading/
  Advanced, Q8 at font scale 1.3/2.0 on the S22, X1 reader open/close + next/prev + contents jump, X3
  translation settings (keys hidden), X4 migration screens; then fast-forward main. Upstream files to add to
  AGENTS.md then: Q9's 21 (AppBar, HistoryScreen/Item/Tab, CommonMangaItem, LibraryToolbar, MangaScreen ×2,
  MangaBottomActionMenu, MangaChapterListItem, MangaToolbar, PreferenceScaffold, PreferenceScreen,
  UpdatesScreen/UiItem/Tab/ViewModel, LibraryTab/ViewModel, NovelsTab, MangaViewModel), Q8 `ChapterHeader.kt`,
  Q11 `NovelPage.kt`, `ReaderSettingsDialog.kt`, presentation-core `SettingsItems.kt`, X3 `Preference.kt`,
  `PreferenceItem.kt`, `SettingsTranslationScreen.kt`, `EditTextPreferenceWidget.kt`, X4
  `MigrationConfigScreen.kt`, `MigrationConfigScreenSheet.kt`.

- **2026-11-07 03:47 Claude — overnight tick 2: Q11 reviewed, not merged yet.** Diff review OK (tokens/typography;
  `fontFamily` only for the live preview of the reader's chosen font; `SettingsItems.kt` change is an opt-in
  `LocalStackedSettingsLabel`, default layout unchanged; `ReaderSettingsDialog.kt` dispatch). Merged cleanly onto
  main in `.worktrees/rv2` (`review/night2`), but the preview build + tests were stopped by the system for low
  memory (Codex building Q9 at the same time). Q11 stays `review`; build, device check (quick sheet + full page,
  Dot + Colour, phone + tablet) and merge at the next tick or in the morning.

- **2026-11-07 01:47–02:05 Claude — overnight tick 1: Q2, Q3, Q4, Q5, Q6, Q7, Q12 merged.** Merged the seven
  `review` branches in table order into `review/night1` (only string-file conflicts: each card appended strings;
  resolved as a union, XML valid). `:app:assemblePreview :app:testDebugUnitTest` green, **888 tests, 0 failures**.
  Backups first: preview `.tachibk` from both devices in `app/build/night1-evidence/`. Installed the preview build
  (never `.dev`) on tablet + S22; checked Dot: Q5 nav on phone pill + tablet rail, More has HISTORY tile; Q7 Browse
  NOVELS/MANGA + SOURCES/EXTENSIONS, full-width names; Q12 Appearance only Colour + Dot, accents, covers switch;
  Q6 details toolbar back/download/filter/more, no N/A tile, genres wrap `+7`; Q3 From/To chapter fields,
  shortcuts, `SAVE CH. 0–24`; Q2 fixed ‹ › around progress. **Fixed on review:** Q2 action labels wrapped into
  each other at 360 dp (SCROLL TO TOP over TRANSLATE) → one line + ellipsis (`86c2ee50d`); proper short labels
  are left to Q8. Q4: preview data had no duplicate Default left; migration ran, one shelf, no crash. Tablet
  Colour Browse checked, back to Dot. Crash buffers empty. AGENTS.md: theme rule replaced (Q12 text) + upstream
  touch list. Follow-ups for Q8: More tiles "DATA AND STOR…", "DOWNLOADED ON…"; details "131 chapters" heading
  not uppercased in Dot; reader action short labels.

- **2026-11-06 23:50 Claude — Q1 merged, Q5 rebased, anime + wave Y planned.** Q1 reviewed (diff, 862 tests,
  tablet Dot rail + S22 Colour pill: indicator slides, dot grid ok, no crashes) → merged `7aad35150`; also removed
  the unused `tsuzuki_standard_steps` interpolator. `tsuzuki/q5` rebased onto it (`6612e1f1b`, Q1 part dropped
  cleanly). Owner answered the anime schema questions (ANIME_SCHEMA §9: separate anime shelves, import-only
  Aniyomi, seasons kept inert, separate watch stats, no fansub filter; Claude may merge migration 36 if all tests
  pass; test repos yuzono + Secozzi). Added anime lane A1.5–A2.4 and wave Y1–Y6; X2 dropped (already built).

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

- **2026-11-14 Codex review lane — PERF1 completed.** All four speed budgets measured on optimized RV5 Preview/S22 with Y4 synthetic data. Cold startup and settled details missed; PERF-START/PERF-DETAILS created without tuning. Library passed; local Native chapter-first-text limited pass near measurement uncertainty. See PERFORMANCE.md and local perf1 evidence. REL preparation next.

- **2026-11-14 Codex review lane — REL-0.1.1 preparation completed.** Merged accepted main into existing release prep; Preview/Release + 1,085 tests passed, 16 KB APK/ELF/profile audits passed. Exact tagged-v0.1.0 Debug → optimized 0.1.1 scratch upgrade retained all Y4 book/chapter/history/shelf state and exported settings. Both candidate Dot Homes inspected, empty crash buffers. Local artifacts use Android Debug signing; official signing/tag/publish and owner-phone install remain owner-only. Device lock released, both kept awake. No push/tag.

- **2026-11-14 final Case B audit:** archived Preview/Release/scratch SHA-256 hashes rechecked, successful build log and empty final crash logs verified. Tablet still on scratch candidate; `wm user-rotation` already reports `free`, so no device setting changed. Required owner-publish stop point remains. Evidence `app/build/review-evidence/rel-011/final-audit.json`.
