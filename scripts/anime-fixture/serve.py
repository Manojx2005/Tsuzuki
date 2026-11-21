#!/usr/bin/env python3
"""Tsuzuki anime fixture server (card AY0). Python 3 standard library only.

Serves the media made by make-media.sh plus the JSON the debug-only "Tsuzuki Fixture" anime source reads.
Binds 127.0.0.1 only; reach it from a device with `adb -s <serial> reverse tcp:8765 tcp:8765`.

Every request prints one line (time, method, path, status, Referer, User-Agent, X-Fixture). That log is the
evidence for the header checks in AY1/AY2.

    python scripts/anime-fixture/serve.py [--port 8765] [--slow-seconds 20] [--stall-seconds 30]
"""

from __future__ import annotations

import argparse
import json
import mimetypes
import os
import re
import sys
import time
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlsplit

HERE = os.path.dirname(os.path.abspath(__file__))
MEDIA = os.path.join(HERE, "media")
HOST = "127.0.0.1"
BASE = "http://127.0.0.1:8765"
REFERER = BASE + "/"
UA_MARK = "TsuzukiFixture"

mimetypes.add_type("application/vnd.apple.mpegurl", ".m3u8")
mimetypes.add_type("video/mp2t", ".ts")
mimetypes.add_type("video/x-matroska", ".mkv")
mimetypes.add_type("text/vtt", ".vtt")
mimetypes.add_type("application/x-subrip", ".srt")


# --- Cases -----------------------------------------------------------------------------------------------
# The source maps these onto Hoster/Video objects. A hoster with "throws" makes getVideoList(hoster) throw;
# "lazy" marks Hoster.lazy. Video "headers" are Video.headers (None = use the source's headers).

def video(path, quality="720p", **extra):
    return {"url": BASE + path, "quality": quality, **extra}


def hoster(name, videos=None, **extra):
    return {"name": name, "videos": videos or [], **extra}


SUBS = [
    {"url": BASE + "/media/en.srt", "lang": "en", "title": "English (SRT)"},
    {"url": BASE + "/media/en.vtt", "lang": "en", "title": "English (WebVTT)"},
    {"url": BASE + "/media/ja.srt", "lang": "ja", "title": "日本語 (SRT)"},
]

CASES: dict[str, dict] = {
    "01": {"title": "01 plain mp4", "thumb": 1, "hosters": [hoster("Fixture", [video("/media/ok.mp4")])]},
    "02": {"title": "02 hls variants", "thumb": 2,
           "hosters": [hoster("Fixture", [video("/media/hls/master.m3u8", "auto")])]},
    "03": {"title": "03 first hoster throws", "thumb": 3, "hosters": [
        hoster("A (throws)", throws=True),
        hoster("B", [video("/media/ok.mp4")]),
    ]},
    "04": {"title": "04 lazy hoster", "hosters": [
        hoster("A (lazy, 480p)", [video("/media/hls/480/index.m3u8", "480p")], lazy=True),
        hoster("B (720p)", [video("/media/hls/720/index.m3u8", "720p")]),
    ]},
    "05": {"title": "05 preferred video", "hosters": [hoster("Fixture", [
        video("/media/hls/1080/index.m3u8", "1080p"),
        video("/media/hls/720/index.m3u8", "720p (preferred)", preferred=True),
        video("/media/hls/480/index.m3u8", "480p"),
    ])]},
    "06": {"title": "06 needs source headers", "hosters": [hoster("Fixture", [video("/referer/ok.mp4")])]},
    "07": {"title": "07 needs video headers", "hosters": [
        hoster("Fixture", [video("/header/ok.mp4", headers={"X-Fixture": "video"})]),
    ]},
    "08": {"title": "08 mpvArgs", "hosters": [hoster("Fixture", [video(
        "/media/ok.mp4", mpvArgs=[["demuxer-lavf-o", "probesize=5000000"], ["script", "x"]],
    )])]},
    "09": {"title": "09 external srt + vtt + ja", "hosters": [
        hoster("Fixture", [video("/media/ok.mp4", subtitleTracks=SUBS)]),
    ]},
    "10": {"title": "10 mkv tracks + chapters", "hosters": [hoster("Fixture", [video("/media/tracks.mkv")])]},
    "11": {"title": "11 timestamps", "hosters": [hoster("Fixture", [video("/media/ok.mp4", timestamps=[
        {"start": 5.0, "end": 15.0, "name": "Opening", "type": "Opening"},
        {"start": 45.0, "end": 55.0, "name": "Ending", "type": "Ending"},
    ])])]},
    "12": {"title": "12 all hosters fail", "hosters": [
        hoster("A", [video("/fail/ok.mp4")]),
        hoster("B", [video("/fail/ok.mp4")]),
    ]},
    "13": {"title": "13 slow", "hosters": [hoster("Fixture", [video("/slow/ok.mp4")])]},
    "14": {"title": "14 stall mid-stream", "hosters": [hoster("Fixture", [video("/stall/ok.mp4")])]},
    "15": {"title": "15 sortVideos honours quality", "hosters": [hoster("Fixture", [
        video("/media/hls/1080/index.m3u8", "1080p"),
        video("/media/hls/720/index.m3u8", "720p"),
        video("/media/hls/480/index.m3u8", "480p"),
    ])]},
    # Served by FixtureNoHosterAnimeSource, which only implements getVideoList(episode).
    "16": {"title": "16 lib16 no hosters", "noHosters": True,
           "hosters": [hoster("Fixture", [video("/media/ok.mp4")])]},
}


