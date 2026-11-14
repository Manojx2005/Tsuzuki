# Anime: what Aniyomi does that Tsuzuki doesn't yet — instructions for Codex

Written by Claude on 2026-11-12 from a side-by-side read of Aniyomi and the newest anime branch
(`tsuzuki/a6.3`, plus the uncommitted A6.4 work in `.worktrees/a6.4`). The cards are in `docs/HANDOFF.md`
("Batch AY"); this file is the detail each card points to. Read §1–§3 once, then only your card's section.

Nothing here has been built or device-tested. Every "Tsuzuki does X" line was read from the code, with the file
named, so check it still holds before you change it.

## 1. The reference

- Aniyomi: `https://github.com/aniyomiorg/aniyomi`, commit `97414446b8a95994c72dd33c41c971a89d4d25b8`
  (2026-09-14, "Add support for extensions-lib v17"). Pin to this commit so line numbers below match.
- Get it read-only, **outside** the repo and every worktree:
  `git clone --filter=blob:none https://github.com/aniyomiorg/aniyomi "$TEMP/aniyomi-ref" && git -C "$TEMP/aniyomi-ref" checkout 97414446b`.
  No network in your sandbox → set the card `blocked: needs the Aniyomi checkout` and say so; don't work from memory.
- Licence: Aniyomi is Apache-2.0, like Tsuzuki. You may adapt its code. When you copy more than a few lines,
  keep any licence header the file has (`AniyomiMPVView.kt` carries an mpvKt Apache header) and add
  `// Adapted from Aniyomi (Apache-2.0): <path>` at the top of the Tsuzuki file. Add Aniyomi to the About credits
  if it isn't there. Still no GPL code (PLAN.md §1).
- Do **not** add Aniyomi's dependencies (`aniyomi-mpv-lib`, `ffmpeg-kit`, torrent server, cast). Tsuzuki has its own
  mpv build in `:anime-player` and its own downloader. Port behaviour, not libraries.

Paths below are relative to `app/src/main/java/eu/kanade/tachiyomi/` unless they start with a module name.
"A:" means Aniyomi, "T:" means Tsuzuki.

## 2. Where things stand (2026-11-12)

- `main` has no anime beyond A1.5/A2.2. The whole anime chain (39 commits, A2.3 → A6.3) sits on `tsuzuki/a6.3`,
  unreviewed: RV3 has not started because RV2 is still open.
