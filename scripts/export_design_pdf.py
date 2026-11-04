#!/usr/bin/env python3
"""Export canvas PDF artboards without downsampling their embedded screenshots.

Example:
    python scripts/export_design_pdf.py boards.pdf docs/design/boards/png \
        --names OnboardingContent,LibraryMixed,...,DotManga --dot-manga

Requires pypdf, Pillow, and (only for pages without a full-page image) pypdfium2.
"""

import argparse
import hashlib
import json
import re
from pathlib import Path

from PIL import Image
from pypdf import PdfReader


MANGA_PHONE_NAMES = (
    "OnboardingContent", "LibraryMixed", "BrowseManga", "MangaDetails",
    "MangaPaged", "MangaReader", "MangaReaderSettings",
)


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def full_page_image(page) -> Image.Image | None:
    """Return an original raster only when its placement covers the entire page."""
    if page.rotation or len(page.images) != 1:
        return None
    placements = []

    def inspect(operator, operands, matrix, text_matrix):
        if operator == b"Do":
            placements.append(list(matrix))

    page.extract_text(visitor_operand_before=inspect)
    if len(placements) != 1:
        return None
    bounds = page.cropbox
    expected = (float(bounds.width), 0, 0, float(bounds.height),
                float(bounds.left), float(bounds.bottom))
    if any(abs(float(actual) - target) > 0.01
           for actual, target in zip(placements[0], expected)):
        return None
    original = page.images[0].image
    expected_height = original.width * float(bounds.height) / float(bounds.width)
    if abs(original.height - expected_height) > 1:
        return None
    return original.copy()


def save_png(image: Image.Image, destination, output):
    destination.parent.mkdir(parents=True, exist_ok=True)
    if image.mode not in ("1", "L", "LA", "P", "RGB", "RGBA"):
        image = image.convert("RGB")
    image.save(destination, format="PNG")
    return {
        "file": destination.relative_to(output).as_posix(),
        "width": image.width,
        "height": image.height,
        "sha256": sha256(destination),
    }


def split_dot_manga(image: Image.Image, output):
    """The seven 390x844 phones and 1280x800 tablet in the 2850x1664 board."""
    scale_x, scale_y = image.width / 2850, image.height / 1664
    if abs(scale_x - scale_y) > 0.002:
        raise ValueError("DotManga must have the 2850:1664 composite aspect ratio")
    boxes = [(name, (index * 410, 0, index * 410 + 390, 844))
             for index, name in enumerate(MANGA_PHONE_NAMES)]
    boxes.append(("TabletMangaSpread", (0, 864, 1280, 1664)))
    records = []
    for name, box in boxes:
        pixels = tuple(round(value * (scale_x if index % 2 == 0 else scale_y))
                       for index, value in enumerate(box))
        crop = image.crop(pixels)
        try:
            record = save_png(crop, output / "dot" / f"{name}.png", output)
        finally:
            crop.close()
        record.update(name=name, source_pixel_bounds=list(pixels))
        records.append(record)
    return records


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pdf", type=Path)
    parser.add_argument("output", type=Path, help="PNG output directory")
    parser.add_argument("--names", required=True, help="Comma-separated board names, in PDF order")
    parser.add_argument("--dot-manga", action="store_true", help="Also split the DotManga composite")
    parser.add_argument("--manifest", type=Path, help="Optional manifest path; default: OUTPUT/PDF-stem.manifest.json")
    args = parser.parse_args()
    names = [name.strip() for name in args.names.split(",")]
    if any(not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]*", name) for name in names):
        parser.error("Board names must contain only letters, digits, dots, underscores and hyphens")
    if len({name.casefold() for name in names}) != len(names):
        parser.error("Board names must be unique, including on case-insensitive filesystems")
    reader = PdfReader(args.pdf)
    if len(names) != len(reader.pages):
        parser.error(f"Expected {len(reader.pages)} names, received {len(names)}")
    if args.dot_manga and "DotManga" not in names:
        parser.error("--dot-manga requires a page named DotManga")
    output = args.output.resolve()
    manifest = {
        "source_file": args.pdf.name,
        "source_sha256": sha256(args.pdf),
        "page_count": len(reader.pages),
        "pages": [],
    }
    rendered_pdf = None
    try:
        for index, (name, page) in enumerate(zip(names, reader.pages)):
            image = full_page_image(page)
            method = "embedded_full_page_image"
            if image is None:
                import pypdfium2

                if rendered_pdf is None:
                    rendered_pdf = pypdfium2.PdfDocument(str(args.pdf))
                rendered_page = rendered_pdf[index]
                bitmap = rendered_page.render(scale=4 / 3)
                try:
                    image = bitmap.to_pil().copy()
                finally:
                    bitmap.close()
                    rendered_page.close()
                method = "render_4_over_3"
            try:
                record = save_png(image, output / f"{name}.png", output)
                record.update(
                    page=index + 1,
                    name=name,
                    extraction=method,
                    pdf_width_points=float(page.cropbox.width),
                    pdf_height_points=float(page.cropbox.height),
                )
                if args.dot_manga and name == "DotManga":
                    record["dot_screens"] = split_dot_manga(image, output)
                manifest["pages"].append(record)
                print(f"{index + 1}/{len(names)} {name}: {image.width}x{image.height} ({method})")
            finally:
                image.close()
    finally:
        if rendered_pdf is not None:
            rendered_pdf.close()
    manifest_path = args.manifest or output / f"{args.pdf.stem}.manifest.json"
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Manifest: {manifest_path}")


if __name__ == "__main__":
    main()
