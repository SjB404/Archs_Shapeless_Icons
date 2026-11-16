#!/usr/bin/env python3
"""
Turns an icon source file into the final pack image (256x256, transparent, logo scaled to fill).

Sources live in icons/src/ and are named after the drawable:
  <name>.svg        clean vector drawing (preferred)
  <name>.pixel.png  pixel art at its native pixel size; scaled up by whole numbers, no smoothing
  <name>.png        high-resolution picture (only when a drawing isn't possible), smoothed scaling

The empty space around the logo is trimmed, then the logo is scaled up until its longest side
fills the icon ("max size"), and centred.
"""
import io
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "icons" / "src"
SIZE = 256


def source_for(name, src_dir=SRC):
    for suffix, kind in ((".svg", "svg"), (".pixel.png", "pixel"), (".png", "png")):
        p = Path(src_dir) / f"{name}{suffix}"
        if p.exists():
            return p, kind
    return None, None


def _smooth_resize(im, size):
    # premultiplied, so see-through pixels can't bleed their hidden colour into the edges
    return im.convert("RGBa").resize(size, Image.LANCZOS).convert("RGBA")


def _trim(im, threshold=3):
    alpha = im.getchannel("A").point(lambda v: 255 if v > threshold else 0)
    box = alpha.getbbox()
    return im.crop(box) if box else None


def _center(im, size=SIZE):
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # plain paste: the canvas is empty, so the logo's pixels are copied exactly
    # (pasting with a mask here would blend black into the edges and leave a dark outline)
    canvas.paste(im, ((size - im.size[0]) // 2, (size - im.size[1]) // 2))
    return canvas


def render_file(path, kind, size=SIZE):
    path = Path(path)
    if kind == "svg":
        import cairosvg
        png = cairosvg.svg2png(url=str(path), output_width=1536)
        im = Image.open(io.BytesIO(png)).convert("RGBA")
        if max(im.size) > 4096:   # very tall drawings: render again by height
            png = cairosvg.svg2png(url=str(path), output_height=1536)
            im = Image.open(io.BytesIO(png)).convert("RGBA")
    else:
        im = Image.open(path).convert("RGBA")

    if kind == "pixel":
        im = _trim(im, 0)
        if im is None:
            return None
        k = max(1, size // max(im.size))
        im = im.resize((im.size[0] * k, im.size[1] * k), Image.NEAREST)
        return _center(im, size)

    im = _trim(im)
    if im is None:
        return None
    scale = size / max(im.size)
    im = _smooth_resize(im, (max(1, round(im.size[0] * scale)), max(1, round(im.size[1] * scale))))
    return _center(im, size)


def render(name, src_dir=SRC, size=SIZE):
    """Final pack image for a drawable, or None if it has no source in icons/src yet."""
    path, kind = source_for(name, src_dir)
    if path is None:
        return None
    return render_file(path, kind, size)
