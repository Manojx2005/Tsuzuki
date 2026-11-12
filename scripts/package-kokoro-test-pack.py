#!/usr/bin/env python3
"""Repack an owner-supplied Kokoro archive for Tsuzuki's Debug listening test."""

import argparse
import shutil
import tarfile
import zipfile
from pathlib import Path


PACK = "kokoro-int8-multi-lang-v1_1"
FILES = ("model.int8.onnx", "voices.bin", "tokens.txt", "lexicon-us-en.txt", "LICENSE")


def package(source: Path, destination: Path) -> None:
    with tarfile.open(source, "r:bz2") as archive:
        members = [archive.getmember(f"{PACK}/{name}") for name in FILES]
        if any(not member.isfile() or member.size == 0 for member in members):
            raise ValueError("The Kokoro archive must contain the expected nonempty regular files")

        with destination.open("xb") as output:
            try:
                with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as result:
                    for member in members:
                        with archive.extractfile(member) as source_file, result.open(member.name, "w") as target:
                            shutil.copyfileobj(source_file, target)
            except BaseException:
                output.close()
                destination.unlink(missing_ok=True)
                raise


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help=f"Owner-supplied {PACK}.tar.bz2")
    parser.add_argument("destination", type=Path, help="New zip path")
    arguments = parser.parse_args()
    package(arguments.source, arguments.destination)
