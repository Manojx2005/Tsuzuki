# Taking changes from Mihon and Tsundoku

Tsuzuki = Tsundoku (novel features) + Tsuzuki design, and Tsundoku = Mihon + novels. All three share one
git history back to Tachiyomi's first commit, so upstream commits can be cherry-picked directly.

## Why new upstream features don't need design work

Tsuzuki's design is layered on top of upstream code instead of replacing it:

- **Dispatch at the top of upstream composables.** `HistoryItem`, `MoreScreen`, `ChapterSettingsDialog`, … call a
  `presentation/tsuzuki/...` version when `TsuzukiTheme.tokens.style != NONE`, and fall through to the stock code
  otherwise. Upstream can change the stock body freely; the dispatch line rarely conflicts.
- **Slots instead of rewrites.** `LibraryToolbar(below = …)`, `MangaToolbar(downloadSheet = …)`,
  `NovelReaderAppBars(onOpenContents = …)`: nullable parameters with a default, so upstream callers keep compiling.
- **Theme-level styling.** Screens Tsuzuki hasn't redesigned still pick up its colours and fonts through
  `MaterialTheme` (colour schemes + `LnrTypography`), `LocalActiveColor` (upstream's "active" tint),
  `PreferenceScreen` groups as tiles, `TabbedDialog` / `TabbedScreen` tabs as pills.

So a new Mihon feature arrives with **stock layout in Tsuzuki colours and fonts**: usable immediately, and it can
get a board and a Tsuzuki version later without blocking the merge.

## Release channel: upstream goes through Tsuzuki

```
Mihon / Tsundoku ──► weekly issue "Upstream changes to review" (.github/workflows/upstream-watch.yml)
                       └─► picked here (workflow below) ──► Tsuzuki main ──► Tsuzuki release ──► in-app updater
```

- Nothing upstream reaches users directly. The in-app updater (`data/updater/AppUpdateChecker.kt`) only checks
  `Manojx2005/Tsuzuki` releases.
- The updater calls GitHub's public releases API, so the repo (or a separate public releases repo, set in
  `GITHUB_REPO`) must be public before the first release.
- Tsundoku's release signing in `app/build.gradle.kts` only runs for the `tsundoku-otaku` owner; Tsuzuki needs its
  own keystore secrets before v0.1 (task queue: release).

## Remotes

Remotes are local git config; on a new machine `scripts/upstream-report.sh` adds them automatically:

```sh
git remote add tsundoku https://github.com/tsundoku-otaku/tsundoku.git
git remote add mihon    https://github.com/mihonapp/mihon.git
```

## Workflow

1. **Report:** `scripts/upstream-report.sh` (or `… mihon`) fetches and lists every upstream commit not yet in
   Tsuzuki (already-picked ones are hidden by patch id), grouped by area, with flags:
   - `.`: touches no file Tsuzuki or Tsundoku changed, usually picks cleanly;
   - `D`: touches a file Tsundoku changed from Mihon (likely conflict);
   - `T`: touches a file Tsuzuki changed (likely conflict, and **look at the screen afterwards**).
2. **Pick in a worktree** so uncommitted work is untouched:
   ```sh
   git worktree add ../Tsuzuki-upstream -b upstream/<topic> HEAD
   cp local.properties ../Tsuzuki-upstream/
   cd ../Tsuzuki-upstream
   git cherry-pick -x <sha>      # oldest first
   ```
   - Conflict only in `CHANGELOG.md`: keep ours (`git checkout --ours CHANGELOG.md && git add CHANGELOG.md &&
     git cherry-pick --continue`).
   - Any other conflict: `git cherry-pick --abort` and log it below. Don't pull in a big refactor just to make one
     fix fit.
3. **Build and test**: `./gradlew :app:assembleDebug :app:testDebugUnitTest`. A clean textual pick can still fail
   to compile when it relies on a Mihon refactor (e.g. `download.manga`, the new `Chapter` model); revert those
   picks (`git revert`) and log them.
