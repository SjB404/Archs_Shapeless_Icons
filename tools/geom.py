"""Small geometry helpers for hand-drawn icon SVGs (rounded polygons)."""
import math


def _unit(v):
    l = math.hypot(*v)
    return (v[0] / l, v[1] / l)


def rounded_poly(points, radii, close=True, fmt="{:.2f}"):
    """SVG path for a polygon whose corners are rounded with circular arcs.
    points: [(x, y), ...]  radii: number or list (one per point, 0 = sharp)."""
    n = len(points)
    if not isinstance(radii, (list, tuple)):
        radii = [radii] * n
    f = lambda v: fmt.format(v).rstrip("0").rstrip(".") if "." in fmt.format(v) else fmt.format(v)
    segs = []
    for i in range(n):
        A, P, B = points[i - 1], points[i], points[(i + 1) % n]
        r = radii[i]
        u1 = _unit((A[0] - P[0], A[1] - P[1]))
        u2 = _unit((B[0] - P[0], B[1] - P[1]))
        cosang = max(-1.0, min(1.0, u1[0] * u2[0] + u1[1] * u2[1]))
        ang = math.acos(cosang)
        if r <= 0 or ang < 1e-6 or abs(ang - math.pi) < 1e-6:
            segs.append(("L", P))
            continue
        t = r / math.tan(ang / 2)
        T1 = (P[0] + u1[0] * t, P[1] + u1[1] * t)
        T2 = (P[0] + u2[0] * t, P[1] + u2[1] * t)
        d1 = (P[0] - A[0], P[1] - A[1])
        d2 = (B[0] - P[0], B[1] - P[1])
        sweep = 1 if d1[0] * d2[1] - d1[1] * d2[0] > 0 else 0
        segs.append(("L", T1))
        segs.append(("A", r, sweep, T2))
    out = []
    for k, s in enumerate(segs):
        if s[0] == "L":
            out.append(("M" if k == 0 else "L") + f"{f(s[1][0])} {f(s[1][1])}")
        else:
            _, r, sweep, T2 = s
            out.append(f"A{f(r)} {f(r)} 0 0 {sweep} {f(T2[0])} {f(T2[1])}")
    if out and out[0].startswith("A"):
        pass
    if close:
        out.append("Z")
    # first segment must be a move
    if not out[0].startswith("M"):
        out[0] = "M" + out[0][1:]
    return " ".join(out)


def line_intersect(p1, d1, p2, d2):
    """Intersection of lines p1 + s*d1 and p2 + t*d2."""
    den = d1[0] * d2[1] - d1[1] * d2[0]
    s = ((p2[0] - p1[0]) * d2[1] - (p2[1] - p1[1]) * d2[0]) / den
    return (p1[0] + s * d1[0], p1[1] + s * d1[1])


def tangent_length(points, i, r):
    A, P, B = points[i - 1], points[i], points[(i + 1) % len(points)]
    u1 = _unit((A[0] - P[0], A[1] - P[1]))
    u2 = _unit((B[0] - P[0], B[1] - P[1]))
    ang = math.acos(max(-1, min(1, u1[0] * u2[0] + u1[1] * u2[1])))
    return r / math.tan(ang / 2), math.degrees(ang)
