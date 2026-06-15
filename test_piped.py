import urllib.request
import json
import socket

instances = [
    "https://pipedapi.kavin.rocks",
    "https://pipedapi.us.projectsegfau.lt",
    "https://api.piped.privacydev.net",
    "https://pipedapi.smnz.de",
    "https://api.piped.yt",
    "https://piped-api.lunar.icu",
    "https://pipedapi.in.projectsegfau.lt",
    "https://pipedapi.adminforge.de",
    "https://piped-api.garudalinux.org",
    "https://pipedapi.tokhmi.xyz"
]

socket.setdefaulttimeout(3)

for url in instances:
    api = f"{url}/streams/ZvXN0TcZLfQ"
    try:
        req = urllib.request.Request(api, headers={"User-Agent": "Mozilla/5.0"})
        res = urllib.request.urlopen(req)
        data = json.loads(res.read())
        if "audioStreams" in data:
            print(f"✅ WORKS: {url}")
        else:
            print(f"❌ FAILED: {url} (No audioStreams)")
    except Exception as e:
        print(f"❌ ERROR: {url} -> {e}")
