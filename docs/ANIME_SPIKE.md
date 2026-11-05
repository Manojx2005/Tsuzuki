# Anime Stages 1–2 spike

## Current state — Stages 1–2 complete in spike (2026-11-04)

Stage 2 is implemented and device-verified in `.worktrees/anime-spike` on
`codex/anime-spike`. Main and upstream worktrees remain untouched. No new
commits/pushes. Source changes remain uncommitted; merging just the branch
commits will NOT bring these source files across.

- [x] Additive migration 36 creates separate `tsuzuki_anime` and
  `tsuzuki_episodes`; no reader table repurposed. Android DB migrated from
  user_version 36 to 37 successfully. Stable source+URL identities, constraints,
  foreign-key cascade, transactional refresh preserving episode/user state.
- [x] Novels / Manga / Anime library switch, real counts, Plan/Watching/Completed
  lists, real covers and recorded-state progress. Dot greyscale/grid and Colour
  checked on S22. Legacy Default retains stock UI with no anime dispatch.
- [x] Real AnimeGG One Piece added, 1,167 unique episodes stored. Opening from
  its saved cover, watch-list moves, refresh, remove/re-add and process restart
  verified. No duplicate episodes; watch status persisted. Novels/Manga stayed
  empty and separate. DB receipt: one anime ID 1, favorite=1, watch_status=1,
  episodes_fetched=1; 1167/1167 distinct episode URLs, no seen/bookmark/progress
  fabricated, reader manga/chapter/history counts stayed 0/0/0.
- [x] Native episode playback still works in the final Stage 2 APK: One Piece
  1180 first 480p stream, H.264/AAC, mediacodec-copy, zero dropped frames at
  sample. This regression ran muted at original 0/15; original Stage 1 audible
  sound had been explicitly confirmed by owner.
- [x] JDK21 required compile/assemble/test command and SQLDelight migration
  verification pass, exit 0; 593 app tests, no failures/errors/skips. Ten SQLite
  schema regression tests pass; actual old DB fixture preserved all rows in
  all nine old tables. All 18 APK ELF libraries and zipalign -c -P 16 pass.
  Per-file Spotless IS CLEAN; six new strings each occur once in eight locales.
- [x] Restore receipt matches all 8 DB/WAL/SHM/preference hashes before relaunch.
  Owner selected storage/completed setup during testing: newer preference
  backup was deliberately retained, and storage/setup values checked equal
  after relaunch. Pre-test DB restored, temporary favorite/episodes removed;
  latest schema recreated cleanly and empty Anime 0 UI verified. No file extras
  needed removal. Phone left Home at 0/15, newest spike APK installed; tablet
  and both devices' existing main packages/data never replaced.
- [x] Final crash buffer has only the historical fixed 08:35:50 failure; no
  Stage 2 crashes. No system picker or permission dialog tapped by Codex.

Latest APK: `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`.
Installed/local SHA256: `59fa9290f0e7bcac57b40621c2aa43b649f40cecf5863fd1caa92573b87c0ac0`.
Build with Android Studio JDK21:
`./gradlew :source-api:compileDebugKotlin :app:assembleDebug :app:testDebugUnitTest :data:verifyDebugDatabaseMigration -PanimeSpike=true`.
`ANIME_SPIKE` defaults false and is set by that debug property; unbundled normal
builds show native playback unavailable instead of loading missing libraries.
Native artifact and licence assets remain local app/build inputs from Stage 1.

### Claude merge instructions

1. Preserve main's uncommitted tree. Copy NEW source/schema/test/licence files;
   hand-merge existing-file hooks and individual string keys. Never overwrite
   main's NovelsTab, LibraryTab, theme/media components or full strings files
   with this older branch's versions. Full file/hash inventory is
   `app/build/anime-stage1-20261104/stage2-merge-files.json`.
2. Recheck main's migration head (35 when work started). Renumber 36.sqm if
   needed, and update scripts/anime-stage2/test_schema.py's migration path.
   Keep its DDL identical to tsuzukiAnime.sq. AppModule already enables foreign
   keys; no DB constructor/adapter changes were needed.
3. Main already has combined-library filters and obsolete-ViewModel cleanup
   absent in this old baseline. Keep them. Port the anime dispatch into that
   existing host/filter; do not blindly apply this prototype readerType helper,
   which splits Tsuzuki Novels/Manga while retaining legacy joined All mode.
   No new navigator structure or MainActivity hooks were introduced.
