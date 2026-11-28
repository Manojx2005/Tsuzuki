# Changelog

All notable changes to Tsuzuki are documented in this file. Tsuzuki is a fork of
[Tsundoku](https://github.com/tsundoku-otaku/tsundoku), which is based on [Mihon](https://github.com/mihonapp/mihon);
the history inherited from Tsundoku is in [`docs/CHANGELOG-tsundoku.md`](docs/CHANGELOG-tsundoku.md).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.4] - 2026-11-28

### Added
- Anime: Play with lets episodes open in another video app (MX Player, VLC, mpv and others) or ask each time;
  where you stop there is saved as your progress.
- Anime: Simkl tracking. Sign in under Settings › Tracking › Anime trackers; status, watched episodes and your
  rating are sent to Simkl (changes made on Simkl's site are not read back).
- Anime: real next-episode airing times from AniList for anime tracked there, on the details page and in Airing
  today.
- Anime: Screenshot in the player's settings menu saves the current frame to Pictures/Tsuzuki, with Share.
- Anime: Local anime. Put each show in its own folder under `localanime/` in Tsuzuki's storage folder (mp4, mkv or
  webm, with optional cover.jpg, details.json and matching .srt/.ass subtitles); it appears in Browse › Anime under
  On this device.
- Anime: downloaded episodes keep the source's extra subtitles, audio tracks and player options.
- Restoring an Aniyomi backup now says so in the restore preview.
- Predictive Back: swiping back previews the screen behind (details, settings pages, browse); screens now slide
  in with a short shared-axis motion.

### Changed
- Smoother overall: Tsuzuki's own screens are included in the startup profile, details pages draw their first
  frame faster, and going back to the Library takes about 30 ms instead of 40.
- Opening and closing the reader and the video player, and going back, now take 300 ms instead of 450 ms.
- Library: switching to Anime slides like switching between Novels and Manga, instead of fading the whole screen.
- Anime details: episodes appear with the page instead of popping in, and the page no longer jumps when the
  continue button arrives; the anime library no longer flashes "empty" while it loads.
- Empty lists show their face upright in the body font, with a clearer message.
- Tablet novel reader: smaller side margins by default (reading column up to 720 dp).
- MyAnimeList now signs in through Tsuzuki's own registration. **If you were signed in to MyAnimeList, sign in
  again** under Settings › Tracking.
- Screen reader: pills, section labels, the Home hero and the custom source wizard announce their state.

### Fixed
- Starting or resuming an anime episode no longer flashes a still frame before playing, and resuming starts
  sooner (from the nearest keyframe).
- Novel recap: shows again after a break (it took the previous chapter from a filtered list), fetches the previous
  chapter when it is not downloaded, and appears as an overlay when you resume mid-chapter.
- Extensions: the "JS" tag follows the theme accent, and the source's "Reverse chapter list" setting is translated.
- Anime: Continue watching from Watch or the anime Library no longer waits for every extension to load first.
- Anime: videos from extensions that serve them through their own local server (newer extensions) now play.

## [0.2.3] - 2026-11-28

### Added
- Anime details: Edit info changes an anime's title, author, artist, synopsis, status and tags. Your edits stay when
  the source refreshes, and Reset brings back the source's details.
- Anime details: tap the title or author, or Search in a genre's menu, to search it on the Anime pill of Global
  search, as for books.
- Watch shows a preview card even before you have played anything: "Start watching" for the newest show in your
  library.
- Anime player: the seek bar marks chapters and shows the current chapter's name. Skip intro also works from the
  opening and ending times a source sends, without AniSkip; a new setting skips AniSkip when the video already has
  chapters (on by default).

### Changed
- The anime player is redesigned: a cleaner control layout with Speed, Lock, Episodes, Audio & subtitles and Next
  episode along the bottom, an episodes side panel, one Audio & subtitles panel with a preview, a ±85 s skip, a
  next-episode countdown and a "Resuming at" cover while the video catches up.
- Anime episodes start much sooner when a source has a slow mirror: playback begins with the first good stream and
  the slower ones keep loading for the quality list and as a fallback.
- More of the app is translated: the Library filter sheet, EPUB import, edit entry, mass import, advanced and
  library settings, source filter presets, quick migrate and tracker sign-in, and the History, Updates and Browse
  toolbar labels.

### Fixed
- Opening the reader panel no longer leaves a blank band above the text.
- Starting or resuming an anime episode no longer shows a stuck frame for a moment.

