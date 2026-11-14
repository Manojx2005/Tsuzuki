# Kokoro-only runtime investigation (NT1-BINARY)

2026-11-14. This produces an **arm64 research candidate**, not an app dependency update or an NT1 review verdict.
No voice model or lexicon is downloaded by these scripts. NT2 licensing, speed and owner listening gates remain.

The three official v2.0.1 Android AARs fail the exact case-insensitive `espeak|piper` native-string rule. The normal
and static-runtime variants contain deprecated warnings, metadata names and API names such as `CreateSpeaker`.
The RKNN variant also contains two native libraries without 16 KB load alignment. Full evidence is generated
under `app/build/nt1-binary-evidence/`; the committed summary is `docs/research/neural-tts-native-audit-20261114.json`.

## Source profile

`kokoro-only-v2.0.1.patch` modifies seven Apache-2.0 upstream files, retaining copyright and modification notices:

- Build only common/version/TTS JNI; omit unrelated speaker/recognizer JNI and the separate C/C++ API libraries.
- Select and validate only Kokoro with a real lexicon; reject other model configurations and legacy directories.
- Omit unused deprecated configuration warnings and unused phonemizer metadata reads.
- Accept text through the lexicon only. Reject pretokenized or external phoneme inputs and remove those branches,
  so the unused phoneme-conversion helper is no longer linked. The app prototype uses the text interface.
- Mark the native version `2.0.1-tsuzuki-kokoro-only` and record the upstream tag's commit.

This is a source build with normal linker dead-code elimination and `llvm-strip --strip-unneeded`. It does not
rename forbidden symbols or edit bytes in a linked library. The raw gate remains unchanged, including Speaker
substring matches. `audit-neural-runtime.py` deliberately reports those matches as failures.

Pinned source: tag `v2.0.1`, commit `af2d8d81dbfa61c4cea4c568da78f3982edca005`.
Source ZIP SHA-256: `71397fbf7c30cf81a56247e4c45bd3748abb590e761fbcfa2989a33ca7644282`.
The original ten-entry dependency manifest is `dependencies-v2.0.1.json`. Nine Sherpa build archives are
selected; the legacy prebuilt ONNX entry remains an allowlisted cache hash but is not fetched or linked.
Root modules override the older Eigen/OpenFst modules inside dependency archives. ONNX Runtime 1.28.2 is
now source-built by `scripts/build-onnx-runtime.ps1` with pinned source/tool ZIP hashes and the dependency
URLs/hashes in its verified upstream `cmake/deps.txt`. Its component hashes are recorded in `ort-install/build.json`.

## Build on this Windows machine

Requires Python 3.12+, Git for Windows (including patch.exe), SDK CMake/Ninja 3.22.1 and NDK 27.1.12297006.
The ONNX recipe obtains checksum-pinned portable CMake 3.31.6 and protoc 21.12 inside `.native/tools`. The recipe checks every existing source file
against the verified archive or the exact recorded profile hashes, then applies/checks the patch. Unknown
local source edits and added files are rejected before extraction. Only the ten pinned dependency archives
may exist alongside upstream source files, and their hashes are checked. Build/cache/output paths stay in
`.native/` under this worktree.

```powershell
./scripts/build-kokoro-runtime.ps1 -SdkRoot 'C:/Users/Manoj/AppData/Local/Android/Sdk'
```

It downloads only pinned source/build/runtime archives. To reuse S4's already-verified Java/API baseline:

```powershell
./scripts/build-kokoro-runtime.ps1 -SdkRoot 'C:/Users/Manoj/AppData/Local/Android/Sdk' `
  -UpstreamAar '../s4/app/libs/sherpa-onnx-2.0.1.aar' -Offline
