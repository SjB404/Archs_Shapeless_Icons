# Design rules

These rules say how every icon in Arch's Shapeless Icons should look. They're built from everything
Arch has asked for so far. Use them together with:

- `ICON_PROCESS.md`: the step-by-step method for making one icon
- `app_notes.json`: what each app's icon should be (Arch's words about it, the rules that matter
  most for it, a short direction, and whether it's redrawn or kept)

R1, R2, R3, R5, R6, R7, R13 and R24 apply to every icon. `app_notes.json` only lists the rules that
matter most for each app.

---

## Where the rules come from

Arch's messages, in order. The rules quote them word for word.

| | Message |
|---|---|
| M1 | First request, with two home screen screenshots: before (`/mnt/user-data/uploads/083a5069-image.png`) and painted over to show what he wants (`/mnt/user-data/uploads/59555be7-image.png`) |
| M2 | Logo colours and max size |
| M3 | Feedback on version 2 (app drawer screenshots) |
| M4 | Use real logos from online |
| M5 | Feedback on version 4, plus asking for a repeatable plan and these rules |
| M6 | Note about lookalike apps |
| M7 | Terra Defense note |
| M8 | Feedback on version 5 (Bible, Play Store too sharp, two-tone Google icons too bland, Snapseed, Sponge) |
| M9 | SoundCloud redesign picture to copy, the icon index, "the icon doesn't need gradients" |
| M10 | Feedback on version 6 (16 Nov 2026), with six pictures to copy, and to do all the work directly |
| M11 | bunq: the full colour range and slightly thicker letters |

---

## Which rule wins

1. **What Arch said about this specific app** (`arch_said` in `app_notes.json`). If he said two
   things about the same app, the newer one wins. Example: bol was "bol should be blue" (M3), then
   "needs to be changed to a lighter blue" (M5), so it's a lighter blue.
2. **Arch's painted redraws** (R4). One exception: when a newer general rule from Arch covers the
   same problem, follow the newer rule and list the app under open questions. The only case right now
   is the Camera. He painted it black in M1, but M5 says dark logos should be lighter or white.
3. **The special-group rules** (R18 to R23), for the apps they cover.
4. **The general rules.** For colour, check them in this order: R12 (things that really are white
   stay white), then R11, then R9, then R10 for readability. R13 sets the exact colour values.
5. **The default**, when nothing else applies: the app's real, current logo, copied exactly, without
   its container, as big as possible.

---

## A. Goal and taste

### R1. What "best looking" means for Arch
**Rule.** The best icon is the app's real logo: recognisable at a glance, as big as possible, with
clean sharp edges, in its real colours, and easy to see in his dark app drawer. Bold and simple
beats fussy and thin. When the brand has a colour, use it instead of plain white.

**From.** M1: "I simply don't want a border on any of my apps, it would look so much better and allow
for way bigger icons". M1 on SoundCloud: "have them be thicker and the icon be orange so it looks
better". M2: logos take their background colour. M5: "basically to get the best icon".

**How.** Before drawing, ask the questions from M5: "what would make this icon look best, how could I
improve this icon, is this icon distinguishable from other apps". Answer them with the rules below.
Don't invent a new style.

**Examples.**
- SoundCloud: fewer, thicker bars, in orange.
- Anker: a readable symbol instead of tiny words.
- Steam: the whole round logo instead of a shaft floating on its own.

### R2. Stay faithful to the real logo
**Rule.** Only change what the rules or Arch ask for: remove the container, make the logo big, fix
readability, and fix the things he named. Keep the logo's shapes, proportions, colours and details.
Never redraw a logo into a "pack style".

**From.** M1: "I used to use flora icons, which I liked, but it changed the look of apps too much".

**How.** Compare your icon with the store icon and the exported original. Ask: would Arch recognise
this app at once? If a change makes the logo look like a different app or a different style, undo
it.

**Examples.**
- Translate stays as it is ("Google translate looks good").
- Brave, Chrome, Spotify and Netflix are simply the real logos.

