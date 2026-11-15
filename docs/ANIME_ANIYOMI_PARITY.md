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

## 9. v0.2 plans for Codex, card by card (Claude, 2026-11-15)

Owner 2026-11-15: **v0.2, the first release with anime, = AY9, AY0, AY1, AY2, AY3, AY4, AY6, then REL-0.2**
(one chain review, release audit, upgrade test from 0.1.1). These plans **replace §5 for those cards where they
differ**. §5 was written on 2026-11-12, before AY-PLAY and RV3-S changed the code. Every "now" below was read from
`main` at `304bd9aff`. Check each one again before you change it: AY9 is changing `AnimePlayerModel`,
`MpvAnimePlayerLauncher`, `AnimeLibraryStore` and `AppModule` while this is written.

**Same for every card**

- Branch `tsuzuki/<card>` in `.worktrees/<card>`, made from the previous card's branch (AY0 from `tsuzuki/ay9`,
  AY1 from `tsuzuki/ay0`, …). If the previous card isn't committed yet, wait. Never branch from `main` alone,
  `tsuzuki/a6.4` or `tsuzuki/anime-sync`.
- Claim the card in HANDOFF (status `codex-working`, worktree, date) before reading code. Claim the Device lock
  line before the first adb command, and release it at the end.
- §3 rules apply in full: mpv behind `AnimePlayerBackend`, extension calls in `callAnimeSource {}` on IO,
  `CancellationException` rethrown, pure logic tested first, 8 locales, `TsuzukiAnime.enabled`, scratch package
  `…tsuzuki.animescratch` only.
- Gradle: `:app:assembleDebug`, then **`testDebugUnitTest` for all modules** (not only `:app`; the 0.1.1 CI broke
  on a `:domain` test). Spotless per file, twice. `git diff --check`.
- Result in HANDOFF: files with one line each, decisions, what you left out and why, test counts, the device
  script with what you saw at every step ("not checked" when not run), evidence path
  `app/build/review-evidence/<card>/`, commit hash. Then `review` and stop. Codex does not merge its own card.
- Fixture first (from AY0 on): a device step uses a fixture case wherever one exists. One real source at the end
  as a smoke test. A live site that fails while the fixture passes is recorded, not a card failure.

### 9.0 AY9 (in progress, Codex): what the reviewer checks

Codex's own device script is in HANDOFF. The reviewer also checks:
1. A screenshot is never taken on the main thread and never after `MpvPlayer.destroy()`. Read the order in
   `onCleared`/`close()`. A capture still running when the player closes is awaited with a short timeout or
   dropped, never left racing.
2. The preview fetcher uses `AnimeHttpSource.client` and `headers`, not the app client. A source that isn't
   installed falls back to the cover without throwing.
3. Frame files are not in backups. The folder is deleted when an anime's episodes are removed. The 100 MB cap is
   enforced. Incognito writes nothing.
4. Fixed-size rows (104×58 dp thumbnail) with stable keys. The One Piece scroll jank number is recorded, not
   just "smooth".
5. The Library grid still shows covers. Dot respects the colour-covers preference (on by default since owner
   2026-11-15).

### 9.1 AY0: fixture server and debug fixture source (T3)

**Why first:** AY1–AY6 each prove their fix with a fixture case. Without it every check depends on a live site.

**Now:** there is no fixture and no built-in debug source. Cleartext HTTP is **already allowed for every host**
(`res/xml/network_security_config.xml` base-config), so §5 step 3 (debug network config) is dropped. The
debug-only pattern in this repo is a class in `app/src/debug` found by name at runtime: `DebugInfoScreen.kt`
opens `eu.kanade.tachiyomi.tsuzuki.anime.debug.AnimeDebugActivity` that way. Main code never imports a debug class.

**Server: `scripts/anime-fixture/`**
1. `make-media.sh` (bash, ffmpeg). If `ffmpeg -version` fails, stop with a clear message and set the card
   `blocked: needs ffmpeg on the PC`. Output goes to `scripts/anime-fixture/media/`; add it to `.gitignore`. Files:
   - `ok.mp4`: 60 s, 1280×720, H.264 + AAC, `testsrc2` with a burnt-in running clock (`drawtext` of `%{pts}`), so
     a screenshot shows the position.
   - `hls/master.m3u8` with 1080/720/480 variants, 4 s TS segments, burnt-in labels `1080`/`720`/`480`.
   - `tracks.mkv`: 90 s, two audio tracks (`jpn` 440 Hz tone, `eng` 880 Hz tone, with titles), an embedded ASS
     subtitle with styled text, three chapters ("Opening" 0–20 s, "Part A" 20–70 s, "Ending" 70–90 s).
   - `en.srt`, `en.vtt`, `ja.srt` (Japanese lines such as 「これは字幕のテストです」), each covering 0–60 s.
   - `thumb-01.jpg` … `thumb-03.jpg`, 640×360 (episode `preview_url` images for AY9).
   The same files come out on every run (fixed seeds, no dates), and all of them together stay under 20 MB.
