#!/usr/bin/env python3
"""
Turns an export zip (from the app) into shapeless icons.

    python3 tools/convert_export.py <unzipped export dir> <out dir> [overrides.json]

Writes <out>/<drawable_name>.png and <out>/map.json ({drawable_name: [component, ...]}).

Default behaviour per app:
  * Take the logo layer (fg) and drop the background layer.
  * If the background is colourful (solid, gradient or pattern) and the logo is white,
    the white parts take the background's colours. If the logo is one flat colour, all of it does.
  * If the logo is inside the background layer, the logo layer is one full picture,
    or the app has an old-style icon: cut away the plain colour around the edges instead.
  * Long fake shadows are removed, then the logo is cropped and scaled up to fill the icon.

Per-app options (icons/overrides.json, keyed by drawable name):
  mode        "full"     keep the whole icon as a circle (pictures with no separate logo)
              "flood"    cut the plain edge colour away from the whole icon
              "fgflood"  cut the plain edge colour away from the logo layer only
              "keeplight" cut away everything dark/coloured around a light logo (e.g. a skull in a ring)
              "mono"     use the one-colour themed-icon layer (set its colour with "color")
              "asis"     use the icon unchanged
              "skip"     leave the app out of the pack
  custom      file in icons/custom/ to use instead (hand-made)
  svg         file in icons/svg/ to use instead (official logo drawing)
  crop        [x0, y0, x1, y1] as fractions, crop the source before anything else
  flood_color colour to cut away (hex, or "auto" = most common colour)
  tol         how different a colour may be and still count as the edge colour (0-1)
  radius      circle size for mode "full", in dp out of 54
  shadow      transparency below which pixels count as fake shadow (default 0.35)
  recolor     "auto" | "none" | "white" | "all"
  color       paint the whole logo this colour
  dark_to     paint the dark parts this colour
  drop_color  make this colour see-through everywhere (e.g. black inside a bell)
  drop_white  make white parts see-through
  infill      fill holes enclosed by the logo with this colour ("bg" = the old background colour)
  infill_keep_center  leave the hole in the middle open (donut shape)
  keep_largest drop small leftover bits, keep the main shape(s)
"""
import json
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

S = 432          # working size (one icon layer, 108dp at 4x)
OUT = 256        # final png size
yy, xx = np.mgrid[0:S, 0:S]
R = np.hypot(xx - S / 2 + .5, yy - S / 2 + .5)
SAFE = R < S * 33 / 108
RING = (R > S * 30 / 108) & (R < S * 35 / 108)   # just inside the visible circle
ROOT = Path(__file__).resolve().parent.parent


# ---------- basics ----------

