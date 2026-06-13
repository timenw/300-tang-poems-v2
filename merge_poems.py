#!/usr/bin/env python3
"""Merge extra poems into poems.json and report stats."""
import json
import os
import glob

# Load current poems
with open('app/src/main/assets/poems.json', 'r', encoding='utf-8') as f:
    current = json.load(f)

current_ids = set(p['id'] for p in current)
print(f"Current: {len(current)} poems, IDs {min(current_ids)}-{max(current_ids)}")

# Load and merge extra poems
added = 0
for f in sorted(glob.glob('extra_poems_part*.json')):
    with open(f, 'r', encoding='utf-8') as fh:
        extra = json.load(fh)
    for p in extra:
        if p['id'] not in current_ids:
            current.append(p)
            current_ids.add(p['id'])
            added += 1
    print(f"  {f}: {len(extra)} poems, IDs {extra[0]['id']}-{extra[-1]['id']}")

# Sort by ID
current.sort(key=lambda p: p['id'])

total = len(current)
all_ids = [p['id'] for p in current]
print(f"\nMerged: {total} poems, IDs {min(all_ids)}-{max(all_ids)}")
print(f"Added: {added} poems")

# Check for gaps
full_set = set(all_ids)
max_id = max(all_ids)
gaps = [i for i in range(1, max_id+1) if i not in full_set]
if gaps:
    print(f"Gaps: {len(gaps)} missing IDs: {gaps[:20]}...")
else:
    print("No gaps - continuous from 1 to", max_id)

# Check how many need audio
audio_dir = 'audio'
audio_files = set()
if os.path.isdir(audio_dir):
    for f in os.listdir(audio_dir):
        if f.endswith('.mp3'):
            try:
                pid = int(f.replace('poem_', '').replace('.mp3', ''))
                audio_files.add(pid)
            except:
                pass

need_audio = [p for p in current if p['id'] not in audio_files]
print(f"\nAudio: {len(audio_files)} files exist, {len(need_audio)} poems need audio")
if need_audio:
    print(f"Need IDs: {[p['id'] for p in need_audio[:20]]}...")

# Save merged
with open('app/src/main/assets/poems.json', 'w', encoding='utf-8') as f:
    json.dump(current, f, ensure_ascii=False, indent=2)
print(f"\nSaved merged poems.json ({os.path.getsize('app/src/main/assets/poems.json')} bytes)")
