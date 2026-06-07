#!/usr/bin/env python3
"""Batch generate TTS audio for all poems using Edge TTS (Yunxi voice)."""
import asyncio
import json
import os
import zipfile
import edge_tts

VOICE = "zh-CN-YunxiNeural"
RATE = "-10%"
OUTPUT_DIR = "audio"
POEMS_FILE = "app/src/main/assets/poems.json"
ZIP_FILE = "poem_audio_v2.zip"

async def generate_one(poem, semaphore):
    pid = poem["id"]
    title = poem["title"]
    content = poem["content"].replace("\n", "，")
    text = f"《{title}》，{poem['author']}。{content}"
    fname = f"poem_{pid:03d}.mp3"
    fpath = os.path.join(OUTPUT_DIR, fname)
    if os.path.exists(fpath) and os.path.getsize(fpath) > 1000:
        return f"  SKIP {fname}"
    async with semaphore:
        try:
            comm = edge_tts.Communicate(text, VOICE, rate=RATE)
            await comm.save(fpath)
            sz = os.path.getsize(fpath)
            return f"  OK   {fname} ({title}, {sz//1024}KB)"
        except Exception as e:
            return f"  FAIL {fname} ({title}): {e}"

async def main():
    with open(POEMS_FILE, "r", encoding="utf-8") as f:
        poems = json.load(f)
    print(f"Loaded {len(poems)} poems")
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    semaphore = asyncio.Semaphore(5)
    tasks = [generate_one(p, semaphore) for p in poems]
    print(f"Generating with voice: {VOICE}")
    results = await asyncio.gather(*tasks)
    for r in results:
        print(r)
    ok = sum(1 for r in results if "OK" in r)
    skip = sum(1 for r in results if "SKIP" in r)
    fail = sum(1 for r in results if "FAIL" in r)
    print(f"\nDone: {ok} generated, {skip} skipped, {fail} failed")
    # Create ZIP
    print(f"\nCreating {ZIP_FILE}...")
    with zipfile.ZipFile(ZIP_FILE, "w", zipfile.ZIP_DEFLATED) as zf:
        for fname in sorted(os.listdir(OUTPUT_DIR)):
            if fname.endswith(".mp3"):
                zf.write(os.path.join(OUTPUT_DIR, fname), fname)
    zip_sz = os.path.getsize(ZIP_FILE)
    print(f"ZIP: {ZIP_FILE} ({zip_sz//1024}KB)")

asyncio.run(main())
