#!/usr/bin/env python3
"""
Batch generate TTS audio for all 300 poems using Edge TTS (Yunxi voice).
Output: audio/poem_001.mp3, audio/poem_002.mp3, ...
"""
import asyncio
import json
import os
import sys
import zipfile

import edge_tts

VOICE = "zh-CN-YunxiNeural"
RATE = "-10%"  # slightly slower for poetry
VOLUME = "+0dB"
OUTPUT_DIR = "audio"
POEMS_FILE = "app/src/main/assets/poems.json"
ZIP_FILE = "poem_audio_v2.zip"


async def generate_one(poem, semaphore):
    """Generate a single poem's audio."""
    poem_id = poem["id"]
    title = poem["title"]
    content = poem["content"].replace("\n", "，")  # replace newlines with comma for natural pause

    # Build text: title + author + content
    text = f"《{title}》，{poem['author']}。{content}"

    filename = f"poem_{poem_id:03d}.mp3"
    filepath = os.path.join(OUTPUT_DIR, filename)

    # Skip if already exists
    if os.path.exists(filepath) and os.path.getsize(filepath) > 1000:
        return f"  SKIP {filename} ({title})"

    async with semaphore:
        try:
            communicate = edge_tts.Communicate(text, VOICE, rate=RATE, volume=VOLUME)
            await communicate.save(filepath)
            size = os.path.getsize(filepath)
            return f"  OK   {filename} ({title}, {size//1024}KB)"
        except Exception as e:
            return f"  FAIL {filename} ({title}): {e}"


async def main():
    # Load poems
    with open(POEMS_FILE, "r", encoding="utf-8") as f:
        poems = json.load(f)

    print(f"Loaded {len(poems)} poems")

    # Create output dir
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # Generate with concurrency limit
    semaphore = asyncio.Semaphore(5)  # max 5 concurrent requests
    tasks = [generate_one(p, semaphore) for p in poems]

    print(f"Generating audio with voice: {VOICE}")
    print(f"Output dir: {OUTPUT_DIR}")
    print("-" * 50)

    results = await asyncio.gather(*tasks)

    for r in results:
        print(r)

    # Summary
    ok = sum(1 for r in results if r.startswith("  OK"))
    skip = sum(1 for r in results if r.startswith("  SKIP"))
    fail = sum(1 for r in results if r.startswith("  FAIL"))
    print("-" * 50)
    print(f"Done: {ok} generated, {skip} skipped, {fail} failed")

    # Create ZIP
    print(f"\nCreating {ZIP_FILE}...")
    with zipfile.ZipFile(ZIP_FILE, "w", zipfile.ZIP_DEFLATED) as zf:
        for fname in sorted(os.listdir(OUTPUT_DIR)):
            if fname.endswith(".mp3"):
                fpath = os.path.join(OUTPUT_DIR, fname)
                zf.write(fpath, fname)
                print(f"  Added {fname}")

    zip_size = os.path.getsize(ZIP_FILE)
    print(f"\nZIP created: {ZIP_FILE} ({zip_size // 1024 // 1024}MB)")


if __name__ == "__main__":
    asyncio.run(main())
