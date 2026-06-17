import urllib.request
import json

invidious_instances = [
    "https://yewtu.be",
    "https://invidious.nerdvpn.de",
    "https://invidious.slipfox.xyz",
    "https://inv.tux.im",
    "https://invidious.protokolla.fi",
    "https://invidious.poast.org",
    "https://inv.zzls.xyz",
    "https://invidious.epicsite.app"
]

for url in invidious_instances:
    try:
        req = urllib.request.Request(f"{url}/api/v1/videos/kcQC0VuxtDg", headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=3) as response:
            if response.status == 200:
                print(f"SUCCESS: {url}")
                break
    except Exception as e:
        print(f"FAILED: {url} - {e}")
