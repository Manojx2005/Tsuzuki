#!/usr/bin/env python3
"""Extract the verified upstream source, rejecting unknown edits and path escapes."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--pristine", action="store_true", help="Reject all profile edits and cached extra files")
    parser.add_argument("--strip-components", type=int, choices=(0, 1), default=1)
    args = parser.parse_args()
    expected = {} if args.pristine else json.loads(
        Path(__file__).with_name("profile-files-v2.0.1.json").read_text(encoding="utf-8")
    )
    destination = args.destination.resolve()
    destination.mkdir(parents=True, exist_ok=True)
    dependencies = [] if args.pristine else json.loads(
        Path(__file__).with_name("dependencies-v2.0.1.json").read_text(encoding="utf-8")
    )
    with zipfile.ZipFile(args.archive) as archive:
        entries = []
        for entry in archive.infolist():
            relative = Path(*Path(entry.filename).parts[args.strip_components:])
            if not relative.parts:
                continue
            target = (destination / relative).resolve()
            if not target.is_relative_to(destination):
                raise ValueError(f"Source path escapes build directory: {entry.filename}")
            entries.append((entry, relative, target))

        # CMake's pinned dependency archives are intentionally cached beside the sources.
        # No other file may influence a source build, including a newly added .cc/.cmake file.
        source_files = {relative.as_posix() for entry, relative, _ in entries if not entry.is_dir()}
        cached = {dependency["filename"]: dependency["sha256"] for dependency in dependencies}
        for local in destination.rglob("*"):
            relative = local.relative_to(destination).as_posix()
            if local.is_symlink():
                raise ValueError(f"Unexpected local source symlink: {relative}")
            if not local.is_file() or relative in source_files:
                continue
            if relative not in cached:
                raise ValueError(f"Unexpected local source file: {relative}. Use a separate build directory.")
            if hashlib.sha256(local.read_bytes()).hexdigest() != cached[relative]:
                raise ValueError(f"Unknown cached dependency: {relative}")

        # Validate all existing content before extracting anything.
        for entry, relative, target in entries:
            if entry.is_dir() or not target.exists():
                continue
            data = archive.read(entry)
            actual = hashlib.sha256(target.read_bytes()).hexdigest()
            allowed = {hashlib.sha256(data).hexdigest(), expected.get(relative.as_posix())}
            if actual not in allowed:
                raise ValueError(f"Unknown local source edit: {relative}. Use a separate build directory.")

        for entry, relative, target in entries:
            if entry.is_dir():
                target.mkdir(parents=True, exist_ok=True)
                continue
            data = archive.read(entry)
            if not target.exists():
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
    print("Pristine upstream source verified" if args.pristine else
          "Upstream source verified; only the recorded profile edits are permitted")


if __name__ == "__main__":
    main()
