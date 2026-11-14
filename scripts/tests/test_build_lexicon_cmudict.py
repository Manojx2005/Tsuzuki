"""CMUdict -> Kokoro lexicon conversion (NT-LEX1)."""
import hashlib
import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).parents[1] / "neural-tts/build-lexicon-cmudict.py"
VOCAB_FIXTURE = Path(__file__).parent / "fixtures/kokoro-vocab-symbols.txt"
PINNED_CACHE = Path(__file__).parents[2] / ".native/archives"

spec = importlib.util.spec_from_file_location("build_lexicon_cmudict", SCRIPT)
lex = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lex)

PRIMARY, SECONDARY = "ˈ", "ˌ"


def vocabulary():
    lines = VOCAB_FIXTURE.read_text(encoding="ascii").splitlines()
    return {chr(int(line[2:], 16)) for line in lines if line.startswith("U+")}


def convert(phones, flap=lex.FLAP_MISAKI):
    return lex.arpabet_to_kokoro(phones.split(), flap)


def symbol_after(text, mark):
    return text[text.index(mark) + 1]


ALL_PHONES = list(lex.CONSONANTS) + [
    base + stress for base in sorted(lex.VOWEL_BASES) for stress in "012"
]


class MappingTest(unittest.TestCase):
    def test_g_is_script_g(self):
        self.assertEqual("ɡ", convert("G"))
        self.assertNotIn("g", convert("G AH1 G"))
        self.assertEqual("ɡˈʊɡ", convert("G UH1 G"))

    def test_five_diphthongs_are_single_capitals(self):
        for arpabet, kokoro in {"AY": "I", "EY": "A", "OW": "O", "AW": "W", "OY": "Y"}.items():
            self.assertEqual(kokoro, convert(arpabet + "0"))
            self.assertEqual(PRIMARY + kokoro, convert(arpabet + "1"))
            self.assertEqual(SECONDARY + kokoro, convert(arpabet + "2"))

    def test_er_depends_on_stress(self):
        self.assertEqual("əɹ", convert("ER0"))
        self.assertEqual("ˈɜɹ", convert("ER1"))
        self.assertEqual("ˌɜɹ", convert("ER2"))

    def test_ah_depends_on_stress(self):
        self.assertEqual("ə", convert("AH0"))
        self.assertEqual("ˈʌ", convert("AH1"))
        self.assertEqual("ˌʌ", convert("AH2"))

    def test_consonants(self):
        self.assertEqual(
            "ʧʤʃʒθðŋɹj", convert("CH JH SH ZH TH DH NG R Y")
        )

    def test_flap_after_vowel_before_unstressed_vowel(self):
        self.assertEqual("wˈɔTəɹ", convert("W AO1 T ER0"))
        self.assertEqual("sˈɪTi", convert("S IH1 T IY0"))
        self.assertEqual("wˈATɪŋ", convert("W EY1 T IH0 NG"))

    def test_flap_symbol_can_be_the_ipa_flap(self):
        self.assertEqual("sˈɪɾi", convert("S IH1 T IY0", lex.FLAP_IPA))

    def test_no_flap_when_next_vowel_is_stressed(self):
        self.assertEqual("ətˈæk", convert("AH0 T AE1 K"))
        self.assertEqual("ətˌæk", convert("AH0 T AE2 K"))

    def test_no_flap_at_word_edges_or_after_a_consonant(self):
        self.assertEqual("tˈɪ", convert("T IH1"))
        self.assertEqual("ˈɪt", convert("IH1 T"))
        self.assertEqual("pˈɑɹti", convert("P AA1 R T IY0"))
        self.assertEqual("ˈɪŋtə", convert("IH1 NG T AH0"))

    def test_d_never_flaps(self):
        self.assertEqual("lˈædəɹ", convert("L AE1 D ER0"))
        self.assertEqual("lˈædəɹ", convert("L AE1 D ER0", lex.FLAP_IPA))

    def test_stress_mark_sits_immediately_before_its_vowel(self):
        self.assertEqual("əbˈWt", convert("AH0 B AW1 T"))
        self.assertEqual("ˌɛkstɹəkt", convert("EH2 K S T R AH0 K T"))
        out = convert("K AH2 M P L IY1 T")
        self.assertEqual("ʌ", symbol_after(out, SECONDARY))
        self.assertEqual("i", symbol_after(out, PRIMARY))
        self.assertEqual("kˌʌmplˈit", out)

    def test_stress_zero_has_no_mark(self):
        out = convert("AA0 AE0 AH0 AO0 IH0 UW0 ER0")
        self.assertNotIn(PRIMARY, out)
        self.assertNotIn(SECONDARY, out)

    def test_unknown_phone_is_rejected(self):
        for bad in ("XX1", "AH5", "AH"):  # unknown phone, bad stress digit, vowel without a digit
            with self.assertRaises(ValueError, msg=bad):
                convert(bad)

    def test_every_symbol_is_in_the_kokoro_vocabulary(self):
        vocab = vocabulary()
        self.assertEqual(114, len(vocab))
        for flap in (lex.FLAP_MISAKI, lex.FLAP_IPA):
            for phone in ALL_PHONES:
                for left, right in (("", ""), ("AH1 ", " AH0"), ("AH1 ", " AH1")):
                    out = lex.arpabet_to_kokoro((left + phone + right).split(), flap)
                    self.assertLessEqual(set(out), vocab, f"{phone} -> {out!r}")

    def test_full_pinned_dictionary_if_cached(self):
        cached = PINNED_CACHE / f"{lex.CMUDICT_COMMIT[:12]}-cmudict.dict"
        if not cached.exists() or lex.sha256_file(cached) != lex.CMUDICT_SHA256:
            self.skipTest("pinned cmudict.dict is not in .native/archives")
        with open(cached, encoding="utf-8", newline="") as f:
            entries = lex.build_entries(f)
        self.assertEqual(126052, len(entries))
        vocab = vocabulary()
        for word, phonemes in entries:
            self.assertLessEqual(set(phonemes), vocab, word)


