# Anime schema design (card N23 / WORKPLAN A2.1)

Status: **design only, not implemented, not owner-approved.** No `.sq`/`.sqm` file is changed by this document.
Implementation is card A2.2 (Codex, T4 xhigh, split small) and must follow this file. Sections marked
**OPEN** need an owner decision before the card is cut.

Sources read for this design: `data/src/main/sqldelight/tachiyomi/{data,migrations}` on current `main`
(latest migration = **35**), `app/.../data/backup/models/*`, WORKPLAN §6, ARCHITECTURE §6, and Aniyomi
(Apache-2.0, `aniyomiorg/aniyomi` master) `data/src/main/sqldelightanime/dataanime/*.sq` (latest anime migration 138),
`BackupAnime/BackupEpisode/BackupAnimeHistory/BackupAnimeTracking/Backup.kt`. Aniyomi code is only a reference;
tables here are written fresh (Apache-2.0 allows copying, but the shapes differ where Tsuzuki needs them to).

## 1. Goals and constraints

1. **Never alter `mangas`** (or `chapters`, `history`, `manga_sync`, `mangas_categories`). Anime gets its own tables.
   Novels and manga keep working byte-for-byte; the migration only *adds* objects.
2. **One database, one migration chain.** Aniyomi uses a second database file (`sqldelightanime`, its own
   `categories` and numbering). Tsuzuki does not: a second DB would double the open/WAL/backup/maintenance paths
   (`DatabaseMaintenance`, backup, `adb run-as cp` restore routine) and make cross-media queries (Home, History,
   Updates, Library "All") impossible in SQL. New tables go into the existing `Database` (`tachiyomi.data`).
3. **Tsundoku migration numbering.** SQLDelight migrations are the integers `N.sqm` turning schema N into N+1.
   Latest on `main` is `35.sqm`, so ours is **`36.sqm`** (schema version becomes 37). Tsundoku/Mihon merges keep
   adding numbers; see §3.3 for how to survive a collision.
4. **Table names match Aniyomi** (`animes`, `episodes`, `animehistory` renamed `anime_history`, `anime_sync`,
   `animes_categories`) so a person reading Aniyomi sources can map 1:1 and so the data-layer mappers can be
   adapted from Aniyomi. Differences are called out per table.
5. **Mirror the Tsundoku performance design**: library lists read aggregate columns on the parent row
   (`total_count`, `read_count`, ...) maintained by triggers, not per-row subqueries.
6. **Real data only** (AGENTS.md): no column exists only because a board shows a number. Columns here are
   needed by the source API (`SAnime`/`SEpisode`), progress, or the queries in §6.
7. 16 KB, size and licence posture are unaffected (pure SQL + Kotlin).

### 1.1 Rejected: the spike's draft

`codex/anime-spike` commit `ad6442e1f` carries a `36.sqm` with `tsuzuki_anime` / `tsuzuki_episodes`. It is too
thin to be the base: no `source`-keyed aggregates, no history, no categories, no tracking, no `memo`/`version`
(needed if Mihon sync columns are ever used), `genre` as plain TEXT, `UNIQUE(source,url)` where the rest of the
codebase uses a non-unique index plus app-level dedupe (restore must be able to hold duplicates temporarily).
**Do not merge that file**; if the spike branch is ever merged, its `36.sqm` must be dropped, otherwise two files
claim number 36. It is reference only. Its `last_position_ms` naming is kept in spirit (see `episodes`).

## 2. Tables

Column types follow the existing `.sq` style (`AS Boolean`, `AS List<String>`, `AS JsonObject` adapters already
registered for `mangas`). `AnimeUpdateStrategy` and `FetchType` come from the vendored `animesource` API
(card A1.2); until that lands the columns can be typed `INTEGER` and the adapter added in A2.2.

### 2.1 `animes`

One row per anime entry (library or merely browsed, like `mangas`).