4. Existing-file hooks: NovelsTab top anime dispatch/type-keyed VMs;
   LibraryTab top dispatch; TsuzukiLibraryMediaSwitch shared selector;
   app/build.gradle opt-in native/app-ID/ANIME_SPIKE field; Stage 1 quickjs
   1.0.15 in app/core and source-api NanoHTTPD/API keep rule. Six tsz_anime*
   keys in base, ja, zh-rCN, ko, es, pt-rBR, fr, de. Match main's existing
   build/dependency choices rather than reverting them.
5. Add/remove and manual watch-state editing still use the debug source browser.
   Production anime browsing/details/navigation/player progress/history,
   downloads/backups/trackers are Stage 3; full styling is Stage 4. Cover taps
   open the debug source screen only in debug builds. No automatic seen/progress
   recording, anime backup support, seasons/fillers/airing badges or source-health
   data is claimed. Anime source loader remains isolated, not a manga source.
6. Keep native GPL disabled and the owner's explicit LGPLv3 exception. Release
   distribution still needs corresponding-source/relinking/runtime provenance
   work and separate owner release decisions. Tablet layout of this new anime
   library has not been device-tested because tablet replacement was excluded.

### Evidence / restoration

All under `app/build/anime-stage1-20261104/`:
- `stage2-build.log`, `stage2-schema.log`, `apk-audit.json`, `zipalign.log`.
- `stage2-dot-final.png`, `stage2-dot-watching.png`, `stage2-restart-dot.png`,
  `stage2-novels.png`, `stage2-manga.png`, `stage2-colour.png`,
  `stage2-default.png`, `stage2-readded.png`, `stage2-player-regression.png`.
- `stage2-after/results.json`: actual schema 37, unique episode count and
  separate reader row counts. Raw captured DB inspected only through a copy.
- `stage2-backup/manifest.json`: pre-test DB/WAL/SHM and old preferences;
  `stage2-owner-prefs/manifest.json`: owner-approved newer setup preferences.
  `stage2-restore-manifest.json`: combined authoritative restoration baseline;
  `stage2-restore-hashes.json`: all 8 matched; `stage2-owner-setup-preserved.json`:
  storage/setup still equal after restart; `stage2-restored-empty.png`.
- `stage2-crash-final.txt` retains historical crash evidence; it was not cleared.

Backups/restoration used run-as copies, adb pull, then adb push + run-as cp;
never binary stdin. Temporary backup copies alone were made readable to adb.
SQLite inspection used disposable copies, and all original restore-source
hashes were rechecked. Restore receipts precede relaunch because SQLite can
legitimately rewrite WAL/SHM and migrate the old DB on restart.

The sections below are historical checkpoints and working notes, not pending
instructions. The completed status and merge cautions above take precedence.

## Stage 1 verified checkpoint (historical; Stage 2 summary above is current)

- [x] Played a real episode with picture and sound on owner-approved Galaxy S22
  `RZCWA139X1X`: **AnimeGG 14.2 → One Piece → episode 1180 → first [SUBBED]
  480p video**. H.264/AAC, 852×480, duration 1,415 seconds. Owner explicitly
  confirmed audible sound in chat. This verifies playback, not watching the
  entire episode end to end.
- [x] Audited fresh run 41645408336 artifact (11 libraries, GPL/GPLv3/nonfree/
  postproc disabled, libmpv GPL off). CONFIG_VERSION3=1 / LGPLv3 was explicitly
  approved by owner. Every PT_LOAD is 16 KB compatible (libmpv also has one
  64 KB segment). No GPL code copied into the app.
- [x] Debug-only browser and minimal player; separate opt-in app ID
  `io.github.manojx2005.tsuzuki.anime.spike`. No main navigation changes.
  Source API/loader: Aniyomi Apache2 pin 97414446…; player bridge: MIT
  aniyomi-mpv-lib pin da7c885c…. Original JNI package is preserved as required.
- [x] Latest JDK21 source-api compile + app assemble + unit test command passed,
  exit 0: 593 tests, 0 failures/errors/skips. 11 native auditor tests pass.
  Per-file Spotless completed. All 18 APK native libraries pass ELF 16 KB
  checks and zipalign -c -P 16; 11 player library hashes match the audited
  artifact byte for byte. QuickJS upgraded to 1.0.15 within spike only.
- [x] Play/pause (144→144 seconds), resume (146→153), seek (~715 seconds),
  Back exits player. Rotation recreates the disposable browser at its extension
  list; navigation state persistence is deliberately absent in this spike.
