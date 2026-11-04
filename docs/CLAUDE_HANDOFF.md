# Final main-only handoff (Codex, 2026-11-04; finished before 03:50 JST)

Owner's two-task scope is complete. No commits, pushes, changes to other worktrees or 17-drafts implementation in this session. Main stays uncommitted. Latest main APK installed on tablet R52Y90HA77L; tablet on Dot Home. Owner data/preferences restored; original automatic-update interval remains absent/default Off. All system/permission dialogs were left alone.

- **Order 14.10 done:** existing pill implementation needed no source correction. Four per-file Spotless runs report IS CLEAN; JDK 21 assemble/tests exit 0. Dot 12 h and Colour Daily taps persist preference values 12/24 and schedule real WorkManager intervals 43,200,000/86,400,000 ms; Off cancels work. Selection survives restart. All 12 DB/preferences/WorkManager backup files restored and SHA-256 verified. Evidence `app/build/tsuzuki-order14-10-20261104/`.
- **Order 14.11 done:** Categories board wide New shelf action and theme background/Dot grid. New `TsuzukiCategoryCreateBar.kt`; thin hooks in presentation `CategoryScreen.kt` and `CategoryFloatingActionButton.kt`. Existing create callback/dialog used; no ViewModel/database changes. One label in all eight locales, now 141 tsz keys each. Dot: empty screen/action/temporary real Shelf_check creation. Colour: row/action/dialog cancel. Default: original stock category row and Add FAB visually confirmed. Per-file formatter clean; JDK 21 assemble + **626 app unit tests pass**, zero failures/errors/skips. Fresh 12-file backup restored and hashes checked; test shelf removed, original Off interval restored, Dot Home, no crashes. Evidence `app/build/tsuzuki-order14-11-20261104/`.
- Upstream touched list and design implementation table updated. Category book counts/footnote remain omitted. Download queue remains deferred. Owner decisions (17-drafts, support/privacy links, pushed-screen palette) still pending; do not treat this session as approval.

**Restore receipts:** each evidence folder has `before/manifest.json` and `restore-hashes.json`; all 12 hashes match per test. Restores used binary-safe adb push + run-as cp, never binary stdin. Receipts were checked before app restart; SQLite/WorkManager can legitimately rewrite live WAL/SHM afterwards. Do not restore an old backup over later owner activity. No reading screen was opened. Device language and density were not changed.

**Validation/build:** `app/build/tsuzuki-order14-11-20261104/build.log` is the final successful build/test run. Only per-file Spotless was used; comparison with the pre-session tracked-file hashes found no unrelated source formatting changes. No new UI tests were added for these reversible layout changes; device checks and existing app tests supply verification.

**Continuation at 04:00:** read root AGENTS.md Current state and order 14 first. Current owner instruction limits Codex to main and forbids commits/other worktrees/17-drafts. Preserve all uncommitted work; do not restart previously completed tasks from the historical notes below. Anime/picks updates in Claude's earlier top note remain historical context only; this session did not inspect or modify those worktrees.

---

# Update (Codex, 2026-11-04 01:24 JST): current main-only task

- Owner restricts this session to main in this folder; no commits, no other worktrees, no 17-drafts implementation. Stop by 03:50 JST; Claude takes over at 04:00 from AGENTS.md.
- Order 14.10 is complete. Existing code unchanged; all four per-file Spotless runs report IS CLEAN. JDK 21 assemble + unit tests exit 0; latest APK installed.
- Tablet Dot 12 h and Colour Daily pills store 12/24 and create real WorkManager intervals 43,200,000/86,400,000 ms. Selection persists after relaunch. Off cancels the periodic work (state 5).
- Original interval absent/default Off. DB/WAL/SHM + all preferences + WorkManager DB trio backed up and restored with adb push/run-as cp. All 12 SHA-256 hashes matched before relaunch. Tablet on Dot Home, empty crash buffer. No reading screen opened or device language changed.
- Evidence and safe test helper: `app/build/tsuzuki-order14-10-20261104/`, especially `before/manifest.json`, `restore-hashes.json`, `build.log`, `library-dot-12h.png`, `library-colour-daily.png`.
- Root AGENTS.md order 14 and implementation status updated; three upstream preference files added to touched list. Optional next order-14 item being selected from approved boards and existing data.

