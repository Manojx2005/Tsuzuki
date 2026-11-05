# Anime player native libraries (arm64-v8a)

The `.so` files in `anime-player/src/main/jniLibs/arm64-v8a/` were built by the GitHub Actions workflow
`.github/workflows/anime-native.yml` (script `scripts/anime-native/build.sh`), run **41645408336** on
2026-11-03, and archived before the artifact expired. `anime-player/native-checksums.txt` records their SHA-256;
the module's `verifyNativeChecksums` task fails the build if any file changes.

## Licences

- **FFmpeg** (libavcodec, libavformat, libavfilter, libavutil, libavdevice, libswresample, libswscale): built as
  **LGPL-3.0-or-later** — `--enable-gpl`, `--enable-version3`-only GPL parts, `--enable-nonfree` and postproc are
  disabled (`ffmpeg-license.txt` = "LGPL version 3 or later"). Licence texts in `ffmpeg/`.
- **mpv** (libmpv): built with `-Dgpl=false`, i.e. LGPL-2.1-or-later. Texts in `mpv/`.
- **libplayer** JNI wrapper: aniyomi-mpv-lib, **MIT** (`aniyomi-mpv-wrapper-MIT.txt`).
- Dependencies: dav1d (BSD-2), freetype2 (FTL), fribidi (LGPL-2.1), harfbuzz (MIT), libass (ISC), libplacebo
  (LGPL-2.1), libxml2 (MIT), mbedTLS (dual Apache-2.0 OR GPL-2.0-or-later; used under Apache-2.0), libc++ (Apache-2.0 with LLVM exception). Texts in the matching folders.

All LGPL components are shipped as separate, dynamically linked shared libraries, so users can replace them.

## Corresponding source

Exact upstream revisions: `source-revisions.txt` and `dependency-versions.sh`. Build flags and patches:
`scripts/anime-native/build.sh` and the workflow above. Anyone can rebuild the libraries from these.

## 16 KB page alignment

`audit.json` lists every library's ELF `PT_LOAD` alignments; all are 16 KB (libmpv's first segment is 64 KB).
