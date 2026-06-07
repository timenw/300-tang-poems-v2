import json
with open("app/src/main/assets/poems.json") as f:
    poems = json.load(f)
print(f"Poems: {len(poems)}")
print(f"First: {poems[0]['id']} {poems[0]['title']}")
print(f"Last:  {poems[-1]['id']} {poems[-1]['title']}")
