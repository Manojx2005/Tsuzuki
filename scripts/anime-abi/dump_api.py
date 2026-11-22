"""Dump the public/protected API of an extensions-lib stub jar (AY6).

usage: python dump_api.py <classes.jar> <out.txt>

One line per member: `<class>#<name> <descriptor>` (constructors are `<init>`), plus `<class>` alone for each class,
sorted. Uses the JDK's `javap` (set JAVA_HOME or have it on PATH); reads the jar only, runs nothing from it.
"""
import os
import re
import subprocess
import sys
import zipfile

jar, out = sys.argv[1:3]
javap = os.path.join(os.environ.get("JAVA_HOME", ""), "bin", "javap") if os.environ.get("JAVA_HOME") else "javap"

with zipfile.ZipFile(jar) as z:
    classes = sorted(
        n[:-6].replace("/", ".") for n in z.namelist()
        if n.endswith(".class") and not n.startswith("META-INF") and "module-info" not in n
    )

lines = set()
header = re.compile(r"^(?:public|protected)?\s*.*?(?:class|interface|enum)\s+([\w.$]+)")
member = re.compile(r"^\s+(?:public|protected)\b.*?([\w$<>-]+)\s*(?:\(.*\))?(?:\s+throws [\w.$, ]+)?;$")
# Compiler-generated: coroutine/lambda classes (`Foo$bar$2`, `$1$1`), inline copies (`$$inlined$`), and
# `$annotations` / `$$forInline` members. Extensions never link against these.
synthetic_class = re.compile(r"\$(?:\d|\w*\$)")
synthetic_member = ("$annotations", "$$forInline")
for start in range(0, len(classes), 200):
    batch = classes[start:start + 200]
    text = subprocess.run(
        [javap, "-protected", "-s", "-cp", jar, *batch], capture_output=True, text=True, check=True,
    ).stdout
    current = None
    pending = None
    for raw in text.splitlines():
        if raw.startswith("Compiled from"):
            continue
        h = header.match(raw)
        if h and not raw.startswith(" "):
            current = h.group(1)
            if synthetic_class.search(current.rsplit(".", 1)[-1]):
                current = None
                continue
            lines.add(current)
            pending = None
            continue
        if current is None:
            continue
        stripped = raw.strip()
        if stripped.startswith("descriptor:") and pending:
            if pending.endswith(synthetic_member):
                pending = None
                continue
            lines.add(f"{current}#{pending} {stripped.split(':', 1)[1].strip()}")
            pending = None
            continue
        m = member.match(raw)
        if m:
            name = m.group(1)
            # Constructors print as the class name.
            pending = "<init>" if name == current or name == current.rsplit(".", 1)[-1] else name
        elif stripped.startswith("static {}"):
            pending = None

with open(out, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(sorted(lines)) + "\n")
print(f"{len(classes)} classes, {len(lines)} lines -> {out}")