4. **Check on a device**: the screens the picks touch, plus one novel and one manga download if download code moved.
5. Merge the branch into `main` once the owner has committed main's work.

When Tsundoku itself later merges Mihon, commits already picked here usually merge cleanly (same patch).

## Big refactors in Mihon not taken (as of 2026-11-03)

Mihon after v0.20.4 redid the reader (Compose-drawn, viewer out of the ViewModel), stores dates as `Instant`,
dropped the legacy chapter model, reworked database queries for a new schema, and moved icons to Material Symbols.
Tsundoku rewrote much of that code for novels, so these are not cherry-pickable; most later Mihon fixes that build on
them fail for the same reason.

## Downloads

Novels and manga share `data/download/Downloader.kt`, with novel branches inside it. Novels are paced on purpose
(`NovelDownloadPreferences`: 3 s + 0–1 s jitter per request, one chapter per novel at a time, configurable under
Settings → Novel Downloads) to avoid source rate limits; Mihon's speed work mainly helps manga.

Mihon's largest speed-up, `730e43fd1` ("download a source's next chapter while the current one finishes"),
redesigns the scheduler that Tsundoku also redesigned (rate-limit guards, novel progress, keep-going-on-error), so
it needs a hand port. First step done (branch `upstream/mihon-picks`): novel-only code moved to `NovelDownloadEngine.kt`, cutting
`Downloader.kt`'s distance from Mihon's from 470/102 to 334/97 lines. It doesn't make `730e43fd1` pick cleanly,
because its conflicts are on Tsundoku changes that apply to every download (rate-limit guard wrapper,
keep-going-on-error, the `mangaTitle`/`mangaId` model); expect to hand-port Mihon scheduler changes. Direction: **one shared queue / notification / service with separate
engines** (manga = kept close to Mihon's file so its download commits pick cleanly; novel = Tsundoku's paced fetcher;
anime = from Aniyomi later).

## Pick log

### 2026-11-04: picks merged into main

Owner authorized local commits and the merge through the CODEX_PROMPT handoff; no push.
Main's existing work was committed in `cf5d4326c`, `5bb7e6555`, `f76fc2811`, and `fa569add0`.
All 28 picks merged without textual conflicts in `c54b2f809`; Tsuzuki hooks were retained.
Changed-file Spotless, `:app:assembleDebug :app:testDebugUnitTest` (630 tests), and tablet checks passed.
Checked Dot Home/details/novel reader, Colour details/novel reader/manga long-strip reader, and Dot manga reader.
Novel group pause held its queue unchanged, Resume advanced it; novel and manga queues survived force-stop
and resumed. Two manga chapters and a novel batch downloaded alongside each other.

Restart testing found an inherited tall-image recovery bug: process death during splitting leaves the original
beside incomplete split files, and retry could choose a split first, producing "0 pages failed" while failing
the folder count. `de061e0ef` prefers the original and removes its interrupted split siblings before regenerating
them, including when splitting has since been disabled. It also passes the existing padded page filename to
the splitter. The same failed chapter completed with the fix; its CBZ had 43 entries, valid CRCs and no empty
files, and opened in the reader. Build/tests passed again. Crash buffer empty.

All 12 fresh owner DB/prefs/WorkManager files were restored with push + run-as cp and SHA-256 verified before
restart. Continue is now Nano Machine chapter 336 (owner state at test start). Only the two newly created test
download folders were removed, and the original download-directory set matches. Tablet left in Dot Home.
Evidence and backup: `app/build/codex-merge-20261104/`; validation logs in `app/build/codex-*-validation.log`.
Phone was connected but not modified. Git unregistered the merged picks worktree; Windows' long-path deletion
failure left files in its old folder, so finish cleanup with long-path support. Anime worktree untouched.

### 2026-11-03: branch `upstream/mihon-picks` (worktree `../Tsuzuki-upstream`)

Applied (18), builds, unit tests pass, novel download checked on the tablet:

