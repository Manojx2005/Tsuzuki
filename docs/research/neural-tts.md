# Neural TTS (sherpa-onnx + Kokoro): GPL / licence research

Date: 2026-11-06. Scope: WORKPLAN Phase 5 "Neural TTS voices" gate. Research only, no app code changed.

## Verdict: GO-WITH-CONDITIONS

Since **sherpa-onnx v2.0.0 (2026-10-23)** the library no longer contains or links espeak-ng or piper-phonemize. The stock v2.0.1 Android AAR is GPL-free in its own native code, and Kokoro v1.0+ works with lexicon files only. The risk is not the library any more. It is (a) pinning to v2.0.0 or newer (every older AAR statically links GPL espeak-ng), (b) not shipping the `espeak-ng-data` folder that the upstream model tarballs still include, and (c) quality: words missing from the lexicon are dropped, which matters for light-novel names.

Conditions:
1. Use only `sherpa-onnx-2.0.1.aar` (or newer, re-scanned). Never any AAR at v1.13.8 or older.
2. Build our own voice pack zip: Kokoro model + `voices.bin` + `tokens.txt` + `lexicon-*.txt` + licence files. Exclude `espeak-ng-data/`.
3. Do not use models that need a phonemiser (Piper/Coqui/Icefall VITS, Kokoro v0.19, Kitten): they no longer work in v2 builds.
4. Add licence/notice texts (sherpa-onnx Apache-2.0, ONNX Runtime MIT, model licences) to About.
5. Keep Android system TTS as default; neural voices are optional. Fall back to it (or accept dropped words) when text has many out-of-lexicon words.
6. Listen to real novel text before committing to UI work (OOV drop rate, see section 5).

## 1. Is espeak-ng linked into the Android JNI library?

**Before v2.0.0: yes, statically.**
- At tag v1.13.8, `sherpa-onnx/csrc/CMakeLists.txt` compiles `piper-phonemize-lexicon.cc` (line 316) and links `piper_phonemize` into the core (line 481): https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/v1.13.8/sherpa-onnx/csrc/CMakeLists.txt
- `cmake/piper-phonemize.cmake` and `cmake/espeak-ng-for-piper.cmake` at v1.13.8 fetch `csukuangfj/piper-phonemize` and `csukuangfj/espeak-ng` and build them with `BUILD_SHARED_LIBS OFF`, so they are baked into the sherpa-onnx libraries: https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/v1.13.8/cmake/piper-phonemize.cmake and the sibling `espeak-ng-for-piper.cmake`
- Licences (GitHub licence API): espeak-ng/espeak-ng GPL-3.0; csukuangfj/piper-phonemize GPL-3.0. https://api.github.com/repos/espeak-ng/espeak-ng/license , https://api.github.com/repos/csukuangfj/piper-phonemize/license

**From v2.0.0: no.**
- PR #4055 "Remove espeak-ng and piper-phonemize from sherpa-onnx", merged 2026-10-20, merge commit `0b3647632f084758e573a588ee5c9425dadc5908`. Its text: "espeak-ng and piper-phonemize are no longer built into sherpa-onnx ... sherpa-onnx is GPL-free after this PR"; `data_dir` on TTS configs is deprecated and ignored. https://github.com/k2-fsa/sherpa-onnx/pull/4055
- Release v2.0.0 (2026-10-23) lists it: https://github.com/k2-fsa/sherpa-onnx/releases/tag/v2.0.0 . Latest is v2.0.1 (2026-10-26); master head when checked was `079a6f05d05a18bc6e993704a557c2f62c7a17a5`, and the files below were read at that commit.
- Top-level `CMakeLists.txt` includes only kaldi-native-fbank, utf8proc, kaldi-decoder, onnxruntime, simple-sentencepiece (plus json, hclust-cpp, openfst optional); `cmake/` has no espeak or piper file; `sherpa-onnx/csrc/CMakeLists.txt` links `fstfar fst`, no piper_phonemize. https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/079a6f05d05a18bc6e993704a557c2f62c7a17a5/CMakeLists.txt ; tree listing: https://api.github.com/repos/k2-fsa/sherpa-onnx/git/trees/079a6f05d05a18bc6e993704a557c2f62c7a17a5?recursive=1
- `sherpa-onnx/csrc/offline-tts-frontend.h` at that commit: "Tokenization utilities (no espeak-ng dependency)". `offline-tts-kokoro-model-config.cc`: "--kokoro-data-dir ... Deprecated. Ignored since v2.0.0: espeak-ng is no longer used."
- Earlier attempt PR #3931 (merged 2026-09-07, "for redistribution-safe builds") describes the same design with `nm`/`strings` scans and a device test on Android 16. https://github.com/k2-fsa/sherpa-onnx/pull/3931

