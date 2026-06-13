#!/usr/bin/env python3
"""Check the order of first 62 poems and compare with audio file timestamps."""
import json
import os
import time

POEMS_FILE = "app/src/main/assets/poems.json"
AUDIO_DIR = "app/src/main/assets/audio"

with open(POEMS_FILE, "r", encoding="utf-8") as f:
    poems = json.load(f)

print("First 62 poems in current poems.json:")
for i, p in enumerate(poems[:62]):
    fname = f"poem_{p['id']:03d}.mp3"
    fpath = os.path.join(AUDIO_DIR, fname)
    mtime = os.path.getmtime(fpath) if os.path.exists(fpath) else 0
    ts = time.strftime("%m-%d %H:%M:%S", time.localtime(mtime)) if mtime else "MISSING"
    print(f"  [{i+1:2d}] id={p['id']:3d}  {p['title']:20s}  by {p['author']:10s}  audio={fname}  mtime={ts}")

# Check if IDs are sequential
ids = [p['id'] for p in poems[:62]]
print(f"\nIDs: {ids[:10]}...{ids[-10:]}")
print(f"Sequential 1-62? {ids == list(range(1,63))}")

# Check poem 164
print(f"\nPoem at index 163 (0-based):")
if len(poems) > 163:
    p = poems[163]
    print(f"  id={p['id']}, title={p['title']}, author={p['author']}")
