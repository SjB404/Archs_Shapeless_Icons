# Arch's Shapeless Icons

Android icon pack with no border shapes: just each app's logo, as big as possible.

- How icons are made (the repeatable plan): `docs/ICON_PROCESS.md`
- Arch's design rules: `docs/DESIGN_RULES.md`; per-app directions: `docs/app_notes.json`
  (Arch's own words about a specific app beat the general rules)
- Icon sources: `icons/src/<drawable>.svg` (or `.pixel.png` for pixel art, `.png` as last resort)
- Notes per icon (sources, decisions): `icons/notes/<drawable>.md`
- Build: `python3 tools/build_icons.py && python3 tools/build_pack.py`, raise the number in `version.txt`
  (always by 0.1, big release or small: 9.8, 9.9, 10.0, 10.1), then push; GitHub Actions
  builds the APK and publishes it under Releases as v<version>. A push that doesn't raise the number builds nothing
  (and doesn't fail), so an old release is never overwritten.
- Arch isn't a designer or a specialist: explain things in plain everyday words.
- Never remove an icon from the pack, even when the app is gone from Arch's phone or an export no longer lists it.
  When an app's activity name changes, add the new name and keep the old one.
- Before working on icons, always show Arch an image list of them first (own icons with labels), and wait.
- When new icons are made, show Arch a picture of each app's own icon (before) next to the Shapeless icon (after),
  both on the dark grey background only.