### R3. Use the real logo: find it, download it, redraw it
**Rule.** Every icon starts from the app's actual logo. Find it online (the brand's own site or press
page, Wikimedia Commons, seeklogo, or the icon libraries on disk), download it, and use it or redraw
from it. Never guess at a logo when the real one can be found.

**From.** M4: "I want you to either get images online, or redraw logos yourself when needed ... so I
want you to find the logo, download the image and use it in the pack, or redraw based on the image".

**How.**
- Use the current logo, unless a rule says otherwise (Google apps: R18).
- Check both export layers. Part of a logo can be in the background layer. Automate's phone with the
  smiley is in `bg.png`, and so is AccuBattery's battery. Steam's circle is its background.
- Libraries sometimes hold a different company with the same name. The "fomo" in the logo library
  is not the fomo app on Arch's phone. Check that the logo matches his app.
- Write the source in the icon's notes file.

**Examples.**
- Anker: the "A with lightning bolt" from seeklogo.
- Burger King: the official 2021 logo.
- Gmail: the classic logo from Wikimedia.

### R4. Arch's painted redraws are references
**Rule.** In M1, Arch painted over his home screen to show what he wants. Those drawings are
references for those apps.

**From.** The M1 screenshots.

**What he painted:**
- **Squib:** kept as the orange blob with two eyes.
- **Files, Play Store, Brave, Phone:** just the logo, with the circle removed and the logo bigger.
- **Weather:** a WHITE cloud with the red maple leaf.
- **Google Home:** the coloured house outline (red roof, yellow left, green bottom, blue right), with
  the inside see-through.
- **Sleep Timer:** a big white crescent moon with no dark circle.
- **SoundCloud:** thick orange bars and a cloud (see R17).
- **Beeper:** the "B" bubble in blue.
- **Camera:** black body, see-through lens, red dot. See R10 and open question 1.
- **Whack-a-mole (Mullvad VPN):** he wrote "VPN" on it and said "I gave up on the vpn". See R21.
- The dark circles with white line icons are folder icons, not apps. Ignore them.

---

## B. Shape and size

### R5. No borders or containers
**Rule.** Nothing goes behind the logo: no circle, squircle, rounded square, coloured square, white
pill, card, or round crop. Just the logo on a see-through background.

**From.** M1: "I simply don't want a border on any of my apps".