def anime_list() -> list[dict]:
    return [
        {"case": key, "title": case["title"], "url": f"/anime/{key}",
         "thumbnail": f"{BASE}/media/thumb-0{case.get('thumb', 1)}.jpg", "noHosters": bool(case.get("noHosters"))}
        for key, case in CASES.items()
    ]


# --new-episodes: every case gains episode 3, so a library refresh after favouriting finds a new episode
# (Updates tests). Set from the command line; tests set it directly.
NEW_EPISODES = False


def episodes(key: str) -> list[dict]:
    case = CASES[key]
    thumb = case.get("thumb")
    # Two episodes so next/previous work. Fixed dates keep the JSON stable (2026-01-01 / 2026-01-08 UTC).
    numbers = (3, 2, 1) if NEW_EPISODES else (2, 1)
    return [
        {"url": f"/episode/{key}/{n}", "name": f"Episode {n}", "number": float(n),
         "dateUpload": 1767225600000 + (n - 1) * 604800000,
         "previewUrl": f"{BASE}/media/thumb-0{thumb}.jpg" if thumb else None}
        for n in numbers
    ]


def videos(key: str) -> dict:
    case = CASES[key]
    return {"case": key, "noHosters": bool(case.get("noHosters")), "hosters": case["hosters"]}


# --- HTTP ------------------------------------------------------------------------------------------------

RANGE_RE = re.compile(r"bytes=(\d*)-(\d*)$")


