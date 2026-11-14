#!/usr/bin/env python3
"""Build a US-English pronunciation lexicon for the Kokoro model from CMUdict only.

NT-LEX1. The upstream Kokoro lexicon (sherpa-onnx `lexicon-us-en.txt`, itself a re-encoding of misaki's
gold/silver JSON) has an unsettled licence. CMUdict is BSD-2-style licensed, so this converts it instead.
Nothing from misaki's data or the upstream lexicon files is read, copied or tuned against.

Input : `cmudict.dict` from a pinned commit of github.com/cmusphinx/cmudict (SHA-256 checked, cached outside
        git; `--input` uses a local file instead, for tests).
Output: one line per word, `word p h o n e s` -- the pronunciation as Kokoro symbols, one symbol per
        space-separated field. Sorted by word, LF line endings, UTF-8, no header, so the same input always
        gives the same bytes.

Line format, confirmed against primary sources at sherpa-onnx tag v2.0.1 (commit af2d8d81...):
  * Writer: scripts/kokoro/v1.0/generate_lexicon_en.py `save()`, lines 49-53:
        tokens = " ".join(list(phones)); f.write(f"{word} {tokens}\\n")
    The word is lowercased (`k.lower()`, line 33); every character of the phoneme string is its own field.
  * Reader: sherpa-onnx/csrc/text-utils.cc `ParseLexiconFile`, lines 1778-1855 (this tag): skips empty lines
    and lines starting with '#'; the "old format" branch reads the first whitespace-separated field as the
    word and every following field as a phoneme token; a line containing "||" would be read as the new
    format, so no word may contain it; fields that are a single code point are kept as they are. The word
    key goes through NormalizeLexiconKey (NFC, lowercase, NFC). A repeated key overwrites the earlier one
    (offline-tts-kokoro-impl.h `LoadLexicon`, line 806: `lexicon_str_[key] = phonemes`), so this writer
    emits exactly one pronunciation per word.

Kokoro symbol conventions, checked against hexgrad/misaki (code only) and Kokoro-82M's config.json `vocab`:
  * Diphthongs are single capital letters: AY -> I, EY -> A, OW -> O, AW -> W, OY -> Y.
  * G is U+0261 (script g); ASCII g is not in the vocabulary.
  * CH -> U+02A7, JH -> U+02A4 (both in the vocabulary).
  * Flapped t. The research note says U+027E; misaki's own pipeline for Kokoro-82M v1.0
    (`en.G2P(version=None)`, `misaki/en.py` line 733, `misaki/espeak.py` line 59, main @ fba12365) rewrites U+027E to "T"
    before the model sees it, so the default here is "T". `--flap ipa` (U+027E) is kept for comparison.
"""
import argparse
import hashlib
import re
import sys
import tempfile
import urllib.request
from pathlib import Path

CMUDICT_REPO = "cmusphinx/cmudict"
CMUDICT_COMMIT = "74790861f652b15e4ac49015a90074ad62a27690"
CMUDICT_URL = f"https://raw.githubusercontent.com/{CMUDICT_REPO}/{CMUDICT_COMMIT}/cmudict.dict"
CMUDICT_SHA256 = "81917843c7f44ce2b094ac63873c2c7a4cf802040792c455ba3ca406891c3d22"
CMUDICT_LICENSE_SHA256 = "bd4ce8e44170a5f9f481310ca85c51de3c4f851a65e679b40e603b143bd3542a"

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CACHE = REPO_ROOT / ".native" / "archives"
DEFAULT_OUTPUT = REPO_ROOT / ".native" / "lexicon" / "lexicon-us-en-cmudict.txt"

STRESS_PRIMARY = "ˈ"
STRESS_SECONDARY = "ˌ"
FLAP_MISAKI = "T"
FLAP_IPA = "ɾ"

CONSONANTS = {
    "B": "b", "CH": "ʧ", "D": "d", "DH": "ð", "F": "f", "G": "ɡ", "HH": "h",
    "JH": "ʤ", "K": "k", "L": "l", "M": "m", "N": "n", "NG": "ŋ", "P": "p",
    "R": "ɹ", "S": "s", "SH": "ʃ", "T": "t", "TH": "θ", "V": "v", "W": "w",
    "Y": "j", "Z": "z", "ZH": "ʒ",
}
# AH and ER depend on stress and are handled in vowel().
VOWELS = {
    "AA": "ɑ", "AE": "æ", "AO": "ɔ", "AW": "W", "AY": "I", "EH": "ɛ",
    "EY": "A", "IH": "ɪ", "IY": "i", "OW": "O", "OY": "Y", "UH": "ʊ", "UW": "u",
}
VOWEL_BASES = set(VOWELS) | {"AH", "ER"}
VOWEL_RE = re.compile(r"^([A-Z]+)([012])$")
ALTERNATE_RE = re.compile(r"^(.+)\((\d+)\)$")


def vowel(base, stress):
    """Kokoro symbols for one vowel, without the stress mark."""
    if base == "AH":
        return "ə" if stress == "0" else "ʌ"
    if base == "ER":
        return "əɹ" if stress == "0" else "ɜɹ"
    return VOWELS[base]


