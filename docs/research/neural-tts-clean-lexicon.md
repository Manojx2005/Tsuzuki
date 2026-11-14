# Neural TTS: lexicon licence and a clean replacement (2026-11-14)

Research by Claude subagents for NT2 step 1. Nothing here was run on a device. Claims marked *inference* are not
from a primary source.

## 1. The upstream lexicon licence is not settled

- `lexicon-us-en.txt` / `lexicon-gb-en.txt` in sherpa-onnx's Kokoro archives are a mechanical re-encoding of
  misaki's `{us,gb}_{gold,silver}.json`: `scripts/kokoro/v1.0/generate_lexicon_en.py` does
  `english = {**silver, **gold}`, lowercases, and splits phonemes; `run.sh` fetches the four JSON files from
  `hexgrad/misaki` `main` (no pinned commit).
- misaki maintainer, issue hexgrad/misaki#51: **silver** = output of `espeak-ng==1.52` (GPL-3.0-or-later) run over
  dwyl/english-words, then filtered against gold; **gold** = "some local datasets of IPA labels", not named.
- The only licence is the repo-level Apache-2.0 (and `license: apache-2.0` on the HF dataset card). No
  data-specific licence or provenance file exists. hexgrad/misaki#107 and #113 ask whether Apache-2.0 covers the
  data and whether espeak-derived entries carry GPL terms; neither has a maintainer answer.
- `lexicon-zh.txt` (pypinyin + misaki zh) was not checked. Kokoro-82M weights are explicitly Apache-2.0.
- **Verdict: NOT SETTLED.** By the Plan 0.1.1 rule the neural voice does not ship in 0.1.1 unless the owner gets
  this cleared (maintainer answer on #113, or legal advice on espeak output), or we stop using these files.

## 2. A lexicon with a clean licence is feasible (US English)

- **CMUdict** (github.com/cmusphinx/cmudict): BSD-2-Clause-style licence, "Copyright (C) 1993-2015 Carnegie
  Mellon University"; redistribution with or without modification permitted if the notice, conditions and
  disclaimer are kept ("The contents of this file are deemed to be source code"). README: use "for any research
  or commercial purpose is completely unrestricted", acknowledgement requested. 135,166 entries (126,052
  headwords), 39 ARPAbet phones with stress digits, American English.
- Other sources: Moby Pronunciator II (public-domain grant, usable as a supplement); ipa-dict `en_US` (CMUdict in
  IPA, redundant); ipa-dict `en_UK` (GPL-3.0-derived, not usable); Britfone (MIT label but sources include
  commercial dictionaries and Wiktionary, unclear); WikiPron (CC BY-SA, avoid). **No clean British source found.**
- **The app can bypass the lexicon file.** sherpa-onnx 2.0.1's Kotlin API has
  `GenerationConfig.tokens: Array<Array<String>>?` (pre-phonemised tokens, takes precedence over text), and
  `--kokoro-lexicon` is optional. So the app can run its own G2P and pass phoneme tokens; unknown words are then
  never dropped. Read from source at tag v2.0.1, not run. In that path the app must chunk sentences itself.
  NT1-BINARY's patch makes the lexicon *required* and removes external token input, so the patch would need to
  change for this route.
- ARPAbet → Kokoro mapping was tested on all 135,166 entries: every output symbol is in the model vocabulary.
  Key points: G → `ɡ` (U+0261, not ASCII g); AY/EY/OW/AW/OY → `I A O W Y`; ER1/2 → `ɜɹ`, ER0 → `əɹ`; AH0 → `ə`,
  AH1/2 → `ʌ`; flap T (`ɾ`) after a vowel before an unstressed vowel, never flap D; stress marks `ˈ ˌ` go
  immediately before the stressed vowel. Agreement with the upstream gold dictionary on 33,322 shared words:
  59.9 % exact, a further 7.1 % differing only in stress marks (diagnostic only; nothing copied).
