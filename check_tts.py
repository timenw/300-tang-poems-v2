#!/usr/bin/env python3
"""Check if edge_tts works by listing voices via subprocess."""
import subprocess
import sys

# Method 1: Try edge-tts CLI
try:
    r = subprocess.run(['edge-tts', '--list-voices'], capture_output=True, text=True, timeout=10)
    if r.returncode == 0:
        print("edge-tts CLI works!")
        lines = [l for l in r.stdout.split('\n') if 'Yunxi' in l]
        for l in lines:
            print(l)
    else:
        print(f"edge-tts CLI error: {r.stderr[:200]}")
except FileNotFoundError:
    print("edge-tts CLI not found")
except subprocess.TimeoutExpired:
    print("edge-tts CLI timed out")
except Exception as e:
    print(f"edge-tts CLI exception: {e}")

# Method 2: Try python edge_tts module
try:
    import edge_tts
    print(f"edge_tts module version: {edge_tts.__version__ if hasattr(edge_tts, '__version__') else 'unknown'}")
    # Try a quick voice list
    import asyncio
    async def check():
        try:
            voices = await edge_tts.list_voices()
            yunxi = [v for v in voices if 'Yunxi' in v['Name']]
            print(f"Found {len(yunxi)} Yunxi voices")
            for v in yunxi[:3]:
                print(f"  {v['Name']}")
            return True
        except Exception as e:
            print(f"edge_tts.list_voices failed: {e}")
            return False
    result = asyncio.run(check())
    print(f"edge_tts module works: {result}")
except ImportError:
    print("edge_tts module not installed")
except Exception as e:
    print(f"edge_tts module exception: {e}")