- [x] Tablet and main worktree untouched. Neither the existing main app on S22
  nor any existing reader DB/preferences/WorkManager files were replaced or
  modified. No owner data restore was necessary, so no DB restore hashes are
  claimed. S22 media volume restored from temporary 4/15 to original 0/15,
  checked by adb; phone left on Home with newest spike APK installed.
- [x] Continuation: explicit `mediacodec-copy` hardware decoding verified on S22,
  zero dropped frames at 105 seconds. Pause/resume/seek/Back rechecked on this
  build. Startup surface/underrun warnings persist, with no further underruns
  during the sampled steady-playback interval. No audio buffering changes.
- [x] Progress recorded only here, no new commits/pushes. Stages 2–4 not started.

Evidence: `app/build/anime-stage1-20261104/`:
`phone-playing.png`, `phone-player-current.png`, `phone-after-seek.png`,
`playback.mp4` (screen recording has no audio), `controls-results.json`,
`apk-audit.json`, `native-needed.json`, `zipalign.log`, `build.log`,
`mpv-final-session.txt`, `audio-playing.txt`, `crash-final.txt`, and fresh native
artifact/audit under `audit-cfe06d3e/`.

Latest APK: `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`.
SHA256: `ebe0811666fe31da6893917c6b18797febcecb4369ac8e10f9a347f32fefa4c7`.
Build using Android Studio JDK21 and
`./gradlew :source-api:compileDebugKotlin :app:assembleDebug :app:testDebugUnitTest -PanimeSpike=true`.
Audited native jniLibs and generated licence-notice assets are under app/build
and intentionally uncommitted; preserve them for reproducing this local spike.

Limits: minimal debug UI, no library/database/history/downloads/backups/trackers
or anime styling. External tracks, source-hosted local servers and arbitrary
extension mpv/ffmpeg arguments are not handled. ffmpeg-kit JNI is unnecessary
for this direct-stream mpv proof and was not ported. A MediaCodec surface warning
can appear even with confirmed `mediacodec-copy` hardware decoding; do not infer
software fallback from that message alone. Startup and control-transition audio
underrun warnings remain; none recurred during the 105-second steady sample.
This does not establish behaviour for other streams/devices or long sessions. Crash buffer retains the earlier fixed
unsupported http-header-fields-append setter failure at 08:35; no new crash
occurred in the successful 08:38 onward playback/control test. Do not clear
shared device logs just to hide this known test failure.

Release distribution remains blocked on a complete corresponding-source and
relinking bundle, exact NDK runtime licence/source provenance, and the owner's
separate release decisions. This local Stage 1 result is not a release-ready
licensing/distribution claim.

## Stage 2 implementation notes (completed; final results above supersede pending notes)

Owner explicitly authorized Stage 2 and requested continuing updates for Claude.
All edits remain in codex/anime-spike; no commits/pushes or main edits. Main's
newer offline boards and handoff were read only. No reliably identified live
Claude chat was available; this file is the shared merge record.

Scope: additive anime/episode tables and library Novels/Manga/Anime selection.
Stages 3–4 (full details/history/downloads/backups/trackers/player design) remain
outside this work. Anime Library Dot and Colour boards reviewed read-only from
main docs/design/boards/png/10-anime/{dot/,}AnimeLibrary.png. Only real data will
be shown; sample numbers, seasons/fillers and newly-airing badges omitted.

Schema: both spike and current main end at 35.sqm. Added 36.sqm plus
`data/src/main/sqldelight/tachiyomi/data/tsuzukiAnime.sq`, with namespaced
`tsuzuki_anime` / `tsuzuki_episodes`, stable source+URL identity, per-anime episode
URL uniqueness, foreign-key cascade, separate favorite/watch status and future
playback-state columns. No old migrations or reader tables altered.
`data:generateDebugDatabaseInterface` and `data:verifyDebugDatabaseMigration`
pass. Claude must recheck main's migration head before merging; renumber this
new migration if another migration has landed. Do not overwrite or rebase main's
uncommitted tree. New AnimeLibraryStore writes metadata/episode refreshes in
transactions and excludes seen/bookmark/position fields from source upserts.
Still pending: regression tests, library UI hooks, real-source persistence and
restart testing, device backup/restore receipts, final merge inventory.

## Decoder follow-up (2026-11-04)

Owner asked to continue. Stayed within the existing Stage 1 spike and tested
remaining player warnings, without entering Stages 2–4 or changing main.
`AnimePlayerActivity.kt` now selects `mediacodec-copy` and reports active hwdec
and dropped video frames. Pinned mpv manual describes this Android decoding
mode and its copy to system RAM:
https://github.com/mpv-player/mpv/blob/d82701962f99051a18d65c215b70d41ebadd9a22/DOCS/man/options.rst

