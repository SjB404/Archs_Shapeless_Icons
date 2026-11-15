#!/usr/bin/env python3
"""
Automatic first pass: turns an export zip (from the app) into shapeless icons.

    python3 tools/convert_export.py <unzipped export dir> <out dir> [overrides.json]

For each app it makes <out>/<drawable_name>.png plus <out>/map.json
({drawable_name: [component, ...]}).

How an icon is made:
  * Normal case: take the logo layer (fg) and drop the background layer.
  * If the background is a strong colour and the logo is white (or one flat dark colour),
    the white parts get the background's colour instead, so the brand colour survives.
  * If the logo is inside the background layer, or the logo layer is one full picture,
    or the app has an old-style icon: remove the plain colour around the edges instead.
  * Long fake shadows are removed, then the logo is cropped and enlarged.
"""
import json
import re
import sys
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image

S = 432          # working size (one icon layer, 108dp at 4x)
OUT = 256        # final png size
yy, xx = np.mgrid[0:S, 0:S]
R = np.hypot(xx - S / 2 + .5, yy - S / 2 + .5)
SAFE = R < S * 33 / 108
RING = (R > S * 30 / 108) & (R < S * 35 / 108)   # just inside the visible circle


def load(p, size=S):
    im = Image.open(p).convert("RGBA")
    if im.size != (size, size):
        im = im.resize((size, size), Image.LANCZOS)
    return np.asarray(im).astype(np.float32) / 255


def over(top, bot):
    a = top[..., 3:4] + bot[..., 3:4] * (1 - top[..., 3:4])
    rgb = (top[..., :3] * top[..., 3:4] + bot[..., :3] * bot[..., 3:4] * (1 - top[..., 3:4])) / np.maximum(a, 1e-6)
    return np.concatenate([rgb, a], -1)


def sat(rgb):
    return rgb.max(-1) - rgb.min(-1)


def lum(rgb):
    return rgb @ np.array([.299, .587, .114], np.float32)


def strong_color(c):
    """Is this background colour worth keeping as the logo colour?"""
    c = np.asarray(c)
    return sat(c) > 0.2 and 0.12 < lum(c) < 0.93


def remove_shadow(img, limit=0.35):
    # long fake shadows are big areas of nearly see-through pixels
    a = img[..., 3]
    faint = (a > 0.02) & (a < limit)
    if faint.sum() > 0.08 * (a > 0.02).sum():
        img = img.copy()
        img[..., 3] = np.where(a < limit, 0, a)
    return img


def flood_remove(img, color=None, tol=0.16, soft=0.10, legacy=False):
    """Make the plain colour (and transparency) touching the edges transparent."""
    rgb, a = img[..., :3], img[..., 3]
    if color is None:
        ring = rgb[RING & (a > 0.9)]
        if legacy or len(ring) < 50:
            # legacy icon: the shape's colour is the most common opaque colour
            op = rgb[a > 0.9]
            if len(op) == 0:
                return img, None
            q = (op * 15).round().astype(int)
            keys, counts = np.unique(q[:, 0] * 256 + q[:, 1] * 16 + q[:, 2], return_counts=True)
            k = keys[counts.argmax()]
            sel = (q[:, 0] * 256 + q[:, 1] * 16 + q[:, 2]) == k
            color = op[sel].mean(0)
        else:
            if ring.std(0).mean() > 0.06:
                return img, None   # edge isn't one plain colour: can't do it safely
            color = ring.mean(0)
    d = np.abs(rgb - color).max(-1)
    passable = (a < (0.6 if legacy else 0.1)) | (d < tol)   # old icons have soft drop shadows
    seen = np.zeros(a.shape, bool)
    q = deque()
    for x in range(S):
        for y in (0, S - 1):
            for p in ((y, x), (x, y)):
                if passable[p] and not seen[p]:
                    seen[p] = True
                    q.append(p)
    while q:
        y, x = q.popleft()
        for ny, nx in ((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)):
            if 0 <= ny < S and 0 <= nx < S and not seen[ny, nx] and passable[ny, nx]:
                seen[ny, nx] = True
                q.append((ny, nx))
    # soften the cut edge: pixels near the removed area fade by how close they are to the colour
    near = seen.copy()
    for _ in range(3):
        near = near | np.roll(near, 1, 0) | np.roll(near, -1, 0) | np.roll(near, 1, 1) | np.roll(near, -1, 1)
    k = np.clip((d - tol * 0.5) / soft, 0, 1)
    na = np.where(seen, 0, np.where(near, a * k, a))
    # un-mix the old background colour out of the softened edge
    keep = np.clip(na / np.maximum(a, 1e-6), 1e-3, 1)[..., None]
    nrgb = np.where(near[..., None] & ~seen[..., None], (rgb - (1 - keep) * color) / keep, rgb)
    out = np.concatenate([np.clip(nrgb, 0, 1), na[..., None]], -1)
    return out, color


