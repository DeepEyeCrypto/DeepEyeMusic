import urllib.request
import json

url = "https://www.youtube.com/youtubei/v1/player"
headers = {
    "Content-Type": "application/json",
    "User-Agent": "com.google.ios.youtube/19.29.1 (iPhone14,3; U; CPU iOS 15_6_1 like Mac OS X)",
}
data = {
    "context": {
        "client": {
            "clientName": "IOS",
            "clientVersion": "19.29.1",
            "deviceMake": "Apple",
            "deviceModel": "iPhone14,3",
            "osName": "iPhone",
            "osVersion": "15.6.1.19G82"
        }
    },
    "videoId": "ZvXN0TcZLfQ",
}
req = urllib.request.Request(url, headers=headers, data=json.dumps(data).encode('utf-8'))
try:
    response = urllib.request.urlopen(req)
    res = json.loads(response.read())
    streaming_data = res.get("streamingData", {})
    formats = streaming_data.get("formats", [])
    adaptive = streaming_data.get("adaptiveFormats", [])
    
    print("Formats:", len(formats))
    for f in formats:
        print(f.get("itag"), "Cipher:", "signatureCipher" in f, "Url:", "url" in f)
        
    print("Adaptive:", len(adaptive))
    for f in adaptive:
        print(f.get("itag"), "Cipher:", "signatureCipher" in f, "Url:", "url" in f)
except Exception as e:
    print("ERROR:", e)
    if hasattr(e, 'read'):
        print(e.read())