# Update (Claude, 2026-11-03 late): supersedes the gates below where they conflict

- Order 8 code is done on `upstream/mihon-picks` (commits `43d299129`..`6f8319531`, see `docs/UPSTREAM.md`). The owner said yes to a manga source; the existing Asura Scans extension was used. Tablet is back on main with the owner's data restored byte-for-byte.
- Anime native run **41645408336 succeeded**; artifact audited locally (see `.worktrees/anime-spike/docs/ANIME_SPIKE.md`). Libraries not yet linked; no playback.
- New owner direction: AGENTS.md orders 13 (draft boards for undesigned screens, awaiting approval) and 14 (build board features, easiest first, starting with the Settings mismatches).

# Continuation handoff — 2026-11-03

Read the current root `AGENTS.md` first. Its ordered queue and owner instructions
supersede the older AGENTS.md checked out in the spike/picks worktrees.

## Preserve this state

- Main remains `33bda2171` with the owner's large uncommitted tree plus this
  continuation. Do not reset, stash, discard, commit or merge it without the
  owner's instruction. The sole approved commit/push is the four native builder files
  on the isolated spike branch; main and the API port remain uncommitted.
- Tablet `R52Y90HA77L` has the latest main APK, Dot Home, physical density 340,
  original chapter/history/preferences restored. Mother of Learning chapter 1
  remains the Continue book. Phone is disconnected; no sources installed on it.
- `../Tsuzuki-upstream` is still the separate `upstream/mihon-picks` worktree at
  `d80df61c3`. No engine changes were made there in this continuation.
- Anime work is isolated on `codex/anime-spike` at **`.worktrees/anime-spike`**,
  based on `33bda2171`. It does not contain main's uncommitted improvements.
  The folder is excluded locally through `.git/info/exclude`. Do not install
  this baseline over the owner's tablet app or delete its uncommitted work.

## Completed on main

Orders 3–7: sheet motion, circular theme reveal across recreation, Home cover
shuffle and palette morph, shared cover transitions, approximate reader Activity
transition, remaining tile press feedback. Owner explicitly approved order 6's
navigation scope changes. Dot/Colour tablet recordings were frame-reviewed;
reduced motion and stock-theme fallbacks checked. Selected Library tab survives
BACK. Owner data and settings were restored afterward.

Order 10: all 138 `tsz_*` keys added to zh-rCN, ko, es, pt-rBR, fr and de.
XML, complete key sets, duplicates, placeholders and escapes checked. No device
language change; translated layouts have not been visually checked.

Order 12: fixed grey cold-start window with
`app/src/main/res/values-night/colors.xml`. The missed inherited night resource
in presentation-core was `#202125`; packaged splash now resolves to black.
Cold recordings in Dot/Colour confirm no grey interval. No reboot required.

Main assemble and app unit tests pass. Formatter still stops on the two known
upstream lint errors; all unrelated formatting was restored exactly. Evidence,
recordings, resource dumps, test-state backups and logs are under ignored
`app/build/tsuzuki-motion-evidence/` and `app/build/tsuzuki-*-build.log`.

## Owner decisions and remaining gates

1. Order 8: explicit MangaDex installation approval is still unanswered. Do not
   install a manga source or start that engine port before approval/source test.
2. Order 9: wait for the owner to commit main; do not commit it for them.
3. Order 11: owner said they will make the repo public **later**, after the rest
   is finished. Release setup/publication remains deferred. No signing changes,
   version bump, tag or release made.
4. Anime: owner chose **another Linux builder**, then explicitly chose
   **GitHub Actions**. Do not ask them to choose the builder again. No self-hosted
   runners exist; use a GitHub-hosted Ubuntu runner. Docker backend exits during
   startup; task-owned CLI waits were stopped. Do not reset/reconfigure Docker.

## Anime Stage 1 checkpoint (incomplete)

