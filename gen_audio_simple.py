#!/usr/bin/env python3
"""Generate audio for poems 63-300 using edge_tts."""
import asyncio
import json
import os
import subprocess
import sys

VOICE = "zh-CN-YunxiNeural"
RATE = "-10%"
OUTPUT_DIR = "audio"
POEMS_FILE = "app/src/main/assets/poems.json"
TOTAL = 238  # poems 63-300

# Load all poems
with open(POEMS_FILE, "r", encoding="utf-8") as f:
    poems = json.load(f)

# Build a map of id -> poem
poem_map = {p["id"]: p for p in poems}

os.makedirs(OUTPUT_DIR, exist_ok=True)

# Check what's already done
done = set()
for f in os.listdir(OUTPUT_DIR):
    if f.endswith(".mp3") and f.startswith("poem_"):
        try:
            pid = int(f.replace("poem_", "").replace(".mp3", ""))
            sz = os.path.getsize(os.path.join(OUTPUT_DIR, f))
            if sz > 1000:
                done.add(pid)
        except:
            pass

print(f"Already done: {len(done)} files")
needed = [i for i in range(63, 301) if i not in done]
print(f"Need to generate: {len(needed)} files")
print(f"Now generating...")

ok = 0
fail = 0

for pid in needed:
    p = poem_map.get(pid)
    if not p:
        print(f"  SKIP poem {pid} - no data")
        continue

    title = p["title"]
    content = p["content"].replace("\n", "，")
    text = f"《{title}》，{p['author']}。{content}"
    fname = f"poem_{pid:03d}.mp3"
    fpath = os.path.join(OUTPUT_DIR, fname)

    try:
        proc = subprocess.run(
            ["edge-tts", "--voice", VOICE, f"--rate={RATE}", "--write-media", fpath, "--text", text],
            capture_output=True, text=True, timeout=30
        )
        if proc.returncode == 0 and os.path.exists(fpath) and os.path.getsize(fpath) > 1000:
            sz = os.path.getsize(fpath) // 1024
            ok += 1
            print(f"  OK   {fname} ({title}, {sz}KB) [{ok}/{len(needed)}]")
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
print(f"Total audio files: {len(done) + ok}")
