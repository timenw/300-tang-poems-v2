#!/usr/bin/env python3
"""Verify audio by generating subtitles and comparing with poem text."""
import json
import os
import subprocess
import hashlib

POEMS_FILE = "app/src/main/assets/poems.json"
AUDIO_DIR = "app/src/main/assets/audio"
VERIFY_DIR = "/tmp/verify"

os.makedirs(VERIFY_DIR, exist_ok=True)

with open(POEMS_FILE, "r", encoding="utf-8") as f:
    poems = json.load(f)

poem_map = {p["id"]: p for p in poems}

# Test first 5 poems
for pid in [1, 2, 3, 4, 5]:
    p = poem_map[pid]
    title = p["title"]
    content = p["content"].replace("\n", "，")
    text = f"《{title}》，{p['author']}。{content}"
    
    mp3_path = os.path.join(VERIFY_DIR, f"v_{pid:03d}.mp3")
    vtt_path = os.path.join(VERIFY_DIR, f"v_{pid:03d}.vtt")
    
    # Generate audio with subtitles
    proc = subprocess.run(
        ["edge-tts", "--voice", "zh-CN-YunxiNeural", "--rate=-10%",
         "--write-media", mp3_path, "--write-subtitles", vtt_path, "--text", text],
        capture_output=True, text=True, timeout=30
    )
    
    if proc.returncode != 0:
        print(f"id={pid} ({title}): TTS failed")
        continue
    
    # Read generated VTT
    if os.path.exists(vtt_path):
        with open(vtt_path, "r") as f:
            vtt_content = f.read()
        # Extract text from VTT (skip header and timestamps)
        lines = vtt_content.strip().split("\n")
        subtitle_text = " ".join(l for l in lines if l and not l.startswith("WEBVTT") and "-->" not in l and not l[0].isdigit())
        print(f"id={pid} ({title}):")
        print(f"  Expected: {text[:60]}...")
        print(f"  Got:      {subtitle_text[:60]}...")
        print(f"  Match: {'YES' if title in subtitle_text else 'NO'}")
    else:
        print(f"id={pid} ({title}): no VTT generated")
    
    # Compare audio MD5
    existing = os.path.join(AUDIO_DIR, f"poem_{pid:03d}.mp3")
    if os.path.exists(existing) and os.path.exists(mp3_path):
        with open(mp3_path, "rb") as f1, open(existing, "rb") as f2:
            h1 = hashlib.md5(f1.read()).hexdigest()
            h2 = hashlib.md5(f2.read()).hexdigest()
        print(f"  Audio MD5 match: {'YES' if h1 == h2 else 'NO'}")
    print()

# Cleanup
import shutil
shutil.rmtree(VERIFY_DIR, ignore_errors=True)
