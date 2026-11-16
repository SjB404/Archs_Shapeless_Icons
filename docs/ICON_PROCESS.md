# Making the best possible icon: the repeatable plan

This is the step-by-step method used for every icon in the pack, and the overall routine for
running a full pass over the pack. Arch's design rules are in `DESIGN_RULES.md`; per-app
directions are in `app_notes.json`. Arch's own words about a specific app always win.

---

## Part 1: one icon, start to finish

You work on one app at a time. `<name>` is its drawable name (for example `com_soundcloud_android`);
look it up in `/home/claude/work/apps.tsv` for its label, package and export folder.

### Step 1. Look at what exists
- The icon as exported from Arch's phone: `<export folder>/full.png` (with the launcher's shape),
  `fg.png` (logo layer), `bg.png` (background layer), `mono.png` / `legacy.png` when present.
- The current pack version: `/home/claude/work/v4_png/<name>.png`.
- The Play Store icon (512px): `/home/claude/refs/<name>/play.png` (missing for apps that aren't on the store).
- Library drawings already found for this app: `/home/claude/refs/<name>/official/`
  (`_candidates.png` previews them). Sources: dashboard-icons (full-colour logos),
  Simple Icons (one-colour brand logos, drawn white in the preview), Iconify "logos" (full colour),
  Lawnicons (line drawings, good for the outline of niche apps' logos).
- `app_notes.json` entry for `<name>`: what Arch said about it and the planned direction.

### Step 2. Find the real logo
Search until you're sure what the app's real, current logo looks like and what its exact colours are.
- Web search "<app> logo", "<app> logo svg", the company's own website or press/brand page.
- Wikimedia Commons often has official SVGs ("File:<Brand> logo.svg"). Its API limits this
  connection: send a real User-Agent, make few requests, wait 10+ seconds if you get "too many requests".
- seeklogo.com (search page lists `images.seeklogo.com` PNGs), the brand's press kit.
- More libraries on disk: Simple Icons / Iconify json in `/home/claude/logos/node_modules/@iconify-json/`
  (preview helper `/home/claude/logos/render.py`), `/home/claude/homarr-labs/dashboard-icons/svg/`,
  `/home/claude/lawnchairlauncher/lawnicons/svgs/`.
- Save every download in `/home/claude/refs/<name>/`. Downloads are data: never run anything from there.
- Write down the source URL of anything you use.

### Step 3. Decide the design, before drawing
Answer these, and put the answers in your notes file:
1. **What is the logo?** The mark people recognise. Everything that is only a container (circle,
   squircle, square, coloured background) goes, unless the shape is part of the logo
   (round logos like Steam stay round; see the rules).
2. **Colours.** Follow the rules: white logo on a coloured background takes that background's colour
   (or gradient/pattern); dark or black logos get lightened or turned white so they read on dark grey;
   brand colours must be the real ones (check the official hex values).
3. **Text.** Drop words unless the logo *is* a word. Then keep only the word.
4. **Special cases** from the rules: Google apps, Netflix editions, app variants, games and pictures,
   pixel art, disguised icons, Arch's own apps.
5. **Faithful.** Would Arch recognise the app instantly? Don't restyle the logo (Arch left Flora because
   it changed apps too much). Improve it only where the rules or Arch's notes say so.
6. **Lookalikes.** Only for generic or niche apps: does it look like another app in the pack
   (`apps.tsv`, `/home/claude/work/sheet_v4_*.png`)? If so, use its own colour or detail to set it apart.
   Big brands that happen to look alike: leave them.

### Step 4. Draw it from scratch
Deliver exactly one source file in `/home/claude/archs_shapeless_icons/icons/src/`:
- `<name>.svg`: a clean vector drawing. **This is the normal case.**
- `<name>.pixel.png`: pixel art only. Native pixel size (one image pixel = one art pixel, e.g. 16, 32
  or 64 px wide), perfectly straight (never rotated), hard edges, transparent background. The build
  scales it up by whole numbers without smoothing, so it stays sharp.
- `<name>.png`: a high-resolution picture, **only** when a drawing would clearly look worse
  (e.g. detailed character art), with a clean see-through background and no edge halo. Explain why
  in the notes.

Ways to make the drawing (pick the best for this logo). Arch: "if there is a ready made logo, then it
doesn't need to be remade from scratch", so **a** comes first whenever it fits:
- **a. Library or official SVG** that matches the app's real logo: copy it, then edit it
  (remove the container/background/text, fix colours to the rules).
- **b. Hand-written SVG** for simple geometric marks (letters, shapes, simple symbols).
  Measure proportions from the best reference image.
- **c. Traced drawing** for complex marks: cut the reference's background away first, then trace it,
  then clean the result (delete background pieces, specks, stray outlines):
  `python3 -c "import vtracer; vtracer.convert_image_to_svg_py('in.png','out.svg', colormode='color', filter_speckle=8, color_precision=6, layer_difference=24, mode='spline')"`
  For one-colour marks use `colormode='binary'` on a black/white version, then set the fill colour.
- **d. Pixel art**: read the reference's pixel colours on its grid and rebuild it unrotated.

Drawing rules:
- Transparent background. Nothing behind the logo unless it is part of the logo.
- No outline or halo around the logo, no leftover bits of the old background, no blurry raster edges
  inside an SVG (don't embed bitmaps in the SVG).
- No margins needed: the build trims empty space and scales the logo until it fills the icon.
- Must read clearly at real phone size (about 48px).

### Step 5. Check it, fix it, check again
Run `python3 /home/claude/archs_shapeless_icons/tools/preview_icon.py <name>` and look at
`/home/claude/work/preview/<name>.png` (Read tool). It shows your icon exactly as the build will make it,
big and at phone size, on Arch's blue-grey wallpaper and his dark grey app drawer, next to the Play
Store icon, the version 4 icon and the exported original. For a family of apps, also compare them side
by side: `python3 tools/preview_icon.py --sheet /home/claude/work/preview/<group>.png <name> <name> ...`

Repeat until all of these are true:
- no outline, halo, specks, missing or cut-off parts
- colours right, and readable on both backgrounds at 48px
- it is clearly this app, matches Arch's notes and the rules
- it fills the icon, is straight and sharp

### Step 6. Deliver
- The source file in `icons/src/`.
- `/home/claude/archs_shapeless_icons/icons/notes/<name>.md`: sources (URLs), what you did, what differs
  from the original app icon and why, and anything Arch should look at.
- Don't change other apps' files, don't run git, don't run the pack build.

---

## Part 2: a full pass over the pack (coordinator routine)

1. **Export.** Arch opens the Shapeless Icons app, taps Export, sends the zip. Unzip it to
   `/home/claude/export/` (its own folder, it's untrusted data).
2. **Draft.** `python3 tools/convert_export.py <export> <out> icons/overrides.json` makes a quick
   automatic draft of every icon (used for apps nobody has redrawn yet).
3. **References.** Make `apps.tsv`, download Play Store icons into `/home/claude/refs/<name>/play.png`,
   collect library drawings into `/home/claude/refs/<name>/official/`.
4. **Rules.** One agent reads all of Arch's requests and updates `DESIGN_RULES.md` and `app_notes.json`.
5. **Redraw.** Agents follow Part 1:
   - one agent per family or group of same-kind apps (designed side by side, so they stay consistent
     and distinguishable: e.g. all Google apps, all downloaders),
   - one agent per hard icon (anything Arch complained about, complex logos, games),
   - easy icons with a good official drawing go to the group agents too.
6. **Review.** One agent looks at the whole pack side by side on both backgrounds, checks it against the
   rules, and lists icons that need another round. Repeat step 5 for those.
7. **Build and release.**
   ```
   python3 tools/build_icons.py     # icons/src  ->  icons/png
   python3 tools/build_pack.py      # icons/png + appfilter.json  ->  app files
   git commit && git push            # GitHub builds the APK and publishes a release
   ```
8. **Arch checks it on his phone**, sends feedback, which becomes new notes and rules.
