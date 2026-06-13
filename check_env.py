#!/usr/bin/env python3
"""Check available CJK fonts and Pillow availability."""
import os
import subprocess

print("=== Checking Pillow ===")
try:
    from PIL import Image, ImageDraw, ImageFont
    print(f"Pillow version: {Image.__version__}")
except ImportError as e:
    print(f"Pillow not installed: {e}")
    print("Installing...")
    subprocess.run(["pip3", "install", "Pillow"], capture_output=True)
    from PIL import Image, ImageDraw, ImageFont
    print("Pillow installed successfully")

print("\n=== Checking CJK Fonts ===")
font_dirs = [
    "/usr/share/fonts/truetype/noto",
    "/usr/share/fonts/truetype/wqy",
    "/usr/share/fonts/opentype/noto",
    "/usr/share/fonts/truetype/droid",
    "/usr/share/fonts/truetype/",
]

found_fonts = []
for d in font_dirs:
    if os.path.isdir(d):
        for root, dirs, files in os.walk(d):
            for f in files:
                fl = f.lower()
                if any(kw in fl for kw in ["cjk", "chinese", "wqy", "notosans", "droid", "fallback"]):
                    fp = os.path.join(root, f)
                    found_fonts.append(fp)

if found_fonts:
    for fp in found_fonts:
        print(f"  {fp}")
else:
    print("  No CJK fonts found in standard dirs")
    # Try fc-list
    try:
        result = subprocess.run(["fc-list", ":lang=zh"], capture_output=True, text=True, timeout=5)
        if result.stdout:
            for line in result.stdout.strip().split("\n")[:10]:
                print(f"  {line}")
        else:
            print("  fc-list returned nothing")
    except:
        print("  fc-list not available")