SAMPLE = """\
a AH0
a(2) EY1
'bout B AW1 T
read R EH1 D
read(2) R IY1 D
read(3) R EH2 D
aalborg AO1 L B AO0 R G # place, danish
# a comment line
zebra Z IY1 B R AH0

colour K AH1 L ER0
"""


class ParsingTest(unittest.TestCase):
    def test_alternates_are_parsed_and_the_first_variant_is_kept(self):
        parsed = list(lex.parse_cmudict(SAMPLE.splitlines()))
        self.assertIn(("read", 2, ["R", "IY1", "D"]), parsed)
        self.assertIn(("read", 3, ["R", "EH2", "D"]), parsed)
        entries = dict(lex.build_entries(SAMPLE.splitlines()))
        self.assertEqual("ə", entries["a"])
        self.assertEqual("ɹˈɛd", entries["read"])
        self.assertNotIn("read(2)", entries)

    def test_lowest_variant_wins_even_if_listed_first(self):
        entries = dict(lex.build_entries(["read(2) R IY1 D", "read R EH1 D"]))
        self.assertEqual("ɹˈɛd", entries["read"])

    def test_trailing_comments_are_stripped(self):
        entries = dict(lex.build_entries(SAMPLE.splitlines()))
        self.assertEqual("ˈɔlbɔɹɡ", entries["aalborg"])
        text = lex.render(lex.build_entries(SAMPLE.splitlines()))
        self.assertNotIn("#", text)
        self.assertNotIn("danish", text)

    def test_comment_lines_and_blank_lines_are_ignored(self):
        words = [w for w, _ in lex.build_entries(SAMPLE.splitlines())]
        self.assertEqual(["'bout", "a", "aalborg", "colour", "read", "zebra"], words)

    def test_words_the_runtime_reader_could_not_read_back_are_skipped(self):
        self.assertEqual([], lex.build_entries(["#x AH0", "a||b AH0"]))

    def test_output_format_matches_the_sherpa_lexicon_line_format(self):
        text = lex.render(lex.build_entries(SAMPLE.splitlines()))
        self.assertNotIn("\r", text)
        self.assertTrue(text.endswith("\n"))
        for line in text.splitlines():
            word, *fields = line.split(" ")
            self.assertEqual(word, word.lower())
            self.assertTrue(fields)
            self.assertTrue(all(len(field) == 1 for field in fields), line)
            self.assertNotIn("||", line)
        self.assertIn("\na ə\n", "\n" + text)
        self.assertIn("'bout b ˈ W t\n", text)

    def test_output_is_sorted(self):
        words = [w for w, _ in lex.build_entries(["zebra Z IY1 B R AH0", "apple AE1 P AH0 L"])]
        self.assertEqual(["apple", "zebra"], words)


