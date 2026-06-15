import urllib.request
import json
import socket

instances = [
    "https://invidious.jing.rocks",
    "https://invidious.nerdvpn.de",
    "https://invidious.io.lol",
    "https://vid.puffyan.us",
    "https://inv.vern.cc",
    "https://yewtu.be",
    "https://invidious.projectsegfau.lt"
]

socket.setdefaulttimeout(3)

for url in instances:
    api = f"{url}/api/v1/videos/ZvXN0TcZLfQ"
    try:
        req = urllib.request.Request(api, headers={"User-Agent": "Mozilla/5.0"})
        res = urllib.request.urlopen(req)
        data = json.loads(res.read())
        if "adaptiveFormats" in data:
            print(f"✅ WORKS: {url}")
        else:
            print(f"❌ FAILED: {url} (No adaptiveFormats)")
    except Exception as e:
        print(f"❌ ERROR: {url} -> {e}")
