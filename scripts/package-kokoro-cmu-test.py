#!/usr/bin/env python3
"""Build the NT2 listening pack from pinned Kokoro weights and our reviewed CMUdict conversion."""
import argparse
import hashlib
import importlib.util
import json
import shutil
import tarfile
import tempfile
import zipfile
from pathlib import Path

ARCHIVE_SHA256 = "259c9c8bc906d6078e4e0570421885dabb68b539c2f970fcbb890a58eeaf3e9c"
LEXICON_SHA256 = "3f9d023400a8b0c4730c095729363c9374c8e8efbf65ad62e2df67f0d18efb50"
ROOT = Path(__file__).resolve().parents[1]
PACK = "kokoro-cmu-v1"


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def package(source, output, manifest_output):
    if digest(source) != ARCHIVE_SHA256:
        raise ValueError("Kokoro archive does not match the official release digest")
    spec = importlib.util.spec_from_file_location("cmudict_builder", ROOT / "scripts/neural-tts/build-lexicon-cmudict.py")
    builder = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(builder)
    cmu = builder.ensure_pinned(ROOT / ".native/archives")
    lexicon = builder.render(builder.build_entries(cmu.read_text(encoding="utf-8").splitlines()))
    if hashlib.sha256(lexicon.encode("utf-8")).hexdigest() != LEXICON_SHA256:
        raise ValueError("CMUdict conversion differs from the reviewed output")
    with tempfile.TemporaryDirectory(dir=output.parent) as temporary:
        stage = Path(temporary)
        # Read only this allowlist. No upstream lexicon or legacy phonemizer data is copied.
        with tarfile.open(source, "r:bz2") as archive:
            for name in ("model.int8.onnx", "voices.bin", "tokens.txt", "LICENSE"):
                member = archive.getmember("kokoro-int8-multi-lang-v1_1/" + name)
                if not member.isfile() or member.size <= 0:
                    raise ValueError("Expected regular model file: " + name)
                with archive.extractfile(member) as incoming, (stage / name).open("wb") as target:
                    shutil.copyfileobj(incoming, target)
        (stage / "lexicon-us-en.txt").write_text(lexicon, encoding="utf-8", newline="")
        vocabulary = {line.split()[0] for line in (stage / "tokens.txt").read_text(encoding="utf-8").splitlines() if line.strip()}
        symbols = {phone for line in lexicon.splitlines() for phone in line.split()[1:]}
        if not symbols <= vocabulary:
            raise ValueError("CMUdict symbols are absent from this model: " + repr(symbols - vocabulary))
        for source_name, name in (("cmudict-LICENSE.txt", "CMUDICT-LICENSE.txt"), ("cmudict-NOTICE.txt", "CMUDICT-NOTICE.txt")):
            shutil.copyfile(ROOT / "scripts/neural-tts/licenses" / source_name, stage / name)
        (stage / "NOTICE.txt").write_text(
            "Kokoro weights: hexgrad/Kokoro-82M-v1.1-zh, Apache-2.0; ONNX conversion by k2-fsa/sherpa-onnx.\n"
            "US English lexicon converted independently from pinned CMUdict, under its included licence.\n"
            "No upstream pronunciation dictionary, legacy phonemizer data or Chinese lexicon included.\n"
            "Debug listening test only. Voice 0, US lexicon; quality and speed have not passed device review.\n",
            encoding="utf-8", newline="",
        )
        files = {p.name: {"size": p.stat().st_size, "sha256": digest(p)} for p in sorted(stage.iterdir())}
        manifest = {"schema": 1, "pack": PACK, "language": "en", "speaker": 0,
                    "archiveSha256": ARCHIVE_SHA256, "cmuCommit": builder.CMUDICT_COMMIT, "files": files}
        manifest_bytes = (json.dumps(manifest, indent=2, sort_keys=True) + "\n").encode("utf-8")
        manifest_output.parent.mkdir(parents=True, exist_ok=True)
        with output.open("xb") as stream:
            with zipfile.ZipFile(stream, "w", compression=zipfile.ZIP_DEFLATED) as archive:
                for path in sorted(stage.iterdir()):
                    info = zipfile.ZipInfo(PACK + "/" + path.name, date_time=(2026, 11, 15, 0, 0, 0))
                    info.compress_type = zipfile.ZIP_DEFLATED
                    archive.writestr(info, path.read_bytes())
                info = zipfile.ZipInfo(PACK + "/manifest.json", date_time=(2026, 11, 15, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                archive.writestr(info, manifest_bytes)
        manifest_output.write_bytes(manifest_bytes)
    print(json.dumps({"pack": str(output), "bytes": output.stat().st_size, "sha256": digest(output)}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--manifest", type=Path, required=True)
    args = parser.parse_args()
    package(args.source, args.output, args.manifest)