JDK21 required build/test command passed (exit 0), 593 unit tests with no
failures/errors/skips; per-file Spotless IS CLEAN. All 18 APK libraries passed
ELF alignment, all 11 native player hashes still match the audited artifact,
and zipalign -c -P 16 passed. Updated only the already-approved separate-ID
spike app on S22. Installed base.apk SHA256 matches latest local APK:
`ebe0811666fe31da6893917c6b18797febcecb4369ac8e10f9a347f32fefa4c7`.

Same real extension/episode/first 480p video: at 4 seconds and 105 seconds,
`hwdec=mediacodec-copy`, `dropped=0`, H.264 video and AAC audio, visible picture.
Current-process log contains one initial surface warning and one initial audio
underrun; no further underruns before the steady sample. Thus hardware decoding
works despite that warning. Kept the documented default audio buffer rather
than masking warnings with arbitrary latency. This follow-up stayed muted at
the owner's original 0/15 volume; audible sound was confirmed by the owner
in the preceding Stage 1 run, not re-confirmed for this follow-up.

Controls rechecked: pause 168→168, resume 173→180, seek 712 seconds with hardware
decoding and zero dropped frames, Back returns to video list. Phone returned
to Home, volume still 0/15. Tablet/main/existing reader data remain untouched.
Crash buffer retains only the earlier 08:35:50 failure, no new crash in this
run. No commits or pushes.

Evidence under `app/build/anime-stage1-20261104/decoder-check/`:
`steady-ui.xml`, `steady.png`, `mpv-session.txt` (before controls),
`after-seek.png`, `controls-results.json`, `mpv-controls-session.txt`,
`crash-final.txt`. Root build/apk-audit/zipalign evidence now describes this
latest APK. `evidence-sha256.json` refreshed for the current evidence files.

The dated checkpoints below are historical and may describe superseded states.

## Compile checkpoint (2026-11-03)

`:source-api:compileDebugKotlin` passes after fetching NanoHTTPD.
`:app:compileDebugKotlin --offline` passes with the adapted loader/models.
The loader is not registered. Installed/Untrusted models stay local to the spike;
no anime database, source-store catalog, install receiver, or player is added.
Private packages use `files/anime-exts`; an install callback is mandatory so a
future host controls notifications. Existing TrustExtension checks, feature/API
version checks, child-first loading/fallback and signature validation remain.
Malformed metadata/signing info return Error instead of a null assertion.

API and loader code retain Aniyomi's Apache-2.0 terms. Original source:
https://github.com/aniyomiorg/aniyomi/tree/97414446b8a95994c72dd33c41c971a89d4d25b8
Native build examined:
https://github.com/aniyomiorg/aniyomi-mpv-lib/blob/da7c885c18ae117ca27239c562ea7572fce3d4f5/buildscripts/scripts/ffmpeg.sh
FFmpeg-kit builds that dependency in android.sh:
https://github.com/jmir1/ffmpeg-kit/blob/0f1bfb5d9ca2711e723406b25f0aa6a0c1500826/android.sh

Source-api formatting passes. App formatting stops only on the baseline's two
known lint errors; unrelated tracked formatting was restored to HEAD. No native
binary licence is claimed: source flags show the GPL path must be replaced and
the resulting artifacts audited before inclusion.

Post-format verification: `:source-api:compileDebugKotlin
:app:compileDebugKotlin :app:testDebugUnitTest --offline` passes (1m 45s).
No new tests claim extension runtime compatibility; no extension or episode has
been loaded yet. Existing host tests pass. No spike APK installed.

Native environment: no Android NDK installed. WSL lists only docker-desktop;
Docker Linux engine is unavailable, and the installed Desktop backend exits
during startup. The task's CLI startup wait was stopped after confirming that
exit. Docker configuration was not changed by this task. A working native build
environment is still needed; do not reset Docker to obtain one.

Pinned mpv Copyright documents an LGPL libmpv build path with `-Dgpl=false`,
while warning that linked libraries and distribution files still matter:
https://github.com/mpv-player/mpv/blob/d82701962f99051a18d65c215b70d41ebadd9a22/Copyright
The next native experiment must also disable FFmpeg GPL/version3 options,
record all dependency licences/configuration, keep compatible dynamic linking,
and verify ELF/ZIP 16 KB alignment before a device install. This is a build
plan, not a claim that a compliant artifact has been produced.

## GitHub Actions builder prepared