2. `serve.py`: Python 3 stdlib only (`http.server` + `ThreadingHTTPServer`). Binds `127.0.0.1:8765`, never
   `0.0.0.0`. Supports `Range` requests (mpv seeks with them; without 206 responses the MP4 seek checks are
   meaningless). Flags: `--slow-seconds N` (default 20). Routes:
   - `/media/<file>`: plain.
   - `/referer/<file>`: 403 unless `Referer: http://127.0.0.1:8765/`.
   - `/ua/<file>`: 403 unless the User-Agent contains `TsuzukiFixture`.
   - `/header/<file>`: 403 unless `X-Fixture: video`.
   - `/fail/<file>`: 500.
   - `/slow/<file>`: waits `--slow-seconds` before the first byte.
   - `/stall/<file>`: sends the first 2 s of data, waits 30 s, then sends the rest.
   - `/api/anime.json`, `/api/episodes/<case>.json`, `/api/videos/<case>.json`: what the fixture source reads.
   - Every request prints one line to stdout: time, method, path, status, Referer, User-Agent, `X-Fixture`. **This
     log is the evidence for header checks** in AY1/AY2.
3. `README.md` in that folder: the commands (`make-media.sh`, `python scripts/anime-fixture/serve.py`,
   `adb -s <serial> reverse tcp:8765 tcp:8765` per device, `adb reverse --remove-all` afterwards) and the case
   table.

**Debug source**
4. `app/src/debug/java/eu/kanade/tachiyomi/tsuzuki/anime/debug/FixtureAnimeSource.kt`: `AnimeHttpSource` +
   `ConfigurableAnimeSource`, `name = "Tsuzuki Fixture"`, `lang = "all"`, fixed `id` (pick a constant, write it in
   the KDoc and the README), `baseUrl = "http://127.0.0.1:8765"`, `headers` with `User-Agent: TsuzukiFixture/1`
   and `Referer: http://127.0.0.1:8765/`. Popular/Latest/Search all return the case list. Each anime has two
   episodes, so next/previous work.
5. A second class, `FixtureNoHosterAnimeSource` ("Tsuzuki Fixture lib16 no hosters", its own fixed id), which
   **does not override** `getHosterList`/`hosterListRequest`/`hosterListParse` and only implements
   `getVideoList(episode)`. That is case 16, the one AY1's hoster-method detection needs.
6. Cases (anime title = case name; the video JSON decides what the source returns):

   | Case | Returns | Expected once AY1 is done |
   |---|---|---|
   | 01 plain mp4 | one hoster, one video `/media/ok.mp4`; episodes have `preview_url` thumb-01 | plays |
   | 02 hls variants | one video `/media/hls/master.m3u8`; thumb-02 | plays, quality from mpv's variants |
   | 03 first hoster throws | hoster A throws in `getVideoList(hoster)`, hoster B returns ok.mp4; thumb-03 | plays from B |
   | 04 lazy hoster | hoster A `lazy = true` (would return 480), hoster B normal 720 | B plays; A loads only when picked |
   | 05 preferred video | three videos, the middle one `preferred = true` | the preferred one plays |
   | 06 needs source headers | `/referer/ok.mp4`, `video.headers = null` | plays (source Referer used) |
   | 07 needs video headers | `/header/ok.mp4`, `video.headers` carries `X-Fixture: video` | plays |
   | 08 mpvArgs | ok.mp4 with `mpvArgs = [("demuxer-lavf-o", "probesize=5000000")]` and one disallowed (`("script", "x")`) | plays; the disallowed one is logged as dropped |
   | 09 external srt + vtt + ja | ok.mp4 + subtitle tracks en.srt, en.vtt, ja.srt (titles and langs set) | three subtitle choices that render |
   | 10 mkv tracks + chapters | tracks.mkv | two audio, one ASS track, three chapters |
   | 11 timestamps | ok.mp4 with `timestamps` Opening 5–15 s, Ending 45–55 s | used by AY5 (after v0.2) |
   | 12 all hosters fail | two hosters, both `/fail/` | error state with Retry, no crash |
   | 13 slow | `/slow/ok.mp4` | loading state, then plays after ~20 s |
   | 14 stall mid-stream | `/stall/ok.mp4` | buffering indicator during the stall (AY4) |
   | 15 sortVideos honours quality | 1080/720/480 videos in that order; `sortVideos` moves the preference's quality first | the preferred quality plays |
   | 16 lib16 no hosters | from `FixtureNoHosterAnimeSource` | plays (AY1 detection) |

   Preference screen: one `ListPreference` "Preferred quality", key `preferred_quality`, entries 1080/720/480,
   default 720, read by `sortVideos`.
