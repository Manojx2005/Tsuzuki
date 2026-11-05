"""Reject GPL-enabled configs, missing libraries and non-16 KB arm64 ELF files."""

import hashlib
import json
from pathlib import Path
import re
import struct
import sys


def audit(root):
    evidence = root / "evidence"
    ffmpeg = (evidence / "ffmpeg-config.h").read_text()
    for name in ("GPL", "GPLV3", "NONFREE", "POSTPROC"):
        matches = re.findall(rf"^#define CONFIG_{name} ([01])$", ffmpeg, re.M)
        if matches != ["0"]:
            raise ValueError(f"FFmpeg CONFIG_{name} must be explicitly disabled: {matches}")

    version3 = re.findall(r"^#define CONFIG_VERSION3 ([01])$", ffmpeg, re.M)
    licence = re.findall(r'^#define FFMPEG_LICENSE "([^"]+)"$', ffmpeg, re.M)
    if len(version3) != 1:
        raise ValueError("FFmpeg version3 configuration must be explicit")
    expected = "LGPL version 3 or later" if version3 == ["1"] else "LGPL version 2.1 or later"
    if licence != [expected]:
        raise ValueError(f"Unexpected FFmpeg licence for version3={version3}: {licence}")

    options = json.loads((evidence / "mpv-buildoptions.json").read_text())
    gpl = [option["value"] for option in options if option["name"] == "gpl"]
    if gpl != [False]:
        raise ValueError(f"libmpv GPL option must be false: {gpl}")

    libraries = root / "jniLibs/arm64-v8a"
    required = {"libmpv.so", "libplayer.so", "libavcodec.so", "libavformat.so", "libavutil.so", "libc++_shared.so"}
    paths = sorted(libraries.glob("*.so"))
    missing = required - {path.name for path in paths}
    if missing or (libraries / "libpostproc.so").exists():
        raise ValueError(f"Missing libraries: {missing}; postproc must be absent")

    report = {}
    for path in paths:
        data = path.read_bytes()
        if data[:6] != b"\x7fELF\x02\x01":
            raise ValueError(f"Expected little-endian ELF64: {path.name}")
        header = struct.unpack_from("<16sHHIQQQIHHHHHH", data)
        if header[2] != 183:
            raise ValueError(f"Expected AArch64: {path.name}")
        alignments = []
        for index in range(header[10]):
            segment = struct.unpack_from("<IIQQQQQQ", data, header[5] + index * header[9])
            if segment[0] == 1:
                alignment = segment[7]
                if alignment < 16384 or (segment[3] - segment[2]) % 16384:
                    raise ValueError(f"Unaligned PT_LOAD in {path.name}: {segment}")
                alignments.append(alignment)
        if not alignments:
            raise ValueError(f"No PT_LOAD in {path.name}")
        report[path.name] = {"sha256": hashlib.sha256(data).hexdigest(), "load_alignments": alignments}
    (evidence / "audit.json").write_text(json.dumps(report, indent=2) + "\n")
    (evidence / "ffmpeg-license.txt").write_text(licence[0] + "\n")
    print(f"Audited {len(report)} arm64 ELF libraries; GPL options disabled, PT_LOAD >= 16 KB.")
    print("This checks build configuration and ELF layout; licence/source review and APK zipalign remain required.")


if __name__ == "__main__":
    audit(Path(sys.argv[1]))