def recolor(img, bgimg, mode):
    """mode 'white': white parts take the background colour. 'all': whole logo does."""
    rgb, a = img[..., :3], img[..., 3:4]
    if mode == "all":
        return np.concatenate([bgimg, a], -1)
    w = np.clip((rgb.min(-1, keepdims=True) - 0.70) / 0.2, 0, 1) * np.clip(1 - sat(rgb)[..., None] / 0.15, 0, 1)
    return np.concatenate([rgb * (1 - w) + bgimg * w, a], -1)


def hexrgb(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)], np.float32)


def touch_up(img, ov):
    """Hand-fix options from overrides.json."""
    img = img.copy()
    rgb, a = img[..., :3], img[..., 3]
    if ov.get("drop_white"):
        w = np.clip((rgb.min(-1) - 0.80) / 0.12, 0, 1) * (sat(rgb) < 0.12)
        img[..., 3] = a * (1 - w)
    if "dark_to" in ov:
        k = np.clip((0.35 - lum(rgb)) / 0.15, 0, 1)[..., None]
        img[..., :3] = rgb * (1 - k) + hexrgb(ov["dark_to"]) * k
    if "color" in ov:
        img[..., :3] = hexrgb(ov["color"])
    return img


def logo_stats(img):
    op = img[..., 3] > 0.5
    if op.sum() == 0:
        return 0, 0
    px = img[..., :3][op]
    white = float(((px.min(-1) > 0.86) & (sat(px) < 0.12)).mean())
    dark = float((lum(px) < 0.2).mean())
    return white, dark