- A6.4 is `codex-working` with uncommitted files in `.worktrees/a6.4`. Finish and commit it before any Batch AY card.
- A6.5–A6.9 are `ready`. Batch AY changes their order and adds detail (§6).
- 1,071 unit tests pass on `tsuzuki/a6.3` (Codex's count; not re-run by Claude).

## 3. Rules for every Batch AY card

**Code**

1. New code goes in `tsuzuki/anime/...` or `presentation/tsuzuki/anime/...`. No new upstream-file edits unless the
   card names the file; if you need one, it is a one-line hook and the file goes on the AGENTS.md list.
2. All mpv calls stay behind `AnimePlayerBackend` (`tsuzuki/anime/ui/AnimePlayerModule.kt` /
   `player/MpvAnimePlayerLauncher.kt`). `AnimePlayerModel` and the Compose code never import `is.xyz.mpv`.
   That keeps the model unit-testable with a fake backend.
3. mpv callbacks arrive on mpv's thread. Hop to main exactly as `AnimePlayerModel.onMain` does; never touch
   Compose state or `MutableStateFlow.update` with UI-affecting logic from the callback thread directly.
4. Every call into an extension goes through `callAnimeSource { }` (`AnimeStreamResolver.kt`) so a
   `LinkageError` becomes a normal error, and runs on `Dispatchers.IO`.
5. `CancellationException` is always rethrown. A caught exception is logged with `logcat(LogPriority.WARN, e)`;
   nothing is swallowed silently.
6. Pure logic (selection, sorting, parsing, merging) lives in top-level functions or small classes with no
   Android types, with a unit test that fails before your change.
7. UI: read the board first (`docs/design/boards/png/10-anime/`, Dot then Colour), tokens +
   `MaterialTheme.typography`, existing building blocks (`TsuzukiPill`, `TsuzukiTile`, `tsuzukiGroupedTile`,
   `TsuzukiSectionLabel`). A control the board doesn't show goes in the existing player options sheet
   (`player/AnimePlayerSheets.kt`) or the player settings page (`player/AnimePlayerSettingsScreen.kt`) as a normal
   row. If neither fits, `blocked: needs a board`.
8. Strings in all 8 locales, same placeholders, in the existing `anime_*_strings.xml` files.
9. Everything stays behind `TsuzukiAnime.enabled`.
10. Before finishing: per-file Spotless twice (restore untouched files with `git show HEAD:<file> > <file>`),
    `./gradlew :app:assembleDebug :app:testDebugUnitTest`, Result in HANDOFF, commit on `tsuzuki/<card>`, `review`.

**Device testing**

**Owner 2026-11-12: anime-lane implementers test their own cards on the tablet and the S22** (AGENTS.md "Test on
devices"). Write the card's **device script** first (numbered steps, each with the expected result, using the AY0
fixture wherever possible so it doesn't depend on a live site), run it yourself, fix what fails, and put the
script with the observed result of every step and the evidence paths in Result. The review lane re-runs the same
script on the merged branch. A step you could not run (device missing, owner using it, no headphones) is written
as "not checked", never as passed.

Limits that still hold: only `R52Y90HA77L` and the S22; never `R5GL23ZYE1V`; never `main`; other lanes' cards
stay off adb. **One thread on the devices at a time:** before the first adb command write
`Devices: in use by <card> since <HH:MM>` on the "Device lock" line in HANDOFF "Batch AY", and set it back to
`Devices: free` when done. If it is taken, keep working on code and tests and try again later; don't install
over another thread's build. Evidence goes to `app/build/review-evidence/<card>/` in your worktree.

Implementer and review lane, for every Batch AY card:

```sh
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
"$ADB" devices -l
# Tablet: R52Y90HA77L over USB. The S22 is on wireless debugging (owner, 2026-11-12), so it shows as
# <ip>:<port> or adb-RZCWA139X1X-…; if it is missing, ask the owner for the IP:port from
# Settings › Developer options › Wireless debugging and run:  "$ADB" connect <ip>:<port>
# Always confirm which device you hold before installing:
"$ADB" -s <serial> shell getprop ro.serialno     # RZCWA139X1X = S22, R52Y90HA77L = tablet. Never R5GL23ZYE1V.
```

1. Anime needs the **debug** APK (`app-arm64-v8a-debug.apk`, id `io.github.manojx2005.tsuzuki.dev`). Export an
   in-app backup of the `.dev` app first; restore it if the card changed data.
2. Start the fixture server on the PC and forward it (AY0): `python scripts/anime-fixture/serve.py` then
   `"$ADB" -s <serial> reverse tcp:8765 tcp:8765` on **each** device. Wireless adb supports `reverse`.
3. Clear and capture logs around each step: `"$ADB" -s <serial> logcat -c`, do the step, then
   `"$ADB" -s <serial> logcat -d -s TsuzukiMpv:V mpv:V > <evidence>/<card>-<device>-<step>.log` and
   `"$ADB" -s <serial> logcat -d -b crash` (must be empty).
4. Screenshot each checked state: `"$ADB" -s <serial> exec-out screencap -p > <evidence>/<card>-<device>-<what>.png`
   and look at it. A playing video is a `SurfaceView`; if the screenshot shows a black video area, that is the
   capture, not a bug — confirm playback from the position label advancing in two screenshots 5 s apart.
5. Both devices, Dot then Colour, landscape and (tablet) portrait. One real extension from the anime repo at the
   end as a smoke test: browse → episode → plays within 15 s → resume works. A live site failing is recorded, not a
   card failure, when the fixture passes.
6. Leave the tablet on Dot Home, `svc power stayon usb` on both, remove `reverse` (`"$ADB" reverse --remove-all`).

## 4. Findings

| # | Area | Aniyomi | Tsuzuki now | Effect on the user | Card |
|---|---|---|---|---|---|
| 1 | Hoster loading | A: `ui/player/loader/HosterLoader.kt`, `EpisodeLoader.kt`. Hosters load in parallel; a hoster that throws becomes `HosterState.Error` and the others continue; `Hoster.lazy` hosters wait until picked | T: `tsuzuki/anime/AnimeStreamResolver.videos()` is `getHosterList(ep).flatMap { it.videoList ?: getVideoList(it) }`: sequential, and one throwing hoster fails the episode | One dead mirror = "can't play", and slow starts on sources with 5+ hosters | AY1 |
| 2 | Source's own order | A: `source.run { hosters.sortHosters() }`, `videos.sortVideos()`; first `Video.preferred` wins | T: `player/AnimePlayerPolicy.rankAnimeVideos` sorts by resolution only | The extension's "preferred server / quality / sub-dub" settings do nothing | AY1 |
| 3 | v16 extensions without hosters | A: `EpisodeLoader.checkHasHosters` (reflection: does the class override `getHosterList`/`hosterListRequest`/`hosterListParse`?) → else `getVideoList(episode)` | T: `libVersion >= 16` always calls `getHosterList`, whose default `hosterListParse` throws `UnsupportedOperationException` | A lib-16 extension that only implements the episode video list never plays | AY1 |
| 4 | Headers | A: `PlayerActivity.setHttpOptions`: `video.headers ?: source.headers` | T: `MpvAnimePlayerLauncher.load` uses `video.headers` only | Hosters that need the source's Referer/User-Agent return 403 | AY1 |
| 5 | `Video.mpvArgs`, `initialized` | A: `mpvArgs` passed as per-file `loadfile` options; `resolveVideo` skipped when `initialized` | T: both ignored | Streams that need demuxer options fail; double resolve | AY1 |
| 6 | Extension settings | A: `ui/browse/anime/extension/details/AnimeSourcePreferencesScreen.kt` for `ConfigurableAnimeSource` | T: no anime file references `ConfigurableAnimeSource` | No way to set an anime source's domain, quality, server, login | AY2 |
| 7 | Subtitle fonts | A: copies `subfont.ttf` into the mpv config dir, user fonts into `fonts/`, sets `sub-fonts-dir`/`osd-fonts-dir` | T: `anime-player/src/main/assets` has only `cacert.pem` | Text subtitles (SRT/VTT, ASS without embedded fonts) may render as nothing or boxes. **Verify first** (AY3 step 1) | AY3 |
| 8 | External subtitle/audio URLs | A: `sub-add`/`audio-add` for any URL | T: `MpvAnimePlayerBackend.tracks()` adds only `toHttpUrlOrNull()` URLs | `data:`, `file:`, `content:` subtitles are dropped | AY3 |
| 9 | Subtitle style | A: `settings/SubtitlePreferences.kt`: font size, bold/italic, colours, border, position, override ASS, secondary subs | T: scale and delay only | Can't fix unreadable subs | AY3 |
| 10 | Media session, noisy | A: `PlayerActivity.setupMediaSession()` + `ACTION_AUDIO_BECOMING_NOISY` receiver | T: audio focus only | Unplugging headphones keeps playing through the speaker; headset/Bluetooth buttons do nothing | AY4 |
| 11 | Buffering state | A: observes `paused-for-cache`, `seeking`, `pause` | T: observes position, duration, cache time, eof, tracks | A stalled stream looks frozen, no spinner | AY4 |
| 12 | Chapters | A: `chapter-list` + `Video.timestamps` merged with AniSkip in `utils/ChapterUtils.mergeChapters`; AniSkip can be off when the file has chapters | T: AniSkip segments only | No chapter marks; source-supplied OP/ED stamps ignored; AniSkip can fight embedded chapters | AY5 |
| 13 | Extension ABI | Extensions are compiled against `aniyomiorg/extensions-lib` stubs and link against app classes at runtime | T: one crash already found and fixed by hand (`RequestsKt.get$default`, `b87145ef8`) | Random `NoSuchMethodError` per extension, worse in minified builds | AY6 |
| 14 | Downloads | A: `data/download/anime/AnimeDownloader.kt` (ffmpeg-kit) muxes external subs/audio into the file and stores `mpvArgs` in metadata | T: own OkHttp HLS/MP4 downloader (`download/AnimeVideoTransfer.kt`, `OfflineHls.kt`) | Check that offline episodes keep external subtitles, extra audio and headers | AY7 |
| 15 | Aniyomi backups | A: `data/backup/models/Backup.kt` — `backupAnime` = proto 3, `backupAnimeCategories` = 4, `backupAnimeSources` = 103 | T: `tsuzuki/anime/backup/AnimeBackupWire.kt` (own numbers; check) | Someone moving from Aniyomi can't bring their anime library | AY8 |

Already matching Aniyomi (don't redo): mpv init order and base options (`vo=gpu`, `profile=fast`, `hwdec`,
`tls-ca-file`, 64 MB demuxer cache), extension loader constants and `SUPPORTED_LIB_VERSIONS = 14, 16, 17`,
trust/signature flow, AniSkip, PiP, audio focus with ducking, double-tap seek and brightness/volume drags,
seasons (`FetchType`), fillermark stored.

Left out on purpose: torrents, casting, custom Lua buttons, `gpu-next`, the statistics overlay.

## 5. Cards

### AY0 Fixture server + debug fixture source (T3)

Everything after this is tested against it, so it comes first. It also covers the "fake-source harness" A6.9 asks for.

1. `scripts/anime-fixture/serve.py` (Python 3 stdlib only, port 8765, binds 127.0.0.1) and
   `scripts/anime-fixture/make-media.sh` (uses the PC's `ffmpeg` to write small files into
   `scripts/anime-fixture/media/`, which is git-ignored: 60 s 720p H.264/AAC `ok.mp4`; a 3-variant HLS
   `master.m3u8`; an MKV with two audio languages (`jpn`, `eng`), an embedded ASS track and 3 chapters; `en.srt`,
   `ja.srt` (Japanese text), `en.vtt`). No `ffmpeg` on the PC → `blocked`, ask the owner to install it.
   Routes: `/media/*` plain; `/referer/*` → 403 unless `Referer: http://127.0.0.1:8765/` ; `/ua/*` → 403 unless the
   User-Agent contains `TsuzukiFixture`; `/fail/*` → 500; `/slow/*` → 20 s delay before the first byte;
   `/stall/*` → sends 2 s of data then stops for 30 s; `/api/*` → the JSON the fixture source reads.
2. Debug-only source, `app/src/debug/java/eu/kanade/tachiyomi/tsuzuki/anime/debug/FixtureAnimeSource.kt`, an
   `AnimeHttpSource` + `ConfigurableAnimeSource` with a fixed id, registered by `AnimeSourceManager` through a
   debug-source-set hook (main code must compile without it; use the existing debug/main split pattern A3.0 left).
   One anime per case, named for the case:
   `01 plain mp4`, `02 hls variants`, `03 first hoster throws`, `04 lazy hoster`, `05 preferred video`,
   `06 needs source headers` (video.headers = null), `07 needs video headers`, `08 mpvArgs`,
   `09 external srt + vtt + ja`, `10 mkv tracks + chapters`, `11 timestamps` (Video.timestamps OP/ED),
   `12 all hosters fail`, `13 slow`, `14 stall mid-stream`, `15 sortVideos honours the quality preference`.
   Its preference screen has one list preference "Preferred quality" (1080/720/480) used by `sortVideos`.
3. Debug network config: allow cleartext to `127.0.0.1` only, in the debug source set.
4. Tests: a JVM test starts nothing on the network; it checks the source's hoster/video construction per case.
5. Result: the exact commands to start the server and the list of cases.

Device script: server + `reverse`; Browse › Anime shows "Tsuzuki Fixture" in debug only (absent in
the preview APK); case 01 plays.

### AY1 Load streams the way Aniyomi does (T4)

Read A: `ui/player/loader/EpisodeLoader.kt`, `HosterLoader.kt`, `PlayerActivity.setVideo` (L1064–1140) and
`setHttpOptions` (L1204–1218). T files: `tsuzuki/anime/AnimeStreamResolver.kt`, `player/AnimePlayerPolicy.kt`
(`rankAnimeVideos`, `AnimeStreamQueue`), `player/AnimePlayerModel.kt` (`openEpisode`, `tryNextStream`),
`player/MpvAnimePlayerLauncher.kt` (`load`), and `download/AnimeDownloads.kt` (it uses the same resolver).

1. `AnimeStreamResolver.hosters(source, libVersion, episode): List<Hoster>`:
   lib < 16 **or** the class doesn't override a hoster method (port `checkHasHosters`) →
   `getVideoList(episode)` then `sortVideos()` then `toHosterList()`; else `getHosterList(episode)` then
   `sortHosters()`. Call the extension functions inside `with(source as AnimeHttpSource)`.
2. `AnimeHosterStates` (pure, no Android): `Idle | Loading | Ready(videos, states) | Error` per hoster, plus
   `selectBestVideo` ported from `HosterLoader` (first `preferred`, else first with a non-empty URL).
3. Loading: all non-lazy hosters in parallel (`async` on IO under one `coroutineScope`); each in its own
   try/catch → `Error`; the first resolved `preferred` video starts playback and cancels the rest; otherwise walk
   `selectBestVideo`, marking a video `Error` when `resolveVideo` fails, until one resolves. Resolve only when
   `!video.initialized`; mark `initialized = true` after.
4. **Order decision** (this replaces resolution-only ranking): the user's Tsuzuki quality choice applies only when
   they picked one in the player (`quality != 0`); with "Auto" the source's order stands. Keep the resolution
   fallback for the mpv-error path. Write this rule in a KDoc on the function and test it.
5. Headers: `video.headers ?: (source as? AnimeHttpSource)?.headers`. Pass them as mpv does now (Referer,
   User-Agent, then the rest via `http-header-fields`), and clear them before each load.
6. `mpvArgs`: `loadfile <url> replace 0 <k="v",…>` as Aniyomi does. Check the vendored mpv accepts the index
   argument (mpv ≥ 0.38); if not, use the 3-argument form and set each option with `file-local-options/<k>`.
7. URL policy: accept `http(s)`, `file:`, `content:` (through a file descriptor, see A: `parseVideoUrl` /
   `resolveUri`) and `data:`. Magnet/torrent → a clear "not supported" error state, not a crash.
8. The quality sheet lists hosters and their videos (A: `controls/components/sheets/QualitySheet.kt`), lazy
   hosters load on tap, failed hosters show as failed. Layout: the existing sheet rows, no new layout.
9. Downloads use the same selection, so a download gets the same video the player would.

Tests: hoster-method detection with three fake sources; one hoster throws → others play; lazy not loaded until
asked; preferred beats order; `sortVideos` honoured on Auto and overridden by a manual quality; header fallback;
`mpvArgs` formatting (quotes, commas); cancellation leaves no running child.

Device script: fixture cases 03, 04, 05, 06, 07, 08, 12, 15 — each with the expected result and, for 06/07, the
server log line showing the header arrived. Case 12 ends in the error state with Retry, no crash. Time to first
frame for case 01 on both devices (from logcat timestamps, `loadfile` → `FILE_LOADED`), recorded.

### AY2 Anime source settings (T3)

Read A: `ui/browse/anime/extension/details/AnimeSourcePreferencesScreen.kt` and T's manga version
`ui/browse/extension/details/SourcePreferencesScreen.kt`.

1. An anime `SourcePreferencesScreen` for `ConfigurableAnimeSource`: build the `PreferenceScreen` with the
   source's `setupPreferenceScreen`, backed by the source's own `SharedPreferences` (`source_<id>`, same key rule
   as Aniyomi so restored settings line up). Prefer reusing the manga screen with the source passed in over a
   copy; if the manga screen is upstream code, add the smallest parameter and list the file in AGENTS.md.
2. Entry points: the gear on each source row in the anime extension details (A3.3's screen) and a "Source
   settings" item in the anime catalogue overflow (`ui/AnimeCatalogueActivity.kt`).
3. After a preference changes, the next catalogue load and the next episode open use it (no app restart). Check
   whether the loaded source instance caches its preferences; if so, refresh `AnimeSourceManager`.
4. Anime source preferences are included in backup/restore the way manga `backupSourcePreferences` are.

Tests: screen builds for a fake configurable source; the stored key name; a non-configurable source shows no gear.
Device script: fixture source › settings › set "Preferred quality" 480 → case 15 plays the 480 variant (resolution
label + server log); restart the app, the setting is still there; Dot and Colour screenshots of the settings page.

### AY3 Subtitles that always render, and subtitle style (T3)

1. **Verify before changing anything**: on the S22 debug build, fixture case 09 with `en.srt`, then `ja.srt`.
   Run this check yourself before writing code and save a screenshot of each. If both render, skip step 2 and
   say so in Result.
2. Fonts: put a font in the mpv config dir as `subfont.ttf` and set `sub-fonts-dir`/`osd-fonts-dir` to a
   `fonts/` folder there (A: `PlayerActivity.copyAssets`, `copyFontsDirectory`). Use fonts the app already ships
   and whose licences are in `docs/licenses/`: a Latin face plus the Japanese subset (`res/font/lnr_jp_*.ttf`),
   copied once (size check, like `cacert.pem`). Don't add a new font file without an OFL/Apache licence recorded
   in `docs/licenses/`.
3. `sub-add`/`audio-add` for any non-blank URL, not only HTTP; pass the title and language like Aniyomi
   (`sub-add <url> auto <title> <lang>`). A failing add is logged and skipped.
4. Style settings in `AnimePlayerSettings` + the player settings page (rows, existing widgets): font size, bold,
   text colour, border colour and size, background, vertical position, "override styled (ASS) subtitles". Map to
   the mpv options in A: `AniyomiMPVView.setupSubtitlesOptions`. Apply live through `applySettings`.
5. A6.4's language choice keeps working: forced/signs tracks are not picked as the main subtitle unless nothing
   else matches (A: `utils/TrackSelect.kt` for the matching rules: ISO 639-1/-2 code, native and English name).

Tests: settings ↔ mpv option mapping, bounds, URL acceptance, track choice with forced tracks.
Device script: case 09 (SRT, VTT, Japanese) and case 10 (embedded ASS) show text; each style setting visibly
changes the SRT; "override ASS" changes case 10; screenshots with the subtitle visible in Dot and Colour.

### AY4 Playback session: headphones, media buttons, buffering (T3)

1. `MediaSession` (platform or media3-session if already a dependency; no new library otherwise) owned by the
   player activity/model: play, pause, next, previous, seek; metadata = anime title, episode name, duration;
   state updated from the model's state. Released in `onCleared`/`onDestroy`.
2. `ACTION_AUDIO_BECOMING_NOISY` → pause (register while started, unregister on stop; use
   `ContextCompat.registerReceiver` with `RECEIVER_NOT_EXPORTED`).
3. Observe `paused-for-cache` and `seeking` in the backend → `AnimePlayerState.buffering`; the centre control
   shows the existing loading indicator while buffering; `playing` stays true so the screen stays on.
4. `force-media-title` = "Title — Episode" (A: `setMpvMediaTitle`), so PiP and the session show it.
5. Hardware keys: space/media play-pause, left/right seek, handled in the activity (A: `onKeyDown`); useful with
   the tablet's keyboard cover.

Tests: state → `PlaybackState` mapping; noisy → paused; buffering flag transitions with the fake backend.
Device script: play, then `"$ADB" shell input keyevent KEYCODE_MEDIA_PAUSE` / `KEYCODE_MEDIA_PLAY` /
`KEYCODE_MEDIA_NEXT` and screenshots showing the state change; `"$ADB" shell dumpsys media_session | grep -A8 tsuzuki`
shows the session and metadata; fixture case 14 shows the spinner during the stall and resumes by itself; the
noisy pause needs real headphones — if none are connected, mark it "not checked" (don't claim it).

### AY5 Chapters and source timestamps (T3)

1. Backend: read mpv `chapter-list` after load → `AnimePlayerState.chapters` (title, start).
2. Merge with `Video.timestamps` and AniSkip by porting `utils/ChapterUtils.mergeChapters` as a pure function.
3. Seek bar: chapter ticks; current chapter name beside the time (board `10-anime/Player`; if it shows no
   chapter label, ticks only). The existing skip button also works for a timestamp of type opening/ending/recap.
4. Setting "Don't use AniSkip when the video has chapters" (default on, as in Aniyomi).

Tests: merge cases (overlap, AniSkip inside a chapter, out-of-range stamps, zero duration).
Device script: case 10 shows 3 ticks and the chapter name; case 11 shows the skip button at the OP stamp without
AniSkip turned on; with AniSkip on and the new setting on, case 10 makes no AniSkip request (server/log evidence).

### AY6 Extension ABI audit (T4)

The app must offer every class and method the extension stubs promise, with the same JVM signature.

1. Get the stubs: `https://github.com/aniyomiorg/extensions-lib` (branches for lib 14, 16, 17). List every
   public class/method an extension can call: `eu.kanade.tachiyomi.network.*` (`GET`, `POST`, `awaitSuccess`,
   `NetworkHelper`, interceptors), `eu.kanade.tachiyomi.util.*` (`asJsoup`, `parseAs`, …), `animesource.*`,
   `uy.kohesive.injekt`, `rx`, preference classes, `AppInfo`.
2. `AnimeExtensionAbiTest`: a table of (class, method, parameter types, return type) checked by reflection against
   the app's classes — extend `AnimeExtensionCompatibilityTest`. Every miss is either fixed by adding a bridge
   overload in a Tsuzuki file (as `b87145ef8` did in `Requests.kt`) or listed in Result as "not offered" with the
   reason.
3. Minified builds: compare `app/proguard-rules.pro` with Aniyomi's. Missing today:
   `-keep,allowoptimization class eu.kanade.tachiyomi.network.RequestsKt { public protected *; }`; check the
   existing network/util keeps already cover it before adding. Then run the same reflection table against the
   **preview** APK's dex (`apkanalyzer dex packages` or `dexdump`) in a script, `scripts/anime-abi-check.sh`,
   so R8 renames are caught without a device.
4. Fixture: the fixture source calls a handful of these helpers on purpose.

Device script: preview APK with the anime switch on, three real extensions of different lib versions
(14, 16, 17): browse, details, episode list, play. `logcat | grep -E 'NoSuchMethod|NoClassDef|AbstractMethod'`
is empty.

### AY7 Offline episodes keep their tracks (T3)

Read A: `data/download/anime/AnimeDownloader.kt` for **what** it preserves; keep Tsuzuki's own downloader.
1. A download saves external subtitle and audio tracks next to the video and a small sidecar (JSON) with their
   language/title, the `mpvArgs`, and the chosen quality; `AnimeDownloads.offlineVideo` rebuilds the `Video` from it.
2. Headers from AY1's fallback are used for every segment and track request.
3. Table in Result: container/case → supported or not (progressive MP4, HLS TS, HLS fMP4, AES-128, byte-range,
   DASH). Unsupported cases fail the download with a readable reason, never a broken file.
Tests: sidecar round-trip; OfflineHls cases already there stay green.
Device script: download cases 02 and 09, airplane mode, play: video, subtitle choice and audio choice all work.

### AY8 Restore an Aniyomi backup (T3)

1. Check first: does `AnimeBackupWire` read Aniyomi's fields (`backupAnime` = 3, `backupAnimeCategories` = 4,
   `backupAnimeSources` = 103; A: `data/backup/models/BackupAnime.kt`)? If it already does, write the test and
   close the card.
2. Otherwise add read-only decoding of those fields in the Tsuzuki backup reader: anime, episodes (seen, position),
   categories → anime shelves, tracking, history. Tsuzuki's own backup format doesn't change. Clashing proto
   numbers with Tsundoku's fields → detect an Aniyomi file by its fields and decode it with a separate schema.
3. Restore preview shows the anime count; anime restore only when `TsuzukiAnime.enabled`.
Tests: a small synthetic Aniyomi backup built in the test with Aniyomi's proto numbers.
Device script: restore the synthetic file from the test resources on a scratch install.

## 6. Changes to the existing A6 cards

- **Order:** A6.4 (finish) → AY0 → AY1 → AY2 → AY3 → AY4 → AY6 → A6.5 → AY5 → A6.8 → AY7 → A6.6 → A6.7 → AY8.
  **A6.9 is replaced by AY0 + AY1 + AY4** (its remaining item, process death in the player, moves into AY4's tests).
- **A6.4:** use the matching rules of A: `utils/TrackSelect.kt` (`containsLang`: `locale.isO3Language`,
  `locale.language`, native and English display names) rather than a hand-made tag table.
- **A6.5 external player:** port A: `ui/player/ExternalIntents.kt`. It already has the per-player extras
  (`title`, `position`, `return_result`, `headers` as a flat String array, `subs`/`subs.name`/`subs.enable`,
  `secure_uri`, `android.media.intent.extra.HTTP_HEADERS`) and the result parsing (`end_by`, `position`,
  `extra_position`/`extra_duration`). Test with mpv-android and VLC if they are installed on the S22; don't
  install apps on the devices without asking the owner.
- **A6.7 screenshot:** A: `PlayerViewModel` L1831 uses `screenshot-to-file <path> <video|subtitles>`; same call.
- **A6.8 local source:** read A: `source-local/src/*/kotlin/tachiyomi/source/local/entries/anime/LocalAnimeSource.kt`
  and `EpisodeLoader.getHostersOnLocal`; needs AY1's `content:`/`file:` URL support.

## 7. Owner report 2026-11-12: "extensions load but videos don't play" — card AY-PLAY (first, before AY0)

### What Claude saw (tablet `R52Y90HA77L`, preview build 0.1.0-8747 = `b87145ef8`, anime switch on)

Evidence: `app/build/review-evidence/ay-play/` in the main checkout (two screenshots, the app's logcat).

1. Browse › Anime › **AnimePahe** › Popular: "unknown error" + Retry. The app logged **nothing** for it.
2. Browse › Anime › **AnimeKai** › One Piece: catalogue, details and 1,181 episodes load. Tap an episode:
   - the player shows only a spinner for more than 25 s (`…spinner-25s.png`);
   - later it shows 720P, position 23:35, remaining −00:00, a Play button, a black picture
     (`…stuck-at-end.png`). Pressing Play changes nothing: 14 s later the position is still 23:35.
   - The app log has **no mpv line at all** and no warning from the player. All 17 installed extensions are
     reloaded three times during this one episode open (23:32:45, 23:33:04, 23:33:17 — the "isn't trusted" and
     "Unsupported class loader" bursts).
3. The episode list shows "第1180話 · EPISODE 1180" twice (one dated 11/11, one 11/04 and seen); the row labelled
   1179 opened a player titled EPISODE 1180. Check the episode-number label against `episode_number`.
4. The player opened in portrait on the tablet although `applyOrientation()` asks for sensor-landscape.

Not established: why mpv shows no picture. The app gives no way to tell, which is the first thing to fix.

### Read from the code (causes to confirm, in this order)

| # | Suspect | Where | Why it fits |
|---|---|---|---|
| a | mpv errors are invisible | `MpvAnimePlayerBackend.initialize`: `msg-level all=v` is set but no `MPVLib.LogObserver` is ever added; `efEvent` is the only error path | Nothing in logcat; a 403 or demuxer failure can't be seen or shown |
| b | Resume at the very end | `AnimePlayerModel.openEpisode`: `start = lastSecondSeen unless episode.seen`; `load()` sets `start=<seconds>` | A saved position at or near the end starts the file at EOF: position = duration, nothing plays |
| c | Play can't restart | `seekTo` returns early when `duration <= 0`; `togglePlayback` relies on `ended` | If duration never arrives, Play is a no-op, exactly as seen |
| d | Slow, fragile resolve | `AnimeStreamResolver.videos`: sequential hosters, no timeout, one failure fails all (finding 1) | 25+ s spinner |
| e | Headers | `load()`: `video.headers` only, no `source.headers` fallback (finding 4) | CDN refuses the stream; with (a) nobody sees it |
| f | Extension reload per open | `openEpisode` calls `AnimeSourceManager.create(context)` + `refresh()` every time | Three full reloads per episode; slow, and a new source instance each time loses the extension's in-memory state (cookies/tokens some sources keep) |
| g | Source errors swallowed | catalogue error path in `ui/AnimeCatalogueActivity.kt` | "Unknown error" with no log and no message |

### AY-PLAY steps (T4; implementer uses the tablet; debug build)

1. **See first.** Add an `MPVLib.LogObserver` in the backend: mpv `warn`/`error` lines → `logcat` tag `TsuzukiMpv`
   always; `v` level only in debug builds. Log one line per stage with elapsed ms: hosters requested → n hosters →
   videos per hoster → chosen video (host only, never the full URL with tokens) → header names sent → `loadfile`
   → `FILE_LOADED` or `END_FILE` with mpv's reason/error. Handle `MPV_EVENT_END_FILE` with its error code as the
   failure path if the vendored `MPVLib` exposes it (check `MPVLib.java`; if only `efEvent`, keep that and also
   treat "no `FILE_LOADED` within 30 s of `loadfile`" as a failure with a visible message).
2. **Reproduce** on the tablet with the debug APK: AnimeKai › One Piece › an unwatched episode, and AnimePahe ›
   Popular. Save logcat to `app/build/review-evidence/ay-play/`. Write the actual mpv/extension error into the
   card before changing behaviour. Compare with the Aniyomi app on the same device and episode (package
   `xyz.jmir.tachiyomi.mi` is installed on the tablet): if Aniyomi plays it, the difference is ours.
3. **Fix what the log shows**, plus these regardless:
   - (b) clamp the start: if `lastSecondSeen >= duration − 10 s` or the episode is at least 90 % seen, start at 0;
     never pass a `start` beyond the file. Use Aniyomi's rule (A: `PlayerActivity.setVideo`, seen → 0 unless
     "preserve position").
   - (c) Play on an ended or zero-duration file reloads the stream from 0 instead of doing nothing.
   - (e) `video.headers ?: source.headers` (pull this one line forward from AY1).
   - (f) one process-wide `AnimeSourceManager` (Injekt singleton) refreshed only when
     `AnimeSourceManager.extensionChanges` changes; the player, details, catalogue, downloads and update job share
     it. "Extensions load once across two episode opens" is the test.
   - (g) every source error is logged with its stack and shown with its message (`e.message`), with Retry and
     "Open in WebView" where the source is an `AnimeHttpSource`. Cloudflare/DDoS-Guard challenges: the anime
     sources must use the same `NetworkHelper` client that carries the app's existing WebView interceptor (check
     `AnimeHttpSource.client`).
   - A loading state that says what it is doing (finding servers / loading video) and a 30 s timeout per hoster.
4. Episode label (item 3 above) and player orientation (item 4): fix if they reproduce in debug; else note it.
5. Tests: start-position clamp; play-after-end reloads; one extension load across two episode opens (fake
   loader counting calls); source error → message; log redaction (no query string).

Device script (run it, then the review lane repeats it): three real sources × two episodes each (one new, one
half-watched) on the tablet and the S22: first frame within 15 s, position advances in two screenshots 5 s
apart, audio focus taken (`dumpsys audio | grep -i tsuzuki`), resume returns to the saved position, a finished
episode restarts from 0. A source that fails shows a readable reason. `logcat -b crash` empty.

AY1 then only adds what AY-PLAY didn't (parallel hosters, lazy, preferred, sort order, mpvArgs, URL schemes).

## 8. Video previews from the design — card AY9 (after AY-PLAY)

What the boards show (`docs/design/boards/png/10-anime/dot/`; Colour versions one folder up):

- **AnimeHome** (the Watch home): the "Continue watching" card is a 16:9 **video frame** of where you stopped,
  with a chip "EP 7 · 12:40 LEFT" top-left and a thin progress line along its bottom edge; title, "S2 · EP 7 ·
  episode name" and the red Play button under it.
- **AnimeDetails**: every episode row has a 16:9 thumbnail on the left with a duration chip bottom-right and, for
  a started episode, a progress line along the bottom; watched rows are dimmed.
- **AnimeLibrary**: the grid uses covers (no video frames) with "EP 7 / 12" and progress dots. Don't put frames
  there.

Today: the tablet's episode rows are text only (seen 2026-11-12). `episodes.preview_url` is already stored
(`AnimeLibraryStore.kt` L279) and used only by Updates.

### AY9 steps (T3)

1. **Sources of a frame, in order:** (1) the last-watched frame Tsuzuki captured itself; (2) the source's
   `SEpisode.preview_url`; (3) the anime cover, cropped to 16:9; (4) the board's plain gradient tile. Never a
   sample image.
2. **Capture:** when playback pauses, stops, or the player closes with position ≥ 5 s, the backend runs mpv
   `screenshot-to-file <tmp> video` (no subtitles, no OSD), then on IO: downscale to 640 px wide, write JPEG
   (quality 80) atomically to `filesDir/anime-frames/<animeId>/<episodeId>.jpg`. At most one capture per 30 s and
   one at close. Skip when the frame is nearly black (mean luma < 8) so a fade doesn't become the preview — keep
   the previous file. Delete an anime's folder when it leaves the library and its episodes are removed; cap the
   whole store at 100 MB, oldest first. No frames in incognito. Frames are not in backups.
3. **Loading:** one Coil model type `AnimeEpisodePreview(animeId, episodeId, previewUrl, coverUrl, frameVersion)`
   with a fetcher that tries the file, then the URL **through the source's OkHttp client and headers** (as manga
   covers do with `MangaCover`; see A: `data/coil/` anime cover fetchers), then the cover. `frameVersion` = file
   mtime, in the cache key, so a new capture replaces the old picture. Decode at the displayed size.
4. **Watch home card** (`presentation/tsuzuki/anime/` — find the existing continue-watching card from A2.4/A3.9):
   frame 16:9 with the corner radius, chip text and progress line measured from `html/10-anime/AnimeHome.html`
   (`renderVals()`); chip = "EP n · mm:ss LEFT" from `totalSeconds − lastSecondSeen`, hidden when the duration is
   unknown. Dot: frame in greyscale like covers (reuse the cover colour filter), progress line red. Colour: full
   colour, accent line.
5. **Episode rows** (`tsuzuki/anime/ui/AnimeEpisodeTiles.kt`): thumbnail left per the AnimeDetails board;
   duration chip only when `totalSeconds > 0` (real data: known after the episode has been opened once, or from
   the source if it provides it); progress line = `lastSecondSeen / totalSeconds`; seen rows dimmed as on the
   board; filler badge from `fillermark`. Tablet two-pane details uses the same row. Lazy list: stable keys,
   `contentType`, fixed thumbnail size so rows don't jump while images load.
6. A setting in the anime player settings page, "Episode previews": on / Wi-Fi only / off (off = gradient tile,
   no network for thumbnails). Captured frames still show when off.
7. Tests: source-order choice; black-frame rejection; store cap and cleanup; cache key changes with mtime;
   chip text; duration hidden when unknown.

Device script: play 20 s of an episode, pause, back → details row shows that frame with a progress line → Watch
home card shows the same frame and "EP n · … LEFT" → play to another scene, back, both updated. A source with
`preview_url` shows thumbnails on unwatched rows (once AY0 exists, add `preview_url` to fixture cases 01–03); one
without shows the cover crop. Dot greyscale and Colour, phone and tablet, screenshots compared side by side with
the two boards. Scroll a 1,000-episode list (One Piece) for 10 s: `dumpsys gfxinfo <pkg>` janky frames recorded.
