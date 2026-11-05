"""Exercise the real migration and SQLDelight statements with SQLite, without a device."""
from pathlib import Path
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCE = (ROOT / "data/src/main/sqldelight/tachiyomi/data/tsuzukiAnime.sq").read_text(encoding="utf-8")
MIGRATION = (ROOT / "data/src/main/sqldelight/tachiyomi/migrations/36.sqm").read_text(encoding="utf-8")
LABELS = list(re.finditer(r"(?m)^(\w+):\s*\n", SOURCE))
QUERIES = {
    match[1]: SOURCE[match.end():LABELS[index + 1].start() if index + 1 < len(LABELS) else len(SOURCE)]
    for index, match in enumerate(LABELS)
}


class AnimeSchemaTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.execute("PRAGMA foreign_keys=ON")
        self.db.executescript("""
            CREATE TABLE mangas (_id INTEGER PRIMARY KEY, title TEXT, favorite INTEGER, is_novel INTEGER);
            CREATE TABLE chapters (_id INTEGER PRIMARY KEY, manga_id INTEGER, read INTEGER, last_page_read INTEGER);
            CREATE TABLE history (_id INTEGER PRIMARY KEY, chapter_id INTEGER, last_read INTEGER);
            INSERT INTO mangas VALUES (1, 'Owner novel', 1, 1), (2, 'Owner manga', 1, 0);
            INSERT INTO chapters VALUES (5, 1, 0, 80);
            INSERT INTO history VALUES (9, 5, 123456);
        """)
        self.db.executescript(MIGRATION)

    def tearDown(self):
        self.db.close()

    def add(self, source=42, url="/series", date=100):
        self.db.execute(QUERIES["addAnimeFavorite"], dict(
            source=source, url=url, title="Real source title", thumbnailUrl="https://example.org/cover.jpg",
            description=None, genre=None, status=1, dateAdded=date,
        ))
        return self.db.execute(QUERIES["getAnimeIdBySourceUrl"], dict(source=source, url=url)).fetchone()[0]

    def episode(self, anime_id, name="Episode 1", upload=10, fetched=100):
        self.db.execute(QUERIES["upsertAnimeEpisode"], dict(
            animeId=anime_id, url="/episode-1", name=name, scanlator=None, episodeNumber=1.0,
            sourceOrder=0, dateUpload=upload, dateFetch=fetched,
        ))

    def test_migration_preserves_reader_rows(self):
        self.assertEqual(self.db.execute("SELECT * FROM mangas ORDER BY _id").fetchall(),
                         [(1, "Owner novel", 1, 1), (2, "Owner manga", 1, 0)])
        self.assertEqual(self.db.execute("SELECT * FROM chapters").fetchall(), [(5, 1, 0, 80)])
        self.assertEqual(self.db.execute("SELECT * FROM history").fetchall(), [(9, 5, 123456)])

    def test_fresh_schema_and_migration_match(self):
        with sqlite3.connect(":memory:") as fresh:
            fresh.executescript(SOURCE[:LABELS[0].start()])
            expected = fresh.execute("SELECT name, sql FROM sqlite_master WHERE name LIKE 'tsuzuki_%' ORDER BY name").fetchall()
        actual = self.db.execute("SELECT name, sql FROM sqlite_master WHERE name LIKE 'tsuzuki_%' ORDER BY name").fetchall()
        self.assertEqual(actual, expected)

    def test_add_is_idempotent_and_preserves_watch_status_and_added_date(self):
        anime_id = self.add()
        self.db.execute(QUERIES["setAnimeWatchStatus"], dict(id=anime_id, watchStatus=1))
        self.assertEqual(self.add(date=999), anime_id)
        self.assertEqual(self.db.execute("SELECT count(*), watch_status, date_added FROM tsuzuki_anime").fetchone(),
                         (1, 1, 100))

    def test_source_is_part_of_identity(self):
        self.assertNotEqual(self.add(source=42), self.add(source=43))

    def test_refresh_preserves_episode_id_and_user_state(self):
        anime_id = self.add()
        self.episode(anime_id)
        self.db.execute("UPDATE tsuzuki_episodes SET seen=1, bookmark=1, last_position_ms=700, duration_ms=1400")
        before = self.db.execute("SELECT _id, seen, bookmark, last_position_ms, duration_ms, date_fetch FROM tsuzuki_episodes").fetchone()
        self.episode(anime_id, name="Updated episode title", upload=20, fetched=999)
        after = self.db.execute("SELECT _id, seen, bookmark, last_position_ms, duration_ms, date_fetch FROM tsuzuki_episodes").fetchone()
        self.assertEqual(before, after)
        self.assertEqual(self.db.execute("SELECT name, date_upload FROM tsuzuki_episodes").fetchone(),
                         ("Updated episode title", 20))

    def test_missing_upload_date_does_not_erase_existing_date(self):
        anime_id = self.add()
        self.episode(anime_id)
        self.episode(anime_id, upload=0)
        self.assertEqual(self.db.execute("SELECT date_upload FROM tsuzuki_episodes").fetchone()[0], 10)

    def test_unfavorite_and_readd_preserve_episodes_and_identity(self):
        anime_id = self.add()
        self.episode(anime_id)
        self.db.execute(QUERIES["removeAnimeFavorite"], dict(id=anime_id))
        self.assertEqual(self.db.execute(QUERIES["getAnimeLibrary"]).fetchall(), [])
        self.assertEqual(self.add(date=999), anime_id)
        self.assertEqual(self.db.execute("SELECT count(*) FROM tsuzuki_episodes").fetchone()[0], 1)
        self.assertEqual(self.db.execute("SELECT date_added FROM tsuzuki_anime").fetchone()[0], 999)

    def test_library_counts_use_real_rows(self):
        anime_id = self.add()
        self.episode(anime_id)
        self.db.execute("UPDATE tsuzuki_episodes SET seen=1")
        self.assertEqual(self.db.execute(QUERIES["getAnimeMediaCounts"]).fetchone(), (1, 1, 1))
        row = self.db.execute(QUERIES["getAnimeLibrary"]).fetchone()
        self.assertEqual(row[-2:], (1, 1))

    def test_foreign_keys_reject_orphans_and_cascade_only_anime(self):
        with self.assertRaises(sqlite3.IntegrityError):
            self.episode(999)
        anime_id = self.add()
        self.episode(anime_id)
        self.db.execute("DELETE FROM tsuzuki_anime WHERE _id=?", (anime_id,))
        self.assertEqual(self.db.execute("SELECT count(*) FROM tsuzuki_episodes").fetchone()[0], 0)
        self.assertEqual(self.db.execute("SELECT last_page_read FROM chapters").fetchone()[0], 80)

    def test_invalid_watch_status_and_progress_are_rejected(self):
        anime_id = self.add()
        self.episode(anime_id)
        for statement in ["UPDATE tsuzuki_anime SET watch_status=8", "UPDATE tsuzuki_episodes SET last_position_ms=-1"]:
            with self.assertRaises(sqlite3.IntegrityError):
                self.db.execute(statement)


if __name__ == "__main__":
    unittest.main()
