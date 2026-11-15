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

- `icons/png/<name>.png`: the icon images (edit these)
- `icons/appfilter.json`: which app each image belongs to
- `tools/build_pack.py`: turns those into the files launchers read
- Every push to `main` builds a new APK and puts it in Releases.
