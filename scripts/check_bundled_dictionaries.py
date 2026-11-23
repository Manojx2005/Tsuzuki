#!/usr/bin/env python3
"""Validate the actual packaged dictionaries; catches Android asset renaming/decompression."""

import argparse
import gzip
import hashlib
import json
import sqlite3
import subprocess
import tempfile
import zipfile
from pathlib import Path


def check(read):
    results = {}
    for language in ("en", "ja"):
        prefix = f"dictionaries/dictionary-{language}"
        manifest = json.loads(read(prefix + ".json"))
        data = gzip.decompress(read(prefix + ".pack"))
        assert manifest["schema"] == 1 and manifest["language"] == language
        assert len(data) == manifest["sizeBytes"]
        assert hashlib.sha256(data).hexdigest() == manifest["sha256"]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "dictionary.sqlite3"
            path.write_bytes(data)
            with sqlite3.connect(path) as db:
                assert db.execute("PRAGMA quick_check").fetchone()[0] == "ok"
                meta = dict(db.execute("SELECT key, value FROM metadata"))
                assert meta["language"] == language and meta["version"] == manifest["version"]
                assert meta["license"].strip()
                results[language] = {
                    **manifest,
                    "entries": db.execute("SELECT count(*) FROM entry").fetchone()[0],
                    "senses": db.execute("SELECT count(*) FROM sense").fetchone()[0],
                }
            # sqlite3's context manager commits but does not close the connection on Windows.
            db.close()
    for name in ("CC-BY-SA-4.0.txt", "JMdict-documentation.html", "wordnet-LICENSE.txt", "jmdict-LICENSE.txt"):
        assert read("dictionaries/" + name).strip()
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group()
    source.add_argument("--apk", type=Path)
    source.add_argument("--git", action="store_true", help="Check committed HEAD asset bytes")
    args = parser.parse_args()
    if args.apk:
        with zipfile.ZipFile(args.apk) as archive:
            result = check(lambda name: archive.read("assets/" + name))
    elif args.git:
        root = Path(__file__).resolve().parents[1]
        result = check(lambda name: subprocess.check_output(
            ["git", "show", "HEAD:app/src/main/assets/" + name], cwd=root,
        ))
    else:
        assets = Path(__file__).resolve().parents[1] / "app/src/main/assets"
        result = check(lambda name: (assets / name).read_bytes())
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