## [0.2.2] - 2026-11-24

### Added
- **Anime works like your books.** Anime now has the same tools as manga and novels:
  - Library: long-press to select anime, then move them to shelves, mark them watched or unwatched, download the
    next 1, 5 or 10 episodes (or every unwatched one), delete downloads, remove them (with Undo) or migrate them.
  - Library: the filter button opens the same Filter, Sort and Display sheet as books: filter by downloaded,
    unwatched, started, completed or tracked; sort by title, last watched, latest episode, unwatched count, date
    added or total episodes; show a comfortable, compact or cover-only grid or a list, with the book library's
    column setting. Covers show unwatched, downloaded and language badges when those are switched on in Library
    settings. The menu offers Update shelf and Open random entry.
  - Episode lists: long-press to select episodes and act on them from the bar at the bottom, and swipe an episode
    to mark it watched, bookmark it or download it, using your Library swipe settings.
  - Anime details: notes, a full-size cover you can save or share, and when the source lists an upcoming episode,
    how many days until it airs.
  - Updates: new episodes look like new chapters, can be selected, downloaded, marked watched or bookmarked, and
    the New and Save all tiles count them. History shows anime like books and can clear anime history.
  - Browse: anime catalogues can show a grid or a list, add or remove a title with a long-press, mark titles
    already in your library, open the source in WebView and search every anime source at once. Anime extensions
    have a details page and are visible before you add a repository.
  - Watch on Home shows New episodes and Watched today. Stats shows a watch-time chart for the week, month or year.
    A tall Continue Watching widget also shows this week's and this month's watch time.
  - Downloads: the anime queue can pause or resume everything, retry or clear failed episodes and move an episode
    to the top or bottom. A new setting deletes anime episodes after you watch them (off by default).
  - Anime details use the book details layout: the same header, action tiles and description with genre pills and
    notes, and on tablets the same two-pane split. Episode lists can select a range with a second long-press, sort
    by source order, show episode numbers only, and mark every episode before the last one you watched.
  - History shows Today, Episodes and Streak tiles for anime. On tablets, Updates previews the tapped episode next
    to the list, and in Updates › All episodes can be selected together with chapters.
- Anime is easier to find: Home always shows Read and Watch, and Settings, More and the first-run setup can turn
  anime on. Links from Aniyomi and Animetail that add an extension repository open in Tsuzuki.
- Novels: look up words offline in English and Japanese right away (the dictionaries are now built in), or import
  another dictionary file.

### Changed
- Backups keep your anime even while Anime is switched off, and the restore preview counts anime titles, shelves
  and history.
- Settings > Data and storage counts anime and episodes and includes downloaded episodes in the storage total.
- The anime download notification shows real progress, which episode of how many, Pause, Cancel and Retry, and
  says when it is waiting for Wi-Fi.
- Removing anime from the Library asks first, as for books, and can also delete their downloaded episodes.
- Anime catalogues and details open and close with the same fade and cover movement as book screens.

### Fixed
- Anime details no longer close when the screen rotates or after returning from the player in landscape.
- Downloaded episodes are kept while you are still watching them, even with "delete after watching" on.

## [0.2.1]

### Added
- **Natural English voice (Kokoro).** Settings > Text-to-speech > Natural voice: download the voice pack once
  (about 124 MB; every file is checked before it is installed) and novels in English are read aloud by an offline
  natural voice. Text it cannot read, and books in other languages, keep the phone voice. Needs a 64-bit ARM phone.
  Off by default; the phone voice stays the default.
- Settings > Text-to-speech: choose the phone voice by language, play a sample, open Android's speech settings, and
  set playback and read-along in one place (also reachable from the Listen panel).

### Changed
- Browse > Anime: the extensions and sources pages are redesigned, with extension icons, names without the
  "Aniyomi:" prefix, search, Update all and language pills.
- Anime migration shows readable titles.

### Fixed
- Pausing Listen at the very end of a chapter no longer replays the last sentence.
- The anime details page no longer shows a second loading spinner while it refreshes.
- Opening the reader menu no longer pushes the page down.
- Downloaded episodes from sources such as AnimeKai play offline (older downloads are repaired the first time
  you play them).

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

### Fixed
- Manga: the loading indicator stays visible, and Next no longer skips two chapters.
- A library update that starts while the app is closed waits for your sources to load instead of skipping books.

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