- Coverage on four public-domain novels: 98.0–99.2 % of tokens are in CMUdict as written; 0.6–1.2 % still missing
  after simple inflection rules, almost all names. Light novels (invented and romanised Japanese names) will be
  worse.
- What a build needs beyond the conversion: a function-word reduction table (citation-form stress on "of", "to",
  "the" gave a 48 % word-error baseline elsewhere), suffix phonology for -s/-ed/-ing/'s, an out-of-vocabulary
  chain (compound split → letter-to-sound model trained on CMUdict → spelling; a romaji table for Japanese
  names), and a number/date/abbreviation normaliser written from scratch (sherpa-onnx has none for English).
- Notices that would ship: CMU's full licence text next to the lexicon and in About, with a statement that the
  data was converted; Moby's public-domain statement if used; Kokoro-82M Apache-2.0; no `espeak-ng-data`.
- **Estimate (the subagent's, rough): 3–4 weeks for a respectable US-only first version.** Main ways it could
  sound worse than upstream: stressed function words, reduced-vowel choices, garbled names from the fallback
  model, no heteronym disambiguation (same as upstream), unexpanded numbers, GB voices reading US phonemes.

## 3. NT1-BINARY artifact review (independent, 2026-11-14)

Pass on every measurable claim (hashes, 0 `espeak`/`piper` bytes in the `.so` in ASCII and UTF-16, 3 PT_LOAD at
0x4000, system-only DT_NEEDED, 18 `Java_*` exports, 13 Python tests). The patch removes code paths, not GPL
code: official 2.0.1 contains no espeak-ng or piper-phonemize source. Open before shipping: ONNX Runtime is a
third-party prebuilt pinned by hash only; the `.so` embeds 25 absolute build paths including the Windows user
name (add `-ffile-prefix-map`); kissfft's full BSD-3 text is missing; `classes.jar` (unmodified official) has
"eSpeak" inside `OfflineSpeaker*` class names and is not scanned; arm64 only; never loaded a model.

## 4. NT-LEX1 independent conversion review (Codex, 2026-11-14)

Reviewed the full six-file diff in `243121aea`. The default pinned CMUdict input rebuilds to
126,052 entries, SHA-256 `3f9d023400a8b0c4730c095729363c9374c8e8efbf65ad62e2df67f0d18efb50`,
identically on two runs. All 50 Python tests pass, including the full-dictionary check (no skips).
The bundled licence is byte-identical to the pinned upstream licence; the 114-symbol fixture exactly
matches the pinned Kokoro config vocabulary. Thirty common words were checked against their CMU input.
No misaki gold/silver data or upstream lexicon text is in the commit. No app integration or synthesis tested.

Primary checks: [CMU phone inventory](https://github.com/cmusphinx/cmudict/blob/74790861f652b15e4ac49015a90074ad62a27690/cmudict.phones),
[stress symbols](https://github.com/cmusphinx/cmudict/blob/74790861f652b15e4ac49015a90074ad62a27690/cmudict.symbols),
[licence](https://github.com/cmusphinx/cmudict/blob/74790861f652b15e4ac49015a90074ad62a27690/LICENSE),
and sherpa-onnx v2.0.1 `generate_lexicon_en.py` / `ParseLexiconFile` for the per-symbol format.
The default flap differs from section 2: the pinned
[model normalization code](https://github.com/hexgrad/misaki/blob/fba1236595f2d2bf21d414ba6e57d25256afada3/misaki/en.py#L733)
rewrites IPA U+027E to `T` before Kokoro v1.0 input. Accept `T` for this conversion, retaining `--flap ipa`
for the later listening comparison. Neither spelling has been listened to here.

`--input` is an explicit unverified local/test input mode with a warning; release data must use the default
hash-enforced path. Python HTTPS remains unverified on this machine because its CA bundle is missing;
independent downloads used curl with normal TLS verification, then the converter verified the pin.
Function-word reduction, suffixes, OOV/names, numbers/dates, British English and owner listening remain out of
scope. Evidence: `.worktrees/review-nt-lex1/app/build/review-evidence/nt-lex1/`.
