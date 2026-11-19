# Arch's Shapeless Icons

An Android icon pack with no circles, squircles or other border shapes: just the logo, bigger.

## Install

Download the newest APK from [Releases](../../releases/latest), install it, then pick **Shapeless Icons** in your launcher's icon pack setting.

## The app

- **Search bar and icon list**: every icon in the pack, plus the apps on the phone that don't have one yet.
  The buttons under the search bar show all of them, only the apps on this phone, or only the ones still missing an icon.
- **Compare**: tap an icon to see it next to the app's own icon. The compare button next to the search bar
  does this for the whole list. The app's own icon is read from the installed app itself, so it's never an old copy.
- **Background**: switches between the app drawer grey and see-through, so the icons show on your real wallpaper.
- **Pick and copy**: tap **Select** (bottom right) or long-press an icon, tap the apps you want, then tap the
  check button: their names are copied one per line, ready to paste in a chat. The gear sets what's copied
  (names, package names, icons). Icons can't go on the clipboard with text, so with an icon switched on the
  share menu also opens with one picture of all the picked icons.
- **Orange mark**: the app changed since its icon was made (it starts through a different entry now),
  so the pack doesn't recognise it until the icon is updated.

## Requesting icons

Open the app, tap **Export** and pick **Apps without an icon** (or **All apps**). It saves a zip to Downloads
with each app's icon split into layers:

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
- `tools/preview_icon.py` and `tools/review_sheets.py` show icons on Arch's app drawer grey and a sample wallpaper
  colour (his real wallpaper changes; the app's see-through background shows the real one).
- Every push to `main` builds a new APK and puts it in Releases, named after `version.txt`.
