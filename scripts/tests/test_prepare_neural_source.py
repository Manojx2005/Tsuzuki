"""Source preparation must preserve edits and reject archive path escapes."""
import subprocess
import hashlib
import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

SCRIPT = Path(__file__).parents[1] / "neural-tts/prepare-source.py"


class PrepareSourceTest(unittest.TestCase):
    def run_prepare(self, archive, destination):
        return subprocess.run([sys.executable, str(SCRIPT), str(archive), str(destination)], capture_output=True, text=True)

    def test_pristine_source_can_be_prepared_repeatedly(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            archive, destination = root / "source.zip", root / "src"
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("sherpa-onnx-2.0.1/source.cc", "original source\n")
            self.assertEqual(0, self.run_prepare(archive, destination).returncode)
            self.assertEqual(0, self.run_prepare(archive, destination).returncode)
            self.assertEqual("original source\n", (destination / "source.cc").read_text())

    def test_unknown_local_edits_are_rejected_and_preserved(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            archive, destination = root / "source.zip", root / "src"
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("sherpa-onnx-2.0.1/source.cc", "original source\n")
            destination.mkdir()
            (destination / "source.cc").write_text("a local edit\n")
            result = self.run_prepare(archive, destination)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("Unknown local source edit", result.stderr)
            self.assertEqual("a local edit\n", (destination / "source.cc").read_text())

    def test_archive_cannot_escape_source_directory(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            archive, destination = root / "source.zip", root / "src"
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("sherpa-onnx-2.0.1/../escaped.cc", "outside\n")
            result = self.run_prepare(archive, destination)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("escapes build directory", result.stderr)
            self.assertFalse((root / "escaped.cc").exists())

    def test_added_source_file_is_rejected_before_any_extraction(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            archive, destination = root / "source.zip", root / "src"
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("sherpa-onnx-2.0.1/source.cc", "original source\n")
            destination.mkdir()
            extra = destination / "extra.cc"
            extra.write_text("unreviewed code\n")
            result = self.run_prepare(archive, destination)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("Unexpected local source file", result.stderr)
            self.assertEqual("unreviewed code\n", extra.read_text())
            self.assertFalse((destination / "source.cc").exists())

    def test_pinned_dependency_name_does_not_allow_changed_archive(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            archive, destination = root / "source.zip", root / "src"
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("sherpa-onnx-2.0.1/source.cc", "original source\n")
            destination.mkdir()
            (destination / "json-3.12.0.tar.gz").write_bytes(b"unverified archive")
            result = self.run_prepare(archive, destination)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("Unknown cached dependency", result.stderr)

    def test_pristine_mode_does_not_accept_a_recorded_profile_edit(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            script = root / "prepare-source.py"
            script.write_bytes(SCRIPT.read_bytes())
            (root / "profile-files-v2.0.1.json").write_text(json.dumps({
                "source.cc": hashlib.sha256(b"profile edit\n").hexdigest(),
            }))
            (root / "dependencies-v2.0.1.json").write_text("[]")
            archive, destination = root / "source.zip", root / "src"
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("upstream/source.cc", "original source\n")
            destination.mkdir()
            (destination / "source.cc").write_bytes(b"profile edit\n")
            command = [sys.executable, str(script), str(archive), str(destination)]
            self.assertEqual(0, subprocess.run(command, capture_output=True).returncode)
            result = subprocess.run(command + ["--pristine"], capture_output=True, text=True)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("Unknown local source edit", result.stderr)

    def test_archive_without_root_directory_preserves_its_paths(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            archive, destination = root / "tools.zip", root / "tools"
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("bin/tool.txt", "verified tool fixture")
                z.writestr("include/header.h", "verified header fixture")
            result = subprocess.run(
                [sys.executable, str(SCRIPT), str(archive), str(destination),
                 "--pristine", "--strip-components", "0"], capture_output=True, text=True,
            )
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual("verified tool fixture", (destination / "bin/tool.txt").read_text())
            self.assertEqual("verified header fixture", (destination / "include/header.h").read_text())


if __name__ == "__main__":
    unittest.main()
