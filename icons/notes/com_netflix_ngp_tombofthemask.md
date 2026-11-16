# Tomb of the Mask (Netflix edition), `com_netflix_ngp_tombofthemask`

Source file: `icons/src/com_netflix_ngp_tombofthemask.pixel.png` (pixel art, 64 x 60 px).

## What it looks like
The little yellow mask guy from the game, as sharp pixel art, filling the icon. A small pixel-art
Netflix "N" sits in the top-left corner. No maze, no black box behind the N.

## Sources
- Netflix edition, Google Play: https://play.google.com/store/apps/details?id=com.netflix.NGP.tombofthemask
  (store icon = `refs/.../play.png`; screenshots saved as `refs/com_netflix_ngp_tombofthemask/web/shot1-8.png`).
  Screenshot 3 shows the guy standing in the game: this is the sprite used.
- Original game (Playgendary), Google Play: https://play.google.com/store/apps/details?id=com.playgendary.tom
  (icon saved as `refs/com_netflix_ngp_tombofthemask/web/orig_icon.png`).
- Netflix N colours: the Netflix logo drawings on disk (`refs/.../official/logos__netflix-icon.svg`,
  `dashboard__netflix.svg`): #E50914 and #B1060F. The N on the Netflix edition's own store icon
  measures the same (#E50815 / #B10510).

## The guy
- The game's sprite is 10 x 10 pixels. Read pixel by pixel from the in-game screenshot and both store
  icons; they agree except for one pixel:
  - the Netflix store icon leaves row 7, column 1 (counting from 1) of the hand empty;
  - the game itself and the original game's icon have it filled.
  I used the game's version (filled). The original icon also gives him longer legs, but the game and
  the Netflix icon don't, so I kept the short legs.
- Yellow #FFF700, measured from the store icons.
- The two short lines on the left of his head are part of him (he has them while standing still
  in the game), so they stay.

## The Netflix badge (reuse this on other Netflix games)
Final icon size 256 x 256:
- **Size:** 36 px wide x 64 px tall (about 14% of the icon's width, 25% of its height). That's
  about the share Netflix's own game icons give the N (13% wide, 24% tall on the 512 px store icon).
- **Position:** top-left corner of the art. The art's square stays as big as it would be without
  the badge. Leave a see-through gap of at least 4 px (at 256) between the N and the art.
- **Colours:** diagonal #E50914, the two uprights #B1060F, fold shadow #83040B (one pixel on each
  upright where it touches the diagonal: the left upright in the top half, the right upright in the
  bottom half, like the shading on the real N). No outline, no black box.
- **Pixel version** (9 x 16; B = #E50914, D = #B1060F, s = #83040B, . = see-through). On a pixel
  icon that the build scales 4x, one N pixel = 4 px at 256:
  ```
  BBB...DDD
  BBB...DDD
  BBB...DDD
  sBBB..DDD
  sBBB..DDD
  DsBBB.DDD
  DsBBB.DDD
  DDsBBBsDD
  DDsBBBsDD
  DDD.BBBsD
  DDD.BBBsD
  DDD..BBBs
  DDD..BBBs
  DDD...BBB
  DDD...BBB
  DDD...BBB
  ```
- **Vector icons:** draw the same N (Simple Icons' Netflix outline has the right shape) at the same
  size and corner, with the same three colours.

## How the file is laid out
64 x 60 image. The guy's pixels are 6 x 6 blocks, starting at x=4; the N uses single pixels at
x 0-8, y 0-15, 1 pixel clear of his top head line. The build makes it 4x bigger with no smoothing:
the guy becomes 240 px (the same size as with no N at all) and the N 36 x 64 px. Every pixel is
either fully solid or fully see-through, so there's no edge halo. The N's pixels are smaller than the
guy's because an N made of his big pixels would have to be at least 3 of them wide (about 30% of the
icon). It still sits on the same pixel grid and stays just as sharp. Generator script:
`/home/claude/work/tomb/make_tomb.py`.

## For Arch to look at
- **The N is smaller than the rules suggest.** R19 / app_notes ask for an N about 30-35% (a third)
  of the icon's width. In this icon's top-left corner, anything wider than about 14% runs into the
  guy's two head lines. A third-width N would cover those lines and cut into his head and hand, or
  the guy would have to shrink to make room. Your own words were "the little yellow guy full size,
  with a small netflix logo", so I kept the N small. If you'd rather have the big N anyway (the guy
  loses his two head lines), there's a mock-up: `/home/claude/work/tomb/mock_third_width.png`
  (preview `/home/claude/work/tomb/pv_mock.png`).
- At real phone size (48 px) the N is about 7 x 12 px: small, but it clearly reads as a red N on
  both the wallpaper and the drawer.
