"""Merge ART profile text files (baseline-prof.txt / startup-prof.txt) without losing rules.

Usage: python -I merge-profiles.py <existing.txt> <collected.txt>... -o <out.txt>

A rule is optional flags (H, S, P) followed by a descriptor (`Lpkg/Class;` or `Lpkg/Class;->method(...)R`).
Identical descriptors keep the union of their flags; class rules have no flags. Output is sorted for stable diffs
and reports how many rules (and how many Tsuzuki rules) were added.
"""
import argparse
import re

RULE = re.compile(r"^([HSP]*)(L.+)$")


def read(path, rules):
    with open(path, encoding="utf-8") as handle:
        for raw in handle:
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            match = RULE.match(line)
            if not match:
                continue
            flags, descriptor = match.groups()
            rules.setdefault(descriptor, set()).update(flags)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("existing")
    parser.add_argument("collected", nargs="+")
    parser.add_argument("-o", "--out", required=True)
    args = parser.parse_args()

    rules = {}
    read(args.existing, rules)
    before = dict((d, set(f)) for d, f in rules.items())
    for path in args.collected:
        read(path, rules)

    order = "HSP"
    with open(args.out, "w", encoding="utf-8", newline="\n") as out:
        for descriptor in sorted(rules):
            flags = "".join(f for f in order if f in rules[descriptor])
            out.write(f"{flags}{descriptor}\n")

    added = [d for d in rules if d not in before]
    widened = [d for d in before if rules[d] != before[d]]
    tsuzuki = sum(1 for d in rules if "tsuzuki" in d)
    print(f"rules {len(before)} -> {len(rules)} (+{len(added)} new, {len(widened)} widened); "
          f"tsuzuki rules now {tsuzuki} (+{sum(1 for d in added if 'tsuzuki' in d)})")


main()