7. Registration. Main: `AnimeSourceManager` gets a second constructor parameter
   `builtIn: () -> List<RegisteredAnimeSource> = { emptyList() }`, merged into `load()` **before**
   `distinctBy { it.source.id }` and upserted as `StubAnimeSource` like the others. `AnimeSourceManager.create()`
   passes `::debugBuiltInAnimeSources`, a main-code function that returns empty unless `BuildConfig.DEBUG`; when it
   is, it loads `eu.kanade.tachiyomi.tsuzuki.anime.debug.FixtureAnimeSources` by name and calls its `all()`
   (returns both sources with `libVersion = 16.0`, `extensionPackage = "tsuzuki.fixture"`). A
   `ClassNotFoundException` returns empty, logged at debug level. Preview and release builds simply don't
   contain the class.
8. Browse › Anime shows the fixture as an installed source in debug builds. Use the existing installed-source path;
   don't add a separate UI. If the extensions tab expects an APK-backed entry, show the fixture only in
   the catalogue's source list, and write down which.

**Tests** (`app/src/testDebug/java/.../tsuzuki/anime/debug/FixtureAnimeSourceTest.kt`; the `testDebug` source set
already exists): for each case, the hoster/video lists built from canned JSON with no network; case 16's class
overrides none of the hoster methods (reflection, the same check AY1 will write); preference default 720.
`AnimeSourceManagerTest`: built-in sources are merged, de-duplicated against installed ids, and empty when the
provider is empty.

**Device script**
1. Lock; `make-media.sh`; start `serve.py`; `reverse` on both devices. Expected: `curl -I
   http://127.0.0.1:8765/media/ok.mp4` on the PC returns 200 with `Accept-Ranges`.
2. Scratch debug on the tablet and S22, anime on: Browse › Anime lists "Tsuzuki Fixture" and the lib16 source;
   the catalogue shows cases 01–16. Screenshot, Dot and Colour.
3. Case 01 plays: position label advances in two screenshots 5 s apart; the server log shows the requests.
4. Build the **preview** scratch APK: `apkanalyzer dex packages <apk> | grep -i fixture` prints nothing, and
   the catalogue doesn't show the fixture.
5. Release lock, `reverse --remove-all`, Dot Home, crash buffer empty.

**Done when:** the server, both sources, the README and tests are committed, case 01 plays on both devices,
and the preview APK has no fixture classes. The other cases (03, 04, 05, 12, 16 …) are expected to fail on this
card: record what each does today, as the "before" for AY1.

### 9.2 AY1: load streams the way Aniyomi does (T4)

**Already done by AY-PLAY, don't redo:** `sortHosters()`/`sortVideos()` called (`AnimeStreamResolver.videos`);
`video.headers ?: source.headers` **in the player** (`withSourceHeaders`); `mpvArgs` passed per file with an
allowlist and length quoting (`AnimeMpvLoad.kt`, `animeMpvLoadCommand`); one shared `AnimeSourceManager`; start
position clamp.

**Still wrong now (read from the code):**
- `AnimeStreamResolver.videos` for lib ≥ 16 calls `getHosterList` always (finding 3), then walks hosters **one
  after another inside one `callAnimeSource`**: one throwing hoster fails the whole episode (finding 1), and
  `lazy` hosters are loaded like the others.
- `rankAnimeVideos(videos, preferred)` **re-sorts by resolution even on Auto** (`preferred <= 0` puts every known
  resolution in bucket 0, then sorts highest first). That throws away the source's order and only uses
  `Video.preferred` as the third tie-breaker. The source's quality/server preferences therefore have no effect.
- `resolve()` ignores `video.initialized` and requires an HTTP(S) URL.
- `AnimeDownloads.transfer` (≈ L255–290) uses `rankAnimeVideos` and `resolve` but **never applies the source's
  headers**, so a download can 403 where playback works.
- No time limit: a hoster that never answers, or mpv that never sends `FILE_LOADED`, leaves the spinner forever.

**Steps**
1. **Hoster detection.** In `AnimeStreamResolver`, `animeSourceHasHosters(source): Boolean`: true when the
   source's class (or a superclass below `AnimeHttpSource`) declares `getHosterList(SEpisode)`,
   `hosterListRequest(SEpisode)` or `hosterListParse(Response)`. Port of A: `EpisodeLoader.checkHasHosters`. Cache
   the result per class in a `ConcurrentHashMap<Class<*>, Boolean>`.
2. **`AnimeStreamResolver.hosters(source, libVersion, episode): List<Hoster>`**: if `libVersion < 16` or
   `!animeSourceHasHosters(source)`: `getVideoList(episode)` → `sortVideos()` → one pseudo hoster named
   `Hoster.NO_HOSTER_LIST` holding those videos. Else `getHosterList(episode)` → `sortHosters()`. Inside
   `callAnimeSource`, on IO.
3. **Per-hoster state, pure** (`tsuzuki/anime/player/AnimeHosterStates.kt`, no Android types):
   `sealed interface AnimeHosterState { Idle; Loading; Ready(videos: List<AnimeVideoState>); Error(cause) }`, with
   `AnimeVideoState { video; status: Unresolved | Resolving | Ready | Failed }`. Functions:
   `selectBestVideo(states): Pair<hosterIndex, videoIndex>?`: the first video marked `preferred` in source order,
   else the first unresolved video with a non-blank URL in source order. Port of A: `HosterLoader.selectBestVideo`.
