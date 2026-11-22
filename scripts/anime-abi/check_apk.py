"""AY6 minified check: every expected extensions-lib class and member survives R8 in an APK, by name, exact
descriptor and visibility.

usage: python check_apk.py <app.apk> [expected-lib14.txt ...]   (default: every expected-lib*.txt next to this file)

Reads the APK's dex files with build-tools `dexdump` (newest found under the Android SDK) and compares
`class#member descriptor` lines. Lines listed in `not-offered.txt` (same format, prefix match) and `android.*` are
skipped. Exits 1 and prints every miss (with the reason) when anything is missing.

Strict rules (AY6-F):
- A member counts only if it is PUBLIC or PROTECTED in the dex (an extension can't link to anything else), and a
  class-only line needs a public class.
- Inherited members are followed through supertypes *inside the APK* only. A lookup that would have to continue
  into a class outside the APK (framework, JDK) fails, because the APK can't prove it. The single exception is the
  three `java.lang.Object` methods every class has (JVM spec). There is no other external allowlist, and lib 14,
  16 and 17 need none (see README).
- Every run first checks three controls on `Video` and exits 2 if the checker itself misjudges one: a known public
  getter must be accepted, a known private field and a nonexistent method must both be rejected.
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

classes = {}  # class -> True when the class itself is public
visible = set()  # "class#name descriptor" for PUBLIC/PROTECTED members
hidden = set()  # the same for private / package-private members (diagnostics only)
supers = {}  # class -> superclass and interfaces, to resolve inherited members as the VM does


def visible_flags(text):
    return "PUBLIC" in text or "PROTECTED" in text


with tempfile.TemporaryDirectory() as tmp, zipfile.ZipFile(apk) as z:
    dex_names = [n for n in z.namelist() if re.fullmatch(r"classes\d*\.dex", n)]
    if not dex_names:
        sys.exit(f"no classes*.dex in {apk}")
    for name in dex_names:
        path = z.extract(name, tmp)
        out = subprocess.run([dexdump, path], capture_output=True, text=True, errors="replace", check=True).stdout
        current = None
        in_header = False
        member = None
        for line in out.splitlines():
            line = line.strip()
            m = re.match(r"Class descriptor\s*:\s*'L(.+);'", line)
            if m:
                current = m.group(1).replace("/", ".")
                classes[current] = False
                supers.setdefault(current, [])
                in_header = True
                member = None
                continue
            if current is None:
                continue
            if in_header:
                m = re.match(r"Access flags\s*:\s*0x[0-9a-fA-F]+\s*\((.*)\)", line)
                if m:
                    classes[current] = visible_flags(m.group(1))
                    continue
                m = re.match(r"(?:Superclass\s*:|#\d+\s*:)\s*'L(.+);'", line)
                if m:
                    supers[current].append(m.group(1).replace("/", "."))
                    continue
            if re.match(r"(Static fields|Instance fields|Direct methods|Virtual methods)\s*-", line):
                in_header = False
                continue
            m = re.match(r"name\s*:\s*'(.+)'", line)
            if m:
                member = [m.group(1), None]
                continue
            m = re.match(r"type\s*:\s*'(.+)'", line)
            if m and member:
                member[1] = m.group(1)
                continue
            m = re.match(r"access\s*:\s*0x[0-9a-fA-F]+\s*\((.*)\)", line)
            if m and member and member[1] is not None:
                key = f"{current}#{member[0]} {member[1]}"
                (visible if visible_flags(m.group(1)) else hidden).add(key)
                member = None

OBJECT_METHODS = {"equals (Ljava/lang/Object;)Z", "hashCode ()I", "toString ()Ljava/lang/String;"}


def check(line):
    """None when the line is satisfied, otherwise the reason it isn't."""
    if "#" not in line:
        if line not in classes:
            return "class missing"
        return None if classes[line] else "class not public"
    cls, signature = line.split("#", 1)
    if cls not in classes:
        return "class missing"
    if f"{cls}#{signature}" in visible or signature in OBJECT_METHODS:
        return None
    if f"{cls}#{signature}" in hidden:
        return "present but not public/protected"
    if signature.startswith("<init>"):
        return "constructor missing"
    seen, queue, outside = set(), list(supers.get(cls, [])), []
    while queue:
        parent = queue.pop()
        if parent in seen:
            continue
        seen.add(parent)
        if f"{parent}#{signature}" in visible:
            return None
        if parent not in classes:
            outside.append(parent)
            continue
        queue.extend(supers.get(parent, []))
    if outside:
        return "not in the APK; can't prove it through " + ", ".join(sorted(outside))
    return "member missing"


VIDEO = "eu.kanade.tachiyomi.animesource.model.Video"
controls = [
    (f"{VIDEO}#getInitialized ()Z", True),  # known public getter
    (f"{VIDEO}#initialized Z", False),  # known PRIVATE FINAL field
    (f"{VIDEO}#reviewMissingMember ()V", False),  # nonexistent
]
bad = [(line, want) for line, want in controls if (check(line) is None) != want]
if bad:
    for line, want in bad:
        print(f"CONTROL FAILED: {line} should be {'accepted' if want else 'rejected'} ({check(line)})")
    sys.exit(2)
print("controls OK: public getter accepted; private field and nonexistent method rejected")

misses = []
for path in expected_files:
    for line in open(path, encoding="utf-8"):
        line = line.strip()
        if not line or line.startswith("android."):
            continue
        if any(line == s or line.startswith(s + "#") or line.startswith(s + ".") for s in skip):
            continue
        why = check(line)
        if why:
            misses.append(f"{os.path.basename(path)}: {line}  [{why}]")

if misses:
    print(f"{len(misses)} expected API lines missing from {os.path.basename(apk)}:")
    print("\n".join(misses))
    sys.exit(1)
print(f"OK: every expected API line is in {os.path.basename(apk)} ({len(classes)} classes, {len(visible)} visible members)")