**I checked the shipped binary myself.** Downloaded `sherpa-onnx-2.0.1.aar` (49,441,341 bytes, sha256 `a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7`) from https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-2.0.1.aar . In `jni/arm64-v8a/libsherpa-onnx-jni.so` there are no `espeak_*` API symbols or `phontab` data (searched `espeak_Initialize`, `espeak_TextToPhonemes`, `espeak_SetVoiceByName`). The only "espeak"/"piper_phonemize" strings are deprecation warnings ("espeak-ng is no longer used", "external phonemizer (e.g. piper_phonemize)") and config field names (`use_espeak`, `has_espeak`). The AAR contains `jni/{arm64-v8a,armeabi-v7a,x86,x86_64}/` with libonnxruntime, libsherpa-onnx-c-api, -cxx-api, -jni, and no licence files. arm64-v8a ELF `PT_LOAD` alignment is 0x4000 (16 KB) in all four libs, matching the project's 16 KB rule. (Other ABIs not checked.)

**CMake switches.** There is no espeak switch any more because there is no espeak. `SHERPA_ONNX_ENABLE_TTS` (default ON, root CMakeLists.txt line 71) only turns the TTS code on or off. With TTS on and no espeak, these work: Kokoro >= v1.0 (lexicon), Matcha (lexicon/dict+fst), VITS with lexicon (MeloTTS, zh), and the newer Supertonic, Pocket and ZipVoice backends (their frontends not individually checked). These stop working: models that need phonemisation (Piper/Coqui/Icefall/Inflect VITS, Kokoro v0.19, Kitten), unless the app phonemises externally and passes `phoneme_codepoints`; the only GPL-free route there would be our own G2P, not planned. Sources: PR #3931 support matrix and PR #4055 API notes. A custom source build is not needed for the Apache-2.0 route (upstream added an F-Droid reproducible-build doc in v2.0.0, PR #3963, if ever wanted).

## 2. Model families without espeak (v2.0.x)

Sizes are release-asset `.tar.bz2` sizes from https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models (asset list via the GitHub API).

| Model (asset) | English? | Text frontend | Size | Weights licence |
|---|---|---|---|---|
| `kokoro-int8-multi-lang-v1_1` | yes (US + GB lexicons) and zh | lexicon (`lexicon-us-en.txt`, `lexicon-gb-en.txt`, `lexicon-zh.txt`) | 147.1 MB (model.int8.onnx 114 MB, voices.bin 54 MB, lexicons about 14 MB unpacked) | Apache-2.0 (tarball ships the Apache text, 11,358 B) |
| `kokoro-int8-multi-lang-v1_0`, `kokoro-multi-lang-v1_0`, `kokoro-multi-lang-v1_1` (fp32) | yes | lexicon | 132.4 / 349.9 / 364.9 MB | Apache-2.0 |
| `vits-melo-tts-en` (MeloTTS English) | yes | `lexicon.txt` + tokens | 162.8 MB (model.onnx 170 MB unpacked) | MIT (tarball LICENSE 1,053 B; HF `myshell-ai/MeloTTS-English` = mit) |
| `vits-melo-tts-zh_en` | zh + en | lexicon | 167.0 MB | MIT likely, UNVERIFIED for this tarball |
| `matcha-icefall-zh-en` | zh + en | dict/lexicon + fst | 79.0 MB | UNVERIFIED |
| `vits-zh-aishell3`, `vits-icefall-zh-aishell3`, `matcha-icefall-zh-baker` | no (Chinese) | lexicon | 31-147 MB | UNVERIFIED (dataset terms) |
| Supertonic `sherpa-onnx-supertonic-3-tts-int8-2026-05-11`, Pocket `sherpa-onnx-pocket-tts-int8-2026-01-26`, ZipVoice `sherpa-onnx-zipvoice-distill-int8-zh-en-emilia` | yes | own frontends | 128.8 / 98.3 / 109.2 MB | UNVERIFIED |

