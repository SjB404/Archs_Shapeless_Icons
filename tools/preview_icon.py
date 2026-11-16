#!/usr/bin/env python3
"""
Shows icons the way they'll look on Arch's phone, next to the originals, so they can be checked.

    python3 tools/preview_icon.py <name>                 -> /home/claude/work/preview/<name>.png
    python3 tools/preview_icon.py --sheet out.png <name> <name> ...   -> one row per icon

<name> is the drawable name (e.g. com_soundcloud_android). The icon is rendered from icons/src/
exactly like the build does it. Single-icon view: big and phone-size, on Arch's blue-grey wallpaper
and dark grey app drawer, plus the Play Store icon, the version 4 icon and the exported original.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from icon_render import ROOT, render  # noqa: E402

BLUE = (79, 125, 149)      # Arch's home screen wallpaper (approx.)
DARK = (47, 47, 47)        # Arch's app drawer
WORK = Path("/home/claude/work")
REFS = Path("/home/claude/refs")


def tile(img, size, bg):
    t = Image.new("RGBA", (size, size), bg + (255,))
    if img is not None:
        im = img.convert("RGBA").convert("RGBa").resize((size, size), Image.LANCZOS).convert("RGBA")
        t.alpha_composite(im)
    return t


def export_full(name):
    import csv
    for r in csv.DictReader(open(WORK / "apps.tsv"), delimiter="\t"):
        if r["drawable"] == name:
            p = Path(r["export_folder"]) / "full.png"
            if not p.exists():
                p = Path(r["export_folder"]) / "legacy.png"
            return Image.open(p) if p.exists() else None
    return None


def load(p):
    p = Path(p)
    return Image.open(p).convert("RGBA") if p.exists() else None


def single(name):
    new = render(name)
    note = "" if new is not None else "  (no source in icons/src yet: showing current png)"
    if new is None:
        new = load(ROOT / "icons" / "png" / f"{name}.png")
    W, H = 1180, 600
    c = Image.new("RGBA", (W, H), (120, 120, 120, 255))
    d = ImageDraw.Draw(c)
    d.text((10, 6), f"{name}{note}", fill="white")
    c.alpha_composite(tile(new, 256, BLUE), (10, 24))
    c.alpha_composite(tile(new, 256, DARK), (276, 24))
    d.text((10, 284), "new: wallpaper / drawer (big)", fill="white")
    x = 10
    for bg in (BLUE, DARK):
        for s in (128, 72, 48):
            c.alpha_composite(tile(new, s, bg), (x, 300 + (128 - s)))
            x += s + 6
        x += 20
    d.text((10, 432), "phone sizes (48 = real size on the phone)", fill="white")
    refs = [("play store", load(REFS / name / "play.png")),
            ("version 4", load(WORK / "v4_png" / f"{name}.png")),
            ("exported original", export_full(name))]
    x = 560
    for label, im in refs:
        c.alpha_composite(tile(im, 190, DARK), (x, 24))
        d.text((x, 218), label + ("" if im is not None else " (none)"), fill="white")
        x += 200
    out = WORK / "preview" / f"{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    c.save(out)
    print(out)


def sheet(out, names):
    cell = 140
    W, H = cell * 4 + 300, (cell + 10) * len(names) + 30
    c = Image.new("RGBA", (W, H), (120, 120, 120, 255))
    d = ImageDraw.Draw(c)
    d.text((10, 8), "new on drawer | new on wallpaper | play store | version 4", fill="white")
    for i, n in enumerate(names):
        y = 28 + i * (cell + 10)
        new = render(n) or load(ROOT / "icons" / "png" / f"{n}.png")
        ims = [(new, DARK), (new, BLUE), (load(REFS / n / "play.png"), DARK), (load(WORK / "v4_png" / f"{n}.png"), DARK)]
        for j, (im, bg) in enumerate(ims):
            c.alpha_composite(tile(im, cell - 10, bg), (10 + j * cell, y))
        d.text((10 + 4 * cell, y + cell // 2 - 6), n, fill="white")
    c.save(out)
    print(out)


if __name__ == "__main__":
    args = sys.argv[1:]
    if args and args[0] == "--sheet":
        sheet(args[1], args[2:])
    else:
        for n in args:
            single(n)
