import urllib.request
import json

piped_instances = [
    "https://api.piped.private.coffee",
    "https://pipedapi.kavin.rocks",
    "https://pipedapi.us.projectsegfau.lt",
    "https://pipedapi.lunar.icu"
]

for url in piped_instances:
    try:
        req = urllib.request.Request(f"{url}/streams/kcQC0VuxtDg", headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=3) as response:
            if response.status == 200:
                print(f"SUCCESS: {url}")
                break
    except Exception as e:
        print(f"FAILED: {url} - {e}")