Do NOT use (they need a phonemiser and fail in v2 builds): `kokoro-en-v0_19` (319.6 MB, the owner's example) and `kokoro-int8-en-v0_19` (103.2 MB), all `vits-piper-*`, `vits-coqui-*`, `vits-mimic3-*`, `vits-ljs`, `vits-icefall-en_US-*`, `matcha-icefall-en_US-ljspeech`, `kitten-*`. Basis: PR #3931 matrix ("Piper / Coqui / Icefall / Inflect VITS, Kokoro v0.19, Kitten ... fail loudly") and v2.0.1 warnings "--vits-data-dir ... --kitten-data-dir ... deprecated and ignored". The exact v2.0.1 runtime behaviour for v0.19 is UNVERIFIED (not executed).

The upstream Kokoro tarball still **contains `espeak-ng-data/`**. Listing of `kokoro-int8-multi-lang-v1_1.tar.bz2` (https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_1.tar.bz2): top level has `espeak-ng-data/` (many `*_dict` files), `model.int8.onnx`, `voices.bin`, `tokens.txt`, `lexicon-us-en.txt`, `lexicon-gb-en.txt`, `lexicon-zh.txt`, `*.fst`, `LICENSE`. v2 ignores the folder. espeak-ng data is GPL-3.0 material, so strip it from our own pack. `vits-melo-tts-en` has no such folder (model.onnx, tokens.txt, lexicon.txt, LICENSE, README.md).

## 3. Licences

- sherpa-onnx code: Apache-2.0 (repo LICENSE at the v2.0.1-era commit). https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/079a6f05d05a18bc6e993704a557c2f62c7a17a5/LICENSE
- Kokoro-82M weights: Apache-2.0 (HF model card says "Apache-licensed weights"; `Kokoro-82M-v1.1-zh` also apache-2.0). https://huggingface.co/hexgrad/Kokoro-82M , https://huggingface.co/api/models/hexgrad/Kokoro-82M . The card lists some CC BY training audio (Koniwa, SIWIS); attribution as a courtesy is advisable (legal effect UNVERIFIED).
- misaki (Kokoro G2P): Apache-2.0; its optional espeak fallback is separate and not used by sherpa-onnx v2. https://github.com/hexgrad/misaki
- ONNX Runtime: MIT. https://api.github.com/repos/microsoft/onnxruntime/license
- Other build deps: kaldi-native-fbank, kaldi-decoder, simple-sentencepiece are Apache-2.0 (GitHub licence API). utf8proc, the openfst fork and hclust-cpp are reported NOASSERTION by GitHub; UNVERIFIED here.
- Origin licence of `lexicon-us-en.txt` / `lexicon-gb-en.txt` (dictionary data): UNVERIFIED. Check before redistributing in our own pack.
- espeak-ng and piper-phonemize: GPL-3.0 (section 1).

## 4. Recommended path

1. Depend on the prebuilt `sherpa-onnx-2.0.1.aar` (or later). No custom native build is needed because upstream already removed espeak. Pin by sha256 and rerun the symbol/string scan on every bump (search the `.so` for `espeak_Initialize` and `phontab`).
2. Ship `kokoro-int8-multi-lang-v1_1` as the default English voice pack (147 MB download, not in the APK), re-zipped on our own release without `espeak-ng-data/`, with `--kokoro-lexicon lexicon-us-en.txt` (or `lexicon-gb-en.txt`). Optional second pack: `vits-melo-tts-en` (MIT).
3. Download into app storage as planned in WORKPLAN; repeat the 16 KB check on the native libs after each bump (arm64 passes at 2.0.1).
4. About screen: Apache-2.0 notice for sherpa-onnx, MIT for ONNX Runtime and MeloTTS, Apache-2.0 for Kokoro.
5. Keep system TTS as the default and the fallback.

If the OOV behaviour below proves unacceptable on real novel text, the answer for neural voices as a default becomes NO-GO and system TTS stays. A custom GPL-free English G2P would be new work, not evaluated here.

## 5. Risks / unverified

- **Out-of-lexicon words are dropped**, with a once-per-process warning (PR #3931 "Message B"; `offline-tts-kokoro-impl.h` at the v2.0.1-era commit logs "OOV word skipped"). PR #4075 (https://github.com/k2-fsa/sherpa-onnx/pull/4075) fixed punctuation/edge handling so quoted or dashed words are not lost, but truly unknown words are still skipped. Light-novel character names, invented terms and romanised Japanese will often be OOV and would be silent. Not measured here. Mitigations: append our own entries to the lexicon file, or fall back to system TTS per chunk.
- v2.0.0 changed Kokoro/ZipVoice/Matcha config semantics (`--kokoro-lang` ignored, lexicon required). The owner's draft was Flutter/Dart and does not apply (already noted in WORKPLAN).
- Not done: synthesis run, real-time factor on the S22 / Tab S11, alignment audit of non-arm64 ABIs.
- Upstream could reintroduce espeak or other GPL dependencies in later releases; re-verify on each upgrade.

## 6. Debug listening test (S4)

The Debug APK uses the pinned Sherpa ONNX 2.0.1 AAR. Its licence texts and ONNX Runtime notices are in
`docs/licenses/neural-tts/` and in Debug APK assets. The renderer keeps Android TTS as its default.
Kokoro runs in a private process so a native model load failure can return playback to Android TTS.

Use an existing owner-supplied `kokoro-int8-multi-lang-v1_1.tar.bz2` to prepare the listening pack.

```sh
python scripts/package-kokoro-test-pack.py kokoro-int8-multi-lang-v1_1.tar.bz2 kokoro-listening-test.zip
```

The zip includes the model, voices, tokens, US English lexicon and the archive's licence.
Extract its top-level directory into the Debug app's private `files/tts/` directory. The resulting
model path is `files/tts/kokoro-int8-multi-lang-v1_1/model.int8.onnx`. The review lane can side-load
this on the S22's Debug app. In Settings → Advanced → Debug info, enable **Kokoro listening test**,
then use Listen in an English novel. Reopen the reader after changing this switch.

The test uses speaker index 0 and the existing speech-rate preference. Pitch and voice selection
apply to Android TTS. Try natural prose and proper names, pause/resume, paragraph seek, chapter
advance, and missing-pack fallback. Lexicon redistribution remains subject to the provenance
check above.
