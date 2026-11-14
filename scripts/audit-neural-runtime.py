#!/usr/bin/env python3
"""Report the unchanged NT1 literal-string and 16 KB ELF gates for AAR/APK files.

This is an artifact check, not a licence verdict. In particular, CreateSpeaker
contains the case-insensitive substring espeak; such matches still fail NT1.
No model is downloaded, loaded or executed. Python standard library only.
"""

import argparse
import hashlib
import json
import re
import struct
import sys
import zipfile
from pathlib import Path

FORBIDDEN = re.compile(rb"espeak|piper", re.IGNORECASE)
PRINTABLE = re.compile(rb"[ -~]{4,}")
RUNTIME_MARKERS = (
    b"espeak_Initialize", b"espeak_Synth", b"espeak_TextToPhonemes",
    b"espeak_SetVoiceByName", b"espeak_ng_Initialize", b"phontab",
)


def inspect_elf(data):
    """Read load alignment and DT_NEEDED directly, for both Android ELF classes."""
    if len(data) < 52 or data[:4] != b"\x7fELF":
        raise ValueError("Native library is not an ELF file")
    if data[5] != 1 or data[4] not in (1, 2):
        raise ValueError("Unsupported ELF encoding or class")
    is64 = data[4] == 2
    minimum = 64 if is64 else 52
    if len(data) < minimum:
        raise ValueError("Truncated ELF header")
    if is64:
        phoff = struct.unpack_from("<Q", data, 32)[0]
        size, count = struct.unpack_from("<HH", data, 54)
        expected, fmt = 56, "<IIQQQQQQ"
    else:
        phoff = struct.unpack_from("<I", data, 28)[0]
        size, count = struct.unpack_from("<HH", data, 42)
        expected, fmt = 32, "<IIIIIIII"
    if count == 0 or size < expected or phoff + size * count > len(data):
        raise ValueError("Invalid or truncated ELF program headers")
    loads, dynamic = [], None
    for index in range(count):
        values = struct.unpack_from(fmt, data, phoff + size * index)
        if is64:
            kind, _, offset, virtual, _, filesz, _, align = values
        else:
            kind, offset, virtual, _, filesz, _, _, align = values
        if offset + filesz > len(data):
            raise ValueError("Truncated ELF segment")
        if kind == 1:
            loads.append((offset, virtual, filesz, align))
        elif kind == 2:
            dynamic = (offset, filesz)
    if not loads:
        raise ValueError("ELF contains no load segments")
    needed, strings_at = [], None
    if dynamic:
        offset, size = dynamic
        entry_size, entry_fmt = (16, "<qQ") if is64 else (8, "<iI")
        for pos in range(offset, offset + size - entry_size + 1, entry_size):
            tag, value = struct.unpack_from(entry_fmt, data, pos)
            if tag == 0:
                break
            if tag == 1:
                needed.append(value)
            elif tag == 5:
                strings_at = value
    dependencies = []
    if needed:
        string_offset = next((off + strings_at - virtual for off, virtual, filesz, _ in loads
                              if strings_at is not None and virtual <= strings_at < virtual + filesz), None)
        if string_offset is None:
            raise ValueError("ELF dependencies have no valid string table")
        for index in needed:
            start = string_offset + index
            end = data.find(b"\0", start)
            if start >= len(data) or end < start:
                raise ValueError("Invalid ELF dependency string")
            dependencies.append(data[start:end].decode("ascii"))
    alignments = [entry[3] for entry in loads]
    return {"elf_class": 64 if is64 else 32, "load_alignments": alignments,
            "aligned_16kb": all(a >= 16384 and a % 16384 == 0 and (a & (a - 1)) == 0 and off % a == virtual % a
                                for off, virtual, _, a in loads),
            "needed": dependencies}


def inspect_library(name, data):
    result = {"name": name, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
    result.update(inspect_elf(data))
    result["literal_grep_matches"] = list(dict.fromkeys(
        match.decode("ascii") for match in PRINTABLE.findall(data) if FORBIDDEN.search(match)
    ))
    result["runtime_markers"] = [marker.decode("ascii") for marker in RUNTIME_MARKERS if marker in data]
    result["passes_nt1_artifact_gates"] = not result["literal_grep_matches"] and result["aligned_16kb"]
    return result


def inspect_artifact(path):
    if path.suffix == ".so":
        libraries = [inspect_library(path.name, path.read_bytes())]
    else:
        with zipfile.ZipFile(path) as archive:
            names = [name for name in archive.namelist() if name.endswith(".so")]
            if len(names) != len(set(names)):
                raise ValueError("Duplicate native ZIP entries cannot pass an unambiguous audit")
            libraries = [inspect_library(name, archive.read(name)) for name in names]
    if not libraries:
        raise ValueError(f"{path.name}: no native libraries; an empty archive cannot pass")
    return {"artifact": path.name, "bytes": path.stat().st_size,
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "libraries": libraries,
            "passes_nt1_artifact_gates": all(lib["passes_nt1_artifact_gates"] for lib in libraries)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("artifacts", nargs="+", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    reports = [inspect_artifact(path) for path in args.artifacts]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(reports, indent=2) + "\n", encoding="utf-8", newline="")
    for report in reports:
        failed = sum(not lib["passes_nt1_artifact_gates"] for lib in report["libraries"])
        print(f"{report['artifact']}: {len(report['libraries'])} libraries, {failed} fail")
    return 0 if all(report["passes_nt1_artifact_gates"] for report in reports) else 1


if __name__ == "__main__":
    sys.exit(main())
