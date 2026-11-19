#!/usr/bin/env python3
"""
Contact sheets of the whole pack, rendered exactly like the build, with app names.
Each app shows twice: on Arch's dark grey app drawer and on a sample blue-grey wallpaper (his real wallpaper changes).

    python3 tools/review_sheets.py [out_prefix]   -> <out_prefix>_1.png, _2.png, ...
"""
import csv
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from icon_render import ROOT, render  # noqa: E402

DARK, BLUE = (47, 47, 47), (79, 125, 149)
APPS = Path("/home/claude/work/apps.tsv")


def main():
    prefix = sys.argv[1] if len(sys.argv) > 1 else "/home/claude/work/review_sheet"
    rows = list(csv.DictReader(open(APPS), delimiter="\t"))
    per_row, rows_per_sheet, t = 6, 8, 96
    per_sheet = per_row * rows_per_sheet
    outs = []
    for s in range(0, len(rows), per_sheet):
        chunk = rows[s:s + per_sheet]
        W = per_row * (2 * t + 12) + 8
        H = ((len(chunk) + per_row - 1) // per_row) * (t + 18) + 8
        img = Image.new("RGB", (W, H), (25, 25, 25))
        d = ImageDraw.Draw(img)
        for k, r in enumerate(chunk):
            x = 8 + (k % per_row) * (2 * t + 12)
            y = 8 + (k // per_row) * (t + 18)
            ic = render(r["drawable"])
            if ic is None:
                p = ROOT / "icons" / "png" / f"{r['drawable']}.png"
                ic = Image.open(p).convert("RGBA") if p.exists() else None
            for j, bg in enumerate((DARK, BLUE)):
                tile = Image.new("RGBA", (t, t), bg + (255,))
                if ic is not None:
                    im = ic.convert("RGBa").resize((t - 12, t - 12), Image.LANCZOS).convert("RGBA")
                    tile.alpha_composite(im, (6, 6))
                img.paste(tile, (x + j * t, y))
            d.text((x, y + t + 3), f"{r['idx']} {r['label'][:28]}", fill=(230, 230, 230))
        out = f"{prefix}_{s // per_sheet + 1}.png"
        img.save(out)
        outs.append(out)
    print("\n".join(outs))


if __name__ == "__main__":
    main()
