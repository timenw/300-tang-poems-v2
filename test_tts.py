#!/usr/bin/env python3
"""Quick test: generate first 3 poems only."""
import asyncio
import json
import os
import edge_tts

VOICE = "zh-CN-YunxiNeural"
RATE = "-10%"
OUTPUT_DIR = "audio_test"

async def main():
    with open("app/src/main/assets/poems.json") as f:
        poems = json.load(f)
    
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    for poem in poems[:3]:
        pid = poem["id"]
        title = poem["title"]
        content = poem["content"].replace("\n", "，")
        text = f"《{title}》，{poem['author']}。{content}"
        
        filename = f"poem_{pid:03d}.mp3"
        filepath = os.path.join(OUTPUT_DIR, filename)
        
        print(f"Generating {filename} ({title})...")
        communicate = edge_tts.Communicate(text, VOICE, rate=RATE)
        await communicate.save(filepath)
        size = os.path.getsize(filepath)
        print(f"  Done: {size//1024}KB")

asyncio.run(main())
