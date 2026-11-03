# Tsuzuki

A reader for light novels, web novels, manga and manhwa on Android, written in Kotlin with Jetpack Compose. *Tsuzuki* (続き) means "continuation": what happens next.

> **Status:** early development (v0.1.0). Not yet released.

Tsuzuki is a fork of **[Tsundoku](https://github.com/tsundoku-otaku/tsundoku)**, which is itself a fork of
**[Mihon](https://github.com/mihonapp/mihon)**. Tsundoku already provides the novel reader, text-to-speech,
translation, EPUB/TXT import, LNReader plugin support and LNReader backup import. That saves months of work.
This fork adds:

- **Two themes, one layout**: *Colour* (each book's cover colours, Instrument Serif + Manrope) and
  *Dot* (monochrome dot-matrix with one red accent, Doto + Space Mono). Pick one in
  Settings › Appearance.
- A bento-style home, source health and migration, merge sources, a notebook with a glossary,
  reading goals, and tablet, foldable and desktop layouts (in progress, see the plan).
- Native Kotlin ports of the LNReader sources over time, with the JS plugin runtime kept as a fallback.

See [`docs/PLAN.md`](docs/PLAN.md) for the full product and engineering plan.

## Building

Requires JDK 17+ and the Android SDK (compile SDK set in `gradle/mihon.versions.toml`).

```sh
./gradlew :app:assembleDebug
```

## Credits and licences

- Code: Apache License 2.0 (see [LICENSE](LICENSE)). Copyright the Mihon and Tsundoku contributors,
  and the Tsuzuki contributors. The original Tsundoku README is kept at
  [`docs/TSUNDOKU_README.md`](docs/TSUNDOKU_README.md).
- LNReader plugin logic: MIT (LNReader contributors).
- Bundled fonts (Doto, Space Mono, Instrument Serif, Manrope, and for Japanese: DotGothic16, M PLUS 1 Code,
  Noto Serif JP, Zen Kaku Gothic New): SIL Open Font License 1.1 ([`docs/licenses/`](docs/licenses/)).

Tsuzuki is not affiliated with Mihon, Tsundoku or LNReader.
