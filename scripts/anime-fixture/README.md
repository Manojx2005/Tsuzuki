# Anime fixture server (card AY0)

A local server with known-good and known-bad anime streams, so the anime cards (AY1–AY6) can prove each fix
without depending on a live site. It pairs with the debug-only **Tsuzuki Fixture** anime source (app side of
AY0, in `app/src/debug`), which reads the case JSON below. Preview and release builds don't contain that source.

Debug-build sources (registered by `AnimeSourceManager` through `debugBuiltInAnimeSources`, found by class name so
main code never imports them): **Tsuzuki Fixture**, id `7311000000000001`, cases 01–15, with a "Preferred quality"
setting (`preferred_quality`, 1080/720/480, default 720) read by `sortVideos`; **Tsuzuki Fixture lib16 no hosters**,
id `7311000000000016`, case 16, which implements only `getVideoList(episode)`. Both send `User-Agent:
TsuzukiFixture/1` and `Referer: http://127.0.0.1:8765/`, and show under Browse › Anime › Sources (not in the
extensions tab, which lists APKs).

## Run it

```sh
bash scripts/anime-fixture/make-media.sh          # once; needs ffmpeg; writes media/ (git-ignored, ~15 MB)
python scripts/anime-fixture/serve.py              # 127.0.0.1:8765, one log line per request
adb -s <serial> reverse tcp:8765 tcp:8765          # per device; the app then reaches the PC at 127.0.0.1:8765
adb -s <serial> reverse --remove tcp:8765        # afterwards
```

- `make-media.sh` gives byte-identical files on every run (fixed patterns, single-threaded x264, bitexact
  muxing). Labels use Arial on Windows / DejaVu Sans on Linux; set `FIXTURE_FONT=/path/to/font.ttf` to choose.
- `serve.py` options: `--slow-seconds N` (default 20, route `/slow/`), `--stall-seconds N` (default 30, route
  `/stall/`), `--port` (tests only; the cases point at 8765).
- `--new-episodes` gives every case an episode 3. Favourite a case, restart the server with the flag and refresh the
  anime library: the episode then shows as new in Updates.
- It binds `127.0.0.1` only. Use `adb reverse`, never a LAN address.
- Tests: `python -m unittest scripts/tests/test_anime_fixture_server.py`.

## Media (`media/`)

| File | What it is |
|---|---|
| `ok.mp4` | 60 s, 1280×720 H.264 + AAC (440 Hz), burnt-in running clock, so a screenshot shows the position |
| `hls/master.m3u8` | 24 s HLS, 1080/720/480 variants (`hls/<h>/index.m3u8`), 4 s segments, labelled `1080p`/`720p`/`480p` |
| `tracks.mkv` | 90 s; audio `jpn` "Japanese (440 Hz)" and `eng` "English (880 Hz)"; one styled ASS track; chapters Opening 0–20 s, Part A 20–70 s, Ending 70–90 s |
| `en.srt`, `en.vtt`, `ja.srt` | 12 cues each, every 5 s from 0 to 60 s; the Japanese one says 「これは字幕のテストです」 and others |
| `thumb-01.jpg` … `thumb-03.jpg` | 640×360 episode previews (`preview_url`, AY9) |

## Routes

| Route | Behaviour |
|---|---|
| `/media/<file>` | plain; `Range` supported (206 / 416), `HEAD` supported |
| `/referer/<file>` | 403 unless `Referer: http://127.0.0.1:8765/` |
| `/ua/<file>` | 403 unless the User-Agent contains `TsuzukiFixture` |
| `/header/<file>` | 403 unless `X-Fixture: video` |
| `/fail/<file>` | 500 |
| `/slow/<file>` | waits `--slow-seconds` before answering |
| `/stall/<file>` | sends about the first 2 s of the file, waits `--stall-seconds`, then the rest |
| `/api/anime.json` | the case list: `case`, `title`, `url`, `thumbnail`, `noHosters` |
| `/api/episodes/<case>.json` | two episodes (2 then 1): `url`, `name`, `number`, `dateUpload`, `previewUrl` |
| `/api/videos/<case>.json` | `hosters[]`: `name`, `lazy`, `throws`, `videos[]` (`url`, `quality`, `preferred`, `headers`, `subtitleTracks`, `mpvArgs`, `timestamps`) |

Each request prints `time method path status referer ua x-fixture`. **That log is the evidence for the header
checks** in AY1/AY2.

## Cases

The anime title is the case name. The source maps the video JSON onto `Hoster`/`Video`.

| Case | Returns | Expected once AY1 is done |
|---|---|---|
| 01 plain mp4 | one hoster, `/media/ok.mp4`; episodes have preview thumb-01 | plays |
| 02 hls variants | `/media/hls/master.m3u8`; thumb-02 | plays, quality from mpv's variants |
| 03 first hoster throws | hoster A throws in `getVideoList(hoster)`, hoster B ok.mp4; thumb-03 | plays from B |
| 04 lazy hoster | A `lazy` (480p), B normal (720p) | B plays; A loads only when picked |
| 05 preferred video | 1080/720/480, the middle one `preferred` | the preferred one plays |
| 06 needs source headers | `/referer/ok.mp4`, no video headers | plays (source Referer used) |
| 07 needs video headers | `/header/ok.mp4`, video headers `X-Fixture: video` | plays |
| 08 mpvArgs | ok.mp4 with `demuxer-lavf-o=probesize=5000000` and a disallowed `script=x` | plays; the disallowed one is logged as dropped |
| 09 external srt + vtt + ja | ok.mp4 + en.srt, en.vtt, ja.srt | three subtitle choices that render |
| 10 mkv tracks + chapters | tracks.mkv | two audio, one ASS track, three chapters |
| 11 timestamps | ok.mp4, Opening 5–15 s, Ending 45–55 s | used by AY5 (after v0.2) |
| 12 all hosters fail | two hosters, both `/fail/` | error state with Retry, no crash |
| 13 slow | `/slow/ok.mp4` | loading state, then plays after ~20 s |
| 14 stall mid-stream | `/stall/ok.mp4` | buffering indicator during the stall (AY4) |
| 15 sortVideos honours quality | 1080/720/480 in that order | the "Preferred quality" setting's choice plays first |
| 16 lib16 no hosters | served by the second source, which only implements `getVideoList(episode)` | plays (AY1 detection) |
