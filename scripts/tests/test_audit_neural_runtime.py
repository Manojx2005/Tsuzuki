"""Fail-closed artifact gate checks, including the deliberate Speaker substring rule."""
import importlib.util
import struct
import tempfile
import unittest
import warnings
import zipfile
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("audit", Path(__file__).parents[1] / "audit-neural-runtime.py")
audit = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(audit)


def elf(bits=64, alignment=16384, payload=b"", virtual=0):
    header_size, ph_size = (64, 56) if bits == 64 else (52, 32)
    data = bytearray(header_size + ph_size + len(payload))
    data[:7] = b"\x7fELF" + bytes((2 if bits == 64 else 1, 1, 1))
    if bits == 64:
        struct.pack_into("<Q", data, 32, header_size)
        struct.pack_into("<HH", data, 54, ph_size, 1)
        struct.pack_into("<IIQQQQQQ", data, header_size, 1, 5, 0, virtual, 0, len(data), len(data), alignment)
    else:
        struct.pack_into("<I", data, 28, header_size)
        struct.pack_into("<HH", data, 42, ph_size, 1)
        struct.pack_into("<IIIIIIII", data, header_size, 1, 0, virtual, 0, len(data), len(data), 5, alignment)
    data[header_size + ph_size:] = payload
    return bytes(data)


class RuntimeAuditTest(unittest.TestCase):
    def test_both_android_elf_classes_pass_real_load_alignment(self):
        for bits in (32, 64):
            with self.subTest(bits=bits):
                self.assertTrue(audit.inspect_library("jni.so", elf(bits))["passes_nt1_artifact_gates"])

    def test_four_kilobyte_load_segments_fail(self):
        self.assertFalse(audit.inspect_library("jni.so", elf(alignment=4096))["aligned_16kb"])

    def test_incongruent_segment_virtual_address_fails(self):
        self.assertFalse(audit.inspect_library("jni.so", elf(virtual=1))["aligned_16kb"])

    def test_speaker_false_positive_still_fails_literal_gate(self):
        report = audit.inspect_library("jni.so", elf(payload=b"CreateSpeakerEmbeddingExtractor\0"))
        self.assertFalse(report["passes_nt1_artifact_gates"])
        self.assertEqual([], report["runtime_markers"])
        self.assertEqual(["CreateSpeakerEmbeddingExtractor"], report["literal_grep_matches"])

    def test_runtime_marker_is_reported_and_fails(self):
        report = audit.inspect_library("jni.so", elf(payload=b"espeak_Initialize\0"))
        self.assertFalse(report["passes_nt1_artifact_gates"])
        self.assertEqual(["espeak_Initialize"], report["runtime_markers"])

    def test_utf16_literals_fail_in_both_byte_orders_and_alignments(self):
        for encoding in ("utf-16-le", "utf-16-be"):
            for offset in (b"", b"\xff"):
                for text in ("CreateSpeakerEmbeddingExtractor", "PiPeR"):
                    with self.subTest(encoding=encoding, offset=offset, text=text):
                        report = audit.inspect_library("jni.so", elf(payload=offset + text.encode(encoding)))
                        self.assertFalse(report["passes_nt1_artifact_gates"])
                        self.assertEqual([], report["literal_grep_matches"])
                        self.assertTrue(any(text in match for match in report["utf16_literal_matches"]))

    def test_unrelated_utf16_text_does_not_fail(self):
        for encoding in ("utf-16-le", "utf-16-be"):
            report = audit.inspect_library("jni.so", elf(payload="Kokoro text runtime".encode(encoding)))
            self.assertTrue(report["passes_nt1_artifact_gates"])

    def test_build_paths_fail_hygiene_in_ascii_and_utf16(self):
        for encoding in ("ascii", "utf-16-le", "utf-16-be"):
            for text in ("C:/Users/test/build/runtime.cc", r"C:\Users\test\build\runtime.cc",
                         "C:/runtime.cc", "/home/runner/work/runtime/core.h"):
                with self.subTest(encoding=encoding, path=text):
                    report = audit.inspect_library("jni.so", elf(payload=text.encode(encoding)))
                    self.assertFalse(report["passes_build_hygiene"])
                    self.assertTrue(report["passes_nt1_artifact_gates"])

    def test_urls_relative_diagnostics_and_android_paths_are_not_build_paths(self):
        for text in ("https://github.com/project/src/core.cc", "tsuzuki/.native/src/core.cc",
                     "/data/user/0/app/files/models", "/proc/self/maps", "R:/6"):
            with self.subTest(text=text):
                self.assertTrue(audit.inspect_library("jni.so", elf(payload=text.encode()))["passes_build_hygiene"])

    def test_bad_or_truncated_native_files_fail_closed(self):
        for data in (b"", b"not ELF", elf()[:80], elf()[:52]):
            with self.subTest(size=len(data)):
                with self.assertRaises(ValueError):
                    audit.inspect_elf(data)

    def test_empty_aar_cannot_pass(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "empty.aar"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("AndroidManifest.xml", "<manifest/>")
            with self.assertRaises(ValueError):
                audit.inspect_artifact(path)

    def test_invalid_non_power_of_two_alignment_fails(self):
        self.assertFalse(audit.inspect_library("jni.so", elf(alignment=49152))["aligned_16kb"])

    def test_duplicate_native_zip_entries_fail_closed(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "duplicate.aar"
            with warnings.catch_warnings():
                warnings.simplefilter("ignore", UserWarning)
                with zipfile.ZipFile(path, "w") as archive:
                    archive.writestr("jni/arm64-v8a/jni.so", elf(payload=b"espeak_Initialize\0"))
                    archive.writestr("jni/arm64-v8a/jni.so", elf())
            with self.assertRaises(ValueError):
                audit.inspect_artifact(path)

    def test_every_abi_is_checked(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "runtime.aar"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("jni/arm64-v8a/jni.so", elf())
                archive.writestr("jni/x86/jni.so", elf(32, payload=b"PIPER\0"))
            report = audit.inspect_artifact(path)
            self.assertEqual(2, len(report["libraries"]))
            self.assertFalse(report["passes_nt1_artifact_gates"])


if __name__ == "__main__":
    unittest.main()