| Column | Type | Notes |
|---|---|---|
| `_id` | INTEGER PK | |
| `source` | INTEGER NOT NULL | anime source id (hash id from the extension) |
| `url` | TEXT NOT NULL | |
| `artist`, `author`, `description` | TEXT | `studio` maps to `artist`, as Aniyomi |
| `genre` | TEXT AS List<String> | |
| `title` | TEXT NOT NULL | |
| `alternative_titles` | TEXT AS List<String> | parity with `mangas` (Tsundoku added it); filled by dedupe/migrate later |
| `status` | INTEGER NOT NULL | SAnime status constants |
| `thumbnail_url` | TEXT | |
| `background_url` | TEXT | Aniyomi has it; useful for the Anime details board hero |
| `favorite` | INTEGER AS Boolean NOT NULL | in library |
| `last_update`, `next_update` | INTEGER | library update scheduling |
| `initialized` | INTEGER AS Boolean NOT NULL | details fetched |
| `viewer` | INTEGER NOT NULL | player flags (skip-op, etc.), per-anime override |
| `episode_flags` | INTEGER NOT NULL | sort/filter/display flags (same bit layout as `chapter_flags`) |
| `cover_last_modified` | INTEGER NOT NULL | |
| `background_last_modified` | INTEGER NOT NULL DEFAULT 0 | |
| `date_added` | INTEGER NOT NULL | |
| `update_strategy` | INTEGER NOT NULL DEFAULT 0 | `AnimeUpdateStrategy` |
| `calculate_interval` | INTEGER NOT NULL DEFAULT 0 | smart update |
| `fetch_type` | INTEGER NOT NULL DEFAULT 1 | Aniyomi `FetchType` (Episodes=1, Seasons=0); keep for extension compatibility |
| `parent_id` | INTEGER NULL | season parent, **see OPEN Q3** (column kept, unused at first) |
| `season_flags` | INTEGER NOT NULL DEFAULT 0 | " |
| `season_number` | REAL NOT NULL DEFAULT -1 | " |
| `season_source_order` | INTEGER NOT NULL DEFAULT 0 | " |
| `notes` | TEXT NOT NULL DEFAULT "" | parity with `mangas.notes` (BookNotes screen) |
| `last_modified_at`, `favorite_modified_at`, `version`, `is_syncing` | as `mangas` | Mihon-sync parity; cheap and keeps BackupAnime 106/107/109 meaningful |
| `memo` | BLOB AS JsonObject NOT NULL DEFAULT '{}' | |
| `total_count` | INTEGER NOT NULL DEFAULT 0 | **aggregate**: episodes |
| `seen_count` | INTEGER NOT NULL DEFAULT 0 | **aggregate** (named `seen`, as Aniyomi episodes) |
| `bookmark_count` | INTEGER NOT NULL DEFAULT 0 | **aggregate** |
| `latest_upload` | INTEGER NOT NULL DEFAULT 0 | **aggregate**: newest `date_upload` |
| `episode_fetched_at` | INTEGER NOT NULL DEFAULT 0 | **aggregate**: newest `date_fetch` |
| `last_seen` | INTEGER NOT NULL DEFAULT 0 | **aggregate**: newest history `last_seen` (Continue watching, sort by last read) |

No `is_novel`. There is no `is_anime` flag anywhere else: the *table* is the discriminator.

Indexes (names prefixed `anime`, because index names are global in SQLite):
- `animelib_favorite_index ON animes(favorite) WHERE favorite = 1`
- `animes_url_index ON animes(url)`
- `animes_source_url_index ON animes(source, url)` (Tsundoku added this to `mangas`; dedupe/refresh hit it)
- `animes_source_index ON animes(source)`
- `animes_parent_id ON animes(parent_id)` (only if seasons, Q3)

Triggers: copy the four `mangas` ones (`update_last_favorited_at_animes`, `update_last_modified_at_animes`,
`update_anime_version`) with the same bodies and `animes` names. Trigger names are global too, so they are
suffixed `_animes`/`anime_`.

**Rationale for dropping unmeasured Aniyomi fields:** none dropped, only `is_novel` added elsewhere. `hidden` on
categories (Aniyomi) is not carried: Tsuzuki categories have no hidden flag.

### 2.2 `episodes`

| Column | Type | Notes |
|---|---|---|
| `_id` | INTEGER PK | |
| `anime_id` | INTEGER NOT NULL | FK `animes(_id)` ON DELETE CASCADE |
| `url`, `name` | TEXT NOT NULL | |
| `scanlator` | TEXT | fansub group; used for the `excluded_scanlators` equivalent (Q5) |
| `seen` | INTEGER AS Boolean NOT NULL | |
| `bookmark` | INTEGER AS Boolean NOT NULL | |
| `fillermark` | INTEGER AS Boolean NOT NULL DEFAULT 0 | Aniyomi, AniSkip-adjacent |
| `last_second_seen` | INTEGER NOT NULL | resume position, **seconds** (Aniyomi + BackupEpisode 6; the spike used ms. Seconds chosen: backup interoperability) |
| `total_seconds` | INTEGER NOT NULL | duration; 0 until first playback |
| `episode_number` | REAL NOT NULL | |
| `source_order` | INTEGER NOT NULL | |
| `date_fetch`, `date_upload` | INTEGER NOT NULL | |
| `summary`, `preview_url` | TEXT | per-episode (Aniyomi) |
| `last_modified_at`, `version`, `is_syncing`, `memo` | as `chapters` | |

