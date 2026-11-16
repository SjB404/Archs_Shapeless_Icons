#!/usr/bin/env python3
"""
Renders every icon source in icons/src/ into icons/png/<name>.png (the images the pack ships).
Apps without a source keep their existing png (e.g. a first draft from convert_export.py).

    python3 tools/build_icons.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from icon_render import ROOT, render, source_for  # noqa: E402

PNG = ROOT / "icons" / "png"


def main():
    mapping = json.loads((ROOT / "icons" / "appfilter.json").read_text())
    drawn, kept, missing = 0, 0, []
    for name in mapping:
        path, _ = source_for(name)
        if path is not None:
            img = render(name)
            if img is None:
                print(f"EMPTY drawing: {path}")
                continue
            img.save(PNG / f"{name}.png", optimize=True)
            drawn += 1
        elif (PNG / f"{name}.png").exists():
            kept += 1
        else:
            missing.append(name)
    print(f"{drawn} icons rendered from icons/src, {kept} kept as they were")
    if missing:
        print("No image at all for:", ", ".join(missing))
        sys.exit(1)


if __name__ == "__main__":
    main()