```

`-Offline` requires the nine Sherpa archives, both source ZIPs, portable tool ZIPs, baseline AAR and the ONNX
CMake dependency cache. The complete cached recipe passed with `-Jobs 2` (the default). A cold source build
uses several GB of workspace. The verified host protoc has a sandbox DLL-relocation conflict on this machine;
it runs normally in the approved terminal context. The recipe explicitly builds RE2 as well as default ONNX
targets before combining the Android static archives.

Output: `.native/output/sherpa-onnx-2.0.1-tsuzuki-kokoro-arm64.aar` and its `.api.json` report. It contains the
unchanged official `classes.jar`, one arm64 JNI library, modification notice, source-build manifest and runtime/dependency licences.
The ten exported TTS/GeneratedAudio JNI methods must exactly match the official arm64 library before packaging.
The script does not copy the artifact into `app/libs/`, edit Gradle or install an app.

The original candidate, before NT1-BINARY-F, is 7,857,879 bytes, SHA-256
`b2cee72b9131cf5190753c966e23e0c0e65ef7a0e9313e434d074d4aa3b5fa47`.
The 20,903,096-byte JNI library is
`9aa11f94d53ad3bac95ae84e6349e3ff0486f63bba39154e10dbde5e6532ff6f`.
Two packaging writes with identical inputs produced the same AAR hash. This does **not** establish identical
native bytes across other build paths/toolchains. The current candidate below removes absolute build paths. Only identical-input packaging repeatability was tested.

## Verification and limits

```powershell
python -m unittest discover -s scripts/tests -p 'test_*neural*.py' -v
python scripts/audit-neural-runtime.py .native/output/sherpa-onnx-2.0.1-tsuzuki-kokoro-arm64.aar --require-build-hygiene `
  --output app/build/nt1-binary-evidence/candidate-aar-audit.json
```

Fourteen auditor regression tests pass, including both ELF classes, truncated inputs, 4 KB/incongruent load
segments, an empty AAR, mixed ABIs, actual runtime markers and the deliberate Speaker substring failure.
Both little- and big-endian UTF-16 literals are scanned at either byte alignment; unrelated wide strings pass.
Seven source-integrity tests pass (repeat preparation, preserved unknown edits, path traversal, extra source
files, changed cached archives, pristine-mode rejection of profile edits and rootless tool archives).
The candidate also passes independent NDK `llvm-strings -a` and `llvm-readelf` checks. Its three PT_LOAD
segments align to 0x4000. DT_NEEDED is only libandroid, liblog, libm, libdl and libc; ONNX/C++ are statically linked.
The negative official-artifact audit exits 1 by design. A zero string count is an artifact fact, not a licence
opinion or evidence of successful inference.

Independent reviewer must inspect the source patch, selected dependencies and notices, rebuild, and integrate
only in a disposable review branch based on S4 (remember its main exclusion `911a2222f`). The candidate is
arm64-only: unsupported ABIs must retain system TTS rather than load it. Check the private worker/no-pack
fallback first. APK packaging still needs its own zipalign/16 KB check. Real synthesis, speed, memory, battery,
proper names and the owner's listening decision remain untested and gated by NT2. No voice pack was obtained here; the AAR contains no models.

## NT1-BINARY-F result (2026-11-14; independent review pending)

The current AAR is **7,853,259 bytes**, SHA-256
`1f70ec2b28f40dfe760a5fe1283e623a1163155c5c530b5a70ae06df1eae4ec1`.
The JNI library is **20,887,080 bytes**,
`ef5ccbfc500fefb8a75100fd0f4fe0528d14669c0dad096781f7b7d13df71b24`.

Both Sherpa and ONNX Runtime are compiled with source/SDK prefix mapping. All 1,009 ONNX C/C++/assembly
commands carry it; Sherpa's selected commands do too. No linked-library bytes or symbols are rewritten.
The 548 upstream CI paths retained by the intermediate prebuilt-ONNX candidate are absent from the final
native string scan. Username counts are zero in ASCII and both UTF-16 byte orders. The strict artifact
check and a broader independent NDK strings scan find no absolute source paths. The path detector recognizes
Windows paths and common CI roots; compiler-command coverage and independent strings inspection remain
part of review rather than relying on that heuristic alone. Runtime Android paths and URLs are not rejected.

