# Changelog

All notable changes to Tsuzuki are documented in this file. Tsuzuki is a fork of
[Tsundoku](https://github.com/tsundoku-otaku/tsundoku), which is based on [Mihon](https://github.com/mihonapp/mihon);
the history inherited from Tsundoku is in [`docs/CHANGELOG-tsundoku.md`](docs/CHANGELOG-tsundoku.md).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.1]

### Added
- When you return to a novel after 24 hours or more, the chapter opens with a "Previously" card showing the last few
  paragraphs of your previous chapter. It works offline and can be turned off in reader settings.
- Long-press a word in a novel to look it up with your phone's lookup or a web search. The app can also read optional
  offline English and Japanese dictionary packs (Settings > Reader > Dictionaries), but none are available to
  download yet.
- Upcoming: an optional bell on each book reminds you on its expected release date. Off by default.
- Backups can keep 2, 4, 8 or 12 copies, show when the last backup ran and how big it was, and preview what a backup
  file holds (books, shelves, history) before you restore it.
- Novel reading shows time left in the chapter and in the book, once there is enough reading history to estimate it.
- Home: swipe left or right on the cover stack to move between books.
- Download queue: pause and resume a manga source's downloads, and retry a failed download.
- Stats count listening: each chapter you listen to is counted once, and Stats show listening time and chapters.
- Errors say what failed and offer Retry or Open in WebView, and long waits show the current step.
- Manga page loading keeps its download percentage visible.

### Changed
- New installs come with no source repositories. Add the ones you want in Browse. Repositories that 0.1.0 already
  added stay until you remove them.
- On wide tablet screens, novel text sits in a centred column about 600 dp wide.
- When a source's title states a chapter number, that number is shown in chapter lists and the reader. Your chapter
  order and stored numbers stay the same.
- Tracking: novel accounts are listed first. The update schedule offers all four smart-update restrictions.
- In Dot and Colour settings, long labels keep their full text and the value shortens instead.
- The Listen panel shows the voice's name, marked online or offline, instead of a raw language code.
- Automatic Listen voices follow English or Japanese chapter text; an explicitly chosen voice stays selected.
- English settings use "Greyscale" and "Colour".

### Improved
- Book details open with a shorter cover transition in Dot and Colour.
- The "Library last updated" line on Updates is a quiet caption in Dot and Colour.
- Moving from the end of one chapter to the next uses a smooth transition.
- The sleep timer is in the compact Listen controls.
- LNReader import and CSV export texts are translated.

### Fixed
- Back pressed while book details are opening returns to the screen you opened them from, keeping the selected tab.
- On the end-of-chapter card in the web reader, Next, Mark read and Contents each act once, and the reader bars stay
  hidden.
- The Add repository button is reachable in Dot and Colour.
- A failed or corrupt dictionary update keeps the working pack.
- A scheduled backup that fails no longer removes older backups.
- Restore rejects files that are not Tsuzuki backups. Settings-only backups still restore.
- In paged reading, your place in a chapter stays the same when the recap card appears or is dismissed.
- The highlighted line while listening is no longer hidden under the top bar, and the chapter-end buttons are no
  longer hidden under the bottom bar or the floating Listen controls.
- Text selection is readable in the Dot theme.
- The Listen controls show the chapter that is actually being read aloud.
- Listening no longer repeats the last chapter when a book ends.

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
