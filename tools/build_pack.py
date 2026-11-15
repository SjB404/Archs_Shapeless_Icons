#!/usr/bin/env python3
"""
Turns the source icons into the files launchers read.

Source (what gets edited):
  icons/png/<name>.png      one image per icon, transparent background, square
  icons/appfilter.json      which app(s) each icon belongs to:
                            { "soundcloud": ["ComponentInfo{com.soundcloud.android/com.soundcloud.android.main.MainActivity}"] }

Output (generated, don't edit by hand):
  app/src/main/res/drawable-nodpi/<name>.png
  app/src/main/res/xml/appfilter.xml   + app/src/main/assets/appfilter.xml
  app/src/main/res/xml/drawable.xml    + app/src/main/assets/drawable.xml
"""
import json
import re
import shutil
import sys
from pathlib import Path
from xml.sax.saxutils import quoteattr

ROOT = Path(__file__).resolve().parent.parent
SRC_PNG = ROOT / "icons" / "png"
MAP = ROOT / "icons" / "appfilter.json"
RES = ROOT / "app" / "src" / "main" / "res"
ASSETS = ROOT / "app" / "src" / "main" / "assets"
OUT_PNG = RES / "drawable-nodpi"

NAME_OK = re.compile(r"^[a-z][a-z0-9_]*$")
COMP_OK = re.compile(r"^ComponentInfo\{[^/{}]+/[^/{}]+\}$")


def main() -> int:
    mapping = json.loads(MAP.read_text()) if MAP.exists() else {}
    errors = []

    for name, comps in mapping.items():
        if not NAME_OK.match(name):
            errors.append(f"bad icon name '{name}' (lowercase letters, digits, _ only; start with a letter)")
        if not (SRC_PNG / f"{name}.png").exists():
            errors.append(f"'{name}' is mapped but icons/png/{name}.png is missing")
        for c in comps:
            if not COMP_OK.match(c):
                errors.append(f"bad component for '{name}': {c}")
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1

    # Fresh copy of the images.
    if OUT_PNG.exists():
        shutil.rmtree(OUT_PNG)
    OUT_PNG.mkdir(parents=True)
    for name in mapping:
        shutil.copy(SRC_PNG / f"{name}.png", OUT_PNG / f"{name}.png")

    appfilter = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for name in sorted(mapping):
        for c in mapping[name]:
            appfilter.append(f"    <item component={quoteattr(c)} drawable={quoteattr(name)} />")
    appfilter.append("</resources>")

    drawable = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>", "    <version>1</version>",
                '    <category title="All" />']
    for name in sorted(mapping):
        drawable.append(f"    <item drawable={quoteattr(name)} />")
    drawable.append("</resources>")

    for folder in (RES / "xml", ASSETS):
        folder.mkdir(parents=True, exist_ok=True)
        (folder / "appfilter.xml").write_text("\n".join(appfilter) + "\n")
        (folder / "drawable.xml").write_text("\n".join(drawable) + "\n")

    print(f"Built pack: {len(mapping)} icons, {sum(len(v) for v in mapping.values())} app links")
    return 0


if __name__ == "__main__":
    sys.exit(main())