**How.** Remove all of these:
- launcher shapes and app background colours
- white "stickers" (FBTO's pill) and dark squares (LSC)
- cards behind a symbol (QR Scanner)
- the round crop of a picture (games)
- black boxes behind a symbol (the Netflix N on Tomb of the Mask)

The shape only stays when it's part of the logo (R8).

**Examples.** Version 4 still has containers on:
- Marktplaats: tan square
- pump.fun: green circle
- LSC Smart Connect: dark square
- QR Scanner: blue card
- Video Watermark: blue card
- PAC-MAN 256: yellow strip

### R6. As big as possible
**Rule.** The logo fills the icon space.

**From.** M2: "the icons need to be max size, so scaled up to fit the available icon space".

**How.**
- The build trims the empty space and scales the drawing until its longest side fills the 256 px
  icon. So don't add margins.
- Don't leave far-away bits, like a shadow, a speck or a stray dot. They count as part of the
  drawing, so the logo ends up smaller.
- Keep layouts compact. A corner badge sits inside the main art's square, and two-line words are
  stacked tightly.
- Never stretch or squash a logo.

**Examples.**
- Parsec is far too small in version 4.
- Aptoide: "aptoide should be just the logo fullscreen" (M3).

### R7. Drawn clean, from scratch
**Update (Arch, 15 Nov):** "if there is a ready made logo, then it doesn't need to be remade from scratch".
A clean official ready-made logo is used as it is, with only the edits the rules need.

**Rule.** Every icon is a clean drawing. It has:
- no outline, halo or fringe around it
- no leftover bits of the old background
- no shadows from the old background
- no blur

**From.** M3: "a lot of apps have a little white line around them, I'm not going to name them one by
one but fix that". M3: "brave has a white excess from the background around it". M3: "Burger king
has cutout residue". M5: "some apps still have a slight outline, I want all apps redrawn from
scratch (except for Anker)".

**How.**
- The normal case is a vector SVG (see `ICON_PROCESS.md`). Pixel art follows R16.
- Holes and insides that only showed the old background become see-through. M3: "battery saver has
  black in the bell, should be transparent". The same goes for the inside of Google Home's house and
  the Camera's lens. The exception is when a rule says to fill them (fomo's pupils, ChatGPT's donut).
- Remove drop shadows and long flat shadows that belong to the old background (Invisible Widget,
  Video Maker, MiX, Automate).
- Never add an outline, glow or shadow to make something easier to see. Change its colour instead
  (R10).
- A line that's part of the real logo is not an outline, so keep it. Examples: Seal's line drawing,
  ChatGPT's knot, the edge of pump.fun's pill.

**Examples.**
- Brave and Burger King, from Arch's feedback.
- OSIRIS has a white and orange edge in version 4.
- Video Watermark has a light edge in version 4.

### R8. When the shape is part of the logo, keep it
**Rule.** Some logos are designed as a circle, ring or badge. Without the shape they look broken or
are hard to recognise. So the shape stays, drawn as part of the logo, as big as possible.

**From.** M5: "certain logos are simply designed to be round, and making them not round makes them
look bad or be unrecognisable, such as the steam logo. simply making the steam logo a circle with a
white shaft thing is good". M5 on Coolblue: "should have a small blue circle around it instead, seen
as that's the logo".

**How.** Ask: would the logo look broken, or be hard to recognise, without the shape? If yes, keep
it. A plain coloured circle behind a logo that works on its own is a container, so it goes. Brawl
Stars' ring went for that reason: "brawl stars should just be the brawl skull".

**Examples.**
- **Keep round:** Steam (blue gradient circle with the white piston); Coolblue (orange disc, white
  words, thin blue ring); Spotify; Chrome; GitHub's mark; Lime; Google Tasks; Google Clock;
  Background Eraser; Scrabble GO's tile; Mevo; Haptic feedback; GIF Maker's badge; Squib's round
  body.
- **Ring logos:** Canvas, Syncthing, LibreTorrent, Life360, Downmio, Find Hub.
- **Objects, not containers, even though they're square-ish:** Minecraft's grass block, Roblox's
  tilted square, the Bible book, Material Files' folder, Kraken Wallet's frame.
- **Circle dropped, because the mark is recognisable alone:** Facebook's f, Pinterest's P, Shazam's
  S (open question 4).

---

## C. Colour

### R9. A white logo on a coloured background takes that colour
**Rule.** If a logo is white and only works because of its coloured background, the logo becomes
that colour, gradient or pattern.

**From.** M2: "if an app relies on the background for its logo color and the actual logo is white,
make the logo whatever color the background was, (or gradient or pattern)".

**How.**
- Take the colour from `bg.png`, or use the official brand colour.
- Gradients and patterns move into the logo: bunq's rainbow stripes, AlleFolders' sunburst.
- This rule does NOT apply in these cases:
  - **Black, dark grey, grey or white backgrounds:** the logo stays white.
  - **White objects:** these follow R12.
- If the colour comes out too dark to see, go on to R10.

**Examples.** Beeper (blue), Bolt (green), Discord (blurple), Etsy (orange), Claude (orange), Twitch
(purple), ING (orange lion), Pinterest (red), Kraken (purple), TalkLife (purple), PostNL (orange),
Temu (orange), bunq (rainbow).

### R10. Dark or black logos get lighter, or white
**Rule.** Black, near-black and dark colours disappear on Arch's dark grey app drawer. Use a lighter
shade of the same colour, or white.

**From.** M5: "the bol logo isn't very readable, needs to be changed to a lighter blue, keep in mind
that dark or black logos are hard to read, so lightening the color or making it white would usually
be better". M3: "ChatGPT should have white infill like a donut shape".

**How.**
- **Black, near-black or grey logos:** make them white, and keep any coloured accents (like Nothing's
  red dot).
- **Dark brand colours** (navy, dark blue, dark teal, dark purple): first try a lighter shade of the
  same colour, like bol's lighter blue. Use white when a lighter shade still doesn't show up, or when
  the art is detailed (Library, Tools, ibisPaint's letters).
- **Black line drawings:** keep the lines black, but fill the drawing's own outer shape with white,
  like a donut. The hole in the middle stays see-through (ChatGPT, Seal).
- Small dark details inside a light shape are fine: Brawl Stars' eyes, Squib's eyes, fomo's pupils.

**Examples.**
- **Turned white:** Camera, Gallery, Glyph, Recorder, MotionEase, Weather (white with red), Lightyear,
  Amazon's "a", GitHub, X, Kraken Wallet, Cake Wallet's slice, hollandsnieuwe's letters, FBTO's
  letters.
- **Lighter shade:** PayPal's back P (brighter blue), bol, Vinted, Invisible Widget, Etsy Seller,
  TalkLife.

### R11. A dark logo on a bright background takes the bright colour
**Rule.** When a dark logo sits on a bright brand colour, the logo takes the bright colour. The dark
version would disappear.

**From.** Worked out from M2, from M5 on Marktplaats ("the marktplaats logo could be just the logo, in
the ugly marktplaats brown, because its not a logo with a bunch of colors"), and from the dark-logo
rule.

**How.** Use the bright background colour for the logo. Dark details that sit fully inside it can
stay dark.

**Examples.** Marktplaats (tan-brown mark), NS (yellow), Wise (lime green), BuzzKill (yellow bubble),
Essential Apps (yellow oval).

### R12. Things that really are white stay white
**Rule.** R9 is for flat logos. A character or object that really is white stays white: a white
chicken, a cloud, the moon, a skull, a sheet of paper.

**From.** M5: "the crossy road chicken is blue". Version 4 wrongly turned the white chicken blue. In
M1, Arch painted the Weather cloud and the Sleep Timer moon white.

**Examples.** Crossy Road's chicken, Weather's cloud, Sleep Timer's moon, Brawl Stars' skull, Seal's
body, Simple Text Editor's paper, Battery Sound, Tikget's arrow.

### R13. Exact brand colours
**Rule.** Use the brand's real colours. No darker, muddier, shifted or "off" versions, and no
recolouring unless a rule above asks for it.

**From.** M5: "Venice AI is the wrong color", "twitch is the wrong color", "Claude is off color", and
"the coolblue blue infill is not only inaccurate".

**How.**
- Look up the official colour codes (brand page, official SVG) and check them against the app's own
  icon.
- Flat colours by default. M9 (SoundCloud): "the icon doesn't need gradients". M10: "remove all the
  gradients from the Microsoft apps". So a logo's gradients become flat colours (pick the middle shade).
  A gradient stays only where Arch asks for one (Kraken Wallet "silver (not grey)", Kraken Pro "give it a
  sheen or something") or where he picked a logo file that has one (Beeper's older logo).
- When the installed icon and what Arch expects differ, go with Arch. For example, Venice is red.

**Examples.** Claude #D97757 (flat), Twitch #9146FF, Venice red #DD3300, Coolblue #FF6600 and
#0090E3, Steam's blue gradient, the Microsoft apps (flat since M10), bol #3366FF (M10: "less faded and more
of an average app blue, maybe a bit richer"), bunq's full 12-colour stripe range (M11).

---

## D. What to show

### R14. Words and wordmarks
**Rule.**
- A logo with a symbol and words keeps only the symbol.
- A logo that is a word keeps only the word: no pill, no box, no extra picture.
- If a word is too small to read, use the brand's symbol if it has one. If not, make the word as
  big as possible.

**From.** M3: "Anker is not readable, replace it with the Anker a with the lightning bolt". M3:
"Amazon is missing its a". M5: "FBTO should be the word only", "justwatch needs the text removed",
"uw zorg online could just be the uwzorg plus logo".

**How.**
- Drop taglines and small second words, like "college" under Deltion.
- An extra picture that only describes the service goes too, like FBTO's medical case or LSC's
  house.
- Game titles follow R15.

**Examples.**
- **Symbol only:** JustWatch, Uw Zorg Online, Anker, ING (no "NL").
- **Word only:** FBTO, bol, Bolt, Etsy, DigiD, bunq, Anvil-MC, hollandsnieuwe, LSC, OSIRIS, My DHL.
- **Word and picture form one logo, so both stay:** Burger King, Coolblue, PostNL, Temu, Amazon's
  "a" and smile, Kraken Pro's PRO tag.

### R15. Games and picture icons: the main subject only
**Rule.** For games and other picture icons, show only the main character or object, cut out
cleanly. No scene, no background, no round crop, no title, no publisher badge.

**From.** M3: "balatro should just be the balatro clown", "brawl stars should just be the brawl
skull". M5: "tomb of the mask netflix edition should be the little yellow guy full size".

**How.**
- Pick the one thing people recognise, usually a face or a character.
- If the original crops it, find full official art elsewhere.
- Drop game title lettering and publisher badges (like Gameloft's), unless the lettering is the logo.

**Examples.** Balatro (the Joker), Brawl Stars (the star badge with the skull: M10 "change the brawl stars
logo to the image provided" replaces the skull-only version), Tomb of the Mask (the yellow guy),
Crossy Road (the chicken), Angry Crusher Catapult (the bird in the slingshot), Plants vs. Zombies
(the zombie), My Little Pony (Twilight Sparkle), MuseDash (the girl's head), Top Lords (the king),
PAC-MAN 256 (Pac-Man), Minecraft (the grass block).

### R16. Pixel art stays sharp and straight
**Rule.** Pixel art stays on its pixel grid: straight (never rotated), with hard edges, scaled up by
whole numbers, never blurry.

**From.** M5: "balatro has rotated pixel art which is blurry, should be sharp pixel art, and
straight, cus its pixel art, redrawing as pixel art if you can't edit it to be as such".

**How.**
- Deliver it as `<name>.pixel.png` at its native size, so one image pixel is one art pixel. The build
  scales it up without smoothing.
- If the source is rotated or blurry, rebuild it pixel by pixel.
- Anything added to a pixel-art icon, like the Netflix N, is drawn as pixel art on the same grid.
- Blocky 3D art that isn't pixel art (Crossy Road) is drawn as clean flat shapes instead.

**Examples.** Balatro, Minecraft, PAC-MAN 256, Tomb of the Mask.

### R17. Generic icons that look bad: simplify and strengthen
**Rule.** A generic or niche app with a weak or fussy icon can be simplified: fewer, thicker, bolder
parts, keeping the same idea and colours.

**From.** M1: "you could fix icons of generic apps that look bad, like the sleep timer one that I
redrew, or soundcloud, it has 7 stripes but it could be 4 or 5, and have them be thicker and the icon
be orange so it looks better". M3: "soundclouds logo looks awful, just redesign it from scratch, make
the lines on the left and a half cloud on the right".

**How.**
- Only do this where Arch asked, or for generic niche apps whose logo is too thin or fussy at phone
  size.
- Big brands keep their real logo (R2).
- Thin lines that disappear at 48 px may be thickened a little.

**Examples.** SoundCloud (4 thick orange bars and a half cloud), Sleep Timer (big white moon), PostNL
(lines thick enough to see), Battery Sound, UnApp.

---

## E. Special groups

### R18. Google apps: each one recognisable by its own colour and shape
**Rule.** Google's newer icons use the same four colours for everything, so they blur together.
Each Google app must be recognisable by its own main colour and shape, like the older designs. Where
the current icon is the four-colour or gradient look that blends in with the rest, use the older
single-colour design.

**From.** M5: "Google apps used to be clear what the difference is, then they sent to using the same
colours for all of them, now it's slightly better but still not what it used to be, like the maps
icob should be red, and the gmail should have the red line and stuff". Also M5: "Google translate
looks good".

**How.**

| App | Design to use |
|---|---|
| Gmail | M10: the 2020 "M" shape in two reds (sides dark red #C5221F, the V red #EA4335) on a white envelope rectangle |
| Maps | M10: the 2020 pin with the same sections, in two reds (the band through the hole dark red, the rest red) |
| Calendar | Blue page with a white "31" |
| Docs / Sheets / Slides | Blue / green / yellow page or tile with white marks (Sheets' grid uses the official, even measurements: M10 "the grid is off") |
| Drive | The triangle in three colours: green, yellow, blue |
| Translate | Version 4, exactly (Arch likes it) |
| Keep Notes | Yellow light bulb |
| Meet | Yellow video camera |
| Files | Blue file with the coloured folded corner (Arch painted it) |
| Home | Four-colour house outline, see-through inside (Arch painted it); corners as round as the Play Store's (M10, see R27) |
| Play Store | The current four-colour play triangle with its very rounded corners (M10: copy of the picture Arch sent) |
| Play Games | The current logo: green play triangle with the mint controller (M10: "fix the play games logo") |
| Photos, Chrome, Google G, Assistant, Wallet, Gemini, Find Hub, Safety | Their own well-known shapes are already distinct: keep them |
| Phone, Contacts, Messages, Clock, Tasks | All blue, but each has its own shape: keep them |

Draw all the Google apps side by side (R26), and check that none of them can be mixed up at 48 px.

### R19. Netflix editions: a small Netflix "N" in the top-left corner
**Rule.** An app that is the Netflix version of a game or app shows that app's own main art, full
size, with a small Netflix "N" in the top-left corner.

**From.** M5: "tomb of the mask netflix edition should be the little yellow guy full size, with a
small netflix logo in the corner to indicate its a netflix version of an app". Also M5: "e.g. all
netflix version apps get a small neflix logo in the corner".

**How.**
- **Which apps:** packages starting with `com.netflix.NGP` (Netflix Games), or apps that say they're
  a Netflix edition. The Netflix app itself is just the N.
- **The N:** Netflix red #E50914 with its darker shading, about 30–35% of the icon's width, in the
  top-left corner, with no black box behind it.
- **Why top-left:** Netflix puts it there on its own game icons. Launchers use the top-right corner
  for notification dots and the bottom-right for system badges.
- **Placement:** keep the N inside the main art's square, so it doesn't make the art smaller (R6). A
  small see-through gap between the N and the art is fine.
- **Pixel art:** on a pixel-art icon, draw the N as pixel art too (R16).

**Examples.** Tomb of the Mask (the only one right now).

### R20. Other variants and editions
**Rule.** Different versions of an app keep whatever makes them different, using the same logic as
the Netflix rule.

**From.** Worked out from M5's Netflix example and "build some type of logic for it". M7 for dev
builds.

**How.**
- **Mods and alternative apps that have their own icon keep it:** DISCXRD's rainbow, Revenge's red,
  Revenge Manager's mask.
- **Pro, unlocker and Lite versions keep the badge their real icon has, in the same place:** SD Maid
  Pro's key (bottom-right), TikTok Lite's lightning bolt (top-left), Kraken Pro's PRO tag.
- **Sister apps from one brand keep their own colour or mark:** Etsy is orange and Etsy Seller is
  blue; the same goes for the Kraken and Proton families.
- **If a version would look exactly like its main app,** add a small badge of the version's own mark
  in the top-left corner, like the Netflix N. No app needs this right now.
- **Dev or test builds Arch is trying out:** leave them as they are (Terra Defense, R23).

### R21. Disguised icons: show the real logo
**Rule (changed by Arch's answer 2).** If an app is using a disguise icon, show the app's real logo
instead. Mullvad VPN shows its own mole, simplified and cleaner ("their logo kinda sucks").
The text below is the old version of this rule, kept for history.

**From.** Worked out from M1. Arch's home screen shows Mullvad VPN disguised as "Whack-a-mole"; he
wrote "VPN" on it and said "I gave up on the vpn".

**How.** Treat the disguise picture as the logo: no circle, big, clean. Don't add Mullvad's mole, a
shield or the word "VPN".

**Examples.** Whack-a-mole: the mallet and the yellow miner's helmet. See open question 2.

### R22. Arch's own app keeps his design
**Rule.** Squib is Arch's own app. Keep his design (the orange blob with two square eyes). Only remove
the launcher shape, draw it clean, and make it as big as possible.

**From.** M1's painted home screen, where it stays the orange blob.

**How.** The round body is the character, not a container (R8). Don't restyle it or add details.

### R23. Fixed exceptions
- **Anker soundcore:** keep the version 4 icon, the real Anker "A" with the lightning bolt. M5: "I
  want all apps redrawn from scratch (except for Anker)". Status "keep".
- **RoboTD Mobile [01]:** keep the version 4 icon (Arch's answer 3: an exception, not the standard).
- **Terra Defense (the 4 dev builds):** keep the version 4 icons. M7: "btw the terra defense unity
  games, you can skip those, those are dev games, they are fine as is right now v4". Status "keep".
  Don't try to tell the four apart.

---

## F. Checking

### R24. Easy to see on both of Arch's backgrounds, at phone size
**Rule.** Every icon must be easy to see at real phone size (about 48 px) on the dark grey app
drawer (#2F2F2F), because every app is there. Apps on his home screen must also be easy to see on
the blue-grey wallpaper (#4F7D95). All the others should be too, since he might move them there.

**From.** M5: dark logos are hard to read, and bol isn't readable. The backgrounds and the home screen
apps come from his screenshots.

**How.**
- Check with `tools/preview_icon.py`. It shows both backgrounds, at 48 px too.
- **Dark grey hides:** black, navy, dark teal, dark purple and dark green.
- **Blue-grey hides:** mid blues, teals and greys. In a test, a mid blue like #4466FF nearly
  vanished on it, while #7F9CFF was easy to see on both backgrounds.
- **If the two backgrounds disagree,** the dark drawer wins, except for home screen apps.
- **Thin lines** disappear at 48 px. Thicken them a little without changing the logo.
- **Fix it with colour** (R10), never with outlines, glows or shadows (R7).

**Home screen apps** (from his screenshot): Squib, Files, Play Store, Whack-a-mole, Weather, Home,
Sleep Timer, and in the dock Beeper, Brave, SoundCloud, Phone and Camera. Beeper's blue on the
wallpaper is fine, because Arch painted it that way himself.

### R25. Lookalikes: generic niche apps must be easy to tell apart
**Rule.** A generic or niche app must not be mistaken for another app in the pack. Big brands that
happen to look alike are left alone.

**From.** M5: "is this icon distinguishable from other apps". M6: "TBH the confusion part is kinda
meant for more generic niche apps that have generic icons, if 2 big apps have similar icons there
isn't much you can do".

**How.** Compare at 48 px against the whole pack (the contact sheets in `/home/claude/work`). If a
niche app looks like another app, set it apart using its own real colours or details. Never invent a
logo.

**Examples.**
- **Set apart:**
  - Your Calendar Widget (white page) vs Google Calendar (blue page)
  - Tikget (white arrow) vs Pinget (red arrow)
  - Calorie Calculator (white fork and spoon) vs FairTrack (green crossed knife and spoon)
  - KDE Connect vs Orientation Control: both are blue phones, but KDE has a dark screen and the K,
    while Orientation Control has turn arrows and a gear
  - Alarm Clock Xtreme (bells and legs) vs Google Clock
- **Left alone:** Discord and Revenge; the Messages, Signal and Beeper bubbles; Etsy and Etsy Seller.

### R26. Families are drawn side by side
**Rule.** Apps from one company or group are designed together, so they get the same treatment and
each one still stands out.

**From.** Worked out from M5 (the Google feedback and the lookalike question).

**Families:**
- **Google:** see R18.
- **Nothing:** Camera, Gallery, Glyph, Recorder, Weather and Essential Apps, plus MotionEase, which
  uses the same dot style. They're white with red accents.
- **Kraken:** Kraken, Krak, Kraken Pro, Kraken Wallet.
- **Proton:** Proton Mail, Proton Pass, Proton Authenticator.
- **Microsoft:** Authenticator, Outlook, Teams, SwiftKey.
- **Discord and its mods:** Discord, DISCXRD, Revenge, Revenge Manager.
- **Etsy:** Etsy, Etsy Seller.
- **SD Maid:** SD Maid, SD Maid Pro.
- **Church apps:** Library, Tools.
- **TikTok, Pinterest and the downloaders:** TikTok Lite, Tik Saver, Tikget, Pinterest, Pinget,
  Downmio, Seal.

---

### R27. Corners that should look equally round get the same curve length, not the same radius
**Rule.** When Arch asks for corners "as rounded as" another icon, match how round they look, not the
number. A sharp corner and a wide (blunt) corner with the same radius look different: the sharp one looks
much rounder. So give every corner the same length of curve (at icon size) as the corners it should match.

**From.** M10: "make the Google home corners as rounded as the Google store (in terms of optical roundness,
not software rounding values cus a sharp and weak corner with the same rounding on each look like
different rounding".

**How.** Measure the reference corners' radius and angle, work out their curve length at icon size, then
pick each new corner's radius so its curve is just as long: a right angle needs a bigger radius than a
sharp 60° corner, and a wide 135° corner a much bigger one.

**Examples.** Google Home (right-angle corners radius 69, eaves 138, to match the Play Store's 60° corners).

### R28. A picture Arch sends is the thing to copy
**Rule.** When Arch sends a picture with "like this" or "change it to this", copy that picture's shapes
as exactly as possible (measure them), then apply only the changes he asked for (colours, no border).

**From.** M10: "copy the shape of the image provided and make it aptoide orange", "change the brawl stars
logo to the image provided", "like the provided image but without the grey border", "change the google
store logo to the image provided".

**How.** Measure the picture (edges, circles, corners), draw clean shapes from the measurements, and
check by laying the drawing over the picture until they match. Fix obvious flaws in the picture only when
they're clearly not part of the design.

**Examples.** Aptoide, Brawl Stars, PostNL, Play Store, Maps and Gmail (sections and shape), DHL (a clean
version of his sketch).

---

## Checklist before delivering an icon

- [ ] It's the real logo, found online and recognisable at a glance (R2, R3).
- [ ] No container is left, unless the shape is part of the logo (R5, R8).
- [ ] It fills the icon, with no stray bits that make it smaller (R6).
- [ ] Clean edges: no outline, halo, leftover background or shadow. Holes are see-through (R7).
- [ ] The colours follow R9 to R13, with the real brand values.
- [ ] Words follow R14. Games and pictures show only the main subject (R15). Pixel art is sharp and
      straight (R16).
- [ ] Special groups are handled (R18 to R23).
- [ ] It's easy to see at 48 px on dark grey, and on blue-grey too (R24).
- [ ] It can't be mixed up with another niche app (R25), and it fits its family (R26).
- [ ] It matches the app's `direction` and `arch_said` in `app_notes.json`.
- [ ] Colours are flat unless Arch asked for a gradient (R13). Matched corners look equally round (R27).

---

## Arch's answers to the open questions (15 Nov 2026)

1. **Black icons:** "inverting the color to where the outer layer is white should probably do fine, white
   camera with a black lens and a red dot, etc". So for black icons (Camera, Gallery, Glyph, Recorder,
   MotionEase): swap black and white: the outer shape white, inner parts that were white become black,
   red accents stay. This replaces the "see-through lens" default in R4/R7/R10 for these apps.
2. **Mullvad VPN (Whack-a-mole):** "mullvad show the original logo, but simplify and beutify it a bit, their
   logo kinda sucks". So R21 changes: show the app's real logo, not the disguise. For Mullvad: its mole,
   simplified and cleaner.
3. **RoboTD Mobile:** "yeah just leave those, but it's an exception, not the standard". Kept like Terra
   Defense (R23). The standard is still to redraw everything.
4. **Round badges:** "steam in particular is build around the circle logo, it looks very nice, Pinterest
   would look nice as standalone red P, same with the others, steam is a bit of an edge case". So Steam
   keeps its circle; Pinterest (red P), Facebook and Shazam drop theirs. Keeping a circle is the
   exception (R8), not the default.
5. **Sleep Timer:** "moon and a clock instead of cloud would be nice". Big white moon plus a small clock.
