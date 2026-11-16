# PAC-MAN 256 (eu_bandainamcoent_pacman256)

Source: the game's own store icon (/home/claude/refs/eu_bandainamcoent_pacman256/play.png), the blocky 3D Pac-Man
with its extruded sides and the "256" block under it. Not generic Pac-Man: the earlier flat pixel Pac-Man was replaced.

Done: split the store icon into layers by colour (yellow face, side/extrusion, cyan 2, red 5, orange 6), traced each
with sharp straight corners (vtracer polygon mode) and stacked them as flat SVG shapes.

Changes from the store icon:
- yellow background, round-corner square and the floor shadow removed (R5, R7)
- dark sides (#2F2E2F) lightened to grey #8A8A96 so they show on the dark drawer (R10)
- digit colours kept as in the game (#5BC2CE, #ED5145, #F39643); face flat #FFFF00

Doubt: it is a clean vector of a 3D-looking pixel logo, not a native pixel-grid file, because the extrusion is diagonal.
The game's yellow face has a faint shading that is left flat.