4. **Loader** (`tsuzuki/anime/player/AnimeHosterLoader.kt`): `coroutineScope { }`; each non-lazy hoster in its
   own `async(Dispatchers.IO)` with **`withTimeout(20_000)`** around its video list
   (`hoster.videoList ?: getVideoList(hoster)` → `sortVideos()`). A hoster that throws or times out becomes
   `Error`, logged with `AnimeLog.w`, and the rest continue. As soon as a hoster delivers a `preferred` video,
   resolve it and start; cancel the hosters still loading only if that resolve works. Otherwise, when every
   non-lazy hoster has finished, walk `selectBestVideo` until one resolves; a failed resolve marks that video
   `Failed`. Lazy hosters stay `Idle` until the user picks them in the sheet. Expose the states as a `StateFlow`
   for the sheet.
5. **Resolve:** skip `resolveVideo` when `video.initialized`; after a successful resolve, copy with
   `initialized = true`. Then `withSourceHeaders`.
6. **Quality order. Decision recorded here; write it in the KDoc and test it:** with the player's quality set to
   **Auto (`quality == 0`), the source's order and `preferred` win**: no re-sorting. Only when the user picked a
   quality in the player does `rankAnimeVideos(…, preferred)` apply, and then as a stable sort that keeps the
   source order within the same resolution. The mpv-error fallback (next candidate) walks the same order.
   `AnimeStreamQueue` is replaced by the loader; remove it once nothing uses it.
7. **mpv watchdog:** after `backend.load`, if neither `onLoaded` nor `onError` arrives within **30 s**, treat it
   as an error: log `mpv FILE_LOADED watchdog`, mark the video `Failed` and try the next one (same path as
   `onError`). Case 13 (20 s slow start) must still pass under 30 s. Cancel the timer on load, error, episode
   change and `onCleared`.
8. **Address types. Decision:** keep the security rule in `checkAnimeVideoUrl`. Extensions may give
   `http`/`https`; the app's own offline files stay bare absolute paths. `file:`, `content:`, `data:`, `magnet:`
   and torrent links from an extension end in the readable "unsupported address type" error and the loader moves
   on to the next video. Aniyomi accepts `file:` and `content:`; we don't, because an extension could then read the
   app's private files. Write this difference in the Result. `resolve()` keeps rejecting non-HTTP URLs. One fix:
   an `http`/`https` URL that `toHttpUrlOrNull` rejects only because of spaces is percent-encoded instead of
   dropped.
9. **Quality sheet** (`AnimePlayerSheets.kt`, the quality section ≈ L160–180): group rows under hoster names
   (skip the heading when there is only the `NO_HOSTER_LIST` pseudo hoster); a loading hoster shows the existing
   small progress row; a failed one shows its name with the "couldn't load" text and is disabled; a lazy hoster
   loads when tapped. Use the existing `AnimePlayerOption` rows; no new layout. Check `Player.png` /
   `PlayerSheets.png` (Dot, then Colour) for the existing look.
10. **Downloads** (`AnimeDownloads.transfer`): use `AnimeStreamResolver.hosters` + the same `selectBestVideo` order
   and the same Auto rule, and pass `withSourceHeaders(...)` to `AnimeVideoTransfer`. A download and the player
   pick the same video for the same settings.
11. Logging: one line per hoster (`hoster <name>: <n> videos (+ms)` / `failed: <reason>`); the existing redaction
   (host only) applies.

**Tests** (`AnimeHosterLoaderTest`, `AnimeStreamResolverTest`, `AnimePlayerPolicyTest`, with fake sources and no
network): hoster detection with three classes (overrides `getHosterList`; overrides only `hosterListParse`;
overrides none); lib 14 path; one hoster throws → the other's video is chosen; a hoster that hangs is cut at the
timeout (virtual time) and the others play; lazy hoster not called until requested; `preferred` beats order;
Auto keeps source order (the current test that expects resolution order must change; say so in Result); a manual
quality overrides; `initialized` skips resolve; download selection equals player selection; watchdog fires
once and not after `onLoaded`; cancelling the episode leaves no running child job.

**Device script** (fixture; both devices; logcat `-s TsuzukiAnime:V` per step)
1. Case 03: plays from hoster B; log shows hoster A failed. Sheet shows A as failed.
2. Case 04: B plays; server log has **no** request for A's URL until A is tapped in the sheet; then A's 480 plays.
3. Case 05: the middle (preferred) video plays (burnt-in label).
4. Case 06: plays; server log line shows `Referer: http://127.0.0.1:8765/`. Case 07: server log shows
   `X-Fixture: video`.
