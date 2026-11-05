# Reddit post: looking for contributors (draft)

Suggested subs: r/androiddev (Saturday "Show-off" / self-promo rules apply), r/opensource, r/LightNovels and
r/manga (check each sub's self-promotion rules first; some only allow it in weekly threads). Attach 3–4 real
screenshots: Home in Colour and Dot, book details with the Save sheet, the reader with the contents drawer.

Before posting: make the GitHub repo public, add a CONTRIBUTING.md and label a few issues `good first issue`.

---

**Title:** Looking for Android devs to help build Tsuzuki, an open-source reader for light novels, manga & (soon) anime [Kotlin / Compose]

Hi all! I'm building **Tsuzuki**, a free, open-source (Apache-2.0) Android reader that puts **light novels, web
novels, manga and manhwa in one library**, with anime planned next. It's a fork of Tsundoku (itself built on Mihon),
so it already has a solid base: Mihon/Tachiyomi-style extensions for manga, 300+ LNReader novel sources running
natively, a novel reader with TTS and translation, EPUB import and backups.

What Tsuzuki adds is a full redesign with two themes:

- **Colour**: the whole app takes on the colours of the book you're reading.
- **Dot**: a monochrome, dot-matrix look with one red accent.

Bento-style Home, a "continue reading" hero, redesigned library, details and reader screens, and every screen is
designed first on a design board (they're in the repo as PNG + HTML, so you never have to guess a layout).

**Where help would make the biggest difference:**

- **Kotlin / Jetpack Compose UI**: building the remaining screens from the boards (reader settings, motion and
  transitions, tablet layouts). Specs are written out with sizes and colours.
- **Downloads & sync**: splitting the downloader so manga, novel and anime engines can each follow their upstream,
  and porting Mihon's newer download speed-ups.
- **Anime (Aniyomi integration)**: bringing in Aniyomi's anime sources and player.
- **Upstream maintenance**: reviewing and cherry-picking Mihon/Tsundoku fixes (there's a script and a weekly report
  for this).
- **Translations**: the app strings exist in English and Japanese so far.
- **Testing on real devices**, especially tablets and non-Samsung phones.

**What it's like to contribute:**

- Kotlin + Compose, Gradle, the familiar Mihon architecture.
- `AGENTS.md` keeps an ordered task queue, so there's always a clear next thing to pick up.
- New UI lives in its own `tsuzuki` packages, so changes stay small and upstream merges stay clean.
- Small PRs welcome; you don't need to know the whole codebase.

Repo: https://github.com/Manojx2005/Tsuzuki

If you've ever wanted one app for your novels *and* your manga that actually looks good, I'd love your help. Happy
to answer questions here or in the issues. Thanks for reading!
