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
The ten dependency archives and SHA-256 values are in `dependencies-v2.0.1.json`; they match the selected
upstream CMake modules. Root modules override the older Eigen/OpenFst modules inside dependency archives.

## Build on this Windows machine

Requires Python 3.12+, Git, SDK CMake 3.22.1 and NDK 27.1.12297006. The recipe checks every existing source file
against the verified archive or the exact recorded profile hashes, then applies/checks the patch. Unknown
local source edits are rejected. Build/cache/output paths stay in `.native/` under this worktree.

```powershell
./scripts/build-kokoro-runtime.ps1 -SdkRoot 'C:/Users/Manoj/AppData/Local/Android/Sdk'
```

It downloads only pinned source/build/runtime archives. To reuse S4's already-verified Java/API baseline:

```powershell
./scripts/build-kokoro-runtime.ps1 -SdkRoot 'C:/Users/Manoj/AppData/Local/Android/Sdk' `
  -UpstreamAar '../s4/app/libs/sherpa-onnx-2.0.1.aar' -Offline
```

`-Offline` requires all ten dependencies, the source ZIP and the baseline AAR already cached. That exact cached
recipe completed successfully in this investigation. A cold build may use `-Jobs 2`; default is 4.

Output: `.native/output/sherpa-onnx-2.0.1-tsuzuki-kokoro-arm64.aar` and its `.api.json` report. It contains the
unchanged official `classes.jar`, one arm64 JNI library, modification notice and runtime/dependency licences.
The ten exported TTS/GeneratedAudio JNI methods must exactly match the official arm64 library before packaging.
The script does not copy the artifact into `app/libs/`, edit Gradle or install an app.

The observed candidate is 7,857,879 bytes, SHA-256
`b2cee72b9131cf5190753c966e23e0c0e65ef7a0e9313e434d074d4aa3b5fa47`.
The 20,903,096-byte JNI library is
`9aa11f94d53ad3bac95ae84e6349e3ff0486f63bba39154e10dbde5e6532ff6f`.
Two packaging writes with identical inputs produced the same AAR hash. This does **not** establish identical
native bytes across other build paths/toolchains; source paths can appear in native diagnostic strings.

## Verification and limits

```powershell
python -m unittest discover -s scripts/tests -p 'test_*neural*.py' -v
python scripts/audit-neural-runtime.py .native/output/sherpa-onnx-2.0.1-tsuzuki-kokoro-arm64.aar `
  --output app/build/nt1-binary-evidence/candidate-aar-audit.json
```

Ten auditor regression tests pass, including both ELF classes, truncated inputs, 4 KB/incongruent load
segments, an empty AAR, mixed ABIs, actual runtime markers and the deliberate Speaker substring failure.
Three source-integrity tests also pass (repeat preparation, preserved unknown edits and path traversal).
The candidate also passes independent NDK `llvm-strings -a` and `llvm-readelf` checks. Its three PT_LOAD
segments align to 0x4000. DT_NEEDED is only libandroid, liblog, libm, libdl and libc; ONNX/C++ are statically linked.
The negative official-artifact audit exits 1 by design. A zero string count is an artifact fact, not a licence
opinion or evidence of successful inference.

Independent reviewer must inspect the source patch, selected dependencies and notices, rebuild, and integrate
only in a disposable review branch based on S4 (remember its main exclusion `911a2222f`). The candidate is
arm64-only: unsupported ABIs must retain system TTS rather than load it. Check the private worker/no-pack
fallback first. APK packaging still needs its own zipalign/16 KB check. Real synthesis, speed, memory, battery,
proper names and the owner's listening decision remain untested and gated by NT2. No model was obtained here.