def fit(img, fill=1.0, area=None):
    a = img[..., 3]
    ys, xs = np.where(a > 0.08)
    if len(xs) == 0:
        return None
    x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
    crop = Image.fromarray((np.clip(img[y0:y1, x0:x1], 0, 1) * 255).astype(np.uint8), "RGBA")
    w, h = crop.size
    scale = fill * OUT / max(w, h)
    if area:
        scale = min(scale, (area * OUT * OUT / (w * h)) ** 0.5)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    crop = crop.resize((nw, nh), Image.LANCZOS)
    canvas = Image.new("RGBA", (OUT, OUT), (0, 0, 0, 0))
    canvas.paste(crop, ((OUT - nw) // 2, (OUT - nh) // 2), crop)
    return canvas


def drawable_name(app):
    pkg = re.search(r"\{([^/]+)/", app["component"]).group(1)
    n = re.sub(r"[^a-z0-9]+", "_", pkg.lower()).strip("_")
    if not n[0].isalpha():
        n = "a_" + n
    return n


def strong_bg(bg):
    """Is the background colourful (solid, gradient or pattern) enough to colour the logo?"""
    m = SAFE & (bg[..., 3] > 0.5)
    if not m.any():
        return False
    px = bg[..., :3][m]
    colorful = (sat(px) > 0.2) & (lum(px) > 0.12) & (lum(px) < 0.93)
    return colorful.mean() > 0.5


def convert(app, folder, ov):
    f = Path(folder)
    mode = ov.get("mode", "auto")
    if "custom" in ov:
        return load(Path(__file__).resolve().parent.parent / "icons" / "custom" / ov["custom"]), "hand-made"
    if mode == "asis":
        p = f / ("legacy.png" if (f / "legacy.png").exists() else "full.png")
        return load(p), "kept as is"
    if not app.get("adaptive"):
        img = load(f / "legacy.png")
        img, col = flood_remove(img, legacy=True, tol=ov.get("tol", 0.16))
        if col is None:
            return None, "legacy, unclear edge"
        w, d = logo_stats(img)
        if strong_color(col) and ov.get("recolor", "auto") != "none" and w > 0.3:
            img = recolor(img, np.broadcast_to(col, img[..., :3].shape), "white")
        return img, "legacy"

    fg = load(f / "fg.png") if (f / "fg.png").exists() else np.zeros((S, S, 4), np.float32)
    bg = load(f / "bg.png") if (f / "bg.png").exists() else np.zeros((S, S, 4), np.float32)
    cov = float(fg[..., 3][SAFE].mean())
    bgcol = bg[..., :3][SAFE & (bg[..., 3] > 0.5)].mean(0) if (bg[..., 3] > 0.5).any() else np.zeros(3)

    if mode == "full":
        # keep the whole icon as a circle (for icons where the shape is the logo)
        img = over(fg, bg).copy()
        img[..., 3] *= np.clip(S * ov.get("radius", 36) / 108 - R + .5, 0, 1)
        return img, "full circle kept"
    if mode == "flood" or (mode == "auto" and (cov < 0.02 or cov > 0.9)):
        fc = hexrgb(ov["flood_color"]) if "flood_color" in ov else None
        img, col = flood_remove(over(fg, bg), fc, ov.get("tol", 0.16))
        if col is None:
            return None, "full picture, unclear edge"
        how = "edge removed"
        bgimg = np.broadcast_to(col, img[..., :3].shape)
        strong = strong_color(col)
    else:
        img = remove_shadow(fg, ov.get("shadow", 0.35))
        how = "logo layer"
        if mode == "fgflood":
            img, _ = flood_remove(img, hexrgb(ov.get("flood_color", "#ffffff")), ov.get("tol", 0.16))
            how += ", inner edge removed"
        bgimg = bg[..., :3]
        strong = strong_bg(bg)

    rc = ov.get("recolor", "auto")
    if rc == "auto":
        w, d = logo_stats(img)
        if strong and (w > 0.85 or d > 0.85):
            rc = "all"
        elif strong and w > 0.05:
            rc = "white"
        else:
            rc = "none"
    if rc != "none":
        img = recolor(img, bgimg, rc)
        how += f", recolored ({rc})"
    return img, how


def main():
    exp, out = Path(sys.argv[1]), Path(sys.argv[2])
    overrides = json.loads(Path(sys.argv[3]).read_text()) if len(sys.argv) > 3 else {}
    out.mkdir(parents=True, exist_ok=True)
    apps = json.load(open(exp / "apps.json"))["apps"]
    mapping, report = {}, []
    for i, app in enumerate(apps):
        name = drawable_name(app)
        ov = overrides.get(name, {})
        if ov.get("mode") == "skip":
            report.append((i, app["label"], name, "skipped"))
            continue
        img, how = convert(app, exp / app["folder"], ov)
        if img is None:
            report.append((i, app["label"], name, "FAILED: " + how))
            continue
        img = touch_up(img, ov)
        pic = fit(img, ov.get("fill", 1.0), ov.get("area"))
        if pic is None:
            report.append((i, app["label"], name, "FAILED: empty"))
            continue
        pic.save(out / f"{name}.png", optimize=True)
        mapping.setdefault(name, [])
        if app["component"] not in mapping[name]:
            mapping[name].append(app["component"])
        report.append((i, app["label"], name, how))
    (out / "map.json").write_text(json.dumps(mapping, indent=1))
    for r in report:
        print(*r, sep=" | ")


if __name__ == "__main__":
    main()