| Mihon | Change |
|---|---|
| 7aacaa349 | Keep the paused download notification after the worker stops |
| 6490c41e6 | Wait for a canceled downloader job to finish before starting another |
| deeb9b258 | Fix downloads not being found for sources sharing a stored source's name |
| d1c5c12bd | Fix webtoon reader leaving a gap after zooming in a resized window |
| 76b146f3d | Show the updates widget's empty and locked text |
| ab7ee00c4 | Fix tap navigation staying off after a cancelled button press |
| 785f4fd1e | Keep downloads in excluded categories when marking them read |
| be44a81fb | Keep listing local chapters when one archive can't be read |
| adc9ee74e | Respect hidden missing chapter indicators in chapter headers |
| a82d27c3a | Keep the chapter selection when new chapters download after a refresh |
| a01867dc3 | Only send source headers to the source in WebView |
| 35bd0a4b3 | Long strip side padding no longer counted in zoom bounds |
| abb126ad2 | Fix startup crash on some Samsung Android 9–11 devices |
| 51b61a36e | Keep the pager on its page when chapters are added before layout |
| 3156b4a1c | Insert split pages only into the chapter they belong to |
| 2df48e19a | Fail the extension install when nothing can confirm it instead of crashing |
| c371d6331 | Ignore long strip taps confirmed after the reader is gone |
| bd2ada2e8 | Fix Sui not being detected as a Shizuku provider |

Picked then reverted (compile against Mihon refactors): 611578313 (index a finished download found on disk),
36ec043a5 (delete downloads of duplicates).

Skipped for conflicts with Tsundoku's code (candidates for hand ports, most valuable first):
730e43fd1 + f02bf5ba7 + 9a6dea804 + fb8e640d6 + ba16158b1 (download speed / less work), 88d4071cc, f0f1e4de5,
a5d584eeb, f28b2d887, f82a7a68f (download correctness), 649fc83ff + 5a5ff22c3 + 5f63c983d + 7ae858e60
(WebView / Cloudflare robustness), c279291e2 (FAB under taskbar on tablets), dd4e9f00f (confirm unfavourite),
a6ee4bc59 (library range selection crash), 1e471a414, 287cbbb98, 8457fb91e, 07b9fb695, ae749bb4f, cdc7d69c9,
c9e1b6e96, 4c88f0264, d3f10772f, e9234f205, b5856135c, a42428284, 0864ce0f4, 5910a51eb, 0c1be369b, 40f552981,
c0c090999, ee5939454; database migrations (6dffca159, a999e49f2, 9dbb7148c, ea2b73ad7) need merging with
Tsundoku's migration numbering.

### 2026-11-03: download engine hand-ports (order 8, same branch)

| Mihon | Picks commit | Notes |
|---|---|---|
| 730e43fd1 | 43d299129 | One job per manga source, shared page-slot semaphore; the next chapter starts once all pages of the current one have a slot. Kept Tsundoku's per-host rate-limit guard/bypass (both refcounted per host, so overlapping chapters stay guarded; a bypassed chapter overlapping a guarded one on the same host is paced for that overlap) and its keep-going-on-error. Novels keep `NovelDownloadEngine.pickActive` and run through the same page loop with one slot. `DownloadNotifier.onProgressChange` is `@Synchronized`. |
| 9a6dea804 | 7ebe29d00 | Applied around Tsundoku's id-based `Download` constructor. |
| f02bf5ba7 | 993a0e112 | `DownloadProvider` lookup ported. Also fixes a Tsundoku bug: `queueChapters` matched downloaded chapters by list position against a `mapNotNull` result, so the wrong chapters could be skipped. |
| fb8e640d6 | f35251118 | Queued/downloaded lookups once per chapter list (MangaViewModel, UpdatesViewModel). |
| ba16158b1 | skipped | Builds on Mihon's MangaViewModel state refactor (`subscriberGate`, `downloadStates` map) that Tsundoku never took, and removes `ChapterList.Item.downloadProgress`, which the chapter rows read. Take it with that refactor after the merge (order 9). |

