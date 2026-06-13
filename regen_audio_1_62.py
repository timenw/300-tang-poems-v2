#!/usr/bin/env python3
"""Regenerate audio for poems 1-62 with correct ID mapping."""
import json
import os
import subprocess

VOICE = "zh-CN-YunxiNeural"
RATE = "-10%"
OUTPUT_DIR = "audio"
POEMS_FILE = "app/src/main/assets/poems.json"

with open(POEMS_FILE, "r", encoding="utf-8") as f:
    poems = json.load(f)

poem_map = {p["id"]: p for p in poems}

print("First 62 poems in JSON order:")
for p in poems[:62]:
    print(f"  id={p['id']:3d}  title={p['title']}  author={p['author']}")

print(f"\nRegenerating audio for poems 1-62...")

ok = 0
fail = 0

for pid in range(1, 63):
    p = poem_map.get(pid)
    if not p:
        print(f"  FAIL poem_{pid:03d} - no poem data!")
        fail += 1
        continue

    title = p["title"]
    content = p["content"].replace("\n", "，")
    text = f"《{title}》，{p['author']}。{content}"
    fname = f"poem_{pid:03d}.mp3"
    fpath = os.path.join(OUTPUT_DIR, fname)

    try:
        proc = subprocess.run(
            ["edge-tts", "--voice", VOICE, f"--rate={RATE}", "--write-media", fpath, "--text", text],
            capture_output=True, text=True, timeout=60
        )
        if proc.returncode == 0 and os.path.exists(fpath) and os.path.getsize(fpath) > 1000:
            sz = os.path.getsize(fpath) // 1024
            ok += 1
            print(f"  OK   {fname} ({title} by {p['author']}, {sz}KB)")
        else:
            fail += 1
            err = proc.stderr[:100] if proc.stderr else "unknown"
            print(f"  FAIL {fname} ({title}): {err}")
    except subprocess.TimeoutExpired:
        fail += 1
        print(f"  FAIL {fname} (timeout)")
    except Exception as e:
        fail += 1
        print(f"  FAIL {fname} ({title}): {e}")

print(f"\nDone: {ok} generated, {fail} failed")