class CommandLineTest(unittest.TestCase):
    def run_cli(self, source, output):
        return subprocess.run(
            [sys.executable, "-I", str(SCRIPT), "--input", str(source), "--output", str(output)],
            capture_output=True, text=True, encoding="utf-8",
        )

    def test_same_input_gives_the_same_bytes_and_the_hash_is_printed(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "cmudict.dict"
            source.write_text(SAMPLE, encoding="utf-8", newline="\n")
            first, second = root / "one/lexicon.txt", root / "two/lexicon.txt"
            result = self.run_cli(source, first)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual(0, self.run_cli(source, second).returncode)
            self.assertEqual(first.read_bytes(), second.read_bytes())
            digest = hashlib.sha256(first.read_bytes()).hexdigest()
            self.assertIn(f"sha256: {digest}", result.stdout)
            self.assertIn("entries: 6", result.stdout)
            self.assertNotIn(b"\r", first.read_bytes())
            first.read_bytes().decode("utf-8")


class PinnedDownloadTest(unittest.TestCase):
    def test_pinned_constants_are_well_formed(self):
        self.assertRegex(lex.CMUDICT_COMMIT, r"^[0-9a-f]{40}$")
        self.assertRegex(lex.CMUDICT_SHA256, r"^[0-9a-f]{64}$")
        self.assertIn(lex.CMUDICT_COMMIT, lex.CMUDICT_URL)

    def test_download_with_matching_hash_is_cached(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "source.dict"
            source.write_bytes(b"a AH0\n")
            cache = root / "cache"
            digest = hashlib.sha256(b"a AH0\n").hexdigest()
            path = lex.ensure_pinned(cache, url=source.as_uri(), sha256=digest)
            self.assertEqual(b"a AH0\n", path.read_bytes())
            source.unlink()  # a second call must come from the cache
            self.assertEqual(path, lex.ensure_pinned(cache, url=source.as_uri(), sha256=digest))

    def test_download_with_wrong_hash_is_refused_and_leaves_nothing(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "source.dict"
            source.write_bytes(b"tampered\n")
            cache = root / "cache"
            with self.assertRaisesRegex(ValueError, "refusing"):
                lex.ensure_pinned(cache, url=source.as_uri(), sha256=hashlib.sha256(b"a AH0\n").hexdigest())
            self.assertEqual([], list(cache.iterdir()))

    def test_cached_file_with_wrong_hash_is_refused(self):
        with tempfile.TemporaryDirectory() as temporary:
            cache = Path(temporary)
            (cache / f"{lex.CMUDICT_COMMIT[:12]}-cmudict.dict").write_bytes(b"tampered\n")
            with self.assertRaisesRegex(ValueError, "expected"):
                lex.ensure_pinned(cache)


if __name__ == "__main__":
    unittest.main()