Tsuzuki fix found while testing, `6f8319531`: on a cold start `SourceManager.isInitialized` turns true before
installed extensions reach the source map (`installedExtensionsFlow` is derived on its own scope), so
`DownloadStore.restore` found no source, dropped every queued manga chapter and cleared the store. The downloader now
waits for the source manager, and restore gives a missing source up to 10 s (once per source), logging when it still
drops one. Mihon restores the same way; worth reporting upstream.

Tablet test (Galaxy Tab S11, Asura Scans + Royal Road): 10 manga chapters with consecutive chapters overlapping
(`Chapter 4_tmp` and `Chapter 5_tmp` at once), 3 novel chapters alongside with request starts 3.0 s and 3.6 s apart,
kill + cold start restores a seeded manga queue (2 → 2) and a novel queue (3 → 3, resumed and finished). Novel group
pause wasn't shown on the device: the 3 chapters finished before the pause landed. The code path is unchanged.
Build and unit tests pass. Test downloads were deleted and the owner's database/preferences restored byte-for-byte.

### 2026-11-04: WebView / Cloudflare robustness group

Fetched Mihon main `8794e89a8`. These three files are identical to that revision:
`core/common/.../network/interceptor/CloudflareInterceptor.kt`, `WebViewInterceptor.kt`, and
`util/system/WebViewUtil.kt`. They include per-host challenge serialization, non-GET challenges on the site's
root, early exit for interactive/failed challenges, renderer-death handling and unsafe-header filtering.
NetworkHelper uses the existing Injekt app coroutine scope; Tsundoku's rate limiting is retained. Both app
WebView entry points use the shared header filter. The existing themed browser/menus stay; its renderer-death
callback closes the failed page with the upstream message (eight locales). WebKit 1.18.0 matches Mihon and
the [official stable release](https://developer.android.com/jetpack/androidx/releases/webkit#1.18.0).

Normal preview/debug builds, changed-file formatting, 630 app tests and 26 core tests pass. Three new core
tests reject Origin/transport/proxy headers, retain source authentication/UA/Referer, and preserve the first
repeated header. Optimized renderer regression passes on Tab S11 (2.689 s) and S22 (4.442 s): actually crash
the renderer, require the callback and page closure, then render a fresh data page. Ordinary daily preview
then loaded real Asura (tablet Colour) and Royal Road (phone Dot) pages; BACK returned to the reader.
Seven ARM64 ELF/APK 16 KB checks and packaged baseline assets pass. Both devices left Dot Home; original
debug data was untouched. Preview copies contain test reading positions/history.

Reproduce the renderer regression with JDK 21 and the separate seeded preview copy:

```powershell
./gradlew.bat :app:assemblePreview :app:assemblePreviewAndroidTest -PtsuzukiTestBuildType=preview
# Install BOTH matching APKs on the selected device before running instrumentation:
# app/build/outputs/apk/preview/app-arm64-v8a-preview.apk
# app/build/outputs/apk/androidTest/preview/app-preview-androidTest.apk
# runner: io.github.manojx2005.tsuzuki.debug.test/androidx.test.runner.AndroidJUnitRunner
# class: eu.kanade.tachiyomi.tsuzuki.WebViewRendererRecoveryTest
```

The test refuses the original `.dev` app. AndroidJUnitRunner needs `androidx.tracing.Trace` retained in its
target; the conditional test-only rule applies solely with this property. Build the normal daily preview
without it. Selecting preview testBuildType removes the debug unit-test task, so run debug unit validation
separately. `assemblePreviewAndroidTest` alone does not package an updated target APK: build both as above.
Local evidence, matching test APK pair/SHA manifest, callbacks, screenshots and daily-process crash logs:
`app/build/codex-preview-20261104/`; final normal-build log `app/build/codex-webview-daily-validation.log`.
Two initial instrumentation-runner startup failures were harness/package issues, not renderer recovery; the
final tests pass. Daily main-process crash logs after cleanup are empty.

Still pending in priority 3: asynchronous source-header resolution, installer and download-provider ports.
The current installer differs in repo/signature models and DI; retain compatibility when adapting it.
No live concurrent Cloudflare challenge was forced in this check.
