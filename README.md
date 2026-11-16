# Arch's Shapeless Icons

An Android icon pack with no circles, squircles or other border shapes: just the logo, bigger.

## Install

Download the newest APK from [Releases](../../releases/latest), install it, then pick **Shapeless Icons** in your launcher's icon pack setting.

## Requesting icons

Open the app and tap **Export**. It saves a zip to Downloads with every app's icon split into layers:

| File | What it is |
|---|---|
| `full.png` | the icon as the launcher shows it now |
| `fg.png` | the logo layer on its own |
| `bg.png` | the background layer on its own |
| `mono.png` | one-colour version (only if the app has one) |
| `legacy.png` | old-style icon with no layers |
| `apps.json` | app names and the IDs the pack needs to match icons to apps |

## How the pack is built

- `icons/src/<name>.svg`: each icon's clean drawing (`.pixel.png` for pixel art, `.png` only when a drawing
  couldn't do the art justice). `icons/notes/<name>.md` says where each logo came from and why it looks the way it does.
- `docs/ICON_PROCESS.md`: the step-by-step method used for every icon, and the routine for a full pass.
- `docs/DESIGN_RULES.md`: Arch's design rules; `docs/app_notes.json`: the direction for each app.
- `docs/IDEAS.md`: ideas for later.
- `tools/build_icons.py` renders the drawings into `icons/png/`; `tools/build_pack.py` turns those into the
  files launchers read (`icons/appfilter.json` says which image belongs to which app).
- `tools/preview_icon.py` and `tools/review_sheets.py` show icons on Arch's wallpaper and app drawer colours.
- Every push to `main` builds a new APK and puts it in Releases.
