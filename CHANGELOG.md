# Changelog

All notable changes to Tsuzuki are documented in this file. Tsuzuki is a fork of
[Tsundoku](https://github.com/tsundoku-otaku/tsundoku), which is based on [Mihon](https://github.com/mihonapp/mihon);
the history inherited from Tsundoku is in [`docs/CHANGELOG-tsundoku.md`](docs/CHANGELOG-tsundoku.md).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0]

The first Tsuzuki release: a reader for light novels, web novels, manga and manhwa in one library.

### Added
- Two app themes: **Dot**, monochrome with one red accent and a dot-matrix display face, and **Colour**, which takes
  its colours from the book you read last. New installs start in Dot; earlier Mihon and Tsundoku themes open in Colour.
- A new Home: tonight's stack of books, resume with one tap, new chapters, today's reading time and your shelf.
- Library with All, Novels and Manga tabs, shelves with book counts, and generated covers for books without art.
- Novel reader: a quick settings sheet plus an All reader settings page, page colours (Book, Paper, Sepia, Sage,
  Night, Black and Custom), and the next chapter loads ahead so turning a chapter doesn't show a blank page.
- Listening: text-to-speech with a sleep timer; on tablets in landscape the Listen panel docks beside the text.
- Tablet layouts: navigation rail, and two-pane Settings, Updates (list plus chapter preview) and Browse.
- Save chapters by chapter-number range; pause and resume novel downloads per book.
- Home-screen widgets for continuing reading and listening.
- Japanese interface with matching Japanese fonts for both themes.
- Settings for accent colour, reduce motion, time format, and keeping covers in colour in Dot.
- Storage breakdown in Data and storage, search result count and time in global search, source counts per extension
  repository, failed extensions listed with the reason, and skipping an app update version.
- The LNReader plugin index (novels) and Keiyoushi (manga) repositories are added on first launch.
- A notification when a new Tsuzuki version is out (checked once a day); tap it to see what's new and update.

### Improved
- Novel chapters download faster: requests wait about 1 second instead of 3, two can go out together, and two books
  download at once. A source's own minimum delay still applies, and slower per-source settings remain available.
- Novels scroll straight from one chapter into the next by default (infinite scroll; it can be turned off).
- Large libraries: Home, Library and Updates do less work when books and badges change.
- Screens keep their selections, sheets and search text when the system closes the app in the background.
- Menus have icons, grouped sections and a separate destructive row; long labels wrap between words.
- Browse: Novels and Manga each have Sources, Extensions and Migrate; swiping stays inside the one you picked.

### Removed
- The System / Light / Dark switch: Dot and Colour are dark designs, so the app always uses its dark look.

### Fixed
- Text-to-speech highlights the paragraph being read, also when a chapter's text is split into several chunks.
- Going back to an already-read chapter opens it at its start, and it scrolls normally again.
- Novel text no longer scrolls under the camera cutout or the gesture bar, and opening the reader menu doesn't move it.
- The rest of the app's settings, novel reader and font manager are now in Japanese.
- On More, tiles no longer slide under the clock when the page scrolls.
- The Next chapter button at the end of a chapter works after a fast scroll to the end, and no longer opens the menu.

### Other
- No telemetry: release builds contain no analytics or crash-reporting libraries.
