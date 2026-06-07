import asyncio
import json
import os
import edge_tts

VOICE = "zh-CN-YunxiNeural"
RATE = "-10%"

async def main():
    with open("app/src/main/assets/poems.json") as f:
        poems = json.load(f)
    
    os.makedirs("audio_test", exist_ok=True)
    
    for poem in poems[:2]:
        pid = poem["id"]
        title = poem["title"]
        content = poem["content"].replace("\n", "，")
        text = f"《{title}》，{poem['author']}。{content}"
        fname = f"poem_{pid:03d}.mp3"
        fpath = f"audio_test/{fname}"
        print(f"Generating {fname} ({title})...")
        comm = edge_tts.Communicate(text, VOICE, rate=RATE)
        await comm.save(fpath)
        sz = os.path.getsize(fpath)
        print(f"  -> {sz//1024}KB OK")
    
    print("TEST DONE")

asyncio.run(main())
