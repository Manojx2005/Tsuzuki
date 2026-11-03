"""Exercise acceptance and rejection boundaries without native build tooling."""

import contextlib
import io
import json
from pathlib import Path
import struct
import tempfile
import unittest

from audit import audit


def elf(alignment=16384, address=0, machine=183):
    ident = b"\x7fELF\x02\x01\x01" + bytes(9)
    header = struct.pack("<16sHHIQQQIHHHHHH", ident, 3, machine, 1, 0, 64, 0, 0, 64, 56, 1, 0, 0, 0)
    segment = struct.pack("<IIQQQQQQ", 1, 5, 0, address, 0, 120, 120, alignment)
    return header + segment


class NativeAuditTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.evidence = self.root / "evidence"
        self.evidence.mkdir()
        self.config = self.evidence / "ffmpeg-config.h"
        self.config.write_text("".join(f"#define CONFIG_{name} 0\n" for name in (
            "GPL", "GPLV3", "NONFREE", "POSTPROC",
        )) + '#define CONFIG_VERSION3 1\n#define FFMPEG_LICENSE "LGPL version 3 or later"\n')
        self.options = self.evidence / "mpv-buildoptions.json"
        self.options.write_text(json.dumps([{"name": "gpl", "value": False}]))
        self.libraries = self.root / "jniLibs/arm64-v8a"
        self.libraries.mkdir(parents=True)
        for name in ("libmpv", "libplayer", "libavcodec", "libavformat", "libavutil", "libc++_shared"):
            (self.libraries / f"{name}.so").write_bytes(elf())

    def run_audit(self):
        with contextlib.redirect_stdout(io.StringIO()):
            audit(self.root)

    def test_aligned_inputs_produce_complete_hash_manifest(self):
        self.run_audit()
        report = json.loads((self.evidence / "audit.json").read_text())
        self.assertEqual(set(report), {p.name for p in self.libraries.glob("*.so")})
        self.assertTrue(all(len(item["sha256"]) == 64 for item in report.values()))

    def test_each_disallowed_ffmpeg_option_is_rejected(self):
        clean = self.config.read_text()
        for name in ("GPL", "GPLV3", "NONFREE", "POSTPROC"):
            with self.subTest(option=name):
                self.config.write_text(clean.replace(f"CONFIG_{name} 0", f"CONFIG_{name} 1"))
                with self.assertRaisesRegex(ValueError, f"CONFIG_{name}"):
                    self.run_audit()

    def test_lgpl_version_modes_are_accepted_and_recorded(self):
        clean = self.config.read_text()
        for version3, licence in ((0, "LGPL version 2.1 or later"), (1, "LGPL version 3 or later")):
            with self.subTest(version3=version3):
                self.config.write_text(clean.replace("CONFIG_VERSION3 1", f"CONFIG_VERSION3 {version3}")
                                       .replace("LGPL version 3 or later", licence))
                self.run_audit()
                self.assertEqual((self.evidence / "ffmpeg-license.txt").read_text().strip(), licence)

    def test_missing_or_non_lgpl_licence_is_rejected(self):
        clean = self.config.read_text()
        for licence in ("GPL version 3 or later", "nonfree and unredistributable", ""):
            with self.subTest(licence=licence):
                self.config.write_text(clean.replace("LGPL version 3 or later", licence))
                with self.assertRaisesRegex(ValueError, "FFmpeg licence"):
                    self.run_audit()

    def test_missing_configuration_is_rejected(self):
        self.config.write_text(self.config.read_text().replace("#define CONFIG_GPL 0\n", ""))
        with self.assertRaisesRegex(ValueError, "CONFIG_GPL"):
            self.run_audit()

    def test_enabled_or_missing_mpv_gpl_option_is_rejected(self):
        for options in ([{"name": "gpl", "value": True}], []):
            with self.subTest(options=options):
                self.options.write_text(json.dumps(options))
                with self.assertRaisesRegex(ValueError, "libmpv GPL"):
                    self.run_audit()

    def test_extra_gpl_postproc_library_is_rejected(self):
        (self.libraries / "libpostproc.so").write_bytes(elf())
        with self.assertRaisesRegex(ValueError, "postproc"):
            self.run_audit()

    def test_missing_player_is_rejected(self):
        (self.libraries / "libplayer.so").unlink()
        with self.assertRaisesRegex(ValueError, "libplayer"):
            self.run_audit()

    def test_4kb_library_is_rejected(self):
        (self.libraries / "libplayer.so").write_bytes(elf(alignment=4096))
        with self.assertRaisesRegex(ValueError, "Unaligned PT_LOAD"):
            self.run_audit()

    def test_incongruent_load_address_is_rejected(self):
        (self.libraries / "libplayer.so").write_bytes(elf(address=4096))
        with self.assertRaisesRegex(ValueError, "Unaligned PT_LOAD"):
            self.run_audit()

    def test_other_architecture_is_rejected(self):
        (self.libraries / "libplayer.so").write_bytes(elf(machine=62))
        with self.assertRaisesRegex(ValueError, "AArch64"):
            self.run_audit()


if __name__ == "__main__":
    unittest.main()
