#!/usr/bin/env python3
"""Retry failed audio generation for specific poem IDs."""
import json
import os
import subprocess

VOICE = "zh-CN-YunxiNeural"
RATE = "-10%"
OUTPUT_DIR = "audio"
POEMS_FILE = "app/src/main/assets/poems.json"
RETRY_IDS = [75, 300]

with open(POEMS_FILE, "r", encoding="utf-8") as f:
    poems = json.load(f)

poem_map = {p["id"]: p for p in poems}

for pid in RETRY_IDS:
    p = poem_map[pid]
    title = p["title"]
    content = p["content"].replace("\n", "，")
    text = f"《{title}》，{p['author']}。{content}"
    fname = f"poem_{pid:03d}.mp3"
    fpath = os.path.join(OUTPUT_DIR, fname)
    
    print(f"Generating {fname} ({title}, {len(text)} chars)...")
    try:
        proc = subprocess.run(
            ["edge-tts", "--voice", VOICE, f"--rate={RATE}", "--write-media", fpath, "--text", text],
            capture_output=True, text=True, timeout=60
        )
        if proc.returncode == 0 and os.path.exists(fpath) and os.path.getsize(fpath) > 1000:
            sz = os.path.getsize(fpath) // 1024
            print(f"  OK {fname} ({sz}KB)")
        else:
            print(f"  FAIL: {proc.stderr[:200]}")
    except Exception as e:
        print(f"  FAIL: {e}")

# Count total
count = len([f for f in os.listdir(OUTPUT_DIR) if f.endswith('.mp3') and os.path.getsize(os.path.join(OUTPUT_DIR, f)) > 1000])
print(f"\nTotal audio files: {count}/300")
