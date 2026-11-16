# Coolblue (`eu_coolblue_shop`)

**Now:** the real Coolblue logo: the orange disc with "cool" over "blue" in white letters, with a
thin Coolblue-blue ring around the disc. Nothing else behind it. The whole round logo fills the icon.

## What Arch said
- Version 4: "the coolblue blue infill is not only inaccurate but is also filled in bad, the cool lie
  logo should have a small blue circle around it instead, seen as that's the logo"

Version 4 had painted the letters blue (the "white logo takes the background colour" rule applied by
mistake), and the blue letters had rough, blotchy edges. Now the letters are white again, as in the
real logo, and the blue is a ring around the disc.

## Sources
- Letters and disc: Coolblue's own logo drawing, taken from the top of their company site
  https://aboutcoolblue.com/en/ (copy: `/home/claude/refs/eu_coolblue_shop/web/about_nav_logo.svg`).
  The letter shapes are copied exactly, not traced.
- Cross-checked against three other copies of the same logo. All have the same letters, sitting
  in the same place in the disc (centred, the words as wide as 1.25 times the disc's radius):
  - the logo at the top of https://www.coolblue.nl/ (`web/coolblue_nl_header_logo.svg`)
  - the big logo on https://aboutcoolblue.com/en/ (`web/about_pageheader_logo.svg`)
  - https://logotyp.us/file/coolblue.svg (`web/logotypus_coolblue.svg`)
- Colours: Coolblue's own website style sheet
  (https://assets.coolblue.nl/_app/pre-cart-microfrontend/_next/static/chunks/39rsd7kc6yupk.css)
  names them "hot orange" #FF6600 and "coolblue" #0090E3. These are exactly the colours in the
  app's own icon on Arch's phone (`fg.png`: orange disc with white letters; `bg.png`: #0090E3) and
  in the Play Store icon.
- Also looked at: the App Store icon (`web/ios_appicon_512.png`), the site's home-screen icon
  (`web/apple-touch-icon.version1.png`) and https://logowik.com/content/uploads/images/coolblue3973.jpg.

## What I did
- Drew three shapes: a blue disc, the orange disc on top of it, and the white letters on top of
  that. Stacking them (instead of cutting a ring shape) means there's no thin see-through seam
  where the orange meets the blue.
- The official drawing has the second "o" and the "e" twice, on top of each other. I kept them
  once, so their edges aren't heavier than the other letters.
- Ring width: 1/8 of the logo's radius (orange disc = 87.5% of the full width). That's about 16 px
  on the 256 px icon and about 3 px at real phone size: thin, but clearly visible on both the dark
  drawer and the blue-grey wallpaper.

## Different from the app's real icon
- The app's own icon is the orange disc on a blue background. Your launcher cuts that into a
  circle, so on your phone it looks like an orange disc in a thick blue ring (the ring is about
  22% of the radius there). Here the ring is thinner, as you asked for a "small" one, which also
  makes the disc and the letters bigger.
- The Play Store and App Store icons show the disc pushed into the top-left corner of a blue
  square. That square is the background, so it's not used.

## For Arch to look at
- Coolblue's own logo (on both their websites and in every copy I found) is only the orange disc
  with white letters. They often put it on their blue, but I found no official version with a
  ring. The ring is here because you asked for it, and it's how the app looks on your phone.
- If the ring looks too thin or too thick on your phone, it's one number to change: anything from
  the thin ring now to the thicker ring your launcher shows today.
