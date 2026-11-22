# Changelog draft: 0.2.0

Draft for REL-0.2 (Claude, 2026-11-21). Not final: it lists what v0.2 will contain **if** every card below passes
review. REL-0.2 removes lines for anything that does not merge, then moves this into `CHANGELOG.md`.
Sources: `git log v0.1.1..main` (merged), and the cards in `review` on 2026-11-21.

<!--
Merge status when drafted:
- On main already: anime groundwork (A2–A6.4, R-A1–R-A5, AY-PLAY, AY0 debug fixture), STR2, STR3, V8.1, V8.3, V8.4,
  V8.5, READER-JP-FONT, READER-NEXT-GUARD.
- In review: AY1, AY9 (still being built), AY2(+F), AY3(+F), AY4(+F), AY6(+F), AY-INT(+F), MARK-UNTIL-LAST-READ(+F),
  PERF-START-2-F2, A11Y1.
- Owner decision: the "app chain" block at the end (recap card, TTS polish, dictionary) and its ~29 MB dictionaries.
-->

## [0.2.0]

### Added
- **Anime (beta).** Turn on Settings > Browse > Anime (beta) to watch anime alongside your books. It is off by
  default, so nothing changes until you switch it on, and it needs a 64-bit ARM phone (most phones from the last
  few years).
  - Install anime extension repositories and sources (Aniyomi extensions), browse and search them, and add
    anime to your library on their own shelves. Seasons of the same show are grouped.
  - A full-screen player with picture-in-picture, subtitle and audio track choice, preferred subtitle and audio
    languages, playback speed, and skipping of openings and endings for anime linked to AniList or MyAnimeList.
  - Streams load the way Aniyomi loads them: the source's own headers and order, its servers grouped in the quality
    sheet, and a server that fails doesn't stop the others from loading.
  - Subtitles always render, including subtitles embedded in the video or given inline by a source, and their
    size, boldness, colour, outline, background and position can be changed.
  - Headphone and Bluetooth buttons, the lock-screen media controls and the picture-in-picture buttons control
    playback, and the player remembers where you were if Android closes it in the background.
  - Episode rows and the Watch home show a picture from where you stopped watching.
  - Download episodes to watch offline, track anime on AniList, MyAnimeList or Kitsu, see new episodes and airing
    dates in Updates, keep a watch history, and see watch time in Stats.
  - Each source's own settings page, included in backups.
  - Migrate anime between sources, check for duplicates before adding, and filter releases by subtitle group.
  - Back up your anime with the rest of the library, and import an Aniyomi backup.
  - A Continue Watching home-screen widget.
- When anime is on, it also appears in the places that show books: Library > All (in its own section), History,
  Updates, global search, the download queue, shelves and tracking settings.
- Library selection > More > **Mark until last read** marks the unread chapters before the furthest chapter you
  have read. Undo is offered first, and if some chapters can't be saved the message says so.

### Changed
- More of the app is translated: Home labels, tracking settings and the remaining English-only settings now follow
  the app language in all eight languages.
- The novel download queue shows its rows and statuses in the app language.
- Japanese text in the TextView novel reader uses the reading font's Japanese partner (Noto Serif JP with Literata).
- Library search keeps the All, Novels and Manga pills visible and hides the library totals while searching.
- Migration offers sources of the same kind (novel sources for a novel, manga sources for manga) and keeps your
  chosen order. Migration rows show the cover, title and selection more clearly.
- About: the licences entry shows its full label, and credits are spaced out.
- Screen readers: section labels are headings, pills and labels are read as written (not letter by letter),
  accent colours are named in the app language, the Home cover stack says which book you're on, and full-screen
  backgrounds behind sheets are no longer announced as buttons.

### Fixed
- Manga: the loading indicator stays visible, and Next no longer skips two chapters.
- A library update that starts while the app is closed waits for your sources to load instead of skipping books.

<!-- Keep or delete as one block, per the owner's decision on the app chain (adds about 29 MB). -->
### Added (if the novel-reader fixes ship in 0.2)
- Japanese and English dictionaries are built in, so a long-press lookup works offline from the first use.
  Japanese words written in kanji are looked up in the Japanese dictionary, using the chapter they are in.
- The "Previously" card shows which chapter it recaps and when you read it, in the reader's own font, and the
  resume excerpt ends where you actually stopped.
- Listen (text to speech) settings and the Listen sheet are fully translated, with an info button and long values
  that wrap instead of being cut off.