Work only in `.worktrees/anime-spike`. Its `docs/ANIME_SPIKE.md` records pinned
upstream revisions and licence findings. Imported 26 Apache-2.0 source API files,
Android PreferenceScreen typealias, NanoHTTPD 2.3.1, an R8 keep rule, and adapted
loader/Installed/Untrusted models under `tsuzuki/anime/extension`. Existing host
trust/signature checks remain; private anime extensions use `files/anime-exts`.
No registration, UI, database migration, source installation or player yet.

Post-format API/app compilation and **593 existing app unit tests** pass, with
zero failures/errors/skips. These do not prove extension ABI/runtime playback.
Source-api formatting passes; app formatting has only the two baseline errors,
and 15 unrelated tracked formatter changes were restored to this branch's HEAD.

Aniyomi app is Apache-2.0 and MPV wrapper MIT, but the native FFmpeg script enables
GPL/version3; the named ffmpeg-kit fork invokes that script despite its separate
GPL_ENABLED=no default. No native dependencies were linked. A non-GPL native
build, dependency/source-licence review and 16 KB ELF/APK audit remain necessary.

Prepared in the spike and published with explicit owner approval (builder files only):

- `.github/workflows/anime-native.yml`: Ubuntu 24.04, arm64, read-only token,
  automatic push trigger limited to `codex/anime-spike` and native build paths,
  120-minute timeout, native/evidence artifact retained 14 days.
- `scripts/anime-native/build.sh`: pinned wrapper/dependencies, NDK r27c,
  GPL/version3/nonfree/postproc disabled in FFmpeg, `-Dgpl=false` in libmpv,
  unused libpostproc JNI reference removed, native libraries/headers/evidence.
- `scripts/anime-native/audit.py`: checks feature configuration, required arm64
  libraries, absence of postproc, ELF PT_LOAD alignment and SHA-256 manifest.
- `scripts/anime-native/test_audit.py`: nine tests of acceptance/rejection
  boundaries; included as a workflow step before the native build.

Validation checkpoint: local Bash syntax, Python AST and whitespace pass.
Official Actionlint **1.7.12** (release ZIP SHA-256 verified against publisher's
checksum file) parses/checks the workflow successfully. All **nine auditor
tests pass**, covering each disallowed FFmpeg flag, missing configuration,
libmpv GPL true/missing, stray postproc, missing player, wrong architecture,
4 KB alignment and incongruent addresses. Synthetic ELF fixtures test audit
logic; they do not establish runtime playback or actual native artifact safety.
A remote build is now running; no verified artifact yet. This is the first native build experiment, not
completed FFmpeg-kit/player integration or a licence-compliance claim.

Owner explicitly approved the builder-only commit/push. Commit
`75a1b7c0998be3874f4344578c98a8e0802a7ec5` contains exactly the four builder files;
it is pushed to `origin/codex/anime-spike`. No PR, release, API-port commit or
main commit was created. Do not ask again for this already granted approval.

**Live GitHub Actions run:**
https://github.com/Manojx2005/Tsuzuki/actions/runs/41644824316
Run ID `41644824316`, confirmed `in_progress` after push. Monitor this run;
do not trigger another merely because an observation times out. No artifact
has been verified yet. Follow-up builder fixes belong only on this branch;
keep the API port and main uncommitted. Python `__pycache__` under scripts is
untracked generated validation output; never include it in a commit.
No episode has been played; leave Stage 1 unchecked.

## Build and recovery reminders

Use Android Studio's JDK 21, not Java 24 on PATH. Gradle/adb require the existing
escalated tool context for caches/device daemon. Main debug app id is
`io.github.manojx2005.tsuzuki.dev`. Do not reuse main's formatting snapshots to
restore spike files; their starting trees differ.

For any binary device restore: `adb push` to staging, then `run-as cp`, and
byte-verify. Never pipe a binary DB into adb stdin on this Windows setup; it
truncates. A consolidated original DB is saved as
`app/build/tsuzuki-motion-evidence/restored-baseline.db`. Do not restore again
unless a new test actually changes owner data.
