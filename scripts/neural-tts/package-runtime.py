#!/usr/bin/env python3
"""Package the isolated arm64 runtime, preserving the official Java classes and TTS JNI API."""
import argparse
import hashlib
import importlib.util
import json
import re
import struct
import subprocess
import tempfile
import zipfile
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("audit", Path(__file__).parents[1] / "audit-neural-runtime.py")
audit = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(audit)
UPSTREAM_SHA256 = "a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7"


def tts_exports(path, llvm_nm):
    output = subprocess.check_output([str(llvm_nm), "--defined-only", "--dynamic", str(path)], encoding="utf-8")
    return sorted(set(re.findall(r"Java_com_k2fsa_sherpa_onnx_(?:OfflineTts_|GeneratedAudio_)[A-Za-z0-9_]+", output)))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--upstream-aar", type=Path, required=True)
    parser.add_argument("--library", type=Path, required=True)
    parser.add_argument("--build", type=Path, required=True)
    parser.add_argument("--llvm-nm", type=Path, required=True)
    parser.add_argument("--ndk-notice", type=Path, required=True)
    parser.add_argument("--onnx-build-manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    profile = Path(__file__).parent
    onnx_record = json.loads(args.onnx_build_manifest.read_text(encoding="utf-8-sig"))
    onnx_library = args.onnx_build_manifest.parent / "lib/libonnxruntime.a"
    if onnx_record["source_archive_sha256"] != "0a71177b993c8406e0ae50574e2319bd0f53d3e89857cd9acdde0aea22ba27f1":
        raise ValueError("ONNX source manifest is not the pinned v1.28.2 source")
    with onnx_library.open("rb") as stream:
        onnx_hash = hashlib.file_digest(stream, "sha256").hexdigest()
    if onnx_hash != onnx_record["library_sha256"]:
        raise ValueError("ONNX static archive differs from its source build manifest")
    upstream = args.upstream_aar.read_bytes()
    if hashlib.sha256(upstream).hexdigest() != UPSTREAM_SHA256:
        raise ValueError("The Java/API baseline is not the pinned official AAR")
    library = args.library.read_bytes()
    if struct.unpack_from("<H", library, 18)[0] != 183:
        raise ValueError("Candidate must be AArch64")
    inspection = audit.inspect_library("jni/arm64-v8a/libsherpa-onnx-jni.so", library)
    if not inspection["passes_nt1_artifact_gates"]:
        raise ValueError("Candidate fails the unchanged literal/16 KB artifact gates")
    if not inspection["passes_build_hygiene"]:
        raise ValueError("Candidate still contains absolute build paths")
    system = {"libandroid.so", "liblog.so", "libm.so", "libdl.so", "libc.so"}
    if not set(inspection["needed"]).issubset(system):
        raise ValueError(f"Unexpected runtime dependency: {inspection['needed']}")
    entries = {}
    with zipfile.ZipFile(args.upstream_aar) as archive:
        for item in archive.infolist():
            if not item.is_dir() and not item.filename.startswith("jni/"):
                entries[item.filename] = archive.read(item)
        with tempfile.TemporaryDirectory() as temporary:
            official = Path(temporary) / "official.so"
            official.write_bytes(archive.read("jni/arm64-v8a/libsherpa-onnx-jni.so"))
            expected = tts_exports(official, args.llvm_nm)
    actual = tts_exports(args.library, args.llvm_nm)
    if not expected or expected != actual:
        raise ValueError(f"TTS JNI API differs: expected {expected}, actual {actual}")
    entries["jni/arm64-v8a/libsherpa-onnx-jni.so"] = library
    entries["assets/tsuzuki-runtime/LICENSE.sherpa-onnx"] = (profile / "LICENSE.sherpa-onnx").read_bytes()
    entries["assets/tsuzuki-runtime/onnx-build.json"] = (json.dumps(onnx_record, indent=2) + "\n").encode()
    # kissfft's root COPYING only points to LICENSES/BSD-3-Clause, which the
    # dependency-root loop below does not descend into. Require its full text.
    entries["assets/tsuzuki-runtime/kissfft-BSD-3-Clause.txt"] = (
        profile / "licenses/kissfft-BSD-3-Clause.txt"
    ).read_bytes()
    entries["assets/tsuzuki-runtime/NOTICE.txt"] = (
        "Tsuzuki Kokoro-only research profile, based on sherpa-onnx v2.0.1.\n"
        "Modified source and reproducible build: scripts/neural-tts/ in Tsuzuki.\n"
        "Only text with a Kokoro lexicon is supported; other JNI/model APIs are unavailable.\n"
        "No model, voice data or lexicon is included. Device synthesis is not verified.\n"
    ).encode("utf-8")
    for notice in (profile / "licenses").glob("*"):
        entries["assets/tsuzuki-runtime/" + notice.name] = notice.read_bytes()
    entries["assets/tsuzuki-runtime/NDK-NOTICE.txt"] = args.ndk_notice.read_bytes()
    dependencies = args.build / "_deps"
    for source in sorted(dependencies.glob("*-src")):
        for notice in sorted(source.iterdir()):
            if notice.is_file() and re.match(r"(?:LICENSE|COPYING|NOTICE)", notice.name, re.IGNORECASE):
                entries[f"assets/tsuzuki-runtime/dependencies/{source.name}/{notice.name}"] = notice.read_bytes()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data, compresslevel=9)
    report = {"tts_jni_exports": actual, "classes_sha256": hashlib.sha256(entries["classes.jar"]).hexdigest(),
              "upstream_aar_sha256": UPSTREAM_SHA256, "candidate_library": inspection,
              "candidate_aar_sha256": hashlib.sha256(args.output.read_bytes()).hexdigest(),
              "candidate_aar_bytes": args.output.stat().st_size, "onnx_source_build": onnx_record}
    args.output.with_suffix(".api.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8", newline="")
    print(f"Packaged {args.output.name}: {report['candidate_aar_bytes']} bytes; {len(actual)} TTS JNI exports match")


if __name__ == "__main__":
    main()