5. Case 08: plays; log shows the dropped `script` option.
6. Case 12: error tile with Retry, no crash; Retry fails the same way.
7. Case 13: loading, then plays without the watchdog firing. Restart the server with `--slow-seconds 40`: the
   watchdog fires at 30 s and shows the error.
8. Case 15 on Auto: the 720 variant (the source's default) plays, not 1080. Pick 1080 in the sheet: 1080 plays.
9. Case 16: plays.
10. Download case 06 and play it in airplane mode: plays (headers were applied to the download).
11. Time to first frame for case 01, five runs per device, `loadfile` → `FILE_LOADED` from log timestamps:
   median recorded.
12. Smoke: one real source episode plays and resumes. Dot and Colour screenshots of the sheet with hosters.

### 9.3 AY2: anime source settings (T3)

**Now:** `ConfigurableAnimeSource` exists in `source-api` with `preferenceKey() = "source_$id"` and
`sourcePreferences()`. No anime screen uses it. The manga page
`ui/browse/extension/details/SourcePreferencesScreen.kt` is upstream code bound to the manga `SourceManager`
(`SourcePreferencesFragment.getInstance(sourceId)`). Manga backups write source preferences in
`data/backup/create/creators/PreferenceBackupCreator.kt` (L36) for manga sources only.

**Steps**
1. `tsuzuki/anime/ui/AnimeSourcePreferencesScreen.kt`: a Voyager `Screen(sourceId)` built like the manga one
   (`FragmentContainer` + a `PreferenceFragmentCompat`), but the fragment
   (`AnimeSourcePreferencesFragment`) gets the source from `Injekt.get<AnimeSourceManager>()`, sets
   `preferenceManager.sharedPreferencesName = source.preferenceKey()`, and calls
   `source.setupPreferenceScreen(screen)` inside `callAnimeSource`. Copy, don't change, the manga file. Add the
   Aniyomi header line (adapted from `AnimeSourcePreferencesScreen.kt`). If the source isn't loaded yet, show the loading
   screen and call `ensureLoaded()`. A source that throws while building its screen gets the ERR1 error state.
2. Look: the manga source settings page is already themed for Dot/Colour (same fragment styling); match it, and put
   Dot and Colour screenshots in Result. No board exists for this page; it is a standard preference list. If the
   fragment's widgets look wrong in Dot, record it. Don't invent a layout.
3. Entry points:
   - Extensions tab (`presentation/tsuzuki/browse/AnimeExtensionsTab.kt`, installed rows in
     `AnimeStoreExtensionRow`): a settings `IconButton` beside remove, shown only when at least one of the
     extension's loaded sources is `ConfigurableAnimeSource`. One configurable source: open it. Several: a small
     picker dialog listing source name + language.
   - Catalogue (`ui/AnimeCatalogueActivity.kt`, top bar actions ≈ L259–270): a settings `IconButton` when the
     current source is configurable. The catalogue is an Activity: start a host activity or push the screen in
     whatever navigator the catalogue already has. Check which before writing.
4. **Changes apply without a restart:** register a `SharedPreferences.OnSharedPreferenceChangeListener` while the
   page is open. When the user leaves the page and something changed, call
   `AnimeSourceManager.extensionsChanged()`, so the next `ensureLoaded()` creates fresh source instances (sources
   often read `baseUrl` once). The open catalogue reloads its first page when it resumes and the manager's
   version changed.
5. **Backups:** first read `PreferenceRestorer` to check whether it restores source preferences **by the stored
   key name** (`source_<id>`) without asking the manga source manager. If it does, anime preferences only need to
   be **written**: add a one-line hook in `PreferenceBackupCreator` that appends
   `TsuzukiAnimeBackup.sourcePreferences()` (new, in `tsuzuki/anime/backup/`, empty unless `TsuzukiAnime.enabled`,
   reads every loaded `ConfigurableAnimeSource`). Add `PreferenceBackupCreator.kt` to the AGENTS.md upstream list.
   If the restorer does depend on the manga manager, restore anime preferences in the anime backup store instead,
   and write which you did. Respect the "source settings" backup option the same way manga does.
6. Key clash: manga and anime both use `source_<id>`. Ids come from hashes of different strings, and Aniyomi has
   the same rule, so keep it. Don't invent a new prefix (restored Aniyomi settings would stop lining up).

**Tests:** fragment-free unit test for the source lookup + key name; a non-configurable source → no settings
entry (pure function deciding the button); backup creator includes a fake configurable anime source's prefs only
when anime is on; restore round trip of one string pref.

**Device script**
1. Extensions tab › fixture (or the catalogue) › settings: "Preferred quality" shows 720. Screenshot Dot and
   Colour, tablet and S22.
2. Set 480, back, open case 15 on Auto: the 480 variant plays (burnt-in label + server log path), **without
   restarting the app**.
3. Force-stop and reopen: still 480.
4. Back up, set 1080, restore the backup: 480 again.
5. A real configurable extension (AnimeKai or AnimePahe, if configurable) opens its settings without a crash.
6. Set it back to 720. Crash buffer empty.

### 9.4 AY3: subtitles that always render, and subtitle style (T3)

**Now:** `MpvAnimePlayerBackend.tracks()` adds external tracks only when `toHttpUrlOrNull()` works, with an empty
title (`sub-add url auto "" lang`). `anime-player/src/main/assets` has only `cacert.pem`, and no font directory is
set. `AnimePlayerSettings` has only `subtitleScale` and `subtitleDelay`. Forced-track and language matching is in
`player/AnimeTrackLanguages.kt` (A6.4).

**Steps**
1. **Check first, before any code.** Fixture case 09 on the S22 and the tablet: pick en.srt, en.vtt, ja.srt in turn;
   screenshot each with text on screen (pause on a line). Case 10: the embedded ASS track. Record what renders.
   Japanese text showing as boxes or nothing is the expected failure.
2. **Fonts** (only if step 1 shows missing or boxed text; if all render, write that and skip to step 3): on
   `initialize`, copy `R.font.lnr_manrope` (Latin) and `R.font.lnr_jp_zenkaku_regular` (Japanese subset) from
   resources into `filesDir/mpv/fonts/`. Copy only when the size differs, like `cacert.pem`, and on IO before
   `MpvPlayer.create`. Set `sub-fonts-dir` and `osd-fonts-dir` to that folder and `sub-font` to the Latin face's
   family name; mpv falls back per glyph to the other file. Both licences should already be in `docs/licenses/`:
   check, and name them in Result. No new font files. Check the subset covers the kana and common kanji in ja.srt; if a
   line still shows boxes, list the missing characters in Result instead of adding a bigger font.
3. **External tracks:** accept `http`/`https` URLs (encoded as AY1 does), plus `data:` subtitle URLs written
   by the app to `cacheDir/anime-subs/<hash>.<ext>` and added as that path. `file:`/`content:` stay refused
   (same rule as AY1 step 8); log and skip. Pass the title and language: `sub-add <url> auto <title> <lang>`,
   `audio-add` likewise. A failed add is logged and skipped, never fails playback.
4. **Style settings** in `AnimePlayerSettings` + `AnimePlayerPreferences` (keys `tsuzuki_anime_player_sub_*`) +
   `MpvAnimePlayerBackend.applySettings` (only changed values, as now):

   | Setting | Values | mpv |
   |---|---|---|
   | Text size | 30–80, default 55 | `sub-font-size` |
   | Bold | on/off, default off | `sub-bold` |
   | Text colour | White, Yellow, Cyan, Green (fixed list) | `sub-color` `#FFFFFFFF` etc. |
   | Border colour | Black, Grey, None | `sub-border-color`; None = `sub-border-size 0` |
   | Border size | 0–5, default 3 | `sub-border-size` |
   | Background | Off / Box | `sub-back-color` `#00000000` / `#99000000` with `sub-border-style=background-box` (check the vendored mpv accepts it; if not, use `sub-shadow-offset`) |
   | Vertical position | 50–100 %, default 95 | `sub-pos` |
   | Override styled (ASS) subtitles | Off / On | `sub-ass-override` `no` / `force` |

   Map them as A: `AniyomiMPVView.setupSubtitlesOptions` does. Colours are a fixed list of pills, not a colour
   picker: **decision, to avoid a new widget without a board**. Existing `subtitleScale` and `subtitleDelay`
   stay.
5. **Where:** a "Subtitles" group on the player settings page (`AnimePlayerSettingsScreen.kt`) with existing rows
   (`AnimePlayerStepper`, `TsuzukiPillRow`, switches). Open `PlayerSettings.png` (Dot, then Colour) first; if the
   board already shows a subtitle group, follow it. Size and position also go in the in-player options sheet
   only if the board shows them there. Changes apply live.
6. **Track choice:** a forced/signs track is not picked as the main subtitle unless nothing else matches the
   language list. Check `AnimeTrackLanguages.kt` against A: `utils/TrackSelect.kt` (ISO 639-1/-2, native and
   English names) and fill what's missing.
7. Strings in 8 locales.

**Tests:** settings → mpv option values (every row, bounds clamped); defaults; data-URL subtitle written once
(hash) and refused schemes skipped; `sub-add` argument list includes title and language; forced-track rule with
three tracks (forced en, full en, ja).

**Device script** (both devices)
1. Before/after screenshots of case 09 en.srt, en.vtt, ja.srt with text visible; case 10 ASS visible.
2. Each style row visibly changes en.srt (size 30 vs 80, yellow, border none, background box, position 60 %):
   one screenshot each, S22.
3. Override ASS on: case 10 text takes the user's style; off: the ASS styling returns.
4. Language list "ja, en": case 09 opens with ja.srt selected; with a forced track present it isn't picked.
5. Dot and Colour screenshots of the Subtitles group. Settings survive force-stop. Crash buffer empty.

### 9.5 AY4: media session, headphones, buffering (T3)

**Now:** audio focus with ducking is in `AnimePlayerModel` (`focusRequest`, `onFocusChanged`). There is no
`MediaSession`, no noisy receiver and no key handling in `AnimePlayerActivity`. The backend observes position,
duration, cache time, eof and tracks, but not `paused-for-cache` or `seeking`. `androidx.media:media` is already a
dependency (`libs.androidx.media`), so use `MediaSessionCompat`. **No new library.** Saved state
(`restoredPosition`, `restoredPaused`) already reaches the model.

**Steps**
1. `tsuzuki/anime/player/AnimeMediaSession.kt`: wraps `MediaSessionCompat` (tag `TsuzukiAnime`). Created by the
   activity in `onCreate`, active while the activity is started or in PiP, released in `onDestroy`. Callback →
   model: `onPlay`/`onPause` → `setPlaying`, `onSkipToNext`/`Previous` → `next`/`previous`,
   `onSeekTo(ms)` → `seekTo`, `onFastForward`/`onRewind` → `seekBy(±10)`. Metadata: title = anime title,
   subtitle = episode label (same text as the player top bar), duration in ms, artwork = the AY9 frame/cover
   bitmap if already decoded (don't fetch for the session).
2. Pure mapping `animePlaybackState(state: AnimePlayerState): AnimeSessionState` (no Android types): playing,
   paused, buffering, error, stopped; available actions (next only when `state.next != null`, etc.); position.
   A thin adapter turns it into `PlaybackStateCompat`. The session is updated from `model.state` collected in the
   activity; position at most once a second plus on every state change.
3. **Noisy:** the activity registers a `BroadcastReceiver` for `AudioManager.ACTION_AUDIO_BECOMING_NOISY` in
   `onStart` (`ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)`) and unregisters in `onStop`. On receive:
   `model.setPlaying(false)`.
4. **Buffering:** add `onBuffering(buffering: Boolean)` to `AnimePlayerBackend.Listener`; the mpv backend observes
   `paused-for-cache` and `seeking` (flags) and reports `pausedForCache || seeking`. The model sets
   `AnimePlayerState.buffering` (new field) on main; `playing` stays as it is, so the screen stays on. Update every
   fake backend in tests. The centre control in `AnimePlayerContent.kt` shows the existing loading indicator
   while `buffering && !loading`; no new visuals.
5. **Media title:** in `backend.load`, set `force-media-title` to "Anime — Episode" (pass it in: add a `title`
   parameter to `load`, or a separate `setMediaTitle`). PiP and the session then show it.
6. **Keys** (activity `onKeyDown`, only when the player has focus and no sheet is open): `SPACE`,
   `MEDIA_PLAY_PAUSE`, `MEDIA_PLAY`, `MEDIA_PAUSE`, `DPAD_LEFT`/`RIGHT` → ∓10 s, `MEDIA_NEXT`/`PREVIOUS`. Media keys
   arriving through the session are handled once, not twice.
7. **Process death** (A6.9's leftover): background the player, `adb shell am kill <pkg>`, reopen from recents →
   same episode, position within 10 s, paused. If it fails, fix it in this card.
8. No background playback, notification or foreground service in v0.2. Write that in Result; it's a later card.

**Tests:** state → session state for each case (loading, playing, buffering, ended, failed, no next); noisy →
`setPlaying(false)` via a fake; buffering transitions with the fake backend (seek start/end,
paused-for-cache on/off); key mapping table; title text.

**Device script** (both devices; case 01 unless named)
1. Play; `adb shell input keyevent KEYCODE_MEDIA_PAUSE` → paused (screenshot); `KEYCODE_MEDIA_PLAY` → plays;
   `KEYCODE_MEDIA_NEXT` → episode 2 opens.
2. `adb shell dumpsys media_session | grep -A12 TsuzukiAnime`: session active, state, title, duration. Save the
   output.
3. Case 14: spinner during the 30 s stall (two screenshots), then it continues by itself without Retry.
4. Seek far in case 02: spinner briefly, then plays.
5. PiP (home gesture during playback): the PiP window title/controls show the media title; play/pause works.
6. Noisy pause needs real headphones: if none are connected, "not checked". Don't simulate it with a broadcast
   and call it checked; a debug-only broadcast test may be added as well, labelled as such.
7. Tablet keyboard cover if attached: space toggles, arrows seek; otherwise
   `adb shell input keyevent KEYCODE_SPACE` / `KEYCODE_DPAD_RIGHT`, labelled adb.
8. Process death (step 7 above). Crash buffer empty.

### 9.6 AY6: extension ABI audit (T4)

**Now:** `AnimeExtensionCompatibilityTest` has two tests (legacy suspend request bridges; linkage error → normal
source error). `proguard-rules.pro` already keeps `eu.kanade.tachiyomi.animesource.**`, `network.RequestsKt`,
`NetworkHelper`, `OkHttpExtensionsKt` and the rate-limit interceptors. Nothing compares the app with the stubs
extensions are compiled against.

**Steps**
1. **Expected API from the stubs.** Get `aniyomiorg/extensions-lib` for lib 14, 16 and 17 (tags or branches; write
   the exact commits). Network needed; if unavailable, `blocked: needs the extensions-lib stubs`. Build or
   download each stub jar and dump every public/protected class, method and field with JVM descriptors
   (`javap -s -public` or a small Kotlin/Java dumper). Check the dump into
   `scripts/anime-abi/expected-lib14.txt`, `-lib16.txt`, `-lib17.txt` (one `class#member descriptor` per line,
   sorted). Include what extensions actually link against: `eu.kanade.tachiyomi.animesource.**`,
   `eu.kanade.tachiyomi.network.**` (`GET`, `POST`, `awaitSuccess`, `asObservableSuccess`, interceptors,
   `NetworkHelper`), `eu.kanade.tachiyomi.util.**` (`asJsoup`, `parseAs`, …), `AppInfo`, the preference classes
   from `androidx.preference`, `uy.kohesive.injekt`, `rx`. List in Result whatever is in the stubs but
   deliberately out of scope.
2. **JVM check:** `AnimeExtensionAbiTest` (next to the compatibility test) loads each line, looks the class up in
   the app's test classpath and checks the member exists with the same descriptor and is not less visible. One
   failure message lists every miss, not just the first.
3. **Fix each miss** with a bridge in a Tsuzuki file (the pattern of `b87145ef8` in `Requests.kt`: an overload with
   the old signature that calls the new one), or list it under "not offered" with the reason (e.g. a torrent API
   we don't ship). An upstream file needing a bridge gets the smallest addition and goes on the AGENTS list.
4. **Minified check:** `scripts/anime-abi-check.sh <apk>` runs `apkanalyzer dex packages --defined-only` (or
   `dexdump -d` from build-tools) on the **preview** APK and checks every expected class and member survives R8
   with its original name. Add keep rules only for what the script shows missing; compare each one with Aniyomi's
   `app/proguard-rules.pro` at the pinned commit and copy theirs where it matches. The script exits non-zero
   on any miss.
5. Fixture: add calls in `FixtureAnimeSource` to a few helpers extensions use (`GET`, `awaitSuccess`, `asJsoup`,
   `parseAs`) so the fixture fails loudly if one disappears.
6. Write a short "How to re-run after a dependency bump" in `scripts/anime-abi/README.md`.

**Tests:** the ABI test passes for lib 14/16/17; a deliberately broken expectation line (in a test-only copy)
fails with a readable list.

**Device script**
1. Build the **preview** scratch APK (minified). `scripts/anime-abi-check.sh` on it: exit 0; save the output.
2. Tablet and S22, anime on: three real extensions, ideally lib 14, 16 and 17 (write each one's lib version from the
   extension list). For each: browse, details, episode list, play an episode for 10 s.
3. `adb logcat -d | grep -E 'NoSuchMethod|NoClassDef|AbstractMethod|IncompatibleClassChange'` empty for the whole run.
4. Fixture cases 01 and 16 play. The preview APK has no fixture, so this step uses the debug APK; say so.
5. Crash buffer empty; Dot Home.

### 9.7 REL-0.2: chain review, release audit, upgrade test (review lane)

Starts when AY6 is `review`. One reviewer, not the implementer of these cards.
1. `review/v0.2` from `main`; merge `tsuzuki/ay6` (which contains AY9 → AY0 → … → AY6). The owner decides whether the
   app chain `review/app-chain` (TTS-SETTINGS, READER-DEFAULTS, DICTIONARY-PACKS, DICTIONARY-DESIGN, RECAP-FIX)
   rides along. **Ask before merging it**: those cards have their own review.
2. Read every card's diff for the §3 rules and the decisions written in §9 (Auto keeps source order; non-HTTP
   addresses refused; colours as fixed pills; no background playback). A card that broke a decision goes back
   with a `-F` card.
3. Re-run every card's device script on the merged build on both devices (fixture), Dot and Colour. Then three real
   sources: browse → play → resume → next episode.
4. Anime off by default: a fresh install and an upgraded install show no anime anywhere; turning it on works
   without a restart.
5. **Upgrade test:** install the published **v0.1.1** release APK on a scratch package with the synthetic
   500-book backup restored (as in REL-0.1.1), then install the v0.2 candidate over it: no crash, migrations
   37–39 run, books, history, shelves and preferences unchanged (same comparison script as REL-0.1.1).
6. `testDebugUnitTest` for **all modules**, Debug + Preview + Release builds, 16 KB ZIP/ELF audit of every APK,
   `scripts/anime-abi-check.sh` on the release APK, locale/placeholder checks.
7. `CHANGELOG.md` v0.2 section in the owner's plain words (anime: what works, what's off by default, known gaps:
   no background playback, no torrents/casting, the later Batch AY cards). versionCode 26, versionName 0.2.0.
8. Stop at the publishing handoff: **the owner tags and publishes.** No tag, no push.
