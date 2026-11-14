import hashlib
import json
import sqlite3
import sys
import tempfile
import unittest
from contextlib import closing
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from build_dictionary_pack import WORDNET_POS, build_pack


class DictionaryPackTest(unittest.TestCase):
    def test_jmdict_indexes_writing_and_reading_and_preserves_sense_restrictions(self):
        source = Path(__file__).with_name("fixtures") / "jmdict-tiny.xml"
        with tempfile.TemporaryDirectory() as tmp:
            output = self.build("jmdict", source, Path(tmp))
            with closing(sqlite3.connect(output)) as db:
                senses = db.execute(
                    "SELECT headword, part, gloss FROM entry JOIN sense ON sense.entry = entry.id "
                    "ORDER BY headword, ordinal"
                ).fetchall()
                self.assertEqual(len(senses), 3)
                self.assertIn(("試せる", "Ichidan verb", "restricted fixture meaning"), senses)
                self.assertNotIn(("ためせる", "Ichidan verb", "restricted fixture meaning"), senses)
                self.assertNotIn("fixture excluded", {row[2] for row in senses})
                self.assertEqual(db.execute("SELECT count(*) FROM term WHERE form = 'ためせる'").fetchone(), (2,))

    def test_wordnet_uses_index_order_and_includes_irregular_forms(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for suffix in WORDNET_POS:
                (root / f"data.{suffix}").touch()
                (root / f"index.{suffix}").touch()
                (root / f"{suffix}.exc").touch()
            (root / "data.noun").write_text(
                "00000001 00 n 01 mouse 0 000 | first synthetic sense\n"
                "00000002 00 n 01 mouse 0 000 | second synthetic sense\n",
                encoding="utf-8",
            )
            (root / "index.noun").write_text(
                "mouse n 2 1 @ 2 2 00000002 00000001\n", encoding="utf-8",
            )
            (root / "noun.exc").write_text("mice mouse\n", encoding="utf-8")
            output = self.build("wordnet", root, root)
            with closing(sqlite3.connect(output)) as db:
                self.assertEqual(
                    db.execute("SELECT gloss FROM sense ORDER BY ordinal").fetchall(),
                    [("second synthetic sense",), ("first synthetic sense",)],
                )
                self.assertEqual(db.execute("SELECT lemma FROM exception WHERE form = 'mice'").fetchone(), ("mouse",))

    def build(self, kind, source, root):
        license_file = root / "fixture-license.txt"
        license_file.write_text("Synthetic test data", encoding="utf-8")
        output = root / "fixture.sqlite3"
        build_pack(kind, source, license_file, "fixture", output)
        manifest = json.loads(output.with_suffix(".json").read_text(encoding="utf-8"))
        self.assertEqual(manifest["sizeBytes"], output.stat().st_size)
        self.assertEqual(manifest["sha256"], hashlib.sha256(output.read_bytes()).hexdigest())
        self.assertEqual(manifest["schema"], 1)
        return output


if __name__ == "__main__":
    unittest.main()
