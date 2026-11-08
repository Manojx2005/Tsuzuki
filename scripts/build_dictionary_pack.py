#!/usr/bin/env python3
"""Build a Tsuzuki offline dictionary pack from local WordNet or JMdict data."""

import argparse
import gzip
import hashlib
import json
import sqlite3
import tempfile
import unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path

SCHEMA = 1
WORDNET_POS = {"noun": "noun", "verb": "verb", "adj": "adjective", "adv": "adverb"}
CREATE_SQL = """
PRAGMA user_version = 1;
CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL) WITHOUT ROWID;
CREATE TABLE entry (id INTEGER PRIMARY KEY, headword TEXT NOT NULL, reading TEXT NOT NULL);
CREATE TABLE term (form TEXT NOT NULL, entry INTEGER NOT NULL, PRIMARY KEY (form, entry)) WITHOUT ROWID;
CREATE TABLE sense (
    entry INTEGER NOT NULL,
    ordinal INTEGER NOT NULL,
    part TEXT NOT NULL,
    gloss TEXT NOT NULL,
    PRIMARY KEY (entry, ordinal)
) WITHOUT ROWID;
CREATE TABLE exception (form TEXT NOT NULL, lemma TEXT NOT NULL, PRIMARY KEY (form, lemma)) WITHOUT ROWID;
"""


def normalize(term):
    return unicodedata.normalize("NFKC", term).replace("_", " ").strip().lower()


def insert_entry(db, headword, reading, forms, senses):
    entry_id = db.execute(
        "INSERT INTO entry(headword, reading) VALUES (?, ?)", (headword, reading)
    ).lastrowid
    terms = {normalize(form) for form in forms if form}
    db.executemany("INSERT INTO term VALUES (?, ?)", ((form, entry_id) for form in sorted(terms)))
    db.executemany(
        "INSERT INTO sense VALUES (?, ?, ?, ?)",
        ((entry_id, order, part, gloss) for order, (part, gloss) in enumerate(senses)),
    )


def import_wordnet(db, directory):
    for suffix, part in WORDNET_POS.items():
        glosses = {}
        with (directory / f"data.{suffix}").open(encoding="utf-8") as source:
            for line in source:
                if line[:1].isdigit():
                    address, _, gloss = line.partition("|")
                    glosses[address.split()[0]] = gloss.strip()
        with (directory / f"index.{suffix}").open(encoding="utf-8") as source:
            for line in source:
                if not line.strip() or line[0].isspace():
                    continue
                fields = line.split()
                headword = fields[0].replace("_", " ")
                offset_start = 6 + int(fields[3])
                senses = [(part, glosses[offset]) for offset in fields[offset_start:]]
                if senses:
                    insert_entry(db, headword, "", [headword], senses)
        with (directory / f"{suffix}.exc").open(encoding="utf-8") as source:
            for line in source:
                parts = line.split()
                if parts:
                    form, *lemmas = parts
                    db.executemany(
                        "INSERT OR IGNORE INTO exception VALUES (?, ?)",
                        ((normalize(form), normalize(lemma)) for lemma in lemmas),
                    )


def jmdict_senses(entry):
    parts = []
    for sense in entry.findall("sense"):
        parts = [pos.text for pos in sense.findall("pos") if pos.text] or parts
        glosses = [
            gloss.text for gloss in sense.findall("gloss")
            if gloss.text and gloss.get("{http://www.w3.org/XML/1998/namespace}lang", "eng") == "eng"
        ]
        if glosses:
            yield (
                {node.text for node in sense.findall("stagk")},
                {node.text for node in sense.findall("stagr")},
                ", ".join(parts),
                "; ".join(glosses),
            )


def import_jmdict(db, path):
    open_input = gzip.open if path.suffix == ".gz" else open
    with open_input(path, "rb") as source:
        events = ET.iterparse(source, events=("start", "end"))
        _, root = next(events)
        if root.tag != "JMdict":
            raise ValueError("Expected a JMdict XML document")
        for event, entry in events:
            if event != "end" or entry.tag != "entry":
                continue
            forms = [node.text for node in entry.findall("k_ele/keb") if node.text]
            senses = list(jmdict_senses(entry))
            for reading_element in entry.findall("r_ele"):
                reading = reading_element.findtext("reb")
                if not reading:
                    continue
                restrictions = [node.text for node in reading_element.findall("re_restr") if node.text]
                applicable = [] if reading_element.find("re_nokanji") is not None else restrictions or forms
                for headword in applicable or [reading]:
                    definitions = [
                        (part, gloss) for written, spoken, part, gloss in senses
                        if (not written or headword in written) and (not spoken or reading in spoken)
                    ]
                    if definitions:
                        insert_entry(db, headword, reading, [headword, reading], definitions)
            root.remove(entry)


def build_pack(kind, source, license_file, version, output):
    if output.suffix != ".sqlite3":
        raise ValueError("The output must have a .sqlite3 extension")
    version = version.strip()
    if not version or len(version) > 80:
        raise ValueError("Provide a version of 1 to 80 characters")
    language = "en" if kind == "wordnet" else "ja"
    attribution = "Princeton University WordNet" if kind == "wordnet" else "EDRDG JMdict"
    license_text = license_file.read_text(encoding="utf-8").strip()
    if not license_text:
        raise ValueError("Provide the source distribution's license text")
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=output.parent) as temp_dir:
        temp_path = Path(temp_dir) / output.name
        db = sqlite3.connect(temp_path)
        try:
            with db:
                db.executescript(CREATE_SQL)
                db.executemany(
                    "INSERT INTO metadata VALUES (?, ?)",
                    {
                        "language": language,
                        "version": version,
                        "attribution": attribution,
                        "license": license_text,
                    }.items(),
                )
                if kind == "wordnet":
                    import_wordnet(db, source)
                else:
                    import_jmdict(db, source)
                if not db.execute("SELECT 1 FROM entry LIMIT 1").fetchone():
                    raise ValueError("The input produced no dictionary entries")
            db.execute("VACUUM")
        finally:
            db.close()
        with temp_path.open("rb") as content:
            sha256 = hashlib.file_digest(content, "sha256").hexdigest()
        manifest = {
            "schema": SCHEMA,
            "language": language,
            "version": version,
            "sizeBytes": temp_path.stat().st_size,
            "sha256": sha256,
        }
        manifest_path = temp_path.with_suffix(".json")
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        temp_path.replace(output)
        manifest_path.replace(output.with_suffix(".json"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("kind", choices=("wordnet", "jmdict"))
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--license", type=Path, required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    build_pack(args.kind, args.input, args.license, args.version, args.output)


if __name__ == "__main__":
    main()
