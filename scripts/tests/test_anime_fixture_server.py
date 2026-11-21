"""Anime fixture server (card AY0): routes, header gates, Range support and the case JSON."""
import importlib.util
import json
import tempfile
import threading
import time
import unittest
import urllib.error
import urllib.request
from pathlib import Path

SPEC = importlib.util.spec_from_file_location(
    "serve", Path(__file__).parents[1] / "anime-fixture" / "serve.py"
)
serve = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(serve)

DATA = bytes(range(256)) * 400  # 102,400 bytes


class FixtureServerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        (Path(cls.tmp.name) / "ok.mp4").write_bytes(DATA)
        cls.old_media = serve.MEDIA
        serve.MEDIA = cls.tmp.name
        cls.server = serve.make_server(0, slow_seconds=0.3, stall_seconds=0.3)
        cls.port = cls.server.server_address[1]
        threading.Thread(target=cls.server.serve_forever, daemon=True).start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        serve.MEDIA = cls.old_media
        cls.tmp.cleanup()

    def get(self, path, headers=None, method="GET"):
        req = urllib.request.Request(f"http://127.0.0.1:{self.port}{path}", headers=headers or {}, method=method)
        try:
            with urllib.request.urlopen(req, timeout=10) as r:
                return r.status, dict(r.headers), r.read()
        except urllib.error.HTTPError as e:
            return e.code, dict(e.headers), e.read()

    def test_binds_loopback_only(self):
        self.assertEqual(self.server.server_address[0], "127.0.0.1")

    def test_plain_media_and_head(self):
        status, headers, body = self.get("/media/ok.mp4")
        self.assertEqual((status, body), (200, DATA))
        self.assertEqual(headers["Accept-Ranges"], "bytes")
        status, headers, body = self.get("/media/ok.mp4", method="HEAD")
        self.assertEqual((status, headers["Content-Length"], body), (200, str(len(DATA)), b""))

    def test_range_requests(self):
        status, headers, body = self.get("/media/ok.mp4", {"Range": "bytes=100-199"})
        self.assertEqual((status, body), (206, DATA[100:200]))
        self.assertEqual(headers["Content-Range"], f"bytes 100-199/{len(DATA)}")
        status, _, body = self.get("/media/ok.mp4", {"Range": "bytes=102300-"})
        self.assertEqual((status, body), (206, DATA[102300:]))
        status, _, body = self.get("/media/ok.mp4", {"Range": "bytes=-50"})
        self.assertEqual((status, body), (206, DATA[-50:]))
        status, headers, _ = self.get("/media/ok.mp4", {"Range": f"bytes={len(DATA)}-"})
        self.assertEqual((status, headers["Content-Range"]), (416, f"bytes */{len(DATA)}"))

    def test_header_gates(self):
        self.assertEqual(self.get("/referer/ok.mp4")[0], 403)
        self.assertEqual(self.get("/referer/ok.mp4", {"Referer": "http://127.0.0.1:8765/"})[0], 200)
        self.assertEqual(self.get("/ua/ok.mp4", {"User-Agent": "curl"})[0], 403)
        self.assertEqual(self.get("/ua/ok.mp4", {"User-Agent": "TsuzukiFixture/1"})[0], 200)
        self.assertEqual(self.get("/header/ok.mp4")[0], 403)
        self.assertEqual(self.get("/header/ok.mp4", {"X-Fixture": "video"})[0], 200)

    def test_fail_slow_and_stall(self):
        self.assertEqual(self.get("/fail/ok.mp4")[0], 500)
        start = time.monotonic()
        status, _, body = self.get("/slow/ok.mp4")
        self.assertGreaterEqual(time.monotonic() - start, 0.25)  # Windows timer granularity
        self.assertEqual((status, body), (200, DATA))
        start = time.monotonic()
        status, _, body = self.get("/stall/ok.mp4")
        self.assertGreaterEqual(time.monotonic() - start, 0.25)  # Windows timer granularity
        self.assertEqual((status, body), (200, DATA))

    def test_paths_cannot_leave_media(self):
        self.assertEqual(self.get("/media/../serve.py")[0], 404)
        self.assertEqual(self.get("/media/%2e%2e/serve.py")[0], 404)
        self.assertEqual(self.get("/media/missing.mp4")[0], 404)
        self.assertEqual(self.get("/nowhere")[0], 404)

    def test_case_json(self):
        status, headers, body = self.get("/api/anime.json")
        self.assertEqual(status, 200)
        self.assertIn("application/json", headers["Content-Type"])
        anime = json.loads(body)
        self.assertEqual([a["case"] for a in anime], [f"{n:02d}" for n in range(1, 17)])
        self.assertEqual([a["case"] for a in anime if a["noHosters"]], ["16"])

        episodes = json.loads(self.get("/api/episodes/01.json")[2])
        self.assertEqual([e["number"] for e in episodes], [2.0, 1.0])
        self.assertTrue(episodes[0]["previewUrl"].endswith("/media/thumb-01.jpg"))

        three = json.loads(self.get("/api/videos/03.json")[2])
        self.assertEqual([h.get("throws", False) for h in three["hosters"]], [True, False])
        four = json.loads(self.get("/api/videos/04.json")[2])
        self.assertEqual([h.get("lazy", False) for h in four["hosters"]], [True, False])
        five = json.loads(self.get("/api/videos/05.json")[2])["hosters"][0]["videos"]
        self.assertEqual([v.get("preferred", False) for v in five], [False, True, False])
        seven = json.loads(self.get("/api/videos/07.json")[2])["hosters"][0]["videos"][0]
        self.assertEqual(seven["headers"], {"X-Fixture": "video"})
        nine = json.loads(self.get("/api/videos/09.json")[2])["hosters"][0]["videos"][0]
        self.assertEqual([s["lang"] for s in nine["subtitleTracks"]], ["en", "en", "ja"])
        self.assertEqual(self.get("/api/videos/99.json")[0], 404)

    def test_new_episodes_flag_adds_episode_three(self):
        try:
            serve.NEW_EPISODES = True
            episodes = json.loads(self.get("/api/episodes/01.json")[2])
            self.assertEqual([e["number"] for e in episodes], [3.0, 2.0, 1.0])
        finally:
            serve.NEW_EPISODES = False
        self.assertEqual(len(json.loads(self.get("/api/episodes/01.json")[2])), 2)

    def test_every_media_url_in_the_cases_is_made_by_make_media(self):
        made = {"ok.mp4", "tracks.mkv", "en.srt", "en.vtt", "ja.srt", "thumb-01.jpg", "thumb-02.jpg",
                "thumb-03.jpg", "hls/master.m3u8", "hls/1080/index.m3u8", "hls/720/index.m3u8",
                "hls/480/index.m3u8"}
        script = (Path(__file__).parents[1] / "anime-fixture" / "make-media.sh").read_text(encoding="utf-8")
        for name in ("ok.mp4", "tracks.mkv", "en.srt", "en.vtt", "ja.srt", "thumb-0", "master.m3u8"):
            self.assertIn(name, script)
        text = json.dumps([serve.videos(k) for k in serve.CASES]) + json.dumps(serve.anime_list())
        for url in set(__import__("re").findall(r'http://127\.0\.0\.1:8765/[a-z]+/([^"]+)', text)):
            self.assertIn(url, made, url)


if __name__ == "__main__":
    unittest.main()
