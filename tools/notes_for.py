#!/usr/bin/env python3
"""Prints the app_notes.json entries for the given drawable names only (saves reading the whole file).

    python3 tools/notes_for.py com_soundcloud_android com_spotify_music
"""
import json
import sys
from pathlib import Path

notes = json.loads((Path(__file__).resolve().parent.parent / "docs" / "app_notes.json").read_text())
for name in sys.argv[1:]:
    print(f"== {name}")
    print(json.dumps(notes.get(name, "NOT FOUND"), indent=1, ensure_ascii=False))
