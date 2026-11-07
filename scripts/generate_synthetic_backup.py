#!/usr/bin/env python3
"""Generate a Tsuzuki library fixture for a scratch install, using only the Python standard library.

Defaults produce 500 books, 30 shelves, 50,000 chapters and reading history.
Wire fields follow app/src/main/java/eu/kanade/tachiyomi/data/backup/models.
"""

import argparse
import gzip
import struct
import time
from pathlib import Path


DAY_MS = 86_400_000
MANGA_SOURCE_ID = 7_346_286_645_911_001_101
NOVEL_SOURCE_ID = 7_346_286_645_911_001_102


def varint(value):
    result = bytearray()
    while value >= 0x80:
        result.append((value & 0x7F) | 0x80)
        value >>= 7
    result.append(value)
    return bytes(result)


def integer(field, value):
    return varint(field << 3) + varint(value)


def message(field, payload):
    return varint((field << 3) | 2) + varint(len(payload)) + payload


def string(field, value):
    return message(field, value.encode("utf-8"))


def float32(field, value):
    return varint((field << 3) | 5) + struct.pack("<f", value)


def chapter_message(url, number, total, uploaded, completed, in_progress, novel):
    progress = 60 if novel else 7
    return b"".join(
        (
            string(1, url),
            string(2, f"[synthetic] Chapter {number:03}"),
            integer(4, int(completed)),
            integer(5, int(number % 17 == 0)),
            integer(6, progress if in_progress else 0),
            integer(7, uploaded + 3_600_000),
            integer(8, uploaded),
            float32(9, number),
            integer(10, total - number),
        )
    )


def book_message(number, shelf_count, chapter_count, anchor_ms):
    novel = number % 2 == 1
    kind = "Novel" if novel else "Manga"
    url = f"/synthetic/{kind.lower()}/{number}"
    newest_upload = anchor_ms - DAY_MS * (1 + number % 14)
    completed_count = (number * 7) % chapter_count
    shelf_order = (number - 1) % shelf_count

    yield integer(1, NOVEL_SOURCE_ID if novel else MANGA_SOURCE_ID)
    yield string(2, url)
    yield string(3, f"[synthetic] {kind} {number:03}")
    yield string(5, "[synthetic] Author")
    yield integer(8, 1)  # Ongoing
    yield integer(13, anchor_ms - (14 + number % 30) * DAY_MS)
    for chapter_number in range(chapter_count, 0, -1):
        chapter_url = f"{url}/chapter/{chapter_number}"
        uploaded = newest_upload - (chapter_count - chapter_number) * DAY_MS
        completed = chapter_number <= completed_count
        in_progress = chapter_number == completed_count + 1
        yield message(
            16,
            chapter_message(chapter_url, chapter_number, chapter_count, uploaded, completed, in_progress, novel),
        )
        if in_progress or chapter_number == completed_count:
            last_read = anchor_ms - (number % 14) * DAY_MS - (15 if in_progress else 30) * 60_000
            duration_ms = (5 + number % 12) * 60_000
            yield message(104, string(1, chapter_url) + integer(2, last_read) + integer(3, duration_ms))
    yield integer(17, shelf_order)
    yield integer(100, 1)  # Favorite
    yield integer(111, 1)  # Initialized
    yield integer(8000, int(novel))


def write_backup(output, books, shelves, chapters_per_book, anchor_ms):
    with output.open("xb") as target, gzip.GzipFile(filename="", fileobj=target, mode="wb", mtime=0) as backup:
        for number in range(1, books + 1):
            book = b"".join(book_message(number, shelves, chapters_per_book, anchor_ms))
            backup.write(message(1, book))
        for order in range(shelves):
            category = string(1, f"[synthetic] Shelf {order + 1:02}") + integer(2, order) + integer(3, order + 1)
            backup.write(message(2, category))
        for name, source_id in (("Manga", MANGA_SOURCE_ID), ("Novel", NOVEL_SOURCE_ID)):
            source = string(1, f"[synthetic] {name} source") + integer(2, source_id)
            backup.write(message(101, source))


def positive_int(raw):
    value = int(raw)
    if value <= 0:
        raise argparse.ArgumentTypeError("must be positive")
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("output", type=Path, help="a new .tachibk file")
    parser.add_argument("--books", type=positive_int, default=500)
    parser.add_argument("--shelves", type=positive_int, default=30)
    parser.add_argument("--chapters-per-book", type=positive_int, default=100)
    parser.add_argument("--anchor-ms", type=positive_int, help="timestamp in milliseconds for reproducible output")
    args = parser.parse_args()
    if args.output.suffix.lower() != ".tachibk":
        parser.error("output must end in .tachibk")
    anchor_ms = args.anchor_ms if args.anchor_ms is not None else time.time_ns() // 1_000_000
    if anchor_ms <= (args.chapters_per_book + 44) * DAY_MS:
        parser.error("the anchor must allow all generated timestamps to be positive")
    try:
        write_backup(args.output, args.books, args.shelves, args.chapters_per_book, anchor_ms)
    except OSError as error:
        parser.error(str(error))
    print(f"Wrote {args.books:,} books, {args.shelves:,} shelves and "
          f"{args.books * args.chapters_per_book:,} chapters to {args.output}")


if __name__ == "__main__":
    main()
