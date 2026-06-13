#!/usr/bin/env python3
"""Regenerate ALL 300 poems' audio from scratch using current poems.json."""
import json
import os
import subprocess
import time

VOICE = "zh-CN-YunxiNeural"
RATE = "-10%"
OUTPUT_DIR = "audio_regen"
POEMS_FILE = "app/src/main/assets/poems.json"

with open(POEMS_FILE, "r", encoding="utf-8") as f:
    poems = json.load(f)

print(f"Loaded {len(poems)} poems from {POEMS_FILE}")
print(f"ID range: {poems[0]['id']} - {poems[-1]['id']}")

os.makedirs(OUTPUT_DIR, exist_ok=True)

ok = 0
fail = 0
fail_list = []

for p in poems:
    pid = p["id"]
    title = p["title"]
    author = p["author"]
    content = p["content"].replace("\n", "，")
    text = f"《{title}》，{author}。{content}"
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
            print(f"  OK   {fname} ({title} by {author}, {sz}KB) [{ok}/{len(poems)}]")
        else:
            fail += 1
            fail_list.append(pid)
            err = proc.stderr[:80] if proc.stderr else "unknown"
            print(f"  FAIL {fname} ({title}): {err}")
    except subprocess.TimeoutExpired:
        fail += 1
        fail_list.append(pid)
        print(f"  FAIL {fname} (timeout)")
    except Exception as e:
        fail += 1
        fail_list.append(pid)
        print(f"  FAIL {fname} ({title}): {e}")

print(f"\n{'='*50}")
print(f"Done: {ok} generated, {fail} failed out of {len(poems)}")
if fail_list:
    print(f"Failed IDs: {fail_list}")
print(f"Output dir: {OUTPUT_DIR}")