def split_phone(phone):
    """Return (base, stress) with stress None for consonants; raise on anything unknown."""
    m = VOWEL_RE.match(phone)
    if m:
        if m.group(1) not in VOWEL_BASES:
            raise ValueError(f"Unknown ARPAbet vowel: {phone}")
        return m.group(1), m.group(2)
    if phone not in CONSONANTS:
        raise ValueError(f"Unknown ARPAbet phone: {phone}")
    return phone, None


def arpabet_to_kokoro(phones, flap=FLAP_MISAKI):
    """Convert a list of ARPAbet phones (stress digits on vowels) to a Kokoro phoneme string.

    T becomes the flap when it follows a vowel and the next phone is an unstressed (digit 0) vowel; D never
    flaps. A stress mark (primary for 1, secondary for 2) goes immediately before its vowel; stress 0 has none.
    """
    parsed = [split_phone(p) for p in phones]
    out = []
    for i, (base, stress) in enumerate(parsed):
        if stress is not None:
            if stress == "1":
                out.append(STRESS_PRIMARY)
            elif stress == "2":
                out.append(STRESS_SECONDARY)
            out.append(vowel(base, stress))
        elif base == "T" and 0 < i < len(parsed) - 1 and parsed[i - 1][1] is not None \
                and parsed[i + 1][1] == "0":
            out.append(flap)
        else:
            out.append(CONSONANTS[base])
    return "".join(out)


def parse_cmudict(lines):
    """Yield (word, variant, phones) from cmudict.dict lines; trailing `# comment` text is dropped.

    `word(2)` alternates give variant 2 (3, ...); a plain word is variant 1.
    """
    for raw in lines:
        fields = raw.split("#", 1)[0].split()
        if len(fields) < 2:
            continue
        word, phones = fields[0], fields[1:]
        variant = 1
        m = ALTERNATE_RE.match(word)
        if m:
            word, variant = m.group(1), int(m.group(2))
        yield word, variant, phones


def build_entries(lines, flap=FLAP_MISAKI):
    """Return a sorted list of (word, phoneme string), one pronunciation (the lowest variant) per word.

    The runtime keeps one pronunciation per key (last one wins), so alternates are parsed but not emitted.
    Words the runtime reader could not read back (starting with '#', or containing '||') are skipped.
    """
    best = {}
    for word, variant, phones in parse_cmudict(lines):
        key = word.lower()
        if key.startswith("#") or "||" in key:
            continue
        if key not in best or variant < best[key][0]:
            best[key] = (variant, phones)
    return [(word, arpabet_to_kokoro(best[word][1], flap)) for word in sorted(best)]


def render(entries):
    """The lexicon file text: `word p h o n e s` per line, LF endings."""
    return "".join(f"{word} {' '.join(phonemes)}\n" for word, phonemes in entries)


def sha256_file(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def ensure_pinned(cache_dir, url=CMUDICT_URL, sha256=CMUDICT_SHA256, name="cmudict.dict"):
    """Return the path of the verified cached file, downloading it first if needed. Refuses on mismatch."""
    cache_dir = Path(cache_dir)
    cache_dir.mkdir(parents=True, exist_ok=True)
    target = cache_dir / f"{CMUDICT_COMMIT[:12]}-{name}"
    if target.exists():
        actual = sha256_file(target)
        if actual != sha256:
            raise ValueError(f"{target} has SHA-256 {actual}, expected {sha256}. Delete it and rerun.")
        return target
    with tempfile.NamedTemporaryFile(dir=cache_dir, suffix=".part", delete=False) as part:
        part_path = Path(part.name)
        try:
            with urllib.request.urlopen(url) as response:
                part.write(response.read())
        except OSError as error:  # URLError is an OSError; includes a Python without CA certificates
            part.close()
            part_path.unlink()
            raise ValueError(
                f"Could not download {url}: {error}. Download it another way, put it at {target} "
                f"(SHA-256 {sha256}) and rerun."
            ) from error
    actual = sha256_file(part_path)
    if actual != sha256:
        part_path.unlink()
        raise ValueError(f"Downloaded {name} has SHA-256 {actual}, expected {sha256}; refusing to use it.")
    part_path.replace(target)
    return target


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--input", type=Path, help="local cmudict.dict (skips the pinned download and hash check)")
    parser.add_argument("--cache-dir", type=Path, default=DEFAULT_CACHE, help=f"default {DEFAULT_CACHE}")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help=f"default {DEFAULT_OUTPUT}")
    parser.add_argument("--flap", choices=("T", "ipa"), default="T",
                        help="symbol for flapped t: T (default, what misaki feeds Kokoro-82M v1.0) or ipa (U+027E)")
    args = parser.parse_args(argv)

    if args.input:
        source = args.input
        print(f"WARNING: {source} is not hash-checked against the pinned CMUdict commit", file=sys.stderr)
    else:
        source = ensure_pinned(args.cache_dir)
    with open(source, encoding="utf-8", newline="") as f:
        entries = build_entries(f, FLAP_IPA if args.flap == "ipa" else FLAP_MISAKI)
    data = render(entries).encode("utf-8")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(data)
    print(f"entries: {len(entries)}")
    print(f"output: {args.output}")
    print(f"sha256: {hashlib.sha256(data).hexdigest()}")


if __name__ == "__main__":
    try:
        main()
    except ValueError as error:
        sys.exit(f"error: {error}")