Owner explicitly selected GitHub Actions after choosing another Linux builder.
No self-hosted runners are configured. Prepared `anime-native.yml`,
`scripts/anime-native/build.sh` and `audit.py` locally. These use a hosted Ubuntu
24.04 runner, pinned dependency revisions and NDK r27c, disable GPL/native
postproc, build arm64 libraries and collect configuration/licence/ELF evidence.
Bash syntax, Python AST and whitespace checks pass. YAML parser validation and
semantic auditor tests remain pending. No remote build has run; no artifact
exists. Explicit permission to commit/push only these builder files has not
yet been requested or granted. Main and this API port remain uncommitted.
See main's `docs/CLAUDE_HANDOFF.md` for the full continuation checkpoint.

Validation update: official Actionlint 1.7.12 passes (publisher ZIP checksum
verified); all nine audit boundary tests pass. Added `test_audit.py` to the
workflow. Bash syntax, Python AST and whitespace pass. These supersede the
earlier parser/test-pending notes. Actual Linux execution and native artifact
verification remain pending. Explicit permission to commit/push only the four
build files was requested and is awaiting reply. No commits/pushes yet.

Owner approved builder-only commit/push. Commit
75a1b7c0998be3874f4344578c98a8e0802a7ec5 contains exactly the four builder
files and is pushed to origin/codex/anime-spike. Actions run 41644824316 is
confirmed in progress: https://github.com/Manojx2005/Tsuzuki/actions/runs/41644824316
No artifact verified yet. API-port and main changes remain uncommitted.
This supersedes earlier commit-approval/remote-execution-pending notes.

## First native artifact (2026-11-03)

Run 41644824316 (builder `75a1b7c09`) failed: FFmpeg did not configure, so mpv's
meson found no `libavcodec` and ndk-build found no `libswresample.so`. Commit
`5c351934b` allows `--enable-version3` (LGPLv3, needed by mbedTLS); GPL/GPLv3/
nonfree stay disabled. Run **41645408336** succeeded:
https://github.com/Manojx2005/Tsuzuki/actions/runs/41645408336
Artifact `anime-native-arm64-41645408336` (14.9 MB, kept 14 days) downloaded to
ignored `build/anime-native-artifact-41645408336/`. Evidence: FFmpeg reports
"LGPL version 3 or later" (`CONFIG_GPL 0`, `CONFIG_NONFREE 0`, `CONFIG_GPLV3 0`,
`CONFIG_VERSION3 1`); libmpv `gpl=false`. Local re-run of `audit.py` on the
download passes: 11 arm64 ELF libraries (avcodec, avdevice, avfilter, avformat,
avutil, swresample, swscale, c++_shared, mpv, player, xml2), every PT_LOAD
>= 16 KB, no postproc. Still required before shipping: per-dependency licence/
source review (LGPLv3 obligations: dynamic linking, offer of source, notices),
APK `zipalign -c -P 16`, and an actual playback test. Libraries are not yet
linked into the app; no episode played; Stage 1 stays unchecked.

## Stage 1 continuation (2026-11-04, spike only)

Owner limits this task to this worktree; main and upstream/picks are being used
by Claude and must not be changed. No commits or pushes without fresh approval.
No device installs authorized yet. New APKs will be built here.

Downloaded run 41645408336 afresh with gh run download into the new empty
`app/build/anime-stage1-20261104/audit-cfe06d3e/native-artifact/`. Treated all
artifact contents as data: no artifact scripts/binaries executed. No symlinks
or paths escaping the artifact root found. Re-ran the local
`scripts/anime-native/audit.py`: exit 0, 11 arm64 libraries. All PT_LOAD alignments
are 16,384 bytes except libmpv's first segment (65,536); all pass the 16 KB
alignment/congruence audit. Exact per-library hashes/alignments are in
`native-artifact/evidence/audit.json`; original uploaded audit saved separately
as `uploaded-audit-untrusted.json`. `summary.json` records the fresh result.

Flags: CONFIG_GPL=0, CONFIG_GPLV3=0, CONFIG_NONFREE=0, CONFIG_POSTPROC=0,
CONFIG_VERSION3=1. FFmpeg identifies itself as LGPL version 3 or later; libmpv
gpl=false. This contradicted the new request for VERSION3=0. Owner explicitly
answered **Allow audited LGPLv3 artifact** in this chat after the mismatch was
reported. That exception does not authorize any device install or GPL code.

FFmpeg configuration from the fresh artifact:

```
--target-os=android --enable-cross-compile --cross-prefix=aarch64-linux-android- --cc=aarch64-linux-android21-clang --pkg-config=pkg-config --nm=llvm-nm --arch=aarch64 --cpu=armv8-a --extra-cflags='-I/home/runner/work/Tsuzuki/Tsuzuki/build/anime-native-work/mpv/buildscripts/prefix/arm64/include ' --extra-ldflags='-L/home/runner/work/Tsuzuki/Tsuzuki/build/anime-native-work/mpv/buildscripts/prefix/arm64/lib -Wl,-z,max-page-size=16384' --enable-jni --enable-mediacodec --enable-mbedtls --enable-libdav1d --enable-libxml2 --disable-vulkan --disable-static --enable-shared --enable-version3 --disable-gpl --disable-nonfree --disable-postproc --disable-stripping --disable-doc --disable-programs --disable-devices
```

### Minimal debug integration

- `app/build.gradle.kts`: `-PanimeSpike=true` selects the separate ID
  `io.github.manojx2005.tsuzuki.anime.spike` and adds only the audited arm64
  artifact jniLibs directory to debug. Normal debug ID stays `.dev`. Release
  does not include the player bridge, entry points or native binaries.
- Debug manifest exposes `tsuzuki.anime.AnimeSpikeActivity` for an explicit
  adb component launch; the player Activity is not exported. No main navigator
  dispatch hooks were needed. Browser: installed/untrusted extensions, explicit
  signer trust dialog, source search/popular, anime, episodes, videos, first
  stream playback. No anime DB/library/history/downloads/trackers implemented.
- New player uses SurfaceView, verified TLS with AndroidCAStore CA bundle,
  per-video HTTP headers, play/pause, seek, back, audio focus, pause on background,
  and position/duration/codec diagnostics. No extension-provided mpv options
  are executed. External tracks, playlists, downloads and background playback
  remain out of scope.
- Copied `MPVLib.java` unchanged from aniyomiorg/aniyomi-mpv-lib
  `da7c885c18ae117ca27239c562ea7572fce3d4f5`, path
  `app/src/main/java/is/xyz/mpv/MPVLib.java`, MIT (Ilya Zhuravlev and sfan5,
  2016). Full notice retained as `docs/licenses/MIT-Aniyomi-MPV.txt`.
  Its original `is.xyz.mpv` package is required by the prebuilt JNI ABI;
  physically it sits under the spike's tsuzuki/anime/player/bridge directory.
  `AnimePlayerActivity.kt` adapts only minimal MIT BaseMPVView/MPVView lifecycle
  and option setup from that same pin; all browser logic is new Tsuzuki code.
  No GPL player/app source was copied.
- Native components: FFmpeg LGPLv3+ (owner-approved), mpv LGPL mode, FriBidi
  and libplacebo LGPL2.1+, dav1d BSD2, libass ISC, HarfBuzz Old MIT, libxml2 MIT,
  mbedTLS chosen Apache2, FreeType chosen FTL (not its GPL alternative), Lua
  MIT, libunibreak zlib, libc++ Apache2 with LLVM exceptions. Source revisions,
  patches, flags and upstream notices are retained in the artifact evidence.
  Supplemental pinned FreeType FTL, Lua 5.2.4 and libunibreak 6.1 notices were
  retrieved as data. The libc++ notice is from LLVM 18.1.8 (same licence family;
  it is not a claim that this is the exact NDK runtime source revision).
  Release distribution still needs a complete corresponding-source/relinking
  bundle and exact NDK licence/source provenance; this remains a local spike.

### Build / device readiness

