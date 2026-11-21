"""AY6 minified check: every expected extensions-lib class and member survives R8 in an APK with its original name.

usage: python check_apk.py <app.apk> [expected-lib14.txt ...]   (default: every expected-lib*.txt next to this file)

Reads the APK's dex files with build-tools `dexdump` (newest found under the Android SDK) and compares
`class#member descriptor` lines. Lines listed in `not-offered.txt` (same format, prefix match) and `android.*` are
skipped. Exits 1 and prints every miss when anything is missing.
"""
import glob
import os
import re
import subprocess
import sys
import tempfile
import zipfile

here = os.path.dirname(os.path.abspath(__file__))
apk = sys.argv[1]
expected_files = sys.argv[2:] or sorted(glob.glob(os.path.join(here, "expected-lib*.txt")))
sdk = os.environ.get("ANDROID_HOME") or os.path.join(os.environ["LOCALAPPDATA"], "Android", "Sdk")
dexdumps = sorted(glob.glob(os.path.join(sdk, "build-tools", "*", "dexdump*")))
if not dexdumps:
    sys.exit("dexdump not found under " + sdk)
dexdump = dexdumps[-1]

skip = []
not_offered = os.path.join(here, "not-offered.txt")
if os.path.exists(not_offered):
    skip = [l.split("\t")[0].strip() for l in open(not_offered, encoding="utf-8") if l.strip() and not l.startswith("#")]

present = set()
supers = {}  # class -> its superclass and interfaces, to resolve inherited members as the VM does
with tempfile.TemporaryDirectory() as tmp, zipfile.ZipFile(apk) as z:
    for name in z.namelist():
        if not re.fullmatch(r"classes\d*\.dex", name):
            continue
        path = z.extract(name, tmp)
        out = subprocess.run([dexdump, path], capture_output=True, text=True, errors="replace").stdout
        current = None
        member = None
        for line in out.splitlines():
            line = line.strip()
            m = re.match(r"Class descriptor\s*:\s*'L(.+);'", line)
            if m:
                current = m.group(1).replace("/", ".")
                present.add(current)
                supers.setdefault(current, [])
                continue
            m = re.match(r"(?:Superclass\s*:|#\d+\s*:)\s*'L(.+);'", line)
            if m and current:
                supers[current].append(m.group(1).replace("/", "."))
                continue
            m = re.match(r"name\s*:\s*'(.+)'", line)
            if m and current:
                member = m.group(1)
                continue
            m = re.match(r"type\s*:\s*'(.+)'", line)
            if m and current and member:
                present.add(f"{current}#{member} {m.group(1)}")
                member = None

OBJECT_METHODS = {"equals (Ljava/lang/Object;)Z", "hashCode ()I", "toString ()Ljava/lang/String;"}


def has(line):
    if line in present:
        return True
    if "#" not in line:
        return False
    cls, signature = line.split("#", 1)
    if signature in OBJECT_METHODS:
        return cls in present
    if signature.startswith("<init>"):
        return False
    seen, queue = set(), list(supers.get(cls, []))
    while queue:
        parent = queue.pop()
        if parent in seen:
            continue
        seen.add(parent)
        if f"{parent}#{signature}" in present:
            return True
        queue.extend(supers.get(parent, []))
    # Library classes outside the APK (framework, androidx kept elsewhere) can't be followed further.
    return any(p not in supers for p in seen)


misses = []
for path in expected_files:
    for line in open(path, encoding="utf-8"):
        line = line.strip()
        if not line or line.startswith("android."):
            continue
        if any(line == s or line.startswith(s + "#") or line.startswith(s + ".") for s in skip):
            continue
        if not has(line):
            misses.append(f"{os.path.basename(path)}: {line}")

if misses:
    print(f"{len(misses)} expected API lines missing from {os.path.basename(apk)}:")
    print("\n".join(misses))
    sys.exit(1)
print(f"OK: every expected API line is in {os.path.basename(apk)} ({len(present)} dex entries read)")
