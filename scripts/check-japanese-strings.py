#!/usr/bin/env python3
"""Check Japanese coverage and Android formatting for both string resource modules."""

import re
from collections import Counter
from pathlib import Path
from xml.etree import ElementTree

REPO = Path(__file__).resolve().parents[1]
FORMAT_TOKEN = re.compile(r"%(?:%|(?:\d+\$)?[-#+0,(<]*\d*(?:\.\d+)?[a-zA-Z])|\{[a-zA-Z_]\w*\}")


def strings(path):
    return ElementTree.parse(path).getroot().findall("string")


def formatting(element):
    tokens = FORMAT_TOKEN.findall("".join(element.itertext()))
    # Indexed arguments and brace templates can move to suit Japanese word order.
    unindexed = [token for token in tokens if token.startswith("%") and token != "%%" and "$" not in token]
    return Counter(tokens), unindexed


def check(module):
    root = REPO / module / "src/commonMain/moko-resources"
    # The source files have inherited duplicate definitions. Use the final value for comparison.
    base = {element.attrib["name"]: element for element in strings(root / "base/strings.xml")}
    japanese = {}
    problems = []
    for element in strings(root / "ja/strings.xml"):
        key = element.attrib["name"]
        if key in japanese:
            problems.append(f"{key}: duplicate Japanese definition")
        japanese[key] = element

    translatable = {key for key, source in base.items() if source.get("translatable") != "false"}
    for key in sorted(translatable):
        source = base[key]
        translated = japanese.get(key)
        if translated is None:
            problems.append(f"{key}: missing Japanese string")
        elif formatting(source) != formatting(translated):
            problems.append(f"{key}: placeholder mismatch")
        elif [node.tag for node in source.iter()] != [node.tag for node in translated.iter()]:
            problems.append(f"{key}: markup mismatch")

    for problem in problems:
        print(f"{module}: {problem}")
    if not problems:
        print(f"{module}: {len(translatable)} translatable strings covered with matching placeholders and markup")
    return not problems


if __name__ == "__main__":
    results = [check(module) for module in ("i18n", "i18n-novel")]
    raise SystemExit(0 if all(results) else 1)