Indexes: `episodes_anime_id_index(anime_id)`, `episodes_unseen_by_anime_index(anime_id, seen) WHERE seen = 0`,
`episodes_library_aggregate_index(anime_id, seen, bookmark, date_upload, date_fetch)` (mirrors
`chapters_library_aggregate_index`), `episodes_updates_idx(date_fetch DESC, anime_id, date_upload)` for the
Updates feed, `episodes_url_index(url)`.

Triggers: `update_last_modified_at_episodes`, `update_episode_and_anime_version` (bodies as `chapters`),
`anime_agg_episode_insert / _update / _delete` (same shape as `manga_agg_chapter_*`, replacing `read`→`seen`,
`chapter_fetched_at`→`episode_fetched_at`; `excluded_scanlators` clauses dropped until Q5 is answered).

### 2.3 `anime_history`

Aniyomi: `animehistory(_id, episode_id UNIQUE, last_seen)`. Tsuzuki's `history` additionally has `time_read`
(total engagement minutes feed Stats + `ReadingDays`). For symmetry and the Stats "watched" tile:

```
anime_history(_id PK, episode_id INTEGER NOT NULL UNIQUE -> episodes ON DELETE CASCADE,
              last_seen INTEGER AS Date, time_watched INTEGER NOT NULL DEFAULT 0)
```
Indexes: `anime_history_episode_id_index(episode_id)`, `idx_anime_history_last_seen(last_seen)`.
Triggers: `anime_agg_history_upsert` / `_update` set `animes.last_seen = max(last_seen, ifnull(new.last_seen,0))`
**with the `ifnull` guard from migration 35** (a NULL poisoned the `mangas` equivalent; do not repeat the bug).
Name differs from Aniyomi (`animehistory`) on purpose: matches Tsuzuki's `history` naming; only the backup
mapper cares.

### 2.4 Categories: reuse `categories`, add `animes_categories` (decision, **OPEN Q1** to confirm)

Options considered:

| Option | For | Against |
|---|---|---|
| A. Shared `categories` with `content_type = 3`, new join `animes_categories` | one category screen (already has All/Manga/Novel pills), one id space, backup `BackupCategory` unchanged, no new category CRUD interactors; an "All" shelf can span media | existing queries that treat `content_type = 0` or `OR 0` as "everything" now see anime shelves; counts query joins only `mangas` |
| B. Separate `anime_categories` table (what Aniyomi effectively has) | zero interference | duplicate CRUD, duplicate UI, duplicate backup list (`backupAnimeCategories`), second reorder logic |

**Recommendation: A.** Concretely:
- `Category.CONTENT_TYPE_ANIME = 3`. Verified unknown values are inert in current code: the only SQL that branches on
  it is `getCategoriesWithBookCounts` (CASE on 0/1/2 against `mangas.is_novel`), where 3 matches no manga; Kotlin
  switches are on 0/1/2 with `else`/exhaustive-free branches (CategoryDialogs, CategoryListItem, ImportEpubDialog,
  MassImportDialog, duplicate finder). Each needs a one-line label for 3 only when the UI offers it.
- `animes_categories(_id PK, anime_id -> animes CASCADE, category_id -> categories CASCADE)`, indexes mirror
  `mangas_categories`, plus the `insert_anime_category_update_version` trigger.