The source-build recipe pins these additional archives:

| Input | SHA-256 |
|---|---|
| ONNX Runtime v1.28.2 source | `0a71177b993c8406e0ae50574e2319bd0f53d3e89857cd9acdde0aea22ba27f1` |
| CMake 3.31.6 Windows x64 | `d163cd3ab4959b0a53fa8988f2ddbd2e6c501658201e6a154386bad9dbe4f836` |
| protoc 21.12 Windows x64 | `71852a30cf62975358edfcbbff93086e8857a079c8e4d6904881aa968d65c7f9` |

The combined 81-component ONNX static archive is
`da1db20d213c68641a48e79b8a99e1c6557859b85186e87f5bf140848b463e50`.
The packaged `onnx-build.json` retains every component hash. ONNX licence and third-party notice files match
the pinned source exactly. kissfft's complete upstream copyright plus BSD-3 text is required during packaging.
The preparer rejects edits and extra files before extraction; pristine mode also verifies portable tools and
ONNX sources, without the Sherpa profile exceptions.

The strict candidate audit, independent NDK readelf/strings/export checks, 21 regression tests and two
byte-identical packaging writes pass. Ten TTS/GeneratedAudio JNI methods match the official library;
18 Java exports remain. `classes.jar` is byte-identical to the official AAR. Three PT_LOAD segments are
0x4000 aligned; DT_NEEDED remains Android system libraries only. Evidence: `app/build/nt1-binary-evidence/`,
including `f-final-independent-audit.json`, `f-source-rebuild.log`, `f-final-readelf.txt`,
`f-repeat-package.log`, `f-tests.log` and the preserved intermediate failures.

This is ready for independent artifact review, not a synthesis or licence verdict. No app dependency,
voice pack, lexicon, Gradle configuration or S4 integration was added. The owner listening gate remains.

## US-English lexicon from CMUdict (NT-LEX1)

The upstream Kokoro lexicon's licence is unsettled (`docs/research/neural-tts-clean-lexicon.md`), so
`build-lexicon-cmudict.py` converts CMUdict (BSD-2-style licence) instead. Conversion only: no function-word
reduction, suffix rules, names, numbers or British English yet, and nothing from misaki's data is used.

```sh
python scripts/neural-tts/build-lexicon-cmudict.py            # pinned download, then convert
python scripts/neural-tts/build-lexicon-cmudict.py --input path/to/cmudict.dict --output out.txt
python -m unittest discover -s scripts/tests
```

- Source: `cmudict.dict` at `cmusphinx/cmudict` commit `74790861f652b15e4ac49015a90074ad62a27690`, SHA-256
  `81917843c7f44ce2b094ac63873c2c7a4cf802040792c455ba3ca406891c3d22`. It is cached as
  `.native/archives/74790861f652-cmudict.dict` (ignored by git); a wrong hash is refused. If Python has no CA
  certificates (MSYS Python), download the file another way into that path and rerun. `--input` skips the check.
- Output (default `.native/lexicon/lexicon-us-en-cmudict.txt`, ignored by git): `word p h o n e s`, one symbol per
  space-separated field, sorted, LF, UTF-8, one pronunciation per word (the lowest `WORD(n)` variant, because the
  runtime keeps one per key). It prints the entry count and SHA-256 (126,052 entries, hash in the card Result).
- The flapped t is written `T`, as misaki feeds Kokoro-82M v1.0; `--flap ipa` writes U+027E instead. Which one
  sounds right is for the owner listening test. Licence texts to ship next to the lexicon:
  `licenses/cmudict-LICENSE.txt` and `licenses/cmudict-NOTICE.txt`.