def load(p, crop=None):
    im = Image.open(p).convert("RGBA")
    if crop:
        w, h = im.size
        im = im.crop((int(crop[0] * w), int(crop[1] * h), int(crop[2] * w), int(crop[3] * h)))
        side = max(im.size)
        sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
        sq.paste(im, ((side - im.size[0]) // 2, (side - im.size[1]) // 2))
        im = sq
    if im.size != (S, S):
        im = resize(im, (S, S))
    return np.asarray(im).astype(np.float32) / 255


def load_svg(p):
    """Official logo drawings (from the Simple Icons / dashboard-icons collections)."""
    import io
    import cairosvg
    png = cairosvg.svg2png(url=str(p), output_width=S)
    im = Image.open(io.BytesIO(png)).convert("RGBA")
    sq = Image.new("RGBA", (max(im.size),) * 2, (0, 0, 0, 0))
    sq.paste(im, ((sq.size[0] - im.size[0]) // 2, (sq.size[1] - im.size[1]) // 2))
    return np.asarray(resize(sq, (S, S))).astype(np.float32) / 255


def resize(im, size):
    """Resize without see-through pixels bleeding their hidden colour into the edges."""
    return im.convert("RGBa").resize(size, Image.LANCZOS).convert("RGBA")


def over(top, bot):
    a = top[..., 3:4] + bot[..., 3:4] * (1 - top[..., 3:4])
    rgb = (top[..., :3] * top[..., 3:4] + bot[..., :3] * bot[..., 3:4] * (1 - top[..., 3:4])) / np.maximum(a, 1e-6)
    return np.concatenate([rgb, a], -1)


def sat(rgb):
    return rgb.max(-1) - rgb.min(-1)


def lum(rgb):
    return rgb @ np.array([.299, .587, .114], np.float32)


def hexrgb(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)], np.float32)


def color_to_alpha(rgb, B):
    """Split each pixel into 'colour B' + 'something on top', return (top colour, top alpha).
    A pixel that is pure B becomes fully see-through; a half-B edge becomes half see-through."""
    B = np.asarray(B, np.float32)
    up = (rgb - B) / np.maximum(1 - B, 1e-4)
    down = (B - rgb) / np.maximum(B, 1e-4)
    al = np.where(rgb > B, up, down).max(-1).clip(0, 1)
    F = (rgb - B) / np.maximum(al, 1e-4)[..., None] + B
    return F.clip(0, 1), al


def mode_color(img):
    op = img[..., :3][img[..., 3] > 0.9]
    if len(op) == 0:
        return None
    q = (op * 15).round().astype(int)
    k = q[:, 0] * 256 + q[:, 1] * 16 + q[:, 2]
    keys, counts = np.unique(k, return_counts=True)
    return op[k == keys[counts.argmax()]].mean(0)


def dilate(m, n):
    return ndimage.binary_dilation(m, iterations=n)


# ---------- cutting away backgrounds ----------

def flood_remove(img, color=None, tol=0.16, legacy=False, passable=None):
    """Make the background touching the edges see-through, with clean (no halo) edges."""
    rgb, a = img[..., :3], img[..., 3]
    if passable is None:
        if color is None:
            ring = rgb[RING & (a > 0.9)]
            if legacy or len(ring) < 50:
                color = mode_color(img)
                if color is None:
                    return img, None
            else:
                if ring.std(0).mean() > 0.06:
                    return img, None   # edge isn't one plain colour: can't do it safely
                color = ring.mean(0)
        d = np.abs(rgb - color).max(-1)
        passable = (a < (0.6 if legacy else 0.1)) | (d < tol)
    lab, _ = ndimage.label(passable)
    edge_labels = np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))
    seen = np.isin(lab, edge_labels[edge_labels > 0])
    out = img.copy()
    out[..., 3] = np.where(seen, 0, a)
    if color is not None:
        band = dilate(seen, 2) & ~seen
        F, al = color_to_alpha(rgb, color)
        out[..., :3] = np.where(band[..., None], F, rgb)
        out[..., 3] = np.where(band, a * al, out[..., 3])
    return out, color


def remove_shadow(img, limit=0.35):
    # long fake shadows are big areas of nearly see-through pixels
    a = img[..., 3]
    faint = (a > 0.02) & (a < limit)
    if faint.sum() > 0.08 * (a > 0.02).sum():
        img = img.copy()
        img[..., 3] = np.where(a < limit, 0, a)
    return img


def recolor(img, bgimg, mode):
    """mode 'white': white parts take the background colour. 'all': whole logo does."""
    rgb, a = img[..., :3], img[..., 3:4]
    if mode == "all":
        return np.concatenate([bgimg, a], -1)
    w = np.clip((rgb.min(-1, keepdims=True) - 0.70) / 0.2, 0, 1) * np.clip(1 - sat(rgb)[..., None] / 0.15, 0, 1)
    return np.concatenate([rgb * (1 - w) + bgimg * w, a], -1)


def strong_bg(px):
    """Is this background colourful (solid, gradient or pattern) enough to colour the logo?"""
    if len(px) == 0:
        return False
    colorful = (sat(px) > 0.25) & (lum(px) > 0.04) & (lum(px) < 0.93)
    return colorful.mean() > 0.5