- New queries: `getCategoriesWithAnimeCounts` (join `animes_categories`/`animes`), and an anime variant of
  `getCategoriesByContentType` returning `content_type IN (3, 0)`; **Q1**: should existing "All" shelves
  (content_type 0) show in the Anime library? Recommended yes (the owner's existing "Favourites" works), with the
  default category `_id = 0` shared.
- The system category (`_id 0`) is shared and cannot be deleted; anime without a row in `animes_categories` falls into it,
  exactly like manga.

### 2.5 `anime_sync` (trackers)

Same columns as Aniyomi / `manga_sync` with `manga_id`→`anime_id`, `last_chapter_read`→`last_episode_seen`,
`total_chapters`→`total_episodes`. `UNIQUE (anime_id, sync_id) ON CONFLICT REPLACE`, index `idx_anime_sync_anime_id`.
(WORKPLAN calls this `anime_tracks`; the table keeps the Aniyomi/`manga_sync` name for mapper reuse, the domain
model is `AnimeTrack`. Say if you prefer the table also named `anime_tracks`: pure rename, no other effect.)

### 2.6 `anime_sources` (stubs for missing extensions)

`sources` is keyed by id and carries `is_novel`/`is_js`. Anime source ids come from the same 63-bit hash
construction, so collisions are improbable but nonzero and a stub for an *anime* source with the same id as a
manga one would corrupt names. Separate table, as Aniyomi's `animesources`:
`anime_sources(_id PK, lang TEXT NOT NULL, name TEXT NOT NULL)`. Used by `StubAnimeSource` so a library entry
whose extension was removed still shows "Source (lang)" instead of a blank. Also `anime_extension_store` is **not**
created: the extension *repository* list stays in `extension_store` (it already has `is_novel`); the anime flag
there is **Q6** (needs a column, so it is a separate small migration card, not part of 36).

### 2.7 Deliberately not tables

- **Downloads**: the manga/novel queue is not in SQLDelight (DownloadStore/WorkManager + files on disk, ARCHITECTURE
  §3). The third engine (`AnimeDownloadEngine`) follows suit: queue in its own store, files under
  `<downloads>/<source>/<anime>/<episode>/`. No schema.
- **Views** (Aniyomi `animelib`, `animehistory` views): none exist in Tsuzuki; library queries are plain joins
  over aggregate columns. Not added.
- **Skip times / AniSkip cache**: network cache, not persistent data (card later; add a table only if needed).
- **Custom buttons / anime relations** (Aniyomi `custom_buttons`, `anime_relations`): not needed by any planned board.
- **Seasons**: only the four inert columns on `animes` (Q3).

## 3. Migration

### 3.1 Number

`data/src/main/sqldelight/tachiyomi/migrations/36.sqm`. Latest on `main` is `35.sqm`; no other local branch has a
`36.sqm` except the unmerged spike draft (§1.1). Re-run `ls migrations | sort -n | tail` at implementation time and
use latest+1; if Tsundoku/Mihon merges bring a `36`, **rename ours up** (`37.sqm`) before merging, never edit a
shipped migration. Because SQLDelight derives the version from the file count, Tsuzuki's migration must be last in
the chain at release time; this is why it should ship in one file, not three.

### 3.2 SQL (shape, to be finalised in A2.2)

```sql
-- 36.sqm: additive anime storage. mangas/chapters/history/mangas_categories are not touched.
CREATE TABLE animes ( ... columns of §2.1 ... );
CREATE INDEX animelib_favorite_index ON animes(favorite) WHERE favorite = 1;
CREATE INDEX animes_url_index ON animes(url);
CREATE INDEX animes_source_url_index ON animes(source, url);
CREATE INDEX animes_source_index ON animes(source);
CREATE TRIGGER update_last_favorited_at_animes ...;      -- bodies copied from mangas triggers
CREATE TRIGGER update_last_modified_at_animes ...;
CREATE TRIGGER update_anime_version ...;

CREATE TABLE episodes ( ... §2.2 ..., FOREIGN KEY(anime_id) REFERENCES animes(_id) ON DELETE CASCADE );
CREATE INDEX episodes_anime_id_index ON episodes(anime_id);
CREATE INDEX episodes_unseen_by_anime_index ON episodes(anime_id, seen) WHERE seen = 0;
CREATE INDEX episodes_library_aggregate_index ON episodes(anime_id, seen, bookmark, date_upload, date_fetch);
CREATE INDEX episodes_updates_idx ON episodes(date_fetch DESC, anime_id, date_upload);
CREATE INDEX episodes_url_index ON episodes(url);
CREATE TRIGGER update_last_modified_at_episodes ...;
CREATE TRIGGER update_episode_and_anime_version ...;
CREATE TRIGGER anime_agg_episode_insert ...; CREATE TRIGGER anime_agg_episode_update ...;
CREATE TRIGGER anime_agg_episode_delete ...;

CREATE TABLE anime_history ( ... §2.3 ... );
CREATE INDEX anime_history_episode_id_index ON anime_history(episode_id);
CREATE INDEX idx_anime_history_last_seen ON anime_history(last_seen);
CREATE TRIGGER anime_agg_history_upsert ...; CREATE TRIGGER anime_agg_history_update ...;  -- ifnull guarded

CREATE TABLE animes_categories ( ... §2.4 ... );
CREATE INDEX animes_categories_anime_category_index ON animes_categories(anime_id, category_id);
CREATE INDEX idx_animes_categories_category_id ON animes_categories(category_id);
CREATE TRIGGER insert_anime_category_update_version ...;

CREATE TABLE anime_sync ( ... §2.5 ... );
CREATE INDEX idx_anime_sync_anime_id ON anime_sync(anime_id);

CREATE TABLE anime_sources (_id INTEGER NOT NULL PRIMARY KEY, lang TEXT NOT NULL, name TEXT NOT NULL);
```
No `ALTER`, no `UPDATE`, no `DROP`, no data copy: nothing in the file can fail on existing owner data, and cost is
O(1). No `INSERT` into `categories` (system row exists). The same statements go into the matching `*.sq` files
(`animes.sq`, `episodes.sq`, `anime_history.sq`, `animes_categories.sq`, `anime_sync.sq`, `anime_sources.sq`) as
`CREATE ...` plus named queries, as the current convention (a `.sq` file *is* the latest schema, `.sqm` is the
upgrade path; they must agree: a fresh install runs the `.sq` creates, an upgrade runs `36.sqm`).

### 3.3 Why not many small migrations

One migration = one rollback unit (§7). If a later change is needed (Q6 `extension_store` flag, `alternative_titles`
backfill, season data) it is `37.sqm`, `38.sqm`; never edit 36 after it shipped to the owner's devices.

### 3.4 Rollback

SQLite/SQLDelight cannot downgrade: an older APK opening a DB with a higher `user_version` crashes or is refused.
Rollback is therefore **restore-from-backup**, not "go back a version":
- Before the first build containing 36 is installed on either device: take the existing 12-file
  (DB/WAL/SHM + prefs + WorkManager) copy and hash it, exactly as in past orders (`push` + `run-as cp`, SHA-256).
- Because 36 only adds tables, an older Tsuzuki build that *ignored* a higher user_version would still work, but
  Android SQLite does not allow that; do not rely on it.
- If 36 is bad: reinstall the previous APK, restore the 12 files, verify hashes. Proven path, no code needed.

## 4. Backup (protobuf) additions

Existing top-level numbers in `Backup.kt`: 1 manga, 2 categories, 101 sources, 104 prefs, 105 source prefs,
106 extension stores. `BackupManga` uses 1–18, 100–112, 8000, 8001; `BackupCategory` 1–3, 100, 8001
(`contentType`). Tsuzuki's own additions live in the 8000s.

**Decision: use Aniyomi's modern numbers at top level so one file format round-trips with Aniyomi.**

| Backup field | Number | Aniyomi? | Collision check |
|---|---|---|---|
| `backupAnime: List<BackupAnime>` | **501** | yes (modern `Backup`) | free |
| `backupAnimeCategories: List<BackupCategory>` | **502** | yes | free |
| `backupAnimeSources: List<BackupAnimeSource>` | **503** | yes | free |
| (reserve 500 = Aniyomi `isLegacy`, 504–506 = Aniyomi extensions/stores/buttons) | not used | yes | do not reuse for anything else |

- **Anime categories are not duplicated**: since categories are shared (§2.4), `backupCategories` (2) keeps carrying
  every shelf incl. `contentType = 3`, and anime rows reference them by the existing `categories: List<Long>` id
  list inside `BackupAnime`. Field 502 is *read* when present (Aniyomi imports, mapped by name into shared
  categories with `content_type = 3`) and *written* only if Q1 is answered B. Default: write nothing to 502.
- `BackupAnime` / `BackupEpisode` / `BackupAnimeHistory` / `BackupAnimeTracking`: copy the Aniyomi proto numbers
  exactly (listed in the Aniyomi files above): shared 1–18, 100–112 as in `BackupManga`, Aniyomi extras 500+
  (`backgroundUrl` 500, `parentId` 502, `id` 503, `seasonFlags` 504, `seasonNumber` 505, `seasonSourceOrder` 506,
  `fetchType` 507); `BackupEpisode` `lastSecondSeen` 6, `totalSeconds` 16, extras 501–503; `BackupAnimeHistory`
  1–3 (`readDuration` ↔ `time_watched`); `BackupAnimeTracking` 1–12, 100. **Tsuzuki-only extras** go in the 8000s
  like the manga ones: `BackupAnime.notes` 8100, `alternativeTitles` 8101.
- Legacy Aniyomi files (top-level 3/4 for anime and anime categories) are read by a small `LegacyAnimeBackup`
  decoder path only if the sniff of 501 fails; they are never written. Tsuzuki top-level 3/4 stay unused.
- Backups written by an older Tsuzuki simply have no 501–503: decode default `emptyList()`, restore does nothing
  for anime. Backups written by this version opened in an older Tsuzuki: unknown fields are skipped by
  kotlinx-protobuf (`ignoreUnknownKeys` behaviour for protobuf is the default). **Verify in the round-trip test** (§7).
- Creator/restorer: `AnimeBackupCreator`, `AnimeBackupRestorer` mirror `MangaBackupCreator`/`MangaRestorer`;
  `BackupOptions` gets an `anime` toggle (default on) so people can omit it. Restore matches anime by
  `(source, url)`, episodes by `url`, history by episode `url`, as manga does.
- **OPEN Q2**: Aniyomi `Backup.isLegacy` (500) defaults to `true` and Aniyomi uses it to choose the decoder. A
  Tsuzuki-written file lacks it, so Aniyomi would decode it as *legacy* (anime in 3/4, not 501) and see no anime.
  Fix is to also write `isLegacy = false` at 500 in Tsuzuki backups (harmless to Tsundoku, an unknown field).
  Recommend yes; the owner should confirm we want export-to-Aniyomi to work at all.

## 5. Source-side Kotlin types needing a stable id

`Episode.url`/`Anime.url` are the source key; `anime.source` is the animesource `id`. Source-id arithmetic is
supplied by the vendored `animesource` API (A1.2); the schema stores only the Long.

## 6. How features will read the tables

All queries are new, in new `.sq` files; no existing query is edited.

- **Library (Novels / Manga / Anime switch, board `LibraryMixed`)**. The existing mixed library composes novel and
  manga from `mangas` (`is_novel` filter). Anime is a third **separate paging source**, not a UNION into the
  `mangas` query: `getLibraryAnime` selects `animes` where `favorite = 1` joined to `animes_categories`, sorts on
  the aggregate columns (`last_seen`, `total_count - seen_count`, `latest_upload`, `episode_fetched_at`, `title`,
  `date_added`), filters on `seen_count`/`bookmark_count`. The "All" pill, if kept, merges three pages in memory
  (already how the combined library handles pages); counts for pills come from `COUNT(*)` per table, cheap.
  Category pills: `getCategoriesWithAnimeCounts`. Selected pill / media switch stays a UI preference.
- **Home (Watch tab)**: *Continue watching* = latest `anime_history.last_seen` joined to `episodes`/`animes` (like
  `getLatestHistory`), progress = `last_second_seen / total_seconds`; shelf = favorites ordered by `last_seen`;
  "New" count = `total_count - seen_count`. These replace the placeholder `WatchPlaceholder`.
- **Updates**: `getRecentAnimeUpdates` (driving table `episodes` on `episodes_updates_idx`, like
  `getRecentUpdates`); `UpdatesViewModel` merges the three streams by `date_fetch`. The "Airing today" strip needs
  source-provided air time; not in this schema, stays out until an extension API supplies it.
- **History**: `getAnimeHistoryWithRelations` (+ Grouped), merged with the existing history by `last_seen`/`last_read`
  in the ViewModel; rows carry a media type so the tile can say Episode instead of Ch. Time watched feeds
  `ReadingDays`-style streak if the owner wants watch minutes to count (**Q4**, default: kept separate, shown in Stats
  as "watched").
- **Downloads**: third engine, no table (§2.7); `DownloadCache` remains the on-disk truth for "downloaded" badges.
- **Trackers**: `anime_sync` via `AnimeTrackRepository`; AniList/MAL/Simkl anime lists (card in 6.3).
- **Stats**: sums `anime_history.time_watched` and counts `episodes` where `seen = 1`.
- **Duplicate finder / migration**: separate (anime sources can only migrate among anime), out of scope here.

## 7. Data-layer additions (A2.2 scope)

Packages follow Aniyomi's split under the existing modules:

- `domain`: `tachiyomi.domain.entries.anime.{model.Anime, interactor.*, repository.AnimeRepository}`,
  `tachiyomi.domain.items.episode.{model.Episode, repository.EpisodeRepository, interactor.*}`,
  `tachiyomi.domain.history.anime.{model.AnimeHistory(WithRelations), repository.AnimeHistoryRepository}`,
  `tachiyomi.domain.track.anime.{model.AnimeTrack, repository.AnimeTrackRepository}`,
  `tachiyomi.domain.library.anime.{model.LibraryAnime, interactor.GetLibraryAnime}`,
  `tachiyomi.domain.source.anime.{model.StubAnimeSource, repository.AnimeSourceRepository}`.
  Category interactors are reused (shared table), with `CONTENT_TYPE_ANIME`.
- `data`: `AnimeRepositoryImpl`, `EpisodeRepositoryImpl`, `AnimeHistoryRepositoryImpl`, `AnimeTrackRepositoryImpl`,
  `AnimeSourceRepositoryImpl`, mappers (`AnimeMapper`, `EpisodeMapper`, ...), `ColumnAdapters` entries for
  `AnimeUpdateStrategy`/`FetchType` (the `Database(...)` construction in the driver module gets the two new adapters).
- `app`: `AnimeBackupCreator/Restorer` and the four `Backup*` models (§4); Injekt registrations in `DomainModule`/
  `AppModule`.
- Interfaces keep Tsundoku's existing method naming (`awaitById`, `subscribe...`) so the review diff is mechanical.

## 8. Upgrade and rollback test plan

Gate for A2.2 and A2.5. "Owner data" = the 12-file copy of each device.

1. **Fresh install**: install the build on an emulator-free scratch profile (no `run-as` data): app starts, DB at
   version 37, all new tables empty, Home/Library/History/Updates for novels and manga behave as before. Schema
   dump of the fresh DB must equal the schema produced by applying all `.sqm` to an old DB (see 3).
2. **Upgrade from current DB**: take the current `main` DB (tablet, copy only), run migration 36 in a scratch
   install (A2.5). Assertions via `sqlite3` on the pulled copy: row counts of `mangas`, `chapters`, `history`,
   `mangas_categories`, `categories`, `manga_sync`, `sources`, `excluded_scanlators`, `extension_store` are identical
   before/after; `SELECT sql FROM sqlite_master` for those tables is byte-identical; `PRAGMA integrity_check` and
   `PRAGMA foreign_key_check` return ok; new tables empty; cold start time within noise (record).
3. **Unit-level schema test** (JVM, `sqlite-jdbc` or SQLDelight `verifyMigration`): apply all migrations from an
   empty DB and compare to the generated schema; insert an anime, episodes, history, category link, track and
   check the four aggregate triggers (insert, seen toggle, delete, history NULL `last_seen` with the ifnull guard),
   and that deleting an `animes` row cascades to all children and leaves manga rows alone.
4. **Backup round trip**: (a) backup with anime → restore into an empty profile → `animes`, `episodes`, history,
   categories (incl. `content_type = 3`), tracks compare equal by `(source,url)`; (b) restore a pre-anime backup
   (e.g. `tablet-transfer.tachibk`): manga/novels restore as before, no anime, no crash; (c) open an anime-bearing
   Tsuzuki backup with the **previous release** build: restores manga/novels, ignores 501–503, no crash;
   (d) decode `isLegacy = false` shape (Q2).
5. **Aniyomi backup import mapping** (fixture: an owner-approved real Aniyomi `.tachibk`, or one hand-built from
   the Aniyomi proto classes): `BackupAnime` → `animes` (artist/author/status/genre/thumbnail/dateAdded/flags),
   `categories: List<Long>` resolved through `backupAnimeCategories` (502) into shared categories created with
   `content_type = 3`, name clashes with an existing shelf merge into it; `BackupEpisode.lastSecondSeen/totalSeconds` →
   same units; `BackupAnimeHistory.readDuration` → `time_watched`; `BackupAnimeTracking` → `anime_sync`;
   seasons (`parentId/seasonNumber`) are imported as separate entries with `parent_id` set (Q3) or flattened; manga part of
   the Aniyomi file goes through the unchanged manga restorer. Unknown source ids → rows kept, `anime_sources` stub
   added if the name is known (`BackupAnimeSource`).
6. **Rollback drill**: with a scratch device profile on the pre-36 build, install the 36 build, use anime briefly,
   reinstall the pre-36 build over it (expected: refuses/crashes or recreates — record which), restore the saved 12
   files, verify SHA-256 and that Home shows the owner's Continue book. Do this once before any release containing 36.
7. **Performance guard**: `EXPLAIN QUERY PLAN` for `getLibraryAnime`, `getRecentAnimeUpdates`,
   `getAnimeHistoryWithRelations` shows index use (no full scans of `episodes`); library of 5k synthetic anime x 24
   episodes paginates without regression in the novel/manga queries (they do not share tables).
8. **Device runbook**: backup 12 files per device first (AGENTS.md routine), apply, run 1+2 on the copies, never on
   the live profile; restore after. Evidence under `app/build/anime-schema-<date>/`.

## 9. Owner decisions (2026-11-06) — these override the recommendations above

- **Q1 Categories → option B, separate.** New `anime_categories` table (own ids, own order, own flags) and join
  `animes_categories(anime_id → animes CASCADE, category_id → anime_categories CASCADE)`. Anime never appears in
  book shelves and book "All" shelves never appear in the Anime library. Anime keeps its own system/default
  category row (`_id 0` inside `anime_categories`, not deletable). Own CRUD interactors + an anime shelf screen
  (reuse the Tsuzuki category UI components with an anime repository). `getCategoriesWithBookCounts` is untouched.
- **Q2 Aniyomi interop → import only.** Tsuzuki reads Aniyomi backups (anime fields 500–503); our own backups store
  anime in the 8000 range and need not open in Aniyomi.
- **Q3 Seasons → keep the four columns, ignore them in the UI** (every season is a normal entry in v1).
- **Q4 Watch time → separate.** Reading streak/goal stay reading-only; watch minutes go to their own stats.
- **Q5 Fansub exclusion → left out of v1.**
- **Q6 Repo media flag → separate small migration** shipped with the anime extension-repo UI card, not in the
  anime-tables migration.
- **Q7 Table name → `anime_sync`** (Claude's call: Aniyomi/mapper-friendly; domain model `AnimeTrack`).
- **Q8 Spike → discarded.** `codex/anime-spike` worktree + local branch deleted 2026-11-06; remote kept as backup.
- **Migration merge:** owner allows Claude to merge the anime-tables migration into main overnight **only if** the
  line-by-line review, the in-memory upgrade test and a scratch-install upgrade test all pass.
- **Test extension repos (owner-approved, debug builds only):** `https://github.com/yuzono/anime-extensions` and
  `https://github.com/Secozzi/aniyomi-extensions`. Install one extension at a time into the private extension dir.

## 10. Original open questions (answered in §9)

- **Q1 Categories.** Share `categories` (new `content_type = 3`, recommended) or separate anime categories? And do
  existing "All" shelves (type 0) appear in the Anime library (recommended: yes)?
- **Q2 Aniyomi interop.** Should Tsuzuki backups open in Aniyomi (write `isLegacy = false` at top-level 500; 501–503
  as Aniyomi)? If not, we can use the 8000 range for anime and stop caring about Aniyomi's format on export (import
  of Aniyomi files is kept either way).
- **Q3 Seasons.** Aniyomi's newest versions model seasons as child `animes` (`parent_id`, `fetch_type`). v1 plan: keep
  the columns, ignore them in the UI (every season is a normal entry). Confirm, or drop the four columns now (they can
  be added later with `ALTER`, but then Aniyomi-backup seasons would be flattened for anyone importing before that).
- **Q4 Watch time in streaks.** Does a day of watching count toward the reading streak/goal, or are they separate?
- **Q5 Excluded scanlators equivalent.** Fansub-group exclusion for anime (an `excluded_scanlators` twin and the
  trigger clauses) or leave out for v1 (recommended: leave out; add in a later migration if asked).
- **Q6 Extension repository flag.** Anime repos need an `is_anime`/media column on `extension_store` (a separate,
  tiny migration, can ship with the anime extension UI card rather than 36).
- **Q7 Table name** `anime_sync` (Aniyomi/mapper-friendly) vs `anime_tracks` (WORKPLAN wording).
- **Q8 Spike branch.** Confirm that `codex/anime-spike`'s `36.sqm` (`tsuzuki_anime`) is discarded; nothing in main
  depends on it.
