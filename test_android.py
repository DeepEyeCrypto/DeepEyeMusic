import urllib.request
import json

url = "https://www.youtube.com/youtubei/v1/player"
headers = {
    "Content-Type": "application/json",
    "User-Agent": "com.google.android.youtube/19.29.37 (Linux; U; Android 14; en_US) gzip",
    "X-Goog-Api-Format-Version": "2"
}
data = {
    "context": {
        "client": {
            "clientName": "ANDROID",
            "clientVersion": "19.29.37",
            "androidSdkVersion": 33
        }
    },
    "videoId": "ZvXN0TcZLfQ",
    "playbackContext": {
        "contentPlaybackContext": {
            "signatureTimestamp": 19890
        }
    }
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
