#!/usr/bin/env python3
"""Generate a Tang Poetry themed APK launcher icon using Pillow."""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math
import os

# Icon sizes for Android mipmap directories
SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

BASE_DIR = "/root/poem300-v2/app/src/main/res"

# Color palette - Tang Dynasty inspired
DARK_RED = (139, 0, 0)        # 深红 - 背景主色
GOLD = (218, 165, 32)          # 金色 - 文字
LIGHT_GOLD = (255, 215, 0)     # 亮金 - 高光
DARK_GOLD = (184, 134, 11)     # 暗金 - 阴影
CREAM = (255, 248, 220)        # 米白 - 装饰
BLACK = (20, 10, 5)            # 近黑 - 边框

def create_icon(size):
    """Create a single icon image at the given size."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Background - dark red rounded rectangle
    margin = max(1, size // 24)
    draw.rounded_rectangle(
        [margin, margin, size - margin, size - margin],
        radius=size // 8,
        fill=DARK_RED
    )
    
    # Inner border - gold
    border = max(1, size // 32)
    inner_margin = margin + border * 2
    draw.rounded_rectangle(
        [inner_margin, inner_margin, size - inner_margin, size - inner_margin],
        radius=size // 10,
        outline=GOLD,
        width=border
    )
    
    # Decorative corner dots (classic Chinese pattern)
    dot_r = max(1, size // 48)
    corners = [
        (inner_margin + border * 3, inner_margin + border * 3),
        (size - inner_margin - border * 3, inner_margin + border * 3),
        (inner_margin + border * 3, size - inner_margin - border * 3),
        (size - inner_margin - border * 3, size - inner_margin - border * 3),
    ]
    for cx, cy in corners:
        draw.ellipse([cx - dot_r, cy - dot_r, cx + dot_r, cy + dot_r], fill=GOLD)
    
    # Main character "诗" (poetry) in center
    char = "诗"
    # Try to find a suitable Chinese font
    font_size = int(size * 0.55)
    font = get_cjk_font(font_size)
    
    # Draw character with shadow effect
    bbox = draw.textbbox((0, 0), char, font=font)
    text_w = bbox[2] - bbox[0]
    text_h = bbox[3] - bbox[1]
    tx = (size - text_w) // 2 - bbox[0]
    ty = (size - text_h) // 2 - bbox[1]
    
    # Shadow
    shadow_offset = max(1, size // 96)
    draw.text((tx + shadow_offset, ty + shadow_offset), char, font=font, fill=DARK_GOLD)
    # Main text
    draw.text((tx, ty), char, font=font, fill=GOLD)
    # Highlight
    draw.text((tx - 1, ty - 1), char, font=font, fill=LIGHT_GOLD)
    
    # Small decorative text at bottom - "唐" (Tang)
    small_font = get_cjk_font(int(size * 0.12))
    small_char = "唐"
    sbbox = draw.textbbox((0, 0), small_char, font=small_font)
    sw = sbbox[2] - sbbox[0]
    sh = sbbox[3] - sbbox[1]
    sx = (size - sw) // 2 - sbbox[0]
    sy = size - inner_margin - border * 2 - sh - sbbox[1]
    draw.text((sx, sy), small_char, font=small_font, fill=CREAM)
    
    return img

def get_cjk_font(size):
    """Try to find a CJK font on the system."""
    font_paths = [
        "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
        "/usr/share/fonts/truetype/noto/NotoSansCJK-Bold.ttc",
        "/usr/share/fonts/truetype/noto/NotoSansCJKsc-Bold.otf",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
        "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
    ]
    for fp in font_paths:
        if os.path.exists(fp):
            try:
                return ImageFont.truetype(fp, size)
            except:
                continue
    return ImageFont.load_default()

def create_round_icon(size):
    """Create a round version of the icon (ic_launcher_round)."""
    img = create_icon(size)
    # Create circular mask
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse([0, 0, size, size], fill=255)
    img.putalpha(mask)
    return img

# Generate all icons
print("Generating Tang Poetry themed icons...")
for dirname, size in SIZES.items():
    dirpath = os.path.join(BASE_DIR, dirname)
    os.makedirs(dirpath, exist_ok=True)
    
    # Regular icon
    icon = create_icon(size)
    icon.save(os.path.join(dirpath, "ic_launcher.png"), "PNG")
    print(f"  {dirname}/ic_launcher.png ({size}x{size})")
    
    # Round icon
    round_icon = create_round_icon(size)
    round_icon.save(os.path.join(dirpath, "ic_launcher_round.png"), "PNG")
    print(f"  {dirname}/ic_launcher_round.png ({size}x{size})")

print("\nDone! All icons generated.")