def logo_stats(img):
    op = img[..., 3] > 0.5
    if op.sum() == 0:
        return 0, 0
    px = img[..., :3][op]
    white = float(((px.min(-1) > 0.86) & (sat(px) < 0.12)).mean())
    dark = float((lum(px) < 0.2).mean())
    return white, dark


# ---------- hand-fix options ----------

def touch_up(img, ov, bgcolor):
    img = img.copy()
    if "drop_color" in ov:
        F, al = color_to_alpha(img[..., :3], hexrgb(ov["drop_color"]))
        img[..., :3], img[..., 3] = F, img[..., 3] * al
    if ov.get("drop_white"):
        F, al = color_to_alpha(img[..., :3], np.ones(3, np.float32))
        keep = sat(img[..., :3]) > 0.12   # only take white out of grey/white pixels
        img[..., :3] = np.where(keep[..., None], img[..., :3], F)
        img[..., 3] = np.where(keep, img[..., 3], img[..., 3] * al)
    if ov.get("keep_largest"):
        lab, n = ndimage.label(img[..., 3] > 0.3)
        if n > 1:
            sizes = ndimage.sum(np.ones_like(lab), lab, range(1, n + 1))
            keep = np.isin(lab, 1 + np.where(sizes >= 0.15 * sizes.max())[0])
            img[..., 3] *= dilate(keep, 2)
    if "infill" in ov:
        col = bgcolor if ov["infill"] == "bg" else hexrgb(ov["infill"])
        open_ = img[..., 3] < 0.5
        lab, _ = ndimage.label(open_)
        edge = np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))
        holes = open_ & ~np.isin(lab, edge)
        if ov.get("infill_keep_center"):
            c = lab[S // 2, S // 2]
            if c > 0:
                holes &= lab != c
        holes = holes | (dilate(holes, 3) & (img[..., 3] > 0.02))
        under = np.zeros_like(img)
        under[..., :3] = col
        under[..., 3] = holes
        img = over(img, under)
    rgb = img[..., :3]
    if "dark_to" in ov:
        k = np.clip((0.35 - lum(rgb)) / 0.15, 0, 1)[..., None]
        img[..., :3] = rgb * (1 - k) + hexrgb(ov["dark_to"]) * k
    if "color" in ov:
        img[..., :3] = hexrgb(ov["color"])
    return img


# ---------- one app ----------

def convert(app, folder, ov):
    f = Path(folder)
    mode = ov.get("mode", "auto")
    crop = ov.get("crop")
    white = np.ones(3, np.float32)
    if "custom" in ov:
        return load(ROOT / "icons" / "custom" / ov["custom"]), "hand-made", white
    if "svg" in ov:
        return load_svg(ROOT / "icons" / "svg" / ov["svg"]), "official logo", white
    if mode == "asis":
        p = f / ("legacy.png" if (f / "legacy.png").exists() else "full.png")
        return load(p, crop), "kept as is", white
    if mode == "mono":
        return load(f / "mono.png", crop), "one-colour layer", white

    if not app.get("adaptive"):
        img = load(f / "legacy.png", crop)
        fc = ov.get("flood_color")
        fc = hexrgb(fc) if fc and fc != "auto" else None
        img, col = flood_remove(img, fc, ov.get("tol", 0.16), legacy=True)
        if col is None:
            return None, "legacy, unclear edge", white
        w, d = logo_stats(img)
        if ov.get("recolor", "auto") != "none" and strong_bg(col[None]) and w > 0.3:
            img = recolor(img, np.broadcast_to(col, img[..., :3].shape), "white")
        return img, "legacy", col

    fg = load(f / "fg.png", crop) if (f / "fg.png").exists() else np.zeros((S, S, 4), np.float32)
    bg = load(f / "bg.png", crop) if (f / "bg.png").exists() else np.zeros((S, S, 4), np.float32)
    fg = remove_shadow(fg, ov.get("shadow", 0.35))
    cov = float(fg[..., 3][SAFE].mean())
    bgpx = bg[..., :3][SAFE & (bg[..., 3] > 0.5)]
    bgcol = bgpx.mean(0) if len(bgpx) else white

    fc = ov.get("flood_color")
    if mode == "full":
        img = over(fg, bg)
        img[..., 3] *= np.clip(S * ov.get("radius", 36) / 108 - R + .5, 0, 1)
        if fc:
            img, _ = flood_remove(img, hexrgb(fc), ov.get("tol", 0.16))
        return img, "full circle kept", bgcol

    if mode == "keeplight":
        img = over(fg, bg) if ov.get("use_bg") else fg
        lit = (lum(img[..., :3]) > ov.get("light", 0.7)) & (sat(img[..., :3]) < 0.25) & (img[..., 3] > 0.5)
        img, _ = flood_remove(img, np.zeros(3, np.float32), passable=~lit)
        return img, "light logo kept", bgcol

    if mode == "flood" or (mode == "auto" and (cov < 0.02 or cov > 0.9)):
        src = over(fg, bg)
        col = hexrgb(fc) if fc and fc != "auto" else (mode_color(src) if fc == "auto" else None)
        img, col = flood_remove(src, col, ov.get("tol", 0.16))
        if col is None:
            return None, "full picture, unclear edge", bgcol
        how, bgimg, strong = "edge removed", np.broadcast_to(col, img[..., :3].shape), strong_bg(col[None])
        bgcol = col
    else:
        img, how = fg, "logo layer"
        if mode == "fgflood":
            col = hexrgb(fc) if fc and fc != "auto" else (mode_color(fg) if fc == "auto" else white)
            img, _ = flood_remove(img, col, ov.get("tol", 0.16))
            how += ", inner edge removed"
        bgimg, strong = bg[..., :3], strong_bg(bgpx)

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
    return img, how, bgcol


def fit(img, fill=1.0):
    a = img[..., 3]
    ys, xs = np.where(a > 0.08)
    if len(xs) == 0:
        return None
    x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
    crop = Image.fromarray((np.clip(img[y0:y1, x0:x1], 0, 1) * 255).astype(np.uint8), "RGBA")
    w, h = crop.size
    scale = fill * OUT / max(w, h)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    crop = resize(crop, (nw, nh))
    canvas = Image.new("RGBA", (OUT, OUT), (0, 0, 0, 0))
    canvas.paste(crop, ((OUT - nw) // 2, (OUT - nh) // 2), crop)
    return canvas


def drawable_name(app):
    pkg = re.search(r"\{([^/]+)/", app["component"]).group(1)
    n = re.sub(r"[^a-z0-9]+", "_", pkg.lower()).strip("_")
    if not n[0].isalpha():
        n = "a_" + n
    return n


def main():
    exp, out = Path(sys.argv[1]), Path(sys.argv[2])
    overrides = json.loads(Path(sys.argv[3]).read_text()) if len(sys.argv) > 3 else {}
    out.mkdir(parents=True, exist_ok=True)
    apps = json.load(open(exp / "apps.json"))["apps"]
    mapping = {}
    for i, app in enumerate(apps):
        name = drawable_name(app)
        ov = overrides.get(name, {})
        if ov.get("mode") == "skip":
            print(i, app["label"], name, "skipped", sep=" | ")
            continue
        img, how, bgcol = convert(app, exp / app["folder"], ov)
        if img is not None:
            img = touch_up(img, ov, bgcol)
        pic = fit(img, ov.get("fill", 1.0)) if img is not None else None
        if pic is None:
            print(i, app["label"], name, "FAILED: " + how, sep=" | ")
            continue
        pic.save(out / f"{name}.png", optimize=True)
        mapping.setdefault(name, [])
        if app["component"] not in mapping[name]:
            mapping[name].append(app["component"])
        print(i, app["label"], name, how, sep=" | ")
    (out / "map.json").write_text(json.dumps(mapping, indent=1))


if __name__ == "__main__":
    main()