class Handler(BaseHTTPRequestHandler):
    server_version = "TsuzukiFixture/1"
    protocol_version = "HTTP/1.1"
    slow_seconds = 20.0
    stall_seconds = 30.0

    # One line per request, written by us (not the default access log).
    def log_message(self, format, *args):  # noqa: A002 - BaseHTTPRequestHandler's signature
        pass

    def _log(self, status: int) -> None:
        h = self.headers
        line = "{} {} {} {} referer={!r} ua={!r} x-fixture={!r}".format(
            time.strftime("%H:%M:%S"), self.command, self.path, status,
            h.get("Referer"), h.get("User-Agent"), h.get("X-Fixture"),
        )
        print(line, flush=True)

    def do_HEAD(self):
        self._handle(head=True)

    def do_GET(self):
        self._handle(head=False)

    def _handle(self, head: bool) -> None:
        path = unquote(urlsplit(self.path).path)
        try:
            if path.startswith("/api/"):
                return self._api(path[len("/api/"):], head)
            route, _, rest = path.lstrip("/").partition("/")
            if route == "media":
                return self._file(rest, head)
            if route == "referer":
                if self.headers.get("Referer") != REFERER:
                    return self._status(HTTPStatus.FORBIDDEN, "Referer required", head)
                return self._file(rest, head)
            if route == "ua":
                if UA_MARK not in (self.headers.get("User-Agent") or ""):
                    return self._status(HTTPStatus.FORBIDDEN, "User-Agent must contain TsuzukiFixture", head)
                return self._file(rest, head)
            if route == "header":
                if self.headers.get("X-Fixture") != "video":
                    return self._status(HTTPStatus.FORBIDDEN, "X-Fixture: video required", head)
                return self._file(rest, head)
            if route == "fail":
                return self._status(HTTPStatus.INTERNAL_SERVER_ERROR, "Fixture failure", head)
            if route == "slow":
                time.sleep(self.slow_seconds)
                return self._file(rest, head)
            if route == "stall":
                return self._file(rest, head, stall=True)
            return self._status(HTTPStatus.NOT_FOUND, "Not found", head)
        except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
            pass  # mpv closes connections when it seeks; not an error.

    def _api(self, name: str, head: bool) -> None:
        if name == "anime.json":
            return self._json(anime_list(), head)
        m = re.fullmatch(r"(episodes|videos)/(\d\d)\.json", name)
        if m and m.group(2) in CASES:
            return self._json(episodes(m.group(2)) if m.group(1) == "episodes" else videos(m.group(2)), head)
        return self._status(HTTPStatus.NOT_FOUND, "Unknown case", head)

    def _json(self, data, head: bool) -> None:
        body = json.dumps(data, ensure_ascii=False, indent=2).encode("utf-8")
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self._log(200)
        if not head:
            self.wfile.write(body)

    def _status(self, status: HTTPStatus, message: str, head: bool) -> None:
        body = (message + "\n").encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self._log(int(status))
        if not head:
            self.wfile.write(body)

    def _file(self, rel: str, head: bool, stall: bool = False) -> None:
        full = os.path.realpath(os.path.join(MEDIA, rel))
        if not full.startswith(os.path.realpath(MEDIA) + os.sep) or not os.path.isfile(full):
            return self._status(HTTPStatus.NOT_FOUND, "No such media (run make-media.sh)", head)
        size = os.path.getsize(full)
        start, end = 0, size - 1
        status = HTTPStatus.OK
        rng = self.headers.get("Range")
        if rng:
            m = RANGE_RE.match(rng.strip())
            if not m or (not m.group(1) and not m.group(2)):
                return self._unsatisfiable(size, head)
            if m.group(1):
                start = int(m.group(1))
                end = min(int(m.group(2)), size - 1) if m.group(2) else size - 1
            else:  # suffix range: last N bytes
                start = max(0, size - int(m.group(2)))
            if start >= size or start > end:
                return self._unsatisfiable(size, head)
            status = HTTPStatus.PARTIAL_CONTENT
        length = end - start + 1
        ctype = mimetypes.guess_type(full)[0] or "application/octet-stream"
        self.send_response(status)
        self.send_header("Content-Type", ctype)
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(length))
        if status == HTTPStatus.PARTIAL_CONTENT:
            self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        self.end_headers()
        self._log(int(status))
        if head:
            return
        with open(full, "rb") as f:
            f.seek(start)
            remaining = length
            # Stall: roughly the first 2 s of a 60 s file, then a pause, then the rest.
            stall_at = max(1, size * 2 // 60) if stall and start == 0 else None
            sent = 0
            while remaining > 0:
                chunk = f.read(min(64 * 1024, remaining))
                if not chunk:
                    break
                if stall_at is not None and sent < stall_at <= sent + len(chunk):
                    cut = stall_at - sent
                    self.wfile.write(chunk[:cut])
                    self.wfile.flush()
                    time.sleep(self.stall_seconds)
                    chunk = chunk[cut:]
                    sent += cut
                    remaining -= cut
                    stall_at = None
                self.wfile.write(chunk)
                sent += len(chunk)
                remaining -= len(chunk)

    def _unsatisfiable(self, size: int, head: bool) -> None:
        self.send_response(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
        self.send_header("Content-Range", f"bytes */{size}")
        self.send_header("Content-Length", "0")
        self.end_headers()
        self._log(416)


class FixtureServer(ThreadingHTTPServer):
    daemon_threads = True

    def handle_error(self, request, client_address):
        # Players drop connections when they seek or close; keep the request log readable.
        if isinstance(sys.exc_info()[1], (BrokenPipeError, ConnectionResetError, ConnectionAbortedError)):
            return
        super().handle_error(request, client_address)


def make_server(port: int, slow_seconds: float, stall_seconds: float) -> ThreadingHTTPServer:
    handler = type("FixtureHandler", (Handler,), {"slow_seconds": slow_seconds, "stall_seconds": stall_seconds})
    server = FixtureServer((HOST, port), handler)
    return server


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--slow-seconds", type=float, default=20.0)
    parser.add_argument("--stall-seconds", type=float, default=30.0)
    parser.add_argument("--new-episodes", action="store_true", help="every case gains episode 3 (Updates tests)")
    args = parser.parse_args(argv)
    global NEW_EPISODES
    NEW_EPISODES = args.new_episodes
    if not os.path.isfile(os.path.join(MEDIA, "ok.mp4")):
        print("media/ is empty: run `bash scripts/anime-fixture/make-media.sh` first.", file=sys.stderr)
        return 1
    if args.port != 8765:
        print("Note: the fixture source and the case JSON use port 8765; other ports are for tests only.")
    server = make_server(args.port, args.slow_seconds, args.stall_seconds)
    print(f"Tsuzuki anime fixture on http://{HOST}:{args.port}  ({len(CASES)} cases)", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