First JDK 21 command with `-PanimeSpike=true` passed, exit 0:
`:source-api:compileDebugKotlin :app:assembleDebug :app:testDebugUnitTest`.
593 tests, 0 failures/errors/skips. Local native auditor's 11 tests also pass.
All edited Kotlin and Gradle files were formatted individually until IS CLEAN;
core's own one-file Spotless task was needed for core's build.gradle.kts
(app's hook does not match files in core). Java is not a Spotless target in
this repo and was left identical to upstream. XML reports IS CLEAN.

The first APK's ELF audit caught the worktree's old QuickJS alpha13 library
with 4 KB PT_LOAD, though zipalign itself passed. Applied the already-known
1.0.15 dependency fix to app and core/common within this spike only; rebuild
and full APK audit pending. Do not use the earlier APK for device testing.

Candidate extension downloaded locally, not installed:
AnimeGG 14.2, `eu.kanade.tachiyomi.animeextension.en.animegg`, current Yuzono
index `https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json`,
APK `https://raw.githubusercontent.com/yuzono/anime-repo/repo/apk/aniyomi-en.animegg-v14.2.apk`.
Source repo `https://github.com/yuzono/anime-extensions` (Apache2).
APK SHA256 `8f1316caa49c917dc93bb0ee114a67d18f1d7917e27c9f76ced7af46594656cf`.
apksigner verifies; signer SHA256
`cbec121aa82ebb02aaa73806992e0368a97d47b5451ed6524816d03084c45905`.
Manifest feature/class/lib14 metadata match loader support. Newest index is
saved under evidence as extension-index.json. Neither source nor signer has
been auto-trusted. No APK or extension device install is authorized yet.

Owner will connect another device in about 15 minutes. Tablet remains untouched;
no device input, install, main app restart, data backup or restoration performed.

### Ready for owner-approved test device

Rebuild after QuickJS 1.0.15: JDK 21 required command passed, exit 0,
593 tests, no failures/errors/skips. All **18** .so files in the new arm64 APK
pass the Python ELF64 PT_LOAD alignment/congruence check. `zipalign -c -P 16`
passes, exit 0 (`apk-audit.json`, `zipalign.log`). Native DT_NEEDED inspection
also finds every required library packaged or supplied by Android; results
in `native-needed.json`. Native audit tests: 11/11 pass.

Newest APK: `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`.
Application ID: `io.github.manojx2005.tsuzuki.anime.spike`.
APK SHA256: `d45b3cefce5f5c4487f002cedcdf10887ea322a2c975bc167822e8028a0939d8`.
To build: set JAVA_HOME to Android Studio's JDK21 and run
`./gradlew :source-api:compileDebugKotlin :app:assembleDebug :app:testDebugUnitTest -PanimeSpike=true`.
The audited native artifact directory must exist at the debug jniLibs path
in app/build.gradle.kts; it is intentionally not committed.

Owner notified of build/audit success. Explicit approval requested for both
this separate-ID app APK and AnimeGG 14.2 on the *other* device when connected.
Tablet is excluded from that request. No device installs or test input yet.
Stage 1 remains incomplete until actual picture/sound playback is verified.

Owner explicitly approved both APK installs on the other device in chat. Approval excludes tablet R52Y90HA77L. Waiting for that device; verify its serial and existing spike-package state before installing. No further approval is needed for these two installs within that scope.

Native notices/configuration/source-pin assets were bundled in the opt-in spike APK. Final rebuild passed (exit 0, 593 unit tests). All 18 ELF libraries pass alignment again, and the 11 artifact library hashes match exactly. Latest APK SHA256 supersedes the earlier hash: `8541d7a6ee970fc0cb0eb0645a270da30c6c84b52e3ef657e02cc100edaa79e8`. Final zipalign result is recorded in zipalign.log. Still no second device visible and no installation/input performed.

### S22 installation and integration testing (2026-11-04)

Other device connected: Galaxy S22 RZCWA139X1X, API 36. Before installs,
`pm path` found neither the spike ID nor AnimeGG installed. Phone was awake
on its ordinary Home screen, no permission prompt. Installed both approved
APKs successfully. Existing `.dev` app was neither replaced nor launched;
no existing reader DB/preferences were changed. Tablet received no input or
install, so tablet backup/reinstall/restore was unnecessary.

Observed loader Untrusted state; opened its own signer dialog and compared
SHA256 with apksigner's verified fingerprint, then trusted that approved
extension through the explicit app UI (not auto-trusted in code). AnimeGG
loaded successfully. Popular catalogue fetched real titles, One Piece
returned episode rows, and episode 1180 returned [SUBBED] 480p/720p/1080p
videos. Screenshots: phone-before.png, phone-extensions.png, phone-trust.png,
phone-source.png, phone-popular.png, phone-videos.png in the evidence folder.

Fixed browser error: getVideoUrl was called even for already-populated direct
videoUrl. It now resolves modern lazy Video metadata and uses populated direct
URLs, falling back to legacy URL resolution only when absent. Rebuild/tests
and APK alignment checks passed before updating the spike app.

MPV Activity opened with no crash, but HTTP 400 prevented decoding. Added
sanitized mpv warning/error output (stream URLs redacted). Diagnostic error
identified incorrect percent-length quoting in the HTTP string-list option;
mpv's own pinned manual specifies raw single-item `-append`. Fix now appends
each header individually, preserving embedded commas/backslashes. Disabled
unused youtube-dl fallback (no external downloader is bundled or needed).
Testing this corrected build next. Not yet a successful episode playback.

Phone media volume was **0/15** before sound testing; audio route was speaker.
Volume has not been changed yet. Restore zero if raised for audible testing.
Crash buffer checked after initial installs/loader/first player launch: empty.

Final installed-APK verification: adb sha256sum of S22 spike base.apk matches local audited newest APK exactly: `67bea8caaa5f140474b1b123e75e7d39ed56af1a38c18535fb22a48f5af4ae0f`. Media volume rechecked 0/15. Final zipalign verification successful. No commits/pushes performed.

### Stage 2 working inventory for Claude

Do not overwrite main files with these older-worktree versions. New files can
be copied; existing-file changes require merging only the hooks/keys listed.
No Stage 1/2 commit exists yet (only the previously approved builder commits).

- NEW data/.../data/tsuzukiAnime.sq and migrations/36.sqm: separate anime and
  episode identities, constraints/indexes, favorite/watch status, real counts,
  metadata upserts that preserve seen/bookmark/progress/date_fetch.
- NEW app/.../tsuzuki/anime/data/AnimeLibraryStore.kt: transactional add/remove,
  refresh and observable counts/library. No DI module replacement needed.
- NEW app/.../tsuzuki/anime/library/AnimeLibraryUi.kt: token/theme based three-way
  media selector, watch-list pills, real cover grid/counts and progress; uses
  existing MangaCover (Dot greyscale), TsuzukiPillRow and TsuzukiProgressBar.
  Hidden-manga preference respected; legacy themes skip anime dispatch.
- Existing NovelsTab.kt: top anime dispatch, type-specific ViewModel keys and
  Tsuzuki reader-type selection. Existing LibraryTab.kt: top anime dispatch.
  Existing TsuzukiLibraryMediaSwitch.kt delegates to the new shared selector.
  **Merge warning:** main already has combined-library filtering and ViewModel
  cleanup improvements absent here. Retain those; merge anime selection into
  main's existing filter/host rather than replacing it with this baseline's
  readerType helper. This prototype shows separate Novels/Manga/Anime in Tsuzuki
  themes while legacy joined-library mode retains All.
- Debug AnimeSpikeActivity.kt: explicit add/remove, manual watch-list status,
  opens saved anime by ID; details response URL is normalized back to the
  original source/catalogue URL for stable identity. Browser error text stays
  visible after a result callback fails. Theme/library launch helpers affect
  only the separate spike package.
- Six new tsz_anime* keys in base, ja, zh-rCN, ko, es, pt-rBR, fr, de. Merge keys
  only; main has many additional strings absent in this older tree.
- NEW scripts/anime-stage2/test_schema.py: 10 SQLite regression tests pass.
  SQLDelight generation and migration verification pass. A disposable copy of
  the actual pre-test Stage 1 DB also preserved every row in all 9 existing
  tables after migration 36 (stage2-migration-fixture/results.json).

Pre-device-test backup: stage2-backup/manifest.json verifies 8 DB/WAL/SHM and
preference files from the S22 spike ID. No WorkManager database existed. Backup
uses run-as copies into the app's own external cache, permissions relaxed only
on those temporary copies, then adb pull. Original file contents/permissions
remain unchanged. Use copies for SQLite inspection so originals cannot be
checkpointed. Final original backup hashes rechecked before installation.
Actual device migration/library/restart checks and restoration still pending.

### Stage 2 device checkpoint and owner setup change

Installed initial passing Stage 2 APK only in S22's separate spike ID. Real
AnimeGG One Piece added successfully, with 1,167 episode rows. Dot library
visually shows its real cover (greyscale), total/media/category counts and
0/1167 episode state; no fake watch progress or NEW badges. The first grid
rendered two columns on this S22 density; correcting its adaptive minimum to
96 dp for three phone columns and adding selected-tab semantics.

First MainActivity launch required onboarding/storage selection. No system
picker or permission prompt was tapped by Codex. Owner explicitly said they
selected storage and completed setup; library then appeared. A temporary
code change to skip setup in the debug helper was REMOVED BEFORE INSTALL,
so no installed APK bypassed normal onboarding. Debug Default-theme shortcut
retained for stock-theme checks.

**Restore baseline changed by owner action:** preserve current preferences
from stage2-owner-prefs/manifest.json (5 files, hashes verified), including the
new storage URI/setup completion. Restore DB/WAL/SHM from pre-test
stage2-backup/manifest.json. Combined expected hashes in
stage2-restore-manifest.json (8 files). Do not restore the older preferences
over the owner's explicit storage/setup choice. Any additional DB/pref files
created solely during this test will be removed when restoring this spike ID.
Main tablet/phone packages and worktrees have not been modified.
