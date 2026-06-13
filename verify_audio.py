#!/usr/bin/env python3
"""Verify audio matches poem content by regenerating and comparing."""
import json
import os
import subprocess
import hashlib

POEMS_FILE = "app/src/main/assets/poems.json"
AUDIO_DIR = "app/src/main/assets/audio"
VERIFY_DIR = "/tmp/verify_audio"

os.makedirs(VERIFY_DIR, exist_ok=True)

with open(POEMS_FILE, "r", encoding="utf-8") as f:
    poems = json.load(f)

poem_map = {p["id"]: p for p in poems}

# Pick a few poems to verify
test_ids = [1, 2, 3, 10, 50]

print("Verifying audio content matches poem text:")
for pid in test_ids:
    p = poem_map.get(pid)
    if not p:
        print(f"  id={pid}: NOT FOUND in poems.json")
        continue
    
    title = p["title"]
    content = p["content"].replace("\n", "，")
    text = f"《{title}》，{p['author']}。{content}"
    
    # Generate fresh audio
    fpath = os.path.join(VERIFY_DIR, f"verify_{pid:03d}.mp3")
    try:
        proc = subprocess.run(
            ["edge-tts", "--voice", "zh-CN-YunxiNeural", "--rate=-10%", "--write-media", fpath, "--text", text],
            capture_output=True, text=True, timeout=30
        )
        if proc.returncode != 0:
            print(f"  id={pid} ({title}): TTS failed")
            continue
        
        # Compare with existing
        existing = os.path.join(AUDIO_DIR, f"poem_{pid:03d}.mp3")
        if os.path.exists(existing):
            with open(fpath, "rb") as f1, open(existing, "rb") as f2:
                h1 = hashlib.md5(f1.read()).hexdigest()
                h2 = hashlib.md5(f2.read()).hexdigest()
            match = "MATCH" if h1 == h2 else "MISMATCH"
            print(f"  id={pid:3d} ({title:20s}): {match}  md5={h1[:8]}.. vs {h2[:8]}..")
        else:
            print(f"  id={pid} ({title}): existing audio MISSING")
    except Exception as e:
        print(f"  id={pid} ({title}): error {e}")

# Clean up
import shutil
shutil.rmtree(VERIFY_DIR, ignore_errors=True)
