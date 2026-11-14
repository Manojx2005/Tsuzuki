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
    args = parser.parse_args()
    expected = json.loads(Path(__file__).with_name("profile-files-v2.0.1.json").read_text(encoding="utf-8"))
    destination = args.destination.resolve()
    destination.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.archive) as archive:
        for entry in archive.infolist():
            relative = Path(*Path(entry.filename).parts[1:])
            if not relative.parts:
                continue
            target = (destination / relative).resolve()
            if not target.is_relative_to(destination):
                raise ValueError(f"Source path escapes build directory: {entry.filename}")
            if entry.is_dir():
                target.mkdir(parents=True, exist_ok=True)
                continue
            data = archive.read(entry)
            if target.exists():
                actual = hashlib.sha256(target.read_bytes()).hexdigest()
                allowed = {hashlib.sha256(data).hexdigest(), expected.get(relative.as_posix())}
                if actual not in allowed:
                    raise ValueError(f"Unknown local source edit: {relative}. Use a separate build directory.")
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
    print("Upstream source verified; only the recorded profile edits are permitted")


if __name__ == "__main__":
    main()
