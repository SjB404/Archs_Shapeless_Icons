# Arch's Shapeless Icons

Android icon pack with no border shapes: just each app's logo, as big as possible.

- How icons are made (the repeatable plan): `docs/ICON_PROCESS.md`
- Arch's design rules: `docs/DESIGN_RULES.md`; per-app directions: `docs/app_notes.json`
  (Arch's own words about a specific app beat the general rules)
- Icon sources: `icons/src/<drawable>.svg` (or `.pixel.png` for pixel art, `.png` as last resort)
- Notes per icon (sources, decisions): `icons/notes/<drawable>.md`
- Build: `python3 tools/build_icons.py && python3 tools/build_pack.py`, raise the number in `version.txt`
  (whole number for a big release: 9; point number for a small one: 8.1, 8.2), then push; GitHub Actions
  builds the APK and publishes it under Releases as v<version>. It refuses a number that's already released.
- Arch isn't a designer or a specialist: explain things in plain everyday words.
