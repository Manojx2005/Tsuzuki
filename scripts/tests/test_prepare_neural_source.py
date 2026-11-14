"""Source preparation must preserve edits and reject archive path escapes."""
import subprocess
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


if __name__ == "__main__":
    unittest.main()
